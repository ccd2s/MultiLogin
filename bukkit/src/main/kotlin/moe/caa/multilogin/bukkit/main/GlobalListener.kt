package moe.caa.multilogin.bukkit.main

import moe.caa.multilogin.api.internal.handle.HandleResult
import moe.caa.multilogin.bukkit.impl.BukkitPlayer
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerLoginEvent
import org.bukkit.event.player.PlayerQuitEvent

class GlobalListener(private val multiLoginBukkit: MultiLoginBukkit) : Listener {

    @EventHandler
    fun onJoin(event: PlayerLoginEvent) {
        val result = multiLoginBukkit.multiCoreAPI!!.playerHandler.pushPlayerJoinGame(
            event.player.uniqueId, event.player.name
        )
        if (result.type == HandleResult.Type.KICK) {
            val message = result.kickMessage
            if (message.isNullOrBlank()) {
                event.disallow(PlayerLoginEvent.Result.KICK_OTHER, "")
            } else {
                event.disallow(PlayerLoginEvent.Result.KICK_OTHER, message)
            }
            return
        }
        multiLoginBukkit.multiCoreAPI!!.playerHandler.callPlayerJoinGame(BukkitPlayer(event.player))
    }

    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {
        multiLoginBukkit.multiCoreAPI!!.playerHandler.pushPlayerQuitGame(
            event.player.uniqueId, event.player.name
        )
    }

    fun register() {
        multiLoginBukkit.server.pluginManager.registerEvents(this, multiLoginBukkit)
    }
}
