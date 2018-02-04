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

import java.awt.BorderLayout;
import java.sql.Connection;
import javax.swing.JFrame;


public class WorkspaceElementMap extends JFrame
{
    public WorkspaceElementMap(Connection con, WorkspaceNode node,
			       MapControls controls)
    {
	// Set up the graph panel
	GraphPanel graphPan = new GraphPanel(con, node, controls);
	this.getContentPane().add(BorderLayout.CENTER, graphPan);
	graphPan.start();

	this.setTitle(node.fullName);
	this.setLocation((int) node.x, (int) node.y);
	this.setSize(graphPan.getSize());
    }
}
