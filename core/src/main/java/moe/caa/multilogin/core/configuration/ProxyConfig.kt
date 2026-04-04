package moe.caa.multilogin.core.configuration

import moe.caa.multilogin.api.internal.util.ValueUtil
import okhttp3.Authenticator
import okhttp3.Credentials
import org.spongepowered.configurate.CommentedConfigurationNode
import org.spongepowered.configurate.serialize.SerializationException
import java.net.InetSocketAddress
import java.net.Proxy

/**
 * 表示一个代理配置
 */
data class ProxyConfig(
    val type: Proxy.Type,
    val hostname: String,
    val port: Int,
    val username: String,
    val password: String,
) {
    companion object {
        @JvmStatic
        @Throws(SerializationException::class, ConfException::class)
        fun read(node: CommentedConfigurationNode): ProxyConfig {
            val type = node.node("type").get(Proxy.Type::class.java, Proxy.Type.DIRECT)
            val hostname = node.node("hostname").getString("127.0.0.1")
            val port = node.node("port").getInt(1080)
            val username = node.node("username").getString("")
            val password = node.node("password").getString("")

            return ProxyConfig(type, hostname, port, username, password)
        }
    }

    fun getProxy(): Proxy {
        if (type == Proxy.Type.DIRECT) {
            return Proxy.NO_PROXY
        }
        return Proxy(type, InetSocketAddress(hostname, port))
    }

    fun getProxyAuthenticator(): Authenticator {
        return Authenticator { _, response ->
            if (ValueUtil.isEmpty(username)) {
                return@Authenticator null
            }
            val credential = Credentials.basic(username, password)
            response.request.newBuilder()
                .header("Proxy-Authorization", credential)
                .build()
        }
    }
}
