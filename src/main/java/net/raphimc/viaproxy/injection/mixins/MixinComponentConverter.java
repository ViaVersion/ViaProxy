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
package net.raphimc.viaproxy.injection.mixins;

import com.viaversion.viaversion.libs.gson.JsonElement;
import com.viaversion.viaversion.libs.gson.JsonNull;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.Tag;
import com.viaversion.viaversion.protocols.protocol1_20_3to1_20_2.util.ComponentConverter;
import net.lenni0451.mcstructs.text.ATextComponent;
import net.lenni0451.mcstructs.text.serializer.TextComponentCodec;
import net.lenni0451.mcstructs.text.serializer.TextComponentSerializer;
import net.raphimc.vialegacy.api.util.converter.JsonConverter;
import net.raphimc.vialegacy.api.util.converter.NbtConverter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(ComponentConverter.class)
public abstract class MixinComponentConverter {

    @Overwrite
    public static JsonElement tagComponentToJson(final Tag tag) {
        final ATextComponent textComponent = TextComponentCodec.V1_20_3.deserializeNbtTree(NbtConverter.viaToMcStructs(tag));
        if (textComponent == null) return JsonNull.INSTANCE;

        return JsonConverter.gsonToVia(TextComponentSerializer.V1_19_4.serializeJson(textComponent));
    }

    @Overwrite
    public static Tag jsonComponentToTag(final JsonElement component) {
        final ATextComponent textComponent = TextComponentSerializer.V1_19_4.deserialize(JsonConverter.viaToGson(component));
        if (textComponent == null) return null;

        return NbtConverter.mcStructsToVia(TextComponentCodec.V1_20_3.serializeNbt(textComponent));
    }

}
