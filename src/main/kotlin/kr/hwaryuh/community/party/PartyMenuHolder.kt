package kr.hwaryuh.community.party

import net.Indyuce.mmocore.party.provided.Party
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder

class PartyMenuHolder(val party: Party) : InventoryHolder {
    override fun getInventory(): Inventory {
        throw UnsupportedOperationException("내부 오류: 자체 메뉴 누락")
    }
}