/*
 * Copyright (C) 2022 Pokemod Cobbled Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cablemc.pokemod.common.util.adapters

import com.cablemc.pokemod.common.Pokemod
import com.cablemc.pokemod.common.api.spawning.SpawnBucket
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import java.lang.reflect.Type

/**
 * Spawn bucket adapter that searches the config for a matching named element to deserialize. The serialized form
 * is simply the name of the bucket.
 *
 * @author Hiroku
 * @since June 20th, 2022
 */
object SpawnBucketAdapter : JsonDeserializer<SpawnBucket>, JsonSerializer<SpawnBucket> {
    override fun serialize(bucket: SpawnBucket, type: Type, ctx: JsonSerializationContext) = JsonPrimitive(bucket.name)
    override fun deserialize(json: JsonElement, type: Type, ctx: JsonDeserializationContext): SpawnBucket {
        return Pokemod.bestSpawner.config.buckets.find { it.name == json.asString }
            ?: throw IllegalStateException("Spawn referred to invalid spawn bucket: ${json.asString}. Is it missing from the config?")
    }
}