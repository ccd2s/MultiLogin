package moe.caa.multilogin.bukkit.injector.proxy

import com.mojang.authlib.GameProfile
import com.mojang.authlib.minecraft.MinecraftSessionService
import com.mojang.authlib.properties.Property
import com.google.common.collect.LinkedHashMultimap
import moe.caa.multilogin.api.internal.auth.AuthResult
import moe.caa.multilogin.api.internal.logger.LoggerProvider
import moe.caa.multilogin.api.service.ServiceType
import moe.caa.multilogin.api.internal.skinrestorer.SkinRestorerResult
import moe.caa.multilogin.bukkit.injector.BukkitInjector
import moe.caa.multilogin.bukkit.main.MultiLoginBukkit
import moe.caa.multilogin.core.auth.LoginAuthResult
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Type
import java.net.InetSocketAddress
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class YggdrasilMinecraftSessionServiceInvocationHandler(
    private val vanillaSessionService: MinecraftSessionService
) : InvocationHandler {
    override fun invoke(proxy: Any, method: Method, args: Array<out Any>): Any? {
        if (method.name.equals("hasJoinedServer")) {
            val profileName: String = if (args[0] is String) args[0] as String else {
                // 旧版 authlib: getName()；新版 authlib: name()
                val gp = args[0] as GameProfile
                try {
                    GameProfile::class.java.getMethod("name").invoke(gp) as String
                } catch (_: NoSuchMethodException) {
                    GameProfile::class.java.getMethod("getName").invoke(gp) as String
                }
            }
            val serverId: String = args[1] as String
            val ip = if (args.size == 3 && args[2] is InetSocketAddress) URLEncoder.encode(
                (args[2] as InetSocketAddress).address.hostAddress,
                StandardCharsets.UTF_8
            ) else ""
            return handle(method, profileName, serverId, ip)
        }
        return method.invoke(vanillaSessionService, *args)
    }

    private fun handle(method: Method, profileName: String, serverId: String, ip: String): Any? {
        val multiCoreAPI = MultiLoginBukkit.instance.multiCoreAPI!!
        try {
            val result = multiCoreAPI.authHandler.auth(profileName, serverId, ip) as LoginAuthResult
            if (result.result == AuthResult.Result.ALLOW) {
                var gameProfile: moe.caa.multilogin.api.profile.GameProfile = result.response
                try {
                    val restorerResult: SkinRestorerResult = multiCoreAPI.skinRestorerHandler.doRestorer(result)
                    if (restorerResult.throwable != null) {
                        LoggerProvider.getLogger()
                            .error("An exception occurred while processing the skin repair.", restorerResult.throwable)
                    }
                    LoggerProvider.getLogger().debug(
                        String.format(
                            "Skin restore result of %s is %s.",
                            result.baseServiceAuthenticationResult.response.name,
                            restorerResult.reason
                        )
                    )
                    if (restorerResult.response != null) {
                        gameProfile = restorerResult.response
                    }
                } catch (e: Exception) {
                    LoggerProvider.getLogger().debug(
                        String.format(
                            "Skin restore result of %s is %s.",
                            result.baseServiceAuthenticationResult.response.name,
                            "error"
                        )
                    )
                    LoggerProvider.getLogger().debug("An exception occurred while processing the skin repair.", e)
                }
                if (result.baseServiceAuthenticationResult.serviceConfig.serviceType == ServiceType.OFFICIAL) {
                    BukkitInjector.clearUnsignedChatPlayer(profileName, gameProfile.name, gameProfile.id)
                } else {
                    BukkitInjector.markUnsignedChatPlayer(profileName, gameProfile.name, gameProfile.id)
                }
                return generateResponse(method.returnType, gameProfile)
            } else {
                BukkitInjector.kickMsg[Thread.currentThread()] = result.kickMessage
                LoggerProvider.getLogger().info("$profileName was kicked out for ${result.kickMessage}")
                return null
            }
        } catch (e: Throwable) {
            val message = multiCoreAPI.languageHandler.getMessage("auth_error")
            BukkitInjector.kickMsg[Thread.currentThread()] = message
            LoggerProvider.getLogger().info("$profileName was kicked out for $message")
            LoggerProvider.getLogger().error("An exception occurred while processing a login request.", e)
        }
        return null
    }

    private fun generateResponse(returnType: Type, response: moe.caa.multilogin.api.profile.GameProfile): Any {
        val multimap = LinkedHashMultimap.create<String, Property>()
        response.propertyMap.forEach { (k, u) ->
            multimap.put(k, Property(u.name, u.value, u.signature))
        }
        val result = try {
            // 1.21.9+: PropertyMap(Multimap) 构造函数 + GameProfile(UUID, String, PropertyMap)
            val properties = com.mojang.authlib.properties.PropertyMap(multimap)
            GameProfile(response.id, response.name, properties)
        } catch (_: NoSuchMethodError) {
            // 1.21.9 以前: GameProfile(UUID, String)，再单独写入属性
            val profile = GameProfile::class.java
                .getConstructor(java.util.UUID::class.java, String::class.java)
                .newInstance(response.id, response.name)
            // 旧版 authlib: getProperties()；新版 authlib: properties()
            @Suppress("UNCHECKED_CAST")
            val props = try {
                GameProfile::class.java.getMethod("properties").invoke(profile)
            } catch (_: NoSuchMethodException) {
                GameProfile::class.java.getMethod("getProperties").invoke(profile)
            } as com.google.common.collect.Multimap<String, Property>
            multimap.forEach { k, v -> props.put(k, v) }
            profile
        }
        if(returnType == result.javaClass){
            return result
        }

        return Class.forName("com.mojang.authlib.yggdrasil.ProfileResult")
            .getConstructor(Class.forName("com.mojang.authlib.GameProfile"))
            .newInstance(result)
    }
}
