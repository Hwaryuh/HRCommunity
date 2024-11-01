package kr.hwaryuh.community.party

import kr.hwaryuh.community.Main
import kr.hwaryuh.community.profile.PreviousMenuType
import net.Indyuce.mmocore.MMOCore
import net.Indyuce.mmocore.api.player.PlayerData
import net.Indyuce.mmocore.party.provided.Party
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

class PartyMenuHolder(val party: Party) : InventoryHolder {
    override fun getInventory(): Inventory {
        throw UnsupportedOperationException("내부 오류: 자체 메뉴 누락")
    }
}

class PartyMenu(private val plugin: Main, private val partyManager: PartyManager, partyInviteManager: PartyInviteManager) : Listener {

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

        val baseTitle = plugin.configManager.getMenuTitle("party-menu")
        val title = baseTitle
            .replace("{owner}", ownerName)
            .replace("{current}", currentPartySize.toString())
            .replace("{max}", maxPartySize.toString())

        val inventory = Bukkit.createInventory(PartyMenuHolder(party), 54, title)

        updatePartyMenuItems(inventory, party, playerData)
        player.openInventory(inventory)
    }

    private fun updatePartyMenuItems(inventory: Inventory, party: Party, playerData: PlayerData) {
        val ownerItem = playerHead(party.owner, party.owner == playerData, party.owner)
        inventory.setItem(10, ownerItem)

        val members = party.members.filter { it != party.owner }
        members.forEachIndexed { index, member ->
            if (index < memberSlots.size - 1) {  // 리더는 없을 수가 없죠 ?
                val memberItem = playerHead(member, party.owner == playerData, party.owner)
                inventory.setItem(memberSlots[index + 1], memberItem)
            }
        }

        for (slot in memberSlots) {
            if (inventory.getItem(slot) == null) {
                inventory.setItem(slot, emptySlot(party.owner == playerData))
            }
        }

        inventory.setItem(8, leaveButton())
    }

    private fun playerHead(playerData: PlayerData, isViewerPartyOwner: Boolean, partyOwner: PlayerData): ItemStack {
        val item = ItemStack(Material.PLAYER_HEAD)
        val meta = item.itemMeta as SkullMeta

        meta.owningPlayer = playerData.player
        meta.displayName(Component.text(playerData.player.name).color(NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false))

        val lore = mutableListOf<Component>()
        lore.add(Component.text("Lv. ${playerData.level} ${playerData.profess.name}")
            .color(NamedTextColor.GRAY)
            .decoration(TextDecoration.ITALIC, false))

        if (isViewerPartyOwner && playerData != partyOwner) {
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
            meta.displayName(Component.text("플레이어 초대···").color(NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false))
        } else {
            meta.displayName(Component.text("빈 플레이어···").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false))
        }
        item.itemMeta = meta
        return item
    }

    private fun leaveButton(): ItemStack {
        val item = ItemStack(Material.RED_WOOL)
        val meta = item.itemMeta
        meta.displayName(Component.text("파티 나가기").color(NamedTextColor.RED).decoration(TextDecoration.ITALIC, false))
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
                    partyManager.handleLeaveOnMenu(playerData, holder.party)
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
                                    plugin.openProfileMenu(player, targetPlayer, true, PreviousMenuType.PARTY)
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
}