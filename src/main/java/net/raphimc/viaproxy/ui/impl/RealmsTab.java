/*
 * This file is part of ViaProxy - https://github.com/RaphiMC/ViaProxy
 * Copyright (C) 2021-2026 RK_01/RaphiMC and contributors
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

import com.google.common.collect.Iterables;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import com.viaversion.viaversion.api.protocol.version.VersionType;
import net.lenni0451.commons.swing.GBC;
import net.lenni0451.commons.swing.layouts.VerticalLayout;
import net.raphimc.minecraftauth.MinecraftAuth;
import net.raphimc.minecraftauth.extra.realms.exception.RealmsRequestException;
import net.raphimc.minecraftauth.extra.realms.model.RealmsJoinInformation;
import net.raphimc.minecraftauth.extra.realms.model.RealmsServer;
import net.raphimc.minecraftauth.extra.realms.service.RealmsService;
import net.raphimc.minecraftauth.extra.realms.service.impl.BedrockRealmsService;
import net.raphimc.minecraftauth.extra.realms.service.impl.JavaRealmsService;
import net.raphimc.viabedrock.api.BedrockProtocolVersion;
import net.raphimc.viabedrock.protocol.data.ProtocolConstants;
import net.raphimc.viaproxy.ViaProxy;
import net.raphimc.viaproxy.saves.impl.accounts.Account;
import net.raphimc.viaproxy.saves.impl.accounts.BedrockAccount;
import net.raphimc.viaproxy.saves.impl.accounts.MicrosoftAccount;
import net.raphimc.viaproxy.ui.I18n;
import net.raphimc.viaproxy.ui.UITab;
import net.raphimc.viaproxy.ui.ViaProxyWindow;
import net.raphimc.viaproxy.util.logging.Logger;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class RealmsTab extends UITab {

    private static final ProtocolVersion LATEST_JAVA_RELEASE;
    private static final ProtocolVersion LATEST_JAVA_SNAPSHOT;

    static {
        ProtocolVersion latestVersion = null;
        ProtocolVersion latestSnapshotVersion = null;
        final List<ProtocolVersion> supportedVersions = ProtocolVersion.getProtocols();
        for (int i = supportedVersions.size() - 1; i >= 0; i--) {
            final ProtocolVersion version = supportedVersions.get(i);
            if (version.getVersionType() != VersionType.RELEASE) continue;
            if (version.isSnapshot() && latestSnapshotVersion == null) {
                latestSnapshotVersion = version;
            } else if (!version.isSnapshot()) {
                latestVersion = version;
                break;
            }
        }
        if (latestVersion == null) throw new IllegalStateException("Could not find compatible version");
        LATEST_JAVA_RELEASE = latestVersion;
        LATEST_JAVA_SNAPSHOT = latestSnapshotVersion;
    }

    private Account currentAccount = null;
    private ProtocolVersion currentSelectedJavaVersion = LATEST_JAVA_RELEASE;

    public RealmsTab(final ViaProxyWindow frame) {
        super(frame, "realms");
    }

    @Override
    protected void onTabOpened() {
        if (ViaProxy.getConfig().getAccount() != this.currentAccount) {
            this.currentAccount = ViaProxy.getConfig().getAccount();
            this.reinit();
        }
    }

    private void reinit() {
        this.contentPane.removeAll();
        this.init(this.contentPane);
        this.contentPane.revalidate();
        this.contentPane.repaint();
    }

    @Override
    protected void init(JPanel contentPane) {
        final JPanel body = new JPanel();
        body.setLayout(new GridBagLayout());

        final JLabel statusLabel = new JLabel("");
        statusLabel.setFont(statusLabel.getFont().deriveFont(20F));
        statusLabel.setHorizontalAlignment(SwingConstants.CENTER);
        body.add(statusLabel);

        if (this.currentAccount == null) {
            statusLabel.setText(I18n.get("tab.realms.no_account"));
        } else {
            statusLabel.setText(I18n.get("tab.realms.refreshing_account"));
            CompletableFuture.runAsync(() -> {
                try {
                    SwingUtilities.invokeLater(() -> {
                        if (this.currentAccount instanceof MicrosoftAccount account) {
                            final JavaRealmsService realmsService = new JavaRealmsService(MinecraftAuth.createHttpClient(), Iterables.getLast(this.currentSelectedJavaVersion.getIncludedVersions()), account.getAuthManager().getMinecraftToken(), account.getAuthManager().getMinecraftProfile());
                            this.loadRealms(realmsService, body, statusLabel);
                        } else if (this.currentAccount instanceof BedrockAccount account) {
                            final BedrockRealmsService realmsService = new BedrockRealmsService(MinecraftAuth.createHttpClient(), ProtocolConstants.BEDROCK_VERSION_NAME, account.getAuthManager().getRealmsXstsToken());
                            this.loadRealms(realmsService, body, statusLabel);
                        } else {
                            statusLabel.setText(I18n.get("tab.realms.unsupported_account"));
                        }
                    });
                } catch (Throwable e) {
                    Logger.LOGGER.error("Failed to refresh account", e);
                    ViaProxyWindow.showError(I18n.get("tab.realms.error_account", e.getMessage()));
                    SwingUtilities.invokeLater(() -> statusLabel.setText(I18n.get("tab.realms.error_account_label")));
                }
            });
        }

        contentPane.setLayout(new BorderLayout());
        contentPane.add(body, BorderLayout.NORTH);
    }

    private void loadRealms(final RealmsService realmsService, final JPanel body, final JLabel statusLabel) {
        statusLabel.setText(I18n.get("tab.realms.availability_check"));

        realmsService.isCompatibleAsync().thenAccept(state -> {
            if (state) {
                SwingUtilities.invokeLater(() -> statusLabel.setText(I18n.get("tab.realms.loading_worlds")));
                realmsService.getWorldsAsync().thenAccept(servers -> SwingUtilities.invokeLater(() -> {
                    body.remove(statusLabel);
                    this.addHeader(body, realmsService instanceof JavaRealmsService);
                    final JPanel realmsPanel = new JPanel();
                    realmsPanel.setLayout(new VerticalLayout(5, 5));
                    if (servers.isEmpty()) {
                        JLabel label = new JLabel(I18n.get("tab.realms.no_worlds"));
                        label.setHorizontalAlignment(SwingConstants.CENTER);
                        label.setFont(label.getFont().deriveFont(20F));
                        realmsPanel.add(label);
                    } else {
                        this.addRealms(realmsPanel, realmsService, servers);
                    }
                    final JScrollPane realmsScrollPane = new JScrollPane(realmsPanel);
                    realmsScrollPane.getVerticalScrollBar().setUnitIncrement(10);
                    contentPane.add(realmsScrollPane, BorderLayout.CENTER);
                    contentPane.revalidate();
                    contentPane.repaint();
                })).exceptionally(e -> {
                    final Throwable cause = e.getCause();
                    Logger.LOGGER.error("Failed to get realms worlds", cause);
                    ViaProxyWindow.showError(I18n.get("tab.realms.error_generic", cause.getMessage()));
                    SwingUtilities.invokeLater(() -> statusLabel.setText(I18n.get("tab.realms.error_generic_label")));
                    return null;
                });
            } else {
                SwingUtilities.invokeLater(() -> statusLabel.setText(I18n.get("tab.realms.unavailable")));
            }
        }).exceptionally(e -> {
            final Throwable cause = e.getCause();
            Logger.LOGGER.error("Failed to check realms availability", cause);
            ViaProxyWindow.showError(I18n.get("tab.realms.error_generic", cause.getMessage()));
            SwingUtilities.invokeLater(() -> statusLabel.setText(I18n.get("tab.realms.error_generic_label")));
            return null;
        });
    }

    private void addHeader(final JPanel parent, final boolean showType) {
        GBC.create(parent).grid(0, 0).weightx(1).insets(5, 5, 5, 5).fill(GBC.HORIZONTAL).add(new JLabel(I18n.get("tab.realms.account", this.currentAccount.getDisplayString())));
        if (showType && LATEST_JAVA_SNAPSHOT != null) {
            JComboBox<String> type = new JComboBox<>();
            type.addItem(I18n.get("tab.realms.release"));
            type.addItem(I18n.get("tab.realms.snapshot"));
            type.setSelectedIndex(this.currentSelectedJavaVersion.isSnapshot() ? 1 : 0);
            type.addActionListener(e -> {
                ProtocolVersion selected = type.getSelectedIndex() == 0 ? LATEST_JAVA_RELEASE : LATEST_JAVA_SNAPSHOT;
                if (selected != this.currentSelectedJavaVersion) {
                    this.currentSelectedJavaVersion = selected;
                    this.reinit();
                }
            });
            GBC.create(parent).grid(1, 0).insets(5, 0, 5, 5).anchor(GBC.LINE_END).add(type);
        }
    }

    private void addRealms(final JPanel parent, final RealmsService realmsService, final List<RealmsServer> servers) {
        servers.sort((o1, o2) -> {
            boolean o1Compatible = o1.isCompatible() && !o1.isExpired();
            boolean o2Compatible = o2.isCompatible() && !o2.isExpired();
            if (o1Compatible && !o2Compatible) return -1;
            if (!o1Compatible && o2Compatible) return 1;
            return 0;
        });
        for (RealmsServer server : servers) {
            final JPanel panel = new JPanel();
            panel.setLayout(new GridBagLayout());
            panel.setBorder(BorderFactory.createLineBorder(UIManager.getColor("Table.gridColor")));

            String nameString = "";
            if (server.getOwnerName() != null) nameString += server.getOwnerName() + " - ";
            String versionString = "";
            if (server.getActiveVersion() != null) versionString += " - " + server.getActiveVersion();
            GBC.create(panel).grid(0, 0).weightx(1).insets(5, 5, 0, 5).fill(GBC.HORIZONTAL).add(new JLabel(nameString + server.getNameOr("") + " (" + server.getState() + ")"));
            GBC.create(panel).grid(1, 0).insets(5, 5, 0, 5).anchor(GBC.LINE_END).add(new JLabel(server.getWorldType() + versionString));
            GBC.create(panel).grid(0, 1).insets(5, 5, 0, 5).fill(GBC.HORIZONTAL).add(new JLabel(server.getMotdOr("")));
            final JButton join = new JButton(I18n.get("tab.realms.join"));
            if (server.isExpired()) {
                join.setEnabled(false);
                join.setToolTipText(I18n.get("tab.realms.expired"));
            } else if (!server.isCompatible()) {
                join.setEnabled(false);
                join.setToolTipText(I18n.get("tab.realms.incompatible"));
            }
            GBC.create(panel).grid(1, 1).insets(5, 0, 5, 5).anchor(GBC.LINE_END).add(join);
            join.addActionListener(event -> {
                join.setEnabled(false);
                join.setText(I18n.get("tab.realms.joining"));
                realmsService.joinWorldAsync(server).thenAccept(joinInformation -> SwingUtilities.invokeLater(() -> {
                    join.setEnabled(true);
                    join.setText(I18n.get("tab.realms.join"));
                    this.setServerAddressAndStartViaProxy(joinInformation, realmsService instanceof JavaRealmsService ? this.currentSelectedJavaVersion : BedrockProtocolVersion.bedrockLatest);
                })).exceptionally(e -> {
                    final Throwable cause = e.getCause();
                    SwingUtilities.invokeLater(() -> {
                        join.setEnabled(true);
                        join.setText(I18n.get("tab.realms.join"));
                        if (realmsService instanceof JavaRealmsService javaRealmsService && cause instanceof RealmsRequestException realmsRequestException && realmsRequestException.getErrorCode() == RealmsRequestException.ERROR_TOS_NOT_ACCEPTED) {
                            final int chosen = JOptionPane.showConfirmDialog(this.viaProxyWindow, I18n.get("tab.realms.accept_tos", "https://aka.ms/MinecraftRealmsTerms"), "ViaProxy", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
                            if (chosen == JOptionPane.YES_OPTION) {
                                javaRealmsService.acceptTosUnchecked();
                                join.doClick(0);
                            }
                        } else {
                            Logger.LOGGER.error("Failed to join realm", cause);
                            ViaProxyWindow.showError(I18n.get("tab.realms.error_generic", cause.getMessage()));
                        }
                    });
                    return null;
                });
            });

            parent.add(panel);
        }
    }

    private void setServerAddressAndStartViaProxy(final RealmsJoinInformation joinInformation, final ProtocolVersion version) {
        final GeneralTab generalTab = this.viaProxyWindow.generalTab;
        if (generalTab.stateButton.isEnabled()) {
            if (!generalTab.stateButton.getText().equals(I18n.get("tab.general.state.start"))) {
                generalTab.stateButton.doClick(0); // Stop the running proxy
            }

            switch (joinInformation.getNetworkProtocol()) {
                case RealmsJoinInformation.PROTOCOL_DEFAULT -> generalTab.serverAddress.setText(joinInformation.getAddress());
                case RealmsJoinInformation.PROTOCOL_NETHERNET -> generalTab.serverAddress.setText("nethernet://" + joinInformation.getAddress());
                case RealmsJoinInformation.PROTOCOL_NETHERNET_JSONRPC -> generalTab.serverAddress.setText("nethernet-rpc://" + joinInformation.getAddress());
                default -> throw new IllegalArgumentException("Unknown realms network protocol: " + joinInformation.getNetworkProtocol());
            }

            generalTab.serverVersion.setSelectedItem(version);
            generalTab.authMethod.setSelectedIndex(0);
            generalTab.stateButton.doClick(0);
            this.viaProxyWindow.contentPane.setSelectedIndex(0);
        }
    }

}
