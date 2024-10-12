package kr.hwaryuh.community.profile

import kr.hwaryuh.community.Main
import kr.hwaryuh.community.party.PartyMenu
import net.Indyuce.mmocore.api.player.PlayerData
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.player.PlayerInteractEntityEvent

class PlayerProfileListener(private val plugin: Main) : Listener {

    @EventHandler
    fun onPlayerInteract(event: PlayerInteractEntityEvent) {
        val player = event.player
        val clickedEntity = event.rightClicked

        if (player.isSneaking && clickedEntity is Player) {
            event.isCancelled = true
            plugin.openProfileMenu(player, clickedEntity, false, PreviousMenuType.NONE)
        }
    }

    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        val inventory = event.inventory
        val holder = inventory.holder

        if (holder is ProfileMenuHolder) {
            val clickedSlot = event.rawSlot

            if (clickedSlot in listOf(0, 8, 9, 17, 18, 27) || clickedSlot >= 54) event.isCancelled = true
            if (event.isShiftClick && clickedSlot >= 54) event.isCancelled = true
            if (event.hotbarButton != -1 && clickedSlot < 54) event.isCancelled = true

            val player = event.whoClicked as Player
            val target = holder.target

            when {
                clickedSlot == 53 && event.currentItem?.type == Material.GHAST_TEAR -> {
                    event.isCancelled = true
                    if (plugin.databaseManager.areFriends(player.uniqueId, target.uniqueId)) {
                        target.name?.let { targetName ->
                            plugin.friendsManager.deleteFriend(player, arrayOf("삭제", targetName))
                        }
                        player.closeInventory()

                        if (holder.fromMenu) {
                            plugin.openFriendsList(player)
                        }
                    }
                }

                clickedSlot == 53 && event.currentItem?.type == Material.EMERALD -> {
                    event.isCancelled = true
                    if (!plugin.databaseManager.areFriends(player.uniqueId, target.uniqueId)) {
                        target.name?.let { targetName ->
                            plugin.friendsManager.addFriend(player, arrayOf("추가", targetName))
                        }
                        player.closeInventory()
                    }
                }

                clickedSlot == 45 && event.currentItem?.type == Material.ARROW && holder.fromMenu -> {
                    event.isCancelled = true
                    player.closeInventory()
                    when (holder.previousMenu) {
                        PreviousMenuType.PARTY -> {
                            val playerData = PlayerData.get(player)
                            plugin.getService(PartyMenu::class.java)?.openPartyMenu(player, playerData)
                        }

                        PreviousMenuType.FRIENDS -> plugin.openFriendsList(player)
                        PreviousMenuType.NONE -> {}
                    }
                }
            }
        }
    }
}