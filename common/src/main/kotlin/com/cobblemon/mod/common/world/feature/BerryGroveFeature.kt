/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.world.feature

import com.cobblemon.mod.common.CobblemonBlocks
import com.cobblemon.mod.common.api.berry.BerryHelper
import com.cobblemon.mod.common.api.tags.CobblemonBlockTags
import com.cobblemon.mod.common.block.BerryBlock
import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.google.common.cache.LoadingCache
import net.minecraft.registry.entry.RegistryEntry
import net.minecraft.registry.tag.BlockTags
import net.minecraft.util.math.Vec3i
import net.minecraft.util.math.intprovider.ClampedNormalIntProvider
import net.minecraft.world.StructureWorldAccess
import net.minecraft.world.biome.Biome
import net.minecraft.world.chunk.ChunkStatus
import net.minecraft.world.gen.blockpredicate.BlockPredicate
import net.minecraft.world.gen.feature.*
import net.minecraft.world.gen.feature.util.FeatureContext
import net.minecraft.world.gen.placementmodifier.BlockFilterPlacementModifier
import net.minecraft.world.gen.stateprovider.BlockStateProvider
import net.minecraft.world.gen.stateprovider.RandomizedIntBlockStateProvider
import java.util.*

class BerryGroveFeature : Feature<SingleStateFeatureConfig>(SingleStateFeatureConfig.CODEC){
    override fun generate(context: FeatureContext<SingleStateFeatureConfig>): Boolean {
        val worldGenLevel: StructureWorldAccess = context.world!!
        val random = context.random!!
        val origin = context.origin!!

        val isGenerating = worldGenLevel.getChunk(origin).status != ChunkStatus.FULL

        if (!isGenerating) return false
        val biome = worldGenLevel.getBiome(origin)
        //This basically goes through and finds the berries whose preferred biome we are in
        //Maybe cache these per biome in a map?
        val validTrees = BerryHelper.getBerriesForBiome(biome)
        if (validTrees.isEmpty()) return false
        val pickedTree = validTrees.random()
        val berry = pickedTree.berry()!!
        val numTreesToGen = pickedTree.berry()?.spawnConditions?.sumOf { cond ->
            (cond.getGroveSize(random).takeIf { cond.canSpawn(berry, biome) } ) ?: 0
        } ?: 0
        var numTreesLeftToGen = numTreesToGen
        val defTreeState = BlockStateProvider.of(pickedTree.defaultState.with(BerryBlock.WAS_GENERATED, true))

        val randomTreeStateProvider = RandomizedIntBlockStateProvider(
            defTreeState, BerryBlock.AGE,
            ClampedNormalIntProvider.of(4f, 1f, 1, 5))
        val blockPlaceFeature = PlacedFeatures.createEntry(
            SIMPLE_BLOCK,
            SimpleBlockFeatureConfig(
                randomTreeStateProvider
            ),
            BlockFilterPlacementModifier.of(BlockPredicate.matchingBlockTag(BlockTags.REPLACEABLE)),
            BlockFilterPlacementModifier.of(BlockPredicate.anyOf(
                BlockPredicate.matchingBlockTag(Vec3i(0, -1, 0), BlockTags.SAND),
                BlockPredicate.matchingBlockTag(Vec3i(0, -1, 0), BlockTags.DIRT)
            )),
        ).value()
        val possiblePositions = listOf(
            origin.north(),
            origin.north().east(),
            origin.east(),
            origin.south().east(),
            origin.south(),
            origin.south().west(),
            origin.west(),
            origin.north().west(),
            origin.up().north(),
            origin.up().north().east(),
            origin.up().east(),
            origin.up().south().east(),
            origin.up().south(),
            origin.up().south().west(),
            origin.up().west(),
            origin.up().north().west(),
            origin.down().north(),
            origin.down().north().east(),
            origin.down().east(),
            origin.down().south().east(),
            origin.down().south(),
            origin.down().south().west(),
            origin.down().west(),
            origin.down().north().west(),
        ).shuffled()
        for (dir in possiblePositions) {
            if (numTreesLeftToGen > 0) {
                if (blockPlaceFeature?.generate(worldGenLevel, context.generator, random, dir) == true) {
                    numTreesLeftToGen--
                }
            }
        }
        return numTreesToGen != numTreesLeftToGen
    }

}