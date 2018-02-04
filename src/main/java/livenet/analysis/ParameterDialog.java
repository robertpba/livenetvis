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
import java.util.Vector;
import javax.swing.*;


public class ParameterDialog extends JDialog implements ActionListener
{

    private JLabel serverLabel = new JLabel("Server:");
    private JLabel portLabel = new JLabel("Port:");
    private JLabel databaseLabel = new JLabel("Database:");
    private JLabel userLabel = new JLabel("User:");
    private JLabel passwdLabel = new JLabel("Password:");
    private JComboBox serverField = new JComboBox();
    private JTextField portField = new JTextField(6);
    private JTextField databaseField = new JTextField(20);
    private JTextField userField = new JTextField(20);
    private JPasswordField passwdField = new JPasswordField(20);
    private JButton okButton = new JButton("OK");
    private JButton cancelButton = new JButton("Cancel");
    private boolean okSelected;
    private Vector params;


    public ParameterDialog(Frame parent)
    {
	this(parent, false, null);
    }


    public ParameterDialog(Frame parent, boolean databasePrompt)
    {
	this(parent, databasePrompt, null);
    }


    public ParameterDialog(Frame parent, boolean databasePrompt, String title)
    {
	super(parent, true);

	if (title == null)
	    title = "Database Connection Parameters";

	this.setTitle(title);

	GridBagLayout gbl = new GridBagLayout();
	GridBagConstraints constr = new GridBagConstraints();
	Container contentPane = this.getContentPane();
	contentPane.setLayout(gbl);

	serverField.setEditable(true);
	serverField.setFont(databaseField.getFont());

	// Server label
	constr.anchor = GridBagConstraints.WEST;
	constr.fill = GridBagConstraints.NONE;
	constr.gridwidth = GridBagConstraints.RELATIVE;
	constr.insets = new Insets(10, 10, 5, 5);

	gbl.setConstraints(serverLabel, constr);
	contentPane.add(serverLabel);

	// Server field
	constr.fill = GridBagConstraints.HORIZONTAL;
	constr.gridwidth = GridBagConstraints.REMAINDER;
	constr.insets = new Insets(10, 0, 5, 10);

	gbl.setConstraints(serverField, constr);
	contentPane.add(serverField);

	// Port label
	constr.fill = GridBagConstraints.NONE;
	constr.gridwidth = GridBagConstraints.RELATIVE;
	constr.insets = new Insets(0, 10, 5, 5);

	gbl.setConstraints(portLabel, constr);
	contentPane.add(portLabel);

	// Port field
	constr.gridwidth = GridBagConstraints.REMAINDER;
	constr.insets = new Insets(0, 0, 5, 10);

	gbl.setConstraints(portField, constr);
	contentPane.add(portField);

	if (databasePrompt) {
	    // Database label
	    constr.gridwidth = GridBagConstraints.RELATIVE;
	    constr.insets = new Insets(0, 10, 5, 5);

	    gbl.setConstraints(databaseLabel, constr);
	    contentPane.add(databaseLabel);

	    // Database field
	    constr.gridwidth = GridBagConstraints.REMAINDER;
	    constr.insets = new Insets(0, 0, 5, 10);

	    gbl.setConstraints(databaseField, constr);
	    contentPane.add(databaseField);
	}

	// User label
	constr.gridwidth = GridBagConstraints.RELATIVE;
	constr.insets = new Insets(0, 10, 5, 5);

	gbl.setConstraints(userLabel, constr);
	contentPane.add(userLabel);

	// User field
	constr.gridwidth = GridBagConstraints.REMAINDER;
	constr.insets = new Insets(0, 0, 5, 10);

	gbl.setConstraints(userField, constr);
	contentPane.add(userField);

	// Password label
	constr.gridwidth = GridBagConstraints.RELATIVE;
	constr.insets = new Insets(0, 10, 10, 5);

	gbl.setConstraints(passwdLabel, constr);
	contentPane.add(passwdLabel);

	// Password field
	constr.gridwidth = GridBagConstraints.REMAINDER;
	constr.insets = new Insets(0, 0, 10, 10);

	gbl.setConstraints(passwdField, constr);
	contentPane.add(passwdField);

	JPanel buttonPanel = new JPanel(new GridLayout(1, 2, 25, 0));

	buttonPanel.add(okButton);
	buttonPanel.add(cancelButton);

	constr.anchor = GridBagConstraints.CENTER;
	constr.insets = new Insets(0, 10, 10, 10);

	gbl.setConstraints(buttonPanel, constr);
	contentPane.add(buttonPanel);

	serverField.addActionListener(this);
	okButton.addActionListener(this);
	cancelButton.addActionListener(this);

	readParameters();

	this.getRootPane().setDefaultButton(okButton);

	this.pack();
	this.setLocationRelativeTo(parent);
	this.setResizable(false);
	this.setVisible(true);
    }


    private void readParameters()
    {
	try {
	    File paramFile = new File("dbparms.obj");

	    if (! paramFile.exists())
		return;

	    FileInputStream paramFis = new FileInputStream(paramFile);
	    ObjectInputStream paramOis = new ObjectInputStream(paramFis);

	    params = (Vector) paramOis.readObject();

	    if (params == null)
		return;

	    if (params.isEmpty())
		return;

	    serverField.removeAllItems();

	    DatabaseParameters dbParms;

	    for (int i = 0; i < params.size(); i++) {
		dbParms = (DatabaseParameters) params.get(i);
		serverField.addItem(dbParms.server);
		if (i == 0) {
		    portField.setText(String.valueOf(dbParms.port));
		    databaseField.setText(dbParms.database);
		    userField.setText(dbParms.user);
		    passwdField.setText(dbParms.password);
		}
	    }

	    serverField.setSelectedIndex(0);

	    paramOis.close();
	    paramFis.close();
	}
	catch (Exception ex) {
	    System.out.println(ex.getMessage());
	}
    }


    private void writeParameters(DatabaseParameters dbParms)
    {
	if (params == null)
	    params = new Vector();

	// remove any occurrences of parameter objects for the same
	// database server
	for (int i = 0; i < params.size(); i++) {
	    DatabaseParameters parm = (DatabaseParameters) params.get(i);
	    if (parm.server.equals(dbParms.server)) {
		params.remove(i);
		i--;
	    }
	}

	// put the parameter object at the front of the Vector
	params.insertElementAt(dbParms, 0);

	// write the parameter Vector to a file
	try {
	    File paramFile = new File("dbparms.obj");
	    FileOutputStream paramFos = new FileOutputStream(paramFile);
	    ObjectOutputStream paramOos = new ObjectOutputStream(paramFos);

	    paramOos.writeObject(params);

	    paramOos.close();
	    paramFos.close();
	}
	catch (Exception ex) {
	    System.out.println(ex.getMessage());
	}
    }


    public String getServer()
    {
	if (okSelected)
	    return ((String) (serverField.getSelectedItem())).trim();
	else
	    return null;
    }


    public int getPort()
    {
	if (okSelected) {
	    try {
		return Integer.valueOf(portField.getText().trim()).intValue();
	    }
	    catch (NumberFormatException ex) {
		return 0;
	    }
	}
	else
	    return 0;
    }


    public String getDatabase()
    {
	if (okSelected)
	    return databaseField.getText().trim();
	else
	    return null;
    }


    public String getUser()
    {
	if (okSelected)
	    return userField.getText().trim();
	else
	    return null;
    }


    public String getPassword()
    {
	if (okSelected)
	    return new String(passwdField.getPassword()).trim();
	else
	    return null;
    }


    public void actionPerformed(ActionEvent e)
    {
	Object source = e.getSource();

	if (source == serverField) {
	    int serverIndex = serverField.getSelectedIndex();
	    if (serverIndex >= 0 && serverIndex < params.size()) {
		DatabaseParameters dbParms =
		    (DatabaseParameters) params.get(serverIndex);
		portField.setText(String.valueOf(dbParms.port));
		databaseField.setText(dbParms.database);
		userField.setText(dbParms.user);
		passwdField.setText(dbParms.password);
	    }
	} else if (source == okButton) {
	    okSelected = true;
	    DatabaseParameters dbParms =
		new DatabaseParameters(getServer(), getPort(), getDatabase(),
				       getUser(), getPassword());
	    writeParameters(dbParms);
	    this.dispose();
	} else if (source == cancelButton) {
	    okSelected = false;
	    this.dispose();
	}
    }
}
