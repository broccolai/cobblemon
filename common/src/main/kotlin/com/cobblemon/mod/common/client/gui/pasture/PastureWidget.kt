package com.cobblemon.mod.common.client.gui.pasture

import com.cobblemon.mod.common.client.gui.summary.widgets.SoundlessWidget
import com.cobblemon.mod.common.net.messages.client.pasture.OpenPasturePacket
import com.cobblemon.mod.common.net.messages.server.pasture.UnpasturePokemonPacket
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.text.Text

class PastureWidget(
    val pasturePCGUIConfiguration: PasturePCGUIConfiguration,
    x: Int,
    y: Int
): SoundlessWidget(
    x, y, WIDTH, HEIGHT, Text.literal("PastureWidget")
) {
    companion object {
        const val WIDTH = 82
        const val HEIGHT = 176
    }

    var pokemon: OpenPasturePacket.PasturePokemonDataDTO? = null
    val unpastureButton = UnpastureButton(
        x = x + 194,
        y = y + 124,
        configuration = pasturePCGUIConfiguration
    ) {
        UnpasturePokemonPacket(pasturePCGUIConfiguration.pastureId, pokemon!!.pokemonId)
    }

    override fun renderButton(matrices: MatrixStack, mouseX: Int, mouseY: Int, delta: Float) {

        if (pokemon != null) {
            unpastureButton.render(matrices, mouseX, mouseY, delta)
        }
    }
}