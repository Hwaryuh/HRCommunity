package kr.hwaryuh.community.service

import kr.hwaryuh.community.Main
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

class FriendsList(private val plugin: Main) : Listener {

    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        val inventory = event.inventory

        when (inventory.holder) {
            is FriendListHolder -> handleFriendListClick(event)
            is FriendRemovalHolder -> handleFriendRemovalClick(event)
        }
    }

    private fun handleFriendListClick(event: InventoryClickEvent) {
        event.isCancelled = true
        val clickedInventory = event.clickedInventory

        if (clickedInventory?.holder is FriendListHolder) {
            val clickedItem = event.currentItem ?: return
            if (clickedItem.type == Material.PLAYER_HEAD) {
                val meta = clickedItem.itemMeta as? SkullMeta ?: return
                val clickedPlayerName = meta.owningPlayer?.name ?: return
                val clickedPlayer = Bukkit.getPlayer(clickedPlayerName)

                if (clickedPlayer != null && clickedPlayer.isOnline) {
                    plugin.openProfileMenu(event.whoClicked as Player, clickedPlayer, true)
                } else {
                    openFriendRemovalGUI(event.whoClicked as Player, UUID.fromString(meta.owningPlayer?.uniqueId.toString()))
                }
            }
        }

        if (event.click.isShiftClick && clickedInventory?.type == InventoryType.PLAYER) {
            event.isCancelled = true
        }
    }

    private fun handleFriendRemovalClick(event: InventoryClickEvent) {
        event.isCancelled = true
        val clickedItem = event.currentItem ?: return

        if (clickedItem.type == Material.GHAST_TEAR && event.slot == 53) {
            val holder = event.inventory.holder as? FriendRemovalHolder ?: return
            val player = event.whoClicked as Player
            plugin.databaseManager.removeFriend(player.uniqueId, holder.friendUUID)
            plugin.databaseManager.removeFriend(holder.friendUUID, player.uniqueId)
            player.sendMessage("친구가 삭제되었습니다.")
            open(player)
        }
    }

    fun open(player: Player) {
        val inventory = Bukkit.createInventory(FriendListHolder(), 54, "친구 목록")
        updateInventory(inventory, player)
        player.openInventory(inventory)
    }

    private fun openFriendRemovalGUI(player: Player, friendUUID: UUID) {
        val inventory = Bukkit.createInventory(FriendRemovalHolder(friendUUID), 54, "친구 삭제")
        val friendHead = playerHead(friendUUID)
        inventory.setItem(22, friendHead)

        val removeFriend = ItemStack(Material.GHAST_TEAR)
        val meta = removeFriend.itemMeta
        meta?.setDisplayName("친구 삭제")
        removeFriend.itemMeta = meta
        inventory.setItem(53, removeFriend)

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

    private class FriendListHolder : org.bukkit.inventory.InventoryHolder {
        override fun getInventory(): Inventory {
            throw UnsupportedOperationException("내부 오류: 자체 메뉴 누락")
        }
    }

    private class FriendRemovalHolder(val friendUUID: UUID) : org.bukkit.inventory.InventoryHolder {
        override fun getInventory(): Inventory {
            throw UnsupportedOperationException("내부 오류: 자체 메뉴 누락")
        }
    }
}