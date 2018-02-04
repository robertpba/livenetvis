package livenet.analysis;

/*
 * The following code is based on original code from Sun Microsystems,
 * which has been heavily modified and extended. Sun's original code
 * now accounts for only about 20% of the total code of this class. To
 * acknowledge the source of the original code, Sun's copyright notice
 * is preserved below.
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


public class TreeGraphPanel extends JPanel implements Runnable, Printable,
    MouseListener, MouseMotionListener, TreeGraphConstants, ControlListener
{
    // set to true to get debugging output
    private final boolean DEBUG = false;

    MapControls controls;

    // The root node of the workspace tree
    WorkspaceNode rootNode;

    Vector nodes = new Vector();
    Vector edges = new Vector();

    Thread relaxer;

    // Number of times the relax() method has been called since the
    // last user action
    long iterations = 0;

    // Maximum number of iterations to recalculate spring forces after
    // the last user action
    long maxIterations = 20;

    // Have major changes to node positions occurred in the last loop?
    boolean noMajorChanges;

    // Fraction of edgelength that constitutes a major change
    double majorChangeFraction = 0.02;

    // Distance from top edge to start placing nodes
    int topOffset = 150;
    
    boolean stress = false;

    // The node being collapsed
    WorkspaceNode collapsedNode;

    // The node clicked on
    WorkspaceNode pick;

    // Is the pick node fixed?
    boolean pickfixed;

    // The node focused on
    WorkspaceNode focusNode;

    // A fixed node
    WorkspaceNode fixedNode;

    // An offscreen image to draw on
    Image offscreen;

    // Size of the offscreen image
    Dimension offscreensize;

    // Graphics object of the offscreen image
    Graphics offgraphics;

    boolean printing = false;
    Font nodeFont = new Font("SansSerif", Font.PLAIN, 10);

    // node colouring modes
    public static final int PARENT_CHILD = 0;
    public static final int ABSOLUTE_WORKSPACE_DENSITY = 1;
    public static final int MINIMUM_WORKSPACE_DENSITY = 2;
    public static final int MAXIMUM_WORKSPACE_DENSITY = 3;
    public static final int MEAN_WORKSPACE_DENSITY = 4;
    public static final int EVOLUTION_INTENSITY = 5;
    public static final int EVOLUTION_RECENCY = 6;
    public static final int MESSAGE_INTENSITY = 7;
    public static final int MESSAGE_RECENCY = 8;

    int nodeColouring = PARENT_CHILD;
    int oldNodeColouring = PARENT_CHILD;

    // values used for node colouring
    Histogram densityHist;
    FloatHistogram evolIntensityHist;
    FloatHistogram evolRecencyHist;
    FloatHistogram msgIntensityHist;
    FloatHistogram msgRecencyHist;

    Color[] densityColours;
    Color[] evolIntensityColours;
    Color[] evolRecencyColours;
    Color[] msgIntensityColours;
    Color[] msgRecencyColours;

    // edge pulling force
    boolean childEdgeForce = true;
    boolean goalEdgeForce = false;
    boolean actionEdgeForce = false;
    boolean discussionEdgeForce = false;
    boolean documentEdgeForce = false;
    boolean messageRuleEdgeForce = false;
    boolean participantEdgeForce = false;

    // edge visibility
    boolean childEdgeVisible = true;
    boolean goalEdgeVisible = false;
    boolean actionEdgeVisible = false;
    boolean discussionEdgeVisible = false;
    boolean documentEdgeVisible = false;
    boolean messageRuleEdgeVisible = false;
    boolean participantEdgeVisible = false;

    // scope of edge visibility changes
    int edgeScope = MapControls.EDGE_VIS_SUBGRAPH;

    // edge length
    int edgeLength = initialEdgeLength;

    // legend stuff
    ColourLegend legend;
    Point legendLocation;
    Dimension legendSize;

    // database connection object
    Connection con;


    public TreeGraphPanel(Connection con, WorkspaceNode rootNode,
			  MapControls controls)
    {
	// disable double buffering
	super(false);

	this.setOpaque(true);
	this.setFont(nodeFont);

	this.con = con;

	addMouseListener(this);

	Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
	this.setSize((int) (screenSize.width * 0.9),
		     (int) (screenSize.height * 0.9));

	initializeNodes(rootNode, 0);
	initializeEdges(rootNode);
	rootNode.setFixed(true);
	rootNode.setVisible(true);
	this.rootNode = rootNode;
	this.fixedNode = rootNode;

	this.controls = controls;
	controls.addControlListener(this);

	calculateStatistics();
    }


    void initializeNodes(WorkspaceNode node, int level)
    {
	node.x = this.getWidth() / 2;
	node.y = topOffset + (level * edgeLength);
	nodes.addElement(node);
	for (int i = 0; i < node.getChildCount(); i++) {
	    initializeNodes(node.getChild(i), level + 1);
	}
    }


    void initializeEdges(WorkspaceNode node)
    {
	// add edges for all linked workspaces
	for (int i = 0; i < node.getWorkspaceLinkCount(); i++) {
	    edges.addElement(node.getWorkspaceLink(i));
	}

	// recursively add edges for the children
	for (int i = 0; i < node.getChildCount(); i++) {
	    initializeEdges(node.getChild(i));
	}
    }


    public void reInitializeEdges()
    {
	edges.removeAllElements();
	initializeEdges(rootNode);
    }


    void calculateStatistics()
    {
	calculateDensities();
	calculateEvolutionIntensities();
	calculateEvolutionRecencies();
	calculateMessageIntensities();
	calculateMessageRecencies();
    }


    void calculateDensities()
    {
	densityHist = new Histogram(nodes.size() * 4);

	for (int i = 0; i < nodes.size(); i++) {
	    WorkspaceNode node = (WorkspaceNode) nodes.elementAt(i);
	    if (node.isSpecial())
		continue;
	    densityHist.put(node.getAbsDensity());
	    densityHist.put(node.getMinRoleDensity());
	    densityHist.put(node.getMaxRoleDensity());
	    densityHist.put((int) node.getMeanRoleDensity());
	}

	int entries = densityHist.getSize();

	if (DEBUG)
	    System.out.println("Original number of entries is " + entries);

	if (entries > 256)
	    entries = 256;
	densityColours = new Color[entries];

	double redInterval = 0.0;
	double greenInterval = 0.0;

	if (DEBUG)
	    System.out.println("Corrected number of entries is " + entries);

	if (entries > 1) {
	    redInterval = (double) 255 / ((double) ((entries - 1) / 3));
	    greenInterval = (double) 255 / ((double) (2 * (entries - 1) / 3));
	}

	if (DEBUG) {
	    System.out.println("Red Interval is " + redInterval);
	    System.out.println("Green Interval is " + greenInterval);
	}

	// from pure green (0,255,0) to pure yellow (255,255,0)
	for (int i = 0; i < (int) (entries / 3); i++) {
	    int red = (int) (i * redInterval);
	    if (DEBUG)
		System.out.print("i=" + i + "  Original red is " + red);
	    red = red < 0 ? 0 : red;
	    red = red > 255 ? 255 : red;
	    if (DEBUG)
		System.out.println(", corrected red is " + red);
	    densityColours[i] = new Color(red, 255 , 0);
	}
	// from pure yellow (255,255,0) to pure red (255,0,0)
	for (int i = (int) (entries / 3); i < entries; i++) {
	    int green = (int) (255 - (((((i + 1) - (entries / 3.0))) - 1)
			       * greenInterval));
	    if (DEBUG)
		System.out.print("i=" + i + "  Original green is " + green);
	    green = green < 0 ? 0 : green;
	    green = green > 255 ? 255 : green;
	    if (DEBUG)
		System.out.println(", corrected green is " + green);
	    densityColours[i] = new Color(255, green , 0);
	}
    }


    void calculateEvolutionIntensities()
    {
	evolIntensityHist = new FloatHistogram(nodes.size());

	for (int i = 0; i < nodes.size(); i++) {
	    WorkspaceNode node = (WorkspaceNode) nodes.elementAt(i);
	    if (node.isSpecial())
		continue;
	    evolIntensityHist.put(node.getEvolutionIntensity());
	}

	int entries = evolIntensityHist.getSize();
	if (entries > 256)
	    entries = 256;
	evolIntensityColours = new Color[entries];

	int interval = 0;

	if (entries > 1)
	    interval = (int) (255 / (entries - 1));

	for (int i = 0; i < entries; i++) {
	    int green = (int) (255 - (i * interval));
	    evolIntensityColours[i] = new Color(255, green , 0);
	}
    }


    void calculateEvolutionRecencies()
    {
	evolRecencyHist = new FloatHistogram(nodes.size());

	for (int i = 0; i < nodes.size(); i++) {
	    WorkspaceNode node = (WorkspaceNode) nodes.elementAt(i);
	    if (node.isSpecial())
		continue;
	    evolRecencyHist.put(node.getEvolutionRecency());
	}

	int entries = evolRecencyHist.getSize();
	if (entries > 256)
	    entries = 256;
	evolRecencyColours = new Color[entries];

	int interval = 0;

	if (entries > 1)
	    interval = (int) (255 / (entries - 1));

	for (int i = 0; i < entries; i++) {
	    int green = (int) (255 - (i * interval));
	    evolRecencyColours[i] = new Color(255, green , 0);
	}
    }


    void calculateMessageIntensities()
    {
	msgIntensityHist = new FloatHistogram(nodes.size());

	for (int i = 0; i < nodes.size(); i++) {
	    WorkspaceNode node = (WorkspaceNode) nodes.elementAt(i);
	    if (node.isSpecial())
		continue;
	    msgIntensityHist.put(node.getMessageIntensity());
	}

	int entries = msgIntensityHist.getSize();
	if (entries > 256)
	    entries = 256;
	msgIntensityColours = new Color[entries];

	int interval = 0;

	if (entries > 1)
	    interval = (int) (255 / (entries - 1));

	for (int i = 0; i < entries; i++) {
	    int green = (int) (255 - (i * interval));
	    msgIntensityColours[i] = new Color(255, green , 0);
	}
    }


    void calculateMessageRecencies()
    {
	msgRecencyHist = new FloatHistogram(nodes.size());

	for (int i = 0; i < nodes.size(); i++) {
	    WorkspaceNode node = (WorkspaceNode) nodes.elementAt(i);
	    if (node.isSpecial())
		continue;
	    msgRecencyHist.put(node.getMessageRecency());
	}

	int entries = msgRecencyHist.getSize();
	if (entries > 256)
	    entries = 256;
	msgRecencyColours = new Color[entries];

	int interval = 0;

	if (entries > 1)
	    interval = (int) (255 / (entries - 1));

	for (int i = 0; i < entries; i++) {
	    int green = (int) (255 - (i * interval));
	    msgRecencyColours[i] = new Color(255, green , 0);
	}
    }


    private void setLongLabels(WorkspaceNode node)
    {
	node.setLongDisplayName();
	for (int i = 0; i < node.getChildCount(); i++)
	    setLongLabels(node.getChild(i));
    }


    private void setShortLabels(WorkspaceNode node)
    {
	node.setShortDisplayName();
	for (int i = 0; i < node.getChildCount(); i++)
	    setShortLabels(node.getChild(i));
    }


    void setEdgeLength(int length)
    {
	edgeLength = length;
	for (int i = 0; i < edges.size(); i++)
	    ((WorkspaceLink) edges.elementAt(i)).length = length;
    }


    WorkspaceNode findNode(String shortName)
    {
	WorkspaceNode node;
	for (int i = 0 ; i < nodes.size() ; i++) {
	    node = (WorkspaceNode) nodes.elementAt(i);
	    if (node.shortName.equals(shortName))
		return node;
	}
	return null;
    }


    Color getNodeColour(WorkspaceNode node)
    {
	if (node.isSpecial())
	    return specialColour;

	Color paintColour = nodeColour;

	try {
	    switch (nodeColouring) {
	    case PARENT_CHILD :
// 		if (node == pick)
// 		    paintColour = selectColour;
// 		else if (node.isFixed())
// 		    paintColour = fixedColour;
		if (node.isLeaf())
		    paintColour = leafColour;
		else
		    paintColour = nodeColour;
		break;
		
	    case ABSOLUTE_WORKSPACE_DENSITY :
		paintColour = densityColours[densityHist.
					     getIndex(node.getAbsDensity())];
		break;

	    case MINIMUM_WORKSPACE_DENSITY :
		paintColour = densityColours[densityHist.
					   getIndex(node.getMinRoleDensity())];
		break;

	    case MAXIMUM_WORKSPACE_DENSITY :
		paintColour = densityColours[densityHist.
					   getIndex(node.getMaxRoleDensity())];
		break;

	    case MEAN_WORKSPACE_DENSITY :
		paintColour = densityColours[densityHist.getIndex((int)
					     node.getMeanRoleDensity())];
		break;

	    case EVOLUTION_INTENSITY :
		paintColour = evolIntensityColours[evolIntensityHist.getIndex(
					        node.getEvolutionIntensity())];
		break;

	    case EVOLUTION_RECENCY :
		paintColour = evolRecencyColours[evolRecencyHist.getIndex(
					         node.getEvolutionRecency())];
		break;

	    case MESSAGE_INTENSITY :
		paintColour = msgIntensityColours[msgIntensityHist.getIndex(
					          node.getMessageIntensity())];
		break;

	    case MESSAGE_RECENCY :
		paintColour = msgRecencyColours[msgRecencyHist.getIndex(
						node.getMessageRecency())];
		break;
	    }
	}
	catch (Exception e) {
	    System.out.println(e.getMessage());
	}

	return paintColour;
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
	if (++iterations > maxIterations && noMajorChanges)
	    return;

	// recalculate spring force
	for (int i = 0 ; i < edges.size() ; i++) {
	    WorkspaceLink e = (WorkspaceLink) edges.elementAt(i);
	    if (! (e.fromNode.isVisible() && e.toNode.isVisible()) ||
		(e.type == WorkspaceLink.CHILD && ! childEdgeForce) ||
		(e.type == WorkspaceLink.GOAL && ! goalEdgeForce) ||
		(e.type == WorkspaceLink.ACTION && ! actionEdgeForce) ||
		(e.type == WorkspaceLink.DISCUSSION &&
		 ! discussionEdgeForce) ||
		(e.type == WorkspaceLink.DOCUMENT && ! documentEdgeForce) ||
		(e.type == WorkspaceLink.MESSAGERULE &&
		 ! messageRuleEdgeForce) ||
		(e.type == WorkspaceLink.PARTICIPANT &&
		 ! participantEdgeForce))
		continue;
	    double vx = e.toNode.x - e.fromNode.x;
	    double vy = e.toNode.y - e.fromNode.y;
	    double len = Math.sqrt(vx * vx + vy * vy);
            len = (len == 0) ? .0001 : len;

	    int springElasticity = 10;

	    // edges connecting two expanded parent nodes should be
	    // more stretchy
	    if (! e.fromNode.isLeaf() && ! e.fromNode.isVisibleLeaf() &&
		! e.toNode.isLeaf() && ! e.toNode.isVisibleLeaf())
		springElasticity *= 5;

	    //double f = (e.length - len) / (len * 3);
	    double f = (e.length - len) / (len * springElasticity);
	    double dx = f * vx;
	    double dy = f * vy;

	    e.toNode.dx += dx;
	    e.toNode.dy += dy;
	    e.fromNode.dx += -dx;
	    e.fromNode.dy += -dy;
	}

	// recalculate repulsion force
	for (int i = 0 ; i < nodes.size() ; i++) {
	    WorkspaceNode n1 = (WorkspaceNode) nodes.elementAt(i);
	    if (! n1.isVisible())
		continue;
	    double dx = 0;
	    double dy = 0;

	    for (int j = 0 ; j < nodes.size() ; j++) {

		if (i == j)
		    continue;

		WorkspaceNode n2 = (WorkspaceNode) nodes.elementAt(j);

		if (! n2.isVisible())
		    continue;

		double vx = n1.x - n2.x;
		double vy = n1.y - n2.y;
		double len = vx * vx + vy * vy;
		if (len == 0) {
		    dx += Math.random();
		    dy += Math.random();
		} else if (len < edgeLength * edgeLength) {
		    // try: double deltas
		    dx += 2 * vx / len;
		    dy += 2 * vy / len;
		}
	    }
	    double dlen = dx * dx + dy * dy;
	    if (dlen > 0) {
		dlen = Math.sqrt(dlen) / 2;
		n1.dx += dx / dlen;
		n1.dy += dy / dlen;
	    }
	}

	Dimension d = this.getSize();
	double maxDx = 0.0;
	double maxDy = 0.0;

	for (int i = 0 ; i < nodes.size() ; i++) {
	    WorkspaceNode n = (WorkspaceNode) nodes.elementAt(i);

	    if (! n.isVisible())
		continue;

	    if (n.dx > maxDx)
		maxDx = n.dx;

	    if (n.dy > maxDy)
		maxDy = n.dy;

	    // If it's a movable node, move it a few pixels according to dx, dy
	    if (! n.fixed) {
		// the move should be in the range -5..5
		n.x += Math.max(-5, Math.min(5, n.dx));
		n.y += Math.max(-5, Math.min(5, n.dy));
            }

// 	    // If the node is too far above its parent, move it down
// 	    if (childEdgeForce && ! (n.getParent() == null))
// 		if ((n.getParent().isVisible()) &&
// 		    (n.y < (n.getParent().y - (0.33 * edgeLength))) )
// 		    n.y = n.getParent().y - (0.33 * edgeLength);

	    // If the node is (partly) outside the panel, move it back in
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

	// if there hasn't been significant movement in the graph
	// disable animation
	noMajorChanges = ((maxDx < edgeLength * majorChangeFraction) &&
			  (maxDy < edgeLength * majorChangeFraction));
    }


    /**
       Draw a node in the panel.
    **/
    public void paintNode(Graphics g, WorkspaceNode n, FontMetrics fm)
    {
	int x = (int) n.x;
	int y = (int) n.y;

	// select the background colour of the node
	Color paintColour = getNodeColour(n);
	g.setColor(paintColour);

	// horizontal and vertical padding
	int padX = fm.getMaxAdvance() / 2;
	if (padX < 1)
	    padX = 10;
	int padY = (int) ((double) fm.getHeight() * 0.1);
	// make sure the node box is big enough for all edge types to
	// lie within its boundary
	if (fm.getHeight() + padY < 17)
	    padY = 17 - fm.getHeight();

	// width and height of the box
	int w = fm.stringWidth(n.displayName) + padX;
	int h = fm.getHeight() + padY;

	// coordinates of the box's top left and bottom right corner
	n.x1 = x - w / 2;
	n.y1 = y - h / 2;
	n.x2 = n.x1 + w - 1;
	n.y2 = n.y1 + h - 1;

	// draw the box, its border and the text label inside it
	g.fillRect(n.x1, n.y1, w, h);
	g.setColor(Color.black);
	g.drawRect(n.x1, n.y1, w - 1, h - 1);
	if (n.hasChildren())
	    g.drawRect(n.x1 - 1, n.y1 - 1, w + 1, h + 1);
	g.drawString(n.displayName, x - (w - padX) / 2,
		     (y - (h - padY) / 2) + fm.getAscent());

	// draw some little brackets outside the corners of the focus node
	if (n.hasFocus()) {
	    int offset = 0;
	    if (n.hasChildren())
		offset = 1;
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
	Dimension d = getSize();

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
	    offgraphics.setColor(getBackground());
	offgraphics.fillRect(0, 0, d.width, d.height);

	// redraw all edges, giving them the appropriate colour
	for (int i = 0 ; i < edges.size() ; i++) {
	    WorkspaceLink e = (WorkspaceLink) edges.elementAt(i);

	    if (! e.visible)
		continue;

	    int x1 = (int) e.fromNode.x;
	    int y1 = (int) e.fromNode.y;
	    int x2 = (int) e.toNode.x;
	    int y2 = (int) e.toNode.y;

	    switch (e.type) {
	    case WorkspaceLink.CHILD :
		if (childEdgeVisible)
		    offgraphics.setColor(childColour);
		else
		    continue;
		break;
	    case WorkspaceLink.GOAL :
		x1 -= 6;
		y1 -= 6;
		x2 -= 6;
		y2 -= 6;
		if (goalEdgeVisible)
		    offgraphics.setColor(goalColour);
		else
		    continue;
		break;
	    case WorkspaceLink.ACTION :
		x1 -= 4;
		y1 -= 4;
		x2 -= 4;
		y2 -= 4;
		if (actionEdgeVisible)
		    offgraphics.setColor(actionColour);
		else
		    continue;
		break;
	    case WorkspaceLink.DISCUSSION :
		x1 -= 2;
		y1 -= 2;
		x2 -= 2;
		y2 -= 2;
		if (discussionEdgeVisible)
		    offgraphics.setColor(discussionColour);
		else
		    continue;
		break;
	    case WorkspaceLink.DOCUMENT :
		x1 += 2;
		y1 += 2;
		x2 += 2;
		y2 += 2;
		if (documentEdgeVisible)
		    offgraphics.setColor(documentColour);
		else
		    continue;
		break;
	    case WorkspaceLink.PARTICIPANT :
		x1 += 4;
		y1 += 4;
		x2 += 4;
		y2 += 4;
		if (participantEdgeVisible)
		    offgraphics.setColor(participantColour);
		else
		    continue;
		break;
	    case WorkspaceLink.MESSAGERULE :
		x1 += 6;
		y1 += 6;
		x2 += 6;
		y2 += 6;
		if (messageRuleEdgeVisible)
		    offgraphics.setColor(messageRuleColour);
		else
		    continue;
		break;
	    default:
		offgraphics.setColor(otherColour);
		break;
	    }

	    // len = difference of actual from-to distance to nominal distance 
	    // int len = (int) Math.abs(Math.sqrt((x1-x2)*(x1-x2) +
	    //                          (y1-y2)*(y1-y2)) - e.length);

	    offgraphics.drawLine(x1, y1, x2, y2);

	    // add a text label of weight to the edge if the weight is
	    // > 1 or if this is a non-child and non-goal edge
	    // note: the e.type comparison was added on 1/2/2001
	    if (e.weight > 1 || (e.type != WorkspaceLink.CHILD &&
		e.type != WorkspaceLink.GOAL)) {
		String label = String.valueOf(e.weight);
		// offgraphics.setColor(stressColour);
		offgraphics.drawString(label, x1 + (x2-x1)/2, y1 + (y2-y1)/2);
		offgraphics.setColor(edgeColour);
	    }
	}

	// redraw all nodes
	FontMetrics fm = offgraphics.getFontMetrics();
	for (int i = 0 ; i < nodes.size() ; i++) {
	    WorkspaceNode n = (WorkspaceNode) nodes.elementAt(i);
	    if (n.isVisible())
		paintNode(offgraphics, n, fm);
	}

	g.drawImage(offscreen, 0, 0, null);
    }


    /**
       Focus on one node. This implies the hiding of all nodes other
       than those which are directly connected to the focus
       nodes. Invocation of this method will cause node visibilities
       to be set appropriately.
    **/
    public void focusOn(WorkspaceNode node)
    {
	// make all (normal) nodes invisible, non-fixed and unfocused
	for (int i = 0; i < nodes.size(); i++) {
	    WorkspaceNode n = (WorkspaceNode) nodes.elementAt(i);
	    if (! n.isSpecial())
		n.setFixed(false);
	    n.setFocus(false);
	    n.setVisible(false);

	}

	// make all edges invisible
	for (int i = 0; i < edges.size(); i++) {
	    ((WorkspaceLink) edges.elementAt(i)).visible = false;
	}

	// set the focus node to be fixed, focused and visible
	node.setFixed(true);
	node.setFocus(true);
	node.setVisible(true);
	fixedNode = node;

	// set the focus node's parent to be fixed too, to prevent the
	// focus node from being pulled down by a "heavy" parent (one
	// which has many other nodes hanging onto it)
	WorkspaceNode parent = node.getParent();
	if (parent != null)
	    parent.setFixed(true);

	// set the edges and nodes connected to the focus node to be
	// visible, subject to the edge type being visible
	expandNode(node);
    }


    public void focusOff(WorkspaceNode node)
    {
	WorkspaceNode parentNode = node.getParent();
	if (parentNode != null) {
	    parentNode.setFixed(true);
	    fixedNode = parentNode;
	    if (! node.isSpecial())
		node.setFixed(false);
	}
	node.setFocus(false);
	focusNode = null;
    }


    public void expandNode(WorkspaceNode node)
    {
	// set the edges and nodes connected to the expanded node to be
	// visible, subject to the edge type being visible
	expandVisibleEdges(node);

	if (childEdgeVisible) {
	    // setting for single node under root node
	    double parentAngle = 0.0;

	    Dimension size = this.getSize();
	    if (node.getParent() != null) {
		// Adjust the children's coordinates to lie on a line
		// continuing through the parent node's edge to the
		// grandparent
		double vectorX = node.x - node.getParent().x;
		double vectorY = node.y - node.getParent().y;
		double vectorLength =
		    Math.sqrt(vectorX * vectorX + vectorY * vectorY);

		// Calculate angle of vector relative to the vertical axis
		parentAngle = Math.asin(-vectorX / vectorLength);

		// Correct angle if node is above parent node
		if (vectorY < 0) {
		    if (parentAngle < 0.0)
			parentAngle = -Math.PI - parentAngle;
		    else
			parentAngle = Math.PI - parentAngle;
		}
	    }

	    int numChildren = node.getChildCount();

	    // Calculate the spread angle of child nodes
	    double spread = 0.0;
	    if (numChildren > 1 && numChildren < 5)
		spread = numChildren * Math.PI / 4.0;
	    else if (numChildren >= 5)
		spread = Math.PI;

	    // Place child nodes at calculated positions
	    for (int i = 0; i < numChildren; i++) {
		WorkspaceNode childNode = node.getChild(i);

		// angle at which to place child node
		double childAngle = parentAngle + (spread / 2);
		if (numChildren > 1)
		    childAngle -= i * spread / (numChildren - 1);

		childNode.x = node.x - (edgeLength * Math.sin(childAngle));
		childNode.y = node.y + (edgeLength * Math.cos(childAngle));
	    }
	}
    }


    private void expandTree(WorkspaceNode node)
    {
	expandNode(node);
	for (int i = 0; i < node.getChildCount(); i++)
	    expandTree(node.getChild(i));

    }


    public void collapseNode(WorkspaceNode node)
    {
	collapsedNode = node;
	node.collapseDescendents();
	revalidateEdgeVisibility();
	revalidateNodeVisibility();
	collapsedNode = null;
    }


    private void expandVisibleEdges(WorkspaceNode node)
    {
	for (int i = 0; i < node.getWorkspaceLinkCount(); i++) {
	    WorkspaceLink edge = node.getWorkspaceLink(i);
	    WorkspaceNode otherNode =
		(edge.fromNode == node) ? edge.toNode : edge.fromNode;

	    // for each edge, set the edge and connecting node to
	    // visible if that type of edge has visibility turned on
	    switch (edge.type) {
	    case WorkspaceLink.CHILD :
		if (childEdgeVisible) {
		    edge.visible = true;
		    otherNode.setVisible(true);
		}
		break;
	    case WorkspaceLink.GOAL :
		if (goalEdgeVisible) {
		    edge.visible = true;
		    otherNode.setVisible(true);
		}
		break;
	    case WorkspaceLink.ACTION :
		if (actionEdgeVisible) {
		    edge.visible = true;
		    otherNode.setVisible(true);
		}
		break;
	    case WorkspaceLink.DISCUSSION :
		if (discussionEdgeVisible) {
		    edge.visible = true;
		    otherNode.setVisible(true);
		}
		break;
	    case WorkspaceLink.DOCUMENT :
		if (documentEdgeVisible) {
		    edge.visible = true;
		    otherNode.setVisible(true);
		}
		break;
	    case WorkspaceLink.MESSAGERULE :
		if (messageRuleEdgeVisible) {
		    edge.visible = true;
		    otherNode.setVisible(true);
		}
		break;
	    case WorkspaceLink.PARTICIPANT :
		if (participantEdgeVisible) {
		    edge.visible = true;
		    otherNode.setVisible(true);
		}
		break;
	    }
	}
    }


    /**
       Turn edge visibility of the specified type on or off, and
       correct node visibility accordingly.
     **/
    public void changeEdgeVisibility(int type, boolean state)
    {
	if (state == true) {

	    // apply edge type visibility change only to visible subgraph;
	    // make edges visible if both from and to node are visible
	    if (edgeScope == MapControls.EDGE_VIS_SUBGRAPH) {
		for (int i = 0; i < edges.size(); i++) {
		    WorkspaceLink edge = (WorkspaceLink) edges.elementAt(i);
		    if (edge.type == type && edge.fromNode.isVisible() &&
			edge.toNode.isVisible())
			edge.visible = true;
		}
	    }

	    // apply edge type visibility change to entire graph; this
	    // needs to be done in two steps:
	    // first make edges visible if either from or to node are visible,
	    // then make edges visible if both from and to node are visible;
	    // the second step is necessary as the first step may make
	    // pairs of nodes visible which have an edge between them
	    else if (edgeScope == MapControls.EDGE_VIS_WHOLEGRAPH) {
		for (int i = 0; i < edges.size(); i++) {
		    WorkspaceLink edge = (WorkspaceLink) edges.elementAt(i);
		    if (edge.type == type && edge.visible == false &&
			(edge.fromNode.isVisible() ||
			 edge.toNode.isVisible())) {
			edge.fromNode.setVisible(true);
			edge.toNode.setVisible(true);
			edge.visible = true;
		    }
		}

		for (int i = 0; i < edges.size(); i++) {
		    WorkspaceLink edge = (WorkspaceLink) edges.elementAt(i);
		    if (edge.type == type && edge.visible == false &&
			edge.fromNode.isVisible() && edge.toNode.isVisible())
			edge.visible = true;
		}
	    }
	} else {
	    // turn edge visibility unconditionally off (for edges of
	    // the specified type)
	    for (int i = 0; i < edges.size(); i++) {
		WorkspaceLink edge = (WorkspaceLink) edges.elementAt(i);
		if (edge.type == type)
		    edge.visible = false;
	    }

	    // cleanup:
	    // after edge visibility has been turned off, there may be some
	    // nodes without connecting edges; these need to be made
	    // invisible, except for the root node and focus node (if any)
	    if (state == false)
		revalidateNodeVisibility();
	}
    }


    /**
       Turns edge visibility on if and only if both nodes linked by an
       edge are visible, and visibility is turned on for the type of
       edge, otherwise edge visibility is turned off.
    **/
    private void revalidateEdgeVisibility()
    {
	for (int i = 0; i < edges.size(); i++) {
	    WorkspaceLink edge = (WorkspaceLink) edges.elementAt(i);
	    if (edge.fromNode.isVisible() && edge.toNode.isVisible() &&
		((edge.type == WorkspaceLink.CHILD && childEdgeVisible) ||
		 (edge.type == WorkspaceLink.GOAL && goalEdgeVisible) ||
		 (edge.type == WorkspaceLink.ACTION && actionEdgeVisible) ||
		 (edge.type == WorkspaceLink.DISCUSSION &&
		  discussionEdgeVisible) ||
		 (edge.type == WorkspaceLink.DOCUMENT &&
		  documentEdgeVisible) ||
		 (edge.type == WorkspaceLink.MESSAGERULE &&
		  messageRuleEdgeVisible) ||
		 (edge.type == WorkspaceLink.PARTICIPANT &&
		  participantEdgeVisible)))
		edge.visible = true;
	    else
		edge.visible = false;
	}
    }


    /**
       Turns node visibility off if the node has no visible edges to
       other nodes, except if the node is the focus node (in focus
       mode), or the node is either the root node or the collapsed
       node (in non-focus mode).
     **/
    private void revalidateNodeVisibility()
    {
	for (int i = 0; i < nodes.size(); i++) {
	    WorkspaceNode node = (WorkspaceNode) nodes.elementAt(i);
	    if (node.isVisible() &&
		((! node.equals(focusNode) && focusNode != null) ||
		 (! (node.equals(rootNode) || node.equals(collapsedNode)) &&
		  focusNode == null))) {
		Vector links = node.getWorkspaceLinks();
		boolean hasVisibleEdges = false;
		for (int j = 0; j < links.size(); j++)
		    if (((WorkspaceLink) links.elementAt(j)).visible) {
			hasVisibleEdges = true;
			break;
		    }
		if (! hasVisibleEdges)
		    node.setVisible(false);
	    }
	}
    }


    public void showLegend()
    {
	// don't do anything if the old and new legends are the same
	if ((oldNodeColouring == nodeColouring) ||
	    (((nodeColouring == ABSOLUTE_WORKSPACE_DENSITY) ||
	      (nodeColouring == MINIMUM_WORKSPACE_DENSITY) ||
	      (nodeColouring == MAXIMUM_WORKSPACE_DENSITY) ||
	      (nodeColouring == MEAN_WORKSPACE_DENSITY)) &&
	     ((oldNodeColouring == ABSOLUTE_WORKSPACE_DENSITY) ||
	      (oldNodeColouring == MINIMUM_WORKSPACE_DENSITY) ||
	      (oldNodeColouring == MAXIMUM_WORKSPACE_DENSITY) ||
	      (oldNodeColouring == MEAN_WORKSPACE_DENSITY))))
	    return;

	if (legend != null) {
	    legendLocation = legend.getLocation();
	    legendSize = legend.getSize();
	    // remove the old legend
	    legend.dispose();
	    legend = null;
	}

	switch (nodeColouring) {
	case ABSOLUTE_WORKSPACE_DENSITY :
	case MINIMUM_WORKSPACE_DENSITY :
	case MAXIMUM_WORKSPACE_DENSITY :
	case MEAN_WORKSPACE_DENSITY :
	    legend = new ColourLegend(densityHist, densityColours,
				      "Workspace Density", "Num. Workspaces",
				      "Objects/Workspace");
	    break;
	case EVOLUTION_INTENSITY :
	    legend = new ColourLegend(evolIntensityHist, evolIntensityColours,
				      "Evolution Intensity", "Num. Workspaces",
				      "New Objects/Week");
	    break;
	case EVOLUTION_RECENCY :
	    legend = new ColourLegend(evolRecencyHist, evolRecencyColours,
				      "Evolution Recency", "Num. Workspaces",
				      "New Recent Objects");
	    break;
	case MESSAGE_INTENSITY :
	    legend = new ColourLegend(msgIntensityHist, msgIntensityColours,
				      "Message Intensity", "Num. Workspaces",
				      "New Msg./Week");
	    break;
	case MESSAGE_RECENCY :
	    legend = new ColourLegend(msgRecencyHist, msgRecencyColours,
				      "Message Recency", "Num. Workspaces",
				      "New Recent Msgs.");
	    break;
	}

	// try to put the legend back where it was before, at its old size
	if (legendLocation != null)
	    legend.setLocation(legendLocation);
	if (legendSize != null)
	    legend.setSize(legendSize);
	legend.setVisible(true);
    }


    public void hideLegend()
    {
	if (legend != null) {
	    legendLocation = legend.getLocation();
	    // remove the old legend
	    legend.dispose();
	    legend = null;
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
	if (e.getClickCount() == 2 && pick != null) {
	    if (relaxer == null)
		start();
	    if (focusNode != null)
		focusOff(focusNode);
	    if (pick.isVisibleLeaf())
		expandNode(pick);
	    else
		collapseNode(pick);
	}
	pick = null;
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
	    WorkspaceNode n = (WorkspaceNode) nodes.elementAt(i);
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

	// right mouse button click: focus on node
	if (((e.getModifiers() & InputEvent.BUTTON3_MASK) ==
	     InputEvent.BUTTON3_MASK) && ! e.isShiftDown() && (pick != null)) {
	    if (relaxer == null)
		start();
	    focusNode = pick;
	    if (! focusNode.hasFocus())
		focusOn(focusNode);
	}
	// shift-right mouse button click:
	// detailed map of workspace internals
	else if (((e.getModifiers() & InputEvent.BUTTON3_MASK) ==
		  InputEvent.BUTTON3_MASK) && e.isShiftDown() &&
		 (pick != null)) {
	    WorkspaceElementMap map = new WorkspaceElementMap(con, pick,
							      controls);
	    map.setVisible(true);
	}
	// middle mouse button click or shift-left mouse button click:
	// info on node
	else if ((((e.getModifiers() & InputEvent.BUTTON2_MASK) ==
		   InputEvent.BUTTON2_MASK) ||
		  (((e.getModifiers() & InputEvent.BUTTON1_MASK) ==
		    InputEvent.BUTTON1_MASK) && e.isShiftDown())) &&
		 (pick != null) && (!pick.specialNode)) {
	    WorkspaceInfoDialog infoDialog = new WorkspaceInfoDialog(pick);
	    infoDialog.setVisible(true);
	}

	if (! (e.getClickCount() == 2))
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


    // starting and stopping the animation

    public void start()
    {
	relaxer = new Thread(this);
	relaxer.start();
	iterations = 0;
    }


    public void stop()
    {
	relaxer = null;
    }

    // handle control events (originating from the associated control panel)
    public void controlActionPerformed(ControlEvent e)
    {
	int type = e.getType();
	int value = e.getValue();

	if (DEBUG)
	    System.out.println("[TreeGraphPanel] Event type: " + type +
			       "  Event value: " + value);

	switch (type) {
	case MapControls.NODE_COL_PARENT_CHILD :
	    oldNodeColouring = nodeColouring;
	    nodeColouring = PARENT_CHILD;
	    hideLegend();
	    iterations = maxIterations - 1;
	    break;
	case MapControls.NODE_COL_ABSOLUTE_WORKSPACE_DENSITY :
	    oldNodeColouring = nodeColouring;
	    nodeColouring = ABSOLUTE_WORKSPACE_DENSITY;
	    showLegend();
	    iterations = maxIterations - 1;
	    break;
	case MapControls.NODE_COL_MINIMUM_WORKSPACE_DENSITY :
	    oldNodeColouring = nodeColouring;
	    nodeColouring = MINIMUM_WORKSPACE_DENSITY;
	    showLegend();
	    iterations = maxIterations - 1;
	    break;
	case MapControls.NODE_COL_MAXIMUM_WORKSPACE_DENSITY :
	    oldNodeColouring = nodeColouring;
	    nodeColouring = MAXIMUM_WORKSPACE_DENSITY;
	    showLegend();
	    iterations = maxIterations - 1;
	    break;
	case MapControls.NODE_COL_MEAN_WORKSPACE_DENSITY :
	    oldNodeColouring = nodeColouring;
	    nodeColouring = MEAN_WORKSPACE_DENSITY;
	    showLegend();
	    iterations = maxIterations - 1;
	    break;
	case MapControls.NODE_COL_EVOLUTION_INTENSITY :
	    oldNodeColouring = nodeColouring;
	    nodeColouring = EVOLUTION_INTENSITY;
	    showLegend();
	    iterations = maxIterations - 1;
	    break;
	case MapControls.NODE_COL_EVOLUTION_RECENCY :
	    oldNodeColouring = nodeColouring;
	    nodeColouring = EVOLUTION_RECENCY;
	    showLegend();
	    iterations = maxIterations - 1;
	    break;
	case MapControls.NODE_COL_MESSAGE_INTENSITY :
	    oldNodeColouring = nodeColouring;
	    nodeColouring = MESSAGE_INTENSITY;
	    showLegend();
	    iterations = maxIterations - 1;
	    break;
	case MapControls.NODE_COL_MESSAGE_RECENCY :
	    oldNodeColouring = nodeColouring;
	    nodeColouring = MESSAGE_RECENCY;
	    showLegend();
	    iterations = maxIterations - 1;
	    break;
	case MapControls.EDGE_PULL_PARENT_CHILD :
	    childEdgeForce = (value == MapControls.ON);
	    iterations = 0;
	    break;
	case MapControls.EDGE_PULL_GOAL :
	    goalEdgeForce = (value == MapControls.ON);
	    iterations = 0;
	    break;
	case MapControls.EDGE_PULL_ACTION :
	    actionEdgeForce = (value == MapControls.ON);
	    iterations = 0;
	    break;
	case MapControls.EDGE_PULL_DISCUSSION :
	    discussionEdgeForce = (value == MapControls.ON);
	    iterations = 0;
	    break;
	case MapControls.EDGE_PULL_DOCUMENT :
	    documentEdgeForce = (value == MapControls.ON);
	    iterations = 0;
	    break;
	case MapControls.EDGE_PULL_MESSAGE_RULE :
	    messageRuleEdgeForce = (value == MapControls.ON);
	    iterations = 0;
	    break;
	case MapControls.EDGE_PULL_PARTICIPANT :
	    participantEdgeForce = (value == MapControls.ON);
	    iterations = 0;
	    break;
	case MapControls.EDGE_VIS_PARENT_CHILD :
	    childEdgeVisible = (value == MapControls.ON);
	    changeEdgeVisibility(WorkspaceLink.CHILD, childEdgeVisible);
	    iterations = 0;
	    break;
	case MapControls.EDGE_VIS_GOAL :
	    goalEdgeVisible = (value == MapControls.ON);
	    changeEdgeVisibility(WorkspaceLink.GOAL, goalEdgeVisible);
	    iterations = maxIterations - 1;
	    break;
	case MapControls.EDGE_VIS_ACTION :
	    actionEdgeVisible = (value == MapControls.ON);
	    changeEdgeVisibility(WorkspaceLink.ACTION, actionEdgeVisible);
	    iterations = maxIterations - 1;
	    break;
	case MapControls.EDGE_VIS_DISCUSSION :
	    discussionEdgeVisible = (value == MapControls.ON);
	    changeEdgeVisibility(WorkspaceLink.DISCUSSION,
				 discussionEdgeVisible);
	    iterations = maxIterations - 1;
	    break;
	case MapControls.EDGE_VIS_DOCUMENT :
	    documentEdgeVisible = (value == MapControls.ON);
	    changeEdgeVisibility(WorkspaceLink.DOCUMENT, documentEdgeVisible);
	    iterations = maxIterations - 1;
	    break;
	case MapControls.EDGE_VIS_MESSAGE_RULE :
	    messageRuleEdgeVisible = (value == MapControls.ON);
	    changeEdgeVisibility(WorkspaceLink.MESSAGERULE,
				 messageRuleEdgeVisible);
	    iterations = maxIterations - 1;
	    break;
	case MapControls.EDGE_VIS_PARTICIPANT :
	    participantEdgeVisible = (value == MapControls.ON);
	    changeEdgeVisibility(WorkspaceLink.PARTICIPANT,
				 participantEdgeVisible);
	    iterations = maxIterations - 1;
	    break;
	case MapControls.EDGE_VIS_SUBGRAPH :
	    edgeScope = MapControls.EDGE_VIS_SUBGRAPH;
	    break;
	case MapControls.EDGE_VIS_WHOLEGRAPH :
	    edgeScope = MapControls.EDGE_VIS_WHOLEGRAPH;
	    break;
	case MapControls.NODE_LABELING :
	    iterations = maxIterations - 1;
	    switch (value) {
	    case MapControls.SHORT_LABELS :
		setShortLabels(rootNode);
		break;
	    case MapControls.LONG_LABELS :
		setLongLabels(rootNode);
		break;
	    }
	    break;
	case MapControls.EDGE_LENGTH :
	    setEdgeLength(value);
	    iterations = 0;
	    break;
	case MapControls.EXPAND_ALL_NODES :
	    expandTree(rootNode);
	    //	    rootNode.expandDescendents();
	    rootNode.setVisible(true);
	    if (focusNode != null)
		focusOff(focusNode);
	    if (fixedNode != null && ! fixedNode.isSpecial())
		fixedNode.setFixed(false);
	    rootNode.setFixed(true);
	    fixedNode = rootNode;
	    revalidateEdgeVisibility();
	    iterations = 0;
	    break;
	case MapControls.COLLAPSE_ALL_NODES :
	    rootNode.collapseDescendents();
	    rootNode.setVisible(true);
	    if (focusNode != null)
		focusOff(focusNode);
	    if (fixedNode != null && ! fixedNode.isSpecial())
		fixedNode.setFixed(false);
	    rootNode.setFixed(true);
	    fixedNode = rootNode;
	    for (int i = 0; i < edges.size(); i++)
		((WorkspaceLink) edges.elementAt(i)).visible = false;
	    iterations = 0;
	    break;
	case MapControls.START_ANIMATION :
	    start();
	    break;
	case MapControls.STOP_ANIMATION :
	    stop();
	    break;
	case MapControls.PRINT :
	    // Get a PrinterJob
            PrinterJob printJob = PrinterJob.getPrinterJob();

            // Set the page format
	    double widthA4 = 210 * (72 / 25.4);
	    double heightA4 = 297 * (72 / 25.4);
	    double marginsize = 30 * (72 / 25.4);
  	    Paper paper = new Paper();
	    paper.setSize(widthA4, heightA4);
	    paper.setImageableArea(marginsize, marginsize,
				   widthA4 - (2 * marginsize),
				   heightA4 - (2 * marginsize));
	    PageFormat pageFormat = new PageFormat();
	    pageFormat.setPaper(paper);
	    pageFormat.setOrientation(PageFormat.LANDSCAPE);

	    printJob.setPrintable(this, pageFormat);

	    if (printJob.printDialog()) {
		try {
		    printJob.print();  
		} catch (Exception ex) {
		    ex.printStackTrace();
		}
	    }
	    break;
	}
    }
}
