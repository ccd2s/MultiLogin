package moe.caa.multilogin.core.configuration

import moe.caa.multilogin.api.MapperConfigAPI
import org.spongepowered.configurate.CommentedConfigurationNode
import org.spongepowered.configurate.ConfigurateException
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.yaml.YamlConfigurationLoader
import java.io.File
import java.util.TreeMap
import kotlin.collections.iterator

/**
 * ChatSessionBlocker 数据包映射配置
 */
class MapperConfig(private val dataFolder: File) : MapperConfigAPI {
    private val packetMapping = object : TreeMap<Int, Int>() {
        override fun put(key: Int, value: Int): Int? {
            if (key < 761) {
                return value
            }

            if (containsValue(value)) {
                val existingKey = findKeyByValue(value)
                if (existingKey != null && existingKey > key) {
                    super.remove(existingKey)
                    super.put(key, value)
                }
                return value
            }

            return super.put(key, value)
        }

        private fun findKeyByValue(value: Int): Int? {
            for ((entryKey, entryValue) in entries) {
                if (entryValue == value) {
                    return entryKey
                }
            }
            return null
        }

        init {
            put(761, 0x20)
            put(762, 0x06)
            put(765, 0x07)
            put(768, 0x08)
            put(771, 0x09)
        }
    }

    override fun getPacketMapping(): MutableMap<Int, Int> = packetMapping

    override fun save() {
        try {
            val loader = YamlConfigurationLoader.builder()
                .file(File(dataFolder, "mapper.yml"))
                .indent(2)
                .build()
            val rootNode: CommentedConfigurationNode = loader.load()
            val mapperNode = rootNode.node("mapper")
            for ((key, value) in packetMapping.entries) {
                mapperNode.node(key.toString()).set(String.format("0x%02X", value))
            }
            loader.save(rootNode)
        } catch (e: ConfigurateException) {
            throw RuntimeException(e)
        }
    }

    override fun reload() {
        val loader = YamlConfigurationLoader.builder()
            .file(File(dataFolder, "mapper.yml"))
            .build()
        try {
            val mapperNode: ConfigurationNode = loader.load().node("mapper")
            for ((key, valueNode) in mapperNode.childrenMap()) {
                val hexValue = valueNode.string
                if (hexValue != null) {
                    val intValue = Integer.decode(hexValue)
                    packetMapping[key.toString().toInt()] = intValue
                }
            }
        } catch (e: ConfigurateException) {
            throw RuntimeException(e)
        }
    }

    override fun toString(): String = "MapperConfig(packetMapping=$packetMapping, dataFolder=$dataFolder)"
}
