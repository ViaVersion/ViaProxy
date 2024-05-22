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
package net.raphimc.viaproxy.proxy.packethandler;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import net.raphimc.netminecraft.constants.ConnectionState;
import net.raphimc.netminecraft.constants.MCPackets;
import net.raphimc.netminecraft.packet.IPacket;
import net.raphimc.netminecraft.packet.PacketTypes;
import net.raphimc.netminecraft.packet.UnknownPacket;
import net.raphimc.netminecraft.packet.impl.common.S2CTransferPacket;
import net.raphimc.viaproxy.ViaProxy;
import net.raphimc.viaproxy.proxy.session.ProxyConnection;
import net.raphimc.viaproxy.proxy.util.TransferDataHolder;
import net.raphimc.viaproxy.util.logging.Logger;

import java.net.InetSocketAddress;
import java.util.List;

public class TransferPacketHandler extends PacketHandler {

    private final int transferId;

    public TransferPacketHandler(ProxyConnection proxyConnection) {
        super(proxyConnection);

        this.transferId = MCPackets.S2C_TRANSFER.getId(this.proxyConnection.getClientVersion().getVersion());
    }

    @Override
    public boolean handleP2S(IPacket packet, List<ChannelFutureListener> listeners) {
        if (packet instanceof UnknownPacket unknownPacket && this.proxyConnection.getP2sConnectionState() == ConnectionState.PLAY) {
            if (unknownPacket.packetId == this.transferId) {
                final S2CTransferPacket transfer = new S2CTransferPacket();
                transfer.read(Unpooled.wrappedBuffer(unknownPacket.data));
                this.handleTransfer(transfer);

                final ByteBuf transferToViaProxy = Unpooled.buffer();
                PacketTypes.writeVarInt(transferToViaProxy, this.transferId);
                this.createTransferPacket().write(transferToViaProxy);
                this.proxyConnection.getC2P().writeAndFlush(transferToViaProxy).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
                return false;
            }
        } else if (packet instanceof S2CTransferPacket transfer) {
            this.handleTransfer(transfer);
            this.proxyConnection.getC2P().writeAndFlush(this.createTransferPacket()).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
            return false;
        }

        return true;
    }

    private void handleTransfer(final S2CTransferPacket transfer) {
        final InetSocketAddress newAddress = new InetSocketAddress(transfer.host, transfer.port);
        TransferDataHolder.addTempRedirect(this.proxyConnection.getC2P(), newAddress);
    }

    private S2CTransferPacket createTransferPacket() {
        if (this.proxyConnection.getClientHandshakeAddress() != null) {
            return new S2CTransferPacket(this.proxyConnection.getClientHandshakeAddress().getHost(), this.proxyConnection.getClientHandshakeAddress().getPort());
        } else {
            Logger.u_warn("transfer", this.proxyConnection, "Client handshake address is invalid, using ViaProxy bind address instead");
            if (!(ViaProxy.getCurrentProxyServer().getChannel().localAddress() instanceof InetSocketAddress bindAddress)) {
                throw new IllegalArgumentException("ViaProxy bind address must be an InetSocketAddress");
            }
            if (!(this.proxyConnection.getC2P().localAddress() instanceof InetSocketAddress clientAddress)) {
                throw new IllegalArgumentException("Client address must be an InetSocketAddress");
            }
            return new S2CTransferPacket(clientAddress.getHostString(), bindAddress.getPort());
        }
    }

}
