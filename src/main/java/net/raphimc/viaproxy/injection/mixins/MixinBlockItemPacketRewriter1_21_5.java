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

package net.raphimc.viaproxy.injection.mixins;

import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.RegistryEntry;
import com.viaversion.viaversion.api.minecraft.data.StructuredData;
import com.viaversion.viaversion.api.minecraft.data.StructuredDataKey;
import com.viaversion.viaversion.api.minecraft.item.HashedItem;
import com.viaversion.viaversion.api.minecraft.item.Item;
import com.viaversion.viaversion.api.minecraft.item.StructuredItem;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.api.type.types.version.Types1_21_5;
import com.viaversion.viaversion.libs.fastutil.ints.Int2IntMap;
import com.viaversion.viaversion.protocols.v1_20_5to1_21.packet.ClientboundConfigurationPackets1_21;
import com.viaversion.viaversion.protocols.v1_21_4to1_21_5.Protocol1_21_4To1_21_5;
import com.viaversion.viaversion.protocols.v1_21_4to1_21_5.packet.ServerboundPacket1_21_5;
import com.viaversion.viaversion.protocols.v1_21_4to1_21_5.rewriter.BlockItemPacketRewriter1_21_5;
import com.viaversion.viaversion.protocols.v1_21to1_21_2.packet.ClientboundPacket1_21_2;
import com.viaversion.viaversion.rewriter.StructuredItemRewriter;
import com.viaversion.viaversion.util.Key;
import net.lenni0451.mcstructs.core.Identifier;
import net.raphimc.viaproxy.protocoltranslator.viaproxy.item_component_hasher.ItemComponentHashStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Mixin(value = BlockItemPacketRewriter1_21_5.class, remap = false)
public abstract class MixinBlockItemPacketRewriter1_21_5 extends StructuredItemRewriter<ClientboundPacket1_21_2, ServerboundPacket1_21_5, Protocol1_21_4To1_21_5> {

    @Unique
    private static final boolean DEBUG = false;

    public MixinBlockItemPacketRewriter1_21_5() {
        super(null, null, null, null, null);
    }

    @Inject(method = "registerPackets", at = @At("RETURN"))
    private void appendPacketHandlers() {
        this.protocol.appendClientbound(ClientboundConfigurationPackets1_21.REGISTRY_DATA, wrapper -> {
            wrapper.resetReader();
            final String key = Key.namespaced(wrapper.passthrough(Types.STRING)); // key
            final RegistryEntry[] entries = wrapper.passthrough(Types.REGISTRY_ENTRY_ARRAY); // entries

            final ItemComponentHashStorage itemComponentHasher = wrapper.user().get(ItemComponentHashStorage.class);
            if (key.equals("minecraft:enchantment")) {
                final List<Identifier> identifiers = new ArrayList<>();
                for (RegistryEntry entry : entries) {
                    identifiers.add(Identifier.of(entry.key()));
                }
                itemComponentHasher.setEnchantmentRegistry(identifiers);
            }
        });
    }

    @Inject(method = "handleItemToClient", at = @At("RETURN"))
    private void trackItemHashes(UserConnection connection, Item item, CallbackInfoReturnable<Item> cir) {
        final ItemComponentHashStorage itemComponentHasher = connection.get(ItemComponentHashStorage.class);
        for (StructuredData<?> structuredData : item.dataContainer().data().values()) {
            itemComponentHasher.trackStructuredData(structuredData);
        }
    }

    @Inject(method = "convertHashedItemToStructuredItem", at = @At("RETURN"))
    private void addMoreDataToStructuredItem(UserConnection connection, HashedItem hashedItem, CallbackInfoReturnable<StructuredItem> cir) {
        final ItemComponentHashStorage itemComponentHasher = connection.get(ItemComponentHashStorage.class);
        final Map<StructuredDataKey<?>, StructuredData<?>> structuredDataMap = cir.getReturnValue().dataContainer().data();
        for (Int2IntMap.Entry hashEntry : hashedItem.dataHashesById().int2IntEntrySet()) {
            final StructuredData<?> structuredData = itemComponentHasher.getStructuredData(hashEntry.getIntKey(), hashEntry.getIntValue());
            if (structuredData != null) {
                structuredDataMap.put(structuredData.key(), structuredData);
            } else if (DEBUG) {
                this.protocol.getLogger().warning("Failed to find structured data for hash: " + hashEntry.getIntValue() + " with id: " + hashEntry.getIntKey());
            }
        }
        for (int dataId : hashedItem.removedDataIds()) {
            final StructuredDataKey<?> structuredDataKey = Types1_21_5.STRUCTURED_DATA.key(dataId);
            structuredDataMap.put(structuredDataKey, StructuredData.empty(structuredDataKey, dataId));
        }
    }

}
