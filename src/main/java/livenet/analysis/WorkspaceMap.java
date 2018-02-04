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

import java.awt.*;
import java.io.*;
import java.sql.*;
import java.util.Vector;
import javax.swing.*;


public class WorkspaceMap extends JFrame implements TreeGraphConstants
{
    // for debugging set this to true
    private boolean DEBUG = false;

    // file holding saved workspace tree data
    private String treeFileName = "wstree.obj";
    
    // Database queries

    // Query to get the root of the workspace tree
    private String rootQuery =
	"SELECT ChildWorkspace, ChildOwner " +
	"FROM   DBA.WorkspaceLinks " +
	"WHERE  Workspace IS NULL AND Owner IS NULL " +
	"AND    Workgroup IN ";

    // Query to get all workspaces of a given workgroup
    private String workspaceQuery =
	"SELECT W.WorkspaceName, W.OwnerName, W.DateCreated, W.Goals, " +
	"       L.Workgroup " +
	"FROM   DBA.Workspaces W, DBA.WorkspaceLinks L " +
	"WHERE  W.WorkspaceName = L.ChildWorkspace " +
	"AND    W.OwnerName = L.ChildOwner " +
	"AND    L.Workgroup IN ";

    // Query to get all workspace links in a given workgroup
    private String workspaceLinkQuery =
	"SELECT Workspace, Owner, ChildWorkspace, ChildOwner " +
	"FROM   DBA.WorkspaceLinks " +
	"WHERE  Workgroup IN ";

    // Query to get all roles in a given workgroup
    private String roleQuery =
	"SELECT R.Workspace, R.Owner, R.Role, R.DateCreated " +
	"FROM   DBA.Roles R, DBA.WorkspaceLinks L " +
	"WHERE  R.Workspace = L.ChildWorkspace " +
	"AND    R.Owner = L.ChildOwner " +
	"AND    L.Workgroup IN ";

    // Query to get all participants in a given workgroup
    private String participantQuery =
	"SELECT P.Workspace, P.Owner, P.Role, P.Participant, P.DateJoined " +
	"FROM   DBA.Participants P, DBA.WorkspaceLinks L " +
	"WHERE  P.Workspace = L.ChildWorkspace " +
	"AND    P.Owner = L.ChildOwner " +
	"AND    L.Workgroup IN ";

    // Query to get all objects in a given workgroup
    private String objectQuery =
	"SELECT O.Workspace, O.Owner, O.ObjectName, O.Type, " +
	"       O.ObjectType, O.Url, O.DateCreated " +
	"FROM   DBA.Objects O, DBA.WorkspaceLinks L " +
	"WHERE  O.Workspace = L.ChildWorkspace " +
	"AND    O.Owner = L.ChildOwner " +
	"AND    L.Workgroup IN ";

    // Todo: check if it is necessary to add O.Type as a joining attribute
    private String objectRoleQuery =
	"SELECT O.Workspace, O.Owner, R.Role, O.ObjectName, O.Type " +
	"FROM   DBA.Objects O, DBA.Folders F, DBA.FolderRoles R, " +
	"       DBA.WorkspaceLinks W " +
	"WHERE  O.Workspace = F.Workspace " +
	"AND    O.Owner = F.Owner " +
	"AND    O.Workspace = W.ChildWorkspace " +
	"AND    O.Owner = W.ChildOwner " +
	"AND    F.Workspace = R.Workspace " +
	"AND    F.Owner = R.Owner " +
	"AND    F.Folder = R.Folder " +
	"AND    W.Workgroup IN ";

    // Query to get all messages in a given workgroup
    private String messageQuery =
	"SELECT M.MsgSubject, M.MsgSender, M.MsgType, M.DateSent, " +
	"       M.MsgSourceWorkspace, M.MsgSourceOwner, " +
	"       M.MsgTargetWorkspace, M.MsgTargetOwner, M.MsgTargetRole, " +
	"       M.MsgTargetType " +
	"FROM   DBA.Messages M, DBA.WorkspaceLinks L " +
	"WHERE  M.MsgSourceWorkspace = L.ChildWorkspace " +
	"AND    M.MsgSourceOwner = L.ChildOwner " +
	"AND    L.Workgroup IN ";

    // Query to get all message types in a given workgroup
    private String messageTypeQuery =
	"SELECT M.Workspace, M.Owner, M.MsgType " +
	"FROM   DBA.MessageTypes M, DBA.WorkspaceLinks L " +
	"WHERE  M.Workspace = L.ChildWorkspace " +
	"AND    M.Owner = L.ChildOwner " +
	"AND    L.Workgroup IN ";

    // Query to get all message rules in a given workgroup
    private String messageRuleQuery =
	"SELECT M.MsgSourceWorkspace, M.MsgSourceOwner, M.MsgType, " +
	"       M.MsgTargetWorkspace, M.MsgTargetOwner, M.MsgTargetRole, " +
	"       M.MsgTargetType " +
	"FROM   DBA.MessageRules M, DBA.WorkspaceLinks L " +
	"WHERE  M.MsgSourceWorkspace = L.ChildWorkspace " +
	"AND    M.MsgSourceOwner = L.ChildOwner " +
	"AND    L.Workgroup IN ";

    private String workgroupList;

    // stuff that get's passed into the constructor
    private Connection con;
    private String[] workgroups;

    // workspace database stuff
    private WorkspaceNode rootNode;
    private Vector workspaces = new Vector();
    private Vector roles = new Vector();
    private Vector participants = new Vector();
    private Vector documents = new Vector();
    private Vector discussions = new Vector();
    private Vector actions = new Vector();
    private Vector messagetypes = new Vector();
    private Vector messagerules = new Vector();
    private Vector messages = new Vector();

    private TreeGraphPanel tgPan;


    public WorkspaceMap(Connection con, String[] workgroups,
			MapControls controls, boolean debug)
    {
	this.con = con;
	this.workgroups = workgroups;
	this.DEBUG = debug;

	if (DEBUG)
	    // read all the workspace data from file
	    readTree();
	else
	    // read all the workspace data from the database
	    getDBData();

	// Set up the graph panel
	tgPan = new TreeGraphPanel(con, rootNode, controls);
	this.getContentPane().add(BorderLayout.CENTER, tgPan);
	tgPan.start();

	StringBuffer wg = new StringBuffer();
	if (workgroups != null) {
	    wg.append(" (Workgroups: ");
	    for (int i = 0; i < workgroups.length; i++) {
		if (i > 0)
		    wg.append(",");
		wg.append(workgroups[i]);
	    }
	    wg.append(")");
	}

	this.setTitle("Workspace Map" + wg);
	//	this.pack();
	this.setLocation(new Point(0, 0));
	this.setSize(tgPan.getSize());
	this.setVisible(true);
    }


    private void getDBData()
    {
	if (workgroups.length > 1) {
	    rootNode = new WorkspaceNode("Workgroups", true);
	    workspaces.addElement(rootNode);
	}

	StringBuffer wgl = new StringBuffer("( ");
	for (int i = 0; i < workgroups.length; i++) {
	    if (i > 0)
		wgl.append(", ");
	    wgl.append("'" + workgroups[i] + "'");
	}
	wgl.append(" )");

	workgroupList = wgl.toString();

	ProgressDialog progDialog = new ProgressDialog(new JFrame(), 10, 350);

	progDialog.setMessage("Getting workspace data...", 1);
	getWorkspaceData();

	progDialog.setMessage("Getting workspace link data...", 2);
	getWorkspaceLinkData();

	progDialog.setMessage("Getting role data...", 3);
	getRoleData();

	progDialog.setMessage("Getting participant data...", 4);
	getParticipantData();

	progDialog.setMessage("Getting object data...", 5);
	getObjectData();

	progDialog.setMessage("Getting object role data...", 6);
	getObjectRoleData();

	progDialog.setMessage("Getting message type data...", 7);
	getMessageTypeData();

	progDialog.setMessage("Getting message rule data...", 8);
	getMessageRuleData();

	progDialog.setMessage("Getting message data...", 9);
	getMessageData();

	progDialog.setMessage("Setting up links...", 10);
	createWorkspaceLinks();

	progDialog.setMessage("Done.");
	progDialog.dispose();
	progDialog = null;
    }


    private void getWorkspaceData()
    {
	try {
	    PreparedStatement stmt = con.prepareStatement(workspaceQuery +
							  workgroupList);
	    // stmt.setString(1, workgroup);
	    ResultSet rs = stmt.executeQuery();

	    while (rs.next()) {
		String workspace = rs.getString(1);
		String owner = rs.getString(2);
		Date date = rs.getDate(3);
		Time time = rs.getTime(3);
		String goal = rs.getString(4);
		String workgroup = rs.getString(5);

		if (goal != null)
		    if (goal == "" || goal.equals("null"))
			goal = null;

		DateTime datetime = new DateTime(date, time);

		WorkspaceNode ws = new WorkspaceNode(workspace, owner,
						     datetime, goal,
						     workgroup);
		workspaces.addElement(ws);
	    }
	    rs.close();
	    stmt.close();
	}
	catch (Exception ex) {
	    System.err.println("getWorkspaceData: " + ex.getMessage());
	}
    }


    private void getWorkspaceLinkData()
    {
	try {
	    PreparedStatement stmt = con.prepareStatement(workspaceLinkQuery +
							  workgroupList);
	    // stmt.setString(1, workgroup);
	    ResultSet rs = stmt.executeQuery();

	    while (rs.next()) {
		String parentWorkspace = rs.getString(1);
		String parentOwner = rs.getString(2);
		String childWorkspace = rs.getString(3);
		String childOwner = rs.getString(4);

		// find the parent and child workspace nodes
		WorkspaceNode childWs = getWorkspace(childWorkspace,
						     childOwner);
		if (parentWorkspace == null && parentOwner == null) {
		    if (workgroups.length > 1) {
			childWs.setParent(rootNode);
			rootNode.addChild(childWs);
		    }
		    else
			rootNode = childWs;
		}
		else {
		    WorkspaceNode parentWs = getWorkspace(parentWorkspace,
							  parentOwner);
		    if (parentWs != null && childWs != null) {
			childWs.setParent(parentWs);
			parentWs.addChild(childWs);
		    }
		}
	    }
	    rs.close();
	    stmt.close();
	}
	catch (Exception ex) {
	    System.err.println("getWorkspaceLinkData: " + ex.getMessage());
	}
    }


    private void getRoleData()
    {
	try {
	    PreparedStatement stmt = con.prepareStatement(roleQuery +
							  workgroupList);
	    // stmt.setString(1, workgroup);
	    ResultSet rs = stmt.executeQuery();

	    while (rs.next()) {
		String workspace = rs.getString(1);
		String owner = rs.getString(2);
		String role = rs.getString(3);
		Date date = rs.getDate(4);
		Time time = rs.getTime(4);

		DateTime datetime = new DateTime(date, time);

		WorkspaceNode ws = getWorkspace(workspace, owner);

		Role r = new Role(ws, role, datetime);

		roles.addElement(r);
		ws.addRole(r);
	    }
	    rs.close();
	    stmt.close();
	}
	catch (SQLException ex) {
	    System.err.println("getRoleData: " + ex.getMessage() + "\n  " +
			       ex.getSQLState());
	}
    }


    private void getParticipantData()
    {
	try {
	    PreparedStatement stmt = con.prepareStatement(participantQuery +
							  workgroupList);
	    // stmt.setString(1, workgroup);
	    ResultSet rs = stmt.executeQuery();

	    while (rs.next()) {
		String workspace = rs.getString(1);
		String owner = rs.getString(2);
		String role = rs.getString(3);
		String participant = rs.getString(4);
		Date date = rs.getDate(5);
		Time time = rs.getTime(5);

		DateTime datetime = new DateTime(date, time);

		WorkspaceNode ws = getWorkspace(workspace, owner);

		Role r = getRole(ws, role);
		if (r == null) {
		    System.out.
			println("getparticipantData returned null role!");
		    System.out.println("Participant: " + participant +
				       ", Workspace: " + ws.fullName +
				       ", Role: " + role);
		    continue;
		}
		
		Participant p = new Participant(ws, participant, r, datetime);

		participants.addElement(p);
		r.addParticipant(p);
		ws.addParticipant(p);
	    }
	    rs.close();
	    stmt.close();
	}
	catch (SQLException ex) {
	    System.err.println("getParticipantData: " + ex.getMessage());
	}
    }


    private void getObjectData()
    {
	try {
	    PreparedStatement stmt = con.prepareStatement(objectQuery +
							  workgroupList);
	    // stmt.setString(1, workgroup);
	    ResultSet rs = stmt.executeQuery();

	    while (rs.next()) {
		String workspace = rs.getString(1);
		String owner = rs.getString(2);
		String name = rs.getString(3);
		String folder = rs.getString(4);
		String type = rs.getString(5);
		String url = rs.getString(6);
		Date date = rs.getDate(7);
		Time time = rs.getTime(7);

		DateTime datetime = new DateTime(date, time);

		WorkspaceNode ws = getWorkspace(workspace, owner);

		if (type.toLowerCase().equals("document")) {
		    Document document = new Document(ws, folder, name, url,
						     datetime);
		    documents.addElement(document);
		    ws.addDocument(document);
		} else if (type.toLowerCase().equals("discussion")) {
		    Discussion discussion = new Discussion(ws, folder, name,
							   url, datetime);
		    discussions.addElement(discussion);
		    ws.addDiscussion(discussion);
		} else if (type.toLowerCase().equals("action")) {
		    Action action = new Action(ws, folder, name, url,
					       datetime);
		    actions.addElement(action);
		    ws.addAction(action);
		}
	    }
	    rs.close();
	    stmt.close();
	}
	catch (Exception ex) {
	    System.err.println("getObjectData: " + ex.getMessage());
	}
    }


    private void getObjectRoleData()
    {
	try {
	    PreparedStatement stmt = con.prepareStatement(objectRoleQuery +
							  workgroupList);
	    // stmt.setString(1, workgroup);
	    ResultSet rs = stmt.executeQuery();

	    while (rs.next()) {
		String workspace = rs.getString(1);
		String owner = rs.getString(2);
		String role = rs.getString(3);
		String obj = rs.getString(4);

		WorkspaceNode ws = getWorkspace(workspace, owner);
		Role r = getRole(ws, role);
		WorkspaceObject o = getWorkspaceObject(ws, obj);

		r.addAssignedObject(o);
		o.addAssignedRole(r);
	    }
	    rs.close();
	    stmt.close();
	}
	catch (Exception ex) {
	    System.err.println("getObjectRoleData: " + ex.getMessage());
	}
    }


    private void getMessageTypeData()
    {
	try {
	    PreparedStatement stmt = con.prepareStatement(messageTypeQuery +
							  workgroupList);
	    // stmt.setString(1, workgroup);
	    ResultSet rs = stmt.executeQuery();

	    while (rs.next()) {
		String workspace = rs.getString(1);
		String owner = rs.getString(2);
		String type = rs.getString(3);

		WorkspaceNode ws = getWorkspace(workspace, owner);

		MessageType mt = new MessageType(ws, type);
		messagetypes.addElement(mt);
		ws.addMessageType(mt);
	    }
	    rs.close();
	    stmt.close();
	}
	catch (Exception ex) {
	    System.err.println("getMessageTypeData: " + ex.getMessage());
	}
    }


    private void getMessageRuleData()
    {
	try {
	    PreparedStatement stmt = con.prepareStatement(messageRuleQuery +
							  workgroupList);
	    // stmt.setString(1, workgroup);
	    ResultSet rs = stmt.executeQuery();

	    while (rs.next()) {
		String srcWorkspace = rs.getString(1);
		String srcOwner = rs.getString(2);
		String srcType = rs.getString(3);
		String targetWorkspace = rs.getString(4);
		String targetOwner = rs.getString(5);
		String targetRole = rs.getString(6);
		String targetType = rs.getString(7);

		WorkspaceNode srcWs = getWorkspace(srcWorkspace, srcOwner);
		WorkspaceNode targetWs = getWorkspace(targetWorkspace,
						      targetOwner);

		MessageType srcT = getMessageType(srcWs, srcType);
		MessageType targetT = getMessageType(targetWs, targetType);

		Role targetR = getRole(targetWs, targetRole);

		MessageRule mr = new MessageRule(srcWs, srcT, targetWs,
						 targetT, targetR);

		messagerules.addElement(mr);
		srcWs.addSrcMessageRule(mr);
		targetWs.addTargetMessageRule(mr);
	    }
	    rs.close();
	    stmt.close();
	}
	catch (Exception ex) {
	    System.err.println("getMessageRuleData: " + ex.getMessage());
	}
    }


    private void getMessageData()
    {
	try {
	    PreparedStatement stmt = con.prepareStatement(messageQuery +
							  workgroupList);
	    // stmt.setString(1, workgroup);
	    ResultSet rs = stmt.executeQuery();

	    while (rs.next()) {
		String subject = rs.getString(1);
		String sender = rs.getString(2);
		String srcType = rs.getString(3);
		Date date = rs.getDate(4);
		Time time = rs.getTime(4);
		String srcWorkspace = rs.getString(5);
		String srcOwner = rs.getString(6);
		String targetWorkspace = rs.getString(7);
		String targetOwner = rs.getString(8);
		String targetRole = rs.getString(9);
		String targetType = rs.getString(10);

		DateTime datetime = new DateTime(date, time);

		WorkspaceNode srcWs = getWorkspace(srcWorkspace, srcOwner);
		WorkspaceNode targetWs = getWorkspace(targetWorkspace,
						      targetOwner);
		MessageType srcT = getMessageType(srcWs, srcType);
		MessageType targetT = getMessageType(targetWs, targetType);
		Role targetR = getRole(targetWs, targetRole);

		Message m = new Message(subject, sender, datetime, srcWs, srcT,
					targetWs, targetT, targetR);

		messages.addElement(m);
		srcWs.addSentMessage(m);
		targetWs.addReceivedMessage(m);
	    }
	    rs.close();
	    stmt.close();
	}
	catch (Exception ex) {
	    System.err.println("getMessageData: " + ex.getMessage());
	}
    }


    void createWorkspaceLinks()
    {
	// The processing takes place in two passes:
	// 1. Collect all information on workspace elements common
	//    across workspaces.
	// 2. Create the actual workspace links and insert into
	//    workspaces, or if workspace links of that type already
	//    exist, increment their counter.
	// Exception: child links and message rule links are inserted
	// during pass 1.

	// the data structure for following vectors:
	// each vector contains a number of arrays
	// each array contains two elements: a string and a vector of
	// workspace nodes
	Vector goals = new Vector();
	Vector documents = new Vector();
	Vector discussions = new Vector();
	Vector actions = new Vector();
	Vector participants = new Vector();


	// Pass 1: collect all information on common workspace elements

	for (int i = 0; i < workspaces.size(); i++) {
	    WorkspaceNode currentNode =
		(WorkspaceNode) workspaces.elementAt(i);

	    // Get goal info
	    if (currentNode.getGoal() != null) {
		boolean goalFound = false;
		for (int g = 0; g < goals.size(); g++) {
		    if (((String)
			 ((Object[]) goals.elementAt(g))[0]).
			equals(currentNode.getGoal())) {
			((Vector)
			 ((Object[])
			  goals.elementAt(g))[1]).addElement(currentNode);
			goalFound = true;
			break;
		    }
		}
		if (!goalFound) {
		    Object goalArray [] = new Object[2];
		    goalArray[0] = currentNode.getGoal();
		    goalArray[1] = new Vector();
		    ((Vector) goalArray[1]).addElement(currentNode);
		    goals.addElement(goalArray);
		}
	    } // if

	    // Get document info
	    Vector nodeDocuments = currentNode.getDocuments();
	    for (int j = 0; j < nodeDocuments.size(); j++) {
		boolean documentFound = false;
		for (int d = 0; d < documents.size(); d++) {
		    if (((String) ((Object[]) documents.elementAt(d))[0]).
			equals(((Document) nodeDocuments.elementAt(j)).
			       url)) {
			((Vector) ((Object[]) documents.elementAt(d))[1]).
			    addElement(currentNode);
			documentFound = true;
			break;
		    }
		}
		if (!documentFound) {
		    Object documentArray [] = new Object[2];
		    documentArray[0] =
			((Document) nodeDocuments.elementAt(j)).url;
		    documentArray[1] = new Vector();
		    ((Vector) documentArray[1]).addElement(currentNode);
		    documents.addElement(documentArray);
		}
	    } // for

	    // Get discussion info
	    Vector nodeDiscussions = currentNode.getDiscussions();
	    for (int j = 0; j < nodeDiscussions.size(); j++) {
		boolean discussionFound = false;
		for (int d = 0; d < discussions.size(); d++) {
		    if (((String) ((Object[]) discussions.elementAt(d))[0]).
			equals(((Discussion)
				nodeDiscussions.elementAt(j)).url)) {
			((Vector) ((Object[]) discussions.elementAt(d))[1]).
			    addElement(currentNode);
			discussionFound = true;
			break;
		    }
		}
		if (!discussionFound) {
		    Object discussionArray [] = new Object[2];
		    discussionArray[0] =
			((Discussion) nodeDiscussions.elementAt(j)).
			url;
		    discussionArray[1] = new Vector();
		    ((Vector) discussionArray[1]).addElement(currentNode);
		    discussions.addElement(discussionArray);
		}
	    } // for

	    // Get action info
	    Vector nodeActions = currentNode.getActions();
	    for (int j = 0; j < nodeActions.size(); j++) {
		boolean actionFound = false;
		for (int a = 0; a < actions.size(); a++) {
		    if (((String) ((Object[]) actions.elementAt(a))[0]).
			equals(((Action)
				nodeActions.elementAt(j)).url)) {
			((Vector) ((Object[]) actions.elementAt(a))[1]).
			    addElement(currentNode);
			actionFound = true;
			break;
		    }
		}
		if (!actionFound) {
		    Object actionArray [] = new Object[2];
		    actionArray[0] =
			((Action) nodeActions.elementAt(j)).
			url;
		    actionArray[1] = new Vector();
		    ((Vector) actionArray[1]).addElement(currentNode);
		    actions.addElement(actionArray);
		}
	    } // for

	    // Get participant info
	    Vector nodeParticipants = currentNode.getParticipants();
	    for (int j = 0; j < nodeParticipants.size(); j++) {
		boolean participantFound = false;
		for (int p = 0; p < participants.size(); p++) {
		    if (((String) ((Object[]) participants.elementAt(p))[0]).
			equals(((Participant)
				nodeParticipants.elementAt(j)).name)) {
			((Vector) ((Object[]) participants.elementAt(p))[1]).
			    addElement(currentNode);
			participantFound = true;
			break;
		    }
		}
		if (!participantFound) {
		    Object participantArray [] = new Object[2];
		    participantArray[0] =
			((Participant) nodeParticipants.elementAt(j)).
			name;
		    participantArray[1] = new Vector();
		    ((Vector) participantArray[1]).addElement(currentNode);
		    participants.addElement(participantArray);
		}
	    } // for
	    
	    // create child links
	    for (int j = 0; j < currentNode.getChildCount(); j++) {
		WorkspaceNode childNode = currentNode.getChild(j);
		WorkspaceLink link =
		    new WorkspaceLink(currentNode, childNode,
				      WorkspaceLink.CHILD, WorkspaceLink.TO,
				      1, initialEdgeLength);
		currentNode.addWorkspaceLink(link);
		childNode.addWorkspaceLink(link);
	    }

	    // create message rule links
	    Vector nodeSrcMessageRules = currentNode.getSrcMessageRules();
	    for (int j = 0; j < nodeSrcMessageRules.size(); j++) {
		MessageRule mr =
		    (MessageRule) nodeSrcMessageRules.elementAt(j);
		WorkspaceLink mrLink = currentNode.
		    getWorkspaceLink(mr.targetWorkspace,
				     WorkspaceLink.MESSAGERULE);
		if (mrLink != null) {
		    mrLink.weight++;
		    mrLink.addLinkObject(mr);
		} else {
		    mrLink =
			new WorkspaceLink(currentNode, mr.targetWorkspace,
					  WorkspaceLink.MESSAGERULE, mr,
					  WorkspaceLink.TO, 1,
					  initialEdgeLength);
		    currentNode.addWorkspaceLink(mrLink);
		    mr.targetWorkspace.addWorkspaceLink(mrLink);
		}
	    }

	} // for

	// Pass 2: Create, or update, workspace links

	// create goal links
	for (int g = 0; g < goals.size(); g++) {
	    Vector goalWorkspaces =
		(Vector) ((Object[]) goals.elementAt(g))[1];
	    for (int i = 0; i < (goalWorkspaces.size() - 1); i++) {
		WorkspaceNode oneWs =
		    (WorkspaceNode) goalWorkspaces.elementAt(i);
		for (int j = (i + 1); j < goalWorkspaces.size(); j++) {
		    WorkspaceNode otherWs =
			(WorkspaceNode) goalWorkspaces.elementAt(j);
		    WorkspaceLink goalLink =
			new WorkspaceLink(oneWs, otherWs, WorkspaceLink.GOAL,
					  WorkspaceLink.NONE, 1,
					  initialEdgeLength);
		    oneWs.addWorkspaceLink(goalLink);
		    otherWs.addWorkspaceLink(goalLink);
		}
	    }
	}

	// create document links
	for (int d = 0; d < documents.size(); d++) {
	    Vector documentWorkspaces =
		(Vector) ((Object[]) documents.elementAt(d))[1];
	    String doc = (String) ((Object[]) documents.elementAt(d))[0];
	    for (int i = 0; i < (documentWorkspaces.size() - 1); i++) {
		WorkspaceNode oneWs =
		    (WorkspaceNode) documentWorkspaces.elementAt(i);
		for (int j = (i + 1); j < documentWorkspaces.size(); j++) {
		    WorkspaceNode otherWs =
			(WorkspaceNode) documentWorkspaces.elementAt(j);
		    WorkspaceLink documentLink = oneWs.
			getWorkspaceLink(otherWs, WorkspaceLink.DOCUMENT);
		    if (documentLink != null) {
			documentLink.weight++;
			documentLink.addLinkObject(doc);
		    } else {
			documentLink =
			    new WorkspaceLink(oneWs, otherWs,
					      WorkspaceLink.DOCUMENT, doc,
					      WorkspaceLink.NONE, 1,
					      initialEdgeLength);
			oneWs.addWorkspaceLink(documentLink);
			otherWs.addWorkspaceLink(documentLink);
		    }
		}
	    }
	}

	// create discussion links
	for (int d = 0; d < discussions.size(); d++) {
	    Vector discussionWorkspaces =
		(Vector) ((Object[]) discussions.elementAt(d))[1];
	    String disc = (String) ((Object[]) discussions.elementAt(d))[0];
	    for (int i = 0; i < (discussionWorkspaces.size() - 1); i++) {
		WorkspaceNode oneWs =
		    (WorkspaceNode) discussionWorkspaces.elementAt(i);
		for (int j = (i + 1); j < discussionWorkspaces.size(); j++) {
		    WorkspaceNode otherWs =
			(WorkspaceNode) discussionWorkspaces.elementAt(j);
		    WorkspaceLink discussionLink = oneWs.
			getWorkspaceLink(otherWs, WorkspaceLink.DISCUSSION);
		    if (discussionLink != null) {
			discussionLink.weight++;
			discussionLink.addLinkObject(disc);
		    } else {
			discussionLink =
			    new WorkspaceLink(oneWs, otherWs,
					      WorkspaceLink.DISCUSSION, disc,
					      WorkspaceLink.NONE, 1,
					      initialEdgeLength);
			oneWs.addWorkspaceLink(discussionLink);
			otherWs.addWorkspaceLink(discussionLink);
		    }
		}
	    }
	}

	// create action links
	for (int a = 0; a < actions.size(); a++) {
	    Vector actionWorkspaces =
		(Vector) ((Object[]) actions.elementAt(a))[1];
	    String act = (String) ((Object[]) actions.elementAt(a))[0];
	    for (int i = 0; i < (actionWorkspaces.size() - 1); i++) {
		WorkspaceNode oneWs =
		    (WorkspaceNode) actionWorkspaces.elementAt(i);
		for (int j = (i + 1); j < actionWorkspaces.size(); j++) {
		    WorkspaceNode otherWs =
			(WorkspaceNode) actionWorkspaces.elementAt(j);
		    WorkspaceLink actionLink = oneWs.
			getWorkspaceLink(otherWs, WorkspaceLink.ACTION);
		    if (actionLink != null) {
			actionLink.weight++;
			actionLink.addLinkObject(act);
		    } else {
			actionLink =
			    new WorkspaceLink(oneWs, otherWs,
					      WorkspaceLink.ACTION, act,
					      WorkspaceLink.NONE, 1,
					      initialEdgeLength);
			oneWs.addWorkspaceLink(actionLink);
			otherWs.addWorkspaceLink(actionLink);
		    }
		}
	    }
	}

	// create participant links
	for (int p = 0; p < participants.size(); p++) {
	    Vector participantWorkspaces =
		(Vector) ((Object[]) participants.elementAt(p))[1];
	    String part = (String) ((Object[]) participants.elementAt(p))[0];
	    for (int i = 0; i < (participantWorkspaces.size() - 1); i++) {
		WorkspaceNode oneWs =
		    (WorkspaceNode) participantWorkspaces.elementAt(i);
		for (int j = (i + 1); j < participantWorkspaces.size(); j++) {
		    WorkspaceNode otherWs =
			(WorkspaceNode) participantWorkspaces.elementAt(j);
		    WorkspaceLink participantLink = oneWs.
			getWorkspaceLink(otherWs, WorkspaceLink.PARTICIPANT);
		    if (participantLink != null) {
			participantLink.weight++;
			participantLink.addLinkObject(part);
		    } else {
			participantLink =
			    new WorkspaceLink(oneWs, otherWs,
					      WorkspaceLink.PARTICIPANT, part,
					      WorkspaceLink.NONE, 1,
					      initialEdgeLength);
			oneWs.addWorkspaceLink(participantLink);
			otherWs.addWorkspaceLink(participantLink);
		    }
		}
	    }
	}
    }


    private WorkspaceNode getWorkspace(String workspace, String owner)
    {
	for (int i = 0; i < workspaces.size(); i++) {
	    WorkspaceNode ws = (WorkspaceNode) workspaces.elementAt(i);
	    if (ws.isSpecial())
		continue;
	    String wsWorkspace = ws.getWorkspace();
	    String wsOwner = ws.getOwner();
	    if (wsWorkspace.equalsIgnoreCase(workspace) &&
		wsOwner.equalsIgnoreCase(owner))
		return ws;
	}
	System.err.println("getWorkspace(" + workspace + ", " + owner +
			   "): null!");
	
	return null;
    }


    private Role getRole(WorkspaceNode workspace, String role)
    {
	for (int i = 0; i < roles.size(); i++) {
	    Role r = (Role) roles.elementAt(i);
	    if (r == null)
		System.err.println("Role is null!");
	    if (r.workspace == null)
		System.err.println("Workspace is null!");
	    if (r.workspace.equals(workspace) &&
		r.name.toLowerCase().equals(role.toLowerCase()))
		return r;
	}
	return null;
    }


    private WorkspaceObject getWorkspaceObject(WorkspaceNode workspace,
					       String workspaceObject)
    {
	for (int i = 0; i < documents.size(); i++) {
	    Document d = (Document) documents.elementAt(i);
	    if (d.workspace.equals(workspace) &&
		d.name.equals(workspaceObject))
		return d;
	}
	for (int i = 0; i < discussions.size(); i++) {
	    Discussion d = (Discussion) discussions.elementAt(i);
	    if (d.workspace.equals(workspace) &&
		d.name.equals(workspaceObject))
		return d;
	}
	for (int i = 0; i < actions.size(); i++) {
	    Action a = (Action) actions.elementAt(i);
	    if (a.workspace.equals(workspace) &&
		a.name.equals(workspaceObject))
		return a;
	}
	return null;
    }


    public MessageType getMessageType(WorkspaceNode workspace, String type)
    {
	for (int i = 0; i < messagetypes.size(); i++) {
	    MessageType m = (MessageType) messagetypes.elementAt(i);
	    if (m.workspace.equals(workspace) && m.type.equals(type))
		return m;
	}
	return null;
    }


    public MessageRule getMessageRule(MessageType srcType,
				      MessageType targetType, Role targetRole)
    {
	for (int i = 0; i < messagerules.size(); i++) {
	    MessageRule m = (MessageRule) messagerules.elementAt(i);
	    if (m.srcType.equals(srcType) && m.targetType.equals(targetType) &&
		m.targetRole.equals(targetRole))
		return m;
	}
	return null;
    }


    void saveTree()
    {
	try {
	    File treeFile = new File(treeFileName);
	    FileOutputStream treeFos = new FileOutputStream(treeFile);
	    ObjectOutputStream treeOos = new ObjectOutputStream(treeFos);

	    treeOos.writeObject(rootNode);

	    treeOos.close();
	    treeFos.close();
	}
	catch (Exception ex) {
	    System.err.println("saveTree: " + ex.getMessage());
	}
    }


    private void readTree()
    {
	try {
	    File treeFile = new File(treeFileName);

	    if (! treeFile.exists()) {
		System.err.println("readTree: can't open tree data file " + treeFileName);
		return;
	    }
	    
	    FileInputStream treeFis = new FileInputStream(treeFile);
	    ObjectInputStream treeOis = new ObjectInputStream(treeFis);

	    rootNode = (WorkspaceNode) treeOis.readObject();

	    treeOis.close();
	    treeFis.close();
	}
	catch (Exception ex) {
	    System.err.println(ex.getMessage());
	}
    }
}
