/*
 * Copyright (C) 2022 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.client.render.models.blockbench.pokemon.gen1

import com.cobblemon.mod.common.client.render.models.blockbench.frame.HeadedFrame
import com.cobblemon.mod.common.client.render.models.blockbench.pokemon.PokemonPose
import com.cobblemon.mod.common.client.render.models.blockbench.pokemon.PokemonPoseableModel
import com.cobblemon.mod.common.entity.PoseType
import com.cobblemon.mod.common.entity.PoseType.Companion.STANDING_POSES
import com.cobblemon.mod.common.entity.PoseType.Companion.UI_POSES
import net.minecraft.client.model.ModelPart
import net.minecraft.util.math.Vec3d

class DewgongModel(root: ModelPart) : PokemonPoseableModel(), HeadedFrame {
    override val rootPart = root.registerChildWithAllChildren("dewgong")
    override val head = getPart("head")

    override val portraitScale = 1.0F
    override val portraitTranslation = Vec3d(0.0, 0.0, 0.0)

    override val profileScale = 1.0F
    override val profileTranslation = Vec3d(0.0, 0.0, 0.0)

    lateinit var standing: PokemonPose
    lateinit var float: PokemonPose
    lateinit var swim: PokemonPose

    override fun registerPoses() {
        standing = registerPose(
            poseName = "standing",
            poseTypes = STANDING_POSES + UI_POSES,
            idleAnimations = arrayOf(
                singleBoneLook(),
                bedrock("0087_dewgong/dewgong", "ground_idle")
            )
        )

        float = registerPose(
            poseName = "float",
            poseType = PoseType.FLOAT,
            idleAnimations = arrayOf(
                singleBoneLook(),
                bedrock("0087_dewgong/dewgong", "water_idle")
            )
        )

        swim = registerPose(
            poseName = "swim",
            poseType = PoseType.SWIM,
            idleAnimations = arrayOf(
                singleBoneLook(),
                bedrock("0087_dewgong/dewgong", "water_swim")
            )
        )
    }

//    override fun getFaintAnimation(
//        pokemonEntity: PokemonEntity,
//        state: PoseableEntityState<PokemonEntity>
//    ) = if (state.isPosedIn(standing, walk)) bedrockStateful("0087_dewgong/dewgong", "faint") else null
}