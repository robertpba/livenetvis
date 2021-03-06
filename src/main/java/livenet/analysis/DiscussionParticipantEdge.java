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

public class DiscussionParticipantEdge extends WorkspaceElementEdge
{
    int fromWeight = 0;
    int toWeight = 0;


    public DiscussionParticipantEdge(WorkspaceElementNode fromNode,
				     WorkspaceElementNode toNode)
    {
	this.fromNode = fromNode;
	this.toNode = toNode;
	this.length = initialEdgeLength / 2;
	this.fromWeight = fromWeight;
	this.toWeight = toWeight;
    }


    public void setFromWeight(int weight)
    {
	this.fromWeight = weight;
    }


    public void setToWeight(int weight)
    {
	this.toWeight = weight;
    }


    public int getFromWeight()
    {
	return(fromWeight);
    }


    public int getToWeight()
    {
	return(toWeight);
    }
}
