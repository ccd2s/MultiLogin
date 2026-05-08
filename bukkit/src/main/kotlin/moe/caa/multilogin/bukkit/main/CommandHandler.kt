package moe.caa.multilogin.bukkit.main

import moe.caa.multilogin.bukkit.impl.BukkitSender
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter

class CommandHandler(private val multiLoginBukkit: MultiLoginBukkit) {

    fun register() {
        val pluginCommand = multiLoginBukkit.getCommand(multiLoginBukkit.name.lowercase())!!
        val executor = Executor()
        pluginCommand.tabCompleter = executor
        pluginCommand.setExecutor(executor)
    }

    private inner class Executor : CommandExecutor, TabCompleter {

        override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<String>): Boolean {
            val ns = arrayOf(command.name, *args)
            multiLoginBukkit.multiCoreAPI!!.commandHandler.execute(BukkitSender(sender), ns)
            return true
        }

        override fun onTabComplete(sender: CommandSender, command: Command, label: String, args: Array<String>): List<String> {
            val ns = arrayOf(command.name, *args)
            return multiLoginBukkit.multiCoreAPI!!.commandHandler.tabComplete(BukkitSender(sender), ns)
        }
    }
}
