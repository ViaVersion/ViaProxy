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
package net.raphimc.viaproxy.cli;

public class ConsoleFormatter {

    private static final String PREFIX = "\033[";
    private static final String SUFFIX = "m";

    public static String convert(final String s) {
        StringBuilder out = new StringBuilder();
        char[] chars = s.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];
            char next = i + 1 < chars.length ? chars[i + 1] : '\0';
            if (c == '§') {
                if (next != '\0') {
                    if (isColor(next)) out.append(convertToAnsi('r'));
                    out.append(convertToAnsi(next));
                    i++;
                }
            } else {
                out.append(c);
            }
        }
        return out + convertToAnsi('r');
    }

    //The ANSI codes are an approximation. True color is used to get the exact color.
    private static String convertToAnsi(final char color) {
        switch (Character.toLowerCase(color)) {
            case '0': //Black
                return PREFIX + getColor("30", 0x00_00_00) + SUFFIX;
            case '1': //Dark Blue
                return PREFIX + getColor("34", 0x00_00_AA) + SUFFIX;
            case '2': //Dark Green
                return PREFIX + getColor("32", 0x00_AA_00) + SUFFIX;
            case '3': //Dark Aqua
                return PREFIX + getColor("36", 0x00_AA_AA) + SUFFIX;
            case '4': //Dark Red
                return PREFIX + getColor("31", 0xAA_00_00) + SUFFIX;
            case '5': //Dark Purple
                return PREFIX + getColor("35", 0xAA_00_AA) + SUFFIX;
            case '6': //Gold
                return PREFIX + getColor("33", 0xFF_AA_00) + SUFFIX;
            case '7': //Gray
                return PREFIX + getColor("37", 0xAA_AA_AA) + SUFFIX;
            case '8': //Dark Gray
                return PREFIX + getColor("90", 0x55_55_55) + SUFFIX;
            case '9': //Blue
                return PREFIX + getColor("94", 0x55_55_FF) + SUFFIX;
            case 'a': //Green
                return PREFIX + getColor("92", 0x55_FF_55) + SUFFIX;
            case 'b': //Aqua
                return PREFIX + getColor("96", 0x55_FF_FF) + SUFFIX;
            case 'c': //Red
                return PREFIX + getColor("91", 0xFF_55_55) + SUFFIX;
            case 'd': //Light Purple
                return PREFIX + getColor("95", 0xFF_55_FF) + SUFFIX;
            case 'e': //Yellow
                return PREFIX + getColor("93", 0xFF_FF_55) + SUFFIX;
            case 'f': //White
                return PREFIX + getColor("97", 0xFF_FF_FF) + SUFFIX;
            case 'k': //Obfuscated
                return ""; //Not supported in terminal
            case 'l': //Bold
                return PREFIX + "1" + SUFFIX;
            case 'm': //Strikethrough
                return PREFIX + "9" + SUFFIX;
            case 'n': //Underline
                return PREFIX + "4" + SUFFIX;
            case 'o': //Italic
                return PREFIX + "3" + SUFFIX;
            case 'r': //Reset
            default:
                return PREFIX + 0 + SUFFIX;
        }
    }

    private static boolean isColor(char color) {
        color = Character.toLowerCase(color);
        return color >= '0' && color <= '9' || color >= 'a' && color <= 'f';
    }

    private static String getColor(final String ansi, final int rgb) {
        return String.format("38;2;%d;%d;%d", (rgb >> 16) & 255, (rgb >> 8) & 255, rgb & 255);
    }

}
