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

import java.awt.Color;


public interface TreeGraphConstants
{
    // dimensions
    public final int initialEdgeLength = 120;
    public final int marginGap = 3;
    public final int focusGap = 3;
    public final int focusThickness = 2;
    public final int focusLength = 5;

    // node colours
    final Color nodeColour = new Color(250, 220, 100);
    final Color leafColour = Color.white;
    final Color specialColour = Color.lightGray;
    final Color fixedColour = nodeColour;
    final Color selectColour = Color.pink;
    final Color edgeColour = Color.black;

    // edge colours
    final Color childColour = Color.black;
    final Color goalColour = Color.red;
    final Color actionColour = Color.cyan;
    final Color discussionColour = Color.green;
    final Color documentColour = Color.blue;
    final Color messageRuleColour = new Color(190, 95, 0);     // brown
    final Color messageTypeColour = Color.gray;
    final Color participantColour = new Color(255, 119, 200);  // pink
    final Color roleColour = nodeColour;
    final Color otherColour = Color.yellow;
    final Color stressColour = Color.darkGray;
}
