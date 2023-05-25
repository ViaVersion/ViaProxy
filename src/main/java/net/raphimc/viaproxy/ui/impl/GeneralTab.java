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
package net.raphimc.viaproxy.ui.impl;

import com.google.common.net.HostAndPort;
import net.raphimc.viaprotocolhack.util.VersionEnum;
import net.raphimc.viaproxy.ViaProxy;
import net.raphimc.viaproxy.cli.options.Options;
import net.raphimc.viaproxy.plugins.PluginManager;
import net.raphimc.viaproxy.plugins.events.GetDefaultPortEvent;
import net.raphimc.viaproxy.saves.impl.UISave;
import net.raphimc.viaproxy.saves.impl.accounts.OfflineAccount;
import net.raphimc.viaproxy.ui.AUITab;
import net.raphimc.viaproxy.ui.ViaProxyUI;
import net.raphimc.viaproxy.util.logging.Logger;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URI;
import java.net.URISyntaxException;

public class GeneralTab extends AUITab {

    private JTextField serverAddress;
    private JComboBox<VersionEnum> serverVersion;
    private JComboBox<String> authMethod;
    private JCheckBox betaCraftAuth;
    private JLabel stateLabel;
    private JButton stateButton;

    public GeneralTab(final ViaProxyUI frame) {
        super(frame, "General");
    }

    @Override
    protected void init(JPanel contentPane) {
        {
            JLabel titleLabel = new JLabel("ViaProxy");
            titleLabel.setBounds(0, 0, 500, 50);
            titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
            titleLabel.setFont(titleLabel.getFont().deriveFont(30F));
            contentPane.add(titleLabel);
        }
        {
            JLabel copyrightLabel = new JLabel("© RK_01 & Lenni0451");
            copyrightLabel.setBounds(360, 10, 500, 20);
            contentPane.add(copyrightLabel);
        }
        {
            JLabel discordLabel = new JLabel("<html><a href=\"\">Discord</a></html>");
            discordLabel.setBounds(10, 10, 45, 20);
            discordLabel.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseReleased(MouseEvent e) {
                    frame.openURL("https://viaproxy.raphimc.net");
                }
            });
            contentPane.add(discordLabel);
        }
        {
            String toolTipText = "Supported formats:\n" +
                    "- address\n" +
                    "- address:port\n" +
                    "- ClassiCube Direct URL";

            JLabel addressLabel = new JLabel("Server Address:");
            addressLabel.setBounds(10, 50, 100, 20);
            addressLabel.setToolTipText(toolTipText);
            contentPane.add(addressLabel);

            this.serverAddress = new JTextField();
            this.serverAddress.setBounds(10, 70, 465, 22);
            this.serverAddress.setToolTipText(toolTipText);
            ViaProxy.saveManager.uiSave.loadTextField("server_address", this.serverAddress);
            contentPane.add(this.serverAddress);
        }
        {
            JLabel serverVersionLabel = new JLabel("Server Version:");
            serverVersionLabel.setBounds(10, 100, 100, 20);
            contentPane.add(serverVersionLabel);

            this.serverVersion = new JComboBox<>(VersionEnum.SORTED_VERSIONS.toArray(new VersionEnum[0]));
            this.serverVersion.setBounds(10, 120, 465, 22);
            this.serverVersion.setRenderer(new DefaultListCellRenderer() {
                @Override
                public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                    if (value instanceof VersionEnum) {
                        VersionEnum version = (VersionEnum) value;
                        value = version.getName();
                    }
                    return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                }
            });
            this.serverVersion.addActionListener(e -> {
                if (!(this.serverVersion.getSelectedItem() instanceof VersionEnum)) return;
                if (((VersionEnum) this.serverVersion.getSelectedItem()).isOlderThanOrEqualTo(VersionEnum.c0_28toc0_30)) {
                    this.betaCraftAuth.setEnabled(true);
                } else {
                    this.betaCraftAuth.setEnabled(false);
                    this.betaCraftAuth.setSelected(false);
                }
            });
            ViaProxy.saveManager.uiSave.loadComboBox("server_version", this.serverVersion);
            contentPane.add(this.serverVersion);
        }
        {
            JLabel authMethodLabel = new JLabel("Minecraft Account:");
            authMethodLabel.setBounds(10, 150, 400, 20);
            contentPane.add(authMethodLabel);

            this.authMethod = new JComboBox<>(new String[]{"Use no account", "Use selected account", "Use OpenAuthMod"});
            this.authMethod.setBounds(10, 170, 465, 22);
            ViaProxy.saveManager.uiSave.loadComboBox("auth_method", this.authMethod);
            contentPane.add(this.authMethod);
        }
        {
            this.betaCraftAuth = new JCheckBox("BetaCraft Auth (Classic)");
            this.betaCraftAuth.setBounds(10, 200, 250, 20);
            this.betaCraftAuth.setToolTipText("Enabling BetaCraft Auth allows you to join Classic servers which have online mode enabled.");
            ViaProxy.saveManager.uiSave.loadCheckBox("betacraft_auth", this.betaCraftAuth);
            contentPane.add(this.betaCraftAuth);
            this.serverVersion.actionPerformed(null);
        }
        {
            this.stateLabel = new JLabel();
            this.stateLabel.setBounds(14, 230, 465, 20);
            this.stateLabel.setVisible(false);
            contentPane.add(this.stateLabel);
        }
        {
            this.stateButton = new JButton("Loading ViaProxy...");
            this.stateButton.setBounds(10, 250, 465, 20);
            this.stateButton.addActionListener(e -> {
                if (this.stateButton.getText().equalsIgnoreCase("Start")) this.start();
                else if (this.stateButton.getText().equalsIgnoreCase("Stop")) this.stop();
            });
            this.stateButton.setEnabled(false);
            contentPane.add(this.stateButton);
        }
    }

    @Override
    public void setReady() {
        SwingUtilities.invokeLater(() -> {
            this.stateButton.setText("Start");
            this.stateButton.setEnabled(true);
        });
    }

    @Override
    public void onClose() {
        UISave save = ViaProxy.saveManager.uiSave;
        save.put("server_address", this.serverAddress.getText());
        save.put("server_version", String.valueOf(this.serverVersion.getSelectedIndex()));
        save.put("auth_method", String.valueOf(this.authMethod.getSelectedIndex()));
        save.put("betacraft_auth", String.valueOf(this.betaCraftAuth.isSelected()));
        ViaProxy.saveManager.save();
    }

    private void setComponentsEnabled(final boolean state) {
        this.serverAddress.setEnabled(state);
        this.serverVersion.setEnabled(state);
        ViaProxy.ui.advancedTab.bindPort.setEnabled(state);
        this.authMethod.setEnabled(state);
        this.betaCraftAuth.setEnabled(state);
        ViaProxy.ui.advancedTab.proxyOnlineMode.setEnabled(state);
        ViaProxy.ui.advancedTab.proxy.setEnabled(state);
        if (state) this.serverVersion.getActionListeners()[0].actionPerformed(null);
    }

    private void updateStateLabel() {
        this.stateLabel.setText("ViaProxy is running! Connect with Minecraft 1.7+ to 127.0.0.1:" + ViaProxy.ui.advancedTab.bindPort.getValue());
        this.stateLabel.setVisible(true);
    }

    private void start() {
        this.setComponentsEnabled(false);
        this.stateButton.setEnabled(false);
        this.stateButton.setText("Starting...");

        new Thread(() -> {
            String serverAddress = this.serverAddress.getText().trim();
            final VersionEnum serverVersion = (VersionEnum) this.serverVersion.getSelectedItem();
            final int bindPort = (int) ViaProxy.ui.advancedTab.bindPort.getValue();
            final int authMethod = this.authMethod.getSelectedIndex();
            final boolean betaCraftAuth = this.betaCraftAuth.isSelected();
            final boolean proxyOnlineMode = ViaProxy.ui.advancedTab.proxyOnlineMode.isSelected();
            final boolean legacySkinLoading = ViaProxy.ui.advancedTab.legacySkinLoading.isSelected();
            final String proxyUrl = ViaProxy.ui.advancedTab.proxy.getText().trim();

            try {
                try {
                    if (serverAddress.startsWith("mc://")) { // ClassiCube Direct URL
                        final URI uri = new URI(serverAddress);
                        serverAddress = uri.getHost() + ":" + uri.getPort();

                        final String[] path = uri.getPath().substring(1).split("/");
                        if (path.length < 2) {
                            throw new IllegalArgumentException("Invalid ClassiCube Direct URL!");
                        }

                        Options.MC_ACCOUNT = new OfflineAccount(path[0]);
                        Options.CLASSIC_MP_PASS = path[1];
                    } else { // Normal address
                        if (authMethod != 1) {
                            Options.MC_ACCOUNT = null;
                        } else if (Options.MC_ACCOUNT == null) {
                            this.frame.accountsTab.markSelected(0);
                        }
                        Options.CLASSIC_MP_PASS = null;
                    }

                    try {
                        HostAndPort hostAndPort = HostAndPort.fromString(serverAddress);
                        Options.CONNECT_ADDRESS = hostAndPort.getHost();
                        Options.CONNECT_PORT = hostAndPort.getPortOrDefault(PluginManager.EVENT_MANAGER.call(new GetDefaultPortEvent(serverVersion, 25565)).getDefaultPort());
                    } catch (Throwable t) {
                        throw new IllegalArgumentException("Invalid server address!");
                    }

                    Options.BIND_PORT = bindPort;
                    Options.ONLINE_MODE = proxyOnlineMode;
                    Options.PROTOCOL_VERSION = serverVersion;
                    Options.BETACRAFT_AUTH = betaCraftAuth;
                    Options.LEGACY_SKIN_LOADING = legacySkinLoading;

                    if (authMethod == 2) {
                        Options.OPENAUTHMOD_AUTH = true;
                    }
                    if (!proxyUrl.isEmpty()) {
                        try {
                            Options.PROXY_URL = new URI(proxyUrl);
                        } catch (URISyntaxException e) {
                            throw new IllegalArgumentException("Invalid proxy URL!");
                        }
                    }
                } catch (Throwable t) {
                    SwingUtilities.invokeLater(() -> {
                        this.frame.showError(t.getMessage());
                    });
                    throw t;
                }

                try {
                    ViaProxy.startProxy();
                } catch (Throwable e) {
                    SwingUtilities.invokeLater(() -> {
                        this.frame.showError("Failed to start ViaProxy! Ensure that the local port is not already in use and try again.");
                    });
                    throw e;
                }

                SwingUtilities.invokeLater(() -> {
                    this.updateStateLabel();
                    this.stateButton.setEnabled(true);
                    this.stateButton.setText("Stop");
                });
            } catch (Throwable e) {
                Logger.LOGGER.error("Error while starting ViaProxy", e);
                SwingUtilities.invokeLater(() -> {
                    this.setComponentsEnabled(true);
                    this.stateButton.setEnabled(true);
                    this.stateButton.setText("Start");
                    this.stateLabel.setVisible(false);
                });
            }
        }).start();
    }

    private void stop() {
        ViaProxy.stopProxy();

        this.stateLabel.setVisible(false);
        this.stateButton.setText("Start");
        this.setComponentsEnabled(true);
    }

}
