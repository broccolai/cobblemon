/*
 * Copyright (C) 2022 Pokemod Cobbled Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cablemc.pokemod.common.battles.runner

import java.io.File
import java.io.IOException

/**
 * Modified from https://lankydan.dev/running-a-kotlin-class-as-a-subprocess
 *
 * Spawns a process and returns it
 */
@Throws(IOException::class, InterruptedException::class)
fun exec(clazz: Class<*>, args: List<String> = emptyList(), jvmArgs: List<String> = emptyList()): Process {
    val javaHome = System.getProperty("java.home")
    val javaBin = javaHome + File.separator + "bin" + File.separator + "java"
    val classpath = System.getProperty("java.class.path")
    val className = clazz.name

    val command = ArrayList<String>()
    command.add(javaBin)
    command.addAll(jvmArgs)
    command.add("-cp")
    command.add(classpath)
    command.add(className)
    command.addAll(args)

    val builder = ProcessBuilder(command)
    return builder.inheritIO().start()
}