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


public class ProgressDialog extends JDialog
{
    private JLabel msgLabel;
    private JProgressBar progBar;


    public ProgressDialog(Frame parent, int numTasks, int width)
    {
	super(parent, "Progress");

	msgLabel = new JLabel();
	progBar = new JProgressBar(0, numTasks);

	GridBagLayout gbl = new GridBagLayout();
	GridBagConstraints constr = new GridBagConstraints();
	Container contentPane = this.getContentPane();
	contentPane.setLayout(gbl);

	constr.anchor = GridBagConstraints.WEST;
	constr.fill = GridBagConstraints.HORIZONTAL;
	constr.gridwidth = GridBagConstraints.REMAINDER;
	constr.insets = new Insets(10, 10, 5, 10);
	constr.weightx = 1.0;

	gbl.setConstraints(msgLabel, constr);
	contentPane.add(msgLabel);

	constr.insets = new Insets(0, 10, 10, 10);

	gbl.setConstraints(progBar, constr);
	contentPane.add(progBar);

	this.pack();
	this.setSize(width, 100);
	this.setLocationRelativeTo(parent);
	this.setResizable(false);
	this.setVisible(true);
    }


    public void setMessage(String msg)
    {
	msgLabel.setText(msg);
	this.paint(this.getGraphics());
    }


    public void setMessage(String msg, int progress)
    {
	msgLabel.setText(msg);
	updateProgress(progress);
    }


    public void updateProgress(int progress)
    {
	progBar.setValue(progress);
	this.paintComponents(this.getGraphics());
    }
}
