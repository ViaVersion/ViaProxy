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

import net.lenni0451.commons.swing.GBC;
import net.raphimc.minecraftauth.MinecraftAuth;
import net.raphimc.minecraftauth.step.msa.StepMsaDeviceCode;
import net.raphimc.viaproxy.ViaProxy;
import net.raphimc.viaproxy.saves.impl.accounts.Account;
import net.raphimc.viaproxy.saves.impl.accounts.BedrockAccount;
import net.raphimc.viaproxy.saves.impl.accounts.MicrosoftAccount;
import net.raphimc.viaproxy.ui.I18n;
import net.raphimc.viaproxy.ui.UITab;
import net.raphimc.viaproxy.ui.ViaProxyWindow;
import net.raphimc.viaproxy.ui.popups.AddAccountPopup;
import net.raphimc.viaproxy.util.TFunction;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

import static net.raphimc.viaproxy.ui.ViaProxyWindow.BODY_BLOCK_PADDING;
import static net.raphimc.viaproxy.ui.ViaProxyWindow.BORDER_PADDING;

public class AccountsTab extends UITab {

    private JList<Account> accountsList;
    private JButton addMicrosoftAccountButton;
    private JButton addBedrockAccountButton;

    private AddAccountPopup addAccountPopup;
    private Thread addThread;

    public AccountsTab(final ViaProxyWindow frame) {
        super(frame, "accounts");
    }

    @Override
    protected void init(JPanel contentPane) {
        JPanel body = new JPanel();
        body.setLayout(new GridBagLayout());

        int gridy = 0;
        {
            JLabel infoLabel = new JLabel("<html><p>" + I18n.get("tab.accounts.description.line1") + "</p></html>");
            GBC.create(body).grid(0, gridy++).weightx(1).insets(BORDER_PADDING, BORDER_PADDING, 0, BORDER_PADDING).fill(GBC.HORIZONTAL).add(infoLabel);
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
                    if (ViaProxy.getConfig().getAccount() == account) {
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
                JMenuItem selectItem = new JMenuItem(I18n.get("tab.accounts.list.context_menu.select"));
                selectItem.addActionListener(event -> {
                    int index = this.accountsList.getSelectedIndex();
                    if (index != -1) this.markSelected(index);
                });
                contextMenu.add(selectItem);
            }
            {
                JMenuItem removeItem = new JMenuItem(I18n.get("tab.accounts.list.context_menu.remove"));
                removeItem.addActionListener(event -> {
                    int index = this.accountsList.getSelectedIndex();
                    if (index != -1) {
                        Account removed = model.remove(index);
                        ViaProxy.getSaveManager().accountsSave.removeAccount(removed);
                        ViaProxy.getSaveManager().save();
                        if (ViaProxy.getConfig().getAccount() == removed) {
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
                JMenuItem moveUp = new JMenuItem(I18n.get("tab.accounts.list.context_menu.move_up"));
                moveUp.addActionListener(event -> {
                    int index = this.accountsList.getSelectedIndex();
                    if (index != -1) this.moveUp(index);
                });
                contextMenu.add(moveUp);
            }
            {
                JMenuItem moveDown = new JMenuItem(I18n.get("tab.accounts.list.context_menu.move_down"));
                moveDown.addActionListener(event -> {
                    int index = this.accountsList.getSelectedIndex();
                    if (index != -1) this.moveDown(index);
                });
                contextMenu.add(moveDown);
            }
            this.accountsList.setComponentPopupMenu(contextMenu);
            GBC.create(body).grid(0, gridy++).weight(1, 1).insets(BODY_BLOCK_PADDING, BORDER_PADDING, 0, BORDER_PADDING).fill(GBC.BOTH).add(scrollPane);
        }
        {
            final JPanel addButtons = new JPanel();
            addButtons.setLayout(new GridLayout(1, 3, BORDER_PADDING, 0));
            {
                JButton addOfflineAccountButton = new JButton(I18n.get("tab.accounts.add_offline.label"));
                addOfflineAccountButton.addActionListener(event -> {
                    String username = JOptionPane.showInputDialog(this.viaProxyWindow, I18n.get("tab.accounts.add_offline.enter_username"), I18n.get("tab.accounts.add.title"), JOptionPane.PLAIN_MESSAGE);
                    if (username != null && !username.trim().isEmpty()) {
                        Account account = ViaProxy.getSaveManager().accountsSave.addAccount(username);
                        ViaProxy.getSaveManager().save();
                        this.addAccount(account);
                    }
                });
                addButtons.add(addOfflineAccountButton);
            }
            {
                this.addMicrosoftAccountButton = new JButton(I18n.get("tab.accounts.add_microsoft.label"));
                this.addMicrosoftAccountButton.addActionListener(event -> {
                    this.addMicrosoftAccountButton.setEnabled(false);
                    this.handleLogin(msaDeviceCodeConsumer -> {
                        return new MicrosoftAccount(MicrosoftAccount.DEVICE_CODE_LOGIN.getFromInput(MinecraftAuth.createHttpClient(), new StepMsaDeviceCode.MsaDeviceCodeCallback(msaDeviceCodeConsumer)));
                    });
                });
                addButtons.add(this.addMicrosoftAccountButton);
            }
            {
                this.addBedrockAccountButton = new JButton(I18n.get("tab.accounts.add_bedrock.label"));
                this.addBedrockAccountButton.addActionListener(event -> {
                    this.addBedrockAccountButton.setEnabled(false);
                    this.handleLogin(msaDeviceCodeConsumer -> {
                        return new BedrockAccount(BedrockAccount.DEVICE_CODE_LOGIN.getFromInput(MinecraftAuth.createHttpClient(), new StepMsaDeviceCode.MsaDeviceCodeCallback(msaDeviceCodeConsumer)));
                    });
                });
                addButtons.add(this.addBedrockAccountButton);
            }

            JPanel border = new JPanel();
            border.setLayout(new GridBagLayout());
            border.setBorder(BorderFactory.createTitledBorder(I18n.get("tab.accounts.add.title")));
            GBC.create(border).grid(0, 0).weightx(1).insets(2, 4, 4, 4).fill(GBC.HORIZONTAL).add(addButtons);

            GBC.create(body).grid(0, gridy++).weightx(1).insets(BODY_BLOCK_PADDING, BORDER_PADDING, BORDER_PADDING, BORDER_PADDING).fill(GBC.HORIZONTAL).add(border);
        }

        contentPane.setLayout(new BorderLayout());
        contentPane.add(body, BorderLayout.CENTER);

        ViaProxy.getSaveManager().accountsSave.getAccounts().forEach(this::addAccount);
        DefaultListModel<Account> model = (DefaultListModel<Account>) this.accountsList.getModel();
        if (!model.isEmpty()) this.markSelected(0);
    }

    private void closePopup() { // Might be getting called multiple times
        if (this.addAccountPopup != null) {
            this.addAccountPopup.markExternalClose();
            this.addAccountPopup.setVisible(false);
            this.addAccountPopup.dispose();
            this.addAccountPopup = null;
        }
        this.addMicrosoftAccountButton.setEnabled(true);
        this.addBedrockAccountButton.setEnabled(true);
    }

    private void addAccount(final Account account) {
        DefaultListModel<Account> model = (DefaultListModel<Account>) this.accountsList.getModel();
        model.addElement(account);
    }

    public void markSelected(final int index) {
        if (index < 0 || index >= this.accountsList.getModel().getSize()) {
            ViaProxy.getConfig().setAccount(null);
            return;
        }

        ViaProxy.getConfig().setAccount(ViaProxy.getSaveManager().accountsSave.getAccounts().get(index));
        this.accountsList.repaint();
    }

    private void moveUp(final int index) {
        DefaultListModel<Account> model = (DefaultListModel<Account>) this.accountsList.getModel();
        if (model.getSize() == 0) return;
        if (index == 0) return;

        Account account = model.remove(index);
        model.add(index - 1, account);
        this.accountsList.setSelectedIndex(index - 1);

        ViaProxy.getSaveManager().accountsSave.removeAccount(account);
        ViaProxy.getSaveManager().accountsSave.addAccount(index - 1, account);
        ViaProxy.getSaveManager().save();
    }

    private void moveDown(final int index) {
        DefaultListModel<Account> model = (DefaultListModel<Account>) this.accountsList.getModel();
        if (model.getSize() == 0) return;
        if (index == model.getSize() - 1) return;

        Account account = model.remove(index);
        model.add(index + 1, account);
        this.accountsList.setSelectedIndex(index + 1);

        ViaProxy.getSaveManager().accountsSave.removeAccount(account);
        ViaProxy.getSaveManager().accountsSave.addAccount(index + 1, account);
        ViaProxy.getSaveManager().save();
    }

    private void handleLogin(final TFunction<Consumer<StepMsaDeviceCode.MsaDeviceCode>, Account> requestHandler) {
        this.addThread = new Thread(() -> {
            try {
                final Account account = requestHandler.apply(msaDeviceCode -> SwingUtilities.invokeLater(() -> new AddAccountPopup(this.viaProxyWindow, msaDeviceCode, popup -> this.addAccountPopup = popup, () -> {
                    this.closePopup();
                    this.addThread.interrupt();
                })));
                SwingUtilities.invokeLater(() -> {
                    this.closePopup();
                    ViaProxy.getSaveManager().accountsSave.addAccount(account);
                    ViaProxy.getSaveManager().save();
                    this.addAccount(account);
                    ViaProxyWindow.showInfo(I18n.get("tab.accounts.add.success", account.getName()));
                });
            } catch (InterruptedException ignored) {
            } catch (TimeoutException e) {
                SwingUtilities.invokeLater(() -> {
                    this.closePopup();
                    ViaProxyWindow.showError(I18n.get("tab.accounts.add.timeout", "60"));
                });
            } catch (Throwable t) {
                SwingUtilities.invokeLater(() -> {
                    this.closePopup();
                    ViaProxyWindow.showException(t);
                });
            }
        }, "Add Account Thread");
        this.addThread.setDaemon(true);
        this.addThread.start();
    }

}
