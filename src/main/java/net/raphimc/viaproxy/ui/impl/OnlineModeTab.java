package net.raphimc.viaproxy.ui.impl;

import net.raphimc.mcauth.MinecraftAuth;
import net.raphimc.mcauth.step.java.StepMCProfile;
import net.raphimc.viaproxy.ui.AUITab;
import net.raphimc.viaproxy.ui.ViaProxyUI;
import net.raphimc.viaproxy.ui.popups.AddAccountPopup;

import javax.swing.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.concurrent.TimeoutException;

public class OnlineModeTab extends AUITab {

    private JList<String> accountsList;
    private JButton addAccountButton;

    private AddAccountPopup addAccountPopup;
    private Thread addThread;

    public OnlineModeTab(final ViaProxyUI frame) {
        super(frame, "Online Mode");
    }

    @Override
    protected void init(JPanel contentPane) {
        {
            JLabel infoLabel = new JLabel("To join online mode servers you have to add minecraft accounts for ViaProxy to use.");
            infoLabel.setBounds(10, 10, 500, 20);
            contentPane.add(infoLabel);
        }
        {
            JLabel infoLabel = new JLabel("<html>If you change your account frequently, you might want to install <a href=\"\">OpenAuthMod</a> on your</html>");
            infoLabel.setBounds(10, 40, 500, 20);
            contentPane.add(infoLabel);

            JLabel infoLabel2 = new JLabel("client. This allows ViaProxy to use the account you are logged in with on the client.");
            infoLabel2.setBounds(10, 60, 500, 20);
            contentPane.add(infoLabel2);

            JLabel clickRect = new JLabel();
            clickRect.setBounds(353, 40, 80, 20);
            clickRect.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseReleased(MouseEvent e) {
                    frame.openURL("https://github.com/RaphiMC/OpenAuthMod/");
                }
            });
            contentPane.add(clickRect);
        }
        {
            JScrollPane scrollPane = new JScrollPane();
            scrollPane.setBounds(10, 85, 465, 205);
            contentPane.add(scrollPane);

            DefaultListModel<String> model = new DefaultListModel<>();
            this.accountsList = new JList<>(model);
            scrollPane.setViewportView(this.accountsList);

            JPopupMenu contextMenu = new JPopupMenu();
            JMenuItem removeItem = new JMenuItem("Remove");
            removeItem.addActionListener(e -> {
                int index = this.accountsList.getSelectedIndex();
                if (index != -1) {
                    model.remove(index);
                    //TODO: Remove from save
                }
                if (index < model.getSize()) this.accountsList.setSelectedIndex(index);
                else if (index > 0) this.accountsList.setSelectedIndex(index - 1);
            });
            contextMenu.add(removeItem);
            this.accountsList.setComponentPopupMenu(contextMenu);
        }
        {
            this.addAccountButton = new JButton("Add Account");
            this.addAccountButton.setBounds(300, 300, 175, 20);
            this.addAccountButton.addActionListener(event -> {
                this.addAccountButton.setEnabled(false);
                this.addThread = new Thread(() -> {
                    try {
                        StepMCProfile.MCProfile profile = MinecraftAuth.requestJavaLogin(msaDeviceCode -> {
                            SwingUtilities.invokeLater(() -> {
                                new AddAccountPopup(this.frame, msaDeviceCode, popup -> this.addAccountPopup = popup, () -> {
                                    this.closePopup();
                                    this.addThread.interrupt();
                                });
                            });
                        });
                        SwingUtilities.invokeLater(() -> {
                            this.closePopup();
                            DefaultListModel<String> model = (DefaultListModel<String>) this.accountsList.getModel();
                            model.addElement(profile.name());
                            this.frame.showInfo("The account " + profile.name() + " was added successfully.");
                            //TODO: Add to save
                        });
                    } catch (InterruptedException ignored) {
                    } catch (TimeoutException e) {
                        SwingUtilities.invokeLater(() -> {
                            this.closePopup();
                            this.frame.showError("The login request timed out.\nPlease login within 60 seconds.");
                        });
                    } catch (Throwable t) {
                        SwingUtilities.invokeLater(() -> {
                            this.closePopup();
                            this.frame.showException(t);
                        });
                    }
                }, "Add Account Thread");
                this.addThread.setDaemon(true);
                this.addThread.start();
            });
            contentPane.add(this.addAccountButton);
        }
    }

    private void closePopup() {
        this.addAccountPopup.markExternalClose();
        this.addAccountPopup.setVisible(false);
        this.addAccountPopup.dispose();
        this.addAccountPopup = null;
        this.addAccountButton.setEnabled(true);
    }

}
