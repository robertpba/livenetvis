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
import java.io.*;
import java.sql.*;
import java.util.List;
import java.util.Vector;
import javax.swing.*;


public class WorkgroupDialog extends JDialog implements ActionListener
{

    private Connection con;  // the database connection object
    private String workgroupQuery =
	"SELECT Name FROM DBA.WorkGroups ORDER BY Name";

    private JLabel workgroupLabel = new JLabel("Workgroups:");
    private JList workgroupList;
    private JButton okButton = new JButton("OK");
    private JButton cancelButton = new JButton("Cancel");
    private String[] selectedWorkgroups;


    public WorkgroupDialog(Frame parent, Connection con)
    {
	super(parent, "Select Workgroup(s)", true);
	this.con = con;

	workgroupList = new JList(getWorkgroups());
	workgroupList.setSelectionMode(ListSelectionModel.
				       MULTIPLE_INTERVAL_SELECTION);
	workgroupList.setSelectedIndex(0);

	JScrollPane listPane = new JScrollPane(workgroupList);

	GridBagLayout gbl = new GridBagLayout();
	GridBagConstraints constr = new GridBagConstraints();
	Container contentPane = this.getContentPane();
	contentPane.setLayout(gbl);

	constr.anchor = GridBagConstraints.WEST;
	constr.fill = GridBagConstraints.NONE;
	constr.gridwidth = GridBagConstraints.REMAINDER;
	constr.insets = new Insets(10, 10, 5, 10);

	gbl.setConstraints(workgroupLabel, constr);
	contentPane.add(workgroupLabel);

	constr.fill = GridBagConstraints.BOTH;
	constr.insets = new Insets(0, 10, 5, 10);

	gbl.setConstraints(listPane, constr);
	contentPane.add(listPane);

	JPanel buttonPanel = new JPanel(new GridLayout(1, 2, 25, 0));

	buttonPanel.add(okButton);
	buttonPanel.add(cancelButton);

	constr.anchor = GridBagConstraints.CENTER;
	constr.insets = new Insets(0, 10, 10, 10);

	gbl.setConstraints(buttonPanel, constr);
	contentPane.add(buttonPanel);

	okButton.addActionListener(this);
	cancelButton.addActionListener(this);

	this.getRootPane().setDefaultButton(okButton);

	this.pack();
	this.setLocationRelativeTo(parent);
	this.setVisible(true);
    }


    private Vector getWorkgroups()
    {
	Vector results = new Vector();
	try {
	    Statement stmt = con.createStatement();
	    ResultSet rs = stmt.executeQuery(workgroupQuery);
	    while (rs.next()) {
		String wg = rs.getString(1);
		if (wg.length() > 0)
		    results.addElement(wg);
	    }
	}
	catch (SQLException e) {
	    JOptionPane.showMessageDialog(this, "Error getting the list of " +
					  "workgroups.", "Error",
					  JOptionPane.ERROR_MESSAGE);
	}
	return results;
    }


    public String[] getSelectedWorkgroups()
    {
	return selectedWorkgroups;
    }


    public void actionPerformed(ActionEvent e)
    {
	Object source = e.getSource();
	
	if (source == okButton) {
	    List<Object> selectedValues = workgroupList.getSelectedValuesList();

	    selectedWorkgroups = new String[selectedValues.size()];

	    int i = 0;
	    
	    for (Object o : selectedValues)
		selectedWorkgroups[i++] = (String) o;

	    this.dispose();
	}
	else if (source == cancelButton) {
	    selectedWorkgroups = null;
	    this.dispose();
	}
    }
}
