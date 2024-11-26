package kr.hwaryuh.community.party

import kr.hwaryuh.community.Main
import kr.hwaryuh.community.profile.PreviousMenuType
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
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

class PartyMenuHolder(val party: MythicPartyBridge) : InventoryHolder {
    override fun getInventory(): Inventory {
        throw UnsupportedOperationException("내부 오류: 자체 메뉴 누락")
    }
}

class PartyMenu(private val plugin: Main, private val partyManager: PartyManager, partyInviteManager: PartyInviteManager) : Listener {
    private val memberSlots = listOf(10, 12, 14, 16, 28, 30, 32, 34)
    private val partyInviteMenu = PartyInviteMenu(plugin, partyInviteManager)

    fun openPartyMenu(player: Player) {
        val party = partyManager.getPlayerParty(player)
        if (party == null) {
            player.sendMessage(Component.text("파티에 속해있지 않습니다.").color(NamedTextColor.RED))
            return
        }

        val maxPartySize = 8 // 기본 최대 파티원 수
        val currentPartySize = party.getMemberCount()
        val ownerName = party.leader.name ?: "알 수 없음"

        val baseTitle = plugin.configManager.getMenuTitle("party-menu")
        val title = baseTitle
            .replace("{owner}", ownerName)
            .replace("{current}", currentPartySize.toString())
            .replace("{max}", maxPartySize.toString())

        val inventory = Bukkit.createInventory(PartyMenuHolder(party), 54, title)

        updatePartyMenuItems(inventory, party, player)
        player.openInventory(inventory)
    }

    private fun updatePartyMenuItems(inventory: Inventory, party: MythicPartyBridge, viewer: Player) {
        memberSlots.forEach { slot ->
            inventory.setItem(slot, null)
        }

        val leader = party.leader.player
        if (leader != null) {
            val leaderHead = playerHead(leader, party.isLeader(viewer), leader)
            inventory.setItem(memberSlots[0], leaderHead)
        }

        val members = party.players.filter { !party.isLeader(it) }
        members.forEachIndexed { index, member ->
            if (index < memberSlots.size - 1) {  // 리더는 이미 설정했으므로 -1
                val memberHead = playerHead(member, party.isLeader(viewer), leader)
                inventory.setItem(memberSlots[index + 1], memberHead)
            }
        }

        for (slot in memberSlots) {
            if (inventory.getItem(slot) == null) {
                inventory.setItem(slot, emptySlot(party.isLeader(viewer)))
            }
        }

        inventory.setItem(8, leaveButton())
    }

    private fun playerHead(player: Player, isViewerPartyOwner: Boolean, leader: Player?): ItemStack {
        val item = ItemStack(Material.PLAYER_HEAD)
        val meta = item.itemMeta as SkullMeta

        meta.owningPlayer = player
        meta.displayName(Component.text(player.name)
            .color(NamedTextColor.YELLOW)
            .decoration(TextDecoration.ITALIC, false))

        val lore = mutableListOf<Component>()

        if (isViewerPartyOwner && leader != player) {
            lore.add(Component.text("쉬프트 우클릭으로 플레이어 추방")
                .color(NamedTextColor.RED)
                .decoration(TextDecoration.ITALIC, false))
        }

        meta.lore(lore)
        item.itemMeta = meta
        return item
    }

    private fun emptySlot(isOwner: Boolean): ItemStack {
        val item = ItemStack(Material.BLACK_STAINED_GLASS_PANE)
        val meta = item.itemMeta
        if (isOwner) {
            meta.displayName(Component.text("플레이어 초대···")
                .color(NamedTextColor.GREEN)
                .decoration(TextDecoration.ITALIC, false))
        } else {
            meta.displayName(Component.text("빈 플레이어···")
                .color(NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false))
        }
        item.itemMeta = meta
        return item
    }

    private fun leaveButton(): ItemStack {
        val item = ItemStack(Material.RED_WOOL)
        val meta = item.itemMeta
        meta.displayName(Component.text("파티 나가기")
            .color(NamedTextColor.RED)
            .decoration(TextDecoration.ITALIC, false))
        item.itemMeta = meta
        return item
    }

    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        val holder = event.inventory.holder
        if (holder is PartyMenuHolder) {
            event.isCancelled = true
            val player = event.whoClicked as? Player ?: return
            val party = holder.party

            when (event.rawSlot) {
                8 -> {
                    player.closeInventory()
                    partyManager.handleLeaveOnMenu(player)
                }

                in memberSlots -> {
                    val clickedItem = event.currentItem
                    when {
                        clickedItem?.type == Material.BLACK_STAINED_GLASS_PANE -> {
                            if (party.isLeader(player)) {
                                partyInviteMenu.openInviteMenu(player, party)
                            }
                        }
                        clickedItem?.type == Material.PLAYER_HEAD -> {
                            val skullMeta = clickedItem.itemMeta as? SkullMeta
                            val targetPlayer = skullMeta?.owningPlayer?.player
                            if (targetPlayer != null) {
                                if (event.isLeftClick) {
                                    player.closeInventory()
                                    plugin.openProfileMenu(player, targetPlayer, true, PreviousMenuType.PARTY)
                                } else if (event.isShiftClick && event.isRightClick &&
                                    party.isLeader(player) && targetPlayer != player) {
                                    partyManager.kickFromParty(player, targetPlayer)
                                    updatePartyMenuItems(event.inventory, party, player)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}