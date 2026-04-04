package moe.caa.multilogin.core.configuration.service.yggdrasil

import moe.caa.multilogin.api.internal.util.Pair
import moe.caa.multilogin.api.internal.util.ValueUtil
import moe.caa.multilogin.core.configuration.ProxyConfig
import moe.caa.multilogin.core.configuration.SkinRestorerConfig
import moe.caa.multilogin.core.configuration.service.BaseServiceConfig
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

abstract class BaseYggdrasilServiceConfig(
    id: Int,
    name: String,
    initUUID: InitUUID,
    initNameFormat: String,
    whitelist: Boolean,
    skinRestorer: SkinRestorerConfig,
    @get:JvmName("isTrackIp") val trackIp: Boolean,
    val timeout: Int,
    val retry: Int,
    val retryDelay: Long,
    val authProxy: ProxyConfig
) : BaseServiceConfig(id, name, initUUID, initNameFormat, whitelist, skinRestorer) {

    /**
     * 生成验证 URL
     */
    fun generateAuthURL(username: String, serverId: String, ip: String): String {
        return ValueUtil.transPapi(
            getAuthURL(),
            Pair("username", URLEncoder.encode(username, StandardCharsets.UTF_8)),
            Pair("serverId", URLEncoder.encode(serverId, StandardCharsets.UTF_8)),
            Pair("ip", generateTraceIpContent(ip))
        )
    }

    /**
     * 生成验证 POST 内容
     */
    fun generateAuthPostContent(username: String, serverId: String, ip: String): String {
        return ValueUtil.transPapi(
            getAuthPostContent(),
            Pair("username", URLEncoder.encode(username, StandardCharsets.UTF_8)),
            Pair("serverId", URLEncoder.encode(serverId, StandardCharsets.UTF_8)),
            Pair("ip", generateTraceIpContent(ip))
        )
    }

    private fun generateTraceIpContent(ip: String): String {
        if (!trackIp) return ""
        if (ValueUtil.isEmpty(ip)) return ""
        val trackIpContent = getAuthTrackIpContent()
        if (ValueUtil.isEmpty(trackIpContent)) return ""
        return ValueUtil.transPapi(trackIpContent, Pair("ip", ip))
    }

    /**
     * 生成验证 URL
     */
    protected abstract fun getAuthURL(): String

    /**
     * 生成验证 POST 内容
     */
    protected abstract fun getAuthPostContent(): String

    /**
     * 生成验证 IP 内容
     */
    protected abstract fun getAuthTrackIpContent(): String

    /**
     * 返回请求类型
     */
    abstract fun getHttpRequestMethod(): HttpRequestMethod

    enum class HttpRequestMethod {
        GET, POST
    }
}
