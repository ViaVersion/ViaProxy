/*
 * This file is part of ViaProxy - https://github.com/RaphiMC/ViaProxy
 * Copyright (C) 2021-2025 RK_01/RaphiMC and contributors
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
import com.formdev.flatlaf.extras.FlatInspector;
import com.formdev.flatlaf.extras.FlatUIDefaultsInspector;
import net.lenni0451.lambdaevents.LambdaManager;
import net.lenni0451.lambdaevents.generator.LambdaMetaFactoryGenerator;
import net.lenni0451.reflect.JavaBypass;
import net.lenni0451.reflect.stream.RStream;
import net.raphimc.viaproxy.ViaProxy;
import net.raphimc.viaproxy.ui.events.UICloseEvent;
import net.raphimc.viaproxy.ui.impl.*;
import net.raphimc.viaproxy.util.logging.Logger;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public class ViaProxyWindow extends JFrame {

    public final LambdaManager eventManager = LambdaManager.threadSafe(new LambdaMetaFactoryGenerator(JavaBypass.TRUSTED_LOOKUP));

    public static final int BORDER_PADDING = 10;
    public static final int BODY_BLOCK_PADDING = 10;

    public final JTabbedPane contentPane = new JTabbedPane();
    private final List<UITab> tabs = new ArrayList<>();

    public final GeneralTab generalTab = new GeneralTab(this);
    public final AdvancedTab advancedTab = new AdvancedTab(this);
    public final AccountsTab accountsTab = new AccountsTab(this);
    public final RealmsTab realmsTab = new RealmsTab(this);
    public final UISettingsTab uiSettingsTab = new UISettingsTab(this);

    private ImageIcon icon;

    public ViaProxyWindow() {
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> showException(e));
        this.eventManager.register(this);

        this.setLookAndFeel();
        this.loadIcons();
        this.initWindow();
        this.initTabs();

        FlatInspector.install("ctrl shift I");
        FlatUIDefaultsInspector.install("ctrl shift O");
        ToolTipManager.sharedInstance().setInitialDelay(100);
        ToolTipManager.sharedInstance().setDismissDelay(10_000);
        SwingUtilities.updateComponentTreeUI(this);
        this.setVisible(true);
    }

    private void setLookAndFeel() {
        try {
            FlatDarkLaf.setup();

            UIManager.getLookAndFeelDefaults().put("TextComponent.arc", 5);
            UIManager.getLookAndFeelDefaults().put("Button.arc", 5);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private void loadIcons() {
        this.icon = new ImageIcon(this.getClass().getClassLoader().getResource("assets/viaproxy/icons/icon.png"));
    }

    private void initWindow() {
        this.setTitle("ViaProxy v" + ViaProxy.VERSION);
        this.setIconImage(this.icon.getImage());
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                ViaProxyWindow.this.eventManager.call(new UICloseEvent());
                ViaProxy.getConfig().save();
                ViaProxy.getSaveManager().save();
            }
        });
        this.setSize(500, 380);
        this.setMinimumSize(this.getSize());
        this.setLocationRelativeTo(null);
        this.setContentPane(this.contentPane);
    }

    private void initTabs() {
        RStream
                .of(this)
                .fields()
                .filter(field -> UITab.class.isAssignableFrom(field.type()))
                .forEach(field -> {
                    final UITab tab = field.get();
                    this.tabs.add(field.get());
                    tab.add(this.contentPane);
                    this.eventManager.register(tab);
                });
        this.contentPane.addChangeListener(e -> {
            int selectedIndex = contentPane.getSelectedIndex();
            if (selectedIndex >= 0 && selectedIndex < ViaProxyWindow.this.tabs.size()) ViaProxyWindow.this.tabs.get(selectedIndex).onTabOpened();
        });
    }

    public static void openURL(final String url) {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(url));
            } else {
                new ProcessBuilder("xdg-open", url).start();
            }
        } catch (Throwable t) {
            showInfo(I18n.get("generic.could_not_open_url", url));
        }
    }

    public static void showException(final Throwable t) {
        Logger.LOGGER.error("Caught exception in thread " + Thread.currentThread().getName(), t);
        StringBuilder builder = new StringBuilder("An error occurred:\n");
        builder.append("[").append(t.getClass().getSimpleName()).append("] ").append(t.getMessage()).append("\n");
        for (StackTraceElement element : t.getStackTrace()) builder.append(element.toString()).append("\n");
        showError(builder.toString());
    }

    public static void showInfo(final String message) {
        showNotification(message, JOptionPane.INFORMATION_MESSAGE);
    }

    public static void showWarning(final String message) {
        showNotification(message, JOptionPane.WARNING_MESSAGE);
    }

    public static void showError(final String message) {
        showNotification(message, JOptionPane.ERROR_MESSAGE);
    }

    public static void showNotification(final String message, final int type) {
        JOptionPane.showMessageDialog(ViaProxy.getForegroundWindow(), message, "ViaProxy", type);
    }

}
