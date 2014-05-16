/**
 * Copyright 2013-2014 by ATLauncher and Contributors
 *
 * This work is licensed under the Creative Commons Attribution-ShareAlike 3.0 Unported License.
 * To view a copy of this license, visit http://creativecommons.org/licenses/by-sa/3.0/.
 */
package com.atlauncher.gui.tabs;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.ImageIcon;
import javax.swing.JPanel;

import com.atlauncher.utils.Utils;

@SuppressWarnings("serial")
public abstract class AbstractSettingsTab extends JPanel {

    final Insets LABEL_INSETS = new Insets(5, 0, 5, 10);
    final Insets FIELD_INSETS = new Insets(5, 0, 5, 0);
    final Insets LABEL_INSETS_SMALL = new Insets(0, 0, 0, 10);
    final Insets FIELD_INSETS_SMALL = new Insets(0, 0, 0, 0);

    final ImageIcon helpIcon = Utils.getIconImage("/assets/image/Help.png");

    final GridBagConstraints gbc;

    public AbstractSettingsTab() {
        setLayout(new GridBagLayout());
        this.gbc = new GridBagConstraints();
    }

}
