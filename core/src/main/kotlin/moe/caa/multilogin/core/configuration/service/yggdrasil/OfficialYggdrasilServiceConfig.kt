package moe.caa.multilogin.core.configuration.service.yggdrasil

import moe.caa.multilogin.api.service.ServiceType
import moe.caa.multilogin.core.configuration.ProxyConfig
import moe.caa.multilogin.core.configuration.SkinRestorerConfig

/**
 * 正版官方 Yggdrasil
 */
class OfficialYggdrasilServiceConfig(
    id: Int,
    name: String,
    initUUID: InitUUID,
    initNameFormat: String,
    whitelist: Boolean,
    skinRestorer: SkinRestorerConfig,
    trackIp: Boolean,
    timeout: Int,
    retry: Int,
    retryDelay: Long,
    authProxy: ProxyConfig,
    customSessionServer: String
) : BaseYggdrasilServiceConfig(
    id, name, initUUID, initNameFormat, whitelist, skinRestorer,
    trackIp, timeout, retry, retryDelay, authProxy
) {
    private val customSessionServer: String =
        if (customSessionServer.endsWith("/")) customSessionServer else "$customSessionServer/"

    override fun getAuthURL(): String {
        return customSessionServer + "session/minecraft/hasJoined?username={0}&serverId={1}{2}"
    }

    override fun getAuthPostContent(): String {
        throw UnsupportedOperationException("get post content")
    }

    override fun getAuthTrackIpContent(): String = "&ip={0}"

    override fun getHttpRequestMethod(): HttpRequestMethod = HttpRequestMethod.GET

    override fun getServiceType(): ServiceType = ServiceType.OFFICIAL
}
