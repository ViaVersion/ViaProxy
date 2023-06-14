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

import net.raphimc.mcauth.MinecraftAuth;
import net.raphimc.mcauth.step.bedrock.StepMCChain;
import net.raphimc.mcauth.step.bedrock.playfab.StepPlayFabToken;
import net.raphimc.mcauth.step.msa.StepMsaDeviceCode;
import net.raphimc.mcauth.util.MicrosoftConstants;
import net.raphimc.viaproxy.ViaProxy;
import net.raphimc.viaproxy.cli.options.Options;
import net.raphimc.viaproxy.saves.impl.accounts.Account;
import net.raphimc.viaproxy.saves.impl.accounts.BedrockAccount;
import net.raphimc.viaproxy.ui.AUITab;
import net.raphimc.viaproxy.ui.ViaProxyUI;
import net.raphimc.viaproxy.ui.popups.AddAccountPopup;
import net.raphimc.viaproxy.util.TFunction;
import org.apache.http.impl.client.CloseableHttpClient;

import javax.swing.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.function.Function;

public class AccountsTab extends AUITab {

    private JList<String> accountsList;
    private JButton addMicrosoftAccountButton;
    private JButton addBedrockAccountButton;

    private AddAccountPopup addAccountPopup;
    private Thread addThread;

    public AccountsTab(final ViaProxyUI frame) {
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
            JLabel info2Label = new JLabel("You can select the account by right clicking it. By default the first one will be used.");
            info2Label.setBounds(10, 30, 500, 20);
            contentPane.add(info2Label);
        }
        {
            JLabel infoLabel = new JLabel("<html>If you change your account frequently, you can install <a href=\"\">OpenAuthMod</a> on your</html>");
            infoLabel.setBounds(10, 60, 500, 20);
            contentPane.add(infoLabel);

            JLabel infoLabel2 = new JLabel("client. This allows ViaProxy to use the account you are logged in with on the client.");
            infoLabel2.setBounds(10, 80, 500, 20);
            contentPane.add(infoLabel2);

            JLabel clickRect = new JLabel();
            clickRect.setBounds(305, 60, 90, 24);
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
            scrollPane.setBounds(10, 105, 465, 110);
            contentPane.add(scrollPane);

            DefaultListModel<String> model = new DefaultListModel<>();
            this.accountsList = new JList<>(model);
            this.accountsList.addMouseListener(new MouseAdapter() {
                public void mousePressed(MouseEvent e) {
                    if (SwingUtilities.isRightMouseButton(e)) {
                        int row = accountsList.locationToIndex(e.getPoint());
                        accountsList.setSelectedIndex(row);
                    } else if (e.getClickCount() == 2) {
                        int index = accountsList.getSelectedIndex();
                        if (index != -1) markSelected(index);
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
                selectItem.addActionListener(event -> {
                    int index = this.accountsList.getSelectedIndex();
                    if (index != -1) this.markSelected(index);
                });
                contextMenu.add(selectItem);
            }
            {
                JMenuItem removeItem = new JMenuItem("Remove");
                removeItem.addActionListener(event -> {
                    int index = this.accountsList.getSelectedIndex();
                    if (index != -1) {
                        String removedName = model.remove(index);
                        if (removedName.contains("<")) {
                            if (model.isEmpty()) this.markSelected(-1);
                            else this.markSelected(0);
                        }

                        final Account account = ViaProxy.saveManager.accountsSave.getAccounts().get(index);
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
                moveUp.addActionListener(event -> {
                    int index = this.accountsList.getSelectedIndex();
                    if (index != -1) this.moveUp(index);
                });
                contextMenu.add(moveUp);
            }
            {
                JMenuItem moveDown = new JMenuItem("Move down ↓");
                moveDown.addActionListener(event -> {
                    int index = this.accountsList.getSelectedIndex();
                    if (index != -1) this.moveDown(index);
                });
                contextMenu.add(moveDown);
            }
            this.accountsList.setComponentPopupMenu(contextMenu);
        }

        final JPanel addButtons = new JPanel();
        addButtons.setBorder(BorderFactory.createTitledBorder("Add Account"));
        addButtons.setBounds(10, 220, 465, 50);
        addButtons.setLayout(null);
        contentPane.add(addButtons);

        {
            JButton addOfflineAccountButton = new JButton("Offline Account");
            addOfflineAccountButton.setBounds(10, 20, 145, 20);
            addOfflineAccountButton.addActionListener(event -> {
                String username = JOptionPane.showInputDialog(this.frame, "Enter your offline mode Username:", "Add Offline Account", JOptionPane.PLAIN_MESSAGE);
                if (username != null && !username.trim().isEmpty()) {
                    Account account = ViaProxy.saveManager.accountsSave.addAccount(username);
                    ViaProxy.saveManager.save();
                    this.addAccount(account);
                }
            });
            addButtons.add(addOfflineAccountButton);
        }
        {
            this.addMicrosoftAccountButton = new JButton("Microsoft Account");
            this.addMicrosoftAccountButton.setBounds(160, 20, 145, 20);
            this.addMicrosoftAccountButton.addActionListener(event -> {
                this.addMicrosoftAccountButton.setEnabled(false);
                this.handleLogin(msaDeviceCodeConsumer -> {
                    try (final CloseableHttpClient httpClient = MicrosoftConstants.createHttpClient()) {
                        return MinecraftAuth.JAVA_DEVICE_CODE_LOGIN.getFromInput(httpClient, new StepMsaDeviceCode.MsaDeviceCodeCallback(msaDeviceCodeConsumer));
                    }
                }, profile -> ViaProxy.saveManager.accountsSave.addAccount(profile));
            });
            addButtons.add(this.addMicrosoftAccountButton);
        }
        {
            this.addBedrockAccountButton = new JButton("Bedrock Account");
            this.addBedrockAccountButton.setBounds(310, 20, 145, 20);
            this.addBedrockAccountButton.addActionListener(event -> {
                this.addBedrockAccountButton.setEnabled(false);
                this.handleLogin(msaDeviceCodeConsumer -> {
                    try (final CloseableHttpClient httpClient = MicrosoftConstants.createHttpClient()) {
                        final StepMCChain.MCChain mcChain = MinecraftAuth.BEDROCK_DEVICE_CODE_LOGIN.getFromInput(httpClient, new StepMsaDeviceCode.MsaDeviceCodeCallback(msaDeviceCodeConsumer));
                        final StepPlayFabToken.PlayFabToken playFabToken = MinecraftAuth.BEDROCK_PLAY_FAB_TOKEN.getFromInput(httpClient, mcChain.prevResult().fullXblSession());

                        return new BedrockAccount(mcChain, playFabToken);
                    }
                }, account -> ViaProxy.saveManager.accountsSave.addAccount(account));
            });
            addButtons.add(this.addBedrockAccountButton);
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
        this.addBedrockAccountButton.setEnabled(true);
    }

    private void addAccount(final Account account) {
        DefaultListModel<String> model = (DefaultListModel<String>) this.accountsList.getModel();
        model.addElement(account.getDisplayString());
    }

    public void markSelected(final int index) {
        if (index < 0 || index >= this.accountsList.getModel().getSize()) {
            Options.MC_ACCOUNT = null;
            return;
        }

        DefaultListModel<String> model = (DefaultListModel<String>) this.accountsList.getModel();
        for (int i = 0; i < model.getSize(); i++) model.setElementAt(model.getElementAt(i).replaceAll("<[^>]+>", ""), i);
        model.setElementAt("<html><span style=\"color:rgb(0, 180, 0)\"><b>" + model.getElementAt(index) + "</b></span></html>", index);

        Account account = ViaProxy.saveManager.accountsSave.getAccounts().get(index);
        if (account != null) Options.MC_ACCOUNT = account;
        else throw new IllegalStateException("Account is null"); //Lists desynced
    }

    private void moveUp(final int index) {
        DefaultListModel<String> model = (DefaultListModel<String>) this.accountsList.getModel();
        if (index == 0) return;
        String name = model.remove(index);
        model.add(index - 1, name);
        this.accountsList.setSelectedIndex(index - 1);

        Account account = ViaProxy.saveManager.accountsSave.getAccounts().get(index);
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

        Account account = ViaProxy.saveManager.accountsSave.getAccounts().get(index);
        if (account != null) {
            ViaProxy.saveManager.accountsSave.removeAccount(account);
            ViaProxy.saveManager.accountsSave.addAccount(index + 1, account);
            ViaProxy.saveManager.save();
        } else {
            throw new IllegalStateException("Account is null");
        }
    }

    private <T> void handleLogin(final TFunction<Consumer<StepMsaDeviceCode.MsaDeviceCode>, T> requestHandler, final Function<T, Account> addHandler) {
        this.addThread = new Thread(() -> {
            try {
                T profile = requestHandler.apply(msaDeviceCode -> {
                    SwingUtilities.invokeLater(() -> {
                        new AddAccountPopup(this.frame, msaDeviceCode, popup -> this.addAccountPopup = popup, () -> {
                            this.closePopup();
                            this.addThread.interrupt();
                        });
                    });
                });
                SwingUtilities.invokeLater(() -> {
                    this.closePopup();
                    Account account = addHandler.apply(profile);
                    ViaProxy.saveManager.save();
                    this.addAccount(account);
                    this.frame.showInfo("The account " + account.getName() + " was added successfully.");
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
    }

}
