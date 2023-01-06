package net.raphimc.viaproxy.ui;

import javax.swing.*;

public abstract class AUITab {

    protected final ViaProxyUI frame;
    private final String name;
    private final JPanel contentPane;

    public AUITab(final ViaProxyUI frame, final String name) {
        this.frame = frame;
        this.name = name;
        this.contentPane = new JPanel();

        this.contentPane.setLayout(null);
        this.init(this.contentPane);
    }

    public void add(final JTabbedPane tabbedPane) {
        tabbedPane.addTab(this.name, this.contentPane);
    }

    protected abstract void init(final JPanel contentPane);

    public void setReady() {
    }

}
