/*
 * Copyright (C) 2022 Pokemod Cobbled Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cablemc.pokemod.common.util

import com.cablemc.pokemod.common.Pokemod
import com.mojang.brigadier.StringReader
import com.mojang.brigadier.exceptions.CommandSyntaxException
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import net.minecraft.util.InvalidIdentifierException

fun Identifier.extractTo(directory : File) {
    val stream = Pokemod::class.java.getResourceAsStream(String.format("/assets/%s/%s", namespace, path))
        ?: throw Exception("Could not read $this")
    Files.copy(stream, directory.toPath(), StandardCopyOption.REPLACE_EXISTING)
    stream.close()
}

/**
 * Creates an identifier from this string, if a namespace is not present the default [namespace] will be used.
 *
 * @param namespace The namespace that will default if none is present, defaults to [Pokemod.MODID].
 */
fun String.asIdentifierDefaultingNamespace(namespace: String = Pokemod.MODID): Identifier {
    val id = this.lowercase()
    return if (id.contains(":")) Identifier(id.substringBefore(":"), id.substringAfter(":")) else Identifier(namespace, id)
}

/**
 * Attempts to parse an [Identifier] from the [StringReader].
 * Unlike [Identifier.fromCommandInput] this will default the namespace to the given [namespace] if non-present.
 * This is useful for when we want to automatically assign an identifier to our mod.
 *
 * @throws CommandSyntaxException If the raw data is not a valid identifier.
 *
 * @param namespace The [Identifier.namespace] being assigned if none is present.
 * @return The parsed [Identifier].
 */
fun StringReader.asIdentifierDefaultingNamespace(namespace: String = Pokemod.MODID): Identifier {
    val start = this.cursor
    while (this.canRead() && Identifier.isCharValid(this.peek())) {
        this.skip()
    }
    val raw = this.string.substring(start, this.cursor)
    try {
        return raw.asIdentifierDefaultingNamespace(namespace)
    } catch (e: InvalidIdentifierException) {
        this.cursor = start
        throw SimpleCommandExceptionType(Text.translatable("argument.id.invalid")).createWithContext(this)
    }
}