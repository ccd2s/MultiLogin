package moe.caa.multilogin.bukkit.injector

import com.mojang.authlib.minecraft.MinecraftSessionService
import moe.caa.multilogin.api.internal.injector.Injector
import moe.caa.multilogin.api.internal.logger.LoggerProvider
import moe.caa.multilogin.api.internal.main.MultiCoreAPI
import moe.caa.multilogin.bukkit.injector.protocol.PacketHandler
import moe.caa.multilogin.bukkit.injector.proxy.SignatureValidatorInvocationHandler
import moe.caa.multilogin.bukkit.injector.proxy.YggdrasilMinecraftSessionServiceInvocationHandler
import moe.caa.multilogin.bukkit.main.MultiLoginBukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent
import java.lang.reflect.*
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap


class BukkitInjector : Injector {
    companion object {
        val kickMsg: MutableMap<Thread, String> = ConcurrentHashMap()
        private val unsignedChatNames: MutableSet<String> = ConcurrentHashMap.newKeySet()
        private val unsignedChatUniqueIds: MutableSet<UUID> = ConcurrentHashMap.newKeySet()

        fun markUnsignedChatPlayer(loginName: String?, profileName: String?, uniqueId: UUID?) {
            normalizeName(loginName)?.let(unsignedChatNames::add)
            normalizeName(profileName)?.let(unsignedChatNames::add)
            uniqueId?.let(unsignedChatUniqueIds::add)
        }

        fun clearUnsignedChatPlayer(loginName: String?, profileName: String?, uniqueId: UUID?) {
            normalizeName(loginName)?.let(unsignedChatNames::remove)
            normalizeName(profileName)?.let(unsignedChatNames::remove)
            uniqueId?.let(unsignedChatUniqueIds::remove)
        }

        fun shouldBlockChatSession(playerName: String?, uniqueId: UUID?): Boolean {
            return (uniqueId != null && unsignedChatUniqueIds.contains(uniqueId)) ||
                unsignedChatNames.contains(normalizeName(playerName))
        }

        private fun normalizeName(name: String?): String? {
            return name?.trim()?.lowercase()?.takeIf { it.isNotEmpty() }
        }
    }

    override fun inject(api: MultiCoreAPI) {
        disableSecureProfileEnforcement(api)
        registerCleanupListener(api)

        var protocolHook = false
        if (api.plugin.runServer.pluginHasEnabled("ProtocolLib")) {
            try {
                PacketHandler().init()
                protocolHook = true
            } catch (e: Throwable) {
                LoggerProvider.getLogger().error("Unable to load ProtocolLib handler, is it up to date?", e)
            }
        }
        if (!protocolHook) {
            LoggerProvider.getLogger().warn(
                "It is strongly recommended that you install ProtocolLib," +
                        " otherwise the client will always prompt 'invalid session' when kicked out by MultiLogin during the login phase."
            )
        }
        try {
            // Service 存在，是高版本的！
            val servicesRecordClass = Class.forName("net.minecraft.server.Services")

            val signatureValidatorClass: Class<*>? = try {
                Class.forName("net.minecraft.util.SignatureValidator")
            } catch (_: Exception) {
                null
            }

            val pairMinecraftServerAndGetServiceField =
                forceGetNMS((api.plugin as MultiLoginBukkit).server, servicesRecordClass, HashSet())
            val minecraftServer = pairMinecraftServerAndGetServiceField.first
            val services = pairMinecraftServerAndGetServiceField.second[minecraftServer]

            val servicesRecordFields = servicesRecordClass.declaredFields

            class ModifiedPair<A, B>(
                var first: A,
                var second: B
            )

            val constructorArg = arrayListOf<ModifiedPair<Field, Any>>()

            var modified = false
            for (field in servicesRecordFields) {
                if (Modifier.isStatic(field.modifiers)) continue
                field.isAccessible = true
                val anyPair = ModifiedPair(field, field[services])
                constructorArg.add(anyPair)
                if (anyPair.second is MinecraftSessionService) {
                    // 替换MinecraftSessionService
                    anyPair.second = Proxy.newProxyInstance(
                        Thread.currentThread().contextClassLoader,
                        arrayOf(MinecraftSessionService::class.java),
                        YggdrasilMinecraftSessionServiceInvocationHandler(anyPair.second as MinecraftSessionService)
                    )
                    modified = true
                } else if (signatureValidatorClass != null && anyPair.second.javaClass.name.contains("SignatureValidator")) {
                    //  替换SignatureValidator
                    anyPair.second = Proxy.newProxyInstance(
                        Thread.currentThread().contextClassLoader,
                        arrayOf(signatureValidatorClass),
                        SignatureValidatorInvocationHandler(anyPair.second)
                    )
                }
            }

            if (!modified) throw RuntimeException("Unsupported server.")

            val declaredConstructor: Constructor<*> = servicesRecordClass.getDeclaredConstructor(
                *constructorArg.map { it.first.type }.toTypedArray()
            )

            val newServices = declaredConstructor.newInstance(*constructorArg.map { it.second }.toTypedArray())
            pairMinecraftServerAndGetServiceField.second[minecraftServer] = newServices
            return
        } catch (_: java.lang.Exception) {
        }
        val pair = forceGetNMS((api.plugin as MultiLoginBukkit).server, MinecraftSessionService::class.java, HashSet())
        pair.second.isAccessible = true
        pair.second[pair.first] = Proxy.newProxyInstance(
            Thread.currentThread().contextClassLoader,
            arrayOf(MinecraftSessionService::class.java),
            YggdrasilMinecraftSessionServiceInvocationHandler(pair.second[pair.first] as MinecraftSessionService)
        )
    }

    private fun disableSecureProfileEnforcement(api: MultiCoreAPI) {
        val server = (api.plugin as MultiLoginBukkit).server

        val disabledByApi = runCatching {
            val method = server.javaClass.methods.firstOrNull { method ->
                method.name == "isEnforcingSecureProfiles" && method.parameterCount == 0
            } ?: return@runCatching false
            method.invoke(server) == false
        }.getOrDefault(false)

        if (disabledByApi) {
            return
        }

        runCatching {
            val dedicatedPropertiesClass = Class.forName("net.minecraft.server.dedicated.DedicatedServerProperties")
            val pair = forceGetNMS(server, dedicatedPropertiesClass, HashSet())
            val properties = pair.second[pair.first]

            var changed = false
            for (field in dedicatedPropertiesClass.declaredFields) {
                if (field.type != Boolean::class.javaPrimitiveType) continue
                if (!field.name.contains("enforceSecureProfile", ignoreCase = true)) continue
                field.isAccessible = true
                if (field.getBoolean(properties)) {
                    field.setBoolean(properties, false)
                }
                changed = true
            }

            if (changed) {
                LoggerProvider.getLogger().info("Disabled secure profile enforcement for third-party login chat compatibility.")
            }
        }.onFailure {
            LoggerProvider.getLogger().debug("Unable to disable secure profile enforcement automatically.", it)
        }
    }

    private fun registerCleanupListener(api: MultiCoreAPI) {
        val plugin = api.plugin as MultiLoginBukkit
        plugin.server.pluginManager.registerEvents(object : Listener {
            @EventHandler
            fun onQuit(event: PlayerQuitEvent) {
                clearUnsignedChatPlayer(event.player.name, event.player.name, event.player.uniqueId)
            }
        }, plugin)
    }

    private fun forceGetNMS(source: Any, needGet: Type, ignore: MutableSet<Type>): Pair<Any, Field> {
        var sourceClass: Class<*> = source.javaClass
        // 双重遍历确保能获取到本类和父类所有的Field
        do {
            for (declaredField in sourceClass.declaredFields) {
                try {
                    declaredField.isAccessible = true
                    // 类型匹配，返回Field所在的类的实例和Field
                    if (declaredField.type === needGet) {
                        if (sourceClass.name.startsWith("net.minecraft.")) {
                            return Pair(source, declaredField)
                        }
                    }
                    val o = declaredField[source]
                    if (ignore.add(o.javaClass)) return forceGetNMS(o, needGet, ignore)
                } catch (_: Throwable) {
                }
            }
            if (sourceClass.superclass == null) break
        } while (sourceClass.superclass.also { sourceClass = it } != null)
        throw ClassNotFoundException(needGet.typeName)
    }
}
