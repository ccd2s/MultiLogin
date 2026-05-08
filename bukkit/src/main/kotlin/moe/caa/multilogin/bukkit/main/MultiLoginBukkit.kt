package moe.caa.multilogin.bukkit.main

import moe.caa.multilogin.api.internal.injector.Injector
import moe.caa.multilogin.api.internal.logger.LoggerProvider
import moe.caa.multilogin.api.internal.logger.bridges.JavaLoggerBridge
import moe.caa.multilogin.api.internal.main.MultiCoreAPI
import moe.caa.multilogin.api.internal.plugin.IPlugin
import moe.caa.multilogin.bukkit.impl.BukkitServer
import moe.caa.multilogin.loader.main.PluginLoader
import org.bukkit.plugin.java.JavaPlugin
import java.io.File

class MultiLoginBukkit : JavaPlugin(), IPlugin {

    private lateinit var _runServer: BukkitServer

    override fun getRunServer(): BukkitServer = _runServer

    var multiCoreAPI: MultiCoreAPI? = null
        private set

    private lateinit var pluginLoader: PluginLoader

    override fun onLoad() {
        instance = this
        LoggerProvider.setLogger(JavaLoggerBridge(logger))
        _runServer = BukkitServer(this)
        pluginLoader = PluginLoader(this)
        try {
            pluginLoader.load("MultiLogin-Bukkit-Injector.JarFile")
        } catch (e: Exception) {
            LoggerProvider.getLogger().error("An exception was encountered while initializing the plugin.", e)
            _runServer.shutdown()
        }
    }

    override fun onEnable() {
        try {
            multiCoreAPI = pluginLoader.coreObject
            multiCoreAPI!!.load()
            val injector = pluginLoader.findClass("moe.caa.multilogin.bukkit.injector.BukkitInjector")
                .getConstructor().newInstance() as Injector
            injector.inject(multiCoreAPI!!)
        } catch (e: Throwable) {
            LoggerProvider.getLogger().error("An exception was encountered while loading the plugin.", e)
            _runServer.shutdown()
            return
        }

        GlobalListener(this).register()
        CommandHandler(this).register()
    }

    override fun onDisable() {
        try {
            multiCoreAPI!!.close()
            pluginLoader.close()
        } catch (e: Exception) {
            LoggerProvider.getLogger().error("An exception was encountered while close the plugin", e)
        } finally {
            multiCoreAPI = null
            _runServer.shutdown()
        }
    }

    override fun getTempFolder(): File = File(dataFolder, "tmp")

    companion object {
        @JvmStatic
        lateinit var instance: MultiLoginBukkit
            private set
    }
}
