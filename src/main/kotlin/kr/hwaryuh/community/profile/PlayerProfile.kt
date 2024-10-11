package kr.hwaryuh.community.profile

import kr.hwaryuh.community.Main
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import io.lumine.mythic.lib.api.item.NBTItem
import org.bukkit.OfflinePlayer
import org.bukkit.inventory.meta.SkullMeta

class PlayerProfile(private val plugin: Main) {

    fun createProfileInventory(viewer: Player, target: Player, fromMenu: Boolean, previousMenu: PreviousMenuType): Inventory {
        val inventory = Bukkit.createInventory(ProfileMenuHolder(target, fromMenu, previousMenu), 54, "${target.name}의 프로필")

        setArmorSlots(inventory, target)
        setWeaponSlots(inventory, target)

        if (viewer != target) {
            if (plugin.databaseManager.areFriends(viewer.uniqueId, target.uniqueId)) {
                val removeFriendItem = removeFriendButton()
                inventory.setItem(53, removeFriendItem)
            } else if (plugin.databaseManager.hasFriendRequest(viewer.uniqueId, target.uniqueId)) {
                val pendingRequestItem = pendingRequestIcon()
                inventory.setItem(53, pendingRequestItem)
            } else {
                val addFriendItem = addFriendButton()
                inventory.setItem(53, addFriendItem)
            }
        }

        if (fromMenu) {
            val backButton = backButton()
            inventory.setItem(45, backButton)
        }

        return inventory
    }

    fun createOfflineProfileInventory(viewer: Player, target: OfflinePlayer, fromMenu: Boolean, previousMenu: PreviousMenuType): Inventory {
        val inventory = Bukkit.createInventory(ProfileMenuHolder(target, fromMenu, previousMenu), 54, "${target.name}의 프로필")

        val infoItem = ItemStack(Material.PLAYER_HEAD)
        val meta = infoItem.itemMeta as SkullMeta
        meta.owningPlayer = target
        meta.setDisplayName("§6${target.name}")
        val lore = mutableListOf<String>()
        lore.add("§7상태: §c오프라인")
        meta.lore = lore
        infoItem.itemMeta = meta
        inventory.setItem(4, infoItem)

        if (viewer.uniqueId != target.uniqueId) {
            if (plugin.databaseManager.areFriends(viewer.uniqueId, target.uniqueId)) {
                val removeFriendItem = removeFriendButton()
                inventory.setItem(53, removeFriendItem)
            } else if (plugin.databaseManager.hasFriendRequest(viewer.uniqueId, target.uniqueId)) {
                val pendingRequestItem = pendingRequestIcon()
                inventory.setItem(53, pendingRequestItem)
            } else {
                val addFriendItem = addFriendButton()
                inventory.setItem(53, addFriendItem)
            }
        }

        if (fromMenu) {
            val backButton = backButton()
            inventory.setItem(45, backButton)
        }

        return inventory
    }


    private fun setArmorSlots(inventory: Inventory, player: Player) {
        val armorSlots = listOf(0, 9, 18, 27)
        val armorPieces = player.inventory.armorContents

        for (i in armorSlots.indices) {
            val armorPiece = armorPieces[3 - i]
            if (armorPiece != null && armorPiece.type != Material.AIR) {
                inventory.setItem(armorSlots[i], armorPiece)
            }
        }
    }

    private fun setWeaponSlots(inventory: Inventory, player: Player) {
        val (mainWeapon, subWeapon) = findMMOItemsWeapons(player)
        mainWeapon?.let { inventory.setItem(8, it) }
        subWeapon?.let { inventory.setItem(17, it) }
    }

    private fun findMMOItemsWeapons(player: Player): Pair<ItemStack?, ItemStack?> {
        var mainWeapon: ItemStack? = null
        var subWeapon: ItemStack? = null

        for (item in player.inventory.contents) {
            if (item == null || item.type == Material.AIR) continue

            val weaponRole = getWeaponRole(item)
            when (weaponRole) {
                "MAIN" -> mainWeapon = item
                "SUB" -> subWeapon = item
            }
        }

        return Pair(mainWeapon, subWeapon)
    }

    private fun getWeaponRole(item: ItemStack): String? {
        val nbtItem = NBTItem.get(item)
        if (!nbtItem.hasType()) return null
        return nbtItem.getString("MMOITEMS_WEAPON_ROLE").takeIf { it.isNotEmpty() }
    }

    private fun removeFriendButton(): ItemStack {
        val removeFriend = ItemStack(Material.GHAST_TEAR)
        val meta = removeFriend.itemMeta
        meta?.setDisplayName("친구 삭제")
        removeFriend.itemMeta = meta
        return removeFriend
    }

    private fun addFriendButton(): ItemStack {
        val addFriend = ItemStack(Material.EMERALD)
        val meta = addFriend.itemMeta
        meta?.setDisplayName("친구 추가")
        addFriend.itemMeta = meta
        return addFriend
    }

    private fun pendingRequestIcon(): ItemStack {
        val pendingRequest = ItemStack(Material.CLOCK)
        val meta = pendingRequest.itemMeta
        meta?.setDisplayName("친구 요청 대기 중")
        pendingRequest.itemMeta = meta
        return pendingRequest
    }

    private fun backButton(): ItemStack {
        val backButton = ItemStack(Material.ARROW)
        val meta = backButton.itemMeta
        meta?.setDisplayName("뒤로 가기")
        backButton.itemMeta = meta
        return backButton
    }
}