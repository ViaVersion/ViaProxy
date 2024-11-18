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

import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import net.lenni0451.commons.swing.GBC;
import net.lenni0451.lambdaevents.EventHandler;
import net.raphimc.viabedrock.api.BedrockProtocolVersion;
import net.raphimc.vialegacy.api.LegacyProtocolVersion;
import net.raphimc.vialoader.util.ProtocolVersionList;
import net.raphimc.viaproxy.ViaProxy;
import net.raphimc.viaproxy.protocoltranslator.viaproxy.ViaProxyConfig;
import net.raphimc.viaproxy.saves.impl.accounts.ClassicAccount;
import net.raphimc.viaproxy.ui.I18n;
import net.raphimc.viaproxy.ui.UITab;
import net.raphimc.viaproxy.ui.ViaProxyWindow;
import net.raphimc.viaproxy.ui.elements.LinkLabel;
import net.raphimc.viaproxy.ui.events.UICloseEvent;
import net.raphimc.viaproxy.util.AddressUtil;
import net.raphimc.viaproxy.util.logging.Logger;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;

import static net.raphimc.viaproxy.ui.ViaProxyWindow.BODY_BLOCK_PADDING;
import static net.raphimc.viaproxy.ui.ViaProxyWindow.BORDER_PADDING;

public class GeneralTab extends UITab {

    JTextField serverAddress;
    JComboBox<ProtocolVersion> serverVersion;
    JComboBox<ViaProxyConfig.AuthMethod> authMethod;
    JCheckBox betaCraftAuth;
    JLabel stateLabel;
    JButton stateButton;

    public GeneralTab(final ViaProxyWindow frame) {
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

        LinkLabel discord = new LinkLabel("Discord", "https://discord.gg/viaversion");
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
            this.serverVersion.setSelectedItem(ViaProxy.getConfig().getTargetVersion());
            GBC.create(body).grid(0, gridy++).weightx(1).insets(0, BORDER_PADDING, 0, BORDER_PADDING).fill(GBC.HORIZONTAL).add(this.serverVersion);
        }
        {
            JLabel minecraftAccountLabel = new JLabel(I18n.get("tab.general.minecraft_account.label"));
            GBC.create(body).grid(0, gridy++).insets(BODY_BLOCK_PADDING, BORDER_PADDING, 0, 0).anchor(GBC.NORTHWEST).add(minecraftAccountLabel);

            this.authMethod = new JComboBox<>(ViaProxyConfig.AuthMethod.values());
            this.authMethod.setRenderer(new DefaultListCellRenderer() {
                @Override
                public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                    if (value instanceof ViaProxyConfig.AuthMethod authMethod) {
                        value = I18n.get(authMethod.getGuiTranslationKey());
                    }
                    return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                }
            });
            this.authMethod.setSelectedItem(ViaProxy.getConfig().getAuthMethod());
            GBC.create(body).grid(0, gridy++).weightx(1).insets(0, BORDER_PADDING, 0, BORDER_PADDING).fill(GBC.HORIZONTAL).add(this.authMethod);
        }
        {
            this.betaCraftAuth = new JCheckBox(I18n.get("tab.general.betacraft_auth.label"));
            this.betaCraftAuth.setToolTipText(I18n.get("tab.general.betacraft_auth.tooltip"));
            this.betaCraftAuth.setSelected(ViaProxy.getConfig().useBetacraftAuth());
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

        final Timer timer = new Timer(100, null);
        timer.addActionListener(event -> {
            if (Via.getManager().getProtocolManager().hasLoadedMappings()) {
                this.stateButton.setText(I18n.get("tab.general.state.start"));
                this.stateButton.setEnabled(true);
                timer.stop();

                if (System.getProperty("viaproxy.gui.autoStart") != null) {
                    this.stateButton.doClick(0);
                }
            }
        });
        timer.start();
    }

    private void setComponentsEnabled(final boolean state) {
        this.serverAddress.setEnabled(state);
        this.serverVersion.setEnabled(state);
        this.viaProxyWindow.advancedTab.bindAddress.setEnabled(state);
        this.authMethod.setEnabled(state);
        this.betaCraftAuth.setEnabled(state);
        this.viaProxyWindow.advancedTab.proxyOnlineMode.setEnabled(state);
        this.viaProxyWindow.advancedTab.proxy.setEnabled(state);
        this.viaProxyWindow.advancedTab.legacySkinLoading.setEnabled(state);
        this.viaProxyWindow.advancedTab.chatSigning.setEnabled(state);
        this.viaProxyWindow.advancedTab.ignorePacketTranslationErrors.setEnabled(state);
        this.viaProxyWindow.advancedTab.allowBetaPinging.setEnabled(state);
        this.viaProxyWindow.advancedTab.simpleVoiceChatSupport.setEnabled(state);
        this.viaProxyWindow.advancedTab.fakeAcceptResourcePacks.setEnabled(state);
        if (state) this.serverVersion.getActionListeners()[0].actionPerformed(null);
    }

    private void updateStateLabel() {
        if (ViaProxy.getConfig().getBindAddress() instanceof InetSocketAddress inetSocketAddress) {
            this.stateLabel.setText(I18n.get("tab.general.state.running", "1.7+", "127.0.0.1:" + inetSocketAddress.getPort()));
        } else {
            this.stateLabel.setText(I18n.get("tab.general.state.running", "1.7+", AddressUtil.toString(ViaProxy.getConfig().getBindAddress())));
        }
        this.stateLabel.setForeground(Color.GREEN);
        this.stateLabel.setVisible(true);
    }

    private void start() {
        final Object selectedVersion = this.serverVersion.getSelectedItem();
        if (!(selectedVersion instanceof ProtocolVersion)) {
            ViaProxyWindow.showError(I18n.get("tab.general.error.no_server_version_selected"));
            return;
        }
        if (ViaProxy.getSaveManager().uiSave.get("notice.ban_warning") == null) {
            ViaProxy.getSaveManager().uiSave.put("notice.ban_warning", "true");
            ViaProxy.getSaveManager().save();

            ViaProxyWindow.showWarning("<html><div style='text-align: center;'>" + I18n.get("tab.general.warning.ban_warning.line1") + "<br><b>" + I18n.get("tab.general.warning.risk") + "</b></div></html>");
        }
        if (selectedVersion.equals(BedrockProtocolVersion.bedrockLatest) && ViaProxy.getSaveManager().uiSave.get("notice.bedrock_warning") == null) {
            ViaProxy.getSaveManager().uiSave.put("notice.bedrock_warning", "true");
            ViaProxy.getSaveManager().save();

            ViaProxyWindow.showWarning("<html><div style='text-align: center;'>" + I18n.get("tab.general.warning.bedrock_warning.line1") + "<br><b>" + I18n.get("tab.general.warning.risk") + "</b></div></html>");
        }

        this.setComponentsEnabled(false);
        this.stateButton.setEnabled(false);
        this.stateButton.setText(I18n.get("tab.general.state.starting"));

        new Thread(() -> {
            final String serverAddress = this.serverAddress.getText().trim();
            final ProtocolVersion serverVersion = (ProtocolVersion) this.serverVersion.getSelectedItem();
            final String bindAddress = this.viaProxyWindow.advancedTab.bindAddress.getText().trim();
            final ViaProxyConfig.AuthMethod authMethod = (ViaProxyConfig.AuthMethod) this.authMethod.getSelectedItem();
            final String proxyUrl = this.viaProxyWindow.advancedTab.proxy.getText().trim();

            try {
                try {
                    if (serverAddress.startsWith("mc://")) { // ClassiCube Direct URL
                        final URI uri = new URI(serverAddress);

                        final String[] path = uri.getPath().substring(1).split("/");
                        if (path.length < 2) {
                            throw new IllegalArgumentException(I18n.get("tab.general.error.invalid_classicube_url"));
                        }

                        ViaProxy.getConfig().setTargetAddress(new InetSocketAddress(uri.getHost(), uri.getPort()));
                        ViaProxy.getConfig().setAccount(new ClassicAccount(path[0], path[1]));
                    } else {
                        try {
                            ViaProxy.getConfig().setTargetAddress(AddressUtil.parse(serverAddress, serverVersion));
                        } catch (Throwable t) {
                            throw new IllegalArgumentException(I18n.get("tab.general.error.invalid_server_address"));
                        }

                        if (authMethod == ViaProxyConfig.AuthMethod.ACCOUNT) {
                            if (ViaProxy.getConfig().getAccount() == null) {
                                this.viaProxyWindow.accountsTab.markSelected(0);
                            }
                        } else {
                            ViaProxy.getConfig().setAccount(null);
                        }
                    }
                    try {
                        ViaProxy.getConfig().setBindAddress(AddressUtil.parse(bindAddress, null));
                    } catch (Throwable t) {
                        throw new IllegalArgumentException(I18n.get("tab.general.error.invalid_bind_address"));
                    }
                    if (!proxyUrl.isBlank()) {
                        try {
                            ViaProxy.getConfig().setBackendProxyUrl(new URI(proxyUrl));
                        } catch (URISyntaxException e) {
                            throw new IllegalArgumentException(I18n.get("tab.general.error.invalid_proxy_url"));
                        }
                    } else {
                        ViaProxy.getConfig().setBackendProxyUrl(null);
                    }
                    this.applyGuiState();
                    this.viaProxyWindow.advancedTab.applyGuiState();
                    ViaProxy.getConfig().save();
                    ViaProxy.getSaveManager().save();
                } catch (Throwable t) {
                    SwingUtilities.invokeLater(() -> ViaProxyWindow.showError(t.getMessage()));
                    throw t;
                }

                try {
                    ViaProxy.startProxy();
                } catch (Throwable e) {
                    SwingUtilities.invokeLater(() -> ViaProxyWindow.showError(I18n.get("tab.general.error.failed_to_start")));
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

    @EventHandler(events = UICloseEvent.class)
    void applyGuiState() {
        ViaProxy.getSaveManager().uiSave.put("server_address", this.serverAddress.getText());
        if (this.serverVersion.getSelectedItem() instanceof ProtocolVersion version) {
            ViaProxy.getConfig().setTargetVersion(version);
        }
        if (this.authMethod.getSelectedItem() instanceof ViaProxyConfig.AuthMethod authMethod) {
            ViaProxy.getConfig().setAuthMethod(authMethod);
        }
        ViaProxy.getConfig().setBetacraftAuth(this.betaCraftAuth.isSelected());
    }

}
