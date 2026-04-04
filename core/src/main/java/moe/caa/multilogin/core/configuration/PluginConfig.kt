package moe.caa.multilogin.core.configuration

import moe.caa.multilogin.api.internal.logger.LoggerProvider
import moe.caa.multilogin.api.internal.logger.bridges.DebugLoggerBridge
import moe.caa.multilogin.api.internal.util.IOUtil
import moe.caa.multilogin.api.service.ServiceType
import moe.caa.multilogin.core.configuration.service.BaseServiceConfig
import moe.caa.multilogin.core.configuration.service.FloodgateServiceConfig
import moe.caa.multilogin.core.configuration.service.yggdrasil.BaseYggdrasilServiceConfig
import moe.caa.multilogin.core.configuration.service.yggdrasil.BlessingSkinYggdrasilServiceConfig
import moe.caa.multilogin.core.configuration.service.yggdrasil.CustomYggdrasilServiceConfig
import moe.caa.multilogin.core.configuration.service.yggdrasil.OfficialYggdrasilServiceConfig
import moe.caa.multilogin.core.main.MultiCore
import org.spongepowered.configurate.CommentedConfigurationNode
import org.spongepowered.configurate.serialize.SerializationException
import org.spongepowered.configurate.yaml.YamlConfigurationLoader
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.URISyntaxException
import java.nio.file.Files
import java.util.*
import java.util.jar.JarFile

/**
 * 表示插件配置处理程序
 */
class PluginConfig(
    private val dataFolder: File,
    private val core: MultiCore,
) {
    @get:JvmName("isForceUseLogin")
    var forceUseLogin: Boolean = false
        private set

    @get:JvmName("isNameCorrect")
    var nameCorrect: Boolean = false
        private set

    @get:JvmName("isCheckUpdate")
    var checkUpdate: Boolean = false
        private set

    @get:JvmName("isFloodgateSupport")
    var floodgateSupport: Boolean = false
        private set

    @get:JvmName("isAutoNameChange")
    var autoNameChange: Boolean = false
        private set

    lateinit var sqlConfig: SqlConfig
        private set

    lateinit var mapperConfig: MapperConfig
        private set

    lateinit var nameAllowedRegular: String
        private set

    @get:JvmName("isWelcomeMsg")
    var welcomeMsg: Boolean = false
        private set

    var serviceIdMap: Map<Int, BaseServiceConfig> = HashMap()
        private set

    var confirmCommandValidTimeMills: Long = 0
        private set

    var linkAcceptValidTimeMills: Long = 0
        private set

    fun getFloodgateAuthenticationService(): FloodgateServiceConfig? {
        for (value in serviceIdMap.values) {
            if (value is FloodgateServiceConfig) {
                return value
            }
        }
        return null
    }

    @Throws(IOException::class, URISyntaxException::class)
    fun reload() {
        val servicesFolder = File(dataFolder, "services")
        if (!dataFolder.exists()) {
            Files.createDirectory(dataFolder.toPath())
        }
        if (!servicesFolder.exists()) {
            Files.createDirectory(servicesFolder.toPath())
        }

        IOUtil.removeAllFiles(File(dataFolder, "examples"))
        saveResource("config.yml", false)
        saveResource("mapper.yml", false)
        saveResourceDir("examples", true)
        if (::mapperConfig.isInitialized) {
            mapperConfig.save()
        }
        mapperConfig = MapperConfig(dataFolder)
        mapperConfig.reload()

        val configConfigurationNode = YamlConfigurationLoader.builder()
            .file(File(dataFolder, "config.yml"))
            .build()
            .load()

        if (configConfigurationNode.node("debug").getBoolean(false)) {
            DebugLoggerBridge.startDebugMode()
        } else {
            DebugLoggerBridge.cancelDebugMode()
        }

        forceUseLogin = configConfigurationNode.node("forceUseLogin").getBoolean(true)
        checkUpdate = configConfigurationNode.node("checkUpdate").getBoolean(true)
        sqlConfig = SqlConfig.read(configConfigurationNode.node("sql"))
        nameAllowedRegular = configConfigurationNode.node("nameAllowedRegular").getString("^[0-9a-zA-Z_]{3,16}$")
        floodgateSupport = configConfigurationNode.node("floodgateSupport").getBoolean(false)
        welcomeMsg = configConfigurationNode.node("welcomeMsg").getBoolean(true)
        nameCorrect = configConfigurationNode.node("nameCorrect").getBoolean(true)
        autoNameChange = configConfigurationNode.node("autoNameChange").getBoolean(true)
        confirmCommandValidTimeMills = configConfigurationNode.node("confirmCommandValidTimeMills").getLong(15000)
        linkAcceptValidTimeMills = configConfigurationNode.node("linkAcceptValidTimeMills").getLong(30000)

        val idMap = HashMap<Int, BaseServiceConfig>()
        Files.list(servicesFolder.toPath()).use { paths ->
            val tmp = ArrayList<BaseServiceConfig>()
            paths.forEach { path ->
                if (!path.toFile().name.lowercase().endsWith(".yml")) {
                    return@forEach
                }
                try {
                    tmp.add(readServiceConfig(YamlConfigurationLoader.builder().path(path).build().load()))
                } catch (e: Exception) {
                    LoggerProvider.getLogger().error(ConfException("Unable to read authentication service config under file $path", e))
                }
            }

            val notRepeat = HashSet<ServiceType>()
            for (config in tmp) {
                if (ONLY_ONE_SERVICE_INFO_MAP.containsKey(config.serviceType)) {
                    if (!notRepeat.add(config.serviceType)) {
                        throw ConfException(
                            String.format(
                                "Duplicates are not allowed for authentication services of type %s, but more than one was found.",
                                ONLY_ONE_SERVICE_INFO_MAP[config.serviceType],
                            ),
                        )
                    }
                }
            }

            for (config in tmp) {
                if (idMap.containsKey(config.serviceId)) {
                    throw ConfException(String.format("The same authentication service id value %d exists.", config.serviceId))
                }
                idMap[config.serviceId] = config
            }

            // 不支持当前 Floodgate
            if (!isCoreFloodgateSupported()) {
                for (config in tmp) {
                    if (config.serviceType == ServiceType.FLOODGATE) {
                        LoggerProvider.getLogger().warn(
                            String.format(
                                "Floodgate not detected, authentication service with id %d and name %s will be invalid.",
                                config.serviceId,
                                config.serviceName,
                            ),
                        )
                        break
                    }
                }
            } else {
                // Floodgate 支持，但是未启用
                if (!floodgateSupport) {
                    for (config in tmp) {
                        if (config.serviceType == ServiceType.FLOODGATE) {
                            LoggerProvider.getLogger().warn(
                                String.format(
                                    "Floodgate support is not enabled, authentication service with id %d and name %s will be invalid.",
                                    config.serviceId,
                                    config.serviceName,
                                ),
                            )
                            break
                        }
                    }
                }
            }
        }

        idMap.forEach { (id, config) ->
            if (config.serviceName.equals("unnamed", ignoreCase = true)) {
                LoggerProvider.getLogger().warn(String.format("The name of authentication service whose id is %d has not been set.", id))
            }
            LoggerProvider.getLogger().info(
                String.format(
                    "Add a authentication service with id %d and name %s.",
                    id,
                    config.serviceName,
                ),
            )
        }

        if (idMap.isEmpty()) {
            LoggerProvider.getLogger().warn(
                "The server has not added any authentication service, which will prevent all players from logging in.",
            )
        } else {
            LoggerProvider.getLogger().info(String.format("Added %d authentication services.", idMap.size))
        }
        serviceIdMap = Collections.unmodifiableMap(idMap)
    }

    @Throws(SerializationException::class, ConfException::class)
    private fun readServiceConfig(load: CommentedConfigurationNode): BaseServiceConfig {
        val nodeId = load.node("id")
        if (nodeId.empty()) {
            throw ConfException("service id is null.")
        }
        val id = nodeId.int
        val name = load.node("name").getString("Unnamed")
        val serviceType =
            load.node("serviceType").get(ServiceType::class.java) ?: throw ConfException("service type is null.")

        val initUUID = load.node("initUUID")
            .get(BaseServiceConfig.InitUUID::class.java, BaseServiceConfig.InitUUID.DEFAULT)
        val whitelist = load.node("whitelist").getBoolean(false)
        val skinRestorer = SkinRestorerConfig.read(load.node("skinRestorer"))

        val initNameFormat = load.node("initNameFormat").getString("{name}")

        if (serviceType == ServiceType.OFFICIAL || serviceType == ServiceType.BLESSING_SKIN || serviceType == ServiceType.CUSTOM_YGGDRASIL) {
            val yggdrasilAuthNode = load.node("yggdrasilAuth")
            val trackIp = yggdrasilAuthNode.node("trackIp").getBoolean(false)
            val timeout = yggdrasilAuthNode.node("timeout").getInt(10000)
            val retry = yggdrasilAuthNode.node("retry").getInt(0)
            val retryDelay = yggdrasilAuthNode.node("retryDelay").getLong(0L)
            val authProxy = ProxyConfig.read(yggdrasilAuthNode.node("authProxy"))

            if (serviceType == ServiceType.OFFICIAL) {
                val customSessionServer = yggdrasilAuthNode.node("official").node("sessionServer")
                    .getString("https://sessionserver.mojang.com")
                return OfficialYggdrasilServiceConfig(
                    id,
                    name,
                    initUUID,
                    initNameFormat,
                    whitelist,
                    skinRestorer,
                    trackIp,
                    timeout,
                    retry,
                    retryDelay,
                    authProxy,
                    customSessionServer,
                )
            }

            if (serviceType == ServiceType.BLESSING_SKIN) {
                return BlessingSkinYggdrasilServiceConfig(
                    id,
                    name,
                    initUUID,
                    initNameFormat,
                    whitelist,
                    skinRestorer,
                    trackIp,
                    timeout,
                    retry,
                    retryDelay,
                    authProxy,
                    Objects.requireNonNull(yggdrasilAuthNode.node("blessingSkin").node("apiRoot").string!!)
                )
            }

            val customNode = yggdrasilAuthNode.node("custom")
            val url = customNode.node("url").string
            val method = customNode.node("method").get(
                BaseYggdrasilServiceConfig.HttpRequestMethod::class.java,
                BaseYggdrasilServiceConfig.HttpRequestMethod.GET,
            )
            val trackIpContent = customNode.node("trackIpContent").string
            val postContent = customNode.node("postContent").string

            return CustomYggdrasilServiceConfig(
                id,
                name,
                initUUID,
                initNameFormat,
                whitelist,
                skinRestorer,
                trackIp,
                timeout,
                retry,
                retryDelay,
                authProxy,
                url!!,
                postContent!!,
                trackIpContent!!,
                method,
            )
        }

        if (serviceType == ServiceType.FLOODGATE) {
            return FloodgateServiceConfig(id, name, initUUID, initNameFormat, whitelist, skinRestorer)
        }

        throw ConfException("Unknown service type ${serviceType.name}")
    }

    @Throws(IOException::class)
    fun saveResource(path: String, cover: Boolean) {
        saveResource(cover, dataFolder, path, path)
    }

    @Throws(IOException::class, URISyntaxException::class)
    fun saveResourceDir(path: String, cover: Boolean) {
        val file = File(dataFolder, path)
        if (!file.exists()) {
            Files.createDirectory(file.toPath())
        }
        JarFile(File(javaClass.protectionDomain.codeSource.location.toURI())).use { jarFile ->
            val jarFiles = jarFile.stream()
                .filter { jarEntry -> jarEntry.realName.startsWith(path) }
                .filter { jarEntry -> jarEntry.realName != "$path/" }
                .toList()
            for (je in jarFiles) {
                val realName = je.realName
                val fileName = realName.substring(path.length)
                saveResource(cover, file, realName, fileName)
            }
        }
    }

    @Throws(IOException::class)
    private fun saveResource(cover: Boolean, file: File, realName: String, fileName: String) {
        val subFile = File(file, fileName)
        val exists = subFile.exists()
        if (exists && !cover) {
            return
        } else if (!exists) {
            Files.createFile(subFile.toPath())
        }
        javaClass.getResourceAsStream("/$realName").use { inputStream ->
            FileOutputStream(subFile).use { outputStream ->
                IOUtil.copy(Objects.requireNonNull(inputStream), outputStream)
            }
        }
        if (!exists) {
            LoggerProvider.getLogger().info("Extract: $realName")
        } else {
            LoggerProvider.getLogger().info("Cover: $realName")
        }
    }

    private fun isCoreFloodgateSupported(): Boolean {
        return core.javaClass.getMethod("isFloodgateSupported").invoke(core) as Boolean
    }

    companion object {
        private val ONLY_ONE_SERVICE_INFO_MAP = mapOf(
            ServiceType.OFFICIAL to "official",
            ServiceType.FLOODGATE to "floodgate",
        )
    }
}
