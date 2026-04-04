package moe.caa.multilogin.core.configuration.service.yggdrasil

import moe.caa.multilogin.api.service.ServiceType
import moe.caa.multilogin.core.configuration.ProxyConfig
import moe.caa.multilogin.core.configuration.SkinRestorerConfig

/**
 * Blessing Skin 皮肤站 Yggdrasil
 */
class BlessingSkinYggdrasilServiceConfig(
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
    apiRoot: String
) : BaseYggdrasilServiceConfig(
    id, name, initUUID, initNameFormat, whitelist, skinRestorer,
    trackIp, timeout, retry, retryDelay, authProxy
) {
    private val apiRoot: String = if (apiRoot.endsWith("/")) apiRoot else "$apiRoot/"

    override fun getAuthURL(): String {
        return apiRoot + "session" + "server" + "/session" + "/minecraft" + "/hasJoined?" +
                "username={0}&serverId={1}{2}"
    }

    override fun getAuthPostContent(): String {
        throw UnsupportedOperationException()
    }

    override fun getAuthTrackIpContent(): String = "&ip={0}"

    override fun getHttpRequestMethod(): HttpRequestMethod = HttpRequestMethod.GET

    override fun getServiceType(): ServiceType = ServiceType.BLESSING_SKIN
}
