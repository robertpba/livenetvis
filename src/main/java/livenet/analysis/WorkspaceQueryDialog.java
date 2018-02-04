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
import javax.swing.*;


public class WorkspaceQueryDialog extends JDialog implements ActionListener
{
    private JRadioButton radio1 =
	new JRadioButton("Show all workspaces", true);
    private JRadioButton radio2 =
	new JRadioButton("Show selected workspaces:");
    private JCheckBox check1 = new JCheckBox("Created by:");
    private JCheckBox check2 = new JCheckBox("Involving participant(s):");
    private JCheckBox check3 = new JCheckBox("Involving documents(s):");
    private JCheckBox check5 = new JCheckBox("Involving action(s):");
    private JCheckBox check6 = new JCheckBox("Involving discussion(s):");
    private JComboBox combo1 = new JComboBox();
    private JButton button1 = new JButton("Participants");
    private JButton button2 = new JButton("Documents");
    private JButton button4 = new JButton("Actions");
    private JButton button5 = new JButton("Discussions");
    private JLabel label1 = new JLabel(" ");
    private JLabel label2 = new JLabel(" ");
    private JLabel label3 = new JLabel(" ");
    private JLabel label4 = new JLabel(" ");
    private JLabel label5 = new JLabel(" ");
    private JButton showButton = new JButton("Show");
    private JButton cancelButton = new JButton("Cancel");
    private String query;
    

    public WorkspaceQueryDialog()
    {
	GridBagLayout gbl1 = new GridBagLayout();
	GridBagLayout gbl2 = new GridBagLayout();
	GridBagLayout gbl3 = new GridBagLayout();
	GridBagLayout gbl4 = new GridBagLayout();

	this.getContentPane().setLayout(gbl1);
	JPanel panel1 = new JPanel(gbl2);
	JPanel panel2 = new JPanel(gbl3);
	JPanel panel3 = new JPanel(gbl4);

	GridBagConstraints constr = new GridBagConstraints();

	// Set up borders for buttons and labels
	button1.setBorder(BorderFactory.createRaisedBevelBorder());
	button2.setBorder(BorderFactory.createRaisedBevelBorder());
	button4.setBorder(BorderFactory.createRaisedBevelBorder());
	button5.setBorder(BorderFactory.createRaisedBevelBorder());
	showButton.setBorder(BorderFactory.createRaisedBevelBorder());
	cancelButton.setBorder(BorderFactory.createRaisedBevelBorder());

	label1.setBorder(BorderFactory.createLoweredBevelBorder());
	label2.setBorder(BorderFactory.createLoweredBevelBorder());
	label3.setBorder(BorderFactory.createLoweredBevelBorder());
	label4.setBorder(BorderFactory.createLoweredBevelBorder());
	label5.setBorder(BorderFactory.createLoweredBevelBorder());

	// Set up action listeners for all action sources
	radio1.addActionListener(this);
	radio2.addActionListener(this);
	check1.addActionListener(this);
	check2.addActionListener(this);
	check3.addActionListener(this);
	check5.addActionListener(this);
	check6.addActionListener(this);
	combo1.addActionListener(this);
	button1.addActionListener(this);
	button2.addActionListener(this);
	button4.addActionListener(this);
	button5.addActionListener(this);
	showButton.addActionListener(this);
	cancelButton.addActionListener(this);

	// Initialize the state of all components
	check1.setEnabled(false);
	check2.setEnabled(false);
	check3.setEnabled(false);
	check5.setEnabled(false);
	check6.setEnabled(false);
	combo1.setEnabled(false);
	button1.setEnabled(false);
	button2.setEnabled(false);
	button4.setEnabled(false);
	button5.setEnabled(false);
	combo1.setEnabled(false);
	button1.setEnabled(false);
	button2.setEnabled(false);
	button4.setEnabled(false);
	button5.setEnabled(false);

	// The two main radio button controls at the top
	constr.anchor = GridBagConstraints.WEST;
	constr.fill = GridBagConstraints.NONE;
	constr.gridwidth = GridBagConstraints.REMAINDER;
	constr.insets = new Insets(10, 20, 0, 20);
	constr.weightx = 0.0;
	constr.weighty = 0.0;

	gbl1.setConstraints(radio1, constr);
	this.getContentPane().add(radio1);

	constr.insets = new Insets(0, 20, 0, 20);

	gbl1.setConstraints(radio2, constr);
	this.getContentPane().add(radio2);

	ButtonGroup bg = new ButtonGroup();
	bg.add(radio1);
	bg.add(radio2);

	// The creator selection option
	constr.anchor = GridBagConstraints.WEST;
	constr.insets = new Insets(0, 0, 0, 0);
	constr.gridwidth = GridBagConstraints.RELATIVE;

	gbl2.setConstraints(check1, constr);
	panel1.add(check1);

	constr.fill = GridBagConstraints.HORIZONTAL;
	constr.gridwidth = GridBagConstraints.REMAINDER;
	constr.insets = new Insets(0, 0, 10, 0);
	constr.weightx = 1.0;

	gbl2.setConstraints(combo1, constr);
	panel1.add(combo1);

	// The participants selection option
	constr.fill = GridBagConstraints.NONE;
	constr.gridwidth = GridBagConstraints.RELATIVE;
	constr.insets = new Insets(0, 0, 0, 0);

	gbl3.setConstraints(check2, constr);
	panel2.add(check2);

	constr.fill = GridBagConstraints.HORIZONTAL;
	constr.gridwidth = GridBagConstraints.REMAINDER;
	constr.gridheight = 1;
	constr.ipadx = 10;
	
	gbl3.setConstraints(button1, constr);
	panel2.add(button1);

	constr.insets = new Insets(2, 20, 10, 0);
	constr.gridwidth = GridBagConstraints.REMAINDER;
	constr.gridheight = 1;
	constr.ipadx = 0;

	gbl3.setConstraints(label1, constr);
	panel2.add(label1);

	// The documents selection option
	constr.fill = GridBagConstraints.NONE;
	constr.gridwidth = GridBagConstraints.RELATIVE;
	constr.insets = new Insets(0, 0, 0, 0);

	gbl3.setConstraints(check3, constr);
	panel2.add(check3);

	constr.fill = GridBagConstraints.HORIZONTAL;
	constr.gridwidth = GridBagConstraints.REMAINDER;
	constr.gridheight = 1;
	constr.ipadx = 10;
	
	gbl3.setConstraints(button2, constr);
	panel2.add(button2);

	constr.insets = new Insets(2, 20, 10, 0);
	constr.gridwidth = GridBagConstraints.REMAINDER;
	constr.gridheight = 1;
	constr.ipadx = 0;

	gbl3.setConstraints(label2, constr);
	panel2.add(label2);

	// The actions selection option
	constr.fill = GridBagConstraints.NONE;
	constr.gridwidth = GridBagConstraints.RELATIVE;
	constr.insets = new Insets(0, 0, 0, 0);

	gbl3.setConstraints(check5, constr);
	panel2.add(check5);

	constr.fill = GridBagConstraints.HORIZONTAL;
	constr.gridwidth = GridBagConstraints.REMAINDER;
	constr.gridheight = 1;
	constr.ipadx = 10;
	
	gbl3.setConstraints(button4, constr);
	panel2.add(button4);

	constr.insets = new Insets(2, 20, 10, 0);
	constr.gridwidth = GridBagConstraints.REMAINDER;
	constr.gridheight = 1;
	constr.ipadx = 0;

	gbl3.setConstraints(label4, constr);
	panel2.add(label4);

	// The discussions selection option
	constr.fill = GridBagConstraints.NONE;
	constr.gridwidth = GridBagConstraints.RELATIVE;
	constr.insets = new Insets(0, 0, 0, 0);

	gbl3.setConstraints(check6, constr);
	panel2.add(check6);

	constr.fill = GridBagConstraints.HORIZONTAL;
	constr.gridwidth = GridBagConstraints.REMAINDER;
	constr.gridheight = 1;
	constr.ipadx = 10;
	
	gbl3.setConstraints(button5, constr);
	panel2.add(button5);

	constr.insets = new Insets(2, 20, 10, 0);
	constr.gridwidth = GridBagConstraints.REMAINDER;
	constr.gridheight = 1;
	constr.ipadx = 0;

	gbl3.setConstraints(label5, constr);
	panel2.add(label5);

	// Add the two panels to the main window
	constr.anchor = GridBagConstraints.WEST;
	constr.fill = GridBagConstraints.HORIZONTAL;
	constr.gridwidth = GridBagConstraints.REMAINDER;
	constr.insets = new Insets(0, 40, 0, 20);
	constr.weightx = 1.0;

	gbl1.setConstraints(panel1, constr);
	this.getContentPane().add(panel1);
	gbl1.setConstraints(panel2, constr);
	this.getContentPane().add(panel2);

	// Make a panel for the two main control buttons
	constr.fill = GridBagConstraints.NONE;
	constr.gridwidth = GridBagConstraints.RELATIVE;
	constr.insets = new Insets(0, 40, 0, 20);
	constr.ipadx = 10;
	constr.ipady = 5;
	constr.weightx = 0.0;

	gbl4.setConstraints(showButton, constr);
	panel3.add(showButton);

	constr.gridwidth = GridBagConstraints.REMAINDER;
	gbl4.setConstraints(cancelButton, constr);
	panel3.add(cancelButton);

	constr.anchor = GridBagConstraints.CENTER;
	constr.insets = new Insets(10, 0, 10, 0);
	constr.ipadx = 0;
	constr.ipady = 0;

	gbl1.setConstraints(panel3, constr);
	this.getContentPane().add(panel3);

	// Done, now show the whole lot on the screen
	this.pack();
	this.setResizable(false);
	this.setTitle("Show Workspaces...");
	this.setVisible(true);
    }


    public void actionPerformed(ActionEvent e)
    {
	Object source = e.getSource();

	if (source == radio1) {
	    check1.setEnabled(false);
	    check2.setEnabled(false);
	    check3.setEnabled(false);
	    check5.setEnabled(false);
	    check6.setEnabled(false);
	    combo1.setEnabled(false);
	    button1.setEnabled(false);
	    button2.setEnabled(false);
	    button4.setEnabled(false);
	    button5.setEnabled(false);
	} else if (source == radio2) {
	    check1.setEnabled(true);
	    check2.setEnabled(true);
	    check3.setEnabled(true);
	    check5.setEnabled(true);
	    check6.setEnabled(true);
	    combo1.setEnabled(check1.isSelected());
	    button1.setEnabled(check2.isSelected());
	    button2.setEnabled(check3.isSelected());
	    button4.setEnabled(check5.isSelected());
	    button5.setEnabled(check6.isSelected());
	} else if (source == check1) {
	    combo1.setEnabled(check1.isSelected());
	} else if (source == check2) {
	    button1.setEnabled(check2.isSelected());
	} else if (source == check3) {
	    button2.setEnabled(check3.isSelected());
	} else if (source == check5) {
	    button4.setEnabled(check5.isSelected());
	} else if (source == check6) {
	    button5.setEnabled(check6.isSelected());
	} else if (source == showButton) {
	    query = constructQuery();
	    this.dispose();
	} else if (source == cancelButton) {
	    query = null;
	    this.dispose();
	}
    }


    private String constructQuery()
    {
	return "";
    }


    public String getQuery()
    {
	return query;
    }


    public static void main(String[] args)
    {
	new WorkspaceQueryDialog();
    }
}
