package moe.caa.multilogin.bukkit.impl

import moe.caa.multilogin.api.internal.plugin.BaseScheduler
import moe.caa.multilogin.api.internal.plugin.IPlayerManager
import moe.caa.multilogin.api.internal.plugin.ISender
import moe.caa.multilogin.api.internal.plugin.IServer
import moe.caa.multilogin.bukkit.main.MultiLoginBukkit

class BukkitServer(private val multiLoginBukkit: MultiLoginBukkit) : IServer {

    private val bkScheduler: BaseScheduler = object : BaseScheduler() {
        override fun runTask(run: Runnable, delay: Long) {
            multiLoginBukkit.server.scheduler.runTaskLater(multiLoginBukkit, run, delay)
        }
    }

    private val playerManager = BukkitPlayerManager(multiLoginBukkit.server)

    override fun getScheduler(): BaseScheduler = bkScheduler

    override fun getPlayerManager(): IPlayerManager = playerManager

    override fun isOnlineMode(): Boolean = multiLoginBukkit.server.onlineMode

    override fun isForwarded(): Boolean = true

    override fun getName(): String = multiLoginBukkit.server.name

    override fun getVersion(): String = multiLoginBukkit.server.version

    override fun shutdown() {
        multiLoginBukkit.server.shutdown()
    }

    override fun getConsoleSender(): ISender = BukkitSender(multiLoginBukkit.server.consoleSender)

    override fun pluginHasEnabled(id: String): Boolean =
        multiLoginBukkit.server.pluginManager.getPlugin(id) != null
}
