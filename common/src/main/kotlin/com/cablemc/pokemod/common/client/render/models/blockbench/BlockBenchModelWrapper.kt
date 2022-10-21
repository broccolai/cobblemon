/*
 * Copyright (C) 2022 Pokemod Cobbled Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cablemc.pokemod.common.client.render.models.blockbench

import com.cablemc.pokemod.common.client.PokemodClient
import net.minecraft.client.model.ModelPart
import net.minecraft.client.model.TexturedModelData
import net.minecraft.client.render.entity.EntityRendererFactory
import net.minecraft.client.render.entity.model.EntityModel
import net.minecraft.client.render.entity.model.EntityModelLayer
import net.minecraft.entity.Entity

/**
 * Wraps around a model generated by BlockBench for easy registration in repositories.
 */
class BlockBenchModelWrapper<T : Entity>(
    val layerLocation: EntityModelLayer,
    val texturedModelDataSupplier: () -> TexturedModelData,
    val modelFactory: (ModelPart) -> EntityModel<T>
) {
    lateinit var entityModel: EntityModel<T>
    var isModelInitialized = false
        private set
    var isLayerInitialized = false
        private set

    fun initializeModel(context: EntityRendererFactory.Context) {
        entityModel = modelFactory(context.modelLoader.getModelPart(layerLocation)).also {
            if (it is PoseableEntityModel<T>) {
                it.registerPoses()
            }
        }
        isModelInitialized = true
    }

    fun initializeModelLayers() {
        if (!isLayerInitialized) {
            PokemodClient.implementation.registerLayer(layerLocation, texturedModelDataSupplier)
            isLayerInitialized = true
        }
    }
}