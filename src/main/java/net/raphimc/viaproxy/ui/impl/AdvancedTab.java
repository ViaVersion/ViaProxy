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
import net.raphimc.viaproxy.ui.ViaProxyUI;
import net.raphimc.viaproxy.ui.events.UICloseEvent;
import net.raphimc.viaproxy.ui.events.UIInitEvent;
import net.raphimc.viaproxy.util.GBC;
import net.raphimc.viaproxy.util.logging.Logger;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.appender.RollingRandomAccessFileAppender;
import org.jdesktop.swingx.VerticalLayout;

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
    JButton viaVersionDumpButton;
    JButton uploadLogsButton;

    public AdvancedTab(final ViaProxyUI frame) {
        super(frame, "Advanced");
    }

    @Override
    protected void init(JPanel contentPane) {
        JPanel top = new JPanel();
        top.setLayout(new VerticalLayout());

        JPanel bottom = new JPanel();
        bottom.setLayout(new VerticalLayout());

        this.addBody(top);
        this.addFooter(bottom);

        contentPane.setLayout(new BorderLayout());
        contentPane.add(top, BorderLayout.NORTH);
        contentPane.add(bottom, BorderLayout.SOUTH);
    }

    private void addBody(final Container parent) {
        JPanel body = new JPanel();
        body.setLayout(new GridBagLayout());

        int gridy = 0;
        {
            String toolTipText = "The port the proxy should bind to.";

            JLabel bindPortLabel = new JLabel("Bind Port:");
            bindPortLabel.setToolTipText(toolTipText);
            GBC.create(body).grid(0, gridy++).insets(BORDER_PADDING, BORDER_PADDING, 0, 0).anchor(GridBagConstraints.NORTHWEST).add(bindPortLabel);

            this.bindPort = new JSpinner(new SpinnerNumberModel(25568, 1, 65535, 1));
            this.bindPort.setToolTipText(toolTipText);
            ViaProxy.saveManager.uiSave.loadSpinner("bind_port", this.bindPort);
            GBC.create(body).grid(0, gridy++).weightx(1).insets(0, BORDER_PADDING, 0, BORDER_PADDING).fill(GridBagConstraints.HORIZONTAL).add(this.bindPort);
        }
        {
            String toolTipText = """
                    URL of a SOCKS(4/5)/HTTP(S) proxy which will be used for TCP connections.
                    Supported formats:
                    - type://address:port
                    - type://username:password@address:port""";

            JLabel proxyLabel = new JLabel("Proxy URL:");
            proxyLabel.setToolTipText(toolTipText);
            GBC.create(body).grid(0, gridy++).insets(BODY_BLOCK_PADDING, BORDER_PADDING, 0, 0).anchor(GridBagConstraints.NORTHWEST).add(proxyLabel);

            this.proxy = new JTextField();
            this.proxy.setToolTipText(toolTipText);
            ViaProxy.saveManager.uiSave.loadTextField("proxy", this.proxy);
            GBC.create(body).grid(0, gridy++).insets(0, BORDER_PADDING, 0, BORDER_PADDING).fill(GridBagConstraints.HORIZONTAL).add(this.proxy);
        }
        {
            this.proxyOnlineMode = new JCheckBox("Proxy Online Mode");
            this.proxyOnlineMode.setToolTipText("""
                    Enabling Proxy Online Mode requires your client to have a valid account.
                    Proxy Online Mode allows your client to see skins on online mode servers and use the signed chat features.""");
            ViaProxy.saveManager.uiSave.loadCheckBox("proxy_online_mode", this.proxyOnlineMode);
            GBC.create(body).grid(0, gridy++).weightx(1).insets(BODY_BLOCK_PADDING, BORDER_PADDING, 0, 0).anchor(GridBagConstraints.NORTHWEST).add(this.proxyOnlineMode);
        }
        {
            this.legacySkinLoading = new JCheckBox("Legacy Skin Loading");
            this.legacySkinLoading.setToolTipText("Enabling Legacy Skin Loading allows you to see skins on <= 1.6.4 servers.");
            ViaProxy.saveManager.uiSave.loadCheckBox("legacy_skin_loading", this.legacySkinLoading);
            GBC.create(body).grid(0, gridy++).weightx(1).insets(BODY_BLOCK_PADDING, BORDER_PADDING, 0, 0).fill(GridBagConstraints.HORIZONTAL).add(this.legacySkinLoading);
        }
        {
            this.chatSigning = new JCheckBox("Chat signing");
            this.chatSigning.setToolTipText("Enables sending signed chat messages on >= 1.19 servers.");
            this.chatSigning.setSelected(true);
            ViaProxy.saveManager.uiSave.loadCheckBox("chat_signing", this.chatSigning);
            GBC.create(body).grid(0, gridy++).weightx(1).insets(BODY_BLOCK_PADDING, BORDER_PADDING, 0, 0).anchor(GridBagConstraints.NORTHWEST).add(this.chatSigning);
        }
        {
            this.ignorePacketTranslationErrors = new JCheckBox("Ignore packet translation errors");
            this.ignorePacketTranslationErrors.setToolTipText("""
                    Enabling this will prevent getting disconnected from the server when a packet translation error occurs and instead only print the error in the console.
                    This may cause issues depending on the type of packet which failed to translate.""");
            this.ignorePacketTranslationErrors.setSelected(false);
            ViaProxy.saveManager.uiSave.loadCheckBox("ignore_packet_translation_errors", this.ignorePacketTranslationErrors);
            GBC.create(body).grid(0, gridy++).weightx(1).insets(BODY_BLOCK_PADDING, BORDER_PADDING, 0, 0).fill(GridBagConstraints.HORIZONTAL).add(this.ignorePacketTranslationErrors);
        }

        parent.add(body, BorderLayout.CENTER);
    }

    private void addFooter(final Container container) {
        JPanel footer = new JPanel();
        footer.setLayout(new GridLayout(1, 2, BORDER_PADDING, 0));

        {
            this.viaVersionDumpButton = new JButton("Create ViaVersion dump");
            this.viaVersionDumpButton.addActionListener(event -> {
                this.viaVersionDumpButton.setEnabled(false);
                DumpUtil.postDump(null).whenComplete((url, e) -> {
                    if (e != null) {
                        Logger.LOGGER.error("Failed to create ViaVersion dump", e);
                        SwingUtilities.invokeLater(() -> ViaProxy.ui.showError(e.getMessage()));
                    } else {
                        ViaProxy.ui.openURL(url);
                        final StringSelection stringSelection = new StringSelection(url);
                        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(stringSelection, stringSelection);
                        SwingUtilities.invokeLater(() -> ViaProxy.ui.showInfo("Copied ViaVersion dump link to clipboard."));
                    }
                    SwingUtilities.invokeLater(() -> this.viaVersionDumpButton.setEnabled(true));
                });
            });
            this.viaVersionDumpButton.setEnabled(false);
            footer.add(this.viaVersionDumpButton);
        }
        {
            this.uploadLogsButton = new JButton("Upload latest.log");
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
                        ViaProxy.ui.openURL(apiResponse.getUrl());
                        final StringSelection selection = new StringSelection(apiResponse.getUrl());
                        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, selection);
                        ViaProxy.ui.showInfo("<html>Uploaded log file to <a href=\"\">" + apiResponse.getUrl() + "</a> (copied to clipboard)</html>");
                    } else {
                        ViaProxy.ui.showError("The log file could not be uploaded: " + apiResponse.getError());
                    }
                } catch (FileNotFoundException e) {
                    ViaProxy.ui.showError("The log file could not be found.");
                } catch (Throwable e) {
                    Logger.LOGGER.error("Failed to upload log file", e);
                    ViaProxy.ui.showError("The log file could not be uploaded.");
                } finally {
                    this.uploadLogsButton.setEnabled(true);
                }
            });
            footer.add(this.uploadLogsButton);
        }

        JPanel padding = new JPanel();
        padding.setLayout(new GridBagLayout());
        GBC.create(padding).grid(0, 0).weightx(1).insets(0, BORDER_PADDING, BORDER_PADDING, BORDER_PADDING).fill(GridBagConstraints.HORIZONTAL).add(footer);

        container.add(padding, BorderLayout.CENTER);
    }

    @EventHandler
    private void onInit(final UIInitEvent event) {
        SwingUtilities.invokeLater(() -> {
            this.viaVersionDumpButton.setEnabled(true);
        });
    }

    @EventHandler
    private void onClose(final UICloseEvent event) {
        UISave save = ViaProxy.saveManager.uiSave;
        save.put("bind_port", String.valueOf(this.bindPort.getValue()));
        save.put("proxy", this.proxy.getText());
        save.put("proxy_online_mode", String.valueOf(this.proxyOnlineMode.isSelected()));
        save.put("legacy_skin_loading", String.valueOf(this.legacySkinLoading.isSelected()));
        save.put("chat_signing", String.valueOf(this.chatSigning.isSelected()));
        save.put("ignore_packet_translation_errors", String.valueOf(this.ignorePacketTranslationErrors.isSelected()));
        ViaProxy.saveManager.save();
    }

}
