package moe.caa.multilogin.bukkit.injector.protocol

import com.comphenix.protocol.PacketType
import com.comphenix.protocol.ProtocolLibrary
import com.comphenix.protocol.events.AbstractStructure
import com.comphenix.protocol.events.PacketAdapter
import com.comphenix.protocol.events.PacketEvent
import com.comphenix.protocol.injector.GamePhase
import com.comphenix.protocol.reflect.StructureModifier
import com.comphenix.protocol.wrappers.WrappedChatComponent
import moe.caa.multilogin.bukkit.injector.BukkitInjector
import moe.caa.multilogin.bukkit.main.MultiLoginBukkit
import java.util.Optional

class PacketHandler {
    fun init() {
        val manager = ProtocolLibrary.getProtocolManager()

        manager.addPacketListener(DisconnectHandler())
        manager.addPacketListener(LoginStartHandler())
        manager.addPacketListener(PlayerSessionHandler())
    }

    private class DisconnectHandler : PacketAdapter(
        params()
            .loginPhase()
        .serverSide()
        .plugin(MultiLoginBukkit.getInstance())
        .types(PacketType.Login.Server.DISCONNECT)) {
        override fun onPacketSending(event: PacketEvent) {
            val s = BukkitInjector.kickMsg.remove(Thread.currentThread()) ?: return
            val packet = event.packet
            event.isReadOnly = false
            packet.chatComponents.write(0, WrappedChatComponent.fromText(s))
            event.isReadOnly = true
        }
    }

    private class PlayerSessionHandler : PacketAdapter(
        params()
            .gamePhase(GamePhase.PLAYING)
            .clientSide()
            .plugin(MultiLoginBukkit.getInstance())
            .types(PacketType.Play.Client.CHAT_SESSION_UPDATE)){
        override fun onPacketReceiving(event: PacketEvent) {
            event.isReadOnly = false
            event.isCancelled = true
        }
    }

    private class LoginStartHandler : PacketAdapter(
        params()
            .loginPhase()
            .clientSide()
            .plugin(MultiLoginBukkit.getInstance())
            .types(PacketType.Login.Client.START)
    ) {
        override fun onPacketReceiving(event: PacketEvent) {
            val packet = event.packet
            event.isReadOnly = false
            sanitizeModifier(packet.modifier)
            event.isReadOnly = true
        }

        private fun sanitizeModifier(modifier: StructureModifier<Any>) {
            for (index in 0 until modifier.size()) {
                val field = modifier.fields[index].field
                val value = modifier.readSafely(index) ?: continue

                if (shouldClear(field.type.name, value)) {
                    if (value is Optional<*>) {
                        modifier.writeSafely(index, Optional.empty<Any>())
                    } else {
                        modifier.writeSafely(index, null)
                    }
                    continue
                }

                if (value is Optional<*>) {
                    val inner = value.orElse(null) ?: continue
                    if (shouldClear(inner.javaClass.name, inner)) {
                        modifier.writeSafely(index, Optional.empty<Any>())
                    } else if (inner is AbstractStructure) {
                        sanitizeModifier(inner.modifier as StructureModifier<Any>)
                    }
                    continue
                }

                if (value is AbstractStructure) {
                    sanitizeModifier(value.modifier as StructureModifier<Any>)
                }
            }
        }

        private fun shouldClear(typeName: String, value: Any): Boolean {
            if (containsProfileKeyMarker(typeName)) {
                return true
            }
            return value is AbstractStructure && containsProfileKeyMarker(value.handle.javaClass.name)
        }

        private fun containsProfileKeyMarker(name: String): Boolean {
            return name.contains("ProfilePublicKey") ||
                name.contains("ProfileKey") ||
                name.contains("PublicKeyData") ||
                name.contains("RemoteChatSession")
        }
    }
}
