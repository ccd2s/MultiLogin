package moe.caa.multilogin.bukkit.impl

import moe.caa.multilogin.api.internal.plugin.IPlayer
import moe.caa.multilogin.bukkit.main.MultiLoginBukkit
import org.bukkit.entity.Player
import java.net.SocketAddress
import java.util.Objects
import java.util.UUID

class BukkitPlayer(private val player: Player) : BukkitSender(player), IPlayer {

    override fun kickPlayer(message: String?) {
        MultiLoginBukkit.instance.server.scheduler.runTask(
            MultiLoginBukkit.instance,
            Runnable { player.kickPlayer(message ?: "") }
        )
    }

    override fun getUniqueId(): UUID = player.uniqueId

    override fun getAddress(): SocketAddress = player.address

    override fun isOnline(): Boolean = player.isOnline

    override fun getName(): String = player.name

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val that = other as BukkitPlayer
        return Objects.equals(player.uniqueId, that.player.uniqueId)
    }

    override fun hashCode(): Int = Objects.hash(player)
}
