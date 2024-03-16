/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.entity.pokemon.ai

import com.cobblemon.mod.common.entity.pokemon.PokemonBehaviourFlag
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity
import com.cobblemon.mod.common.pokemon.ai.MoveBehaviour
import com.cobblemon.mod.common.pokemon.ai.OmniPathNodeMaker
import com.cobblemon.mod.common.util.getWaterAndLavaIn
import com.cobblemon.mod.common.util.toVec3d
import com.google.common.collect.ImmutableSet
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.ceil
import net.minecraft.entity.Entity
import net.minecraft.entity.ai.pathing.MobNavigation
import net.minecraft.entity.ai.pathing.NavigationType
import net.minecraft.entity.ai.pathing.Path
import net.minecraft.entity.ai.pathing.PathNode
import net.minecraft.entity.ai.pathing.PathNodeNavigator
import net.minecraft.entity.ai.pathing.PathNodeType
import net.minecraft.entity.damage.DamageSource
import net.minecraft.registry.tag.FluidTags
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.MathHelper
import net.minecraft.util.math.Vec3d
import net.minecraft.world.World

class PokemonNavigation(val world: World, val pokemonEntity: PokemonEntity) : MobNavigation(pokemonEntity, world) {
    // Lazy init because navigation is instantiated during entity construction and pokemonEntity.form isn't set yet.
    // (pokemonEntity.behaviour is a shortcut to pokemonEntity.form.behaviour)
    // It's JVM field instantiation order stuff, too niche to explain further.
    val moving: MoveBehaviour by lazy { pokemonEntity.behaviour.moving }

    var cachedCurrentNode: PathNode? = null
    var currentNodeDistance = 0F

    data class NavigationContext(
        val onHit: (damage: DamageSource) -> Unit = {},
        val onRecalculate: (dueToDistance: Boolean) -> Unit = {},
        val onArrival: () -> Unit = {},
        val onCannotReach: () -> Unit = {},
        val sprinting: Boolean = false,
        val destinationProximity: Float = 0.5F
    )

    var navigationContext = NavigationContext()

    override fun createPathNodeNavigator(range: Int): PathNodeNavigator {
        this.nodeMaker = OmniPathNodeMaker()
        nodeMaker.setCanEnterOpenDoors(true)
        return PathNodeNavigator(nodeMaker, range)
    }

    override fun isAtValidPosition(): Boolean {
        val (_, isTouchingLava) = entity.world.getWaterAndLavaIn(entity.boundingBox)
        val isAtValidPosition = (!entity.isInLava && !entity.isSubmergedIn(FluidTags.LAVA)) ||
                (isTouchingLava && moving.swim.canSwimInLava) ||
                this.entity.hasVehicle()
        return isAtValidPosition
    }

    override fun canSwim(): Boolean {
        return moving.swim.canSwimInWater
    }

    fun setCanPathThroughFire(canPathThroughFire: Boolean) {
        val omniPathNodeMaker = this.nodeMaker as OmniPathNodeMaker
        omniPathNodeMaker.canPathThroughFire = canPathThroughFire
    }

    override fun getPos() = Vec3d(entity.x, getPathfindingY().toDouble(), entity.z)

    override fun continueFollowingPath() {
        val vec3d = this.pos

        val targetVec = targetPos?.toVec3d()?.add(0.5, 0.0, 0.5)
        if (targetVec != null && targetVec.distanceTo(vec3d) <= navigationContext.destinationProximity && currentPath != null) {
            currentPath = null
            cachedCurrentNode = null
            navigationContext.onArrival()
            // If we arrived at a not-flying destination
            val node = currentPath?.currentNode?.type
            if (node != null && node != PathNodeType.OPEN && pokemonEntity.couldStopFlying()) {
                pokemonEntity.setBehaviourFlag(PokemonBehaviourFlag.FLYING, false)
            }
            return
        }

        nodeReachProximity = if (entity.width > 0.75f) entity.width / 2.0f else 0.75f - entity.width / 2.0f

        val currentNode = currentPath!!.currentNode
        if (currentNode != cachedCurrentNode) {
            cachedCurrentNode = currentNode
            currentNodeDistance = currentNode.pos.distanceTo(pokemonEntity.pos).toFloat()
        } else if (cachedCurrentNode != null && cachedCurrentNode!!.pos.distanceTo(pokemonEntity.pos) > currentNodeDistance + 1) {
            recalculatePath()
            navigationContext.onRecalculate(true)
            return
        }

        /*
         * The difference between this and the overrided function is that we use the vector
         * position for the d,e,f which improves behaviour of larger pokemon
         */
        val targetVec3d = currentPath!!.getNodePosition(entity)
        val d = abs(entity.x - targetVec3d.x)
        val e = abs(entity.y - targetVec3d.y)
        val f = abs(entity.z - targetVec3d.z)
        val closeEnough = d < nodeReachProximity.toDouble() && f < this.nodeReachProximity.toDouble() && e < 1.0

        if (closeEnough || entity.navigation.canJumpToNext(currentPath!!.currentNode.type) && shouldJumpToNextNode(vec3d)) {
            currentPath!!.next()
            if (currentPath!!.isFinished) {
                currentPath = null
                navigationContext.onArrival()
                // If we arrived at a not-flying destination
                if (currentNode.type != PathNodeType.OPEN && pokemonEntity.couldStopFlying()) {
                    pokemonEntity.setBehaviourFlag(PokemonBehaviourFlag.FLYING, false)
                }
            } else {
                val newNode = currentPath!!.currentNode
                if (currentNode.type != newNode.type) {
                    if (newNode.type == PathNodeType.OPEN) {
                        pokemonEntity.setBehaviourFlag(PokemonBehaviourFlag.FLYING, true)
                    } else if (currentNode.type != PathNodeType.OPEN && pokemonEntity.couldStopFlying()) { // if we just reached a non-flying node and the next node isn't a flying node, stop flying
                        pokemonEntity.setBehaviourFlag(PokemonBehaviourFlag.FLYING, false)
                    }
                }
            }
        }

        checkTimeouts(vec3d)
    }

    fun isAirborne(world: World, pos: BlockPos) =
        world.getBlockState(pos).canPathfindThrough(world, pos, NavigationType.AIR)
                && world.getBlockState(pos.down(1)).canPathfindThrough(world, pos.down(1), NavigationType.AIR)
                && world.getBlockState(pos.down(2)).canPathfindThrough(world, pos.down(2), NavigationType.AIR)

    override fun tick() {
        super.tick()
//        val currentPath = getCurrentPath()
//        val node = if (currentPath == null || currentPath.isFinished) null else currentPath.lastNode
//
//        val isFlying = pokemonEntity.getBehaviourFlag(PokemonBehaviourFlag.FLYING)
//        val canWalk = pokemonEntity.behaviour.moving.walk.canWalk
//        val canFly = pokemonEntity.behaviour.moving.fly.canFly
//        if (node != null) {
//            if (node.type == PathNodeType.OPEN) {
//                val canFly = moving.fly.canFly
//                if (canFly && !pokemonEntity.getBehaviourFlag(PokemonBehaviourFlag.FLYING)) {
//                    pokemonEntity.setBehaviourFlag(PokemonBehaviourFlag.FLYING, true)
//                }
//            } else if (node.type != PathNodeType.OPEN && isFlying && canWalk) {
//                pokemonEntity.setBehaviourFlag(PokemonBehaviourFlag.FLYING, false)
//            }
//        } else if (!isFlying && canFly && isAirborne(pokemonEntity.world, pokemonEntity.blockPos)) {
//            pokemonEntity.setBehaviourFlag(PokemonBehaviourFlag.FLYING, true)
//        } else if (isFlying && canWalk && !pokemonEntity.world.getBlockState(pokemonEntity.blockPos).canPathfindThrough(pokemonEntity.world, pokemonEntity.blockPos.down(), NavigationType.LAND)) {
//            pokemonEntity.setBehaviourFlag(PokemonBehaviourFlag.FLYING, false)
//        }
    }

    fun findPath(target: BlockPos, distance: Int): Path? = findPathTo(ImmutableSet.of(target), 8, false, distance)

    override fun findPathTo(target: BlockPos, distance: Int): Path? {
        var target = target

        var blockPos: BlockPos
        if (this.world.getBlockState(target).isAir && !pokemonEntity.behaviour.moving.fly.canFly) {
            blockPos = target.down()
            while (blockPos.y > this.world.bottomY && this.world.getBlockState(blockPos).isAir) {
                blockPos = blockPos.down()
            }
            while (blockPos.y < this.world.topY && this.world.getBlockState(blockPos).isAir) {
                blockPos = blockPos.up()
            }
            target = blockPos
        }

        val path = if (!this.world.getBlockState(target).isSolid) {
            findPath(target, distance)
        } else {
            blockPos = target.up()
            while (blockPos.y < this.world.topY && this.world.getBlockState(blockPos).isSolid) {
                blockPos = blockPos.up()
            }
            findPath(blockPos, distance)
        }

//        path?.let {
//            try {
//                var i = 0
//                while (true) {
//                    val node = it.getNode(i)
//                    val blockState = if (node.type == PathNodeType.OPEN) {
//                        Blocks.BONE_BLOCK.defaultState
//                    } else if (node.type == PathNodeType.WALKABLE) {
//                        Blocks.GOLD_BLOCK.defaultState
//                    } else if (node.type == PathNodeType.WATER) {
//                        Blocks.PACKED_ICE.defaultState
//                    } else {
//                        Blocks.COAL_BLOCK.defaultState
//                    }
//                    entity.world.setBlockState(node.blockPos, blockState)
//                    i++
//                }
//            } catch(e: Exception) {
//            }
//            entity.remove(Entity.RemovalReason.DISCARDED)
//        }

        return path
    }

    fun startMovingTo(x: Double, y: Double, z: Double, speed: Double = 1.0, navigationContext: NavigationContext) {
        this.navigationContext = navigationContext
        this.startMovingTo(x, y, z, speed)
    }

    override fun findPathTo(entity: Entity, distance: Int): Path? {
        return this.findPathTo(entity.blockPos, distance)
    }

    override fun startMovingAlong(path: Path?, speed: Double): Boolean {
        if (path != null && path.length > 0) {
            val node = path.getNode(0)!!
//            pokemonEntity.discard()
//            return false
            // If we just started moving and it's to an open node, fly
            if (node.type == PathNodeType.OPEN && pokemonEntity.form.behaviour.moving.fly.canFly && !pokemonEntity.isFlying()) {
                pokemonEntity.setBehaviourFlag(PokemonBehaviourFlag.FLYING, true)
            }
        }

        return super.startMovingAlong(path, speed)
    }

    override fun adjustPath() {
        super.adjustPath()
        val path = getCurrentPath() ?: return
        var i = 2

        // Tries to skip some nodes that are all lined up
        skipLoop@
        while (i < path.length) {
            val firstNode = path.getNode(i - 2)
            val middleNode = path.getNode(i - 1)
            val nextNode = path.getNode(i)

            val directionToMiddle = middleNode.blockPos.subtract(firstNode.blockPos).toVec3d().normalize()
            val nodeType = firstNode.type
            if (nodeType != middleNode.type || nodeType != nextNode.type || nodeType == PathNodeType.WALKABLE) {
                i++
                continue
            }

            val directionToEnd = nextNode.blockPos.subtract(middleNode.blockPos).toVec3d().normalize()

            // If we'd be making a big (greater than 45 degrees) turn by removing the middle node, that's a bit much, leave it alone.
            if (acos(directionToMiddle.dotProduct(directionToEnd)) > PI / 3) {
                i++
                continue
            }

            var directionFromFirstToEnd = nextNode.blockPos.subtract(firstNode.blockPos).toVec3d()
            val length = directionFromFirstToEnd.length()
            directionFromFirstToEnd = directionFromFirstToEnd.normalize()

            // Get all the nodes our hitbox would touch on our way there
            for (dist in 1..ceil(length).toInt() * 2) {
                val vec = firstNode.pos.add(directionFromFirstToEnd.multiply(dist.toDouble() / 2.0))
                val interveningNodeType = pokemonEntity.navigation.nodeMaker.getNodeType(world, vec.x.toInt(), vec.y.toInt(), vec.z.toInt(), pokemonEntity)
                if (interveningNodeType != nodeType) {
                    i++
                    continue@skipLoop
                }
            }

            // Construct a new node list that cuts out unnecessary in-between bits
            val remainingNodes = mutableListOf<PathNode>()
            var j = i
            while (j < path.length) {
                remainingNodes.add(path.getNode(j))
                j++
            }

            path.length = i + remainingNodes.size - 1
            for (k in remainingNodes.indices) {
                path.setNode(i - 1 + k, remainingNodes[k])
            }
        }

        // Could check for direct sunlight or rain or something
//        if (avoidSunlight) {
//            if (this.world.isSkyVisible(BlockPos(entity.x, entity.y + 0.5, entity.z))) {
//                return
//            }
//            for (i in 0 until currentPath!!.length) {
//                val pathNode = currentPath!!.getNode(i)
//                if (!this.world.isSkyVisible(BlockPos(pathNode.x, pathNode.y, pathNode.z))) continue
//                currentPath!!.length = i
//                return
//            }
//        }
    }

    fun getPathfindingY(): Int {
        val inSwimmableFluid = (entity.isSubmergedIn(FluidTags.WATER) && moving.swim.canSwimInWater) ||
                (entity.isSubmergedIn(FluidTags.LAVA) && moving.swim.canSwimInLava)
        if (!inSwimmableFluid) {
            return MathHelper.floor(entity.y + 0.5)
        }

        return entity.blockY
    }

    override fun adjustTargetY(pos: Vec3d) = pos.y

    override fun stop() {
        super.stop()
        this.currentNodeDistance = -1F
        this.cachedCurrentNode = null
        currentPath = null
        nodeMaker.clear()
        // In case a path is cancelled instead of completed, check if we should stop flying
        if (pokemonEntity.couldStopFlying() && !isAirborne(pokemonEntity.world, pokemonEntity.blockPos)) {
            pokemonEntity.setBehaviourFlag(PokemonBehaviourFlag.FLYING, false)
        }
    }
}