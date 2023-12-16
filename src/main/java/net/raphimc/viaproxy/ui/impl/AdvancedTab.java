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

import com.viaversion.viaversion.util.DumpUtil;
import gs.mclo.api.MclogsClient;
import gs.mclo.api.response.UploadLogResponse;
import net.lenni0451.lambdaevents.EventHandler;
import net.raphimc.viaproxy.ViaProxy;
import net.raphimc.viaproxy.saves.impl.UISave;
import net.raphimc.viaproxy.ui.AUITab;
import net.raphimc.viaproxy.ui.I18n;
import net.raphimc.viaproxy.ui.ViaProxyUI;
import net.raphimc.viaproxy.ui.events.UICloseEvent;
import net.raphimc.viaproxy.ui.events.UIInitEvent;
import net.raphimc.viaproxy.util.GBC;
import net.raphimc.viaproxy.util.logging.Logger;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.appender.RollingRandomAccessFileAppender;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.io.FileNotFoundException;

import static net.raphimc.viaproxy.ui.ViaProxyUI.BODY_BLOCK_PADDING;
import static net.raphimc.viaproxy.ui.ViaProxyUI.BORDER_PADDING;

public class AdvancedTab extends AUITab {

    JSpinner bindPort;
    JTextField proxy;
    JCheckBox proxyOnlineMode;
    JCheckBox legacySkinLoading;
    JCheckBox chatSigning;
    JCheckBox ignorePacketTranslationErrors;
    JCheckBox verifyUsernames;
    JButton viaVersionDumpButton;
    JButton uploadLogsButton;

    public AdvancedTab(final ViaProxyUI frame) {
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

        int gridy = 0;
        {
            JLabel bindPortLabel = new JLabel(I18n.get("tab.advanced.bind_port.label"));
            bindPortLabel.setToolTipText(I18n.get("tab.advanced.bind_port.tooltip"));
            GBC.create(body).grid(0, gridy++).insets(BORDER_PADDING, BORDER_PADDING, 0, 0).anchor(GridBagConstraints.NORTHWEST).add(bindPortLabel);

            this.bindPort = new JSpinner(new SpinnerNumberModel(25568, 1, 65535, 1));
            this.bindPort.setToolTipText(I18n.get("tab.advanced.bind_port.tooltip"));
            ViaProxy.getSaveManager().uiSave.loadSpinner("bind_port", this.bindPort);
            GBC.create(body).grid(0, gridy++).weightx(1).insets(0, BORDER_PADDING, 0, BORDER_PADDING).fill(GridBagConstraints.HORIZONTAL).add(this.bindPort);
        }
        {
            JLabel proxyLabel = new JLabel(I18n.get("tab.advanced.proxy_url.label"));
            proxyLabel.setToolTipText(I18n.get("tab.advanced.proxy_url.tooltip"));
            GBC.create(body).grid(0, gridy++).insets(BODY_BLOCK_PADDING, BORDER_PADDING, 0, 0).anchor(GridBagConstraints.NORTHWEST).add(proxyLabel);

            this.proxy = new JTextField();
            this.proxy.setToolTipText(I18n.get("tab.advanced.proxy_url.tooltip"));
            ViaProxy.getSaveManager().uiSave.loadTextField("proxy", this.proxy);
            GBC.create(body).grid(0, gridy++).insets(0, BORDER_PADDING, 0, BORDER_PADDING).fill(GridBagConstraints.HORIZONTAL).add(this.proxy);
        }
        {
            this.proxyOnlineMode = new JCheckBox(I18n.get("tab.advanced.proxy_online_mode.label"));
            this.proxyOnlineMode.setToolTipText(I18n.get("tab.advanced.proxy_online_mode.tooltip"));
            ViaProxy.getSaveManager().uiSave.loadCheckBox("proxy_online_mode", this.proxyOnlineMode);
            GBC.create(body).grid(0, gridy++).insets(BODY_BLOCK_PADDING, BORDER_PADDING, 0, 0).anchor(GridBagConstraints.NORTHWEST).add(this.proxyOnlineMode);
        }
        {
            this.legacySkinLoading = new JCheckBox(I18n.get("tab.advanced.legacy_skin_loading.label"));
            this.legacySkinLoading.setToolTipText(I18n.get("tab.advanced.legacy_skin_loading.tooltip"));
            ViaProxy.getSaveManager().uiSave.loadCheckBox("legacy_skin_loading", this.legacySkinLoading);
            GBC.create(body).grid(0, gridy++).insets(BODY_BLOCK_PADDING, BORDER_PADDING, 0, 0).anchor(GridBagConstraints.NORTHWEST).add(this.legacySkinLoading);
        }
        {
            this.chatSigning = new JCheckBox(I18n.get("tab.advanced.chat_signing.label"));
            this.chatSigning.setToolTipText(I18n.get("tab.advanced.chat_signing.tooltip"));
            this.chatSigning.setSelected(true);
            ViaProxy.getSaveManager().uiSave.loadCheckBox("chat_signing", this.chatSigning);
            GBC.create(body).grid(0, gridy++).insets(BODY_BLOCK_PADDING, BORDER_PADDING, 0, 0).anchor(GridBagConstraints.NORTHWEST).add(this.chatSigning);
        }
        {
            this.ignorePacketTranslationErrors = new JCheckBox(I18n.get("tab.advanced.ignore_packet_translation_errors.label"));
            this.ignorePacketTranslationErrors.setToolTipText(I18n.get("tab.advanced.ignore_packet_translation_errors.tooltip"));
            this.ignorePacketTranslationErrors.setSelected(false);
            ViaProxy.getSaveManager().uiSave.loadCheckBox("ignore_packet_translation_errors", this.ignorePacketTranslationErrors);
            GBC.create(body).grid(0, gridy++).insets(BODY_BLOCK_PADDING, BORDER_PADDING, 0, 0).anchor(GridBagConstraints.NORTHWEST).add(this.ignorePacketTranslationErrors);
        }
        {
            this.verifyUsernames = new JCheckBox(I18n.get("tab.advanced.verify_usernames.label"));
            this.verifyUsernames.setToolTipText(I18n.get("tab.advanced.verify_usernames.tooltip"));
            this.verifyUsernames.setSelected(false);
            ViaProxy.getSaveManager().uiSave.loadCheckBox("verify_usernames", this.verifyUsernames);
            GBC.create(body).grid(0, gridy++).insets(BODY_BLOCK_PADDING, BORDER_PADDING, 0, 0).anchor(GridBagConstraints.NORTHWEST).add(this.verifyUsernames);
        }

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
                        SwingUtilities.invokeLater(() -> ViaProxy.getUI().showError(e.getMessage()));
                    } else {
                        ViaProxy.getUI().openURL(url);
                        final StringSelection stringSelection = new StringSelection(url);
                        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(stringSelection, stringSelection);
                        SwingUtilities.invokeLater(() -> ViaProxy.getUI().showInfo(I18n.get("tab.advanced.create_viaversion_dump.success")));
                    }
                    SwingUtilities.invokeLater(() -> this.viaVersionDumpButton.setEnabled(true));
                });
            });
            this.viaVersionDumpButton.setEnabled(false);
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
                    final UploadLogResponse apiResponse = mclogsClient.uploadLog(logFile.toPath());
                    if (apiResponse.isSuccess()) {
                        ViaProxy.getUI().openURL(apiResponse.getUrl());
                        final StringSelection selection = new StringSelection(apiResponse.getUrl());
                        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, selection);
                        ViaProxy.getUI().showInfo("<html>" + I18n.get("tab.advanced.upload_latest_log.success", "<a href=\"\">" + apiResponse.getUrl() + "</a>") + "</html>");
                    } else {
                        ViaProxy.getUI().showError(I18n.get("tab.advanced.upload_latest_log.error_generic", apiResponse.getError()));
                    }
                } catch (FileNotFoundException e) {
                    ViaProxy.getUI().showError(I18n.get("tab.advanced.upload_latest_log.error_not_found"));
                } catch (Throwable e) {
                    Logger.LOGGER.error("Failed to upload log file", e);
                    ViaProxy.getUI().showError(I18n.get("tab.advanced.upload_latest_log.error_generic", e.getMessage()));
                } finally {
                    this.uploadLogsButton.setEnabled(true);
                }
            });
            footer.add(this.uploadLogsButton);
        }

        JPanel padding = new JPanel();
        padding.setLayout(new GridBagLayout());
        GBC.create(padding).grid(0, 0).weightx(1).insets(0, BORDER_PADDING, BORDER_PADDING, BORDER_PADDING).fill(GridBagConstraints.HORIZONTAL).add(footer);

        container.add(padding, BorderLayout.SOUTH);
    }

    @EventHandler
    private void onInit(final UIInitEvent event) {
        SwingUtilities.invokeLater(() -> {
            this.viaVersionDumpButton.setEnabled(true);
        });
    }

    @EventHandler
    private void onClose(final UICloseEvent event) {
        UISave save = ViaProxy.getSaveManager().uiSave;
        save.put("bind_port", String.valueOf(this.bindPort.getValue()));
        save.put("proxy", this.proxy.getText());
        save.put("proxy_online_mode", String.valueOf(this.proxyOnlineMode.isSelected()));
        save.put("legacy_skin_loading", String.valueOf(this.legacySkinLoading.isSelected()));
        save.put("chat_signing", String.valueOf(this.chatSigning.isSelected()));
        save.put("ignore_packet_translation_errors", String.valueOf(this.ignorePacketTranslationErrors.isSelected()));
        save.put("verify_usernames", String.valueOf(this.verifyUsernames.isSelected()));
        ViaProxy.getSaveManager().save();
    }

}
