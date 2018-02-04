package livenet.analysis;

/*
 * The following code is based on original code from Sun Microsystems,
 * which has been heavily modified and extended. Sun's original code
 * now accounts for only about 20% of the total code of this class. To
 * acknowledge the source of the original code, Sun's copyright notice
 * is preserved.
 */

/*
 * @(#)Graph.java	1.7 98/07/17
 *
 * Copyright (c) 1997, 1998 Sun Microsystems, Inc. All Rights Reserved.
 *
 * Sun grants you ("Licensee") a non-exclusive, royalty free, license to use,
 * modify and redistribute this software in source and binary code form,
 * provided that i) this copyright notice and license appear on all copies of
 * the software; and ii) Licensee does not utilize the software in a manner
 * which is disparaging to Sun.
 *
 * This software is provided "AS IS," without a warranty of any kind. ALL
 * EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND WARRANTIES, INCLUDING ANY
 * IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR
 * NON-INFRINGEMENT, ARE HEREBY EXCLUDED. SUN AND ITS LICENSORS SHALL NOT BE
 * LIABLE FOR ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING
 * OR DISTRIBUTING THE SOFTWARE OR ITS DERIVATIVES. IN NO EVENT WILL SUN OR ITS
 * LICENSORS BE LIABLE FOR ANY LOST REVENUE, PROFIT OR DATA, OR FOR DIRECT,
 * INDIRECT, SPECIAL, CONSEQUENTIAL, INCIDENTAL OR PUNITIVE DAMAGES, HOWEVER
 * CAUSED AND REGARDLESS OF THE THEORY OF LIABILITY, ARISING OUT OF THE USE OF
 * OR INABILITY TO USE SOFTWARE, EVEN IF SUN HAS BEEN ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGES.
 *
 * This software is not designed or intended for use in on-line control of
 * aircraft, air traffic, aircraft navigation or aircraft communications; or in
 * the design, construction, operation or maintenance of any nuclear
 * facility. Licensee represents and warrants that it will not use or
 * redistribute the Software for such purposes.
 */

import java.util.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.print.*;
import java.sql.*;
import javax.swing.*;


public class GraphWeightPanel extends JPanel implements Runnable, Printable,
    ItemListener, MouseListener, MouseMotionListener, TreeGraphConstants,
    ControlListener
{
    // set to true to get debugging output
    private final boolean DEBUG = false;

    WorkspaceElementNode node;

    Vector discussants = new Vector();
    Vector nodes = new Vector();
    Vector edges = new Vector();

    Thread relaxer;

    // Number of times the relax() method has been called since the
    // last user action
    long iterations = 0;

    // Maximum number of iterations to recalculate spring forces after
    // the last user action
    final long maxIterations = 10;

    // Paint-related stuff
    final double screenSizeFraction = 0.5;
    WorkspaceElementNode pick;
    boolean pickfixed;
    Image offscreen;
    Dimension offscreensize;
    Graphics offgraphics;
    WorkspaceElementNode focusNode;
    WorkspaceElementNode fixedNode;
    boolean printing = false;

    // stuff for the popup menu
    JPopupMenu popup;
    JCheckBoxMenuItem shortLabelMI;
    JCheckBoxMenuItem animationMI;

    // edge length
    int edgeLength = initialEdgeLength;

    // display of short node labels
    boolean shortLabels = false;

    // animation
    boolean animation = true;

    // Database connection object
    Connection con;

    // Query to retrieve all discussion users (including those who are
    // no longer assigned to the discussion, and those who have never
    // posted a statement) and a count of statements
    String discUserQuery =
	"SELECT P.Participant, 0 " +
	"FROM   DBA.Objects O, DBA.Folders F, DBA.FolderRoles R, " +
	"       DBA.Participants P " +
	"WHERE  O.Url = ? " +
	"AND    O.Workspace = F.Workspace " +
	"AND    O.Owner = F.Owner " +
	"AND    O.Type = F.Folder " +
	"AND    F.Workspace = R.Workspace " +
	"AND    F.Owner = R.Owner " +
	"AND    F.Folder = R.Folder " +
	"AND    P.Workspace = R.Workspace " +
	"AND    P.Owner = R.Owner " +
	"AND    P.Role = R.Role " +
	"AND    NOT EXISTS " +
	"       (SELECT S.Originator " +
	"        FROM   DBA.Statements S, Blocks B " +
	"        WHERE  S.BlockId = B.BlockId " +
	"        AND    B.BlockURL = O.Url " +
	"        AND    S.Originator = P.Participant) " +
	"UNION " +
	"SELECT S.Originator, COUNT(*) " +
	"FROM   DBA.Statements S, DBA.Blocks B " +
	"WHERE  S.BlockId = B.BlockId " +
	"AND    B.BlockURL = ? " +
	"GROUP  BY S.Originator";

    // Query to retrieve pairs of discussants and count of replies
    String discUserPairQuery =
	"SELECT S1.Originator, S2.Originator, COUNT(*) " +
	"FROM   DBA.Statements S1, DBA.Statements S2, DBA.Blocks B " +
	"WHERE  S1.BlockId = B.BlockId " +
	"AND    B.BlockURL = ? " +
	"AND    S1.StatementNo = S2.ParentStmtNo " +
	"AND    S1.Originator <> S2.Originator " +
	"GROUP  BY S1.Originator, S2.Originator";


    public GraphWeightPanel(Connection con, WorkspaceElementNode node,
			    MapControls controls)
    {
	// disable double buffering
	super(false);

	this.setOpaque(true);

	this.con = con;

	addMouseListener(this);

	// set the size of the window
	Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
	this.setSize((int) (screenSize.width * screenSizeFraction),
		     (int) (screenSize.height * screenSizeFraction));

	String discUrl = ((Discussion) node.element).url;

	initializeNodes(discUrl);

	initializeEdges(discUrl);

	this.node = node;

	controls.addControlListener(this);
    }


    void initializeNodes(String discUrl)
    {
	try {
	    PreparedStatement stmt = con.prepareStatement(discUserQuery);
	    stmt.setString(1, discUrl);
	    stmt.setString(2, discUrl);
	    ResultSet rs = stmt.executeQuery();

	    while (rs.next()) {
		String user = rs.getString(1);
		int count = rs.getInt(2);

		DiscussionParticipant part =
		    new DiscussionParticipant(user, count);

		WorkspaceElementNode n = new 
		    WorkspaceElementNode(WorkspaceElement.PARTICIPANT, part);

		n.setVisible(true);

		if (!shortLabels)
		    n.setLongDisplayName();

		nodes.addElement(n);
	    }
	    rs.close();
	    stmt.close();


	    // Calculate node positions
	    int numNodes = nodes.size();
	    // a single node is placed in the centre of the window
	    if (numNodes == 1) {
		WorkspaceElementNode n =
		    (WorkspaceElementNode) nodes.elementAt(0);
		n.x = this.getWidth() / 2;
		n.y = this.getHeight() / 2;
	    } else if (numNodes > 1)
		// if there is more than one node, they are placed on
		// a circle centered on the centre of the window
		for (int i = 0; i < numNodes; i++) {
		    WorkspaceElementNode n =
			(WorkspaceElementNode) nodes.elementAt(i);
		    double angle = i * 2 * Math.PI / numNodes;
		    n.x = (this.getWidth() / 2) -
			(edgeLength * Math.sin(angle));
		    n.y = (this.getHeight() / 2) +
			(edgeLength * Math.cos(angle));
	    }
	}
	catch (SQLException ex) {
	    System.err.println("initializeNodes: " + ex.getMessage() +
			       "\n  " + ex.getSQLState());
	}
    }


    void initializeEdges(String discUrl)
    {
	try {
	    PreparedStatement stmt = con.prepareStatement(discUserPairQuery);
	    stmt.setString(1, discUrl);
	    ResultSet rs = stmt.executeQuery();

	    while (rs.next()) {
		String user1 = rs.getString(1);
		String user2 = rs.getString(2);
		int count = rs.getInt(3);

		WorkspaceElementNode node1 = getNode(user1);
		WorkspaceElementNode node2 = getNode(user2);

		if (node1 != null && node2 != null) {
		    // check if there is an edge from user1 to user2
		    DiscussionParticipantEdge e = getEdge(node1, node2);
		    if (e != null)
			e.fromWeight = count;
		    else {
			// check if there is an edge from user2 to user1
			e = getEdge(node2, node1);
			if (e != null)
			    e.toWeight = count;
			else {
			    // no edge, so create a new one
			    e = new DiscussionParticipantEdge(node1, node2);
			    e.fromWeight = count;
			}
		    }
		    edges.addElement(e);
		} else {
		    System.err.println("initializeEdges: node(s) not found");
		}
	    }
	    rs.close();
	    stmt.close();
	}
	catch (SQLException ex) {
	    System.err.println("initializeEdges: " + ex.getMessage() +
			       "\n  " + ex.getSQLState());
	}
    }


    WorkspaceElementNode getNode(String user)
    {
	for (int i = 0; i < nodes.size(); i++) {
	    WorkspaceElementNode node =
		(WorkspaceElementNode) nodes.elementAt(i);
	    if (((Participant) node.element).name.equals(user))
		return node;
	}
	return null;
    }


    DiscussionParticipantEdge getEdge(WorkspaceElementNode node1,
				      WorkspaceElementNode node2)
    {
	for (int i = 0; i < edges.size(); i++) {
	    DiscussionParticipantEdge edge =
		(DiscussionParticipantEdge) edges.elementAt(i);
	    if (edge.fromNode.equals(node1) && edge.toNode.equals(node2))
		return edge;
	}
	return null;
    }


    Color getNodeColour(WorkspaceElementNode node)
    {
	Color paintColour = Color.white;

	int count = ((DiscussionParticipant) node.element).numStmts;

	if (count > 0 && count < 100) {
	    int green = (int) 255 - (count * (255 / 100));
	    paintColour = new Color(255, green , 0);
	}

	if (count >= 100)
	    paintColour = Color.red;

	return paintColour;
    }


    public Color calculateTextColour(Color bgColour)
    {
	int red = bgColour.getRed();
	int green = bgColour.getGreen();
	int blue = bgColour.getBlue();

	int grayscale = red * 3 + green * 6 + blue;

	if (grayscale > 5 * 255)
	    return Color.black;

	return Color.white;
    }


    public void run()
    {
        Thread me = Thread.currentThread();
	while (relaxer == me) {
	    relax();
	    try {
		Thread.sleep(100);
	    } catch (InterruptedException e) {
		break;
	    }
	}
    }


    /**
       Perform the calculation of spring forces and repulsion forces
       that apply to edges and nodes respectively.
    **/
    synchronized void relax()
    {
	if (++iterations > maxIterations) // && noMajorChanges)
	    return;

	// recalculate spring force
	for (int i = 0 ; i < edges.size() ; i++) {
	    WorkspaceElementEdge e = (WorkspaceElementEdge) edges.elementAt(i);
	    if (! (e.fromNode.isVisible() && e.toNode.isVisible()))
		continue;
	    double vx = e.toNode.x - e.fromNode.x;
	    double vy = e.toNode.y - e.fromNode.y;
	    double len = Math.sqrt(vx * vx + vy * vy);
            len = (len == 0) ? .0001 : len;
	    //double f = (e.length - len) / (len * 3);
	    double f = (e.length - len) / (len * 10);
	    double dx = f * vx;
	    double dy = f * vy;

	    e.toNode.dx += dx;
	    e.toNode.dy += dy;
	    e.fromNode.dx += -dx;
	    e.fromNode.dy += -dy;
	}

	// recalculate repulsion force
	for (int i = 0 ; i < nodes.size() ; i++) {
	    WorkspaceElementNode n1 =
		(WorkspaceElementNode) nodes.elementAt(i);
	    if (! n1.isVisible())
		continue;
	    double dx = 0;
	    double dy = 0;

	    for (int j = 0 ; j < nodes.size() ; j++) {

		if (i == j)
		    continue;

		WorkspaceElementNode n2 =
		    (WorkspaceElementNode) nodes.elementAt(j);

		if (! n2.isVisible())
		    continue;

		double vx = n1.x - n2.x;
		double vy = n1.y - n2.y;
		double len = vx * vx + vy * vy;
		if (len == 0) {
		    dx += Math.random();
		    dy += Math.random();
		} else if (len < edgeLength * edgeLength) {
		    dx += vx / len;
		    dy += vy / len;
		}
	    }
	    double dlen = dx * dx + dy * dy;
	    if (dlen > 0) {
		dlen = Math.sqrt(dlen) / 10;  //2
		n1.dx += dx / dlen;
		n1.dy += dy / dlen;
	    }
	}

	Dimension d = this.getSize();
	for (int i = 0 ; i < nodes.size() ; i++) {
	    WorkspaceElementNode n = (WorkspaceElementNode) nodes.elementAt(i);

	    if (! n.isVisible())
		continue;

	    // If it's a movable node, move it a few pixels according to dx, dy
	    if (! n.fixed) {
		n.x += Math.max(-5, Math.min(5, n.dx));
		n.y += Math.max(-5, Math.min(5, n.dy));
            }

	    // If the node is (partly) outside the allowable bounds,
	    // move it back in
            if (n.x1 < marginGap) {
		n.x += marginGap - n.x1;
		n.x2 += marginGap - n.x1;
                n.x1 = 0 + marginGap;
            } else if (n.x2 > d.width - marginGap) {
		n.x -= n.x2 - (d.width - marginGap);
		n.x1 -= n.x2 - (d.width - marginGap);
                n.x2 = d.width - marginGap;
            }
            if (n.y1 < marginGap) {
		n.y += marginGap - n.y1;
		n.y2 += marginGap - n.y1;
                n.y1 = marginGap;
            } else if (n.y2 > d.height - marginGap) {
		n.y -= n.y2 - (d.height - marginGap);
		n.y1 -= n.y2 - (d.height - marginGap);
                n.y2 = d.height - marginGap;
            }

	    n.dx /= 2;
	    n.dy /= 2;
	}
	repaint();
    }


    /**
       Draw a node in the panel.
    **/
    public void paintNode(Graphics g, WorkspaceElementNode n, FontMetrics fm)
    {
	int x = (int) n.x;
	int y = (int) n.y;

	// select the background colour of the node
	Color paintColour = getNodeColour(n);
	g.setColor(paintColour);
	Color textColour = calculateTextColour(paintColour);

	// width and height of the box
	int w = fm.stringWidth(n.displayName) + 10;
	int h = fm.getHeight() + 4;

	// coordinates of the box's top left and bottom right corner
	n.x1 = x - w / 2;
	n.y1 = y - h / 2;
	n.x2 = n.x1 + w - 1;
	n.y2 = n.y1 + h - 1;

	// draw the box, its border and the text label inside it
	g.fillRect(n.x1, n.y1, w, h);
	g.setColor(Color.black);
	g.drawRect(n.x1, n.y1, w - 1, h - 1);
	g.setColor(textColour);
	g.drawString(n.displayName, x - (w - 10) / 2, (y - (h - 4) / 2) +
		     fm.getAscent());

	// draw some little brackets outside the corners of the focus node
	if (n.hasFocus()) {
	    int offset = 0;
	    for (int i = 1; i <= focusThickness; i++) {
		// top-left vertical line
		g.drawLine(n.x1 - (offset + focusGap + i),
			   n.y1 - (offset + focusGap + i),
			   n.x1 - (offset + focusGap + i),
			   n.y1 + focusLength - (offset + focusGap));
		// top-left horizontal line
		g.drawLine(n.x1 - (offset + focusGap + i),
			   n.y1 - (offset + focusGap + i),
			   n.x1 + focusLength - (offset + focusGap),
			   n.y1 - (offset + focusGap + i));

		// top-right horizontal line
		g.drawLine(n.x1 + (w - 1) + offset + focusGap - focusLength,
			   n.y1 - (offset + focusGap + i),
			   n.x1 + (w - 1) + offset + focusGap + i,
			   n.y1 - (offset + focusGap + i));

		// top-right vertical line
		g.drawLine(n.x1 + (w - 1) + offset + focusGap + i,
			   n.y1 - (offset + focusGap + i),
			   n.x1 + (w - 1) + offset + focusGap + i,
			   n.y1 + focusLength - (offset + focusGap));

		// bottom-left vertical line
		g.drawLine(n.x1 - (offset + focusGap + i),
			   n.y1 + h + offset + focusGap - focusLength,
			   n.x1 - (offset + focusGap + i),
			   n.y1 + h + offset + focusGap + i);

		// bottom-left horizontal line
		g.drawLine(n.x1 - (offset + focusGap + i),
			   n.y1 + h + offset + focusGap + i,
			   n.x1 + focusLength - (offset + focusGap),
			   n.y1 + h + offset + focusGap + i);

		// bottom-right horizontal line
		g.drawLine(n.x1 + (w - 1) + offset + focusGap - focusLength,
			   n.y1 + h + offset + focusGap + i,
			   n.x1 + (w - 1) + offset + focusGap + i,
			   n.y1 + h + offset + focusGap + i);

		// bottom-right vertical line
		g.drawLine(n.x1 + (w - 1) + offset + focusGap + i,
			   n.y1 + h + offset + focusGap - focusLength,
			   n.x1 + (w - 1) + offset + focusGap + i,
			   n.y1 + h + offset + focusGap + i);
	    }
	}
    }


    /**
      Redraw the whole graph offscreen and then show it.
    **/
    public synchronized void paint(Graphics g)
    {
	Dimension d = this.getSize();

	if ((offscreen == null) || (d.width != offscreensize.width) ||
	    (d.height != offscreensize.height)) {
	    offscreen = createImage(d.width, d.height);
	    offscreensize = d;
	    offgraphics = offscreen.getGraphics();
	    offgraphics.setFont(getFont());
	}

	// fill the whole panel with the background colour
	if (printing) {
	    offgraphics.setColor(Color.white);
	    printing = false;
	}
	else
	    offgraphics.setColor(this.getBackground());
	offgraphics.fillRect(0, 0, d.width, d.height);

	// redraw all edges, giving them the appropriate colour
	for (int i = 0 ; i < edges.size() ; i++) {
	    DiscussionParticipantEdge e =
		(DiscussionParticipantEdge) edges.elementAt(i);

	    // only proceed if both nodes are visible
	    // (i.e. don't draw edges to invisible nodes)
 	    if (! (e.fromNode.isVisible() && e.toNode.isVisible()))
 		continue;

	    double x1 = e.fromNode.x;
	    double y1 = e.fromNode.y;
	    double x2 = e.toNode.x;
	    double y2 = e.toNode.y;

	    // mid-point of the edge connecting the two nodes
	    double xmid = (x1 + ((x2 - x1) / 2));
	    double ymid = (y1 + ((y2 - y1) / 2));

	    // angle of the edge connecting the two nodes
	    double angle = Math.atan(Math.abs(x1 - x2) / Math.abs(y1 - y2));

	    // count of statements of the two users
	    int fromCount = e.fromWeight;
	    int toCount = e.toWeight;

	    // sanity checking
	    if (fromCount < 0)
		fromCount = 0;

	    if (toCount < 0)
		toCount = 0;

	    // map number of statements to line thickness in pixels
	    int fromWidth = 1 + (fromCount / 10);
	    int toWidth = 1 + (toCount / 10);

	    // limit line thickness to max. 10
	    if (fromWidth > 10)
		fromWidth = 10;

	    if (toWidth > 10)
		toWidth = 10;

	    // x/y distances from the edge for drawing thick lines
	    double deltay1 = Math.sin(angle) * (fromWidth / 2);
	    double deltax1 = Math.sqrt(((fromWidth * fromWidth) / 4) -
				       (deltay1 * deltay1));

	    double deltay2 = Math.sin(angle) * (toWidth / 2);
	    double deltax2 = Math.sqrt(((toWidth * toWidth) / 4) -
				       (deltay2 * deltay2));

	    // make sure the distances head the right way
	    if (x1 < x2) {
		deltax1 = deltax1 * -1;
		deltax2 = deltax2 * -1;
	    }

	    if (y1 < y2) {
		deltay1 = deltay1 * -1;
		deltay2 = deltay2 * -1;
	    }

	    // if the from user didn't post statements, just draw a
	    // thin gray line
	    if (fromCount == 0) {
		offgraphics.setColor(Color.gray);
		offgraphics.drawLine((int) x1, (int) y1,
				     (int) xmid, (int) ymid);
	    } else {
		// calculate points of the polygon that is the from node's
		// part of the edge
		int fromX[] = {(int) (x1 - deltax1),
			       (int) (x1 + deltax1),
			       (int) (xmid + deltax1),
			       (int) (xmid - deltax1)};
		int fromY[] = {(int) (y1 + deltay1),
			       (int) (y1 - deltay1),
			       (int) (ymid - deltay1),
			       (int) (ymid + deltay1)};

		// draw the polygon, in black
		offgraphics.setColor(Color.black);
		offgraphics.fillPolygon(fromX, fromY, 4);
		// draw the centre line, in case the polygon is too thin
		offgraphics.drawLine((int) x1, (int) y1,
				     (int) xmid, (int) ymid);
	    }

	    // if the to user didn't post statements, just draw a
	    // thin gray line
	    if (toCount == 0) {
		offgraphics.setColor(Color.gray);
		offgraphics.drawLine((int) xmid, (int) ymid,
				     (int) x2, (int) y2);
	    } else {
		// calculate points of the polygon that is the to node's
		// part of the edge
		int toX[] = {(int) (xmid - deltax2),
			     (int) (xmid + deltax2),
			     (int) (x2 + deltax2),
			     (int) (x2 - deltax2)};
		int toY[] = {(int) (ymid + deltay2),
			     (int) (ymid - deltay2),
			     (int) (y2 - deltay2),
			     (int) (y2 + deltay2)};

		// draw the polygon, in black
		offgraphics.setColor(Color.black);
		offgraphics.fillPolygon(toX, toY, 4);
		// draw the centre line, in case the polygon is too thin
		offgraphics.drawLine((int) xmid, (int) ymid,
				     (int) x2, (int) y2);
	    }

	    String fromLabel = String.valueOf(fromCount);
	    String toLabel = String.valueOf(toCount);

	    int offset = 5;  // distance of label from edge

	    int lab1x = (int) (x1 + ((x2 - x1) / 3) + deltax1 + offset);
	    int lab1y = (int) (y1 + ((y2 - y1) / 3) - deltay1 - offset);

	    int lab2x = (int) (x1 + (2 * (x2 - x1) / 3) + deltax2 + offset);
	    int lab2y = (int) (y1 + (2 * (y2 - y1) / 3) - deltay2 - offset);

	    offgraphics.setColor(Color.blue);
	    offgraphics.drawString(fromLabel, lab1x, lab1y);
	    offgraphics.drawString(toLabel, lab2x, lab2y);
	}

	offgraphics.setColor(Color.black);

	// redraw all nodes
	FontMetrics fm = offgraphics.getFontMetrics();
	for (int i = 0 ; i < nodes.size() ; i++) {
	    WorkspaceElementNode n = (WorkspaceElementNode) nodes.elementAt(i);
	    if (n.isVisible())
		paintNode(offgraphics, n, fm);
	}

	g.drawImage(offscreen, 0, 0, null);
    }


    void changeNodeVisibility(int type, boolean state)
    {
	for (int i = 0; i < nodes.size(); i++) {
	    WorkspaceElementNode node =
		(WorkspaceElementNode) nodes.elementAt(i);
	    if (node.type == type)
		node.setVisible(state);
	}
    }


    /**
       Print the currently displayed map.
    **/
    public int print(Graphics g, PageFormat pf, int pi)
	throws PrinterException
    {
        if (pi >= 1) {
            return Printable.NO_SUCH_PAGE;
        }
	printing = true;
        paint(g);
        return Printable.PAGE_EXISTS;
    }


    // mouse listener methods

    public void mouseClicked(MouseEvent e)
    {
	if (DEBUG)
	    System.out.println("[MouseClicked] mouse clicked " +
			       e.getClickCount() + " times.");
	iterations = 0;
    }


    public void mouseEntered(MouseEvent e)
    {
    }


    public void mouseExited(MouseEvent e)
    {
    }


    public void mousePressed(MouseEvent e)
    {
	if (DEBUG)
	    System.out.println("[MousePressed] mouse pressed " +
			       e.getClickCount() + " times.");

	// Pick the node that was clicked on
	double bestdist = Double.MAX_VALUE;
	int x = e.getX();
	int y = e.getY();
	Point p = e.getPoint();
	for (int i = 0 ; i < nodes.size() ; i++) {
	    WorkspaceElementNode n = (WorkspaceElementNode) nodes.elementAt(i);
	    if (! n.contains(p))
		continue;
	    double dist = (n.x - x) * (n.x - x) + (n.y - y) * (n.y - y);
	    if (n.isVisible() && dist < bestdist) {
		pick = n;
		bestdist = dist;
	    }
	}

	if (pick != null) {
	    pickfixed = pick.fixed;
	    pick.fixed = true;
	    pick.x = x;
	    pick.y = y;
	}

	addMouseMotionListener(this);

	if (e.isPopupTrigger())
	    processPopupClick(e.getX(), e.getY());

	repaint();
	e.consume();
	iterations = 0;
    }


    public void mouseReleased(MouseEvent e)
    {
	if (DEBUG)
	    System.out.println("[MouseReleased] mouse released " +
			       e.getClickCount() + " times.");

	if (pick != null) {
	    pick.x = e.getX();
	    pick.y = e.getY();
	    pick.fixed = pickfixed;
	}

	removeMouseMotionListener(this);

	if (e.isPopupTrigger())
	    processPopupClick(e.getX(), e.getY());

	pick = null;
	repaint();
	e.consume();
	iterations = 0;
    }


    // mouseMotionListener methods

    public void mouseDragged(MouseEvent e)
    {
	if (pick != null) {
	    pick.x = e.getX();
	    pick.y = e.getY();
	}
	repaint();
	e.consume();
    }


    public void mouseMoved(MouseEvent e)
    {
    }


    // Handle popup mouse clicks (such as right button on most systems)
    private void processPopupClick(int x, int y)
    {
	if (popup == null) {
	    // construct the popup menu
	    popup = new JPopupMenu("Show");

	    // create the menu items
	    shortLabelMI =
		new JCheckBoxMenuItem("Short Labels", shortLabels);
	    shortLabelMI.addItemListener(this);
	    popup.add(shortLabelMI);

	    animationMI =
		new JCheckBoxMenuItem("Animation", animation);
	    animationMI.addItemListener(this);
	    popup.add(animationMI);
	}

	// pop up the popup menu near the mouse pointer
	popup.show(this, x, y);
    }



    public void itemStateChanged(ItemEvent e)
    {
	Object source = e.getItemSelectable();
	boolean state = (e.getStateChange() == ItemEvent.SELECTED);

	if (source == shortLabelMI) {
	    shortLabels = state;
	    if (shortLabels)
		for (int i = 0; i < nodes.size(); i++)
		    ((WorkspaceElementNode) nodes.elementAt(i)).
			setShortDisplayName();
	    else
		for (int i = 0; i < nodes.size(); i++)
		    ((WorkspaceElementNode) nodes.elementAt(i)).
			setLongDisplayName();
	}
	else if (source == animationMI) {
	    animation = state;
	    if (animation)
		start();
	    else
		stop();
	}
    }


    // starting and stopping the animation

    public void start()
    {
	relaxer = new Thread(this);
	relaxer.start();
    }


    public void stop()
    {
	relaxer = null;
    }


    // handle control events (originating from the associated control panel)
    public void controlActionPerformed(ControlEvent e)
    {
	int type = e.getType();

	// nothing to do; maybe later we'll process some control events
    }
}
