/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.pokemon.ai

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity
import com.cobblemon.mod.common.util.canFit
import com.google.common.collect.Maps
import it.unimi.dsi.fastutil.longs.Long2ObjectFunction
import it.unimi.dsi.fastutil.longs.Long2ObjectMap
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap
import java.util.EnumSet
import net.minecraft.block.AbstractRailBlock
import net.minecraft.block.Blocks
import net.minecraft.block.FenceGateBlock
import net.minecraft.entity.ai.pathing.LandPathNodeMaker
import net.minecraft.entity.ai.pathing.NavigationType
import net.minecraft.entity.ai.pathing.PathNode
import net.minecraft.entity.ai.pathing.PathNodeMaker
import net.minecraft.entity.ai.pathing.PathNodeType
import net.minecraft.entity.ai.pathing.TargetPathNode
import net.minecraft.entity.mob.MobEntity
import net.minecraft.fluid.FluidState
import net.minecraft.registry.tag.BlockTags
import net.minecraft.registry.tag.FluidTags
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.util.math.MathHelper
import net.minecraft.util.math.Vec3d
import net.minecraft.world.BlockView
import net.minecraft.world.chunk.ChunkCache

/**
 * A path node maker that constructs paths knowing that the entity might be capable of
 * traveling across land, water, and air. This most closely resembles the aquatic
 * node maker.
 *
 * @author Hiroku
 * @since September 10th, 2022
 */
class OmniPathNodeMaker : PathNodeMaker() {
    private val nodePosToType: Long2ObjectMap<PathNodeType> = Long2ObjectOpenHashMap()

    var canPathThroughFire: Boolean = false

    override fun init(cachedWorld: ChunkCache, entity: MobEntity) {
        super.init(cachedWorld, entity)
        nodePosToType.clear()
    }

    override fun clear() {
        super.clear()
        nodePosToType.clear()
    }

    override fun getNode(x: Double, y: Double, z: Double): TargetPathNode? {
        return asTargetPathNode(super.getNode(MathHelper.floor(x), MathHelper.floor(y + 0.5), MathHelper.floor(z)))
    }

    override fun getStart(): PathNode? {
        val x = MathHelper.floor(entity.boundingBox.minX)
        val y = MathHelper.floor(entity.boundingBox.minY + 0.5)
        val z = MathHelper.floor(entity.boundingBox.minZ)
        val node = super.getNode(x, y, z)
        node.type = this.getNodeType(entity, node.blockPos)
        node.penalty = entity.getPathfindingPenalty(node.type)
        return node
    }

    fun getNodeType(entity: MobEntity, pos: BlockPos): PathNodeType? {
        return this.getNodeType(entity, pos.x, pos.y, pos.z)
    }

    fun getNodeType(entity: MobEntity, x: Int, y: Int, z: Int): PathNodeType? {
        return this.nodePosToType.computeIfAbsent(BlockPos.asLong(x, y, z),
            Long2ObjectFunction<PathNodeType?> {
                this.getNodeType(
                    cachedWorld, x, y, z,
                    entity
                )
            })
    }
    
    override fun getSuccessors(successors: Array<PathNode?>, node: PathNode): Int {
        var i = 0
        val map = Maps.newEnumMap<Direction, PathNode?>(Direction::class.java)
        val upperMap = Maps.newEnumMap<Direction, PathNode?>(Direction::class.java)
        val lowerMap = Maps.newEnumMap<Direction, PathNode?>(Direction::class.java)

        val upIsOpen = entity.canFit(node.blockPos.up())

        // Non-diagonal surroundings in 3d space
        for (direction in Direction.values()) {
            val pathNode = this.getNode(node.x + direction.offsetX, node.y + direction.offsetY, node.z + direction.offsetZ) ?: continue
            map[direction] = pathNode
            if (!hasNotVisited(pathNode)) {
                continue
            }
            successors[i++] = pathNode
        }

        // Diagonals
        for (direction in Direction.Type.HORIZONTAL.iterator()) {
            val direction2 = direction.rotateYClockwise()
            val x = node.x + direction.offsetX + direction2.offsetX
            val z = node.z + direction.offsetZ + direction2.offsetZ
            val pathNode2 = this.getNode(x, node.y, z) ?: continue
            // Skip 'inaccessible' diagonals if we're pathing from a blocked node since we're trying to get unstuck
            if (isAccessibleDiagonal(pathNode2, map[direction], map[direction2]) || (node.type == PathNodeType.BLOCKED && !pathNode2.visited)) {
                successors[i++] = pathNode2
            }
        }

        // Upward non-diagonals
        for (direction in Direction.Type.HORIZONTAL.iterator()) {
            val pathNode2 = getNode(node.x + direction.offsetX, node.y + 1, node.z + direction.offsetZ) ?: continue
            if (upIsOpen && hasNotVisited(pathNode2)) {
                successors[i++] = pathNode2
                upperMap[direction] = pathNode2
            }
        }

        // Upward diagonals
        for (direction in Direction.Type.HORIZONTAL.iterator()) {
            val direction2 = direction.rotateYClockwise()
            val pathNode2 = getNode(node.x + direction.offsetX + direction2.offsetX, node.y + 1, node.z + direction.offsetZ + direction2.offsetZ) ?: continue
            if (isAccessibleDiagonal(pathNode2, upperMap[direction], upperMap[direction2])) {
                successors[i++] = pathNode2
            }
        }

        val connectingBlockPos = BlockPos.Mutable()
        // Downward non-diagonals
        for (direction in Direction.Type.HORIZONTAL.iterator()) {
            connectingBlockPos.set(node.blockPos.add(direction.vector))
            val blockState = cachedWorld.getBlockState(connectingBlockPos)
            val traversableByTangent = blockState.canPathfindThrough(cachedWorld, connectingBlockPos, NavigationType.AIR)
            val pathNode2 = getNode(node.x + direction.offsetX, node.y - 1, node.z + direction.offsetZ) ?: continue
            if (hasNotVisited(pathNode2) && traversableByTangent) {
                successors[i++] = pathNode2
                lowerMap[direction] = pathNode2
            }
        }

        // Downward diagonals
        for (direction in Direction.Type.HORIZONTAL.iterator()) {
            val direction2 = direction.rotateYClockwise()
            val pathNode2 = getNode(node.x + direction.offsetX + direction2.offsetX, node.y - 1, node.z + direction.offsetZ + direction2.offsetZ) ?: continue
            if (isAccessibleDiagonal(pathNode2, lowerMap[direction], lowerMap[direction2])) {
                successors[i++] = pathNode2
            }
        }

        // If they're in a blocked node and there are multiple successors, choose whichever is closest to get out of the blocked position.
        // This addresses instances where they're next to a fence and should move away from the fence in the nearest open
        // direction before regular pathing.
        if (entity.getPathfindingPenalty(node.type) < 0 && i > 1) {
            val x = entity.boundingBox.minX
            val y = entity.boundingBox.minY + 0.5
            val z = entity.boundingBox.minZ
            val pos = Vec3d(x, y, z)

            var n = 1
            var closestSuccessor = successors[0]!!
            var closestDistance = closestSuccessor.pos.add(0.5, 0.0, 0.5).distanceTo(pos)

            while (n < i) {
                val next = successors[n]!!
                val nextDist = next.pos.add(0.5, 0.0, 0.5).distanceTo(pos)
                if (nextDist < closestDistance) {
                    closestSuccessor = next
                    closestDistance = nextDist
                }

                n++
            }

            successors[0] = closestSuccessor
            i = 1
        }

        return i
    }

    fun hasNotVisited(pathNode: PathNode?): Boolean {
        return pathNode != null && !pathNode.visited
    }

    fun isAccessibleDiagonal(pathNode: PathNode?, vararg borderNodes: PathNode?): Boolean {
        return hasNotVisited(pathNode) && borderNodes.all { it != null && it.penalty >= 0.0F }
    }

    fun isValidPathNodeType(pathNodeType: PathNodeType): Boolean {
        return when {
            (pathNodeType == PathNodeType.BREACH || pathNodeType == PathNodeType.WATER || pathNodeType == PathNodeType.WATER_BORDER) && canSwimInWater() -> true
            pathNodeType == PathNodeType.OPEN && canFly() -> true
            pathNodeType == PathNodeType.WALKABLE && (canWalk() || canFly()) -> true
            else -> false
        }
    }

    override fun getNode(x: Int, y: Int, z: Int): PathNode? {
        var nodePenalty = 0F
        var pathNode: PathNode? = null

        val pathNodeType = addPathNodePos(x, y, z)
        if (isValidPathNodeType(pathNodeType) &&
            entity.getPathfindingPenalty(pathNodeType).also { nodePenalty = it } >= 0.0f &&
            super.getNode(x, y, z).also { pathNode = it } != null
        ) {
            pathNode!!.type = pathNodeType
            pathNode!!.penalty = pathNode!!.penalty.coerceAtLeast(nodePenalty)
        }
        return pathNode
    }

    fun addPathNodePos(x: Int, y: Int, z: Int): PathNodeType {
        return nodePosToType.computeIfAbsent(BlockPos.asLong(x, y, z), Long2ObjectFunction { getNodeType(cachedWorld, x, y, z, entity) })
    }

    override fun getDefaultNodeType(world: BlockView, x: Int, y: Int, z: Int): PathNodeType {
        val pos = BlockPos(x, y, z)
        val below = BlockPos(x, y - 1, z)
        val blockState = world.getBlockState(pos)
        val blockStateBelow = world.getBlockState(below)
        val belowSolid = blockStateBelow.isSolidBlock(world, below)
        val isWater = blockState.fluidState.isIn(FluidTags.WATER)
        val isLava = blockState.fluidState.isIn(FluidTags.LAVA)
        val canBreatheUnderFluid = canSwimUnderFluid(blockState.fluidState)

        val figuredNode = if (blockStateBelow.isIn(BlockTags.FENCES) || blockStateBelow.isIn(BlockTags.WALLS) || (blockStateBelow.block is FenceGateBlock && !blockStateBelow.get(FenceGateBlock.OPEN))) {
            PathNodeType.FENCE
        } else if (isWater && belowSolid && !canSwimInWater() && canBreatheUnderFluid) {
            PathNodeType.WALKABLE
        } else if (isWater || (isLava && canSwimUnderlava())) {
            PathNodeType.WATER
        } else if (blockState.canPathfindThrough(world, pos, NavigationType.LAND) && !blockStateBelow.canPathfindThrough(world, below, NavigationType.AIR)) {
            PathNodeType.WALKABLE
        } else if (blockState.canPathfindThrough(world, pos, NavigationType.AIR) && blockStateBelow.canPathfindThrough(world, below, NavigationType.AIR)) {
            PathNodeType.OPEN
        } else PathNodeType.BLOCKED

        return adjustNodeType(world, canOpenDoors, canEnterOpenDoors, below, figuredNode)
    }

    override fun getNodeType(world: BlockView, x: Int, y: Int, z: Int, mob: MobEntity): PathNodeType? {
        val set = EnumSet.noneOf(PathNodeType::class.java)
        val sizeX = (mob.boundingBox.maxX - mob.boundingBox.minX).toInt() + 1
        val sizeY = (mob.boundingBox.maxY - mob.boundingBox.minY).toInt() + 1
        val sizeZ = (mob.boundingBox.maxZ - mob.boundingBox.minZ).toInt() + 1
        val type = findNearbyNodeTypes(world, x, y, z, sizeX, sizeY, sizeZ, canOpenDoors, canEnterOpenDoors, set, PathNodeType.BLOCKED, BlockPos(x, y, z))

        if (PathNodeType.DAMAGE_CAUTIOUS in set) {
            return PathNodeType.DAMAGE_CAUTIOUS
        } else if (PathNodeType.DANGER_OTHER in set) {
            return PathNodeType.DANGER_OTHER
        }
        return if (PathNodeType.FENCE in set) {
            PathNodeType.FENCE
        } else if (PathNodeType.UNPASSABLE_RAIL in set) {
            PathNodeType.UNPASSABLE_RAIL
        } else if (PathNodeType.DAMAGE_OTHER in set) {
            PathNodeType.DAMAGE_OTHER
        } else {
            var pathNodeType2: PathNodeType? = PathNodeType.BLOCKED
            val nearbyTypeIterator = set.iterator()
            while (nearbyTypeIterator.hasNext()) {
                val nearbyType = nearbyTypeIterator.next()
                if (mob.getPathfindingPenalty(nearbyType) < 0) {
                    return nearbyType
                }
                // The || is because we prefer WALKABLE where possible - OPEN is legit but if there's either OPEN or WALKABLE then WALKABLE is better since land pokes can read that.
                if (mob.getPathfindingPenalty(nearbyType) > mob.getPathfindingPenalty(pathNodeType2) || (nearbyType == PathNodeType.WALKABLE)) {
                    pathNodeType2 = nearbyType
                } else if (type == PathNodeType.WATER && nearbyType == PathNodeType.WATER) {
                    pathNodeType2 = PathNodeType.WATER
                }
            }
            if (type == PathNodeType.OPEN && mob.getPathfindingPenalty(pathNodeType2) == 0.0f && sizeX <= 1) {
                PathNodeType.OPEN
            } else {
                pathNodeType2!!
            }
        }
    }

    fun findNearbyNodeTypes(
        world: BlockView,
        x: Int,
        y: Int,
        z: Int,
        sizeX: Int,
        sizeY: Int,
        sizeZ: Int,
        canOpenDoors: Boolean,
        canEnterOpenDoors: Boolean,
        nearbyTypes: EnumSet<PathNodeType>,
        type: PathNodeType,
        pos: BlockPos
    ): PathNodeType {
        var type = type
        for (i in 0 until sizeX) {
            for (j in 0 until sizeY) {
                for (k in 0 until sizeZ) {
                    val l = i + x
                    val m = j + y
                    val n = k + z
                    val pathNodeType = getDefaultNodeType(world, l, m, n)
                    if (i == 0 && j == 0 && k == 0) {
                        type = pathNodeType
                    }
                    nearbyTypes.add(pathNodeType)
                }
            }
        }
        return type
    }

    protected fun adjustNodeType(
        world: BlockView,
        canOpenDoors: Boolean,
        canEnterOpenDoors: Boolean,
        pos: BlockPos,
        type: PathNodeType
    ): PathNodeType {
        val blockState = world.getBlockState(pos)
        val block = blockState.block

        if (blockState.isOf(Blocks.CACTUS) || blockState.isOf(Blocks.SWEET_BERRY_BUSH)) {
            return PathNodeType.DANGER_OTHER
        }

        if (LandPathNodeMaker.inflictsFireDamage(blockState) && !this.canPathThroughFire) {
            return PathNodeType.DANGER_FIRE
        }

        if (world.getFluidState(pos).isIn(FluidTags.WATER)) {
            return PathNodeType.WATER_BORDER
        }

        if (blockState.isOf(Blocks.WITHER_ROSE) || blockState.isOf(Blocks.POINTED_DRIPSTONE)) {
            return PathNodeType.DAMAGE_CAUTIOUS
        }

        return if (type == PathNodeType.DOOR_WOOD_CLOSED && canOpenDoors && canEnterOpenDoors) {
            PathNodeType.WALKABLE_DOOR
        } else if (type == PathNodeType.DOOR_OPEN && !canEnterOpenDoors) {
            PathNodeType.BLOCKED
        } else if (type == PathNodeType.RAIL && block !is AbstractRailBlock && world.getBlockState(pos.down()).block !is AbstractRailBlock) {
            PathNodeType.UNPASSABLE_RAIL
        } else if (type == PathNodeType.LEAVES) {
            PathNodeType.BLOCKED
        } else type
    }

    fun canWalk(): Boolean {
        return if (this.entity is PokemonEntity) {
            (this.entity as PokemonEntity).behaviour.moving.walk.canWalk
        } else {
            true
        }
    }

     fun canSwimInWater(): Boolean {
         return if (this.entity is PokemonEntity) {
             (this.entity as PokemonEntity).behaviour.moving.swim.canSwimInWater
         } else {
             false
         }
     }

    fun canSwimUnderlava(): Boolean {
        return if (this.entity is PokemonEntity) {
            (this.entity as PokemonEntity).behaviour.moving.swim.canBreatheUnderlava
        } else {
            false
        }
    }

    fun canSwimUnderFluid(fluidState: FluidState): Boolean {
        return if (this.entity is PokemonEntity) {
            if (fluidState.isIn(FluidTags.LAVA)) {
                (this.entity as PokemonEntity).behaviour.moving.swim.canBreatheUnderlava
            } else if (fluidState.isIn(FluidTags.WATER)) {
                (this.entity as PokemonEntity).behaviour.moving.swim.canBreatheUnderwater
            } else {
                false
            }
        } else {
            false
        }
    }

    fun canFly(): Boolean {
        return if (this.entity is PokemonEntity) {
            (this.entity as PokemonEntity).behaviour.moving.fly.canFly
        } else {
            false
        }
    }
}