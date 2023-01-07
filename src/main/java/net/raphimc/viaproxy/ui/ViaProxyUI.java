package net.raphimc.viaproxy.ui;

import com.formdev.flatlaf.FlatDarkLaf;
import net.lenni0451.reflect.stream.RStream;
import net.raphimc.viaproxy.ViaProxy;
import net.raphimc.viaproxy.ui.impl.AccountsTab;
import net.raphimc.viaproxy.ui.impl.GeneralTab;
import net.raphimc.viaproxy.util.logging.Logger;

import javax.swing.*;
import java.awt.*;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public class ViaProxyUI extends JFrame {

    private final JTabbedPane contentPane = new JTabbedPane();
    private final List<AUITab> tabs = new ArrayList<>();
    private final GeneralTab generalTab = new GeneralTab(this);
    private final AccountsTab accountsTab = new AccountsTab(this);

    private ImageIcon icon;

    public ViaProxyUI() {
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> this.showException(e));

        this.setLookAndFeel();
        this.loadIcons();
        this.initWindow();
        this.initTabs();

        SwingUtilities.updateComponentTreeUI(this);
        this.setVisible(true);
    }

    private void setLookAndFeel() {
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
        this.setTitle("ViaProxy v" + ViaProxy.VERSION);
        this.setIconImage(this.icon.getImage());
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setSize(500, 403);
        this.setResizable(false);
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

        this.contentPane.setEnabledAt(1, false);
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
