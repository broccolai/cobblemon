/*
 * Copyright (C) 2022 Pokemod Cobbled Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cablemc.pokemod.common.pokemon.status

import com.cablemc.pokemod.common.api.pokemon.status.Status
import net.minecraft.util.Identifier

/**
 * Represents a status that only remains during a battle.
 *
 * @author Deltric
 */
class VolatileStatus(name: Identifier) : Status(name, "", "", null)