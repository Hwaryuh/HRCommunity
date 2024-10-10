package kr.hwaryuh.community.service

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder
import org.bukkit.inventory.ItemStack
import io.lumine.mythic.lib.api.item.NBTItem

class PlayerProfile {

    fun createProfileInventory(target: Player): Inventory {
        val inventory = Bukkit.createInventory(ProfileInventoryHolder(target), 54, "${target.name}의 프로필")

        setArmorSlots(inventory, target)
        setWeaponSlots(inventory, target)

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
}

class ProfileInventoryHolder(val target: Player) : InventoryHolder {
    override fun getInventory(): Inventory {
        throw UnsupportedOperationException("내부 오류: 자체 인벤토리 누락") // ProfileInventoryHolder does not have its own inventory.
    }
}