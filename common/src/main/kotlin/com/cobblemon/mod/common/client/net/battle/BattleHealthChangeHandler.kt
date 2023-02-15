/*
 * Copyright (C) 2022 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.client.net.battle

import com.cobblemon.mod.common.api.net.ClientNetworkPacketHandler
import com.cobblemon.mod.common.client.CobblemonClient
import com.cobblemon.mod.common.client.battle.animations.HealthChangeAnimation
import com.cobblemon.mod.common.net.messages.client.battle.BattleHealthChangePacket
import net.minecraft.client.MinecraftClient

object BattleHealthChangeHandler : ClientNetworkPacketHandler<BattleHealthChangePacket> {
    override fun handle(packet: BattleHealthChangePacket, client: MinecraftClient) {
        client.execute {
            val battle = CobblemonClient.battle ?: return@execute
            val (_, activePokemon) = battle.getPokemonFromPNX(packet.pnx)
            activePokemon.animations.add(
                HealthChangeAnimation(newHealthRatio = packet.newHealthRatio, newHealth = packet.newHealth)
            )
        }
    }
}