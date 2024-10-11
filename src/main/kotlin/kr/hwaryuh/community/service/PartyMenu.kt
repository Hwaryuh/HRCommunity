package kr.hwaryuh.community.service

import kr.hwaryuh.community.Main
import kr.hwaryuh.community.party.PartyInviteManager
import kr.hwaryuh.community.party.PartyManager
import net.Indyuce.mmocore.MMOCore
import net.Indyuce.mmocore.api.player.PlayerData
import net.Indyuce.mmocore.party.provided.Party
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.SkullMeta

class PartyMenu(private val plugin: Main, private val partyManager: PartyManager, private val partyInviteManager: PartyInviteManager) : Listener {

    private val memberSlots = listOf(10, 12, 14, 16, 28, 30, 32, 34)
    private val partyInviteMenu = PartyInviteMenu(plugin, partyInviteManager)

    fun openPartyMenu(player: Player, playerData: PlayerData) {
        val party = playerData.party as? Party
        if (party == null) {
            player.sendMessage(Component.text("파티에 속해있지 않습니다.").color(NamedTextColor.RED))
            return
        }

        val maxPartySize = MMOCore.plugin.configManager.maxPartyPlayers
        val currentPartySize = party.members.size
        val ownerName = party.owner.player.name

        val inventory = Bukkit.createInventory(
            PartyMenuHolder(party, playerData),
            54,
            Component.text("${ownerName}의 파티 ($currentPartySize/$maxPartySize)")
        )

        updatePartyMenuItems(inventory, party, playerData)
        player.openInventory(inventory)
    }

    private fun updatePartyMenuItems(inventory: Inventory, party: Party, playerData: PlayerData) {
        val ownerItem = createPlayerHead(party.owner, isOwner = true)
        inventory.setItem(10, ownerItem)

        val members = party.members.filter { it != party.owner }
        members.forEachIndexed { index, member ->
            if (index < memberSlots.size - 1) {  // 리더는 없을 수가 없죠 ?
                val memberItem = createPlayerHead(member, isOwner = false)
                inventory.setItem(memberSlots[index + 1], memberItem)
            }
        }

        for (slot in memberSlots) {
            if (inventory.getItem(slot) == null) {
                inventory.setItem(slot, createEmptySlot(party.owner == playerData))
            }
        }

        inventory.setItem(8, leaveButton())
    }

    private fun createPlayerHead(playerData: PlayerData, isOwner: Boolean): ItemStack {
        val item = ItemStack(Material.PLAYER_HEAD)
        val meta = item.itemMeta as SkullMeta

        meta.owningPlayer = playerData.player
        meta.displayName(Component.text(playerData.player.name).color(NamedTextColor.YELLOW))

        val lore = mutableListOf<Component>()
        lore.add(Component.text("Lv. ${playerData.level} ${playerData.profess.name}").color(NamedTextColor.GRAY))

        meta.lore(lore)

        item.itemMeta = meta
        return item
    }

    private fun createEmptySlot(isOwner: Boolean): ItemStack {
        val item = ItemStack(Material.BLACK_STAINED_GLASS_PANE)
        val meta = item.itemMeta
        if (isOwner) {
            meta.displayName(Component.text("플레이어 초대···").color(NamedTextColor.GREEN))
        } else {
            meta.displayName(Component.text("빈 플레이어···").color(NamedTextColor.GRAY))
        }
        item.itemMeta = meta
        return item
    }

    private fun leaveButton(): ItemStack {
        val item = ItemStack(Material.RED_WOOL)
        val meta = item.itemMeta
        meta.displayName(Component.text("파티 나가기").color(NamedTextColor.RED))
        item.itemMeta = meta
        return item
    }

    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        val holder = event.inventory.holder
        if (holder is PartyMenuHolder) {
            event.isCancelled = true
            val player = event.whoClicked as? Player ?: return
            val playerData = PlayerData.get(player)

            when (event.rawSlot) {
                8 -> {
                    player.closeInventory()
                    PartyManager.leaveParty(playerData, holder.party)
                }
                in memberSlots -> {
                    val clickedItem = event.currentItem
                    when {
                        clickedItem?.type == Material.BLACK_STAINED_GLASS_PANE -> {
                            if (holder.party.owner == playerData) {
                                partyInviteMenu.openInviteMenu(player, holder.party)
                            }
                        }
                        clickedItem?.type == Material.PLAYER_HEAD -> {
                            val skullMeta = clickedItem.itemMeta as? SkullMeta
                            val targetPlayer = skullMeta?.owningPlayer?.player
                            if (targetPlayer != null) {
                                if (event.isLeftClick) {
                                    player.closeInventory()
                                    plugin.openProfileMenu(player, targetPlayer, true)
                                } else if (event.isShiftClick && event.isRightClick && holder.party.owner == playerData && targetPlayer != player) {
                                    partyManager.kickFromParty(player, playerData, arrayOf("추방", targetPlayer.name))
                                    updatePartyMenuItems(event.inventory, holder.party, playerData)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private class PartyMenuHolder(val party: Party, val playerData: PlayerData) : InventoryHolder {
        override fun getInventory(): Inventory {
            throw UnsupportedOperationException("내부 오류: 자체 메뉴 누락")
        }
    }
}