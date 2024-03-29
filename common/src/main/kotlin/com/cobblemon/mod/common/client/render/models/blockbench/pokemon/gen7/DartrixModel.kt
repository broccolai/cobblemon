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

class DartrixModel(root: ModelPart) : PokemonPoseableModel(), BipedFrame, BiWingedFrame {
    override val rootPart = root.registerChildWithAllChildren("dartrix")

    override val leftWing = getPart("wing_left1")
    override val rightWing = getPart("wing_right1")

    override val leftLeg = getPart("leg_left")
    override val rightLeg = getPart("leg_right")

    override val portraitScale = 2.6F
    override val portraitTranslation = Vec3d(-0.1, -0.7, 0.0)

    override val profileScale = 1.1F
    override val profileTranslation = Vec3d(0.0, 0.1, 0.0)

    lateinit var fly: PokemonPose
    lateinit var flyidle: PokemonPose
    lateinit var standing: PokemonPose
    lateinit var walk: PokemonPose

    override val cryAnimation = CryProvider { _, _ -> bedrockStateful("dartrix", "cry") }

    override fun registerPoses() {
        standing = registerPose(
            poseName = "standing",
            poseTypes = STATIONARY_POSES - PoseType.HOVER + UI_POSES,
            idleAnimations = arrayOf(
                bedrock("dartrix", "ground_idle")
            )
        )

        flyidle = registerPose(
                poseName = "hover",
                poseType = PoseType.HOVER,
                transformTicks = 10,
                idleAnimations = arrayOf(
                    bedrock("dartrix", "air_idle"),
                    WingFlapIdleAnimation(this,
                        flapFunction = sineFunction(verticalShift = -10F.toRadians(), period = 0.9F, amplitude = 0.6F),
                        timeVariable = { state, _, _ -> state?.animationSeconds ?: 0F },
                        axis = ModelPartTransformation.Z_AXIS
                    )
                )
        )

        fly = registerPose(
                poseName = "fly",
                poseType = PoseType.FLY,
                transformTicks = 10,
                idleAnimations = arrayOf(
                    bedrock("dartrix", "air_fly"),
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
            idleAnimations = arrayOf(
                bedrock("dartrix", "ground_idle"),
                BipedWalkAnimation(this, periodMultiplier = 0.75F, amplitudeMultiplier = 0.7F)
                //bedrock("dartrix", "ground_walk")
            )
        )
    }

//    override fun getFaintAnimation(
//        pokemonEntity: PokemonEntity,
//        state: PoseableEntityState<PokemonEntity>
//    ) = if (state.isPosedIn(standing, walk)) bedrockStateful("dartrix", "faint") else null
}