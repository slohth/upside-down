package dev.slohth.upsidedown

import dev.slohth.upsidedown.profile.Profile.Companion.profile
import dev.slohth.upsidedown.skin.Skin
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.entity.Player
import revxrsal.commands.annotation.Command
import java.util.*

class TestCommand {

    val upsideDowners: MutableSet<UUID> = HashSet()

    @Command("upsidedown")
    fun upsideDown(sender: Player) {
        if (upsideDowners.contains(sender.uniqueId)) {

            upsideDowners.remove(sender.uniqueId)
            sender.profile().upsideDown(false)
            sender.sendMessage(Component.text("You are no longer upside down!", NamedTextColor.RED))

        } else {

            upsideDowners.add(sender.uniqueId)
            sender.profile().upsideDown(true)
            sender.sendMessage(Component.text("You are now upside down!", NamedTextColor.GREEN))

        }
    }

    @Command("skin")
    fun skin(sender: Player, input: String) {

        val skin: Skin? = if (input.length <= 16) { Skin.of(input) } else { Skin.of(UUID.fromString(input)) }
        sender.profile().skin(skin ?: Skin.STEVE)
        sender.sendMessage(Component.text("Your skin has been changed!", NamedTextColor.GREEN))

    }

    @Command("reset")
    fun reset(sender: Player) {
        sender.profile().reset()
        sender.sendMessage(Component.text("Reset to default values!", NamedTextColor.YELLOW))
    }

}