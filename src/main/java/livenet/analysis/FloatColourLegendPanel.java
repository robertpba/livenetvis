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
import java.awt.geom.AffineTransform;
import java.text.NumberFormat;
import javax.swing.*;


public class FloatColourLegendPanel extends JPanel
{
    // set to true to get debugging output
    private final boolean DEBUG = false;

    FloatHistogram hist;
    Color[] colours;
    String leftLabel;
    String rightLabel;

    float maxValue;       // maximum histogram value
    float maxKey;         // maximum histogram key
    int numXTicks = 20;   // number of tick marks on X axis
    int numYTicks = 10;   // number of tick marks on y axis

    // various measures (in pixels)
    int width = 200;      // width of the panel
    int height = 120;     // height of the panel
    int xMargin = 10;     // base horizontal margin
    int leftMargin = 0;   // additional left margin
    int rightMargin = 0;  // additional right margin
    int yMargin = 10;     // base vertical margin
    int topMargin = 0;    // additional top margin
    int bottomMargin = 0; // additional bottom margin
    int markLength = 5;   // length of the tick mark
    int labelGap = 2;     // gap between the tick mark and number label

    Font scaleFont = new Font("SansSerif", Font.PLAIN, 8);
    Font labelFont = new Font("SansSerif", Font.PLAIN, 10);

    NumberFormat formatter;


    public FloatColourLegendPanel(FloatHistogram hist, Color[] colours,
				  String leftLabel, String rightLabel)
    {
	this.hist = hist;
	this.colours = colours;
	this.leftLabel = leftLabel;
	this.rightLabel = rightLabel;

	formatter = NumberFormat.getNumberInstance();

	maxValue = hist.getMaxValue();
	maxKey = hist.getMaxKey();

	int size = hist.getSize();
	if (size < numXTicks)
	    numXTicks = size;
	if (size < numYTicks)
	    numYTicks = size;

	this.setSize(width, height);
    }


    public void paintComponent(Graphics g)
    {
	super.paintComponent(g);

	g.setFont(scaleFont);
	FontMetrics fm1 = g.getFontMetrics();

	g.setFont(labelFont);
	FontMetrics fm2 = g.getFontMetrics();

	// determine width of the widest left-hand side label
	int maxLeftNumWidth = 0;
	float increment = (float) 1.0;
	if (maxValue > numYTicks)
	    increment = (float) (maxValue / numYTicks);
	for (float i = (float) 0.0; i <= maxValue; i += increment) {
	    String label = formatter.format(i);
	    int labelWidth = fm1.stringWidth(label);
	    if (labelWidth > maxLeftNumWidth)
		maxLeftNumWidth = labelWidth;
	}

	// determine width of the widest right-hand side label
	int maxRightNumWidth = 0;
	increment = (float) 1.0;
	if (maxKey > numYTicks)
	    increment = (float) (maxKey / numYTicks);
	for (float i = (float) 0.0; i <= maxKey; i += increment) {
	    String label = formatter.format(i);
	    int labelWidth = fm1.stringWidth(label);
	    if (labelWidth > maxRightNumWidth)
		maxRightNumWidth = labelWidth;
	}

	// set margins the first time we paint this component
	if (leftMargin == 0)
	    leftMargin = xMargin + markLength + labelGap + maxLeftNumWidth +
		labelGap + fm2.getHeight();

	if (rightMargin == 0)
	    rightMargin = xMargin + markLength + labelGap + maxRightNumWidth +
		labelGap + fm2.getHeight();

	if (bottomMargin == 0)
	    bottomMargin = yMargin + markLength + labelGap + fm1.getHeight();

        Insets insets = this.getInsets();
        int legendWidth = this.getWidth() -
	    (insets.left + insets.right + leftMargin + rightMargin);
        int legendHeight = this.getHeight() -
	    (insets.top + insets.bottom + topMargin + bottomMargin);
	int leftX = insets.left + leftMargin;
	int topY = insets.top + yMargin;

	int numValues = hist.getSize();

	// box around the edge of the legend
	g.drawRect(leftX - 1, topY - 1, legendWidth + 1, legendHeight + 1);

	int valuePrevX = -1;
	int valuePrevY = -1;
	int keyPrevX = -1;
	int keyPrevY = -1;

	g.setFont(scaleFont);

	// scale on left edge of legend
	int fontHeight = fm1.getAscent() + fm1.getDescent();
	int yFontOffset = (int) (fm1.getAscent() / 2);

	increment = (float) 1.0;
	if (maxValue > numYTicks)
	    increment = (float) (maxValue / numYTicks);

	for (float i = (float) 0.0; i <= maxValue; i += increment) {
	    int markX1 = leftX;
	    int markX2 = markX1 - markLength;
	    int markY = topY + (legendHeight - 1) -
		(int) ((i * legendHeight) / maxValue);
	    g.setColor(Color.black);
	    g.drawLine(markX1, markY, markX2, markY);

	    String label = formatter.format(i);
	    int labelWidth = fm1.stringWidth(label);
	    g.drawString(label, markX2 - (labelGap + labelWidth),
			 markY + yFontOffset);
	}

	// scale on right edge of legend
	increment = (float) 1.0;
	if (maxKey > numYTicks)
	    increment = (float) (maxKey / numYTicks);

	for (float i = (float) 0.0; i <= maxKey; i += increment) {
	    int markX1 = leftX + legendWidth;
	    int markX2 = markX1 + markLength;
	    int markY = topY + (legendHeight - 1) -
		(int) ((i * legendHeight) / maxKey);
	    g.setColor(Color.blue);
	    g.drawLine(markX1, markY, markX2, markY);

	    String label = formatter.format(i);
	    int labelWidth = fm1.stringWidth(label);
	    g.drawString(label, markX2 + labelGap, markY + yFontOffset);
	}

	float firstKey = hist.getMinKey();

	// label for first value (0.0) at bottom of legend
	String firstLabel = formatter.format(0.0);
	int firstLabelWidth = fm1.stringWidth(firstLabel);
	int firstLabelX = leftX - (int) (firstLabelWidth / 2);
	if (firstKey == 0.0)
	    firstLabelX += (int) (legendWidth / (2 * colours.length));

	g.drawString(firstLabel, firstLabelX, topY + legendHeight +
		     markLength + labelGap + fm1.getAscent());

	// label for last value (maxKey) at bottom of legend
	String lastLabel = formatter.format(maxKey);
	int lastLabelWidth = fm1.stringWidth(lastLabel);
	g.drawString(lastLabel, leftX + legendWidth -
		     ((int) ((lastLabelWidth / 2) +
		      (legendWidth / (2 * colours.length)))),
		     topY + legendHeight + markLength + labelGap +
		     fm1.getAscent());


	int drawnKeyTicks = 0;
	int prevKeyTickX = leftX;
	float nextKeyTick = maxKey / numXTicks;
	int markY1 = topY + legendHeight;
	int markY2 = markY1 + markLength;

	// draw the 0 mark tick (this one doesn't count towards numXTicks)
	int markX = prevKeyTickX;
	if (firstKey == 0.0)
	    markX += (int) (legendWidth / (2 * colours.length));

	g.drawLine(markX, markY1, markX, markY2);

	// draw the main part of the legend (the colourful centre), plus
	// the bottom tick marks
	for (int i = 0; i < colours.length; i++) {

	    // Step 1: draw the colour-filled rectangles
	    int x1 = leftX + (int) ((i * legendWidth) / colours.length);
	    int x2 = leftX +
		(int) (((i + 1) * legendWidth) / colours.length) - 1;
	    g.setColor(colours[i]);
	    g.fillRect(x1, topY, (x2 - x1) + 1, legendHeight);

	    // Step 2: draw the polyline indicating histogram values
	    float histValue = hist.getValueAt(i);
	    if (histValue < 0.0)
		histValue = (float) 0.0;

	    int valueLineX = x1 + (int) ((x2 - x1) / 2);
	    int valueLineY = topY + (legendHeight - 1) -
		(int) ((histValue * legendHeight) / maxValue);

	    if (valuePrevX == -1 && valuePrevY == -1) {
		valuePrevX = valueLineX;
		valuePrevY = valueLineY;
	    }

	    g.setColor(Color.black);
	    g.drawLine(valuePrevX, valuePrevY, valueLineX, valueLineY);

	    valuePrevX = valueLineX;
	    valuePrevY = valueLineY;

	    // Step 3: draw the polyline indicating histogram keys
	    float histKey = hist.getKeyAt(i);

	    if (DEBUG)
		System.out.println("Histogram Key " + histKey);

	    if (histKey < 0.0)
		histKey = (float) 0.0;

	    int keyLineX = x1 + (int) ((x2 - x1) / 2);
	    int keyLineY = topY + (legendHeight - 1) -
		(int) ((histKey * legendHeight) / maxKey);

	    if (keyPrevX == -1 && keyPrevY == -1) {
		keyPrevX = keyLineX;
		keyPrevY = keyLineY;
	    }

	    g.setColor(Color.blue);
	    g.drawLine(keyPrevX, keyPrevY, keyLineX, keyLineY);

	    keyPrevX = keyLineX;
	    keyPrevY = keyLineY;

	    // Step 4: draw the tick marks at the bottom
	    if (histKey > nextKeyTick) {

		// figure out how many ticks have been missed
		int missedTicks = 1;

		if (DEBUG)
		    System.out.print("Hist Key " + histKey +
				     " Missed ticks: ");

		while (nextKeyTick < histKey) {

		    if (DEBUG)
			System.out.print(nextKeyTick + " ");

		    missedTicks++;
		    nextKeyTick = (float)
			(((drawnKeyTicks + missedTicks) * maxKey) / numXTicks);
		}
		missedTicks--;

		if (DEBUG)
		    System.out.println("");

		// draw the missed ticks evenly spread between preceding key
		// and current position

		// first figure out the X position of the previous key, if any
		int prevKeyX = leftX;
		if (i > 0) {
		    int prevKeyX1 = leftX + (int) (((i - 1) * legendWidth) /
						   colours.length);
		    int prevKeyX2 = keyLineX - 1;
		    prevKeyX = prevKeyX1 + (int) ((prevKeyX2 - prevKeyX1) / 2);
		}
		    
		int tickDelta = keyLineX - prevKeyX;
		int missedTickX = 0;

		for (int j = 1; j <= missedTicks; j++) {
		    missedTickX = prevKeyX +
			(int) ((j * tickDelta) / (missedTicks + 1));
		    g.drawLine(missedTickX, markY1, missedTickX, markY2);
		    if (DEBUG)
			System.out.println("Tick " +
					   (drawnKeyTicks + missedTicks) +
					   " Xpos " + missedTickX +
					   " Previous Xpos " + prevKeyTickX +
					   " (missed tick) Next Regular Tick: " +
					   nextKeyTick);
		}
		drawnKeyTicks += missedTicks;
		prevKeyTickX = missedTickX;
	    }
	    if (histKey == nextKeyTick) {
		g.drawLine(keyLineX, markY1, keyLineX, markY2);
		drawnKeyTicks++;
		nextKeyTick =
		    (float) (((drawnKeyTicks + 1) * maxKey) / numXTicks);
		if (DEBUG)
		    System.out.println("Tick " + drawnKeyTicks +
				       " Xpos " + keyLineX +
				       " Previous Xpos " + prevKeyTickX +
				       " Histo Key: " + histKey +
				       " Next Tick: " + nextKeyTick);
		prevKeyTickX = keyLineX;
	    }
	}

	g.setFont(labelFont);

	// label next to the left hand scale
	g.setColor(Color.black);
	int leftLabelLength = fm2.stringWidth(leftLabel);
	int leftLabelYOffset = 0;
	if (leftLabelLength < legendHeight)
	    leftLabelYOffset = (int) ((legendHeight - leftLabelLength) / 2);
	double leftLabelXPos = (double) (leftX - (markLength + labelGap +
						  maxLeftNumWidth + labelGap));
	double leftLabelYPos = (double) (topY + (legendHeight - 1) -
					 leftLabelYOffset);
	Graphics2D g2 = (Graphics2D) g;
	AffineTransform at = new AffineTransform();
	// Apply a translation transform to make room for the
	// rotated text.
	at.setToTranslation(leftLabelXPos, leftLabelYPos);
	g2.transform(at);
	// Create a rotation transform to rotate the text
	at.setToRotation(Math.PI * 1.5);
	g2.transform(at);
	g2.drawString(leftLabel, 0.0f, 0.0f);


	// label next to the right hand scale
	g.setColor(Color.blue);
	int rightLabelLength = fm2.stringWidth(rightLabel);
	int rightLabelYOffset = 0;
	if (rightLabelLength < legendHeight)
	    rightLabelYOffset = (int) ((legendHeight - rightLabelLength) / 2);
	double rightLabelXPos = (double) (leftX + legendWidth + markLength +
					  labelGap + maxRightNumWidth +
					  labelGap + fm2.getAscent());
	double rightLabelYPos = (double) (topY + (legendHeight - 1) -
					 rightLabelYOffset);
	AffineTransform atr = new AffineTransform();
	g2.setTransform(atr);
	// Apply a translation transform to make room for the
	// rotated text.
	atr.setToTranslation(rightLabelXPos, rightLabelYPos);
	g2.transform(atr);
	// Create a rotation transform to rotate the text
 	atr.setToRotation(Math.PI * 1.5);
 	g2.transform(atr);
	g2.drawString(rightLabel, 0.0f, 0.0f);
    }
} // ColourLegendPanel
