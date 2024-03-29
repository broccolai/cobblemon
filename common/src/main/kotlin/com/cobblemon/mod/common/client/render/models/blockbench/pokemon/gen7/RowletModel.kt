/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.client.render.models.blockbench.pokemon.gen7

import com.cobblemon.mod.common.client.render.models.blockbench.animation.BipedWalkAnimation
import com.cobblemon.mod.common.client.render.models.blockbench.animation.WingFlapIdleAnimation
import com.cobblemon.mod.common.client.render.models.blockbench.frame.BiWingedFrame
import com.cobblemon.mod.common.client.render.models.blockbench.frame.BipedFrame
import com.cobblemon.mod.common.client.render.models.blockbench.pokemon.CryProvider
import com.cobblemon.mod.common.client.render.models.blockbench.pokemon.PokemonPose
import com.cobblemon.mod.common.client.render.models.blockbench.pokemon.PokemonPoseableModel
import com.cobblemon.mod.common.client.render.models.blockbench.pose.ModelPartTransformation
import com.cobblemon.mod.common.client.render.models.blockbench.wavefunction.sineFunction
import com.cobblemon.mod.common.entity.PoseType
import com.cobblemon.mod.common.entity.PoseType.Companion.MOVING_POSES
import com.cobblemon.mod.common.entity.PoseType.Companion.STATIONARY_POSES
import com.cobblemon.mod.common.entity.PoseType.Companion.UI_POSES
import com.cobblemon.mod.common.util.math.geometry.toRadians
import net.minecraft.client.model.ModelPart
import net.minecraft.util.math.Vec3d

class RowletModel(root: ModelPart) : PokemonPoseableModel(), BipedFrame, BiWingedFrame {
    override val rootPart = root.registerChildWithAllChildren("rowlet")

    override val leftWing = getPart("wing_left_main")
    override val rightWing = getPart("wing_right_main")

    override val leftLeg = getPart("foot_left")
    override val rightLeg = getPart("foot_right")

    override val portraitScale = 2.6F
    override val portraitTranslation = Vec3d(-0.15, -1.7, 0.0)

    override val profileScale = 1.1F
    override val profileTranslation = Vec3d(0.0, 0.09, 0.0)

    lateinit var fly: PokemonPose
    lateinit var flyidle: PokemonPose
    lateinit var standing: PokemonPose
    lateinit var walk: PokemonPose

    override val cryAnimation = CryProvider { _, _ -> bedrockStateful("rowlet", "cry") }

    override fun registerPoses() {
        val blink = quirk { bedrockStateful("rowlet", "blink") }
        standing = registerPose(
            poseName = "standing",
            poseTypes = STATIONARY_POSES - PoseType.HOVER + UI_POSES,
            quirks = arrayOf(blink),
            idleAnimations = arrayOf(
                bedrock("rowlet", "ground_idle")
            )
        )

        flyidle = registerPose(
                poseName = "hover",
                poseType = PoseType.HOVER,
                transformTicks = 10,
                quirks = arrayOf(blink),
                idleAnimations = arrayOf(
                    bedrock("rowlet", "air_idle"),
                    WingFlapIdleAnimation(this,
                        flapFunction = sineFunction(verticalShift = -8F.toRadians(), period = 1.0F, amplitude = 0.4F),
                        timeVariable = { state, _, _ -> state?.animationSeconds ?: 0F },
                        axis = ModelPartTransformation.Z_AXIS
                    )
                )
        )

        fly = registerPose(
                poseName = "fly",
                poseType = PoseType.FLY,
                transformTicks = 10,
                quirks = arrayOf(blink),
                idleAnimations = arrayOf(
                    bedrock("rowlet", "air_fly"),
                    WingFlapIdleAnimation(this,
                        flapFunction = sineFunction(verticalShift = -14F.toRadians(), period = 0.9F, amplitude = 0.9F),
                        timeVariable = { state, _, _ -> state?.animationSeconds ?: 0F },
                        axis = ModelPartTransformation.Z_AXIS
                    )
                )
        )

        walk = registerPose(
            poseName = "walk",
            poseTypes = MOVING_POSES - PoseType.FLY,
            quirks = arrayOf(blink),
            idleAnimations = arrayOf(
                bedrock("rowlet", "ground_idle"),
                BipedWalkAnimation(this, periodMultiplier = 0.8F, amplitudeMultiplier = 0.7F)
                //bedrock("rowlet", "ground_walk")
            )
        )
    }

//    override fun getFaintAnimation(
//        pokemonEntity: PokemonEntity,
//        state: PoseableEntityState<PokemonEntity>
//    ) = if (state.isPosedIn(standing, walk)) bedrockStateful("rowlet", "faint") else null
}