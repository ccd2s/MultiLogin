package moe.caa.multilogin.bukkit.impl

import moe.caa.multilogin.api.internal.plugin.IPlayer
import moe.caa.multilogin.api.internal.plugin.ISender
import org.bukkit.command.CommandSender
import org.bukkit.command.ConsoleCommandSender
import org.bukkit.entity.Player

open class BukkitSender(private val sender: CommandSender) : ISender {

    override fun isPlayer(): Boolean = sender is Player

    override fun isConsole(): Boolean = sender is ConsoleCommandSender

    override fun hasPermission(permission: String): Boolean = sender.hasPermission(permission)

    override fun sendMessagePL(message: String) {
        for (s in message.split("\\r?\\n".toRegex())) {
            sender.sendMessage(s)
        }
    }

    override fun getName(): String = sender.name

    override fun getAsPlayer(): IPlayer = BukkitPlayer(sender as Player)
}
