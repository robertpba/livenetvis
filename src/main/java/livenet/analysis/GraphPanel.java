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
import java.sql.Connection;
import javax.swing.*;


public class GraphPanel extends JPanel implements Runnable, Printable,
    ItemListener, MouseListener, MouseMotionListener, TreeGraphConstants,
    ControlListener
{
    // set to true to get debugging output
    private final boolean DEBUG = false;

    WorkspaceNode wsNode;

    Vector nodes = new Vector();
    Vector edges = new Vector();

    Thread relaxer;

    // Number of times the relax() method has been called since the
    // last user action
    long iterations = 0;

    // Maximum number of iterations to recalculate spring forces after
    // the last user action
    final long maxIterations = 200;

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
    JCheckBoxMenuItem roleMI;
    JCheckBoxMenuItem participantMI;
    JCheckBoxMenuItem actionMI;
    JCheckBoxMenuItem discussionMI;
    JCheckBoxMenuItem documentMI;
    JCheckBoxMenuItem messageRuleMI;
    JCheckBoxMenuItem messageTypeMI;
    JCheckBoxMenuItem shortLabelMI;
    JCheckBoxMenuItem animationMI;

    // edge length
    int edgeLength = (int) (initialEdgeLength / 2);

    // visibility flags
    boolean roleVisible = true;
    boolean participantVisible = true;
    boolean actionVisible = true;
    boolean discussionVisible = true;
    boolean documentVisible = true;
    boolean messageRuleVisible = true;
    boolean messageTypeVisible = true;

    // display of short node labels
    boolean shortLabels = true;

    // animation
    boolean animation = true;

    // MapControls object
    MapControls controls;

    // Database connection object
    Connection con;


    public GraphPanel(Connection con, WorkspaceNode wsNode,
		      MapControls controls)
    {
	// disable double buffering
	super(false);

	this.setOpaque(true);

	this.con = con;

	addMouseListener(this);

	Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
	this.setSize((int) (screenSize.width * screenSizeFraction),
		     (int) (screenSize.height * screenSizeFraction));

	initializeNodes(wsNode);
	initializeEdges(wsNode);
	this.wsNode = wsNode;

	this.controls = controls;
	controls.addControlListener(this);
    }


    void initializeNodes(WorkspaceNode node)
    {
	Vector roles = node.getRoles();
	for (int i = 0; i < roles.size(); i++) {
	    WorkspaceElementNode n = new 
		WorkspaceElementNode(WorkspaceElement.ROLE,
				     (WorkspaceElement) roles.elementAt(i));
	    n.x = (i + 0.5) * this.getWidth() / roles.size();
	    n.y = 1.5 * this.getHeight() / 4;
	    n.setVisible(roleVisible);
	    nodes.addElement(n);
	}

	Vector participants = node.getParticipants();
	for (int i = 0; i < participants.size(); i++) {
	    WorkspaceElementNode n = new 
		WorkspaceElementNode(WorkspaceElement.PARTICIPANT,
				     (WorkspaceElement)
				     participants.elementAt(i));
	    n.x = (i + 0.5) * this.getWidth() / participants.size();
	    n.y = 0.5 * this.getHeight() / 4;
	    n.setVisible(participantVisible);
	    nodes.addElement(n);
	}

	Vector actions = node.getActions();
	for (int i = 0; i < actions.size(); i++) {
	    WorkspaceElementNode n = new 
		WorkspaceElementNode(WorkspaceElement.ACTION,
				     (WorkspaceElement)
				     actions.elementAt(i));
	    n.x = (i + 0.5) * this.getWidth() / actions.size();
	    n.y = 2.5 * this.getHeight() / 4;
	    n.setVisible(actionVisible);
	    nodes.addElement(n);
	}

	Vector discussions = node.getDiscussions();
	for (int i = 0; i < discussions.size(); i++) {
	    WorkspaceElementNode n = new 
		WorkspaceElementNode(WorkspaceElement.DISCUSSION,
				     (WorkspaceElement)
				     discussions.elementAt(i));
	    n.x = (i + 0.5) * this.getWidth() / discussions.size();
	    n.y = 2.5 * this.getHeight() / 4;
	    n.setVisible(discussionVisible);
	    nodes.addElement(n);
	}

	Vector documents = node.getDocuments();
	for (int i = 0; i < documents.size(); i++) {
	    WorkspaceElementNode n = new 
		WorkspaceElementNode(WorkspaceElement.DOCUMENT,
				     (WorkspaceElement)
				     documents.elementAt(i));
	    n.x = (i + 0.5) * this.getWidth() / documents.size();
	    n.y = 2.5 * this.getHeight() / 4;
	    n.setVisible(documentVisible);
	    nodes.addElement(n);
	}

	Vector messagerules = node.getSrcMessageRules();
	for (int i = 0; i < messagerules.size(); i++) {
	    WorkspaceElementNode n = new 
		WorkspaceElementNode(WorkspaceElement.MESSAGERULE,
				     (WorkspaceElement)
				     messagerules.elementAt(i));
	    n.x = (i + 0.5) * this.getWidth() / messagerules.size();
	    n.y = 3.5 * this.getHeight() / 4;
	    n.setVisible(messageRuleVisible);
	    nodes.addElement(n);
	}

	Vector messagetypes = node.getMessageTypes();
	for (int i = 0; i < messagetypes.size(); i++) {
	    WorkspaceElementNode n = new 
		WorkspaceElementNode(WorkspaceElement.MESSAGETYPE,
				     (WorkspaceElement)
				     messagetypes.elementAt(i));
	    n.x = (i + 0.5) * this.getWidth() / messagetypes.size();
	    n.y = 3.5 * this.getHeight() / 4;
	    n.setVisible(messageTypeVisible);
	    nodes.addElement(n);
	}
    }


    void initializeEdges(WorkspaceNode node)
    {
	Vector roles = node.getRoles();
	for (int i = 0; i < roles.size(); i++) {
	    Role role = (Role) roles.elementAt(i);
	    WorkspaceElementNode roleNode = getRoleNode(role);

	    // add edges between participants and roles
	    Vector participants = role.participants;
	    for (int j = 0; j < participants.size(); j++) {
		WorkspaceElementEdge e = new
		    WorkspaceElementEdge(getParticipantNode((Participant)
					 participants.elementAt(j)), roleNode);
		edges.addElement(e);
	    }

	    // add edges between roles and objects
	    Vector objects = role.assignedObjects;
	    for (int j = 0; j < objects.size(); j++) {
		WorkspaceElementEdge e = new
		    WorkspaceElementEdge(roleNode,
					 getObjectNode((WorkspaceObject)
						       objects.elementAt(j)));
		edges.addElement(e);
	    }
	}
    }


    WorkspaceElementNode getRoleNode(Role role)
    {
	for (int i = 0; i < nodes.size(); i++) {
	    WorkspaceElementNode node =
		(WorkspaceElementNode) nodes.elementAt(i);
	    if (node.type == WorkspaceElement.ROLE)
		if (node.element.equals(role))
		    return node;
	}
	return null;
    }


    WorkspaceElementNode getParticipantNode(Participant participant)
    {
	for (int i = 0; i < nodes.size(); i++) {
	    WorkspaceElementNode node =
		(WorkspaceElementNode) nodes.elementAt(i);
	    if (node.type == WorkspaceElement.PARTICIPANT)
		if (node.element.equals(participant))
		    return node;
	}
	return null;
    }


    WorkspaceElementNode getObjectNode(WorkspaceObject object)
    {
	for (int i = 0; i < nodes.size(); i++) {
	    WorkspaceElementNode node =
		(WorkspaceElementNode) nodes.elementAt(i);
	    if (node.element instanceof WorkspaceObject)
		if (node.element.equals(object))
		    return node;
	}
	return null;
    }


    Color getNodeColour(WorkspaceElementNode node)
    {
	Color paintColour = nodeColour;

	switch (node.type) {
	case WorkspaceElement.ROLE :
	    paintColour = roleColour;
	    break;
	case WorkspaceElement.PARTICIPANT :
	    paintColour = participantColour;
	    break;
	case WorkspaceElement.ACTION :
	    paintColour = actionColour;
	    break;
	case WorkspaceElement.DISCUSSION :
	    paintColour = discussionColour;
	    break;
	case WorkspaceElement.DOCUMENT :
	    paintColour = documentColour;
	    break;
	case WorkspaceElement.MESSAGERULE :
	    paintColour = messageRuleColour;
	    break;
	case WorkspaceElement.MESSAGETYPE :
	    paintColour = messageTypeColour;
	    break;
	}
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
	    WorkspaceElementEdge e = (WorkspaceElementEdge) edges.elementAt(i);

	    if (e == null)
		continue;
	    else if (e.fromNode == null || e.toNode == null)
		continue;
	    else if (! (e.fromNode.isVisible() && e.toNode.isVisible()))
		continue;

	    int x1 = (int) e.fromNode.x;
	    int y1 = (int) e.fromNode.y;
	    int x2 = (int) e.toNode.x;
	    int y2 = (int) e.toNode.y;

	    offgraphics.setColor(Color.black);

	    // len = difference of actual from-to distance to nominal distance 
	    // int len = (int) Math.abs(Math.sqrt((x1-x2)*(x1-x2) +
	    //                          (y1-y2)*(y1-y2)) - e.length);

	    offgraphics.drawLine(x1, y1, x2, y2);
	}

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

	if (e.isPopupTrigger() && pick == null)
	    processPopupClick(e.getX(), e.getY());

	repaint();
	e.consume();
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

	// shift-right mouse button click:
	// detailed map of workspace internals
	else if (((e.getModifiers() & InputEvent.BUTTON3_MASK) ==
		  InputEvent.BUTTON3_MASK) && e.isShiftDown() &&
		 (pick != null)) {
	    if (pick.type == WorkspaceElement.DISCUSSION) {
		DiscussionMap map = new DiscussionMap(con, pick, controls);
		map.setVisible(true);
	    }
	}

	pick = null;
	repaint();
	e.consume();
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
	    roleMI = new JCheckBoxMenuItem("Roles", roleVisible);
	    roleMI.addItemListener(this);
	    popup.add(roleMI);

	    participantMI =
		new JCheckBoxMenuItem("Participants", participantVisible);
	    participantMI.addItemListener(this);
	    popup.add(participantMI);

	    actionMI = new JCheckBoxMenuItem("Actions", actionVisible);
	    actionMI.addItemListener(this);
	    popup.add(actionMI);

	    discussionMI =
		new JCheckBoxMenuItem("Discussions", discussionVisible);
	    discussionMI.addItemListener(this);
	    popup.add(discussionMI);

	    documentMI =
		new JCheckBoxMenuItem("Documents", documentVisible);
	    documentMI.addItemListener(this);
	    popup.add(documentMI);

	    messageRuleMI =
		new JCheckBoxMenuItem("Message Rules", messageRuleVisible);
	    messageRuleMI.addItemListener(this);
	    popup.add(messageRuleMI);

	    messageTypeMI =
		new JCheckBoxMenuItem("Message Types", messageTypeVisible);
	    messageTypeMI.addItemListener(this);
	    popup.add(messageTypeMI);

	    popup.add(new JSeparator());

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

	if (source == roleMI) {
	    roleVisible = state;
	    changeNodeVisibility(WorkspaceElement.ROLE, state);
	}
	else if (source == participantMI) {
	    participantVisible = state;
	    changeNodeVisibility(WorkspaceElement.PARTICIPANT, state);
	}
	else if (source == actionMI) {
	    actionVisible = state;
	    changeNodeVisibility(WorkspaceElement.ACTION, state);
	}
	else if (source == discussionMI) {
	    discussionVisible = state;
	    changeNodeVisibility(WorkspaceElement.DISCUSSION, state);
	}
	else if (source == documentMI) {
	    documentVisible = state;
	    changeNodeVisibility(WorkspaceElement.DOCUMENT, state);
	}
	else if (source == messageRuleMI) {
	    messageRuleVisible = state;
	    changeNodeVisibility(WorkspaceElement.MESSAGERULE, state);
	}
	else if (source == messageTypeMI) {
	    messageTypeVisible = state;
	    changeNodeVisibility(WorkspaceElement.MESSAGETYPE, state);
	}
	else if (source == shortLabelMI) {
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
