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

import java.util.Vector;


public class WorkspaceLink implements java.io.Serializable
{
    /* Constants */

    // link types:
    final public static int CHILD = 0;
    final public static int GOAL = 1;
    final public static int ACTION = 2;
    final public static int DISCUSSION = 3;
    final public static int DOCUMENT = 4;
    final public static int MESSAGERULE = 5;
    final public static int PARTICIPANT = 6;

    // arrow types:
    final public static int NONE = 0;
    final public static int FROM = 1;
    final public static int TO = 2;
    final public static int BOTH = 3;


    /* Link properties */

    WorkspaceNode fromNode;
    WorkspaceNode toNode;
    int type;
    Vector linkObjects = new Vector();
    int arrowMode;
    int weight;
    boolean visible = false;
    double length;


    /* Constructors */

    public WorkspaceLink(WorkspaceNode fromNode, WorkspaceNode toNode,
			 int type, Object object, int arrowMode, int weight,
			 int length)
    {
	this(fromNode, toNode, type, arrowMode, weight, length);
	linkObjects.addElement(object);
    }


    public WorkspaceLink(WorkspaceNode fromNode, WorkspaceNode toNode,
			 int type, int arrowMode, int weight, int length)
    {
	this.fromNode = fromNode;
	this.toNode = toNode;
	this.type = type;
	this.arrowMode = arrowMode;
	this.weight = weight;
	this.length = length;
    }


    public void addLinkObject(Object object)
    {
	linkObjects.addElement(object);
    }
}
