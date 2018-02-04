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
import java.sql.*;
import java.util.Vector;
import javax.swing.*;
import javax.swing.event.*;


public class TableDataSelectionDialog extends JDialog implements ActionListener
{
    private JTable table;
    private JButton okButton = new JButton("OK");
    private JButton cancelButton = new JButton("Cancel");
    private int selectedRow;


    public TableDataSelectionDialog(Connection con, String query,
				    Vector columnNames, String title,
				    JFrame parent)
    {
	Vector rowData = new Vector();
	// Read all the result data into a Vector of Vectors
	try {
	    Statement stmt = con.createStatement();
	    ResultSet rs = stmt.executeQuery(query);
	    int colCount = columnNames.size();
	    while (rs.next()) {
		Vector newRow = new Vector();
		for (int i = 0; i < colCount; i++)
		    newRow.addElement(rs.getString(i + 1));
		rowData.addElement(newRow);
	    }

	    // Close the statement object
	    stmt.close();
	}
	catch (SQLException ex) {
	    System.out.println(ex.getMessage());
	    return;
	}

	table = new JTable(rowData, columnNames);
	JScrollPane scrollPane = new JScrollPane(table);

	// Set up a list selection listener for the table
	ListSelectionModel rowSM = table.getSelectionModel();
	rowSM.addListSelectionListener(new ListSelectionListener() {
	    public void valueChanged(ListSelectionEvent e) {
		ListSelectionModel lsm = (ListSelectionModel)e.getSource();
		if (lsm.isSelectionEmpty()) {
		    System.out.println("No rows are selected.");
		} else {
		    selectedRow = lsm.getMinSelectionIndex();
		}
	    }
	});

	Container contentPane = this.getContentPane();
	GridBagLayout gbl = new GridBagLayout();
	contentPane.setLayout(gbl);

	GridBagConstraints constr = new GridBagConstraints();

	constr.fill = GridBagConstraints.BOTH;
	constr.gridwidth = GridBagConstraints.REMAINDER;
	constr.insets = new Insets(15, 10, 15, 10);
	constr.weightx = 1.0;
	constr.weighty = 1.0;
	gbl.setConstraints(scrollPane, constr);
	contentPane.add(scrollPane);

	JPanel buttonPanel = new JPanel(new GridLayout());
	buttonPanel.add(okButton);
	buttonPanel.add(cancelButton);

	constr.anchor = GridBagConstraints.CENTER;
	constr.fill = GridBagConstraints.NONE;
	constr.gridwidth = GridBagConstraints.REMAINDER;
	constr.insets = new Insets(0, 10, 15, 10);
	constr.weightx = 0.0;
	constr.weighty = 0.0;
	gbl.setConstraints(buttonPanel, constr);
	contentPane.add(buttonPanel);

	okButton.addActionListener(this);
	cancelButton.addActionListener(this);
    }


    public void actionPerformed(ActionEvent e)
    {
	// This is the component that created the event
	Object source = e.getSource();

	// Return a value corresponding to the button that was pressed
	if (source == okButton) {
	    //return this.getSelections();
	} else if (source == cancelButton) {
	    //return null;
	}
    }
}
