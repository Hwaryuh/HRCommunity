package kr.hwaryuh.community.service

import kr.hwaryuh.community.Main
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.SkullMeta

class FriendsList(private val plugin: Main, private val player: Player) {
    private val inventory: Inventory = Bukkit.createInventory(null, 54, "친구 목록")

    fun open() {
        updateInventory()
        player.openInventory(inventory)
    }

    private fun updateInventory() {
        inventory.clear()
        val friends = plugin.databaseManager.getFriends(player.uniqueId)

        friends.forEachIndexed { index, friendUUID ->
            if (index >= 54) return@forEachIndexed

            val friendHead = ItemStack(Material.PLAYER_HEAD)
            val meta = friendHead.itemMeta as SkullMeta
            val friendName = Bukkit.getOfflinePlayer(friendUUID).name ?: "Unknown"

            meta.owningPlayer = Bukkit.getOfflinePlayer(friendUUID)
            meta.setDisplayName(friendName)
            friendHead.itemMeta = meta

            inventory.setItem(index, friendHead)
        }
    }
}