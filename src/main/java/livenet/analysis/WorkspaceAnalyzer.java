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

import com.sybase.jdbcx.SybDriver;
import java.sql.*;
import javax.swing.JFrame;
import javax.swing.JOptionPane;


public class WorkspaceAnalyzer extends JFrame implements ControlListener
{
    // set to true to get debugging output
    private static boolean DEBUG = false;

    // set to true to read workspace data from a file
    private static boolean READ = false;

    // set to true to write workspace data to a file
    private static boolean WRITE = false;

    private String driverClass = "com.sybase.jdbc2.jdbc.SybDriver";
    private Connection con;  // the database connection object

    private WorkspaceMap wsMap;


    public WorkspaceAnalyzer()
    {
	if (DEBUG && READ) {
	    MapControls controls = new MapControls();
	    controls.addControlListener(this);
	    wsMap = new WorkspaceMap(null, null, controls, DEBUG);
	} else {
	    ParameterDialog parmDialog = new ParameterDialog(this);
	    String server = parmDialog.getServer();

	    if (server == null)
		System.exit(0);

	    int port = parmDialog.getPort();
	    String user = parmDialog.getUser();
	    String password = parmDialog.getPassword();

	    String url = "jdbc:sybase:Tds:" + server + ":" + port;

	    try
		{
		    SybDriver sybDriver = (SybDriver)
			Class.forName(driverClass).newInstance();
		    sybDriver.setVersion(com.sybase.jdbcx.SybDriver.VERSION_5);
		    DriverManager.registerDriver(sybDriver);

		    // Connect to the DBMS
		    con = DriverManager.getConnection(url, user, password);
	    }
	    catch (SQLException ex)
		{
		    JOptionPane.showMessageDialog(this, "Error connecting to" +
						  " the database server.",
						  "Error",
						  JOptionPane.ERROR_MESSAGE);
		    System.exit(1);
		}
	    catch (java.lang.Exception ex) 
		{
		    JOptionPane.showMessageDialog(this, "Error loading the " +
						  "database driver.", "Error",
						  JOptionPane.ERROR_MESSAGE);
		    System.exit(2);
		}

	    WorkgroupDialog wgd = new WorkgroupDialog(this, con);
	    String[] workgroups = wgd.getSelectedWorkgroups();
	    if (workgroups == null) {
		closeConnection();
		System.exit(0);
	    }
	    else {
		MapControls controls = new MapControls();
		controls.addControlListener(this);
		wsMap = new WorkspaceMap(con, workgroups, controls, false);
	    }
	}
    }


    public void closeConnection()
    {
	try {
	    // Close the connection
	    if (con != null)
		con.close();
	}
	catch(SQLException ex) {
	    JOptionPane.showMessageDialog(this, ex.getMessage(), "Error",
					  JOptionPane.ERROR_MESSAGE);
	}
    }


    public void controlActionPerformed(ControlEvent e)
    {
	if (DEBUG)
	    System.out.println("[WorkspaceAnalyzer] Event type:  " +
			       e.getType() + "  Event value: " + e.getValue());

	if (e.getType() == MapControls.CLOSE) {
	    if (WRITE)
		wsMap.saveTree();
	    System.exit(0);
	}
    }


    public static void main(String argv[])
    {
	if (argv.length == 1) {
	    if (argv[0].startsWith("debug"))
		DEBUG = true;
	    if (argv[0].equals("debug-read"))
		READ = true;
	    else if (argv[0].equals("debug-write"))
		WRITE = true;
	}

	WorkspaceAnalyzer analyzer = new WorkspaceAnalyzer();
    }
}
