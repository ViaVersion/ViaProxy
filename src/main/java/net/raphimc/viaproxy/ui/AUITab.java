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
package net.raphimc.viaproxy.ui;

import javax.swing.*;

public abstract class AUITab {

    protected final ViaProxyUI frame;
    private final String name;
    protected final JPanel contentPane;

    public AUITab(final ViaProxyUI frame, final String name) {
        this.frame = frame;
        this.name = I18n.get("tab." + name + ".name");
        this.contentPane = new JPanel();

        this.contentPane.setLayout(null);
        this.init(this.contentPane);
    }

    public String getName() {
        return this.name;
    }

    public void add(final JTabbedPane tabbedPane) {
        tabbedPane.addTab(this.name, this.contentPane);
    }

    protected abstract void init(final JPanel contentPane);

    protected void onTabOpened() {
    }

}
