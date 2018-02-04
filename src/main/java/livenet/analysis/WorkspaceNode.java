package livenet.analysis;

/*
 * Copyright (c) 2000-2018 Robert Biuk-Aghai
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you
 * may not use this file except in compliance with the License.  You
 * may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */

import java.awt.Point;
import java.util.Date;
import java.util.Vector;


public class WorkspaceNode implements TreeGraphConstants, java.io.Serializable
{
    // stuff that may get passed into the constructor
    private String workspace;
    private String owner;
    private String goal;
    private DateTime creationDateTime;
    private WorkspaceNode parent;
    private String workgroup;

    // stuff that is populated through methods
    private Vector children = new Vector();
    private Vector roles = new Vector();
    private Vector participants = new Vector();
    private Vector documents = new Vector();
    private Vector discussions = new Vector();
    private Vector actions = new Vector();
    private Vector messageTypes = new Vector();
    private Vector srcMessageRules = new Vector();
    private Vector targetMessageRules = new Vector();
    private Vector sentMessages = new Vector();
    private Vector receivedMessages = new Vector();
    private Vector workspaceLinks = new Vector();

    // the names which are used on the workspace node's visual representation
    String shortName;
    String fullName;
    String displayName;

    // indicates a special node
    boolean specialNode = false;

    // workspace measures

    // Workspace density measures the number of workspace elements within the
    // workspace. Workspace elements counting toward workspace density include:
    // documents, discussions, actions, message types, and message rules.
    private int absDensity = 0;
    private int maxRoleDensity = Integer.MIN_VALUE;
    private int minRoleDensity = Integer.MAX_VALUE;
    private float meanRoleDensity = (float) 0.0;

    // Evolution intensity measures the number of workspace elements added to
    // a workspace starting from the moment the workspace was created until
    // the present time, per year. Workspace elements counting toward
    // evolution intensity include: roles, participants, documents,
    // discussions,actions, message types, and message rules.
    private float evolIntensity = (float) 0.0;
    private int evolutionObjects = 0;
    private float evolutionWeeks;

    // Evolution recency measures how recent workspace elements were
    // added to the workspace. Elements added further back in time
    // count less toward evolution recency than those added closer to
    // the present time, by applying a deflating factor to their
    // weight.
    private float evolutionRecency = (float) 0.0;

    // The recency interval determines how many days are considered in
    // the calculation of evolution and message recency measures. Note
    // that the boundary value is not included in the calculation;
    // i.e. if recencyInterval = 30, then the period which will be
    // included in recency calculations is from "today" until 29 days
    // back.
    private int recencyInterval = 30;

    // Message recency measures how recent messages were sent or
    // received to/from the workspace. Messages count towards message
    // recency in proportion to their age, as with evolution recency.
    private float messageRecency = (float) 0.0;

    // coordinates of the node's centre
    double x;
    double y;

    // coordinates of the node's bounding box
    int x1;
    int y1;
    int x2;
    int y2;

    // deltas for adjusting the node's position
    double dx;
    double dy;

    // is the node's position fixed?
    boolean fixed = false;

    // should the node be visible?
    boolean visible = false;

    // should the node be focused on?
    boolean focus = false;


    public WorkspaceNode(String workspace, String owner)
    {
	this(workspace, owner, null);
    }


    public WorkspaceNode(String workspace, String owner, WorkspaceNode parent)
    {
	this.workspace = workspace;
	this.owner = owner;
	this.parent = parent;
	this.shortName = createShortName(workspace, owner);
	this.fullName = createFullName(workspace, owner);
	this.setShortDisplayName();
    }


    public WorkspaceNode(String workspace, String owner, DateTime datetime,
			 String goal, String workgroup)
    {
	this(workspace, owner);
	this.creationDateTime = datetime;
	this.goal = goal;
	this.workgroup = workgroup;
	int workspaceDays = datetime.getDaysDiff(new DateTime());
	this.evolutionWeeks = (float) (workspaceDays / 7.0);
    }


    // This constructor is for use with special nodes, such as the one
    // used as a collection for workgroups' root nodes
    public WorkspaceNode(String name, boolean specialNode)
    {
	this.shortName = name;
	this.fullName = name;
	this.displayName = name;
	this.specialNode = specialNode;
    }


    public void setParent(WorkspaceNode parent)
    {
	this.parent = parent;
    }


    public void addChild(WorkspaceNode child)
    {
	children.addElement(child);
    }


    public void addWorkspaceLink(WorkspaceLink link)
    {
	workspaceLinks.addElement(link);
    }


    public String getWorkspace()
    {
	return this.workspace;
    }


    public String getOwner()
    {
	return this.owner;
    }


    public String getGoal()
    {
	return this.goal;
    }


    public DateTime getCreationDateTime()
    {
	return this.creationDateTime;
    }


    public String getWorkgroup()
    {
	return this.workgroup;
    }


    public WorkspaceNode getParent()
    {
	return this.parent;
    }


    public Vector getChildren()
    {
	return this.children;
    }


    public WorkspaceNode getChild(int i)
    {
	try {
	    return (WorkspaceNode) this.children.elementAt(i);
	}
	catch(ArrayIndexOutOfBoundsException ex) {
	    return null;
	}
    }


    public int getChildCount()
    {
	return this.children.size();
    }


    public boolean hasChildren()
    {
	return (this.children.size() > 0);
    }


    public Vector getWorkspaceLinks()
    {
	return this.workspaceLinks;
    }


    public WorkspaceLink getWorkspaceLink(int i)
    {
	try {
	    return (WorkspaceLink) this.workspaceLinks.elementAt(i);
	}
	catch(ArrayIndexOutOfBoundsException ex) {
	    return null;
	}
    }


    public int getWorkspaceLinkCount()
    {
	return this.workspaceLinks.size();
    }


    /**
       Returns a boolean indicating whether a workspace link of the
       specified type to the specified workspace exists.
    **/
    public boolean haveWorkspaceLink(WorkspaceNode otherNode, int type)
    {
	for (int i = 0; i < workspaceLinks.size(); i++) {
	    WorkspaceLink link = (WorkspaceLink) workspaceLinks.elementAt(i);
	    if (((link.fromNode.equals(this) && link.toNode.equals(otherNode))
		 ||
		 (link.toNode.equals(this) && link.fromNode.equals(otherNode)))
		&& (link.type == type))
		return true;
	}
	return false;
    }


    /**
       Returns a workspace link of the specified type to the specified
       workspace, or null if it doesn't exists.
    **/
    public WorkspaceLink getWorkspaceLink(WorkspaceNode otherNode, int type)
    {
	for (int i = 0; i < workspaceLinks.size(); i++) {
	    WorkspaceLink link = (WorkspaceLink) workspaceLinks.elementAt(i);
	    if (((link.fromNode.equals(this) && link.toNode.equals(otherNode))
		 ||
		 (link.toNode.equals(this) && link.fromNode.equals(otherNode)))
		&& (link.type == type))
		return link;
	}
	return null;
    }


    public WorkspaceNode getNode(String workspace, String owner)
    {
	if (this.workspace.equals(workspace) && this.owner.equals(owner))
	    return this;
	else {
	    for (int i = 0; i < this.getChildCount(); i++) {
		WorkspaceNode node =
		    this.getChild(i).getNode(workspace, owner);
		if (node != null)
 		    return node;
	    }
	}
	return null;
    }


    public Vector getRoles()
    {
	return roles;
    }


    public Vector getParticipants()
    {
	return participants;
    }


    public Vector getDocuments()
    {
	return documents;
    }


    public Vector getDiscussions()
    {
	return discussions;
    }


    public Vector getActions()
    {
	return actions;
    }


    public Vector getMessageTypes()
    {
	return messageTypes;
    }


    public Vector getSrcMessageRules()
    {
	return srcMessageRules;
    }


    public Vector getTargetMessageRules()
    {
	return targetMessageRules;
    }


    public Vector getSentMessages()
    {
	return sentMessages;
    }


    public Vector getReceivedMessages()
    {
	return receivedMessages;
    }


    String createShortName(String workspace, String owner)
    {
	if (workspace.length() > 10)
	    workspace = workspace.substring(0, 4) + "..." +
		workspace.substring(workspace.length() - 4);
	if (owner.length() > 10)
	    owner = owner.substring(0, 4) + "..." +
		owner.substring(owner.length() - 4);
	// return workspace + ":" + owner;
	return workspace;
    }


    String createFullName(String workspace, String owner)
    {
	//return workspace + ":" + owner;
	return workspace;
    }


    public void setFixed(boolean fixed)
    {
	this.fixed = fixed;
    }


    public boolean isFixed()
    {
	return fixed;
    }


    public void setVisible(boolean visible)
    {
	this.visible = visible;
    }


    public boolean isVisible()
    {
	return visible;
    }


    public boolean isVisibleLeaf()
    {
	for (int i = 0; i < getChildCount(); i++)
	    if (getChild(i).isVisible())
		return false;
	return true;
    }


    public boolean isLeaf()
    {
	if (children.size() == 0)
	    return true;
	else
	    return false;
    }


    public boolean isSpecial()
    {
	return specialNode;
    }


    public void expandChildren()
    {
	for (int i = 0; i < getChildCount(); i++) {
	    WorkspaceNode childNode = getChild(i);
	    childNode.setVisible(true);
	}
    }


    public void collapseChildren()
    {
	for (int i = 0; i < getChildCount(); i++) {
	    WorkspaceNode childNode = getChild(i);
	    childNode.setVisible(false);
	}
    }


    public void expandDescendents()
    {
	for (int i = 0; i < getChildCount(); i++) {
	    WorkspaceNode childNode = getChild(i);
	    childNode.setVisible(true);
	    childNode.expandDescendents();
	}
    }


    public void collapseDescendents()
    {
	for (int i = 0; i < getChildCount(); i++) {
	    WorkspaceNode childNode = getChild(i);
	    childNode.setVisible(false);
	    childNode.collapseDescendents();
	}
    }


    public void setFocus(boolean focus)
    {
	this.focus = focus;
    }


    public boolean hasFocus()
    {
	return focus;
    }


    public void setShortDisplayName()
    {
	this.displayName = this.shortName;
    }


    public void setLongDisplayName()
    {
	this.displayName = this.fullName;
    }


    public boolean contains(Point p)
    {
	return (p.x >= x1 && p.x <= x2 && p.y >= y1 && p.y <= y2);
    }


    public void recalculateRoleDensities()
    {
	int totalDensities = 0;
	for (int i = 0; i < roles.size(); i++) {
	    int thisDensity =
		((Role) roles.elementAt(i)).assignedObjects.size();
	    if (thisDensity > maxRoleDensity)
		maxRoleDensity = thisDensity;
	    if (thisDensity < minRoleDensity)
		minRoleDensity = thisDensity;
	    totalDensities += thisDensity;
	}
	meanRoleDensity = (float) totalDensities / (float) roles.size();
    }


    public int getAbsDensity()
    {
	return absDensity;
    }


    public int getMinRoleDensity()
    {
	recalculateRoleDensities();
	return minRoleDensity;
    }


    public int getMaxRoleDensity()
    {
	recalculateRoleDensities();
	return maxRoleDensity;
    }


    public float getMeanRoleDensity()
    {
	recalculateRoleDensities();
	return meanRoleDensity;
    }


    public float getEvolutionIntensity()
    {
	float evolutionIntensity = (float) 0.0;
	if (evolutionWeeks > 0.0)
	    evolutionIntensity =
		(float) evolutionObjects / (float) evolutionWeeks;
	return evolutionIntensity;
    }


    public float getEvolutionRecency()
    {
	return evolutionRecency;
    }


    private void addToEvolutionRecency(DateTime datetime)
    {
	int age = datetime.getDaysDiff(new DateTime());
	if (age < recencyInterval)
	    evolutionRecency +=
		(float) (1 - (Math.log(age + 1) / Math.log(recencyInterval)));
    }


    public float getMessageIntensity()
    {
	float messageIntensity = (float) 0.0;
	if (evolutionWeeks > 0.0)
	    messageIntensity =
		(float) (sentMessages.size() + receivedMessages.size()) /
		(float) evolutionWeeks;
	return messageIntensity;
    }


    public float getMessageRecency()
    {
	return messageRecency;
    }


    private void addToMessageRecency(DateTime datetime)
    {
	int age = datetime.getDaysDiff(new DateTime());
	if (age < recencyInterval)
	    messageRecency +=
		(float) (1 - (Math.log(age + 1) / Math.log(recencyInterval)));
    }


    public void addRole(Role r)
    {
	roles.addElement(r);
	evolutionObjects++;
	addToEvolutionRecency(r.creationDateTime);
    }


    public void addParticipant(Participant p)
    {
	participants.addElement(p);
	evolutionObjects++;
	addToEvolutionRecency(p.creationDateTime);
    }


    public void addDocument(Document d)
    {
	documents.addElement(d);
	absDensity++;
	evolutionObjects++;
	addToEvolutionRecency(d.creationDateTime);
    }


    public void addDiscussion(Discussion d)
    {
	discussions.addElement(d);
	absDensity++;
	evolutionObjects++;
	addToEvolutionRecency(d.creationDateTime);
    }


    public void addAction(Action a)
    {
	actions.addElement(a);
	absDensity++;
	evolutionObjects++;
	addToEvolutionRecency(a.creationDateTime);
    }


    public void addMessageType(MessageType m)
    {
	messageTypes.addElement(m);
	absDensity++;
	evolutionObjects++;
    }


    public void addSrcMessageRule(MessageRule m)
    {
	srcMessageRules.addElement(m);
	absDensity++;
	evolutionObjects++;
    }


    public void addTargetMessageRule(MessageRule m)
    {
	targetMessageRules.addElement(m);
    }


    public void addSentMessage(Message m)
    {
	sentMessages.addElement(m);
	addToMessageRecency(m.sentDateTime);
    }


    public void addReceivedMessage(Message m)
    {
	receivedMessages.addElement(m);
	addToMessageRecency(m.sentDateTime);
    }
}
