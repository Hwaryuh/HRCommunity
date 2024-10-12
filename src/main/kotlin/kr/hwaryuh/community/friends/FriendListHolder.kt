package kr.hwaryuh.community.friends

import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder

class FriendListHolder : InventoryHolder {
    override fun getInventory(): Inventory {
        throw UnsupportedOperationException("내부 오류: 자체 메뉴 누락")
    }
}