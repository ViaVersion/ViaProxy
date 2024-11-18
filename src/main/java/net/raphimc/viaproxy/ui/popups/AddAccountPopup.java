/*
 * This file is part of ViaProxy - https://github.com/RaphiMC/ViaProxy
 * Copyright (C) 2021-2024 RK_01/RaphiMC and contributors
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
package net.raphimc.viaproxy.ui.popups;

import net.lenni0451.commons.swing.GBC;
import net.raphimc.minecraftauth.step.msa.StepMsaDeviceCode;
import net.raphimc.viaproxy.ui.I18n;
import net.raphimc.viaproxy.ui.ViaProxyWindow;
import net.raphimc.viaproxy.ui.elements.LinkLabel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.function.Consumer;

import static net.raphimc.viaproxy.ui.ViaProxyWindow.BODY_BLOCK_PADDING;
import static net.raphimc.viaproxy.ui.ViaProxyWindow.BORDER_PADDING;

public class AddAccountPopup extends JDialog {

    private final ViaProxyWindow parent;
    private final StepMsaDeviceCode.MsaDeviceCode deviceCode;
    private boolean externalClose;

    public AddAccountPopup(final ViaProxyWindow parent, final StepMsaDeviceCode.MsaDeviceCode deviceCode, final Consumer<AddAccountPopup> popupConsumer, final Runnable closeListener) {
        super(parent, true);
        this.parent = parent;
        this.deviceCode = deviceCode;
        popupConsumer.accept(this);

        this.initWindow(closeListener);
        this.initComponents();
        this.pack();
        this.setVisible(true);
    }

    private void initWindow(final Runnable closeListener) {
        this.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (!AddAccountPopup.this.externalClose) closeListener.run();
            }
        });
        this.setTitle(I18n.get("popup.login_account.title"));
        this.setSize(400, 140);
        this.setResizable(false);
        this.setLocationRelativeTo(this.parent);
    }

    private void initComponents() {
        JPanel contentPane = new JPanel();
        contentPane.setLayout(new GridBagLayout());
        {
            JLabel browserLabel = new JLabel("<html><p>" + I18n.get("popup.login_account.instructions.browser") + "</p></html>");
            GBC.create(contentPane).grid(0, 0).weightx(1).insets(BORDER_PADDING, BORDER_PADDING, 0, BORDER_PADDING).fill(GBC.HORIZONTAL).add(browserLabel);

            GBC.create(contentPane).grid(0, 1).weightx(1).insets(0, BORDER_PADDING, 0, BORDER_PADDING).fill(GBC.HORIZONTAL).add(new LinkLabel(this.deviceCode.getDirectVerificationUri(), AddAccountPopup.this.deviceCode.getDirectVerificationUri()));

            JLabel closeInfo = new JLabel("<html><p>" + I18n.get("popup.login_account.instructions.close") + "</p></html>");
            GBC.create(contentPane).grid(0, 2).weightx(1).insets(BODY_BLOCK_PADDING, BORDER_PADDING, BORDER_PADDING, BORDER_PADDING).fill(GBC.HORIZONTAL).add(closeInfo);
        }
        this.setContentPane(contentPane);
    }

    public void markExternalClose() {
        this.externalClose = true;
    }

}
