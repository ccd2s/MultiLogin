package moe.caa.multilogin.core.configuration

import org.spongepowered.configurate.CommentedConfigurationNode
import org.spongepowered.configurate.serialize.SerializationException

/**
 * 表示数据库配置
 */
class SqlConfig private constructor(
    val backend: SqlBackend,
    val ip: String,
    val port: Int,
    val username: String,
    val password: String,
    val database: String,
    val tablePrefix: String,
    val connectUrl: String,
) {
    companion object {
        @JvmStatic
        @Throws(SerializationException::class)
        fun read(node: CommentedConfigurationNode): SqlConfig {
            val backend = node.node("backend").get(SqlBackend::class.java, SqlBackend.H2)
            val ip = node.node("ip").getString("127.0.0.1")
            val port = node.node("port").getInt(3306)
            val username = node.node("username").getString("root")
            val password = node.node("password").getString("root")
            val database = node.node("database").getString("multilogin")
            val tablePrefix = node.node("tablePrefix").getString("multilogin")
            val connectUrl = node.node("connectUrl").getString("")

            return SqlConfig(backend, ip, port, username, password, database, tablePrefix, connectUrl)
        }
    }

    enum class SqlBackend {
        H2, MYSQL,
    }

    override fun toString(): String {
        return "SqlConfig(backend=$backend, ip=$ip, port=$port, username=$username, password=$password, database=$database, tablePrefix=$tablePrefix, connectUrl=$connectUrl)"
    }
}
