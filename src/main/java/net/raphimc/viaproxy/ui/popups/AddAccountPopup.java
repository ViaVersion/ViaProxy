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

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.function.Consumer;

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
                if (!externalClose) closeListener.run();
            }
        });
        this.setTitle("Add Account");
        this.setSize(400, 200);
        this.setResizable(false);
        this.setLocationRelativeTo(this.parent);
    }

    private void initComponents() {
        JPanel contentPane = new JPanel();
        contentPane.setLayout(null);
        this.setContentPane(contentPane);
        {
            JLabel browserLabel = new JLabel("Please open the following URL in your browser:");
            browserLabel.setBounds(10, 10, 380, 20);
            contentPane.add(browserLabel);

            JLabel urlLabel = new JLabel("<html><a href=\"\">" + this.deviceCode.verificationUri() + "</a></html>");
            urlLabel.setBounds(10, 30, 380, 20);
            urlLabel.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseReleased(MouseEvent e) {
                    parent.openURL(deviceCode.verificationUri());
                }
            });
            contentPane.add(urlLabel);

            JLabel enterCodeLabel = new JLabel("Enter the following code:");
            enterCodeLabel.setBounds(10, 50, 380, 20);
            contentPane.add(enterCodeLabel);

            JLabel codeLabel = new JLabel(this.deviceCode.userCode());
            codeLabel.setBounds(10, 70, 380, 20);
            contentPane.add(codeLabel);

            JLabel closeInfo = new JLabel("The popup will close automatically after you have been logged in.");
            closeInfo.setBounds(10, 100, 380, 20);
            contentPane.add(closeInfo);
        }
        {
            JButton copyCodeButton = new JButton("Copy Code");
            copyCodeButton.setBounds(this.getWidth() / 2 - 130 / 2, 130, 100, 20);
            copyCodeButton.addActionListener(event -> {
                StringSelection selection = new StringSelection(this.deviceCode.userCode());
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, selection);
            });
            contentPane.add(copyCodeButton);
        }
    }

    public void markExternalClose() {
        this.externalClose = true;
    }

}
