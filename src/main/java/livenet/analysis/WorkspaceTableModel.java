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

import java.sql.*;
import java.util.Vector;
import javax.swing.table.AbstractTableModel;


public class WorkspaceTableModel extends AbstractTableModel
{
    private String query =
	"SELECT WorkspaceName, OwnerName FROM DBA.Workspaces ORDER BY 2, 1";

    private final int NUMCOLUMNS = 2;

    private final String[] columnNames = { "Workspace", "Owner" };
    private Vector data;


    public WorkspaceTableModel(Connection con)
    {
	try
	    {
		Statement stmt = con.createStatement();
		ResultSet rs = stmt.executeQuery(query);
		data = new Vector();
		while (rs.next())
		    {
			String workspace = rs.getString(1);
			String owner = rs.getString(2);
			WorkspaceIdentifier wsid =
			    new WorkspaceIdentifier(workspace, owner);
			data.addElement(wsid);
		    }

		// Close the statement object
		stmt.close();

	    }
	catch (SQLException ex) {
	    System.out.println(ex.getMessage());
	    return;
	}
    }


    public String getColumnName(int col)
    {
	return columnNames[col];
    }


    public int getRowCount()
    {
	return data.size();
    }


    public int getColumnCount()
    {
	return NUMCOLUMNS;
    }


    public Object getValueAt(int row, int column)
    {
	WorkspaceIdentifier wsid = (WorkspaceIdentifier) data.get(row);
	if (column == 0)
	    return wsid.workspace;
	else if (column == 1)
	    return wsid.owner;
	else
	    return null;
    }
}
