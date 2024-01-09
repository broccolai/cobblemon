/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.client.render.block

import com.cobblemon.mod.common.CobblemonBlocks
import com.cobblemon.mod.common.block.entity.fossil.FossilTubeBlockEntity
import com.cobblemon.mod.common.block.multiblock.FossilMultiblockStructure
import com.cobblemon.mod.common.client.CobblemonBakingOverrides
import com.cobblemon.mod.common.client.render.models.blockbench.fossil.FossilModel
import com.cobblemon.mod.common.client.render.models.blockbench.repository.FossilModelRepository
import net.minecraft.block.HorizontalFacingBlock
import net.minecraft.client.render.OverlayTexture
import net.minecraft.client.render.RenderLayer
import net.minecraft.client.render.VertexConsumerProvider
import net.minecraft.client.render.block.entity.BlockEntityRenderer
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.util.Identifier
import net.minecraft.util.math.Direction
import net.minecraft.util.math.RotationAxis

class FossilTubeRenderer(ctx: BlockEntityRendererFactory.Context) : BlockEntityRenderer<FossilTubeBlockEntity> {
    override fun render(
        entity: FossilTubeBlockEntity,
        tickDelta: Float,
        matrices: MatrixStack,
        vertexConsumers: VertexConsumerProvider,
        light: Int,
        overlay: Int
    ) {
        if (entity.multiblockStructure == null) {
            return
        }
        val struct = entity.multiblockStructure as FossilMultiblockStructure
        val connectionDir = struct.tubeConnectorDirection
        // FYI, rendering models this way ignores the pivots set in the model, so set the pivots manually
        when (connectionDir) {
            Direction.NORTH -> matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(0f), 0.5f, 0f, 0.5f)
            Direction.EAST -> matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(270f), 0.5f, 0f, 0.5f)
            Direction.SOUTH -> matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180f), 0.5f, 0f, 0.5f)
            Direction.WEST -> matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(90f), 0.5f, 0f, 0.5f)
            else -> {}
        }

        val cutoutBuffer = vertexConsumers.getBuffer(RenderLayer.getCutout())
        if (connectionDir != null) {
            matrices.push()
            CONNECTOR_MODEL.getQuads(entity.cachedState, null, entity.world?.random).forEach { quad ->
                cutoutBuffer.quad(matrices.peek(), quad, 0.75f, 0.75f, 0.75f, light, OverlayTexture.DEFAULT_UV)
            }
            matrices.pop()
        }
        val fillLevel = struct.fillLevel
        if (fillLevel == 0 && struct.createdPokemon == null) {
            return
        }

        if (struct.isRunning() or (struct.createdPokemon != null)) renderBaby(entity, tickDelta, matrices, vertexConsumers, light, overlay)

        matrices.push()
        val transparentBuffer = vertexConsumers.getBuffer(RenderLayer.getTranslucent())

        val fluidModel = if (struct.isRunning()) FLUID_MODELS[8]
        else if (struct.createdPokemon != null) FLUID_MODELS[7]
        else FLUID_MODELS[fillLevel-1]
        fluidModel.getQuads(entity.cachedState, null, entity.world?.random).forEach { quad ->
            transparentBuffer?.quad(matrices.peek(), quad, 0.75f, 0.75f, 0.75f, light, OverlayTexture.DEFAULT_UV)
        }

        matrices.pop()
    }

    private fun renderBaby(
        entity: FossilTubeBlockEntity,
        tickDelta: Float,
        matrices: MatrixStack,
        vertexConsumers: VertexConsumerProvider,
        light: Int,
        overlay: Int
    ) {
        val struc = entity.multiblockStructure as? FossilMultiblockStructure ?: return
        val fossil = struc.resultingFossil ?: return
        val timeRemaining = struc.timeRemaining

        val tankBlockState = entity.world?.getBlockState(entity.pos) ?: return
        if(tankBlockState.block != CobblemonBlocks.FOSSIL_TUBE) {
            // Block has been destroyed/replaced
            return
        }
        val tankDirection = tankBlockState.get(HorizontalFacingBlock.FACING)
        val struct = entity.multiblockStructure as FossilMultiblockStructure
        val connectionDir = struct.tubeConnectorDirection

        val aspects = emptySet<String>()
        val state = struc.fossilState
        state.updatePartialTicks(tickDelta)

        val completionPercentage = Math.min( 1 - timeRemaining / FossilMultiblockStructure.TIME_TO_TAKE.toFloat(), 1.0f)
        val fossilFetusModel = FossilModelRepository.getPoser(fossil.identifier, aspects)

        val modelList = mutableListOf<FossilModel>()
        val textureList = mutableListOf<Identifier>()
        val bodyOffsetYList = mutableListOf<Float>() // a*x + b
        val scaleBoundsList =  mutableListOf<Triple<Float, Float, Float>>() // minScale, maxScale
        val thresholdsList =  mutableListOf<Triple<Float, Float, Float>>() // minTime, maxTime

        EMBRYO_THRESHOLDS.forEachIndexed { idx, bounds ->
            if(completionPercentage >= bounds.first && completionPercentage <= bounds.third) {
                scaleBoundsList.add(EMBRYO_SCALE_BOUNDS[idx])
                thresholdsList.add(EMBRYO_THRESHOLDS[idx])
                if(idx < EMBRYO_IDENTIFIERS.size) {
                    // embryo models
                    val tempModel = FossilModelRepository.getPoser(EMBRYO_IDENTIFIERS[idx],aspects)
                    modelList.add(tempModel)
                    textureList.add(FossilModelRepository.getTexture(EMBRYO_IDENTIFIERS[idx], aspects, state.animationSeconds))
                    bodyOffsetYList.add(tempModel.yGrowthPoint)
                } else {
                    // fossil fetus model
                    modelList.add(fossilFetusModel)
                    textureList.add(FossilModelRepository.getTexture(fossil.identifier, aspects, state.animationSeconds))
                    bodyOffsetYList.add(fossilFetusModel.yGrowthPoint)
                }
            }
        }

        modelList.forEachIndexed() { idx, model ->
            val vertexConsumer = vertexConsumers.getBuffer(model.getLayer(textureList[idx]))
            val pose = model.poses.values.first()
            state.currentModel = model
            state.setPose(pose.poseName)
            state.timeEnteredPose = 0F

            val scale: Float = if (timeRemaining == 0) {
                model.maxScale
            } else {
                //TODO: DRY + Needs to be able to shrink models back down instead of disappearing when reaching max scale
                val startScale = scaleBoundsList[idx].first
                val maxScale = scaleBoundsList[idx].second
                val endScale = scaleBoundsList[idx].third

                val minTime = thresholdsList[idx].first
                val peakTime = thresholdsList[idx].second
                val endTime = thresholdsList[idx].third

                if(completionPercentage <= peakTime) {
                    ((completionPercentage - minTime) / (peakTime - minTime) * (maxScale - startScale) + startScale) * fossilFetusModel.maxScale
                } else {
                    (-1f * (completionPercentage - peakTime) / (endTime - peakTime) * (maxScale - endScale) + maxScale) * fossilFetusModel.maxScale
                }

            }

            matrices.push()
            matrices.translate(0.5, 1.0 + fossilFetusModel.yTranslation,  0.5);
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(180F))
            if(tankDirection.rotateCounterclockwise(Direction.Axis.Y) == connectionDir) {
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-90F))
            } else if(tankDirection == connectionDir) {
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180F))
            } else if(tankDirection.opposite != connectionDir) {
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(90F))
            }

            matrices.push()
            matrices.scale(scale, scale, scale)
            matrices.translate(0.0, bodyOffsetYList[idx].toDouble(), 0.0)
            model.setupAnimStateful(
                    entity = null,
                    state = state,
                    headYaw = 0F,
                    headPitch = 0F,
                    limbSwing = 0F,
                    limbSwingAmount = 0F,
                    ageInTicks = state.animationSeconds * 20
            )
            model.render(matrices, vertexConsumer, light, overlay, 1.0f, 1.0f, 1.0f, 1.0f)
            model.withLayerContext(vertexConsumers, state, FossilModelRepository.getLayers(fossil.identifier, aspects)) {
                model.render(matrices, vertexConsumer, light, OverlayTexture.DEFAULT_UV, 1F, 1F, 1F, 1F)
            }
            model.setDefault()
            matrices.pop()
            matrices.pop()

        }
    }

    companion object {
        val FLUID_MODELS = listOf(
            CobblemonBakingOverrides.FOSSIL_FLUID_CHUNKED_1.getModel(),
            CobblemonBakingOverrides.FOSSIL_FLUID_CHUNKED_2.getModel(),
            CobblemonBakingOverrides.FOSSIL_FLUID_CHUNKED_3.getModel(),
            CobblemonBakingOverrides.FOSSIL_FLUID_CHUNKED_4.getModel(),
            CobblemonBakingOverrides.FOSSIL_FLUID_CHUNKED_5.getModel(),
            CobblemonBakingOverrides.FOSSIL_FLUID_CHUNKED_6.getModel(),
            CobblemonBakingOverrides.FOSSIL_FLUID_CHUNKED_7.getModel(),
            CobblemonBakingOverrides.FOSSIL_FLUID_CHUNKED_8.getModel(),
            CobblemonBakingOverrides.FOSSIL_FLUID_BUBBLING.getModel()
        )

        val CONNECTOR_MODEL = CobblemonBakingOverrides.RESTORATION_TANK_CONNECTOR.getModel()
        val EMBRYO_IDENTIFIERS = listOf<Identifier>(Identifier("cobblemon", "embryo_stage1"),
                Identifier("cobblemon", "embryo_stage2"),
                Identifier("cobblemon", "embryo_stage3"))
        //TODO: Is this something that should be configurable per fossil?
        val EMBRYO_THRESHOLDS = listOf(Triple(0f, 0.1f, 0.2f), Triple(0f,  0.2f,0.55f), Triple(0.1f, 0.6f, 0.85f), Triple(0.4f, 1.0f, 1.0f))
        val EMBRYO_SCALE_BOUNDS = listOf(Triple(0.2f, 0.5f, 0.0f), Triple(0f, 0.9f, 0.2f), Triple(0.1f, 1.1f, 0.0f), Triple(0.2f, 1.0f, 1.0f))
    }
}