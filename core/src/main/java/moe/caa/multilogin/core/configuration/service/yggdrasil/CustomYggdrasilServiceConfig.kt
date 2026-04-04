package moe.caa.multilogin.core.configuration.service.yggdrasil

import moe.caa.multilogin.api.service.ServiceType
import moe.caa.multilogin.core.configuration.ProxyConfig
import moe.caa.multilogin.core.configuration.SkinRestorerConfig

/**
 * 自定义 Yggdrasil
 */
class CustomYggdrasilServiceConfig(
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
    private val url: String,
    private val postContent: String,
    private val trackIpContent: String,
    private val method: HttpRequestMethod
) : BaseYggdrasilServiceConfig(
    id, name, initUUID, initNameFormat, whitelist, skinRestorer,
    trackIp, timeout, retry, retryDelay, authProxy
) {

    override fun getAuthURL(): String = url

    override fun getAuthPostContent(): String = postContent

    override fun getAuthTrackIpContent(): String = trackIpContent

    override fun getHttpRequestMethod(): HttpRequestMethod = method

    override fun getServiceType(): ServiceType = ServiceType.CUSTOM_YGGDRASIL
}
