/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.client.render.models.blockbench.pokemon.gen1

import com.cobblemon.mod.common.client.render.models.blockbench.PoseableEntityState
import com.cobblemon.mod.common.client.render.models.blockbench.frame.BimanualFrame
import com.cobblemon.mod.common.client.render.models.blockbench.frame.BipedFrame
import com.cobblemon.mod.common.client.render.models.blockbench.frame.HeadedFrame
import com.cobblemon.mod.common.client.render.models.blockbench.pokemon.CryProvider
import com.cobblemon.mod.common.client.render.models.blockbench.pokemon.PokemonPose
import com.cobblemon.mod.common.client.render.models.blockbench.pokemon.PokemonPoseableModel
import com.cobblemon.mod.common.entity.PoseType
import com.cobblemon.mod.common.entity.PoseType.Companion.STATIONARY_POSES
import com.cobblemon.mod.common.entity.PoseType.Companion.UI_POSES
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity
import net.minecraft.client.model.ModelPart
import net.minecraft.util.math.Vec3d

class SquirtleModel(root: ModelPart) : PokemonPoseableModel(), HeadedFrame, BipedFrame, BimanualFrame {
    override val rootPart = root.registerChildWithAllChildren("squirtle")
    override val head = getPart("head")
    override val rightArm = getPart("arm_right")
    override val leftArm = getPart("arm_left")
    override val rightLeg = getPart("leg_right")
    override val leftLeg = getPart("leg_left")

    override val portraitScale = 2.1F
    override val portraitTranslation = Vec3d(-0.1, -0.25, 0.0)

    override val profileScale = 0.8F
    override val profileTranslation = Vec3d(0.0, 0.6, 0.0)

    lateinit var sleep: PokemonPose
    lateinit var standing: PokemonPose
    lateinit var swimIdle: PokemonPose
    lateinit var swim: PokemonPose
    lateinit var walk: PokemonPose

    override val cryAnimation = CryProvider { _, _ -> bedrockStateful("squirtle", "cry") }

    override fun registerPoses() {
        val blink = quirk { bedrockStateful("squirtle", "blink")}
        sleep = registerPose(
            poseType = PoseType.SLEEP,
            transformTicks = 10,
            quirks = arrayOf(blink),
            idleAnimations = arrayOf(bedrock("squirtle", "sleep"))
        )

        standing = registerPose(
            poseName = "standing",
            poseTypes = UI_POSES + STATIONARY_POSES - PoseType.FLOAT,
            transformTicks = 10,
            quirks = arrayOf(blink),
            idleAnimations = arrayOf(
                singleBoneLook(),
                bedrock("squirtle", "ground_idle")
            )
        )

        walk = registerPose(
            poseType = PoseType.WALK,
            transformTicks = 10,
            quirks = arrayOf(blink),
            idleAnimations = arrayOf(
                singleBoneLook(),
                bedrock("squirtle", "ground_walk")
            )
        )

        swimIdle = registerPose(
            poseName = "swim_idle",
            poseTypes = setOf(PoseType.FLOAT, PoseType.HOVER),
            transformTicks = 10,
            quirks = arrayOf(blink),
            idleAnimations = arrayOf(
                singleBoneLook(),
                bedrock("squirtle", "water_idle")
            )
        )

        swim = registerPose(
            poseName = "swim",
            poseTypes = setOf(PoseType.SWIM, PoseType.FLY),
            transformTicks = 10,
            quirks = arrayOf(blink),
            idleAnimations = arrayOf(
                singleBoneLook(),
                bedrock("squirtle", "water_swim")
            )
        )
    }

    override fun getFaintAnimation(
        pokemonEntity: PokemonEntity,
        state: PoseableEntityState<PokemonEntity>
    ) = bedrockStateful("squirtle", "faint")
}