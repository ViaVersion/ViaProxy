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
package net.raphimc.viaproxy.ui.impl;

import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import net.lenni0451.commons.swing.GBC;
import net.lenni0451.lambdaevents.EventHandler;
import net.raphimc.viabedrock.api.BedrockProtocolVersion;
import net.raphimc.vialegacy.api.LegacyProtocolVersion;
import net.raphimc.vialoader.util.ProtocolVersionList;
import net.raphimc.viaproxy.ViaProxy;
import net.raphimc.viaproxy.cli.options.Options;
import net.raphimc.viaproxy.saves.impl.UISave;
import net.raphimc.viaproxy.saves.impl.accounts.OfflineAccount;
import net.raphimc.viaproxy.ui.AUITab;
import net.raphimc.viaproxy.ui.I18n;
import net.raphimc.viaproxy.ui.ViaProxyUI;
import net.raphimc.viaproxy.ui.events.UICloseEvent;
import net.raphimc.viaproxy.ui.events.UIInitEvent;
import net.raphimc.viaproxy.util.AddressUtil;
import net.raphimc.viaproxy.util.logging.Logger;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;

import static net.raphimc.viaproxy.ui.ViaProxyUI.BODY_BLOCK_PADDING;
import static net.raphimc.viaproxy.ui.ViaProxyUI.BORDER_PADDING;

public class GeneralTab extends AUITab {

    JTextField serverAddress;
    JComboBox<ProtocolVersion> serverVersion;
    JComboBox<String> authMethod;
    private JCheckBox betaCraftAuth;
    private JLabel stateLabel;
    JButton stateButton;

    public GeneralTab(final ViaProxyUI frame) {
        super(frame, "general");
    }

    @Override
    protected void init(JPanel contentPane) {
        JPanel top = new JPanel();
        top.setLayout(new BorderLayout());

        contentPane.setLayout(new BorderLayout());
        contentPane.add(top, BorderLayout.NORTH);

        this.addHeader(top);
        this.addBody(top);
        this.addFooter(contentPane);
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
        GBC.create(header).grid(0, 0).width(0).insets(BORDER_PADDING, BORDER_PADDING, 0, 0).anchor(GBC.NORTHWEST).add(discord);

        JLabel title = new JLabel("ViaProxy");
        title.setFont(title.getFont().deriveFont(30F));
        GBC.create(header).grid(1, 0).weightx(1).width(0).insets(BORDER_PADDING, 0, 0, 0).anchor(GBC.CENTER).add(title);

        JLabel copyright = new JLabel("© RK_01 & Lenni0451");
        GBC.create(header).grid(2, 0).width(0).insets(BORDER_PADDING, 0, 0, BORDER_PADDING).anchor(GBC.NORTHEAST).add(copyright);

        parent.add(header, BorderLayout.NORTH);
    }

    private void addBody(final Container parent) {
        JPanel body = new JPanel();
        body.setLayout(new GridBagLayout());

        int gridy = 0;
        {
            JLabel serverAddressLabel = new JLabel(I18n.get("tab.general.server_address.label"));
            serverAddressLabel.setToolTipText(I18n.get("tab.general.server_address.tooltip"));
            GBC.create(body).grid(0, gridy++).insets(BODY_BLOCK_PADDING, BORDER_PADDING, 0, 0).anchor(GBC.NORTHWEST).add(serverAddressLabel);

            this.serverAddress = new JTextField();
            this.serverAddress.setToolTipText(I18n.get("tab.general.server_address.tooltip"));
            ViaProxy.getSaveManager().uiSave.loadTextField("server_address", this.serverAddress);
            GBC.create(body).grid(0, gridy++).weightx(1).insets(0, BORDER_PADDING, 0, BORDER_PADDING).fill(GBC.HORIZONTAL).add(this.serverAddress);
        }
        {
            JLabel serverVersionLabel = new JLabel(I18n.get("tab.general.server_version.label"));
            GBC.create(body).grid(0, gridy++).insets(BODY_BLOCK_PADDING, BORDER_PADDING, 0, 0).anchor(GBC.NORTHWEST).add(serverVersionLabel);

            this.serverVersion = new JComboBox<>(ProtocolVersionList.getProtocolsNewToOld().toArray(new ProtocolVersion[0]));
            this.serverVersion.setRenderer(new DefaultListCellRenderer() {
                @Override
                public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                    if (value instanceof ProtocolVersion version) {
                        value = version.getName();
                    }
                    return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                }
            });
            this.serverVersion.addActionListener(event -> {
                if (this.betaCraftAuth == null) return; // This is called when the JComboBox is created (before betaCraftAuth is set)
                if (!(this.serverVersion.getSelectedItem() instanceof ProtocolVersion selectedVersion)) return;
                if (selectedVersion.olderThanOrEqualTo(LegacyProtocolVersion.c0_28toc0_30)) {
                    this.betaCraftAuth.setEnabled(true);
                } else {
                    this.betaCraftAuth.setEnabled(false);
                    this.betaCraftAuth.setSelected(false);
                }
            });
            ViaProxy.getSaveManager().uiSave.loadComboBoxProtocolVersion("server_version", this.serverVersion);
            GBC.create(body).grid(0, gridy++).weightx(1).insets(0, BORDER_PADDING, 0, BORDER_PADDING).fill(GBC.HORIZONTAL).add(this.serverVersion);
        }
        {
            JLabel minecraftAccountLabel = new JLabel(I18n.get("tab.general.minecraft_account.label"));
            GBC.create(body).grid(0, gridy++).insets(BODY_BLOCK_PADDING, BORDER_PADDING, 0, 0).anchor(GBC.NORTHWEST).add(minecraftAccountLabel);

            this.authMethod = new JComboBox<>(new String[]{I18n.get("tab.general.minecraft_account.option_select_account"), I18n.get("tab.general.minecraft_account.option_no_account"), I18n.get("tab.general.minecraft_account.option_openauthmod")});
            ViaProxy.getSaveManager().uiSave.loadComboBox("auth_method", this.authMethod);
            GBC.create(body).grid(0, gridy++).weightx(1).insets(0, BORDER_PADDING, 0, BORDER_PADDING).fill(GBC.HORIZONTAL).add(this.authMethod);
        }
        {
            this.betaCraftAuth = new JCheckBox(I18n.get("tab.general.betacraft_auth.label"));
            this.betaCraftAuth.setToolTipText(I18n.get("tab.general.betacraft_auth.tooltip"));
            ViaProxy.getSaveManager().uiSave.loadCheckBox("betacraft_auth", this.betaCraftAuth);
            GBC.create(body).grid(0, gridy++).insets(BODY_BLOCK_PADDING, BORDER_PADDING, 0, 0).anchor(GBC.NORTHWEST).add(this.betaCraftAuth);
            // Simulate user action on serverVersion to update betaCraftAuth
            final ActionEvent fakeAction = new ActionEvent(this.serverVersion, ActionEvent.ACTION_PERFORMED, "");
            for (ActionListener listener : this.serverVersion.getActionListeners()) {
                listener.actionPerformed(fakeAction);
            }
        }

        parent.add(body, BorderLayout.CENTER);
    }

    private void addFooter(final Container parent) {
        JPanel footer = new JPanel();
        footer.setLayout(new GridBagLayout());

        this.stateLabel = new JLabel("");
        this.stateLabel.setVisible(false);
        GBC.create(footer).grid(0, 0).weightx(1).insets(0, BORDER_PADDING, 0, BORDER_PADDING).anchor(GBC.WEST).fill(GBC.HORIZONTAL).add(this.stateLabel);

        this.stateButton = new JButton(I18n.get("tab.general.state.loading"));
        this.stateButton.addActionListener(event -> {
            if (this.stateButton.getText().equalsIgnoreCase(I18n.get("tab.general.state.start"))) this.start();
            else if (this.stateButton.getText().equalsIgnoreCase(I18n.get("tab.general.state.stop"))) this.stop();
        });
        this.stateButton.setEnabled(false);
        GBC.create(footer).grid(0, 1).weightx(1).insets(0, BORDER_PADDING, BORDER_PADDING, BORDER_PADDING).anchor(GBC.WEST).fill(GBC.HORIZONTAL).add(this.stateButton);

        parent.add(footer, BorderLayout.SOUTH);
    }

    @EventHandler
    private void setReady(final UIInitEvent event) {
        SwingUtilities.invokeLater(() -> {
            this.stateButton.setText(I18n.get("tab.general.state.start"));
            this.stateButton.setEnabled(true);
        });
    }

    @EventHandler
    private void onClose(final UICloseEvent event) {
        UISave save = ViaProxy.getSaveManager().uiSave;
        save.put("server_address", this.serverAddress.getText());
        if (this.serverVersion.getSelectedItem() instanceof ProtocolVersion version) {
            save.put("server_version", version.getName());
        }
        save.put("auth_method", String.valueOf(this.authMethod.getSelectedIndex()));
        save.put("betacraft_auth", String.valueOf(this.betaCraftAuth.isSelected()));
        ViaProxy.getSaveManager().save();
    }

    private void setComponentsEnabled(final boolean state) {
        this.serverAddress.setEnabled(state);
        this.serverVersion.setEnabled(state);
        ViaProxy.getUI().advancedTab.bindPort.setEnabled(state);
        this.authMethod.setEnabled(state);
        this.betaCraftAuth.setEnabled(state);
        ViaProxy.getUI().advancedTab.proxyOnlineMode.setEnabled(state);
        ViaProxy.getUI().advancedTab.proxy.setEnabled(state);
        ViaProxy.getUI().advancedTab.legacySkinLoading.setEnabled(state);
        ViaProxy.getUI().advancedTab.chatSigning.setEnabled(state);
        ViaProxy.getUI().advancedTab.ignorePacketTranslationErrors.setEnabled(state);
        if (state) this.serverVersion.getActionListeners()[0].actionPerformed(null);
    }

    private void updateStateLabel() {
        this.stateLabel.setText(I18n.get("tab.general.state.running", "1.7+", "127.0.0.1:" + ViaProxy.getUI().advancedTab.bindPort.getValue()));
        this.stateLabel.setForeground(Color.GREEN);
        this.stateLabel.setVisible(true);
    }

    private void start() {
        final Object selectedVersion = this.serverVersion.getSelectedItem();
        if (!(selectedVersion instanceof ProtocolVersion)) {
            this.frame.showError(I18n.get("tab.general.error.no_server_version_selected"));
            return;
        }
        if (ViaProxy.getSaveManager().uiSave.get("notice.ban_warning") == null) {
            ViaProxy.getSaveManager().uiSave.put("notice.ban_warning", "true");
            ViaProxy.getSaveManager().save();

            this.frame.showWarning("<html><div style='text-align: center;'>" + I18n.get("tab.general.warning.ban_warning.line1") + "<br><b>" + I18n.get("tab.general.warning.risk") + "</b></div></html>");
        }
        if (selectedVersion.equals(BedrockProtocolVersion.bedrockLatest) && ViaProxy.getSaveManager().uiSave.get("notice.bedrock_warning") == null) {
            ViaProxy.getSaveManager().uiSave.put("notice.bedrock_warning", "true");
            ViaProxy.getSaveManager().save();

            this.frame.showWarning("<html><div style='text-align: center;'>" + I18n.get("tab.general.warning.bedrock_warning.line1") + "<br><b>" + I18n.get("tab.general.warning.risk") + "</b></div></html>");
        }

        this.setComponentsEnabled(false);
        this.stateButton.setEnabled(false);
        this.stateButton.setText(I18n.get("tab.general.state.starting"));

        new Thread(() -> {
            String serverAddress = this.serverAddress.getText().trim();
            final ProtocolVersion serverVersion = (ProtocolVersion) this.serverVersion.getSelectedItem();
            final int bindPort = (int) ViaProxy.getUI().advancedTab.bindPort.getValue();
            final int authMethod = this.authMethod.getSelectedIndex();
            final boolean betaCraftAuth = this.betaCraftAuth.isSelected();
            final boolean proxyOnlineMode = ViaProxy.getUI().advancedTab.proxyOnlineMode.isSelected();
            final boolean legacySkinLoading = ViaProxy.getUI().advancedTab.legacySkinLoading.isSelected();
            final String proxyUrl = ViaProxy.getUI().advancedTab.proxy.getText().trim();
            final boolean chatSigning = ViaProxy.getUI().advancedTab.chatSigning.isSelected();
            final boolean ignorePacketTranslationErrors = ViaProxy.getUI().advancedTab.ignorePacketTranslationErrors.isSelected();

            try {
                try {
                    if (serverAddress.startsWith("mc://")) { // ClassiCube Direct URL
                        final URI uri = new URI(serverAddress);

                        final String[] path = uri.getPath().substring(1).split("/");
                        if (path.length < 2) {
                            throw new IllegalArgumentException(I18n.get("tab.general.error.invalid_classicube_url"));
                        }

                        Options.CONNECT_ADDRESS = new InetSocketAddress(uri.getHost(), uri.getPort());
                        Options.MC_ACCOUNT = new OfflineAccount(path[0]);
                        Options.CLASSIC_MP_PASS = path[1];
                    } else {
                        try {
                            Options.CONNECT_ADDRESS = AddressUtil.parse(serverAddress, serverVersion);
                        } catch (Throwable t) {
                            throw new IllegalArgumentException(I18n.get("tab.general.error.invalid_server_address"));
                        }

                        if (authMethod != 0) {
                            Options.MC_ACCOUNT = null;
                        } else if (Options.MC_ACCOUNT == null) {
                            this.frame.accountsTab.markSelected(0);
                        }
                        Options.CLASSIC_MP_PASS = null;
                    }

                    Options.BIND_ADDRESS = new InetSocketAddress("0.0.0.0", bindPort);
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
                            throw new IllegalArgumentException(I18n.get("tab.general.error.invalid_proxy_url"));
                        }
                    } else {
                        Options.PROXY_URL = null;
                    }
                } catch (Throwable t) {
                    SwingUtilities.invokeLater(() -> this.frame.showError(t.getMessage()));
                    throw t;
                }

                try {
                    ViaProxy.startProxy();
                } catch (Throwable e) {
                    SwingUtilities.invokeLater(() -> this.frame.showError(I18n.get("tab.general.error.failed_to_start")));
                    throw e;
                }

                SwingUtilities.invokeLater(() -> {
                    this.updateStateLabel();
                    this.stateButton.setEnabled(true);
                    this.stateButton.setText(I18n.get("tab.general.state.stop"));
                });
            } catch (Throwable e) {
                Logger.LOGGER.error("Error while starting ViaProxy", e);
                SwingUtilities.invokeLater(() -> {
                    this.setComponentsEnabled(true);
                    this.stateButton.setEnabled(true);
                    this.stateButton.setText(I18n.get("tab.general.state.start"));
                    this.stateLabel.setVisible(false);
                });
            }
        }).start();
    }

    private void stop() {
        ViaProxy.stopProxy();

        this.stateLabel.setVisible(false);
        this.stateButton.setText(I18n.get("tab.general.state.start"));
        this.setComponentsEnabled(true);
    }

}
