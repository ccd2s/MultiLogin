package moe.caa.multilogin.bukkit.impl

import moe.caa.multilogin.api.internal.plugin.IPlayer
import moe.caa.multilogin.api.internal.plugin.IPlayerManager
import org.bukkit.Server
import java.util.UUID

class BukkitPlayerManager(private val server: Server) : IPlayerManager {

    override fun getPlayers(name: String): Set<IPlayer> =
        onlinePlayers.filter { it.name.equals(name, ignoreCase = true) }.toSet()

    override fun getPlayer(uuid: UUID): IPlayer? =
        server.getPlayer(uuid)?.let { BukkitPlayer(it) }

    override fun getOnlinePlayers(): Set<IPlayer> =
        server.onlinePlayers.map { BukkitPlayer(it) }.toSet()
}
