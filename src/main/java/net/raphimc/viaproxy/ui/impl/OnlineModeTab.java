package net.raphimc.viaproxy.ui.impl;

import net.raphimc.mcauth.MinecraftAuth;
import net.raphimc.mcauth.step.java.StepMCProfile;
import net.raphimc.viaproxy.ViaProxy;
import net.raphimc.viaproxy.cli.options.Options;
import net.raphimc.viaproxy.ui.AUITab;
import net.raphimc.viaproxy.ui.ViaProxyUI;
import net.raphimc.viaproxy.ui.popups.AddAccountPopup;

import javax.swing.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.concurrent.TimeoutException;

public class OnlineModeTab extends AUITab {

    private JList<String> accountsList;
    private JButton addMicrosoftAccountButton;

    private AddAccountPopup addAccountPopup;
    private Thread addThread;

    public OnlineModeTab(final ViaProxyUI frame) {
        super(frame, "Accounts");
    }

    @Override
    protected void init(JPanel contentPane) {
        {
            JLabel infoLabel = new JLabel("To join online mode servers you have to add minecraft accounts for ViaProxy to use.");
            infoLabel.setBounds(10, 10, 500, 20);
            contentPane.add(infoLabel);
        }
        {
            JLabel info2Label = new JLabel("You can select the account to use by right clicking it. By default the first one will be used.");
            info2Label.setBounds(10, 30, 500, 20);
            contentPane.add(info2Label);
        }
        {
            JLabel infoLabel = new JLabel("<html>If you change your account frequently, you might want to install <a href=\"\">OpenAuthMod</a> on your</html>");
            infoLabel.setBounds(10, 60, 500, 20);
            contentPane.add(infoLabel);

            JLabel infoLabel2 = new JLabel("client. This allows ViaProxy to use the account you are logged in with on the client.");
            infoLabel2.setBounds(10, 80, 500, 20);
            contentPane.add(infoLabel2);

            JLabel clickRect = new JLabel();
            clickRect.setBounds(353, 60, 80, 20);
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
            scrollPane.setBounds(10, 105, 465, 185);
            contentPane.add(scrollPane);

            DefaultListModel<String> model = new DefaultListModel<>();
            this.accountsList = new JList<>(model);
            this.accountsList.addMouseListener(new MouseAdapter() {
                public void mousePressed(MouseEvent e) {
                    if (SwingUtilities.isRightMouseButton(e)) {
                        int row = accountsList.locationToIndex(e.getPoint());
                        accountsList.setSelectedIndex(row);
                    }
                }
            });
            this.accountsList.addKeyListener(new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent e) {
                    int index = accountsList.getSelectedIndex();
                    if (index == -1) return;
                    if (e.getKeyCode() == KeyEvent.VK_UP) {
                        moveUp(index);
                        e.consume();
                    } else if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                        moveDown(index);
                        e.consume();
                    }
                }
            });
            scrollPane.setViewportView(this.accountsList);

            JPopupMenu contextMenu = new JPopupMenu();
            {
                JMenuItem selectItem = new JMenuItem("Select account");
                selectItem.addActionListener(e -> {
                    int index = this.accountsList.getSelectedIndex();
                    if (index != -1) this.markSelected(index);
                });
                contextMenu.add(selectItem);
            }
            {
                JMenuItem removeItem = new JMenuItem("Remove");
                removeItem.addActionListener(e -> {
                    int index = this.accountsList.getSelectedIndex();
                    if (index != -1) {
                        String removedName = model.remove(index);
                        if (removedName.contains("<")) {
                            if (model.isEmpty()) this.markSelected(-1);
                            else this.markSelected(0);
                        }

                        final StepMCProfile.MCProfile account = ViaProxy.saveManager.accountsSave.getAccounts().get(index);
                        if (account != null) {
                            ViaProxy.saveManager.accountsSave.removeAccount(account);
                            ViaProxy.saveManager.save();
                        } else {
                            throw new IllegalStateException("Account is null");
                        }
                    }
                    if (index < model.getSize()) this.accountsList.setSelectedIndex(index);
                    else if (index > 0) this.accountsList.setSelectedIndex(index - 1);
                });
                contextMenu.add(removeItem);
            }
            {
                JMenuItem moveUp = new JMenuItem("Move up ↑");
                moveUp.addActionListener(e -> {
                    int index = this.accountsList.getSelectedIndex();
                    if (index != -1) this.moveUp(index);
                });
                contextMenu.add(moveUp);
            }
            {
                JMenuItem moveDown = new JMenuItem("Move down ↓");
                moveDown.addActionListener(e -> {
                    int index = this.accountsList.getSelectedIndex();
                    if (index != -1) this.moveDown(index);
                });
                contextMenu.add(moveDown);
            }
            this.accountsList.setComponentPopupMenu(contextMenu);
        }
        {
            JButton addOfflineAccountButton = new JButton("Add Offline Account");
            addOfflineAccountButton.setBounds(10, 300, 230, 20);
            addOfflineAccountButton.addActionListener(event -> {
                String username = JOptionPane.showInputDialog(this.frame, "Enter your offline mode Username:", "Add Offline Account", JOptionPane.PLAIN_MESSAGE);
                if (username != null) {
                    StepMCProfile.MCProfile account = ViaProxy.saveManager.accountsSave.addOfflineAccount(username);
                    ViaProxy.saveManager.save();
                    this.addAccount(account);
                }
            });
            contentPane.add(addOfflineAccountButton);
        }
        {
            this.addMicrosoftAccountButton = new JButton("Add Microsoft Account");
            this.addMicrosoftAccountButton.setBounds(245, 300, 230, 20);
            this.addMicrosoftAccountButton.addActionListener(event -> {
                this.addMicrosoftAccountButton.setEnabled(false);
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
                            ViaProxy.saveManager.accountsSave.addAccount(profile);
                            ViaProxy.saveManager.save();
                            this.addAccount(profile);
                            this.frame.showInfo("The account " + profile.name() + " was added successfully.");
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
            contentPane.add(this.addMicrosoftAccountButton);
        }
    }

    @Override
    public void setReady() {
        ViaProxy.saveManager.accountsSave.getAccounts().forEach(this::addAccount);
        DefaultListModel<String> model = (DefaultListModel<String>) this.accountsList.getModel();
        if (!model.isEmpty()) this.markSelected(0);
    }

    private void closePopup() {
        this.addAccountPopup.markExternalClose();
        this.addAccountPopup.setVisible(false);
        this.addAccountPopup.dispose();
        this.addAccountPopup = null;
        this.addMicrosoftAccountButton.setEnabled(true);
    }

    private void addAccount(final StepMCProfile.MCProfile account) {
        DefaultListModel<String> model = (DefaultListModel<String>) this.accountsList.getModel();
        if (account.prevResult().items().isEmpty()) model.addElement(account.name() + " (Offline)");
        else model.addElement(account.name() + " (Microsoft)");
    }

    private void markSelected(final int index) {
        if (index == -1) {
            Options.MC_ACCOUNT = null;
            return;
        }

        DefaultListModel<String> model = (DefaultListModel<String>) this.accountsList.getModel();
        for (int i = 0; i < model.getSize(); i++) model.setElementAt(model.getElementAt(i).replaceAll("<[^>]+>", ""), i);
        model.setElementAt("<html><span style=\"color:rgb(0, 180, 0)\"><b>" + model.getElementAt(index) + "</b></span></html>", index);

        StepMCProfile.MCProfile account = ViaProxy.saveManager.accountsSave.getAccounts().get(index);
        if (account != null) Options.MC_ACCOUNT = account;
        else throw new IllegalStateException("Account is null"); //Lists desynced
    }

    private void moveUp(final int index) {
        DefaultListModel<String> model = (DefaultListModel<String>) this.accountsList.getModel();
        if (index == 0) return;
        String name = model.remove(index);
        model.add(index - 1, name);
        this.accountsList.setSelectedIndex(index - 1);

        StepMCProfile.MCProfile account = ViaProxy.saveManager.accountsSave.getAccounts().get(index);
        if (account != null) {
            ViaProxy.saveManager.accountsSave.removeAccount(account);
            ViaProxy.saveManager.accountsSave.addAccount(index - 1, account);
            ViaProxy.saveManager.save();
        } else {
            throw new IllegalStateException("Account is null");
        }
    }

    private void moveDown(final int index) {
        DefaultListModel<String> model = (DefaultListModel<String>) this.accountsList.getModel();
        if (index == model.getSize() - 1) return;
        String name = model.remove(index);
        model.add(index + 1, name);
        this.accountsList.setSelectedIndex(index + 1);

        StepMCProfile.MCProfile account = ViaProxy.saveManager.accountsSave.getAccounts().get(index);
        if (account != null) {
            ViaProxy.saveManager.accountsSave.removeAccount(account);
            ViaProxy.saveManager.accountsSave.addAccount(index + 1, account);
            ViaProxy.saveManager.save();
        } else {
            throw new IllegalStateException("Account is null");
        }
    }

}
