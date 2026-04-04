package moe.caa.multilogin.core.configuration.service

import moe.caa.multilogin.api.service.IService
import moe.caa.multilogin.api.service.ServiceType
import moe.caa.multilogin.core.configuration.ConfException
import moe.caa.multilogin.core.configuration.SkinRestorerConfig
import java.nio.charset.StandardCharsets
import java.util.UUID
import java.util.function.BiFunction

abstract class BaseServiceConfig(
    val id: Int,
    val name: String,
    val initUUID: InitUUID,
    val initNameFormat: String,
    @get:JvmName("isWhitelist") val whitelist: Boolean,
    val skinRestorer: SkinRestorerConfig
) : IService {

    init {
        checkValid()
    }

    @Throws(ConfException::class)
    protected open fun checkValid() {
        if (id !in 0..127) {
            throw ConfException(
                "Yggdrasil id $id is out of bounds, The value can only be between 0 and 127."
            )
        }
    }

    fun generateName(loginName: String): String {
        return initNameFormat.replace("{name}", loginName).replace(" ", "_")
    }

    override fun getServiceId(): Int = id

    override fun getServiceName(): String = name

    abstract override fun getServiceType(): ServiceType

    /**
     * 初始化的UUID生成器
     */
    enum class InitUUID(private val biFunction: BiFunction<UUID, String, UUID>) {
        DEFAULT(BiFunction { u, _ -> u }),
        OFFLINE(BiFunction { _, n ->
            UUID.nameUUIDFromBytes(("OfflinePlayer:$n").toByteArray(StandardCharsets.UTF_8))
        }),
        RANDOM(BiFunction { _, _ -> UUID.randomUUID() });

        fun generateUUID(onlineUUID: UUID, currentUsername: String): UUID {
            return biFunction.apply(onlineUUID, currentUsername)
        }
    }
}
