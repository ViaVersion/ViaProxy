/*
 * This file is part of ViaProxy - https://github.com/RaphiMC/ViaProxy
 * Copyright (C) 2023 RK_01/RaphiMC and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.raphimc.viaproxy.util;

import java.awt.*;

public class GBC {

    public static GBC create() {
        return new GBC(null);
    }

    public static GBC create(final Container parent) {
        return new GBC(parent);
    }


    private final Container parent;
    private final GridBagConstraints gbc;

    private GBC(final Container parent) {
        this.parent = parent;
        this.gbc = new GridBagConstraints();
    }

    public GBC gridx(final int gridx) {
        this.gbc.gridx = gridx;
        return this;
    }

    public GBC gridy(final int gridy) {
        this.gbc.gridy = gridy;
        return this;
    }

    public GBC grid(final int gridx, final int gridy) {
        this.gbc.gridx = gridx;
        this.gbc.gridy = gridy;
        return this;
    }

    public GBC width(final int gridwidth) {
        this.gbc.gridwidth = gridwidth;
        return this;
    }

    public GBC height(final int gridheight) {
        this.gbc.gridheight = gridheight;
        return this;
    }

    public GBC weightx(final double weightx) {
        this.gbc.weightx = weightx;
        return this;
    }

    public GBC weighty(final double weighty) {
        this.gbc.weighty = weighty;
        return this;
    }

    public GBC anchor(final int anchor) {
        this.gbc.anchor = anchor;
        return this;
    }

    public GBC fill(final int fill) {
        this.gbc.fill = fill;
        return this;
    }

    public GBC insets(final Insets insets) {
        this.gbc.insets = insets;
        return this;
    }

    public GBC insets(final int top, final int left, final int bottom, final int right) {
        this.gbc.insets = new Insets(top, left, bottom, right);
        return this;
    }

    public GBC ipadx(final int ipadx) {
        this.gbc.ipadx = ipadx;
        return this;
    }

    public GBC ipady(final int ipady) {
        this.gbc.ipady = ipady;
        return this;
    }

    public void add(final Component component) {
        this.parent.add(component, this.gbc);
    }

    public GridBagConstraints get() {
        return this.gbc;
    }

}
