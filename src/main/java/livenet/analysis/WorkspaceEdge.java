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


class WorkspaceEdge
{
    /* Constants */

    // link (edge) types:
    final static int CHILD = 0;
    final static int LINKED = 1;
    final static int ACTION = 2;
    final static int DISCUSSION = 3;
    final static int DOCUMENT = 4;
    final static int MESSAGERULE = 5;

    // arrow types:
    final static int NONE = 0;
    final static int FROM = 1;
    final static int TO = 2;
    final static int BOTH = 3;


    /* Edge properties */

    WorkspaceNode fromNode;
    WorkspaceNode toNode;
    int type;
    int arrowMode;
    int weight;
    double length;


    /* Constructors */

    public WorkspaceEdge(WorkspaceNode fromNode, WorkspaceNode toNode,
			 int type, int arrowMode, int weight, int length)
    {
	this.fromNode = fromNode;
	this.toNode = toNode;
	this.type = type;
	this.arrowMode = arrowMode;
	this.weight = weight;
	this.length = length;
    }
}
