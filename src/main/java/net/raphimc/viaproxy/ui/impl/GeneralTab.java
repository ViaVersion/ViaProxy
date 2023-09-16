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
import net.lenni0451.lambdaevents.EventHandler;
import net.raphimc.vialoader.util.VersionEnum;
import net.raphimc.viaproxy.ViaProxy;
import net.raphimc.viaproxy.cli.options.Options;
import net.raphimc.viaproxy.plugins.PluginManager;
import net.raphimc.viaproxy.plugins.events.GetDefaultPortEvent;
import net.raphimc.viaproxy.saves.impl.UISave;
import net.raphimc.viaproxy.saves.impl.accounts.OfflineAccount;
import net.raphimc.viaproxy.ui.AUITab;
import net.raphimc.viaproxy.ui.ViaProxyUI;
import net.raphimc.viaproxy.ui.events.UICloseEvent;
import net.raphimc.viaproxy.ui.events.UIInitEvent;
import net.raphimc.viaproxy.util.GBC;
import net.raphimc.viaproxy.util.logging.Logger;
import org.jdesktop.swingx.VerticalLayout;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URI;
import java.net.URISyntaxException;

import static net.raphimc.viaproxy.ui.ViaProxyUI.BODY_BLOCK_PADDING;
import static net.raphimc.viaproxy.ui.ViaProxyUI.BORDER_PADDING;

public class GeneralTab extends AUITab {

    private JTextField serverAddress;
    private JComboBox<VersionEnum> serverVersion;
    private JComboBox<String> authMethod;
    private JCheckBox betaCraftAuth;
    private JLabel stateLabelLabel;
    private JButton stateButton;

    public GeneralTab(final ViaProxyUI frame) {
        super(frame, "General");
    }

    @Override
    protected void init(JPanel contentPane) {
        JPanel top = new JPanel();
        top.setLayout(new VerticalLayout());

        JPanel bottom = new JPanel();
        bottom.setLayout(new VerticalLayout());

        this.addHeader(top);
        this.addBody(top);
        this.addFooter(bottom);

        contentPane.setLayout(new BorderLayout());
        contentPane.add(top, BorderLayout.NORTH);
        contentPane.add(bottom, BorderLayout.SOUTH);
    }

    private void addHeader(final Container parent) {
        JPanel header = new JPanel();
        header.setLayout(new GridBagLayout());

        JLabel discord = new JLabel("<html><a href=\"\">Discord</a></html>");
        discord.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                frame.openURL("https://discord.gg/viaversion");
            }
        });
        discord.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        GBC.create(header).grid(0, 0).width(0).insets(BORDER_PADDING, BORDER_PADDING, 0, 0).anchor(GridBagConstraints.NORTHWEST).add(discord);

        JLabel title = new JLabel("ViaProxy");
        title.setFont(title.getFont().deriveFont(30F));
        GBC.create(header).grid(1, 0).weightx(1).width(0).insets(BORDER_PADDING, 0, 0, 0).anchor(GridBagConstraints.CENTER).add(title);

        JLabel copyright = new JLabel("© RK_01 & Lenni0451");
        GBC.create(header).grid(2, 0).width(0).insets(BORDER_PADDING, 0, 0, BORDER_PADDING).anchor(GridBagConstraints.NORTHEAST).add(copyright);

        parent.add(header);
    }

    private void addBody(final Container parent) {
        JPanel body = new JPanel();
        body.setLayout(new GridBagLayout());

        int gridy = 0;
        {
            String toolTipText = """
                    Supported formats:
                    - address
                    - address:port
                    - ClassiCube Direct URL""";

            JLabel serverAddressLabel = new JLabel("Server Address:");
            serverAddressLabel.setToolTipText(toolTipText);
            GBC.create(body).grid(0, gridy++).insets(BODY_BLOCK_PADDING, BORDER_PADDING, 0, 0).anchor(GridBagConstraints.NORTHWEST).add(serverAddressLabel);

            this.serverAddress = new JTextField();
            this.serverAddress.setToolTipText(toolTipText);
            ViaProxy.saveManager.uiSave.loadTextField("server_address", this.serverAddress);
            GBC.create(body).grid(0, gridy++).weightx(1).insets(0, BORDER_PADDING, 0, BORDER_PADDING).fill(GridBagConstraints.HORIZONTAL).add(this.serverAddress);
        }
        {
            JLabel serverVersionLabel = new JLabel("Server Version:");
            GBC.create(body).grid(0, gridy++).insets(BODY_BLOCK_PADDING, BORDER_PADDING, 0, 0).anchor(GridBagConstraints.NORTHWEST).add(serverVersionLabel);

            this.serverVersion = new JComboBox<>(VersionEnum.SORTED_VERSIONS.toArray(new VersionEnum[0]));
            this.serverVersion.setRenderer(new DefaultListCellRenderer() {
                @Override
                public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                    if (value instanceof VersionEnum version) value = version.getName();
                    return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                }
            });
            this.serverVersion.addActionListener(event -> {
                if (this.betaCraftAuth == null) return; // This is called when the JComboBox is created (before betaCraftAuth is set)
                if (!(this.serverVersion.getSelectedItem() instanceof VersionEnum)) return;
                if (((VersionEnum) this.serverVersion.getSelectedItem()).isOlderThanOrEqualTo(VersionEnum.c0_28toc0_30)) {
                    this.betaCraftAuth.setEnabled(true);
                } else {
                    this.betaCraftAuth.setEnabled(false);
                    this.betaCraftAuth.setSelected(false);
                }
            });
            ViaProxy.saveManager.uiSave.loadComboBox("server_version", this.serverVersion);
            GBC.create(body).grid(0, gridy++).weightx(1).insets(0, BORDER_PADDING, 0, BORDER_PADDING).fill(GridBagConstraints.HORIZONTAL).add(this.serverVersion);
        }
        {
            JLabel minecraftAccountLabel = new JLabel("Minecraft Account:");
            GBC.create(body).grid(0, gridy++).insets(BODY_BLOCK_PADDING, BORDER_PADDING, 0, 0).anchor(GridBagConstraints.NORTHWEST).add(minecraftAccountLabel);

            this.authMethod = new JComboBox(new String[]{"Use no account", "Use selected account", "Use OpenAuthMod"});
            ViaProxy.saveManager.uiSave.loadComboBox("auth_method", this.authMethod);
            GBC.create(body).grid(0, gridy++).weightx(1).insets(0, BORDER_PADDING, 0, BORDER_PADDING).fill(GridBagConstraints.HORIZONTAL).add(this.authMethod);
        }
        {
            this.betaCraftAuth = new JCheckBox("BetaCraft Auth (Classic)");
            this.betaCraftAuth.setToolTipText("Enabling BetaCraft Auth allows you to join Classic servers which have online mode enabled.");
            ViaProxy.saveManager.uiSave.loadCheckBox("betacraft_auth", this.betaCraftAuth);
            GBC.create(body).grid(0, gridy++).insets(BODY_BLOCK_PADDING, BORDER_PADDING, 0, 0).anchor(GridBagConstraints.NORTHWEST).add(this.betaCraftAuth);
            // Simulate user action on serverVersion to update betaCraftAuth
            final ActionEvent fakeAction = new ActionEvent(this.serverVersion, ActionEvent.ACTION_PERFORMED, "");
            for (ActionListener listener : this.serverVersion.getActionListeners()) {
                listener.actionPerformed(fakeAction);
            }
        }

        parent.add(body);
    }

    private void addFooter(final Container parent) {
        JPanel footer = new JPanel();
        footer.setLayout(new GridBagLayout());

        this.stateLabelLabel = new JLabel("State: Not Connected");
        this.stateLabelLabel.setVisible(false);
        GBC.create(footer).grid(0, 0).weightx(1).insets(0, BORDER_PADDING, 0, BORDER_PADDING).anchor(GridBagConstraints.WEST).fill(GridBagConstraints.HORIZONTAL).add(this.stateLabelLabel);

        this.stateButton = new JButton("Loading ViaProxy...");
        this.stateButton.addActionListener(event -> {
            if (this.stateButton.getText().equalsIgnoreCase("Start")) this.start();
            else if (this.stateButton.getText().equalsIgnoreCase("Stop")) this.stop();
        });
        this.stateButton.setEnabled(false);
        GBC.create(footer).grid(0, 1).weightx(1).insets(0, BORDER_PADDING, BORDER_PADDING, BORDER_PADDING).anchor(GridBagConstraints.WEST).fill(GridBagConstraints.HORIZONTAL).add(this.stateButton);

        parent.add(footer);
    }

    @EventHandler
    private void setReady(final UIInitEvent event) {
        SwingUtilities.invokeLater(() -> {
            this.stateButton.setText("Start");
            this.stateButton.setEnabled(true);
        });
    }

    @EventHandler
    private void onClose(final UICloseEvent event) {
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
        ViaProxy.ui.advancedTab.legacySkinLoading.setEnabled(state);
        ViaProxy.ui.advancedTab.chatSigning.setEnabled(state);
        ViaProxy.ui.advancedTab.ignorePacketTranslationErrors.setEnabled(state);
        if (state) this.serverVersion.getActionListeners()[0].actionPerformed(null);
    }

    private void updateStateLabel() {
        this.stateLabelLabel.setText("ViaProxy is running! Connect with Minecraft 1.7+ to 127.0.0.1:" + ViaProxy.ui.advancedTab.bindPort.getValue());
        this.stateLabelLabel.setVisible(true);
    }

    private void start() {
        Object selectedItem = this.serverVersion.getSelectedItem();
        if (!(selectedItem instanceof VersionEnum)) {
            this.frame.showError("Please select a server version!");
            return;
        }
        if (ViaProxy.saveManager.uiSave.get("notice.ban_warning") == null) {
            ViaProxy.saveManager.uiSave.put("notice.ban_warning", "true");
            ViaProxy.saveManager.save();

            this.frame.showWarning("<html><div style='text-align: center;'>ViaProxy may trigger anti-cheats, due to block, item, movement and other differences between versions.<br><b>USE AT YOUR OWN RISK!</b></div></html>");
        }
        if (VersionEnum.bedrockLatest.equals(selectedItem) && ViaProxy.saveManager.uiSave.get("notice.bedrock_warning") == null) {
            ViaProxy.saveManager.uiSave.put("notice.bedrock_warning", "true");
            ViaProxy.saveManager.save();

            this.frame.showWarning("<html><div style='text-align: center;'>ViaBedrock is currently in very early development and not ready for general use.<br><b>CONTINUE AT YOUR OWN RISK!</b></div></html>");
        }

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
            final boolean chatSigning = ViaProxy.ui.advancedTab.chatSigning.isSelected();
            final boolean ignorePacketTranslationErrors = ViaProxy.ui.advancedTab.ignorePacketTranslationErrors.isSelected();

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
                    Options.OPENAUTHMOD_AUTH = authMethod == 2;
                    Options.CHAT_SIGNING = chatSigning;
                    Options.IGNORE_PACKET_TRANSLATION_ERRORS = ignorePacketTranslationErrors;

                    if (!proxyUrl.isEmpty()) {
                        try {
                            Options.PROXY_URL = new URI(proxyUrl);
                        } catch (URISyntaxException e) {
                            throw new IllegalArgumentException("Invalid proxy URL!");
                        }
                    } else {
                        Options.PROXY_URL = null;
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
                    this.stateLabelLabel.setVisible(false);
                });
            }
        }).start();
    }

    private void stop() {
        ViaProxy.stopProxy();

        this.stateLabelLabel.setVisible(false);
        this.stateButton.setText("Start");
        this.setComponentsEnabled(true);
    }

}
