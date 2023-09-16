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
package net.raphimc.viaproxy.ui;

import com.formdev.flatlaf.FlatDarkLaf;
import net.lenni0451.reflect.stream.RStream;
import net.raphimc.viaproxy.ViaProxy;
import net.raphimc.viaproxy.ui.impl.AccountsTab;
import net.raphimc.viaproxy.ui.impl.AdvancedTab;
import net.raphimc.viaproxy.ui.impl.GeneralTab;
import net.raphimc.viaproxy.util.logging.Logger;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public class ViaProxyUI extends JFrame {

    public static final int BORDER_PADDING = 10;
    public static final int BODY_BLOCK_PADDING = 10;

    private final JTabbedPane contentPane = new JTabbedPane();
    private final List<AUITab> tabs = new ArrayList<>();

    public final GeneralTab generalTab = new GeneralTab(this);
    public final AdvancedTab advancedTab = new AdvancedTab(this);
    public final AccountsTab accountsTab = new AccountsTab(this);

    private ImageIcon icon;

    public ViaProxyUI() {
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> this.showException(e));

        this.setLookAndFeel();
        this.loadIcons();
        this.initWindow();
        this.initTabs();

        ToolTipManager.sharedInstance().setInitialDelay(100);
        ToolTipManager.sharedInstance().setDismissDelay(10_000);
        SwingUtilities.updateComponentTreeUI(this);
        this.setVisible(true);
    }

    private void setLookAndFeel() {
        try {
            FlatDarkLaf.setup();

            final Font font = Font.createFont(Font.TRUETYPE_FONT, this.getClass().getClassLoader().getResourceAsStream("assets/fonts/OpenSans-Regular.ttf")).deriveFont(Font.PLAIN, 12F);
            GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(font);
            UIManager.getLookAndFeelDefaults().put("defaultFont", font);
            UIManager.getLookAndFeelDefaults().put("TextComponent.arc", 5);
            UIManager.getLookAndFeelDefaults().put("Button.arc", 5);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private void loadIcons() {
        this.icon = new ImageIcon(this.getClass().getClassLoader().getResource("assets/icons/icon.png"));
    }

    private void initWindow() {
        this.setTitle("ViaProxy v" + ViaProxy.VERSION);
        this.setIconImage(this.icon.getImage());
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                for (AUITab tab : ViaProxyUI.this.tabs) tab.onClose();
            }
        });
        this.setSize(500, 360);
        this.setMinimumSize(this.getSize());
        this.setLocationRelativeTo(null);
        this.setContentPane(this.contentPane);
    }

    private void initTabs() {
        RStream
                .of(this)
                .fields()
                .filter(field -> AUITab.class.isAssignableFrom(field.type()))
                .forEach(field -> {
                    AUITab tab = field.get();
                    this.tabs.add(field.get());
                    tab.add(this.contentPane);
                });

        this.contentPane.setEnabledAt(this.contentPane.indexOfTab(this.accountsTab.getName()), false);
    }


    public void setReady() {
        for (AUITab tab : this.tabs) tab.setReady();
        for (int i = 0; i < this.contentPane.getTabCount(); i++) this.contentPane.setEnabledAt(i, true);
    }

    public void openURL(final String url) {
        try {
            Desktop.getDesktop().browse(new URI(url));
        } catch (Throwable t) {
            this.showInfo("Couldn't open the link :(\nHere it is for you: " + url);
        }
    }

    public void showException(final Throwable t) {
        Logger.LOGGER.error("Caught exception in thread " + Thread.currentThread().getName(), t);
        StringBuilder builder = new StringBuilder("An error occurred:\n");
        builder.append("[").append(t.getClass().getSimpleName()).append("] ").append(t.getMessage()).append("\n");
        for (StackTraceElement element : t.getStackTrace()) builder.append(element.toString()).append("\n");
        this.showError(builder.toString());
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
