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
package net.raphimc.viaproxy.ui.popups;

import net.raphimc.mcauth.step.msa.StepMsaDeviceCode;
import net.raphimc.viaproxy.ui.ViaProxyUI;
import net.raphimc.viaproxy.util.GBC;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.function.Consumer;

import static net.raphimc.viaproxy.ui.ViaProxyUI.BODY_BLOCK_PADDING;
import static net.raphimc.viaproxy.ui.ViaProxyUI.BORDER_PADDING;

public class AddAccountPopup extends JDialog {

    private final ViaProxyUI parent;
    private final StepMsaDeviceCode.MsaDeviceCode deviceCode;
    private boolean externalClose;

    public AddAccountPopup(final ViaProxyUI parent, final StepMsaDeviceCode.MsaDeviceCode deviceCode, final Consumer<AddAccountPopup> popupConsumer, final Runnable closeListener) {
        super(parent, true);
        this.parent = parent;
        this.deviceCode = deviceCode;
        popupConsumer.accept(this);

        this.initWindow(closeListener);
        this.initComponents();
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
        this.setTitle("Add Account");
        this.setSize(400, 200);
        this.setResizable(false);
        this.setLocationRelativeTo(this.parent);
    }

    private void initComponents() {
        JPanel contentPane = new JPanel();
        contentPane.setLayout(new GridBagLayout());
        {
            JLabel browserLabel = new JLabel("Please open the following URL in your browser:");
            GBC.create(contentPane).grid(0, 0).insets(BORDER_PADDING, BORDER_PADDING, 0, 0).anchor(GridBagConstraints.NORTHWEST).add(browserLabel);

            JLabel urlLabel = new JLabel("<html><a href=\"\">" + this.deviceCode.verificationUri() + "</a></html>");
            urlLabel.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseReleased(MouseEvent e) {
                    AddAccountPopup.this.parent.openURL(AddAccountPopup.this.deviceCode.verificationUri());
                }
            });
            GBC.create(contentPane).grid(0, 1).insets(0, BORDER_PADDING, 0, 0).anchor(GridBagConstraints.NORTHWEST).add(urlLabel);

            JLabel enterCodeLabel = new JLabel("Enter the following code:");
            GBC.create(contentPane).grid(0, 2).insets(BODY_BLOCK_PADDING, BORDER_PADDING, 0, 0).anchor(GridBagConstraints.NORTHWEST).add(enterCodeLabel);

            JLabel codeLabel = new JLabel(this.deviceCode.userCode());
            GBC.create(contentPane).grid(0, 3).insets(0, BORDER_PADDING, 0, 0).anchor(GridBagConstraints.NORTHWEST).add(codeLabel);

            JLabel closeInfo = new JLabel("The popup will close automatically after you have been logged in.");
            GBC.create(contentPane).grid(0, 4).insets(BODY_BLOCK_PADDING, BORDER_PADDING, 0, 0).anchor(GridBagConstraints.NORTHWEST).add(closeInfo);
        }
        {
            JButton copyCodeButton = new JButton("Copy Code");
            copyCodeButton.addActionListener(event -> {
                StringSelection selection = new StringSelection(this.deviceCode.userCode());
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, selection);
            });
            GBC.create(contentPane).grid(0, 5).weightx(1).insets(BORDER_PADDING, BORDER_PADDING, BORDER_PADDING, BORDER_PADDING).fill(GridBagConstraints.HORIZONTAL).add(copyCodeButton);
        }
        this.setContentPane(contentPane);
    }

    public void markExternalClose() {
        this.externalClose = true;
    }

}
