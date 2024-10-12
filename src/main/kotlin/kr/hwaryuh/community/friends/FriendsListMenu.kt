package kr.hwaryuh.community.friends

import kr.hwaryuh.community.Main
import kr.hwaryuh.community.profile.PreviousMenuType
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryType
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.SkullMeta
import java.util.UUID

class FriendsListMenu(private val plugin: Main) : Listener {

    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        val inventory = event.inventory

        if (inventory.holder is FriendListHolder) {
            handleFriendListClick(event)
        }
    }

    private fun handleFriendListClick(event: InventoryClickEvent) {
        event.isCancelled = true
        val clickedInventory = event.clickedInventory

        if (clickedInventory?.holder is FriendListHolder) {
            val clickedItem = event.currentItem ?: return
            if (clickedItem.type == Material.PLAYER_HEAD) {
                val meta = clickedItem.itemMeta as? SkullMeta ?: return
                val friendUUID = meta.owningPlayer?.uniqueId ?: return
                val viewer = event.whoClicked as Player

                val onlineFriend = Bukkit.getPlayer(friendUUID)
                if (onlineFriend != null && onlineFriend.isOnline) {
                    plugin.openProfileMenu(viewer, onlineFriend, true, PreviousMenuType.FRIENDS)
                } else {
                    val offlineFriend = Bukkit.getOfflinePlayer(friendUUID)
                    plugin.openOfflineProfileMenu(viewer, offlineFriend, true, PreviousMenuType.FRIENDS)
                }
            }
        }

        if (event.click.isShiftClick && clickedInventory?.type == InventoryType.PLAYER) {
            event.isCancelled = true
        }
    }

    fun open(player: Player) {
        val inventory = Bukkit.createInventory(FriendListHolder(), 54, "친구 목록")
        updateInventory(inventory, player)
        player.openInventory(inventory)
    }

    private fun updateInventory(inventory: Inventory, player: Player) {
        inventory.clear()
        val friends = plugin.databaseManager.getFriends(player.uniqueId)

        friends.forEachIndexed { index, friendUUID ->
            if (index >= 54) return@forEachIndexed
            val friendHead = playerHead(friendUUID)
            inventory.setItem(index, friendHead)
        }
    }

    private fun playerHead(playerUUID: UUID): ItemStack {
        val playerHead = ItemStack(Material.PLAYER_HEAD)
        val meta = playerHead.itemMeta as SkullMeta
        val offlinePlayer = Bukkit.getOfflinePlayer(playerUUID)
        meta.owningPlayer = offlinePlayer
        meta.setDisplayName(offlinePlayer.name ?: "알 수 없음")
        playerHead.itemMeta = meta
        return playerHead
    }
}