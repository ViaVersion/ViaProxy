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
import net.raphimc.mcauth.step.msa.StepMsaDeviceCode;
import net.raphimc.mcauth.util.MicrosoftConstants;
import net.raphimc.viaproxy.ViaProxy;
import net.raphimc.viaproxy.cli.options.Options;
import net.raphimc.viaproxy.saves.impl.accounts.Account;
import net.raphimc.viaproxy.saves.impl.accounts.BedrockAccount;
import net.raphimc.viaproxy.saves.impl.accounts.MicrosoftAccount;
import net.raphimc.viaproxy.ui.AUITab;
import net.raphimc.viaproxy.ui.ViaProxyUI;
import net.raphimc.viaproxy.ui.popups.AddAccountPopup;
import net.raphimc.viaproxy.util.GBC;
import net.raphimc.viaproxy.util.TFunction;
import org.apache.http.impl.client.CloseableHttpClient;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

import static net.raphimc.viaproxy.ui.ViaProxyUI.BODY_BLOCK_PADDING;
import static net.raphimc.viaproxy.ui.ViaProxyUI.BORDER_PADDING;

public class AccountsTab extends AUITab {

    private JList<Account> accountsList;
    private JButton addMicrosoftAccountButton;
    private JButton addBedrockAccountButton;

    private AddAccountPopup addAccountPopup;
    private Thread addThread;

    public AccountsTab(final ViaProxyUI frame) {
        super(frame, "Accounts");
    }

    @Override
    protected void init(JPanel contentPane) {
        JPanel body = new JPanel();
        body.setLayout(new GridBagLayout());

        {
            JLabel infoLabel = new JLabel("""
                    <html>
                    <p>To join online mode servers you have to add minecraft accounts for ViaProxy to use.</p>
                    <p>You can select the account by right clicking it. By default the first one will be used.</p>
                    <br>
                    <p>If you change your account frequently, you can install OpenAuthMod on your client.</p>
                    <p>This allows ViaProxy to use the account you are logged in with on the client.</p>
                    </html>""");
            GBC.create(body).grid(0, 0).weightx(1).insets(BORDER_PADDING, BORDER_PADDING, 0, BORDER_PADDING).fill(GridBagConstraints.HORIZONTAL).add(infoLabel);
        }
        {
            JScrollPane scrollPane = new JScrollPane();
            DefaultListModel<Account> model = new DefaultListModel<>();
            this.accountsList = new JList<>(model);
            this.accountsList.addMouseListener(new MouseAdapter() {
                public void mousePressed(MouseEvent e) {
                    if (SwingUtilities.isRightMouseButton(e)) {
                        int row = AccountsTab.this.accountsList.locationToIndex(e.getPoint());
                        AccountsTab.this.accountsList.setSelectedIndex(row);
                    } else if (e.getClickCount() == 2) {
                        int index = AccountsTab.this.accountsList.getSelectedIndex();
                        if (index != -1) AccountsTab.this.markSelected(index);
                    }
                }
            });
            this.accountsList.addKeyListener(new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent e) {
                    int index = AccountsTab.this.accountsList.getSelectedIndex();
                    if (index == -1) return;
                    if (e.getKeyCode() == KeyEvent.VK_UP) {
                        AccountsTab.this.moveUp(index);
                        e.consume();
                    } else if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                        AccountsTab.this.moveDown(index);
                        e.consume();
                    }
                }
            });
            this.accountsList.setCellRenderer(new DefaultListCellRenderer() {
                @Override
                public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                    DefaultListCellRenderer component = (DefaultListCellRenderer) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                    Account account = (Account) value;
                    if (Options.MC_ACCOUNT == account) {
                        component.setText("<html><span style=\"color:rgb(0, 180, 0)\"><b>" + account.getDisplayString() + "</b></span></html>");
                    } else {
                        component.setText(account.getDisplayString());
                    }
                    return component;
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
                        Account removed = model.remove(index);
                        ViaProxy.saveManager.accountsSave.removeAccount(removed);
                        ViaProxy.saveManager.save();
                        if (Options.MC_ACCOUNT == removed) {
                            if (model.isEmpty()) this.markSelected(-1);
                            else this.markSelected(0);
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
            GBC.create(body).grid(0, 1).weight(1, 1).insets(BODY_BLOCK_PADDING, BORDER_PADDING, 0, BORDER_PADDING).fill(GridBagConstraints.BOTH).add(scrollPane);
        }
        {
            final JPanel addButtons = new JPanel();
            addButtons.setLayout(new GridLayout(1, 3, BORDER_PADDING, 0));
            contentPane.add(addButtons);
            {
                JButton addOfflineAccountButton = new JButton("Offline Account");
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
                this.addMicrosoftAccountButton.addActionListener(event -> {
                    this.addMicrosoftAccountButton.setEnabled(false);
                    this.handleLogin(msaDeviceCodeConsumer -> {
                        try (final CloseableHttpClient httpClient = MicrosoftConstants.createHttpClient()) {
                            return new MicrosoftAccount(MinecraftAuth.JAVA_DEVICE_CODE_LOGIN.getFromInput(httpClient, new StepMsaDeviceCode.MsaDeviceCodeCallback(msaDeviceCodeConsumer)));
                        }
                    });
                });
                addButtons.add(this.addMicrosoftAccountButton);
            }
            {
                this.addBedrockAccountButton = new JButton("Bedrock Account");
                this.addBedrockAccountButton.addActionListener(event -> {
                    this.addBedrockAccountButton.setEnabled(false);
                    this.handleLogin(msaDeviceCodeConsumer -> {
                        try (final CloseableHttpClient httpClient = MicrosoftConstants.createHttpClient()) {
                            return new BedrockAccount(MinecraftAuth.BEDROCK_DEVICE_CODE_LOGIN.getFromInput(httpClient, new StepMsaDeviceCode.MsaDeviceCodeCallback(msaDeviceCodeConsumer)));
                        }
                    });
                });
                addButtons.add(this.addBedrockAccountButton);
            }

            JPanel border = new JPanel();
            border.setLayout(new GridBagLayout());
            border.setBorder(BorderFactory.createTitledBorder("Add Account"));
            GBC.create(border).grid(0, 0).weightx(1).insets(2, 4, 4, 4).fill(GridBagConstraints.HORIZONTAL).add(addButtons);

            GBC.create(body).grid(0, 2).weightx(1).insets(BODY_BLOCK_PADDING, BORDER_PADDING, BORDER_PADDING, BORDER_PADDING).fill(GridBagConstraints.HORIZONTAL).add(border);
        }

        contentPane.setLayout(new BorderLayout());
        contentPane.add(body, BorderLayout.CENTER);
    }

    @Override
    public void setReady() {
        ViaProxy.saveManager.accountsSave.getAccounts().forEach(this::addAccount);
        DefaultListModel<Account> model = (DefaultListModel<Account>) this.accountsList.getModel();
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
        DefaultListModel<Account> model = (DefaultListModel<Account>) this.accountsList.getModel();
        model.addElement(account);
    }

    public void markSelected(final int index) {
        if (index < 0 || index >= this.accountsList.getModel().getSize()) {
            Options.MC_ACCOUNT = null;
            return;
        }

        Options.MC_ACCOUNT = ViaProxy.saveManager.accountsSave.getAccounts().get(index);
        this.accountsList.repaint();
    }

    private void moveUp(final int index) {
        DefaultListModel<Account> model = (DefaultListModel<Account>) this.accountsList.getModel();
        if (model.getSize() == 0) return;
        if (index == 0) return;

        Account account = model.remove(index);
        model.add(index - 1, account);
        this.accountsList.setSelectedIndex(index - 1);

        ViaProxy.saveManager.accountsSave.removeAccount(account);
        ViaProxy.saveManager.accountsSave.addAccount(index - 1, account);
        ViaProxy.saveManager.save();
    }

    private void moveDown(final int index) {
        DefaultListModel<Account> model = (DefaultListModel<Account>) this.accountsList.getModel();
        if (model.getSize() == 0) return;
        if (index == model.getSize() - 1) return;

        Account account = model.remove(index);
        model.add(index + 1, account);
        this.accountsList.setSelectedIndex(index + 1);

        ViaProxy.saveManager.accountsSave.removeAccount(account);
        ViaProxy.saveManager.accountsSave.addAccount(index + 1, account);
        ViaProxy.saveManager.save();
    }

    private void handleLogin(final TFunction<Consumer<StepMsaDeviceCode.MsaDeviceCode>, Account> requestHandler) {
        this.addThread = new Thread(() -> {
            try {
                final Account account = requestHandler.apply(msaDeviceCode -> {
                    SwingUtilities.invokeLater(() -> {
                        new AddAccountPopup(this.frame, msaDeviceCode, popup -> this.addAccountPopup = popup, () -> {
                            this.closePopup();
                            this.addThread.interrupt();
                        });
                    });
                });
                SwingUtilities.invokeLater(() -> {
                    this.closePopup();
                    ViaProxy.saveManager.accountsSave.addAccount(account);
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
