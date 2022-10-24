/*
 * Copyright (C) 2022 Pokemod Cobbled Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cablemc.pokemod.common.client.keybind

import com.cablemc.pokemod.common.client.keybind.keybinds.DownShiftPartyBinding
import com.cablemc.pokemod.common.client.keybind.keybinds.HidePartyBinding
import com.cablemc.pokemod.common.client.keybind.keybinds.PartySendBinding
import com.cablemc.pokemod.common.client.keybind.keybinds.PokeNavigatorBinding
import com.cablemc.pokemod.common.client.keybind.keybinds.UpShiftPartyBinding
import dev.architectury.registry.client.keymappings.KeyMappingRegistry

/**
 * Main registry for Keybinds
 *
 * @author Qu
 * @since 2022-02-17
 */
object PokemodKeybinds {

    private val keybinds = mutableListOf<PokemodKeyBinding>()
    fun register() {
        registerKeybind(HidePartyBinding)
        registerKeybind(PokeNavigatorBinding)
        registerKeybind(DownShiftPartyBinding)
        registerKeybind(PartySendBinding)
        registerKeybind(UpShiftPartyBinding)

        keybinds.forEach { KeyMappingRegistry.register(it) }
    }

    fun onAnyKey(key: Int, scanCode: Int, action: Int, modifiers: Int) {
        keybinds.toMutableList().forEach {
            it.onKeyInput()
        }
    }

    fun getAllKeybinds() = keybinds

    private fun registerKeybind(keybind: PokemodKeyBinding): PokemodKeyBinding {
        return keybind.also {
            keybinds.add(it)
        }
    }
}