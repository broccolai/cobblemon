package com.cablemc.pokemoncobbled.common.command.argument

import com.cablemc.pokemoncobbled.common.PokemonCobbled
import com.cablemc.pokemoncobbled.common.pokemon.Pokemon
import com.mojang.brigadier.StringReader
import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.exceptions.CommandSyntaxException
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.SharedSuggestionProvider
import net.minecraft.network.chat.TranslatableComponent
import net.minecraft.server.level.ServerPlayer
import java.util.concurrent.CompletableFuture

class PartySlotArgumentType : ArgumentType<Int> {

    override fun parse(reader: StringReader): Int {
        val slot = reader.readInt()
        return when {
            slot < MIN -> throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.integerTooLow().createWithContext(reader, slot, MIN)
            slot > MAX -> throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.integerTooHigh().createWithContext(reader, slot, MAX)
            else -> slot
        }
    }

    override fun <S : Any> listSuggestions(context: CommandContext<S>, builder: SuggestionsBuilder): CompletableFuture<Suggestions> = SharedSuggestionProvider.suggest(EXAMPLES, builder)

    override fun getExamples() = EXAMPLES

    companion object {

        private const val MIN = 1
        private const val MAX = 6
        private val EXAMPLES = (MIN..MAX).map { it.toString() }
        private val INVALID_SLOT = DynamicCommandExceptionType { slot -> TranslatableComponent("pokemoncobbled.command.general.invalid-party-slot", slot) }

        fun partySlot() = PartySlotArgumentType()

        fun <S> getPokemon(context: CommandContext<S>, name: String): Pokemon {
            val slot = context.getArgument(name, Int::class.java)
            val source = context.source as? CommandSourceStack ?: throw CommandSourceStack.ERROR_NOT_PLAYER.create()
            val player = source.entity as? ServerPlayer ?: throw CommandSourceStack.ERROR_NOT_PLAYER.create()
            val party = PokemonCobbled.storage.getParty(player)
            return party.get(slot - 1) ?: throw INVALID_SLOT.create(slot)
        }
    }

}