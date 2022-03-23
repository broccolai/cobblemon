package com.cablemc.pokemoncobbled.common.pokemon

import com.cablemc.pokemoncobbled.common.api.abilities.AbilityTemplate
import com.cablemc.pokemoncobbled.common.api.pokemon.effect.ShoulderEffect
import com.cablemc.pokemoncobbled.common.api.pokemon.evolution.Evolution
import com.cablemc.pokemoncobbled.common.api.pokemon.evolution.PreEvolution
import com.cablemc.pokemoncobbled.common.api.types.ElementalType
import com.google.gson.annotations.SerializedName
import net.minecraft.world.entity.EntityDimensions

data class FormData(
    @SerializedName("name")
    val name: String = "normal",
    @SerializedName("baseStats")
    private val _baseStats: PokemonStats? = null,
    @SerializedName("maleRatio")
    private val _maleRatio: Float? = null,
    @SerializedName("baseScale")
    private var _baseScale: Float? = null,
    @SerializedName("hitbox")
    private var _hitbox: EntityDimensions? = null,
    @SerializedName("catchRate")
    private var _catchRate: Int? = null,
    @SerializedName("primaryType")
    private val _primaryType: ElementalType? = null,
    @SerializedName("secondaryType")
    private val _secondaryType: ElementalType? = null,
    @SerializedName("standardAbilities")
    private val _standardAbilities: List<AbilityTemplate>? = null,
    @SerializedName("hiddenAbility")
    private val _hiddenAbility: AbilityTemplate? = null,
    @SerializedName("shoulderMountable")
    private val _shoulderMountable: Boolean? = null,
    @SerializedName("shoulderEffects")
    private val _shoulderEffects: MutableList<ShoulderEffect>? = null,
    @SerializedName("evolutions")
    private val _evolutions: MutableSet<Evolution>? = null,
    val preEvolution: PreEvolution? = null
) {

    val baseStats: PokemonStats
        get() = _baseStats ?: species.baseStats

    val maleRatio: Float
        get() = _maleRatio ?: species.maleRatio
    val baseScale: Float
        get() = _baseScale ?: species.baseScale
    val hitbox: EntityDimensions
        get() = _hitbox ?: species.hitbox
    val catchRate: Int
        get() = _catchRate ?: species.catchRate

    val primaryType: ElementalType
        get() = _primaryType ?: species.primaryType

    val secondaryType: ElementalType?
        get() = _secondaryType ?: species.secondaryType

    val standardAbilities: List<AbilityTemplate>
        get() = _standardAbilities ?: species.standardAbilities

    val hiddenAbility: AbilityTemplate?
        get() = _hiddenAbility

    val shoulderMountable: Boolean
        get() = _shoulderMountable ?: species.shoulderMountable

    val shoulderEffects: MutableList<ShoulderEffect>
        get() = _shoulderEffects ?: species.shoulderEffects

    val types: Iterable<ElementalType>
        get() = secondaryType?.let { listOf(primaryType, it) } ?: listOf(primaryType)

    val evolutions: MutableSet<Evolution>
        get() = _evolutions ?: species.evolutions

    @Transient
    lateinit var species: Species
}