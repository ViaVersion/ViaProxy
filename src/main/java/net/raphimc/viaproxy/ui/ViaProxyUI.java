package net.raphimc.viaproxy.ui;

import com.formdev.flatlaf.FlatDarkLaf;
import com.google.common.net.HostAndPort;
import net.raphimc.vialegacy.util.VersionEnum;
import net.raphimc.viaproxy.ViaProxy;
import net.raphimc.viaproxy.cli.options.Options;
import net.raphimc.viaproxy.util.logging.Logger;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URI;

public class ViaProxyUI extends JFrame {

    private final JPanel contentPane = new JPanel();

    private ImageIcon icon;

    private JTextField serverAddress;
    private JComboBox<VersionEnum> serverVersion;
    private JSpinner bindPort;
    private JComboBox<String> authMethod;
    private JCheckBox betaCraftAuth;
    private JLabel stateLabel;
    private JButton stateButton;

    public ViaProxyUI() {
        this.applyDarkFlatLafTheme();
        this.loadIcons();
        this.initWindow();
        this.initElements();

        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
            Logger.LOGGER.error("Caught exception in thread " + t.getName(), e);
            final StringBuilder builder = new StringBuilder("An error occurred:\n");
            builder.append("[").append(e.getClass().getSimpleName()).append("] ").append(e.getMessage()).append("\n");
            for (StackTraceElement element : e.getStackTrace()) {
                builder.append("\tat ").append(element.toString()).append("\n");
            }
            this.showError(builder.toString());
        });

        SwingUtilities.updateComponentTreeUI(this);
        this.setVisible(true);
    }

    private void applyDarkFlatLafTheme() {
        try {
            UIManager.setLookAndFeel(new FlatDarkLaf());
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private void loadIcons() {
        this.icon = new ImageIcon(this.getClass().getClassLoader().getResource("assets/icons/icon.png"));
    }

    private void initWindow() {
        this.setTitle("ViaProxy");
        this.setIconImage(this.icon.getImage());
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setSize(500, 370);
        this.setResizable(false);
        this.setLocationRelativeTo(null);
        this.setContentPane(this.contentPane);
    }

    private void initElements() {
        this.contentPane.setLayout(null);
        {
            JLabel titleLabel = new JLabel("ViaProxy");
            titleLabel.setBounds(0, 0, 500, 50);
            titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
            titleLabel.setFont(titleLabel.getFont().deriveFont(30F));
            this.contentPane.add(titleLabel);
        }
        {
            JLabel copyrightLabel = new JLabel("© RK_01 & Lenni0451");
            copyrightLabel.setBounds(360, 10, 500, 20);
            this.contentPane.add(copyrightLabel);
        }
        {
            JLabel discordLabel = new JLabel("Discord");
            discordLabel.setBounds(10, 10, 500, 20);
            discordLabel.setForeground(new Color(124, 171, 241));
            discordLabel.addMouseListener(new MouseAdapter() {
                private static final String LINK = "https://viaproxy.raphimc.net";

                @Override
                public void mouseReleased(MouseEvent e) {
                    try {
                        Desktop.getDesktop().browse(new URI(LINK));
                    } catch (Throwable t) {
                        showInfo("Couldn't open the link :(\nHere it is for you: " + LINK);
                    }
                }
            });
            this.contentPane.add(discordLabel);
        }
        {
            JLabel addressLabel = new JLabel("Server Address:");
            addressLabel.setBounds(10, 50, 100, 20);
            this.contentPane.add(addressLabel);

            this.serverAddress = new JTextField();
            this.serverAddress.setBounds(10, 70, 465, 20);
            this.contentPane.add(this.serverAddress);
        }
        {
            JLabel serverVersionLabel = new JLabel("Server Version:");
            serverVersionLabel.setBounds(10, 100, 100, 20);
            this.contentPane.add(serverVersionLabel);

            this.serverVersion = new JComboBox<>(VersionEnum.RENDER_VERSIONS.toArray(new VersionEnum[0]));
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
            this.contentPane.add(this.serverVersion);
        }
        {
            JLabel bindPortLabel = new JLabel("Bind Port:");
            bindPortLabel.setBounds(10, 150, 100, 20);
            this.contentPane.add(bindPortLabel);

            this.bindPort = new JSpinner(new SpinnerNumberModel(25568, 1, 65535, 1));
            this.bindPort.setBounds(10, 170, 465, 20);
            this.bindPort.setEditor(new JSpinner.NumberEditor(this.bindPort, "#"));
            ((JSpinner.DefaultEditor) this.bindPort.getEditor()).getTextField().setHorizontalAlignment(SwingConstants.LEFT);
            this.contentPane.add(this.bindPort);
        }
        {
            JLabel authMethodLabel = new JLabel("Auth Method:");
            authMethodLabel.setBounds(10, 200, 100, 20);
            this.contentPane.add(authMethodLabel);

            this.authMethod = new JComboBox<>(new String[]{"OpenAuthMod"});
            this.authMethod.setBounds(10, 220, 465, 20);
            this.contentPane.add(this.authMethod);
        }
        {
            this.betaCraftAuth = new JCheckBox("BetaCraft Auth (Classic)");
            this.betaCraftAuth.setBounds(10, 250, 465, 20);
            this.contentPane.add(this.betaCraftAuth);
        }
        {
            this.stateLabel = new JLabel();
            this.stateLabel.setBounds(14, 280, 465, 20);
            this.stateLabel.setVisible(false);
            this.contentPane.add(this.stateLabel);
        }
        {
            this.stateButton = new JButton("Loading ViaProxy...");
            this.stateButton.setBounds(10, 300, 465, 20);
            this.stateButton.addActionListener(e -> {
                if (this.stateButton.getText().equalsIgnoreCase("Start")) this.start();
                else if (this.stateButton.getText().equalsIgnoreCase("Stop")) this.stop();
            });
            this.stateButton.setEnabled(false);
            this.contentPane.add(this.stateButton);
        }
    }

    private void setComponentsEnabled(final boolean state) {
        this.serverAddress.setEnabled(state);
        this.serverVersion.setEnabled(state);
        this.bindPort.setEnabled(state);
        this.authMethod.setEnabled(state);
        this.betaCraftAuth.setEnabled(state);
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
            final String serverAddress = this.serverAddress.getText();
            final VersionEnum serverVersion = (VersionEnum) this.serverVersion.getSelectedItem();
            final int bindPort = (int) this.bindPort.getValue();
            final String authMethod = (String) this.authMethod.getSelectedItem();
            final boolean betaCraftAuth = this.betaCraftAuth.isSelected();

            try {
                final HostAndPort hostAndPort = HostAndPort.fromString(serverAddress);

                Options.BIND_ADDRESS = "127.0.0.1";
                Options.BIND_PORT = bindPort;
                Options.CONNECT_ADDRESS = hostAndPort.getHost();
                Options.CONNECT_PORT = hostAndPort.getPortOrDefault(25565);
                Options.PROTOCOL_VERSION = serverVersion;

                Options.OPENAUTHMOD_AUTH = true;
                Options.BETACRAFT_AUTH = betaCraftAuth;

                ViaProxy.startProxy();

                SwingUtilities.invokeLater(() -> {
                    this.updateStateLabel();
                    this.stateButton.setEnabled(true);
                    this.stateButton.setText("Stop");
                });
            } catch (Throwable e) {
                SwingUtilities.invokeLater(() -> {
                    this.showError("Invalid server address!");
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


    public void setReady() {
        SwingUtilities.invokeLater(() -> {
            this.stateButton.setText("Start");
            this.stateButton.setEnabled(true);
        });
    }


    public void showInfo(final String message) {
        this.showNotification(message, JOptionPane.INFORMATION_MESSAGE);
    }

    public void showWarning(final String message) {
        this.showNotification(message, JOptionPane.WARNING_MESSAGE);
    }

    public void showError(final String message) {
        this.showNotification(message, JOptionPane.ERROR_MESSAGE);
    }

    public void showNotification(final String message, final int type) {
        JOptionPane.showMessageDialog(this, message, "ViaProxy", type);
    }

}
