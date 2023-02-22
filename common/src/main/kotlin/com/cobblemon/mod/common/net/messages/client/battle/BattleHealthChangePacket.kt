/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.net.messages.client.battle

import com.cobblemon.mod.common.api.net.NetworkPacket
import com.cobblemon.mod.common.util.cobblemonResource
import net.minecraft.network.PacketByteBuf

/**
 * Informs the client that a Pokémon's health has changed. Executes a tile animation.
 *
 * Handled by [com.cobblemon.mod.common.client.net.battle.BattleHealthChangeHandler].
 *
 * @author Hiroku
 * @since June 5th, 2022
 */
class BattleHealthChangePacket(val pnx: String, val newHealthRatio: Float, val newHealth: Int) : NetworkPacket<BattleHealthChangePacket> {
    override val id = ID
    override fun encode(buffer: PacketByteBuf) {
        buffer.writeString(pnx)
        buffer.writeFloat(newHealthRatio)
        buffer.writeInt(newHealth)
    }

    companion object {
        val ID = cobblemonResource("battle_health_change")
        fun decode(buffer: PacketByteBuf) = BattleHealthChangePacket(buffer.readString(), buffer.readFloat(), buffer.readInt())
    }
}