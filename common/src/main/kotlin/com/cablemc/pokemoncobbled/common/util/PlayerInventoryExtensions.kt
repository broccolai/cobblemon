/*
 * Copyright (C) 2022 Pokemon Cobbled Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cablemc.pokemoncobbled.common.util

import net.minecraft.entity.player.PlayerInventory
import net.minecraft.item.ItemStack
import java.util.function.Predicate
import java.util.stream.Collectors

fun PlayerInventory.removeAmountIf(amount: Int, rule: Predicate<ItemStack>) {
    this.combinedInventory.forEach {
        var index = 0
        val matches = it.stream()
            .map { a -> Pair(index++, a) }
            .filter { rule.test(it.second) }
            .collect(Collectors.toList())

        var remaining = amount
        while (remaining > 0) {
            val element = matches.removeFirstOrNull() ?: return@forEach

            val result = this.removeStack(element.first, amount)
            remaining -= result.count
        }
    }
}