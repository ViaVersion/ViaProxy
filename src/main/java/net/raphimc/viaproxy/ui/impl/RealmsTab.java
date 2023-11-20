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

import com.google.common.collect.Iterables;
import net.raphimc.minecraftauth.responsehandler.exception.RealmsResponseException;
import net.raphimc.minecraftauth.service.realms.AbstractRealmsService;
import net.raphimc.minecraftauth.service.realms.BedrockRealmsService;
import net.raphimc.minecraftauth.service.realms.JavaRealmsService;
import net.raphimc.minecraftauth.service.realms.model.RealmsWorld;
import net.raphimc.minecraftauth.util.MicrosoftConstants;
import net.raphimc.viabedrock.protocol.data.ProtocolConstants;
import net.raphimc.vialoader.util.VersionEnum;
import net.raphimc.viaproxy.ViaProxy;
import net.raphimc.viaproxy.cli.options.Options;
import net.raphimc.viaproxy.saves.impl.accounts.Account;
import net.raphimc.viaproxy.saves.impl.accounts.BedrockAccount;
import net.raphimc.viaproxy.saves.impl.accounts.MicrosoftAccount;
import net.raphimc.viaproxy.ui.AUITab;
import net.raphimc.viaproxy.ui.I18n;
import net.raphimc.viaproxy.ui.ViaProxyUI;
import net.raphimc.viaproxy.util.GBC;
import net.raphimc.viaproxy.util.PaddedVerticalLayout;
import net.raphimc.viaproxy.util.logging.Logger;
import org.apache.http.impl.client.CloseableHttpClient;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class RealmsTab extends AUITab {

    private static final VersionEnum LATEST_JAVA_RELEASE;
    private static final VersionEnum LATEST_JAVA_SNAPSHOT;
    private static final CloseableHttpClient HTTP_CLIENT = MicrosoftConstants.createHttpClient();

    static {
        VersionEnum latestVersion = null;
        VersionEnum latestSnapshotVersion = null;
        for (int i = VersionEnum.OFFICIAL_SUPPORTED_PROTOCOLS.size() - 1; i >= 0; i--) {
            VersionEnum version = VersionEnum.OFFICIAL_SUPPORTED_PROTOCOLS.get(i);
            if (version.getProtocol().isSnapshot() && latestSnapshotVersion == null) {
                latestSnapshotVersion = version;
            } else if (!version.getProtocol().isSnapshot()) {
                latestVersion = version;
                break;
            }
        }
        if (latestVersion == null) throw new IllegalStateException("Could not find compatible version");
        LATEST_JAVA_RELEASE = latestVersion;
        LATEST_JAVA_SNAPSHOT = latestSnapshotVersion;
    }

    private Account currentAccount = Options.MC_ACCOUNT;
    private VersionEnum currentSelectedJavaVersion = LATEST_JAVA_RELEASE;

    public RealmsTab(final ViaProxyUI frame) {
        super(frame, "realms");
    }

    @Override
    protected void onTabOpened() {
        if (Options.MC_ACCOUNT != this.currentAccount) {
            this.currentAccount = Options.MC_ACCOUNT;
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
        } else if (this.currentAccount instanceof MicrosoftAccount account) {
            final JavaRealmsService realmsService = new JavaRealmsService(HTTP_CLIENT, Iterables.getLast(this.currentSelectedJavaVersion.getProtocol().getIncludedVersions()), account.getMcProfile());
            this.loadRealms(realmsService, body, statusLabel);
        } else if (this.currentAccount instanceof BedrockAccount account) {
            final BedrockRealmsService realmsService = new BedrockRealmsService(HTTP_CLIENT, ProtocolConstants.BEDROCK_VERSION_NAME, account.getRealmsXsts());
            this.loadRealms(realmsService, body, statusLabel);
        } else {
            statusLabel.setText(I18n.get("tab.realms.unsupported_account"));
        }

        contentPane.setLayout(new BorderLayout());
        contentPane.add(body, BorderLayout.NORTH);
    }

    private void loadRealms(final AbstractRealmsService realmsService, final JPanel body, final JLabel statusLabel) {
        statusLabel.setText(I18n.get("tab.realms.availability_check"));

        realmsService.isAvailable().thenAccept(state -> {
            if (state) {
                SwingUtilities.invokeLater(() -> statusLabel.setText(I18n.get("tab.realms.loading_worlds")));
                realmsService.getWorlds().thenAccept(worlds -> SwingUtilities.invokeLater(() -> {
                    body.remove(statusLabel);
                    this.addHeader(body, realmsService instanceof JavaRealmsService);
                    final JPanel realmsPanel = new JPanel();
                    realmsPanel.setLayout(new PaddedVerticalLayout(5, 5));
                    if (worlds.isEmpty()) {
                        JLabel label = new JLabel(I18n.get("tab.realms.no_worlds"));
                        label.setHorizontalAlignment(SwingConstants.CENTER);
                        label.setFont(label.getFont().deriveFont(20F));
                        realmsPanel.add(label);
                    } else {
                        this.addRealms(realmsPanel, realmsService, worlds);
                    }
                    final JScrollPane realmsScrollPane = new JScrollPane(realmsPanel);
                    realmsScrollPane.getVerticalScrollBar().setUnitIncrement(10);
                    contentPane.add(realmsScrollPane, BorderLayout.CENTER);
                    contentPane.revalidate();
                    contentPane.repaint();
                })).exceptionally(e -> {
                    final Throwable cause = e.getCause();
                    Logger.LOGGER.error("Failed to get realms worlds", cause);
                    ViaProxy.getUI().showError(I18n.get("tab.realms.error_generic", cause.getMessage()));
                    SwingUtilities.invokeLater(() -> statusLabel.setText(I18n.get("tab.realms.error_label")));
                    return null;
                });
            } else {
                SwingUtilities.invokeLater(() -> statusLabel.setText(I18n.get("tab.realms.unavailable")));
            }
        }).exceptionally(e -> {
            final Throwable cause = e.getCause();
            Logger.LOGGER.error("Failed to check realms availability", cause);
            ViaProxy.getUI().showError(I18n.get("tab.realms.error_generic", cause.getMessage()));
            SwingUtilities.invokeLater(() -> statusLabel.setText(I18n.get("tab.realms.error_label")));
            return null;
        });
    }

    private void addHeader(final JPanel parent, final boolean showType) {
        GBC.create(parent).grid(0, 0).weightx(1).insets(5, 5, 5, 5).fill(GridBagConstraints.HORIZONTAL).add(new JLabel(I18n.get("tab.realms.account", this.currentAccount.getDisplayString())));
        if (showType && LATEST_JAVA_SNAPSHOT != null) {
            JComboBox<String> type = new JComboBox<>();
            type.addItem(I18n.get("tab.realms.release"));
            type.addItem(I18n.get("tab.realms.snapshot"));
            type.setSelectedIndex(this.currentSelectedJavaVersion.getProtocol().isSnapshot() ? 1 : 0);
            type.addActionListener(e -> {
                VersionEnum selected = type.getSelectedIndex() == 0 ? LATEST_JAVA_RELEASE : LATEST_JAVA_SNAPSHOT;
                if (selected != this.currentSelectedJavaVersion) {
                    this.currentSelectedJavaVersion = selected;
                    this.reinit();
                }
            });
            GBC.create(parent).grid(1, 0).insets(5, 0, 5, 5).anchor(GridBagConstraints.LINE_END).add(type);
        }
    }

    private void addRealms(final JPanel parent, final AbstractRealmsService realmsService, final List<RealmsWorld> worlds) {
        worlds.sort((o1, o2) -> {
            boolean o1Compatible = o1.isCompatible() && !o1.isExpired();
            boolean o2Compatible = o2.isCompatible() && !o2.isExpired();
            if (o1Compatible && !o2Compatible) return -1;
            if (!o1Compatible && o2Compatible) return 1;
            return 0;
        });
        for (RealmsWorld world : worlds) {
            final JPanel panel = new JPanel();
            panel.setLayout(new GridBagLayout());
            panel.setBorder(BorderFactory.createLineBorder(UIManager.getColor("Table.gridColor")));

            String nameString = "";
            if (!world.getOwnerName().isEmpty()) nameString += world.getOwnerName() + " - ";
            String versionString = "";
            if (!world.getActiveVersion().isEmpty()) versionString += " - " + world.getActiveVersion();
            GBC.create(panel).grid(0, 0).weightx(1).insets(5, 5, 0, 5).fill(GridBagConstraints.HORIZONTAL).add(new JLabel(nameString + world.getName() + " (" + world.getState() + ")"));
            GBC.create(panel).grid(1, 0).insets(5, 5, 0, 5).anchor(GridBagConstraints.LINE_END).add(new JLabel(world.getWorldType() + versionString));
            GBC.create(panel).grid(0, 1).insets(5, 5, 0, 5).fill(GridBagConstraints.HORIZONTAL).add(new JLabel(world.getMotd()));
            final JButton join = new JButton(I18n.get("tab.realms.join"));
            if (world.isExpired()) {
                join.setEnabled(false);
                join.setToolTipText(I18n.get("tab.realms.expired"));
            } else if (!world.isCompatible()) {
                join.setEnabled(false);
                join.setToolTipText(I18n.get("tab.realms.incompatible"));
            }
            GBC.create(panel).grid(1, 1).insets(5, 0, 5, 5).anchor(GridBagConstraints.LINE_END).add(join);
            join.addActionListener(event -> {
                join.setEnabled(false);
                join.setText(I18n.get("tab.realms.joining"));
                realmsService.joinWorld(world).thenAccept(address -> SwingUtilities.invokeLater(() -> {
                    join.setEnabled(true);
                    join.setText(I18n.get("tab.realms.join"));
                    this.setServerAddressAndStartViaProxy(address, realmsService instanceof JavaRealmsService ? this.currentSelectedJavaVersion : VersionEnum.bedrockLatest);
                })).exceptionally(e -> {
                    final Throwable cause = e.getCause();
                    SwingUtilities.invokeLater(() -> {
                        join.setEnabled(true);
                        join.setText(I18n.get("tab.realms.join"));
                        if (realmsService instanceof JavaRealmsService javaRealmsService && cause instanceof RealmsResponseException realmsResponseException && realmsResponseException.getRealmsErrorCode() == RealmsResponseException.TOS_NOT_ACCEPTED) {
                            final int chosen = JOptionPane.showConfirmDialog(ViaProxy.getUI(), I18n.get("tab.realms.accept_tos", "https://aka.ms/MinecraftRealmsTerms"), "ViaProxy", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
                            if (chosen == JOptionPane.YES_OPTION) {
                                javaRealmsService.acceptTos();
                                join.doClick();
                            }
                        } else {
                            Logger.LOGGER.error("Failed to join realm", cause);
                            ViaProxy.getUI().showError(I18n.get("tab.realms.error_generic", cause.getMessage()));
                        }
                    });
                    return null;
                });
            });

            parent.add(panel);
        }
    }

    private void setServerAddressAndStartViaProxy(final String address, final VersionEnum version) {
        final GeneralTab generalTab = ViaProxy.getUI().generalTab;
        if (generalTab.stateButton.isEnabled()) {
            if (!generalTab.stateButton.getText().equals(I18n.get("tab.general.state.start"))) {
                generalTab.stateButton.doClick(); // Stop the running proxy
            }
            generalTab.serverAddress.setText(address);
            generalTab.serverVersion.setSelectedItem(version);
            generalTab.authMethod.setSelectedIndex(1);
            generalTab.stateButton.doClick();
            ViaProxy.getUI().contentPane.setSelectedIndex(0);
        }
    }

}
