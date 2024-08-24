package dev.slohth.upsidedown.profile

import com.mojang.authlib.GameProfile
import com.mojang.authlib.properties.Property
import com.mojang.datafixers.util.Pair
import dev.slohth.upsidedown.UpsideDown
import dev.slohth.upsidedown.skin.Skin
import gg.flyte.twilight.scheduler.delay
import gg.flyte.twilight.scheduler.sync
import net.kyori.adventure.text.Component
import net.minecraft.network.protocol.game.*
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EquipmentSlot
import org.bukkit.Bukkit
import org.bukkit.craftbukkit.CraftServer
import org.bukkit.craftbukkit.entity.CraftPlayer
import org.bukkit.entity.Display
import org.bukkit.entity.Player
import org.bukkit.entity.TextDisplay
import org.bukkit.event.player.PlayerRespawnEvent
import java.util.*
import kotlin.math.floor

class Profile(val uuid: UUID) {

    private val player get() = Bukkit.getPlayer(uuid)!!
    private val trueProfile = (player as CraftPlayer).profile

    private var nametag: TextDisplay? = null

    init {
        PROFILES[uuid] = this
        nametag(true)
    }

    fun upsideDown(flag: Boolean) {
        val skin = (player as CraftPlayer).profile.properties.get("textures").first()
        properties((if (flag) "Dinnerbone" else trueProfile.name), skin.value, skin.signature)
        respawn()
    }

    fun skin(skin: Skin) {
        properties((player as CraftPlayer).profile.name, skin.texture(), skin.signature())
        respawn()
    }

    fun reset() {
        gameProfile(trueProfile)
        respawn()
    }

    fun remove() {
        nametag(false)
        PROFILES.remove(uuid)
    }

    private fun nametag(flag: Boolean) {
        if (flag) {
            if (nametag != null) nametag(false)
            nametag = player.world.spawn(player.location.add(0.0, 2.5, 0.0), TextDisplay::class.java)

            nametag!!.text(Component.text(trueProfile.name))
            nametag!!.billboard = Display.Billboard.CENTER
            nametag!!.isSeeThrough = true

            delay(1) {
                player.addPassenger(nametag!!)
                val transformation = nametag!!.transformation
                transformation.translation.add(0f, 0.2f, 0.0f)
                nametag!!.transformation = transformation
            }
        } else {
            player.removePassenger(nametag ?: return)
            nametag?.remove()
            nametag = null
        }
    }

    private fun properties(name: String, texture: String, signature: String?) {
        val gameProfile = GameProfile(uuid, name)
        gameProfile.properties.put("textures", Property("textures", texture, signature))
        gameProfile(gameProfile)
    }

    private fun gameProfile(profile: GameProfile) {
        try {
            val field = (player as CraftPlayer).handle.javaClass.superclass.getDeclaredField("cD")
            field.isAccessible = true
            field.set((player as CraftPlayer).handle, profile)
            field.isAccessible = false
        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun respawn() {
        nametag(false)
        respawnPackets()
        UpsideDown.TEAM!!.addPlayer(player)
        nametag(true)
    }

    private fun respawnPackets() {
        val serverPlayer = (player as CraftPlayer).handle
        val serverPlayerList = (Bukkit.getServer() as CraftServer).handle

        val removeEntity = ClientboundRemoveEntitiesPacket(serverPlayer.id)
        val removeEntityInfo = ClientboundPlayerInfoRemovePacket(listOf(serverPlayer.uuid))

        val addEntityInfo = ClientboundPlayerInfoUpdatePacket.createPlayerInitializing(listOf(serverPlayer))
        val addEntity = ClientboundAddEntityPacket(serverPlayer, 0, serverPlayer.blockPosition())

        val heldItem = ClientboundSetCarriedItemPacket(serverPlayer.inventory.selected)

        // Fixes second skin layer rendering bug
        val updateSkin = ClientboundSetEntityDataPacket(serverPlayer.id, serverPlayer.entityData.nonDefaultValues ?: emptyList())
        val updateStatus = ClientboundEntityEventPacket(serverPlayer, 28)

        val position = ClientboundPlayerPositionPacket(
            serverPlayer.x, serverPlayer.y, serverPlayer.z,
            serverPlayer.bukkitYaw, serverPlayer.bukkitEntity.pitch,
            HashSet(), 0
        )
        val headRoation = ClientboundRotateHeadPacket(serverPlayer, floor(serverPlayer.getYHeadRot() * 256.0f / 360.0f).toInt().toByte())

        sync {
            for (target in serverPlayerList.players) {
                val connection = target.connection

                if (target.uuid != serverPlayer.uuid) {
                    connection.sendPacket(removeEntity)
                    connection.sendPacket(addEntity)
                    connection.sendPacket(headRoation)
                    connection.sendPacket(updateSkin)
                }

                connection.sendPacket(removeEntityInfo)
                connection.sendPacket(addEntityInfo)

                for (slot in EquipmentSlot.entries) {
                    val item = serverPlayer.getItemBySlot(slot)
                    if (!item.isEmpty) {
                        connection.sendPacket(ClientboundSetEquipmentPacket(serverPlayer.id, listOf(Pair.of(slot, item))))
                    }
                }
            }

            val connection = serverPlayer.connection

            serverPlayerList.respawn(serverPlayer, true, Entity.RemovalReason.KILLED, PlayerRespawnEvent.RespawnReason.PLUGIN, player.location)

            connection.sendPacket(position)
            connection.sendPacket(heldItem)
            connection.sendPacket(updateSkin)
            connection.sendPacket(updateStatus)

            serverPlayer.onUpdateAbilities()
            serverPlayer.resetSentInfo() // health update
            serverPlayer.bukkitEntity.recalculatePermissions()
        }
    }

    companion object {
        val PROFILES: MutableMap<UUID, Profile> = HashMap()
        fun Player.profile(): Profile = PROFILES[this.uniqueId] ?: Profile(this.uniqueId)
    }
}