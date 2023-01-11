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
package net.raphimc.viaproxy.util;

import java.lang.reflect.Array;

public class ArrayHelper {

    public static ArrayHelper instanceOf(final String... array) {
        return new ArrayHelper(array);
    }


    private String[] array;

    public ArrayHelper(final String[] array) {
        this.array = array;
    }

    public int getLength() {
        return this.array.length;
    }

    public boolean isLength(final int length) {
        return this.getLength() == length;
    }

    public boolean isSmaller(final int length) {
        return this.getLength() < length;
    }

    public boolean isSmallerOrEqual(final int length) {
        return this.getLength() <= length;
    }

    public boolean isLarger(final int length) {
        return this.getLength() > length;
    }

    public boolean isLargerOrEqual(final int length) {
        return this.getLength() >= length;
    }

    public boolean isEmpty() {
        return this.getLength() == 0;
    }

    public boolean isIndexValid(final int index) {
        return index >= 0 && index < this.getLength();
    }

    public String get(final int index) {
        if (!this.isIndexValid(index)) {
            return null;
        }

        return this.array[index];
    }


    public boolean isString(final int index) {
        return this.isIndexValid(index);
    }

    public boolean isBoolean(final int index) {
        if (!this.isIndexValid(index)) {
            return false;
        }

        try {
            Boolean.valueOf(this.getString(index));
            return true;
        } catch (Exception ignored) {
        }

        return false;
    }

    public boolean isChar(final int index) {
        if (!this.isIndexValid(index) || !this.isString(index)) {
            return false;
        }

        return this.getString(index).length() == 1;
    }

    public boolean isShort(final int index) {
        if (!this.isIndexValid(index)) {
            return false;
        }

        try {
            Short.valueOf(this.get(index));
            return true;
        } catch (Exception ignored) {
        }

        return false;
    }

    public boolean isInteger(final int index) {
        if (!this.isIndexValid(index)) {
            return false;
        }

        try {
            Integer.valueOf(this.get(index));
            return true;
        } catch (Exception ignored) {
        }

        return false;
    }

    public boolean isLong(final int index) {
        if (!this.isIndexValid(index)) {
            return false;
        }

        try {
            Long.valueOf(this.get(index));
            return true;
        } catch (Exception ignored) {
        }

        return false;
    }

    public boolean isFloat(final int index) {
        if (!this.isIndexValid(index)) {
            return false;
        }

        try {
            Float.valueOf(this.get(index));
            return true;
        } catch (Exception ignored) {
        }

        return false;
    }

    public boolean isDouble(final int index) {
        if (!this.isIndexValid(index)) {
            return false;
        }

        try {
            Double.valueOf(this.get(index));
            return true;
        } catch (Exception ignored) {
        }

        return false;
    }


    public String getString(final int index, final String standart) {
        if (!this.isIndexValid(index) || !this.isString(index)) {
            return standart;
        }

        return this.get(index);
    }

    public boolean getBoolean(final int index, final boolean standart) {
        if (!this.isIndexValid(index) || !this.isBoolean(index)) {
            return standart;
        }

        return Boolean.parseBoolean(this.getString(index));
    }

    public char getChar(final int index, final char standart) {
        if (!this.isIndexValid(index) || !this.isChar(index)) {
            return standart;
        }

        return this.getString(index, String.valueOf(standart)).charAt(0);
    }

    public short getShort(final int index, final short standart) {
        if (!this.isIndexValid(index) || !this.isShort(index)) {
            return standart;
        }

        return Short.parseShort(this.get(index));
    }

    public int getInteger(final int index, final int standart) {
        if (!this.isIndexValid(index) || !this.isInteger(index)) {
            return standart;
        }

        return Integer.parseInt(this.get(index));
    }

    public long getLong(final int index, final long standart) {
        if (!this.isIndexValid(index) || !this.isLong(index)) {
            return standart;
        }

        return Long.parseLong(this.get(index));
    }

    public float getFloat(final int index, final float standart) {
        if (!this.isIndexValid(index) || !this.isFloat(index)) {
            return standart;
        }

        return Float.parseFloat(this.get(index));
    }

    public double getDouble(final int index, final double standart) {
        if (!this.isIndexValid(index) || !this.isDouble(index)) {
            return standart;
        }

        return Double.parseDouble(this.get(index));
    }


    public String getString(final int index) {
        return this.getString(index, "");
    }

    public boolean getBoolean(final int index) {
        return this.getBoolean(index, false);
    }

    public char getChar(final int index) {
        return this.getChar(index, "A".toCharArray()[0]);
    }

    public short getShort(final int index) {
        return this.getShort(index, (short) 0);
    }

    public int getInteger(final int index) {
        return this.getInteger(index, 0);
    }

    public long getLong(final int index) {
        return this.getLong(index, 0);
    }

    public float getFloat(final int index) {
        return this.getFloat(index, 0);
    }

    public double getDouble(final int index) {
        return this.getDouble(index, 0);
    }

    public ArrayHelper add(final String object, final String... objects) {
        this.array = this.advance(object, objects);
        return this;
    }


    public String[] advance(final String obToAdd, final String... obs) {
        String[] newArray = new String[this.getLength() + 1 + obs.length];

        int i = 0;
        for (String ob : this.array) {
            Array.set(newArray, i, ob);

            i++;
        }
        Array.set(newArray, i, obToAdd);
        i++;
        for (String ob : obs) {
            Array.set(newArray, i, ob);
            i++;
        }

        return newArray;
    }

    public String[] advanceToStrings(final String strToAdd, final String... strs) {
        String[] newArray = new String[this.getLength() + 1 + strs.length];

        int i = 0;
        for (Object ob : this.array) {
            newArray[i] = ob.toString();

            i++;
        }
        newArray[i] = strToAdd;
        i++;
        for (String str : strs) {
            newArray[i] = str;
            i++;
        }

        return newArray;
    }

    public String[] getAsArray() {
        return this.array;
    }

    public String getAsString() {
        return this.getAsString(0, " ");
    }

    public String getAsString(final String combiner) {
        return this.getAsString(0, combiner);
    }

    public String getAsString(final int start) {
        return this.getAsString(start, " ");
    }

    public String getAsString(final int start, final String combiner) {
        return this.getAsString(start, this.getLength() - 1, combiner);
    }

    public String getAsString(final int start, final int end) {
        return this.getAsString(start, end, " ");
    }

    public String getAsString(int start, int end, final String combiner) {
        if (start < 0) {
            start = 0;
        }
        if (end > this.getLength() - 1) {
            end = this.getLength() - 1;
        }
        if (end < start) {
            return "";
        }

        String out = "";
        for (int i = start; i <= end; i++) {
            if (out.isEmpty()) {
                out = this.getString(i);
            } else {
                out += combiner + this.getString(i);
            }
        }
        return out;
    }


    @Override
    public String toString() {
        String complete = "";
        for (String t : this.array) {
            complete += (complete.isEmpty() ? "" : ", ") + t;
        }
        return "[" + complete + "]";
    }

}
