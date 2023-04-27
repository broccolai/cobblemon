package com.cobblemon.mod.common.client.render.models.blockbench.pokemon.gen2

import com.cobblemon.mod.common.client.render.models.blockbench.pokemon.PokemonPose
import com.cobblemon.mod.common.client.render.models.blockbench.pokemon.PokemonPoseableModel
import com.cobblemon.mod.common.entity.PoseType
import net.minecraft.client.model.ModelPart
import net.minecraft.util.math.Vec3d

class ChinchouModel (root: ModelPart) : PokemonPoseableModel() {
    override val rootPart = root.registerChildWithAllChildren("chinchou")

    override val portraitScale = 1.75F
    override val portraitTranslation = Vec3d(-0.3, -0.8, 0.0)

    override val profileScale = 0.65F
    override val profileTranslation = Vec3d(-0.05, 0.45, 0.0)

    lateinit var standing: PokemonPose
    lateinit var swim: PokemonPose

    override fun registerPoses() {
        val blink = quirk("blink") { bedrockStateful("chinchou", "blink").setPreventsIdle(false)}

        standing = registerPose(
                poseName = "standing",
                poseTypes = PoseType.STATIONARY_POSES + PoseType.WALK,
                quirks = arrayOf(blink),
                idleAnimations = arrayOf(
                        bedrock("chinchou", "ground_idle")
                )
        )

        swim = registerPose(
                poseName = "swimming",
                poseTypes = PoseType.SWIMMING_POSES + PoseType.UI_POSES,
                quirks = arrayOf(blink),
                idleAnimations = arrayOf(
                        bedrock("chinchou", "swim_idle")
                )
        )
    }
}