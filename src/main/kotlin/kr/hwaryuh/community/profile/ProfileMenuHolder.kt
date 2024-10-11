package kr.hwaryuh.community.profile

import org.bukkit.OfflinePlayer
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder

class ProfileMenuHolder(val target: OfflinePlayer, val fromMenu: Boolean, val previousMenu: PreviousMenuType) : InventoryHolder {
    override fun getInventory(): Inventory {
        throw UnsupportedOperationException("내부 오류: 자체 메뉴 누락")
    }
}