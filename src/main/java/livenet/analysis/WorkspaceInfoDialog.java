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
import java.awt.event.*;
import java.text.NumberFormat;
import java.util.HashSet;
import java.util.Vector;
import javax.swing.*;


public class WorkspaceInfoDialog extends JDialog implements ActionListener
{
    JButton okButton = new JButton("OK");

    public WorkspaceInfoDialog(WorkspaceNode node)
    {
	this.setTitle(node.getWorkspace() + "[" + node.getOwner() + "]");
	String workspace = node.getWorkspace();
	String owner = node.getOwner();
	String workgroup = node.getWorkgroup();
	String creationDate = node.getCreationDateTime().toString();
	int age = node.getCreationDateTime().getDaysDiff(new DateTime());
	int numRoles = node.getRoles().size();
	int numParticipants = node.getParticipants().size();
	int numActions = node.getActions().size();
	int numDiscussions = node.getDiscussions().size();
	int numDocuments = node.getDocuments().size();
	int numMessageTypes = node.getMessageTypes().size();
	int numMessageRules = node.getSrcMessageRules().size();
	int numMessages = node.getSentMessages().size();

	Vector workspaceLinks = node.getWorkspaceLinks();

	HashSet sharedActions = new HashSet();
	HashSet sharedDiscussions = new HashSet();
	HashSet sharedDocuments = new HashSet();
	HashSet sharedParticipants = new HashSet();

	HashSet sharedActionWS = new HashSet();
	HashSet sharedDiscussionWS = new HashSet();
	HashSet sharedDocumentWS = new HashSet();
	HashSet sharedParticipantWS = new HashSet();

	for (int i = 0; i < workspaceLinks.size(); i++) {
	    WorkspaceLink link = (WorkspaceLink) workspaceLinks.elementAt(i);
	    WorkspaceNode otherWs = (link.fromNode.equals(node) ?
				     link.toNode : link.fromNode);
	    switch (link.type) {
	    case WorkspaceLink.ACTION :
		for (int j = 0; j < link.linkObjects.size(); j++)
		    sharedActions.add(link.linkObjects.elementAt(j));
		sharedActionWS.add(otherWs);
		break;
	    case WorkspaceLink.DISCUSSION :
		for (int j = 0; j < link.linkObjects.size(); j++)
		    sharedDiscussions.add(link.linkObjects.elementAt(j));
		sharedDiscussionWS.add(otherWs);
		break;
	    case WorkspaceLink.DOCUMENT :
		for (int j = 0; j < link.linkObjects.size(); j++)
		    sharedDocuments.add(link.linkObjects.elementAt(j));
		sharedDocumentWS.add(otherWs);
		break;
	    case WorkspaceLink.PARTICIPANT :
		for (int j = 0; j < link.linkObjects.size(); j++)
		    sharedParticipants.add(link.linkObjects.elementAt(j));
		sharedParticipantWS.add(otherWs);
		break;
	    }
	}

	int numSharedActions = sharedActions.size();
	int numSharedDiscussions = sharedDiscussions.size();
	int numSharedDocuments = sharedDocuments.size();
	int numSharedParticipants = sharedParticipants.size();

	int numSharedActionsWS = sharedActionWS.size();
	int numSharedDiscussionsWS = sharedDiscussionWS.size();
	int numSharedDocumentsWS = sharedDocumentWS.size();
	int numSharedParticipantsWS = sharedParticipantWS.size();

	int numSubworkspaces = node.getChildCount();

	int numSameGoalSubs = 0;
	String goal = node.getGoal();
	if (goal != null) {
	    Vector childWorkspaces = node.getChildren();
	    for (int i = 0; i < childWorkspaces.size(); i++) {
		if (((WorkspaceNode) childWorkspaces.elementAt(i)).
		    getGoal().equals(goal))
		    numSameGoalSubs++;
	    }
	}

	int numIntExtMR = 0;
	Vector srcMessageRules = node.getSrcMessageRules();
	for (int i = 0; i < srcMessageRules.size(); i++) {
	    if (((MessageRule) srcMessageRules.elementAt(i)).srcWorkspace ==
		node &&
		((MessageRule) srcMessageRules.elementAt(i)).targetWorkspace !=
		node)
		numIntExtMR++;
	}

	int numExtIntMR = 0;
	Vector targetMessageRules = node.getTargetMessageRules();
	for (int i = 0; i < targetMessageRules.size(); i++) {
	    if (((MessageRule) targetMessageRules.elementAt(i)).srcWorkspace !=
		node &&
		((MessageRule) targetMessageRules.elementAt(i)).targetWorkspace
		== node)
		numExtIntMR++;
	}

	int sentIntMessages = 0;
	int sentExtMessages = 0;
	Vector sentMessages = node.getSentMessages();
	for (int i = 0; i < sentMessages.size(); i++) {
	    Message msg = (Message) sentMessages.elementAt(i);
	    if (msg.srcWorkspace == node) {
		if (msg.targetWorkspace == node)
		    sentIntMessages++;
		else
		    sentExtMessages++;
	    }
	}

	int rcvdExtMessages = 0;
	Vector rcvdMessages = node.getReceivedMessages();
	for (int i = 0; i < rcvdMessages.size(); i++) {
	    Message msg = (Message) rcvdMessages.elementAt(i);
	    if (msg.srcWorkspace != node && msg.targetWorkspace == node)
		rcvdExtMessages++;
	}

	int absD = node.getAbsDensity();
	int minD = node.getMinRoleDensity();
	int maxD = node.getMaxRoleDensity();
	float meanD = node.getMeanRoleDensity();
	float evolI = node.getEvolutionIntensity();
	float evolR = node.getEvolutionRecency();
	float msgI = node.getMessageIntensity();
	float msgR = node.getMessageRecency();

	NumberFormat formatter;
	formatter = NumberFormat.getNumberInstance();

	JTextArea resultText = new JTextArea();
	resultText.setFont(new Font("Courier", Font.PLAIN, 11));

	resultText.append("Workspace Information\n");
	resultText.append("=====================\n");
	resultText.append("\n");
	resultText.append("Workspace:  " + workspace + "\n");
	resultText.append("Owner:      " + owner + "\n");
	resultText.append("Workgroup:  " + workgroup + "\n");
	resultText.append("Created on: " + creationDate + "\n");
	resultText.append("Age:        " + age + " days\n");
	resultText.append("\n");
	resultText.append("Workspace-Internal Objects\n");
	resultText.append("--------------------------\n");
	resultText.append("Roles:         " + numRoles + "\n");
	resultText.append("Participants:  " + numParticipants + "\n");
	resultText.append("Actions:       " + numActions + "\n");
	resultText.append("Discussions:   " + numDiscussions + "\n");
	resultText.append("Documents:     " + numDocuments + "\n");
	resultText.append("Message Types: " + numMessageTypes + "\n");
	resultText.append("Message Rules: " + numMessageRules + "\n");
	resultText.append("Messages:      " + sentIntMessages +
			  " (intra-workspace)\n");
	resultText.append("\n");
	resultText.append("Cross-Workspace Relationships\n");
	resultText.append("-----------------------------\n");
	resultText.append("Actions in common with other workspaces:       " +
			  numSharedActions + "\n");
	resultText.append(" - workspaces actions are in common with:      " +
			  numSharedActionsWS + "\n");
	resultText.append("Discussions in common with other workspaces:   " +
			  numSharedDiscussions + "\n");
	resultText.append(" - workspaces discussions are in common with:  " +
			  numSharedDiscussionsWS + "\n");
	resultText.append("Documents in common with other workspaces:     " +
			  numSharedDocuments + "\n");
	resultText.append(" - workspaces documents are in common with:    " +
			  numSharedDocumentsWS + "\n");
	resultText.append("Participants in common with other workspaces:  " +
			  numSharedParticipants + "\n");
	resultText.append(" - workspaces participants are in common with: " +
			  numSharedParticipantsWS + "\n");
	resultText.append("Subworkspaces:                                 " +
			  numSubworkspaces + "\n");
	resultText.append(" - with same goal as this workspace:           " +
			  numSameGoalSubs + "\n");
	resultText.append("Internal message rules with external targets:  " +
			  numIntExtMR + "\n");
	resultText.append("External message rules with internal targets:  " +
			  numExtIntMR + "\n");
	resultText.append("Messages sent to other workspaces:             " +
			  sentExtMessages + "\n");
	resultText.append("Messages received from other workspaces:       " +
			  rcvdExtMessages + "\n");
	resultText.append("\n");
	resultText.append("Workspace Statistics\n");
	resultText.append("--------------------\n");
	resultText.append("Absolute Workspace Density: " + absD +
			  " objects\n");
	resultText.append("Min. Workspace Density:     " + minD +
			  " objects assigned to role\n");
	resultText.append("Max. Workspace Density:     " + maxD +
			  " objects assigned to role\n");
	resultText.append("Mean Workspace Density:     " +
			  formatter.format(meanD) +
			  " objects assigned to role\n");
	resultText.append("Evolution Intensity:        " +
			  formatter.format(evolI) + " new objects per week\n");
	resultText.append("Evolution Recency:          " +
			  formatter.format(evolR) +
			  " new recent objects (deflated)\n");
	resultText.append("Message Intensity:          " +
			  formatter.format(msgI) + " new messages per week\n");
	resultText.append("Message Recency:            " +
			  formatter.format(msgR) +
			  " new recent messages (deflated)\n");

	resultText.setEditable(false);

	JScrollPane textPane = new JScrollPane(resultText);
	textPane.setPreferredSize(new Dimension(450, 300));

	resultText.setCaretPosition(0);

	okButton.addActionListener(this);

	this.getContentPane().add(BorderLayout.CENTER, textPane);
	this.getContentPane().add(BorderLayout.SOUTH, okButton);
	this.pack();
    }


    public void actionPerformed(ActionEvent e)
    {
	if (e.getSource() == okButton)
	    this.dispose();
    }    
} // WorkspaceInfoDialog
