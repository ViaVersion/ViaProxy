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
import net.raphimc.viaproxy.ViaProxy;
import net.raphimc.viaproxy.ui.I18n;
import net.raphimc.viaproxy.ui.UITab;
import net.raphimc.viaproxy.ui.ViaProxyWindow;
import net.raphimc.viaproxy.util.logging.Logger;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import static net.raphimc.viaproxy.ui.ViaProxyWindow.BORDER_PADDING;

public class UISettingsTab extends UITab {

    public UISettingsTab(final ViaProxyWindow frame) {
        super(frame, "ui_settings");
    }

    @Override
    protected void init(JPanel contentPane) {
        JPanel body = new JPanel();
        body.setLayout(new GridBagLayout());

        int gridy = 0;
        {
            JLabel languageLabel = new JLabel(I18n.get("tab.ui_settings.language.label"));
            GBC.create(body).grid(0, gridy++).insets(BORDER_PADDING, BORDER_PADDING, 0, BORDER_PADDING).anchor(GBC.NORTHWEST).add(languageLabel);

            JComboBox<String> language = new JComboBox<>(I18n.getAvailableLocales().toArray(new String[0]));
            language.setRenderer(new DefaultListCellRenderer() {
                @Override
                public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                    if (value instanceof String locale) {
                        value = "<html><b>" + I18n.getSpecific(locale, "language.name") + "</b> (" + I18n.get("tab.ui_settings.language.completion", I18n.getSpecific(locale, "language.completion")) + ") " + I18n.get("tab.ui_settings.language.author", I18n.getSpecific(locale, "language.author")) + "</html>";
                    }
                    return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                }
            });
            language.setSelectedItem(I18n.getCurrentLocale());
            language.addActionListener(event -> {
                if (!(language.getSelectedItem() instanceof String locale)) return;
                if (locale.equals(I18n.getCurrentLocale())) return;
                I18n.setLocale(locale);
                ViaProxyWindow.showInfo(I18n.get("tab.ui_settings.language.success", I18n.get("language.name"), locale));
                try {
                    final File f = new File(ViaProxy.class.getProtectionDomain().getCodeSource().getLocation().toURI());
                    Runtime.getRuntime().exec(new String[]{System.getProperty("java.home") + "/bin/java", "-jar", f.getAbsolutePath()});
                    System.exit(0);
                } catch (URISyntaxException | IOException e) {
                    Logger.LOGGER.error("Could not start the ViaProxy jar", e);
                    ViaProxyWindow.showException(e);
                    System.exit(1);
                }
            });
            GBC.create(body).grid(0, gridy++).weightx(1).insets(0, BORDER_PADDING, 0, BORDER_PADDING).fill(GBC.HORIZONTAL).add(language);
        }

        contentPane.setLayout(new BorderLayout());
        contentPane.add(body, BorderLayout.NORTH);
    }

}
