/*
 * Copyright (C) 2022 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.pokemon

import com.cobblemon.mod.common.Cobblemon
import com.cobblemon.mod.common.api.abilities.AbilityPool
import com.cobblemon.mod.common.api.data.ClientDataSynchronizer
import com.cobblemon.mod.common.api.drop.DropTable
import com.cobblemon.mod.common.api.pokemon.effect.ShoulderEffect
import com.cobblemon.mod.common.api.pokemon.egg.EggGroup
import com.cobblemon.mod.common.api.pokemon.evolution.Evolution
import com.cobblemon.mod.common.api.pokemon.evolution.PreEvolution
import com.cobblemon.mod.common.api.pokemon.experience.ExperienceGroups
import com.cobblemon.mod.common.api.pokemon.moves.Learnset
import com.cobblemon.mod.common.api.pokemon.stats.Stat
import com.cobblemon.mod.common.api.types.ElementalType
import com.cobblemon.mod.common.api.types.ElementalTypes
import com.cobblemon.mod.common.entity.PoseType.Companion.FLYING_POSES
import com.cobblemon.mod.common.entity.PoseType.Companion.SWIMMING_POSES
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity
import com.cobblemon.mod.common.net.IntSize
import com.cobblemon.mod.common.pokemon.ai.PokemonBehaviour
import com.cobblemon.mod.common.util.lang
import com.cobblemon.mod.common.util.readSizedInt
import com.cobblemon.mod.common.util.writeSizedInt
import net.minecraft.entity.EntityDimensions
import net.minecraft.network.PacketByteBuf
import net.minecraft.text.MutableText
import net.minecraft.util.Identifier

class Species : ClientDataSynchronizer<Species> {
    var name: String = "bulbasaur"
    val translatedName: MutableText
        get() = lang("species.$name.name")
    var nationalPokedexNumber = 1

    var baseStats = hashMapOf<Stat, Int>()
        private set
    /** The ratio of the species being male. If -1, the Pokémon is genderless. */
    var maleRatio: Float = 0.5F
        private set
    var catchRate = 45
        private set
    // Only modifiable for debugging sizes
    var baseScale = 1F
    var baseExperienceYield = 10
    var baseFriendship = 0
    var evYield = hashMapOf<Stat, Int>()
        private set
    var experienceGroup = ExperienceGroups.first()
    var hitbox = EntityDimensions(1F, 1F, false)
    var primaryType = ElementalTypes.GRASS
        internal set
    var secondaryType: ElementalType? = null
        internal set
    var abilities = AbilityPool()
        private set
    var shoulderMountable: Boolean = false
        private set
    var shoulderEffects = mutableListOf<ShoulderEffect>()
        private set
    var moves = Learnset()
        private set
    var features = mutableSetOf<String>()
        private set
    private var standingEyeHeight: Float? = null
    private var swimmingEyeHeight: Float? = null
    private var flyingEyeHeight: Float? = null
    var behaviour = PokemonBehaviour()
        private set
    var pokedex = mutableListOf<String>()
        private set
    var drops = DropTable()
        private set
    var eggCycles = 120
        private set
    var eggGroups = hashSetOf<EggGroup>()
        private set
    var dynamaxBlocked = false
    var implemented = false

    /**
     * The height in decimeters
     */
    var height = 1F
        private set

    /**
     * The weight in hectograms
     */
    var weight = 1F
        private set

    var forms = mutableListOf<FormData>()
        private set

    val standardForm by lazy { FormData().initialize(this) }

    internal var labels = hashSetOf<String>()
        private set

    // Only exists for use of the field in Pokémon do not expose to end user due to how the species/form data is structured
    internal var evolutions: MutableSet<Evolution> = hashSetOf()
        private set

    internal var preEvolution: PreEvolution? = null
        private set

    @Transient
    lateinit var resourceIdentifier: Identifier

    fun initialize() {
        Cobblemon.statProvider.provide(this)
        if (this.forms.isNotEmpty() && this.forms.none { it.name == this.standardForm.name }) {
            this.forms.add(0, this.standardForm)
        }
        this.forms.forEach { it.initialize(this) }
        // These properties are lazy, these need all species to be reloaded but SpeciesAdditions is what will eventually trigger this after all species have been loaded
        this.preEvolution?.species
        this.preEvolution?.form
        this.evolutions.size
    }

    fun create(level: Int = 10) = Pokemon().apply {
        species = this@Species
        this.level = level
        initialize()
    }

    fun getForm(aspects: Set<String>) = forms.firstOrNull { it.aspects.all { it in aspects } } ?: standardForm

    fun eyeHeight(entity: PokemonEntity): Float {
        val multiplier = this.resolveEyeHeight(entity) ?: VANILLA_DEFAULT_EYE_HEIGHT
        return entity.height * multiplier
    }

    private fun resolveEyeHeight(entity: PokemonEntity): Float? = when {
        entity.getPoseType() in SWIMMING_POSES -> this.swimmingEyeHeight ?: standingEyeHeight
        entity.getPoseType() in FLYING_POSES -> this.flyingEyeHeight ?: standingEyeHeight
        else -> this.standingEyeHeight
    }

    override fun encode(buffer: PacketByteBuf) {
        buffer.writeIdentifier(this.resourceIdentifier)
        buffer.writeBoolean(this.implemented)
        buffer.writeString(this.name)
        buffer.writeInt(this.nationalPokedexNumber)
        buffer.writeMap(this.baseStats,
            { keyBuffer, stat -> Cobblemon.statProvider.encode(keyBuffer, stat)},
            { valueBuffer, value -> valueBuffer.writeSizedInt(IntSize.U_SHORT, value) }
        )
        // ToDo remake once we have custom typing support
        buffer.writeString(this.primaryType.name)
        buffer.writeNullable(this.secondaryType) { pb, type -> pb.writeString(type.name) }
        buffer.writeCollection(this.pokedex) { pb, line -> pb.writeString(line) }
        buffer.writeCollection(this.forms) { pb, form -> form.encode(pb) }
        buffer.writeString(this.experienceGroup.name)
        this.moves.encode(buffer)
        buffer.writeFloat(baseScale)
        // Hitbox start
        buffer.writeFloat(hitbox.width)
        buffer.writeFloat(hitbox.height)
        buffer.writeBoolean(hitbox.fixed)
        // Hitbox end
    }

    override fun decode(buffer: PacketByteBuf) {
        this.implemented = buffer.readBoolean()
        // identifier is decoded in the sync packet for easier debug log
        this.name = buffer.readString()
        this.nationalPokedexNumber = buffer.readInt()
        this.baseStats.putAll(buffer.readMap(
            { keyBuffer -> Cobblemon.statProvider.decode(keyBuffer) },
            { valueBuffer -> valueBuffer.readSizedInt(IntSize.U_SHORT) })
        )
        this.primaryType = ElementalTypes.getOrException(buffer.readString())
        this.secondaryType = buffer.readNullable { pb -> ElementalTypes.getOrException(pb.readString()) }
        this.pokedex.clear()
        this.pokedex += buffer.readList { pb -> pb.readString() }
        this.forms.clear()
        this.forms += buffer.readList{ pb -> FormData().apply { decode(pb) } }.filterNotNull()
        this.experienceGroup = ExperienceGroups.findByName(buffer.readString())!!
        this.moves.decode(buffer)
        this.baseScale = buffer.readFloat()
        this.hitbox = EntityDimensions(buffer.readFloat(), buffer.readFloat(), buffer.readBoolean())
    }

    override fun shouldSynchronize(other: Species): Boolean {
        if (other.resourceIdentifier.toString() != other.resourceIdentifier.toString())
            return false
        return other.name != this.name
                || other.nationalPokedexNumber != this.nationalPokedexNumber
                || other.baseStats != this.baseStats
                || other.hitbox != this.hitbox
                || other.primaryType != this.primaryType
                || other.secondaryType != this.secondaryType
                || other.standingEyeHeight != this.standingEyeHeight
                || other.swimmingEyeHeight != this.swimmingEyeHeight
                || other.flyingEyeHeight != this.flyingEyeHeight
                || other.dynamaxBlocked != this.dynamaxBlocked
                || other.pokedex != this.pokedex
                || other.forms != this.forms
                // We only sync level up moves atm
                || this.moves.shouldSynchronize(other.moves)
    }

    override fun toString() = name

    companion object {
        private const val VANILLA_DEFAULT_EYE_HEIGHT = .85F
    }
}