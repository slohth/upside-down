package dev.slohth.upsidedown

import dev.slohth.upsidedown.profile.Profile.Companion.profile
import gg.flyte.twilight.Twilight
import gg.flyte.twilight.event.event
import gg.flyte.twilight.twilight
import org.bukkit.Bukkit
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scoreboard.Scoreboard
import org.bukkit.scoreboard.Team
import revxrsal.commands.bukkit.BukkitCommandHandler

class UpsideDown : JavaPlugin() {

    lateinit var twilight: Twilight
    lateinit var scoreboard: Scoreboard

    override fun onEnable() {
        twilight = twilight(this)

        val handler = BukkitCommandHandler.create(this)
        handler.registerBrigadier()
        handler.register(TestCommand())

        scoreboard = Bukkit.getScoreboardManager().newScoreboard
        TEAM = scoreboard.registerNewTeam("default")
        TEAM!!.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.NEVER)

        event<PlayerJoinEvent> {
            player.scoreboard = scoreboard
            TEAM!!.addPlayer(player)
            player.profile()
        }

        event<PlayerQuitEvent> {
            player.profile().remove()
        }
    }

    companion object {
        var TEAM: Team? = null
    }

}