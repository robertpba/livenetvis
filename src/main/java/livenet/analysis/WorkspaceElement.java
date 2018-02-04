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

/**
   An element contained within a workspace. Workspace element types:

   <ul>
   <li>roles</li>
   <li>participants</li>
   <li>document</li>
   <li>discussion</li>
   <li>action</li>
   <li>messagetype</li>
   <li>messagerule</li>
   </ul>

   The purpose of this class is primarily to group a set of related
   classes together.
**/
public abstract class WorkspaceElement implements java.io.Serializable
{
    // element types:
    final public static int ROLE = 0;
    final public static int PARTICIPANT = 1;
    final public static int ACTION = 2;
    final public static int DISCUSSION = 3;
    final public static int DOCUMENT = 4;
    final public static int MESSAGERULE = 5;
    final public static int MESSAGETYPE = 6;

    WorkspaceNode parentWorkspace;
}
