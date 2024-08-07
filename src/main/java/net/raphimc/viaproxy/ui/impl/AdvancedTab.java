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

import com.viaversion.viaversion.util.DumpUtil;
import gs.mclo.api.MclogsClient;
import gs.mclo.api.response.UploadLogResponse;
import net.lenni0451.commons.swing.GBC;
import net.lenni0451.lambdaevents.EventHandler;
import net.raphimc.viaproxy.ViaProxy;
import net.raphimc.viaproxy.ui.I18n;
import net.raphimc.viaproxy.ui.UITab;
import net.raphimc.viaproxy.ui.ViaProxyWindow;
import net.raphimc.viaproxy.ui.events.UICloseEvent;
import net.raphimc.viaproxy.util.logging.Logger;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.appender.RollingRandomAccessFileAppender;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.io.FileNotFoundException;

import static net.raphimc.viaproxy.ui.ViaProxyWindow.BODY_BLOCK_PADDING;
import static net.raphimc.viaproxy.ui.ViaProxyWindow.BORDER_PADDING;

public class AdvancedTab extends UITab {

    JTextField bindAddress;
    JTextField proxy;
    JCheckBox proxyOnlineMode;
    public JCheckBox legacySkinLoading;
    JCheckBox chatSigning;
    JCheckBox ignorePacketTranslationErrors;
    JCheckBox allowBetaPinging;
    JCheckBox simpleVoiceChatSupport;
    JCheckBox fakeAcceptResourcePacks;
    JButton viaVersionDumpButton;
    JButton uploadLogsButton;

    public AdvancedTab(final ViaProxyWindow frame) {
        super(frame, "advanced");
    }

    @Override
    protected void init(JPanel contentPane) {
        contentPane.setLayout(new BorderLayout());

        this.addBody(contentPane);
        this.addFooter(contentPane);
    }

    private void addBody(final Container parent) {
        JPanel body = new JPanel();
        body.setLayout(new GridBagLayout());

        JPanel checkboxes = new JPanel();
        checkboxes.setLayout(new GridLayout(0, 2, BORDER_PADDING, BORDER_PADDING));

        int gridy = 0;
        {
            JLabel bindPortLabel = new JLabel(I18n.get("tab.advanced.bind_address.label"));
            bindPortLabel.setToolTipText(I18n.get("tab.advanced.bind_address.tooltip"));
            GBC.create(body).grid(0, gridy++).insets(BORDER_PADDING, BORDER_PADDING, 0, 0).anchor(GBC.NORTHWEST).add(bindPortLabel);

            this.bindAddress = new JTextField();
            this.bindAddress.setToolTipText(I18n.get("tab.advanced.bind_address.tooltip"));
            this.bindAddress.setText("0.0.0.0:25568");
            ViaProxy.getSaveManager().uiSave.loadTextField("bind_address", this.bindAddress);
            GBC.create(body).grid(0, gridy++).weightx(1).insets(0, BORDER_PADDING, 0, BORDER_PADDING).fill(GBC.HORIZONTAL).add(this.bindAddress);
        }
        {
            JLabel proxyLabel = new JLabel(I18n.get("tab.advanced.proxy_url.label"));
            proxyLabel.setToolTipText(I18n.get("tab.advanced.proxy_url.tooltip"));
            GBC.create(body).grid(0, gridy++).insets(BODY_BLOCK_PADDING, BORDER_PADDING, 0, 0).anchor(GBC.NORTHWEST).add(proxyLabel);

            this.proxy = new JTextField();
            this.proxy.setToolTipText(I18n.get("tab.advanced.proxy_url.tooltip"));
            ViaProxy.getSaveManager().uiSave.loadTextField("proxy", this.proxy);
            GBC.create(body).grid(0, gridy++).insets(0, BORDER_PADDING, 0, BORDER_PADDING).fill(GBC.HORIZONTAL).add(this.proxy);
        }
        {
            this.proxyOnlineMode = new JCheckBox(I18n.get("tab.advanced.proxy_online_mode.label"));
            this.proxyOnlineMode.setToolTipText(I18n.get("tab.advanced.proxy_online_mode.tooltip"));
            this.proxyOnlineMode.setSelected(ViaProxy.getConfig().isProxyOnlineMode());
            checkboxes.add(this.proxyOnlineMode);
        }
        {
            this.legacySkinLoading = new JCheckBox(I18n.get("tab.advanced.legacy_skin_loading.label"));
            this.legacySkinLoading.setToolTipText(I18n.get("tab.advanced.legacy_skin_loading.tooltip"));
            ViaProxy.getSaveManager().uiSave.loadCheckBox("legacy_skin_loading", this.legacySkinLoading);
            checkboxes.add(this.legacySkinLoading);
        }
        {
            this.chatSigning = new JCheckBox(I18n.get("tab.advanced.chat_signing.label"));
            this.chatSigning.setToolTipText(I18n.get("tab.advanced.chat_signing.tooltip"));
            this.chatSigning.setSelected(ViaProxy.getConfig().shouldSignChat());
            checkboxes.add(this.chatSigning);
        }
        {
            this.ignorePacketTranslationErrors = new JCheckBox(I18n.get("tab.advanced.ignore_packet_translation_errors.label"));
            this.ignorePacketTranslationErrors.setToolTipText(I18n.get("tab.advanced.ignore_packet_translation_errors.tooltip"));
            this.ignorePacketTranslationErrors.setSelected(false);
            this.ignorePacketTranslationErrors.setSelected(ViaProxy.getConfig().shouldIgnoreProtocolTranslationErrors());
            checkboxes.add(this.ignorePacketTranslationErrors);
        }
        {
            this.allowBetaPinging = new JCheckBox(I18n.get("tab.advanced.allow_beta_pinging.label"));
            this.allowBetaPinging.setToolTipText(I18n.get("tab.advanced.allow_beta_pinging.tooltip"));
            this.allowBetaPinging.setSelected(false);
            this.allowBetaPinging.setSelected(ViaProxy.getConfig().shouldAllowBetaPinging());
            checkboxes.add(this.allowBetaPinging);
        }
        {
            this.simpleVoiceChatSupport = new JCheckBox(I18n.get("tab.advanced.simple_voice_chat_support.label"));
            this.simpleVoiceChatSupport.setToolTipText(I18n.get("tab.advanced.simple_voice_chat_support.tooltip"));
            this.simpleVoiceChatSupport.setSelected(false);
            this.simpleVoiceChatSupport.setSelected(ViaProxy.getConfig().shouldSupportSimpleVoiceChat());
            checkboxes.add(this.simpleVoiceChatSupport);
        }
        {
            this.fakeAcceptResourcePacks = new JCheckBox(I18n.get("tab.advanced.fake_accept_resource_packs.label"));
            this.fakeAcceptResourcePacks.setToolTipText(I18n.get("tab.advanced.fake_accept_resource_packs.tooltip"));
            this.fakeAcceptResourcePacks.setSelected(false);
            this.fakeAcceptResourcePacks.setSelected(ViaProxy.getConfig().shouldFakeAcceptResourcePacks());
            checkboxes.add(this.fakeAcceptResourcePacks);
        }
        GBC.create(body).grid(0, gridy++).insets(BODY_BLOCK_PADDING, BORDER_PADDING, 0, BODY_BLOCK_PADDING).fill(GBC.BOTH).weight(1, 1).add(checkboxes);

        parent.add(body, BorderLayout.NORTH);
    }

    private void addFooter(final Container container) {
        JPanel footer = new JPanel();
        footer.setLayout(new GridLayout(1, 2, BORDER_PADDING, 0));

        {
            this.viaVersionDumpButton = new JButton(I18n.get("tab.advanced.create_viaversion_dump.label"));
            this.viaVersionDumpButton.addActionListener(event -> {
                this.viaVersionDumpButton.setEnabled(false);
                DumpUtil.postDump(null).whenComplete((url, e) -> {
                    if (e != null) {
                        Logger.LOGGER.error("Failed to create ViaVersion dump", e);
                        SwingUtilities.invokeLater(() -> ViaProxyWindow.showError(e.getMessage()));
                    } else {
                        ViaProxyWindow.openURL(url);
                        final StringSelection stringSelection = new StringSelection(url);
                        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(stringSelection, stringSelection);
                        SwingUtilities.invokeLater(() -> ViaProxyWindow.showInfo(I18n.get("tab.advanced.create_viaversion_dump.success")));
                    }
                    SwingUtilities.invokeLater(() -> this.viaVersionDumpButton.setEnabled(true));
                });
            });
            footer.add(this.viaVersionDumpButton);
        }
        {
            this.uploadLogsButton = new JButton(I18n.get("tab.advanced.upload_latest_log.label"));
            this.uploadLogsButton.addActionListener(event -> {
                final org.apache.logging.log4j.core.Logger logger = (org.apache.logging.log4j.core.Logger) LogManager.getRootLogger();
                final RollingRandomAccessFileAppender fileAppender = (RollingRandomAccessFileAppender) logger.getAppenders().get("LatestFile");
                fileAppender.getManager().flush();
                final File logFile = new File(fileAppender.getFileName());

                try {
                    this.uploadLogsButton.setEnabled(false);
                    final MclogsClient mclogsClient = new MclogsClient("ViaProxy", ViaProxy.VERSION);
                    final UploadLogResponse apiResponse = mclogsClient.uploadLog(logFile.toPath()).get();
                    if (apiResponse.isSuccess()) {
                        ViaProxyWindow.openURL(apiResponse.getUrl());
                        final StringSelection selection = new StringSelection(apiResponse.getUrl());
                        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, selection);
                        ViaProxyWindow.showInfo("<html>" + I18n.get("tab.advanced.upload_latest_log.success", "<a href=\"\">" + apiResponse.getUrl() + "</a>") + "</html>");
                    } else {
                        ViaProxyWindow.showError(I18n.get("tab.advanced.upload_latest_log.error_generic", apiResponse.getError()));
                    }
                } catch (FileNotFoundException e) {
                    ViaProxyWindow.showError(I18n.get("tab.advanced.upload_latest_log.error_not_found"));
                } catch (Throwable e) {
                    Logger.LOGGER.error("Failed to upload log file", e);
                    ViaProxyWindow.showError(I18n.get("tab.advanced.upload_latest_log.error_generic", e.getMessage()));
                } finally {
                    this.uploadLogsButton.setEnabled(true);
                }
            });
            footer.add(this.uploadLogsButton);
        }

        JPanel padding = new JPanel();
        padding.setLayout(new GridBagLayout());
        GBC.create(padding).grid(0, 0).weightx(1).insets(0, BORDER_PADDING, BORDER_PADDING, BORDER_PADDING).fill(GBC.HORIZONTAL).add(footer);

        container.add(padding, BorderLayout.SOUTH);
    }

    @EventHandler(events = UICloseEvent.class)
    void applyGuiState() {
        ViaProxy.getSaveManager().uiSave.put("bind_address", this.bindAddress.getText());
        ViaProxy.getSaveManager().uiSave.put("proxy", this.proxy.getText());
        ViaProxy.getConfig().setProxyOnlineMode(this.proxyOnlineMode.isSelected());
        ViaProxy.getSaveManager().uiSave.put("legacy_skin_loading", String.valueOf(this.legacySkinLoading.isSelected()));
        ViaProxy.getConfig().setChatSigning(this.chatSigning.isSelected());
        ViaProxy.getConfig().setIgnoreProtocolTranslationErrors(this.ignorePacketTranslationErrors.isSelected());
        ViaProxy.getConfig().setAllowBetaPinging(this.allowBetaPinging.isSelected());
        ViaProxy.getConfig().setSimpleVoiceChatSupport(this.simpleVoiceChatSupport.isSelected());
        ViaProxy.getConfig().setFakeAcceptResourcePacks(this.fakeAcceptResourcePacks.isSelected());
    }

}
