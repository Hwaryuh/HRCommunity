package kr.hwaryuh.community.event

import kr.hwaryuh.community.Main
import kr.hwaryuh.community.service.ProfileInventoryHolder
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.player.PlayerInteractEntityEvent

class PlayerProfileEvent(private val plugin: Main) : Listener {

    @EventHandler
    fun onPlayerInteract(event: PlayerInteractEntityEvent) {
        val player = event.player
        val clickedEntity = event.rightClicked

        if (player.isSneaking && clickedEntity is Player) {
            event.isCancelled = true
            plugin.openProfileMenu(player, clickedEntity)
        }
    }

    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        val inventory = event.inventory
        val holder = inventory.holder

        if (holder is ProfileInventoryHolder) {
            val clickedSlot = event.rawSlot

            if (clickedSlot in listOf(0, 8, 9, 17, 18, 27) || clickedSlot >= 54) event.isCancelled = true
            if (event.isShiftClick && clickedSlot >= 54) event.isCancelled = true
            if (event.hotbarButton != -1 && clickedSlot < 54) event.isCancelled = true
        }
    }
}