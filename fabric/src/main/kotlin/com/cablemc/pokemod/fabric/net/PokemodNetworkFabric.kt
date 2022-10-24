/*
 * Copyright (C) 2022 Pokemod Cobbled Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cablemc.pokemod.fabric.net

import com.cablemc.pokemod.common.NetworkDelegate
import com.cablemc.pokemod.common.PokemodNetwork
import com.cablemc.pokemod.common.api.net.NetworkPacket
import com.cablemc.pokemod.common.net.PacketHandler
import com.cablemc.pokemod.common.util.pokemodResource
import io.netty.buffer.Unpooled
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.network.PacketByteBuf
import net.minecraft.server.network.ServerPlayerEntity

abstract class PreparedFabricMessage<T : NetworkPacket>(protected val registeredMessage: RegisteredMessage<T>) : PokemodNetwork.PreparedMessage<T> {
    override fun registerMessage() {}
}
class PreparedClientBoundFabricMessage<T : NetworkPacket>(registeredMessage: RegisteredMessage<T>) : PreparedFabricMessage<T>(registeredMessage) {
    override fun registerHandler(handler: PacketHandler<T>) {
        ClientPlayNetworking.registerGlobalReceiver(
            registeredMessage.identifier
        ) { _, _, buf, _ ->
            val packet = registeredMessage.packetClass.getDeclaredConstructor().newInstance().also { it.decode(buf) }
            val context = FabricClientNetworkContext()
            handler(packet, context)
        }
    }
}
class PreparedServerBoundFabricMessage<T : NetworkPacket>(registeredMessage: RegisteredMessage<T>) : PreparedFabricMessage<T>(registeredMessage) {
    override fun registerHandler(handler: PacketHandler<T>) {
        ServerPlayNetworking.registerGlobalReceiver(
            registeredMessage.identifier
        ) { _, player, _, buf, _ ->
            val packet = registeredMessage.packetClass.getDeclaredConstructor().newInstance().also { it.decode(buf) }
            val context = FabricServerNetworkContext(player)
            handler(packet, context)
        }
    }
}
class FabricServerNetworkContext(override val player: ServerPlayerEntity) : PokemodNetwork.NetworkContext
class FabricClientNetworkContext(override val player: ServerPlayerEntity? = null) : PokemodNetwork.NetworkContext
class RegisteredMessage<T : NetworkPacket>(
    val packetClass: Class<T>
) {
    val identifier = pokemodResource(packetClass.simpleName.lowercase())
    fun encode(packet: NetworkPacket): PacketByteBuf? {
        if (packetClass.isInstance(packet)) {
            return PacketByteBuf(Unpooled.buffer()).also { packet.encode(it) }
        }
        return null
    }
}

object PokemodFabricNetworkDelegate : NetworkDelegate {
    val registeredMessages = mutableListOf<RegisteredMessage<*>>()

    override fun sendPacketToPlayer(player: ServerPlayerEntity, packet: NetworkPacket) {
        val registeredMessage = registeredMessages.find { it.packetClass.isInstance(packet) }
            ?: throw IllegalStateException("There was no registered message for packet of type ${packet::class.simpleName}!")
        ServerPlayNetworking.send(player, registeredMessage.identifier, registeredMessage.encode(packet))
    }

    override fun sendPacketToServer(packet: NetworkPacket) {
        val registeredMessage = registeredMessages.find { it.packetClass.isInstance(packet) }
            ?: throw IllegalStateException("There was no registered message for packet of type ${packet::class.simpleName}!")
        ClientPlayNetworking.send(registeredMessage.identifier, registeredMessage.encode(packet))
    }

    override fun <T : NetworkPacket> buildMessage(
        packetClass: Class<T>,
        toServer: Boolean
    ): PokemodNetwork.PreparedMessage<T> {
        val registeredMessage = RegisteredMessage(packetClass)
        registeredMessages.add(registeredMessage)

        return if (toServer) {
            PreparedServerBoundFabricMessage(registeredMessage)
        } else {
            PreparedClientBoundFabricMessage(registeredMessage)
        }
    }
}