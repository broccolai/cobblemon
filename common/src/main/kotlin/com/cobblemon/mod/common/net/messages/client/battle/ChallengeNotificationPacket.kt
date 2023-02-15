/*
 * Copyright (C) 2022 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.net.messages.client.battle

import com.cobblemon.mod.common.api.net.NetworkPacket
import com.cobblemon.mod.common.util.cobblemonResource
import net.minecraft.network.PacketByteBuf
import net.minecraft.text.MutableText

/**
 * Packet send when a player has challenged to battle. The responsibility
 * of this packet currently is to send a battle challenge message that includes
 * the keybind to challenge them back. In future this is likely to include information
 * about the battle.
 *
 * Handled by [com.cobblemon.mod.common.client.net.battle.ChallengeNotificationHandler].
 *
 * @author Hiroku
 * @since August 5th, 2022
 */
class ChallengeNotificationPacket(val challengerName: MutableText): NetworkPacket<ChallengeNotificationPacket> {
    override val id = ID
    override fun encode(buffer: PacketByteBuf) {
        buffer.writeText(challengerName)
    }
    companion object {
        val ID = cobblemonResource("challenge_notification")
        fun decode(buffer: PacketByteBuf) = ChallengeNotificationPacket(buffer.readText().copy())
    }
}