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

import java.awt.Point;
import java.util.Vector;


public class WorkspaceElementNode implements TreeGraphConstants
{
    // stuff that may get passed into the constructor
    int type;
    WorkspaceElement element;

    // the names which are used on the workspace element's visual
    // representation
    String shortName;
    String fullName;
    String displayName;

    // coordinates of the node's centre
    double x;
    double y;

    // coordinates of the node's bounding box
    int x1;
    int y1;
    int x2;
    int y2;

    // deltas for adjusting the node's position
    double dx;
    double dy;

    // is the node's position fixed?
    boolean fixed = false;

    // should the node be visible?
    boolean visible = false;

    // should the node be focused on?
    boolean focus = false;


    public WorkspaceElementNode(int type, WorkspaceElement element)
    {
	this.type = type;
	this.element = element;
	switch (type) {
	case WorkspaceElement.ROLE :
	    fullName = ((Role) element).name;
	    shortName = createShortName(fullName);
	    break;
	case WorkspaceElement.PARTICIPANT :
	    fullName = ((Participant) element).name;
	    shortName = createShortName(fullName);
	    break;
	case WorkspaceElement.ACTION :
	    fullName = ((Action) element).name;
	    shortName = createShortName(fullName);
	    break;
	case WorkspaceElement.DISCUSSION :
	    fullName = ((Discussion) element).name;
	    shortName = createShortName(fullName);
	    break;
	case WorkspaceElement.DOCUMENT :
	    fullName = ((Document) element).name;
	    shortName = createShortName(fullName);
	    break;
	case WorkspaceElement.MESSAGERULE :
	    fullName = "Message Rule";
	    shortName = "Msg.Rule";
	    break;
	case WorkspaceElement.MESSAGETYPE :
	    fullName = ((MessageType) element).type;
	    shortName = createShortName(fullName);
	    break;
	}
	displayName = shortName;
    }


    String createShortName(String name)
    {
	if (name.length() > 10)
	    name = name.substring(0, 7) + "...";
	return name;
    }


    public void setFixed(boolean fixed)
    {
	this.fixed = fixed;
    }


    public boolean isFixed()
    {
	return fixed;
    }


    public void setVisible(boolean visible)
    {
	this.visible = visible;
    }


    public boolean isVisible()
    {
	return visible;
    }


    public void setFocus(boolean focus)
    {
	this.focus = focus;
    }


    public boolean hasFocus()
    {
	return focus;
    }


    public void setShortDisplayName()
    {
	this.displayName = this.shortName;
    }


    public void setLongDisplayName()
    {
	this.displayName = this.fullName;
    }


    public boolean contains(Point p)
    {
	return (p.x >= x1 && p.x <= x2 && p.y >= y1 && p.y <= y2);
    }
}
