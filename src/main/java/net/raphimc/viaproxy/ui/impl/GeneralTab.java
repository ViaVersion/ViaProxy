package net.raphimc.viaproxy.ui.impl;

import com.google.common.net.HostAndPort;
import net.raphimc.viaprotocolhack.util.VersionEnum;
import net.raphimc.viaproxy.ViaProxy;
import net.raphimc.viaproxy.cli.options.Options;
import net.raphimc.viaproxy.ui.AUITab;
import net.raphimc.viaproxy.ui.ViaProxyUI;
import net.raphimc.viaproxy.util.logging.Logger;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class GeneralTab extends AUITab {

    private JTextField serverAddress;
    private JComboBox<VersionEnum> serverVersion;
    private JSpinner bindPort;
    private JComboBox<String> authMethod;
    private JCheckBox betaCraftAuth;
    private JCheckBox proxyOnlineMode;
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
            JLabel addressLabel = new JLabel("Server Address:");
            addressLabel.setBounds(10, 50, 100, 20);
            contentPane.add(addressLabel);

            this.serverAddress = new JTextField();
            this.serverAddress.setBounds(10, 70, 465, 20);
            contentPane.add(this.serverAddress);
        }
        {
            JLabel serverVersionLabel = new JLabel("Server Version:");
            serverVersionLabel.setBounds(10, 100, 100, 20);
            contentPane.add(serverVersionLabel);

            this.serverVersion = new JComboBox<>(VersionEnum.SORTED_VERSIONS.toArray(new VersionEnum[0]));
            this.serverVersion.setBounds(10, 120, 465, 20);
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
            contentPane.add(this.serverVersion);
        }
        {
            JLabel bindPortLabel = new JLabel("Local Port:");
            bindPortLabel.setBounds(10, 150, 100, 20);
            contentPane.add(bindPortLabel);

            this.bindPort = new JSpinner(new SpinnerNumberModel(25568, 1, 65535, 1));
            this.bindPort.setBounds(10, 170, 465, 20);
            contentPane.add(this.bindPort);
        }
        {
            JLabel authMethodLabel = new JLabel("Minecraft Account:");
            authMethodLabel.setBounds(10, 200, 400, 20);
            contentPane.add(authMethodLabel);

            this.authMethod = new JComboBox<>(new String[]{"Use no account", "Use selected account", "Use OpenAuthMod"});
            this.authMethod.setBounds(10, 220, 465, 20);
            contentPane.add(this.authMethod);
        }
        {
            this.betaCraftAuth = new JCheckBox("BetaCraft Auth (Classic)");
            this.betaCraftAuth.setBounds(10, 250, 150, 20);
            contentPane.add(this.betaCraftAuth);
        }
        {
            this.proxyOnlineMode = new JCheckBox("Proxy Online Mode");
            this.proxyOnlineMode.setBounds(350, 250, 465, 20);
            this.proxyOnlineMode.setToolTipText("Enabling Proxy Online Mode requires your client to have a valid account.\nProxy Online Mode allows your client to see skins on online mode servers and use the signed chat features.");
            contentPane.add(this.proxyOnlineMode);
        }
        {
            this.stateLabel = new JLabel();
            this.stateLabel.setBounds(14, 280, 465, 20);
            this.stateLabel.setVisible(false);
            contentPane.add(this.stateLabel);
        }
        {
            this.stateButton = new JButton("Loading ViaProxy...");
            this.stateButton.setBounds(10, 300, 465, 20);
            this.stateButton.addActionListener(e -> {
                if (this.stateButton.getText().equalsIgnoreCase("Start")) this.start();
                else if (this.stateButton.getText().equalsIgnoreCase("Stop")) this.stop();
            });
            this.stateButton.setEnabled(false);
            contentPane.add(this.stateButton);
        }
    }

    private void setComponentsEnabled(final boolean state) {
        this.serverAddress.setEnabled(state);
        this.serverVersion.setEnabled(state);
        this.bindPort.setEnabled(state);
        this.authMethod.setEnabled(state);
        this.betaCraftAuth.setEnabled(state);
        this.proxyOnlineMode.setEnabled(state);
    }

    private void updateStateLabel() {
        this.stateLabel.setText("ViaProxy is running! Connect with Minecraft 1.7+ to 127.0.0.1:" + this.bindPort.getValue());
        this.stateLabel.setVisible(true);
    }

    private void start() {
        this.setComponentsEnabled(false);
        this.stateButton.setEnabled(false);
        this.stateButton.setText("Starting...");

        new Thread(() -> {
            final String serverAddress = this.serverAddress.getText().trim();
            final VersionEnum serverVersion = (VersionEnum) this.serverVersion.getSelectedItem();
            final int bindPort = (int) this.bindPort.getValue();
            final int authMethod = this.authMethod.getSelectedIndex();
            final boolean betaCraftAuth = this.betaCraftAuth.isSelected();
            final boolean proxyOnlineMode = this.proxyOnlineMode.isSelected();

            try {
                try {
                    final HostAndPort hostAndPort = HostAndPort.fromString(serverAddress);

                    Options.BIND_PORT = bindPort;
                    Options.ONLINE_MODE = proxyOnlineMode;
                    Options.CONNECT_ADDRESS = hostAndPort.getHost();
                    Options.CONNECT_PORT = hostAndPort.getPortOrDefault(25565);
                    Options.PROTOCOL_VERSION = serverVersion;
                    Options.BETACRAFT_AUTH = betaCraftAuth;

                    if (authMethod != 1) {
                        Options.MC_ACCOUNT = null;
                    } else if (Options.MC_ACCOUNT == null) {
                        this.frame.accountsTab.markSelected(0);
                    }

                    if (authMethod == 2) {
                        Options.OPENAUTHMOD_AUTH = true;
                    }
                } catch (Throwable e) {
                    SwingUtilities.invokeLater(() -> {
                        this.frame.showError("Invalid server address!");
                    });
                    throw e;
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

    @Override
    public void setReady() {
        SwingUtilities.invokeLater(() -> {
            this.stateButton.setText("Start");
            this.stateButton.setEnabled(true);
        });
    }

}
