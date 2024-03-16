/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.entity.pokemon

import com.cobblemon.mod.common.CobblemonSounds
import com.cobblemon.mod.common.api.entity.PokemonSideDelegate
import com.cobblemon.mod.common.api.pokemon.stats.Stats
import com.cobblemon.mod.common.api.pokemon.status.Statuses
import com.cobblemon.mod.common.battles.BattleRegistry
import com.cobblemon.mod.common.entity.PoseType
import com.cobblemon.mod.common.entity.pokemon.ai.PokemonMoveControl
import com.cobblemon.mod.common.pokemon.Pokemon
import com.cobblemon.mod.common.pokemon.activestate.ActivePokemonState
import com.cobblemon.mod.common.pokemon.activestate.SentOutState
import com.cobblemon.mod.common.util.playSoundServer
import com.cobblemon.mod.common.util.update
import com.cobblemon.mod.common.world.gamerules.CobblemonGameRules
import java.util.Optional
import net.minecraft.entity.Entity
import net.minecraft.entity.ai.pathing.PathNodeType
import net.minecraft.entity.attribute.EntityAttributes
import net.minecraft.entity.damage.DamageSource
import net.minecraft.entity.data.TrackedData
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.text.Text

/** Handles purely server logic for a Pokémon */
class PokemonServerDelegate : PokemonSideDelegate {
    lateinit var entity: PokemonEntity
    var acknowledgedHPStat = -1

    override fun changePokemon(pokemon: Pokemon) {
        updatePathfindingPenalties(pokemon)
        entity.initGoals()
        updateMaxHealth()
    }

    fun updatePathfindingPenalties(pokemon: Pokemon) {
        val moving = pokemon.form.behaviour.moving
        entity.setPathfindingPenalty(PathNodeType.LAVA, if (moving.swim.canSwimInLava) 12F else -1F)
        entity.setPathfindingPenalty(PathNodeType.WATER, if (moving.swim.canSwimInWater) 12F else -1F)
        entity.setPathfindingPenalty(PathNodeType.WATER_BORDER, if (moving.swim.canSwimInWater) 6F else -1F)
        if (moving.swim.canBreatheUnderwater) {
            entity.setPathfindingPenalty(PathNodeType.WATER, if (moving.walk.avoidsLand) 0F else 4F)
        }
        if (moving.swim.canBreatheUnderlava) {
            entity.setPathfindingPenalty(PathNodeType.LAVA, if (moving.swim.canSwimInLava) 4F else -1F)
        }
        if (moving.walk.avoidsLand) {
            entity.setPathfindingPenalty(PathNodeType.WALKABLE, 12F)
        }

        if (moving.walk.canWalk && moving.fly.canFly) {
            entity.setPathfindingPenalty(PathNodeType.WALKABLE, 0F)
        }

        entity.navigation.setCanPathThroughFire(entity.isFireImmune)
    }

    fun updateMaxHealth() {
        val currentHealthRatio = entity.health.toDouble() / entity.maxHealth
        // Why you would remove HP is beyond me but protects us from obscure crash due to crappy addon
        acknowledgedHPStat = entity.form.baseStats[Stats.HP] ?: return

        val minStat = 50 // Metapod's base HP
        val maxStat = 150 // Slaking's base HP
        val baseStat = acknowledgedHPStat.coerceIn(minStat..maxStat)
        val r = (baseStat - minStat) / (maxStat - minStat).toDouble()
        val minPossibleHP = 10.0 // half of a player's HP
        val maxPossibleHP = 100.0 // Iron Golem HP
        val maxHealth = minPossibleHP + r * (maxPossibleHP - minPossibleHP)

        entity.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH)?.baseValue = maxHealth
        entity.health = currentHealthRatio.toFloat() * maxHealth.toFloat()
    }

    override fun initialize(entity: PokemonEntity) {
        this.entity = entity
        with(entity) {
            speed = 0.1F
            entity.despawner.beginTracking(this)
        }
    }

    fun getBattle() = entity.battleId?.let(BattleRegistry::getBattle)

    override fun onTrackedDataSet(data: TrackedData<*>) {
        super.onTrackedDataSet(data)
        if (this::entity.isInitialized) {
            when (data) {
                PokemonEntity.BEHAVIOUR_FLAGS -> updatePoseType()
            }
        }
    }

    override fun tick(entity: PokemonEntity) {
        val state = entity.pokemon.state
        if (state !is ActivePokemonState || state.entity != entity) {
            if (!entity.isDead && entity.health > 0) {
                entity.pokemon.state = SentOutState(entity)
            }
        }

        if (entity.ownerUuid != null && entity.pokemon.storeCoordinates.get() == null) {
            entity.discard()
        }

        val tethering = entity.tethering
        if (tethering != null && entity.pokemon.tetheringId != tethering.tetheringId) {
            entity.discard()
        }

//        if (!entity.behaviour.moving.walk.canWalk && entity.behaviour.moving.fly.canFly && !entity.getBehaviourFlag(PokemonBehaviourFlag.FLYING)) {
//            entity.setBehaviourFlag(PokemonBehaviourFlag.FLYING, true)
//        }

        entity.dataTracker.update(PokemonEntity.BATTLE_ID) { opt ->
            val battleId = opt.orElse(null)
            if (battleId != null && BattleRegistry.getBattle(battleId).let { it == null || it.ended }) {
                Optional.empty()
            } else {
                opt
            }
        }

        val battle = getBattle()
        if (entity.ticksLived % 20 == 0 && battle != null) {
            val activeBattlePokemon = battle
                .activePokemon
                .find { it.battlePokemon?.uuid == entity.pokemon.uuid }

            if (activeBattlePokemon != null) {
                activeBattlePokemon.position = entity.world as ServerWorld to entity.pos
            }
        }

        if (entity.form.baseStats[Stats.HP] != acknowledgedHPStat) {
            updateMaxHealth()
        }

        if (entity.ownerUuid != entity.pokemon.getOwnerUUID()) {
            entity.ownerUuid = entity.pokemon.getOwnerUUID()
        }

        if (entity.ownerUuid == null && tethering != null) {
            entity.ownerUuid = tethering.playerId
        }

        if (entity.ownerUuid != null && entity.owner == null && entity.tethering == null) {
            entity.remove(Entity.RemovalReason.DISCARDED)
        }

        entity.dataTracker.set(PokemonEntity.SPECIES, entity.pokemon.species.resourceIdentifier.toString())
        if (entity.dataTracker.get(PokemonEntity.NICKNAME) != entity.pokemon.nickname) {
            entity.dataTracker.set(PokemonEntity.NICKNAME, entity.pokemon.nickname ?: Text.empty())
        }
        entity.dataTracker.set(PokemonEntity.ASPECTS, entity.pokemon.aspects)
        entity.dataTracker.set(PokemonEntity.LABEL_LEVEL, entity.pokemon.level)
        entity.dataTracker.set(PokemonEntity.MOVING, !entity.navigation.isIdle)

        updatePoseType()
    }

    fun updatePoseType() {
        val isSleeping = entity.pokemon.status?.status == Statuses.SLEEP && entity.behaviour.resting.canSleep
        val isMoving = (entity.moveControl as? PokemonMoveControl)?.isMoving == true
        val isPassenger = entity.hasVehicle()
        val isUnderwater = entity.getIsSubmerged()
        val isFlying = entity.getBehaviourFlag(PokemonBehaviourFlag.FLYING)

        val poseType = when {
            isPassenger -> PoseType.STAND
            isSleeping -> PoseType.SLEEP
            isMoving && isUnderwater  -> PoseType.SWIM
            isUnderwater -> PoseType.FLOAT
            isMoving && isFlying -> PoseType.FLY
            isFlying -> PoseType.HOVER
            isMoving -> PoseType.WALK
            else -> PoseType.STAND
        }

        entity.dataTracker.set(PokemonEntity.POSE_TYPE, poseType)
    }

    override fun drop(source: DamageSource?) {
        val player = source?.source as? ServerPlayerEntity
        if (entity.pokemon.isWild()) {
            entity.killer = player
        }
    }

    override fun updatePostDeath() {
        entity.dataTracker.set(PokemonEntity.DYING_EFFECTS_STARTED, true)
        ++entity.deathTime

        if (entity.deathTime == 30) {
            val owner = entity.owner
            if (owner != null) {
                entity.world.playSoundServer(owner.pos, CobblemonSounds.POKE_BALL_RECALL, volume = 0.6F)
                entity.dataTracker.set(PokemonEntity.PHASING_TARGET_ID, owner.id)
                entity.dataTracker.set(PokemonEntity.BEAM_MODE, 2)
            }
        }

        if (entity.deathTime == 60) {
            if (entity.owner == null) {
                entity.world.sendEntityStatus(entity, 60.toByte()) // Sends smoke effect
                if(entity.world.gameRules.getBoolean(CobblemonGameRules.DO_POKEMON_LOOT)) {
                    (entity.drops ?: entity.pokemon.form.drops).drop(entity, entity.world as ServerWorld, entity.pos, entity.killer)
                }
            }

            entity.remove(Entity.RemovalReason.KILLED)
        }
    }
}