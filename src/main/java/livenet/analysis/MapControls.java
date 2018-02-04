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
import java.util.Vector;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;


public class MapControls extends JFrame implements ActionListener,
    ChangeListener, ItemListener
{
    // static fields for control event types
    public static final int NODE_COL_PARENT_CHILD = 0;
    public static final int NODE_COL_ABSOLUTE_WORKSPACE_DENSITY = 1;
    public static final int NODE_COL_MINIMUM_WORKSPACE_DENSITY = 2;
    public static final int NODE_COL_MAXIMUM_WORKSPACE_DENSITY = 3;
    public static final int NODE_COL_MEAN_WORKSPACE_DENSITY = 4;
    public static final int NODE_COL_EVOLUTION_INTENSITY = 5;
    public static final int NODE_COL_EVOLUTION_RECENCY = 6;
    public static final int NODE_COL_MESSAGE_INTENSITY = 7;
    public static final int NODE_COL_MESSAGE_RECENCY = 8;
    public static final int EDGE_PULL_PARENT_CHILD = 9;
    public static final int EDGE_PULL_GOAL = 10;
    public static final int EDGE_PULL_DOCUMENT = 11;
    public static final int EDGE_PULL_DISCUSSION = 12;
    public static final int EDGE_PULL_ACTION = 13;
    public static final int EDGE_PULL_MESSAGE_RULE = 14;
    public static final int EDGE_PULL_PARTICIPANT = 15;
    public static final int EDGE_VIS_PARENT_CHILD = 16;
    public static final int EDGE_VIS_GOAL = 17;
    public static final int EDGE_VIS_DOCUMENT = 18;
    public static final int EDGE_VIS_DISCUSSION = 19;
    public static final int EDGE_VIS_ACTION = 20;
    public static final int EDGE_VIS_MESSAGE_RULE = 21;
    public static final int EDGE_VIS_PARTICIPANT = 22;
    public static final int EDGE_VIS_SUBGRAPH = 23;
    public static final int EDGE_VIS_WHOLEGRAPH = 24;
    public static final int NODE_LABELING = 25;
    public static final int EDGE_LENGTH = 26;
    public static final int EXPAND_ALL_NODES = 27;
    public static final int COLLAPSE_ALL_NODES = 28;
    public static final int START_ANIMATION = 29;
    public static final int STOP_ANIMATION = 30;
    public static final int PRINT = 31;
    public static final int CLOSE = 32;

    // static fields for edge pull / visibility values
    public static final int OFF = 0;
    public static final int ON = 1;

    // static fields for node labeling values
    public static final int SHORT_LABELS = 0;
    public static final int LONG_LABELS = 1;

    // panels for the different sections of the window
    JPanel nodeColourPanel = new JPanel(new GridLayout(0, 1, 5, 0));
    JPanel nodeLabelPanel = new JPanel(new GridLayout(1, 0, 5, 0));
    JPanel edgeVisibPanel = new JPanel(new GridLayout(0, 1, 5, 0));
    JPanel nodeVisibPanel = new JPanel(new GridLayout(1, 0, 5, 0));
    JPanel edgeLengthPanel = new JPanel();
    JPanel animationPanel = new JPanel(new GridLayout(1, 0, 5, 0));
    JPanel controlButtonPanel = new JPanel(new GridLayout(1, 0, 5, 0));

    // panels for the colour labels and check boxes
    Box edgeVisChildBox = new Box(BoxLayout.X_AXIS);
    Box edgeVisGoalBox = new Box(BoxLayout.X_AXIS);
    Box edgeVisDocBox = new Box(BoxLayout.X_AXIS);
    Box edgeVisDiscBox = new Box(BoxLayout.X_AXIS);
    Box edgeVisActBox = new Box(BoxLayout.X_AXIS);
    Box edgeVisMsgRuleBox = new Box(BoxLayout.X_AXIS);
    Box edgeVisPartBox = new Box(BoxLayout.X_AXIS);
    Box edgeScopeBox = new Box(BoxLayout.X_AXIS);

    // radio buttons for the node colouring panel
    JRadioButton nodeColParentBut = new JRadioButton("Parent/Child", true);
    JRadioButton nodeColAbsWDBut = new JRadioButton("Abs. Workspace Density");
    JRadioButton nodeColMinWDBut = new JRadioButton("Min. Workspace Density");
    JRadioButton nodeColMaxWDBut = new JRadioButton("Max. Workspace Density");
    JRadioButton nodeColMeanWDBut = new JRadioButton("Mean Workspace Density");
    JRadioButton nodeColEvoIntBut = new JRadioButton("Evolution Intensity");
    JRadioButton nodeColEvoRecBut = new JRadioButton("Evolution Recency");
    JRadioButton nodeColMsgIntBut = new JRadioButton("Message Intensity");
    JRadioButton nodeColMsgRecBut = new JRadioButton("Message Recency");

    // check boxes for the edge pull / visibility panel
    JCheckBox edgePullChildBut = new JCheckBox("", true);
    JCheckBox edgePullGoalBut = new JCheckBox("");
    JCheckBox edgePullDocBut = new JCheckBox("");
    JCheckBox edgePullDiscBut = new JCheckBox("");
    JCheckBox edgePullActBut = new JCheckBox("");
    JCheckBox edgePullMsgRuleBut = new JCheckBox("");
    JCheckBox edgePullPartBut = new JCheckBox("");

    JCheckBox edgeVisChildBut = new JCheckBox("Parent/Child", true);
    JCheckBox edgeVisGoalBut = new JCheckBox("Goal");
    JCheckBox edgeVisDocBut = new JCheckBox("Document");
    JCheckBox edgeVisDiscBut = new JCheckBox("Discussion");
    JCheckBox edgeVisActBut = new JCheckBox("Action");
    JCheckBox edgeVisMsgRuleBut = new JCheckBox("Message Rule");
    JCheckBox edgeVisPartBut = new JCheckBox("Participant");

    // label to precede following two radio buttons
    JLabel edgeScopeLabel = new JLabel("Apply To:");

    // radio buttons for the edge pull / visibility panel
    JRadioButton edgeSubGraphBut = new JRadioButton("Visible subgraph", true);
    JRadioButton edgeWholeGraphBut = new JRadioButton("Entire graph");

    // radio buttons for the node labeling panel
    JRadioButton nodeLabShortBut = new JRadioButton("Short", true);
    JRadioButton nodeLabLongBut = new JRadioButton("Long");

    // the node visibility buttons for the node visibility panel
    JButton nodeVisExpBut = new JButton("Expand");
    JButton nodeVisColBut = new JButton("Collapse");

    // the edge length slider for the edge length panel
    JSlider edgeLenSlider = new JSlider(JSlider.HORIZONTAL, 50, 500,
					TreeGraphConstants.initialEdgeLength);

    // radio buttons for the animation panel
    JRadioButton animationOnBut = new JRadioButton("On", true);
    JRadioButton animationOffBut = new JRadioButton("Off");

    // the major control buttons for the control button panel
    JButton printBut = new JButton("Print");
    JButton closeBut = new JButton("Close");

    // the set of control event listeners
    Vector listeners = new Vector();


    public MapControls()
    {
	super("Map Controls");

	// Set the layout manager for the whole window
	Container cont = this.getContentPane();
	GridBagLayout gbl = new GridBagLayout();
	cont.setLayout(gbl);

	// Lay out the main panels in the window

	GridBagConstraints c = new GridBagConstraints();
	c.anchor = GridBagConstraints.WEST;
	c.fill = GridBagConstraints.BOTH;
	c.gridx = 0;
	c.gridy = 0;
	c.insets = new Insets(10, 10, 0, 0);
	c.weightx = 0.0;
	c.weighty = 0.0;

	gbl.setConstraints(nodeColourPanel, c);
	cont.add(nodeColourPanel);

	c.gridy = 1;
	c.insets = new Insets(5, 10, 0, 0);
	gbl.setConstraints(nodeLabelPanel, c);
	cont.add(nodeLabelPanel);

	c.gridx = 1;
	c.gridy = 0;
	c.gridheight = 2;
	c.insets = new Insets(10, 5, 0, 10);
	gbl.setConstraints(edgeVisibPanel, c);
	cont.add(edgeVisibPanel);

	c.gridheight = 1;
	c.gridx = 0;
	c.gridy = 2;
	c.insets = new Insets(5, 10, 0, 0);
	gbl.setConstraints(animationPanel, c);
	cont.add(animationPanel);

	c.gridx = 1;
	c.insets = new Insets(5, 5, 0, 10);
	gbl.setConstraints(edgeLengthPanel, c);
	cont.add(edgeLengthPanel);

	c.gridx = 0;
	c.gridy = 3;
	c.insets = new Insets(5, 10, 10, 0);
	gbl.setConstraints(nodeVisibPanel, c);
	cont.add(nodeVisibPanel);

	c.gridx = 1;
	c.insets = new Insets(5, 5, 10, 10);
	gbl.setConstraints(controlButtonPanel, c);
	cont.add(controlButtonPanel);

	// Create borders for the main panels

	nodeColourPanel.setBorder(BorderFactory.
				  createTitledBorder("Node Colouring"));
	nodeLabelPanel.setBorder(BorderFactory.
				 createTitledBorder("Node Labels"));
	nodeVisibPanel.setBorder(BorderFactory.
				 createTitledBorder("All Nodes"));
	edgeVisibPanel.setBorder(BorderFactory.
				 createTitledBorder("Edge Pull / Visibility"));
	edgeLengthPanel.setBorder(BorderFactory.
				  createTitledBorder("Edge Length"));
	animationPanel.setBorder(BorderFactory.
				 createTitledBorder("Animation"));
	controlButtonPanel.setBorder(BorderFactory.
				     createTitledBorder("Map"));

	// Add individual components to the main panels

	nodeColourPanel.add(nodeColParentBut);
	nodeColourPanel.add(nodeColAbsWDBut);
	nodeColourPanel.add(nodeColMinWDBut);
	nodeColourPanel.add(nodeColMaxWDBut);
	nodeColourPanel.add(nodeColMeanWDBut);
	nodeColourPanel.add(nodeColEvoIntBut);
	nodeColourPanel.add(nodeColEvoRecBut);
	nodeColourPanel.add(nodeColMsgIntBut);
	nodeColourPanel.add(nodeColMsgRecBut);

	nodeLabelPanel.add(nodeLabShortBut);
	nodeLabelPanel.add(nodeLabLongBut);

	nodeVisibPanel.add(nodeVisExpBut);
	nodeVisibPanel.add(nodeVisColBut);

	animationPanel.add(animationOnBut);
	animationPanel.add(animationOffBut);

	// the edge visibility check boxes need to be arranged in panels with
	// their colour indicators
	JLabel edgeVisChildLab = new JLabel(" ");
	JLabel edgeVisGoalLab = new JLabel(" ");
	JLabel edgeVisDocLab = new JLabel(" ");
	JLabel edgeVisDiscLab = new JLabel(" ");
	JLabel edgeVisActLab = new JLabel(" ");
	JLabel edgeVisMsgRuleLab = new JLabel(" ");
	JLabel edgeVisPartLab = new JLabel(" ");

	edgeVisChildLab.setBackground(TreeGraphConstants.childColour);
	edgeVisGoalLab.setBackground(TreeGraphConstants.goalColour);
	edgeVisDocLab.setBackground(TreeGraphConstants.documentColour);
	edgeVisDiscLab.setBackground(TreeGraphConstants.discussionColour);
	edgeVisActLab.setBackground(TreeGraphConstants.actionColour);
	edgeVisMsgRuleLab.setBackground(TreeGraphConstants.messageRuleColour);
	edgeVisPartLab.setBackground(TreeGraphConstants.participantColour);

	edgeVisChildLab.setOpaque(true);
	edgeVisGoalLab.setOpaque(true);
	edgeVisDocLab.setOpaque(true);
	edgeVisDiscLab.setOpaque(true);
	edgeVisActLab.setOpaque(true);
	edgeVisMsgRuleLab.setOpaque(true);
	edgeVisPartLab.setOpaque(true);

	int h = (int) ((new JCheckBox()).getPreferredSize().height * 0.6);
	Dimension cbd = new Dimension(h, h);

	edgeVisChildLab.setMaximumSize(cbd);
	edgeVisGoalLab.setMaximumSize(cbd);
	edgeVisDocLab.setMaximumSize(cbd);
	edgeVisDiscLab.setMaximumSize(cbd);
	edgeVisActLab.setMaximumSize(cbd);
	edgeVisMsgRuleLab.setMaximumSize(cbd);
	edgeVisPartLab.setMaximumSize(cbd);

	// disable the edge pull buttons for which the corresponding
	// edge visibility buttons are not set
	edgePullGoalBut.setEnabled(false);
	edgePullDocBut.setEnabled(false);
	edgePullDiscBut.setEnabled(false);
	edgePullActBut.setEnabled(false);
	edgePullMsgRuleBut.setEnabled(false);
	edgePullPartBut.setEnabled(false);

	edgeVisChildBox.add(edgeVisChildLab);
	edgeVisChildBox.add(Box.createRigidArea(new Dimension(5, 0)));
	edgeVisChildBox.add(edgePullChildBut);
	edgeVisChildBox.add(edgeVisChildBut);

	edgeVisGoalBox.add(edgeVisGoalLab);
	edgeVisGoalBox.add(Box.createRigidArea(new Dimension(5, 0)));
	edgeVisGoalBox.add(edgePullGoalBut);
	edgeVisGoalBox.add(edgeVisGoalBut);

	edgeVisDocBox.add(edgeVisDocLab);
	edgeVisDocBox.add(Box.createRigidArea(new Dimension(5, 0)));
	edgeVisDocBox.add(edgePullDocBut);
	edgeVisDocBox.add(edgeVisDocBut);

	edgeVisDiscBox.add(edgeVisDiscLab);
	edgeVisDiscBox.add(Box.createRigidArea(new Dimension(5, 0)));
	edgeVisDiscBox.add(edgePullDiscBut);
	edgeVisDiscBox.add(edgeVisDiscBut);

	edgeVisActBox.add(edgeVisActLab);
	edgeVisActBox.add(Box.createRigidArea(new Dimension(5, 0)));
	edgeVisActBox.add(edgePullActBut);
	edgeVisActBox.add(edgeVisActBut);

	edgeVisMsgRuleBox.add(edgeVisMsgRuleLab);
	edgeVisMsgRuleBox.add(Box.createRigidArea(new Dimension(5, 0)));
	edgeVisMsgRuleBox.add(edgePullMsgRuleBut);
	edgeVisMsgRuleBox.add(edgeVisMsgRuleBut);

	edgeVisPartBox.add(edgeVisPartLab);
	edgeVisPartBox.add(Box.createRigidArea(new Dimension(5, 0)));
	edgeVisPartBox.add(edgePullPartBut);
	edgeVisPartBox.add(edgeVisPartBut);

	edgeVisibPanel.add(edgeVisChildBox);
	edgeVisibPanel.add(edgeVisGoalBox);
	edgeVisibPanel.add(edgeVisDocBox);
	edgeVisibPanel.add(edgeVisDiscBox);
	edgeVisibPanel.add(edgeVisActBox);
	edgeVisibPanel.add(edgeVisMsgRuleBox);
	edgeVisibPanel.add(edgeVisPartBox);
	edgeVisibPanel.add(edgeScopeLabel);
	edgeVisibPanel.add(edgeSubGraphBut);
	edgeVisibPanel.add(edgeWholeGraphBut);

	edgeLengthPanel.add(edgeLenSlider);

	controlButtonPanel.add(printBut);
	controlButtonPanel.add(closeBut);

	// Set up button groups for radio buttons

	ButtonGroup nodeColGroup = new ButtonGroup();
	nodeColGroup.add(nodeColParentBut);
	nodeColGroup.add(nodeColAbsWDBut);
	nodeColGroup.add(nodeColMinWDBut);
	nodeColGroup.add(nodeColMaxWDBut);
	nodeColGroup.add(nodeColMeanWDBut);
	nodeColGroup.add(nodeColEvoIntBut);
	nodeColGroup.add(nodeColEvoRecBut);
	nodeColGroup.add(nodeColMsgIntBut);
	nodeColGroup.add(nodeColMsgRecBut);

	ButtonGroup edgeScopeGroup = new ButtonGroup();
	edgeScopeGroup.add(edgeSubGraphBut);
	edgeScopeGroup.add(edgeWholeGraphBut);

	ButtonGroup nodeLabGroup = new ButtonGroup();
	nodeLabGroup.add(nodeLabShortBut);
	nodeLabGroup.add(nodeLabLongBut);

	ButtonGroup animationGroup = new ButtonGroup();
	animationGroup.add(animationOnBut);
	animationGroup.add(animationOffBut);

	// Configure the slider
	edgeLenSlider.setMajorTickSpacing(50);
	edgeLenSlider.setMinorTickSpacing(10);
	edgeLenSlider.setPaintLabels(false);
	edgeLenSlider.setPaintTicks(true);
	edgeLenSlider.setPaintTrack(true);
	edgeLenSlider.setSnapToTicks(false);

	// Set up item listeners for radio buttons and check boxes
	nodeColParentBut.addItemListener(this);
	nodeColAbsWDBut.addItemListener(this);
	nodeColMinWDBut.addItemListener(this);
	nodeColMaxWDBut.addItemListener(this);
	nodeColMeanWDBut.addItemListener(this);
	nodeColEvoIntBut.addItemListener(this);
	nodeColEvoRecBut.addItemListener(this);
	nodeColMsgIntBut.addItemListener(this);
	nodeColMsgRecBut.addItemListener(this);
	edgePullChildBut.addItemListener(this);
	edgePullGoalBut.addItemListener(this);
	edgePullDocBut.addItemListener(this);
	edgePullDiscBut.addItemListener(this);
	edgePullActBut.addItemListener(this);
	edgePullMsgRuleBut.addItemListener(this);
	edgePullPartBut.addItemListener(this);
	edgeVisChildBut.addItemListener(this);
	edgeVisGoalBut.addItemListener(this);
	edgeVisDocBut.addItemListener(this);
	edgeVisDiscBut.addItemListener(this);
	edgeVisActBut.addItemListener(this);
	edgeVisMsgRuleBut.addItemListener(this);
	edgeVisPartBut.addItemListener(this);
	edgeSubGraphBut.addItemListener(this);
	edgeWholeGraphBut.addItemListener(this);
	nodeLabShortBut.addItemListener(this);
	nodeLabLongBut.addItemListener(this);
	animationOnBut.addItemListener(this);
	animationOffBut.addItemListener(this);

	// Set up action listeners for ordinary buttons
	nodeVisExpBut.addActionListener(this);
	nodeVisColBut.addActionListener(this);
	printBut.addActionListener(this);
	closeBut.addActionListener(this);

	// Set up change listener for the slider
	edgeLenSlider.addChangeListener(this);

	// get the window up on the screen
	this.pack();
	this.setResizable(false);
	Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
	Dimension windowSize = this.getSize();
	this.setLocation(screenSize.width - windowSize.width, 0);
	this.setVisible(true);
    }


    public void actionPerformed(ActionEvent e)
    {
	Object source = e.getSource();

	int type = 0;

	if (source.equals(nodeVisExpBut))
	    type = EXPAND_ALL_NODES;
	else if (source.equals(nodeVisColBut))
	    type = COLLAPSE_ALL_NODES;
	else if (source.equals(printBut))
	    type = PRINT;
	else if (source.equals(closeBut))
	    type = CLOSE;

	ControlEvent ce = new ControlEvent(type);

	// post the event to all registered listeners
	for (int i = 0; i < listeners.size(); i++)
	    ((ControlListener) listeners.elementAt(i)).
		controlActionPerformed(ce);
    }


    public void stateChanged(ChangeEvent e)
    {
	Object source = e.getSource();

	if (source.equals(edgeLenSlider)) {
	    ControlEvent ce = new ControlEvent(EDGE_LENGTH,
					       edgeLenSlider.getValue());
	    for (int i = 0; i < listeners.size(); i++)
		((ControlListener) listeners.elementAt(i)).
		    controlActionPerformed(ce);
	}
    }


    public void itemStateChanged(ItemEvent e)
    {
	Object source = e.getItemSelectable();
	int state = e.getStateChange();

	int type = 0;
	int value = 0;

	// secondary events are needed when edge pull checkboxes need
	// to be enabled/disabled as a result of the corresponding
	// visibility checkbox being set
	boolean secondaryEvent = false;
	int secondaryType = 0;
	int secondaryValue = 0;

	if (source.equals(nodeColParentBut)) {
	    if (state == ItemEvent.DESELECTED)
		return;
	    type = NODE_COL_PARENT_CHILD;
	}
	else if (source.equals(nodeColAbsWDBut)) {
	    if (state == ItemEvent.DESELECTED)
		return;
	    type = NODE_COL_ABSOLUTE_WORKSPACE_DENSITY;
	}
	else if (source.equals(nodeColMinWDBut)) {
	    if (state == ItemEvent.DESELECTED)
		return;
	    type = NODE_COL_MINIMUM_WORKSPACE_DENSITY;
	}
	else if (source.equals(nodeColMaxWDBut)) {
	    if (state == ItemEvent.DESELECTED)
		return;
	    type = NODE_COL_MAXIMUM_WORKSPACE_DENSITY;
	}
	else if (source.equals(nodeColMeanWDBut)) {
	    if (state == ItemEvent.DESELECTED)
		return;
	    type = NODE_COL_MEAN_WORKSPACE_DENSITY;
	}
	else if (source.equals(nodeColEvoIntBut)) {
	    if (state == ItemEvent.DESELECTED)
		return;
	    type = NODE_COL_EVOLUTION_INTENSITY;
	}
	else if (source.equals(nodeColEvoRecBut)) {
	    if (state == ItemEvent.DESELECTED)
		return;
	    type = NODE_COL_EVOLUTION_RECENCY;
	}
	else if (source.equals(nodeColMsgIntBut)) {
	    if (state == ItemEvent.DESELECTED)
		return;
	    type = NODE_COL_MESSAGE_INTENSITY;
	}
	else if (source.equals(nodeColMsgRecBut)) {
	    if (state == ItemEvent.DESELECTED)
		return;
	    type = NODE_COL_MESSAGE_RECENCY;
	}
	else if (source.equals(edgePullChildBut)) {
	    type = EDGE_PULL_PARENT_CHILD;
	    value = (state == ItemEvent.SELECTED) ? ON : OFF;
	}
	else if (source.equals(edgePullGoalBut)) {
	    type = EDGE_PULL_GOAL;
	    value = (state == ItemEvent.SELECTED) ? ON : OFF;
	}
	else if (source.equals(edgePullDocBut)) {
	    type = EDGE_PULL_DOCUMENT;
	    value = (state == ItemEvent.SELECTED) ? ON : OFF;
	}
	else if (source.equals(edgePullDiscBut)) {
	    type = EDGE_PULL_DISCUSSION;
	    value = (state == ItemEvent.SELECTED) ? ON : OFF;
	}
	else if (source.equals(edgePullActBut)) {
	    type = EDGE_PULL_ACTION;
	    value = (state == ItemEvent.SELECTED) ? ON : OFF;
	}
	else if (source.equals(edgePullMsgRuleBut)) {
	    type = EDGE_PULL_MESSAGE_RULE;
	    value = (state == ItemEvent.SELECTED) ? ON : OFF;
	}
	else if (source.equals(edgePullPartBut)) {
	    type = EDGE_PULL_PARTICIPANT;
	    value = (state == ItemEvent.SELECTED) ? ON : OFF;
	}
	else if (source.equals(edgeVisChildBut)) {
	    type = EDGE_VIS_PARENT_CHILD;
	    value = (state == ItemEvent.SELECTED) ? ON : OFF;
	    edgePullChildBut.setEnabled(state == ItemEvent.SELECTED);
	    if (edgePullChildBut.isSelected()) {
		secondaryEvent = true;
		secondaryType = EDGE_PULL_PARENT_CHILD;
		secondaryValue = (state == ItemEvent.SELECTED) ? ON : OFF;
	    }
	}
	else if (source.equals(edgeVisGoalBut)) {
	    type = EDGE_VIS_GOAL;
	    value = (state == ItemEvent.SELECTED) ? ON : OFF;
	    edgePullGoalBut.setEnabled(state == ItemEvent.SELECTED);
	    if (edgePullGoalBut.isSelected()) {
		secondaryEvent = true;
		secondaryType = EDGE_PULL_GOAL;
		secondaryValue = (state == ItemEvent.SELECTED) ? ON : OFF;
	    }
	}
	else if (source.equals(edgeVisDocBut)) {
	    type = EDGE_VIS_DOCUMENT;
	    value = (state == ItemEvent.SELECTED) ? ON : OFF;
	    edgePullDocBut.setEnabled(state == ItemEvent.SELECTED);
	    if (edgePullDocBut.isSelected()) {
		secondaryEvent = true;
		secondaryType = EDGE_PULL_DOCUMENT;
		secondaryValue = (state == ItemEvent.SELECTED) ? ON : OFF;
	    }
	}
	else if (source.equals(edgeVisDiscBut)) {
	    type = EDGE_VIS_DISCUSSION;
	    value = (state == ItemEvent.SELECTED) ? ON : OFF;
	    edgePullDiscBut.setEnabled(state == ItemEvent.SELECTED);
	    if (edgePullDiscBut.isSelected()) {
		secondaryEvent = true;
		secondaryType = EDGE_PULL_DISCUSSION;
		secondaryValue = (state == ItemEvent.SELECTED) ? ON : OFF;
	    }
	}
	else if (source.equals(edgeVisActBut)) {
	    type = EDGE_VIS_ACTION;
	    value = (state == ItemEvent.SELECTED) ? ON : OFF;
	    edgePullActBut.setEnabled(state == ItemEvent.SELECTED);
	    if (edgePullActBut.isSelected()) {
		secondaryEvent = true;
		secondaryType = EDGE_PULL_ACTION;
		secondaryValue = (state == ItemEvent.SELECTED) ? ON : OFF;
	    }
	}
	else if (source.equals(edgeVisMsgRuleBut)) {
	    type = EDGE_VIS_MESSAGE_RULE;
	    value = (state == ItemEvent.SELECTED) ? ON : OFF;
	    edgePullMsgRuleBut.setEnabled(state == ItemEvent.SELECTED);
	    if (edgePullMsgRuleBut.isSelected()) {
		secondaryEvent = true;
		secondaryType = EDGE_PULL_MESSAGE_RULE;
		secondaryValue = (state == ItemEvent.SELECTED) ? ON : OFF;
	    }
	}
	else if (source.equals(edgeVisPartBut)) {
	    type = EDGE_VIS_PARTICIPANT;
	    value = (state == ItemEvent.SELECTED) ? ON : OFF;
	    edgePullPartBut.setEnabled(state == ItemEvent.SELECTED);
	    if (edgePullPartBut.isSelected()) {
		secondaryEvent = true;
		secondaryType = EDGE_PULL_PARTICIPANT;
		secondaryValue = (state == ItemEvent.SELECTED) ? ON : OFF;
	    }
	}
	else if (source.equals(edgeSubGraphBut)) {
	    type = EDGE_VIS_SUBGRAPH;
	}
	else if (source.equals(edgeWholeGraphBut)) {
	    type = EDGE_VIS_WHOLEGRAPH;
	}
	else if (source.equals(nodeLabShortBut)) {
	    if (state == ItemEvent.DESELECTED)
		return;
	    type = NODE_LABELING;
	    value = SHORT_LABELS;
	}
	else if (source.equals(nodeLabLongBut)) {
	    if (state == ItemEvent.DESELECTED)
		return;
	    type = NODE_LABELING;
	    value = LONG_LABELS;
	}
	else if (source.equals(animationOnBut)) {
	    type = START_ANIMATION;
	}
	else if (source.equals(animationOffBut)) {
	    type = STOP_ANIMATION;
	}

	ControlEvent ce = new ControlEvent(type, value);

	ControlEvent sce = null;
	if (secondaryEvent)
	    sce = new ControlEvent(secondaryType, secondaryValue);

	// post the event(s) to all registered listeners
	for (int i = 0; i < listeners.size(); i++) {
	    ((ControlListener) listeners.elementAt(i)).
		controlActionPerformed(ce);
	    if (secondaryEvent)
		((ControlListener) listeners.elementAt(i)).
		    controlActionPerformed(sce);
	}
    }


    public void addControlListener(ControlListener listener)
    {
	listeners.addElement(listener);
    }


    public static void main(String[] args)
    {
	new MapControls();
    }
}
