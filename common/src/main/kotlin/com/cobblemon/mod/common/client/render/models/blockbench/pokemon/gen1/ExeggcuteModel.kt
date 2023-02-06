/*
 * Copyright (C) 2022 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.client.render.models.blockbench.pokemon.gen1

import com.cobblemon.mod.common.client.render.models.blockbench.pokemon.PokemonPose
import com.cobblemon.mod.common.client.render.models.blockbench.pokemon.PokemonPoseableModel
import com.cobblemon.mod.common.entity.PoseType.Companion.MOVING_POSES
import com.cobblemon.mod.common.entity.PoseType.Companion.STATIONARY_POSES
import com.cobblemon.mod.common.entity.PoseType.Companion.UI_POSES
import net.minecraft.client.model.ModelPart
import net.minecraft.util.math.Vec3d
class ExeggcuteModel(root: ModelPart) : PokemonPoseableModel() {
    override val rootPart = root.registerChildWithAllChildren("exeggcute")

    override val portraitScale = 2.3F
    override val portraitTranslation = Vec3d(-0.25, -1.87, 0.0)

    override val profileScale = 1.0F
    override val profileTranslation = Vec3d(-0.05, 0.17, 0.0)

    lateinit var standing: PokemonPose
    lateinit var walk: PokemonPose

    override fun registerPoses() {
        val blink = quirk("blink") { bedrockStateful("exeggcute", "blink1").setPreventsIdle(false)}
        val blink2 = quirk("blink2") { bedrockStateful("exeggcute", "blink2").setPreventsIdle(false)}
        val blink3 = quirk("blink3") { bedrockStateful("exeggcute", "blink3").setPreventsIdle(false)}
        val blink4 = quirk("blink4") { bedrockStateful("exeggcute", "blink4").setPreventsIdle(false)}
        val blink5 = quirk("blink5") { bedrockStateful("exeggcute", "blink5").setPreventsIdle(false)}
        standing = registerPose(
            poseName = "standing",
            poseTypes = STATIONARY_POSES + UI_POSES,
            transformTicks = 10,
            quirks = arrayOf(blink, blink2, blink3, blink4, blink5),
            idleAnimations = arrayOf(
                //bedrock("exeggcute", "ground_idle")
            )
        )

        walk = registerPose(
            poseName = "walk",
            poseTypes = MOVING_POSES,
            transformTicks = 10,
            quirks = arrayOf(blink, blink2, blink3, blink4, blink5),
            idleAnimations = arrayOf(
                //bedrock("exeggcute", "ground_walk")
            )
        )
    }

//    override fun getFaintAnimation(
//        pokemonEntity: PokemonEntity,
//        state: PoseableEntityState<PokemonEntity>
//    ) = if (state.isPosedIn(standing, walk)) bedrockStateful("exeggcute", "faint") else null
}