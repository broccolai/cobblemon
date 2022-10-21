/*
 * Copyright (C) 2022 Pokemod Cobbled Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cablemc.pokemod.common.api.storage.factory

import com.cablemc.pokemod.common.api.storage.PokemonStore
import com.cablemc.pokemod.common.api.storage.PokemonStoreManager
import com.cablemc.pokemod.common.api.storage.StorePosition
import com.cablemc.pokemod.common.api.storage.party.PlayerPartyStore
import com.cablemc.pokemod.common.api.storage.pc.PCStore
import com.cablemc.pokemod.common.world.block.entity.PCBlockEntity
import java.util.UUID
import net.minecraft.server.network.ServerPlayerEntity

/**
 * A provider for [PokemonStore]'s when given a player UUID. An implementation of this interface
 * does not need to always provide a value. In some cases you may only want to provide a store for
 * specific players, or under specific conditions, or only for parties, etc.
 *
 * If you have an implementation of this interface and want it to be used by Pokémon Cobbled, register
 * the factory using [PokemonStoreManager.registerFactory].
 *
 * @author Hiroku
 * @since November 29th, 2021
 */
interface PokemonStoreFactory {
    fun getPlayerParty(playerID: UUID): PlayerPartyStore?
    fun getPC(playerID: UUID): PCStore?
    fun getPCForPlayer(player: ServerPlayerEntity, pcBlockEntity: PCBlockEntity): PCStore? = getPC(player.uuid)

    fun <E : StorePosition, T : PokemonStore<E>> getCustomStore(storeClass: Class<T>, uuid: UUID): T?
    fun shutdown()
}