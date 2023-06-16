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
import net.raphimc.viaproxy.ViaProxy;
import net.raphimc.viaproxy.saves.impl.UISave;
import net.raphimc.viaproxy.ui.AUITab;
import net.raphimc.viaproxy.ui.ViaProxyUI;
import net.raphimc.viaproxy.util.logging.Logger;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.appender.RollingRandomAccessFileAppender;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.io.FileNotFoundException;

public class AdvancedTab extends AUITab {

    JSpinner bindPort;
    JTextField proxy;
    JCheckBox proxyOnlineMode;
    JCheckBox legacySkinLoading;
    JCheckBox chatSigning;
    JButton viaVersionDumpButton;
    JButton uploadLogsButton;

    public AdvancedTab(final ViaProxyUI frame) {
        super(frame, "Advanced");
    }

    @Override
    protected void init(JPanel contentPane) {
        {
            String toolTipText = "The port the proxy should bind to.";

            JLabel bindPortLabel = new JLabel("Bind Port:");
            bindPortLabel.setBounds(10, 10, 100, 20);
            bindPortLabel.setToolTipText(toolTipText);
            contentPane.add(bindPortLabel);

            this.bindPort = new JSpinner(new SpinnerNumberModel(25568, 1, 65535, 1));
            this.bindPort.setBounds(10, 30, 465, 22);
            this.bindPort.setToolTipText(toolTipText);
            ViaProxy.saveManager.uiSave.loadSpinner("bind_port", this.bindPort);
            contentPane.add(this.bindPort);
        }
        {
            String toolTipText = "URL of a SOCKS(4/5)/HTTP(S) proxy which will be used for TCP connections.\n" +
                    "Supported formats:\n" +
                    "- type://address:port\n" +
                    "- type://username:password@address:port";

            JLabel proxyLabel = new JLabel("Proxy URL:");
            proxyLabel.setBounds(10, 60, 100, 20);
            proxyLabel.setToolTipText(toolTipText);
            contentPane.add(proxyLabel);

            this.proxy = new JTextField();
            this.proxy.setBounds(10, 80, 465, 22);
            this.proxy.setToolTipText(toolTipText);
            ViaProxy.saveManager.uiSave.loadTextField("proxy", this.proxy);
            contentPane.add(this.proxy);
        }
        {
            this.proxyOnlineMode = new JCheckBox("Proxy Online Mode");
            this.proxyOnlineMode.setBounds(10, 110, 465, 20);
            this.proxyOnlineMode.setToolTipText("Enabling Proxy Online Mode requires your client to have a valid account.\n" +
                    "Proxy Online Mode allows your client to see skins on online mode servers and use the signed chat features.");
            ViaProxy.saveManager.uiSave.loadCheckBox("proxy_online_mode", this.proxyOnlineMode);
            contentPane.add(this.proxyOnlineMode);
        }
        {
            this.legacySkinLoading = new JCheckBox("Legacy Skin Loading");
            this.legacySkinLoading.setBounds(10, 140, 465, 20);
            this.legacySkinLoading.setToolTipText("Enabling Legacy Skin Loading allows you to see skins on <= 1.6.4 servers");
            ViaProxy.saveManager.uiSave.loadCheckBox("legacy_skin_loading", this.legacySkinLoading);
            contentPane.add(this.legacySkinLoading);
        }
        {
            this.chatSigning = new JCheckBox("Chat signing");
            this.chatSigning.setBounds(10, 170, 465, 20);
            this.chatSigning.setToolTipText("Enables sending signed chat messages on >= 1.19 servers");
            this.chatSigning.setSelected(true);
            ViaProxy.saveManager.uiSave.loadCheckBox("chat_signing", this.chatSigning);
            contentPane.add(this.chatSigning);
        }
        {
            this.viaVersionDumpButton = new JButton("Create ViaVersion dump");
            this.viaVersionDumpButton.setBounds(10, 250, 225, 20);
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
            contentPane.add(this.viaVersionDumpButton);
        }
        {
            this.uploadLogsButton = new JButton("Upload latest.log");
            this.uploadLogsButton.setBounds(249, 250, 225, 20);
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
            contentPane.add(this.uploadLogsButton);
        }
    }

    @Override
    public void setReady() {
        SwingUtilities.invokeLater(() -> {
            this.viaVersionDumpButton.setEnabled(true);
        });
    }

    @Override
    public void onClose() {
        UISave save = ViaProxy.saveManager.uiSave;
        save.put("bind_port", String.valueOf(this.bindPort.getValue()));
        save.put("proxy", this.proxy.getText());
        save.put("proxy_online_mode", String.valueOf(this.proxyOnlineMode.isSelected()));
        save.put("legacy_skin_loading", String.valueOf(this.legacySkinLoading.isSelected()));
        save.put("chat_signing", String.valueOf(this.chatSigning.isSelected()));
        ViaProxy.saveManager.save();
    }

}
