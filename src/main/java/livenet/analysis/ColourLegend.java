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
import javax.swing.*;


public class ColourLegend extends JFrame
{
    public ColourLegend(Histogram hist, Color[] colours, String title,
			String leftLabel, String rightLabel)
    {
	super(title);

	ColourLegendPanel colourPanel =
	    new ColourLegendPanel(hist, colours, leftLabel, rightLabel);

	this.getContentPane().add(BorderLayout.CENTER, colourPanel);
	this.setSize(colourPanel.getSize());
    }


    public ColourLegend(FloatHistogram hist, Color[] colours, String title,
			String leftLabel, String rightLabel)
    {
	super(title);

	FloatColourLegendPanel colourPanel =
	    new FloatColourLegendPanel(hist, colours, leftLabel, rightLabel);

	this.getContentPane().add(BorderLayout.CENTER, colourPanel);
	this.setSize(colourPanel.getSize());
    }
} // ColourLegend
