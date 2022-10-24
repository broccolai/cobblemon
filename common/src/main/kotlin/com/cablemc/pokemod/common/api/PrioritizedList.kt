/*
 * Copyright (C) 2022 Pokemod Cobbled Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cablemc.pokemod.common.api

/**
 * Functions like a list which is ordered by [Priority]. Within each category of priority,
 * order is first-come-first-served. Having the same value in multiple priorities is possible
 * but can cause stickiness.
 *
 * @author Hiroku
 * @since February 18th, 2022
 */
open class PrioritizedList<T> : Iterable<T> {
    protected val priorityMap = mutableMapOf<Priority, MutableList<T>>()
    protected val ordered = mutableListOf<T>()

    val mapping: Map<Priority, List<T>>
        get() = priorityMap.toMap()

    private fun reorder() {
        ordered.clear()
        Priority.values().forEach {
            priorityMap[it]?.let { ordered.addAll(it) }
        }
    }

    fun add(priority: Priority, value: T) {
        priorityMap.putIfAbsent(priority, mutableListOf())
        priorityMap[priority]?.add(value)
        reorder()
    }

    fun remove(value: T) {
        priorityMap.values.forEach { it.remove(value) }
        reorder()
    }

    fun remove(priority: Priority, value: T) {
        priorityMap[priority]?.remove(value)
        reorder()
    }

    fun clear() {
        priorityMap.clear()
        ordered.clear()
    }

    fun isEmpty() = ordered.isEmpty()

    override fun iterator() = ordered.iterator()
}