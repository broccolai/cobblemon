/*
 * Copyright (C) 2022 Pokemod Cobbled Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cablemc.pokemod.common.client

import com.cablemc.pokemod.common.Pokemod.LOGGER
import com.cablemc.pokemod.common.PokemodBlockEntities
import com.cablemc.pokemod.common.PokemodBlocks
import com.cablemc.pokemod.common.PokemodClientImplementation
import com.cablemc.pokemod.common.PokemodItems
import com.cablemc.pokemod.common.api.scheduling.ScheduledTaskTracker
import com.cablemc.pokemod.common.client.battle.ClientBattle
import com.cablemc.pokemod.common.client.gui.PartyOverlay
import com.cablemc.pokemod.common.client.gui.battle.BattleOverlay
import com.cablemc.pokemod.common.client.keybind.PokemodKeybinds
import com.cablemc.pokemod.common.client.net.ClientPacketRegistrar
import com.cablemc.pokemod.common.client.render.block.HealingMachineRenderer
import com.cablemc.pokemod.common.client.render.layer.PokemonOnShoulderRenderer
import com.cablemc.pokemod.common.client.render.models.blockbench.bedrock.animation.BedrockAnimationRepository
import com.cablemc.pokemod.common.client.render.models.blockbench.repository.PokeBallModelRepository
import com.cablemc.pokemod.common.client.render.models.blockbench.repository.PokemonModelRepository
import com.cablemc.pokemod.common.client.render.pokeball.PokeBallRenderer
import com.cablemc.pokemod.common.client.render.pokemon.PokemonRenderer
import com.cablemc.pokemod.common.client.starter.ClientPlayerData
import com.cablemc.pokemod.common.client.storage.ClientStorageManager
import com.cablemc.pokemod.common.data.CobbledDataProvider
import dev.architectury.event.events.client.ClientGuiEvent
import dev.architectury.event.events.client.ClientPlayerEvent.CLIENT_PLAYER_JOIN
import dev.architectury.event.events.client.ClientPlayerEvent.CLIENT_PLAYER_QUIT
import dev.architectury.registry.client.rendering.BlockEntityRendererRegistry
import dev.architectury.registry.client.rendering.ColorHandlerRegistry
import dev.architectury.registry.client.rendering.RenderTypeRegistry
import net.minecraft.client.color.block.BlockColorProvider
import net.minecraft.client.color.item.ItemColorProvider
import net.minecraft.client.render.RenderLayer
import net.minecraft.client.render.entity.EntityRenderer
import net.minecraft.client.render.entity.EntityRendererFactory
import net.minecraft.client.render.entity.LivingEntityRenderer
import net.minecraft.client.render.entity.model.PlayerEntityModel
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.resource.ResourceManager

object PokemodClient {
    lateinit var implementation: PokemodClientImplementation
    val storage = ClientStorageManager()
    var battle: ClientBattle? = null
    var clientPlayerData = ClientPlayerData()
    /** If true then we won't bother them anymore about choosing a starter even if it's a thing they can do. */
    var checkedStarterScreen = false

    lateinit var overlay: PartyOverlay
    lateinit var battleOverlay: BattleOverlay

    fun onLogin() {
        clientPlayerData = ClientPlayerData()
        storage.onLogin()
        CobbledDataProvider.canReload = false
    }

    fun onLogout() {
        storage.onLogout()
        battle = null
        battleOverlay = BattleOverlay()
        ScheduledTaskTracker.clear()
        checkedStarterScreen = false
        CobbledDataProvider.canReload = true
    }

    fun initialize(implementation: PokemodClientImplementation) {
        LOGGER.info("Initializing Pokémon Cobbled client")
        this.implementation = implementation

        CLIENT_PLAYER_JOIN.register { onLogin() }
        CLIENT_PLAYER_QUIT.register { onLogout() }

        overlay = PartyOverlay()
        battleOverlay = BattleOverlay()

        ClientPacketRegistrar.registerHandlers()
        PokemodKeybinds.register()

        ClientGuiEvent.RENDER_HUD.register(ClientGuiEvent.RenderHud { _, _ -> ScheduledTaskTracker.update() })

//        ReloadListenerRegistry.register(ResourceType.CLIENT_RESOURCES, object : ResourceReloader {
//            override fun getName() = "cobbled"
//            override fun reload(
//                synchronizer: ResourceReloader.Synchronizer?,
//                manager: ResourceManager,
//                prepareProfiler: Profiler?,
//                applyProfiler: Profiler?,
//                prepareExecutor: Executor?,
//                applyExecutor: Executor?
//            ): CompletableFuture<Void> {
//                return CompletableFuture.supplyAsync {
//                    reloadCodedAssets(manager)
//                    null
//                }
////                return CompletableFuture.completedFuture(null)
//            }
//        })

        LOGGER.info("Initializing PokéBall models")
        PokeBallModelRepository.init()

        BlockEntityRendererRegistry.register(PokemodBlockEntities.HEALING_MACHINE.get(), ::HealingMachineRenderer)

        registerBlockRenderTypes()
        registerColors()
    }

    fun registerColors() {
        ColorHandlerRegistry.registerBlockColors(BlockColorProvider { blockState, blockAndTintGetter, blockPos, i ->
            return@BlockColorProvider 0x71c219
        }, com.cablemc.pokemod.common.PokemodBlocks.APRICORN_LEAVES.get())

        ColorHandlerRegistry.registerItemColors(ItemColorProvider { itemStack, i ->
            return@ItemColorProvider 0x71c219
        }, PokemodItems.APRICORN_LEAVES.get())
    }

    private fun registerBlockRenderTypes() {
        RenderTypeRegistry.register(RenderLayer.getCutout(),
            PokemodBlocks.APRICORN_DOOR.get(),
            PokemodBlocks.APRICORN_TRAPDOOR.get(),
            PokemodBlocks.BLACK_APRICORN_SAPLING.get(),
            PokemodBlocks.BLUE_APRICORN_SAPLING.get(),
            PokemodBlocks.GREEN_APRICORN_SAPLING.get(),
            PokemodBlocks.PINK_APRICORN_SAPLING.get(),
            PokemodBlocks.RED_APRICORN_SAPLING.get(),
            PokemodBlocks.WHITE_APRICORN_SAPLING.get(),
            PokemodBlocks.YELLOW_APRICORN_SAPLING.get(),
            PokemodBlocks.BLACK_APRICORN.get(),
            PokemodBlocks.BLUE_APRICORN.get(),
            PokemodBlocks.GREEN_APRICORN.get(),
            PokemodBlocks.PINK_APRICORN.get(),
            PokemodBlocks.RED_APRICORN.get(),
            PokemodBlocks.WHITE_APRICORN.get(),
            PokemodBlocks.YELLOW_APRICORN.get(),
            PokemodBlocks.HEALING_MACHINE.get())
    }

    fun beforeChatRender(matrixStack: MatrixStack, partialDeltaTicks: Float) {
        if (battle == null) {
            overlay.render(matrixStack, partialDeltaTicks)
        } else {
            battleOverlay.render(matrixStack, partialDeltaTicks)
        }
    }

    fun onAddLayer(skinMap: Map<String, EntityRenderer<out PlayerEntity>>?) {
        var renderer: LivingEntityRenderer<PlayerEntity, PlayerEntityModel<PlayerEntity>>? = skinMap?.get("default") as LivingEntityRenderer<PlayerEntity, PlayerEntityModel<PlayerEntity>>
        renderer?.addFeature(PokemonOnShoulderRenderer(renderer))
        renderer = skinMap.get("slim") as LivingEntityRenderer<PlayerEntity, PlayerEntityModel<PlayerEntity>>?
        renderer?.addFeature(PokemonOnShoulderRenderer(renderer))
    }

    fun registerPokemonRenderer(context: EntityRendererFactory.Context): PokemonRenderer {
        LOGGER.info("Registering Pokémon renderer")
        PokemonModelRepository.initializeModels(context)
        return PokemonRenderer(context)
    }

    fun registerPokeBallRenderer(context: EntityRendererFactory.Context): PokeBallRenderer {
        LOGGER.info("Registering PokéBall renderer")
        PokeBallModelRepository.initializeModels(context)
        return PokeBallRenderer(context)
    }

    fun reloadCodedAssets(resourceManager: ResourceManager) {
        LOGGER.info("Reloading assets")
        BedrockAnimationRepository.clear()
        PokemonModelRepository.reload(resourceManager)
        LOGGER.info("Loaded assets")
//        PokeBallModelRepository.reload(resourceManager)
    }

    fun endBattle() {
        battle = null
    }
}