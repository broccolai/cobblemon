/*
 * Copyright (C) 2022 Pokemod Cobbled Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cablemc.pokemod.common.api.pokemon.evolution.adapters

import com.cablemc.pokemod.common.api.pokemon.evolution.Evolution
import com.cablemc.pokemod.common.pokemon.evolution.adapters.CobbledEvolutionAdapter
import com.google.gson.JsonDeserializer
import com.google.gson.JsonSerializer
import kotlin.reflect.KClass

/**
 * Saves and loads [Evolution]s with JSON.
 * For the default implementation see [CobbledEvolutionAdapter].
 *
 * @author Licious
 * @since March 20th, 2022
 */
interface EvolutionAdapter : JsonDeserializer<Evolution>, JsonSerializer<Evolution> {

    /**
     * Registers the given type of [Evolution] to it's associated ID for deserialization.
     *
     * @param T The type of [Evolution].
     * @param id The id of the evolution event.
     * @param type The [KClass] of the type.
     */
    fun <T : Evolution> registerType(id: String, type: KClass<T>)

}