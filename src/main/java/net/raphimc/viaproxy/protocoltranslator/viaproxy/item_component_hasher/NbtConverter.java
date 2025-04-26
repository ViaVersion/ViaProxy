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
package net.raphimc.viaproxy.protocoltranslator.viaproxy.item_component_hasher;

import com.viaversion.nbt.tag.*;
import net.lenni0451.mcstructs.nbt.NbtTag;
import net.lenni0451.mcstructs.nbt.utils.NbtCodecUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class NbtConverter {

    public static Tag mcStructsToVia(final NbtTag tag) {
        if (tag == null) return null;
        return switch (tag.getNbtType()) {
            case END -> throw new UnsupportedOperationException();
            case BYTE -> new ByteTag(tag.asByteTag().getValue());
            case SHORT -> new ShortTag(tag.asShortTag().getValue());
            case INT -> new IntTag(tag.asIntTag().getValue());
            case LONG -> new LongTag(tag.asLongTag().getValue());
            case FLOAT -> new FloatTag(tag.asFloatTag().getValue());
            case DOUBLE -> new DoubleTag(tag.asDoubleTag().getValue());
            case BYTE_ARRAY -> new ByteArrayTag(tag.asByteArrayTag().getValue());
            case STRING -> new StringTag(tag.asStringTag().getValue());
            case LIST -> {
                final ListTag<? super Tag> listTag = new ListTag<>(Collections.emptyList());
                for (NbtTag subTag : tag.asListTag()) {
                    listTag.add(mcStructsToVia(subTag));
                }
                yield listTag;
            }
            case COMPOUND -> {
                final CompoundTag compoundTag = new CompoundTag();
                for (Map.Entry<String, NbtTag> entry : tag.asCompoundTag()) {
                    compoundTag.put(entry.getKey(), mcStructsToVia(entry.getValue()));
                }
                yield compoundTag;
            }
            case INT_ARRAY -> new IntArrayTag(tag.asIntArrayTag().getValue());
            case LONG_ARRAY -> new LongArrayTag(tag.asLongArrayTag().getValue());
        };
    }

    public static NbtTag viaToMcStructs(final Tag tag) {
        if (tag == null) return null;
        if (tag instanceof ByteTag byteTag) {
            return new net.lenni0451.mcstructs.nbt.tags.ByteTag(byteTag.asByte());
        } else if (tag instanceof ShortTag shortTag) {
            return new net.lenni0451.mcstructs.nbt.tags.ShortTag(shortTag.asShort());
        } else if (tag instanceof IntTag intTag) {
            return new net.lenni0451.mcstructs.nbt.tags.IntTag(intTag.asInt());
        } else if (tag instanceof LongTag longTag) {
            return new net.lenni0451.mcstructs.nbt.tags.LongTag(longTag.asLong());
        } else if (tag instanceof FloatTag floatTag) {
            return new net.lenni0451.mcstructs.nbt.tags.FloatTag(floatTag.asFloat());
        } else if (tag instanceof DoubleTag doubleTag) {
            return new net.lenni0451.mcstructs.nbt.tags.DoubleTag(doubleTag.asDouble());
        } else if (tag instanceof ByteArrayTag byteArrayTag) {
            return new net.lenni0451.mcstructs.nbt.tags.ByteArrayTag(byteArrayTag.getValue());
        } else if (tag instanceof StringTag stringTag) {
            return new net.lenni0451.mcstructs.nbt.tags.StringTag(stringTag.getValue());
        } else if (tag instanceof MixedListTag mixedListTag) { // VV moment
            final List<NbtTag> tags = new ArrayList<>();
            for (Tag subTag : mixedListTag) {
                tags.add(viaToMcStructs(subTag));
            }
            return NbtCodecUtils.wrapMarkers(tags);
        } else if (tag instanceof ListTag<?> listTag) {
            final net.lenni0451.mcstructs.nbt.tags.ListTag<NbtTag> mcListTag = new net.lenni0451.mcstructs.nbt.tags.ListTag<>();
            for (Tag subTag : listTag) {
                mcListTag.add(viaToMcStructs(subTag));
            }
            return mcListTag;
        } else if (tag instanceof CompoundTag compoundTag) {
            final net.lenni0451.mcstructs.nbt.tags.CompoundTag mcCompoundTag = new net.lenni0451.mcstructs.nbt.tags.CompoundTag();
            for (Map.Entry<String, Tag> entry : compoundTag.entrySet()) {
                mcCompoundTag.add(entry.getKey(), viaToMcStructs(entry.getValue()));
            }
            return mcCompoundTag;
        } else if (tag instanceof IntArrayTag intArrayTag) {
            return new net.lenni0451.mcstructs.nbt.tags.IntArrayTag(intArrayTag.getValue());
        } else if (tag instanceof LongArrayTag longArrayTag) {
            return new net.lenni0451.mcstructs.nbt.tags.LongArrayTag(longArrayTag.getValue());
        } else {
            throw new UnsupportedOperationException("Unknown tag type: " + tag.getClass().getName());
        }
    }

}
