package kr.hwaryuh.community.service

import kr.hwaryuh.community.Main
import kr.hwaryuh.community.party.PartyInviteManager
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

class PartyInviteMenu(private val plugin: Main, private val partyInviteManager: PartyInviteManager) : Listener {

    private val ITEMS_PER_PAGE = 45

    fun openInviteMenu(player: Player, party: Party, page: Int = 0) {
        val inventory = Bukkit.createInventory(
            InviteMenuHolder(party, page),
            54,
            Component.text("누구를 초대할까요?")
        )

        val worldPlayers = player.world.players.filter { it != player && it !in party.members.mapNotNull { it.player } }
        val totalPages = (worldPlayers.size - 1) / ITEMS_PER_PAGE + 1

        val startIndex = page * ITEMS_PER_PAGE
        val endIndex = minOf(startIndex + ITEMS_PER_PAGE, worldPlayers.size)

        for (i in startIndex until endIndex) {
            val inviteePlayer = worldPlayers[i]
            val playerHead = playerHead(inviteePlayer)
            inventory.addItem(playerHead)
        }

        inventory.setItem(49, backButton())

        if (page > 0) { inventory.setItem(45, navigationButton("이전", Material.ARROW)) }
        if (page < totalPages - 1) { inventory.setItem(53, navigationButton("다음", Material.ARROW)) }

        player.openInventory(inventory)
    }

    private fun playerHead(player: Player): ItemStack {
        val item = ItemStack(Material.PLAYER_HEAD)
        val meta = item.itemMeta as SkullMeta

        meta.owningPlayer = player
        meta.displayName(Component.text(player.name).color(NamedTextColor.YELLOW))

        val playerData = PlayerData.get(player)
        val lore = mutableListOf<Component>()
        lore.add(Component.text("Lv. ${playerData.level} ${playerData.profess.name}").color(NamedTextColor.GRAY))

        meta.lore(lore)

        item.itemMeta = meta
        return item
    }

    private fun backButton(): ItemStack {
        val item = ItemStack(Material.BARRIER)
        val meta = item.itemMeta
        meta.displayName(Component.text("뒤로가기").color(NamedTextColor.RED))
        item.itemMeta = meta
        return item
    }

    private fun navigationButton(text: String, material: Material): ItemStack {
        val item = ItemStack(material)
        val meta = item.itemMeta
        meta.displayName(Component.text(text).color(NamedTextColor.YELLOW))
        item.itemMeta = meta
        return item
    }

    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        val holder = event.inventory.holder
        if (holder is InviteMenuHolder) {
            event.isCancelled = true  // 모든 클릭 이벤트를 취소합니다.
            val player = event.whoClicked as? Player ?: return
            val playerData = PlayerData.get(player)

            when (event.rawSlot) {
                in 0 until 45 -> {
                    val clickedItem = event.currentItem
                    if (clickedItem?.type == Material.PLAYER_HEAD) {
                        val skullMeta = clickedItem.itemMeta as? SkullMeta
                        val inviteePlayer = skullMeta?.owningPlayer?.player
                        if (inviteePlayer != null) {
                            partyInviteManager.inviteToParty(player, playerData, arrayOf("초대", inviteePlayer.name))
                            player.closeInventory()
                        }
                    }
                }
                45 -> {
                    if (holder.page > 0) {
                        openInviteMenu(player, holder.party, holder.page - 1)
                    }
                }
                49 -> {
                    player.closeInventory()
                    (plugin.getService(PartyMenu::class.java))?.openPartyMenu(player, playerData)
                }
                53 -> {
                    val worldPlayers = player.world.players.filter { it != player && it !in holder.party.members.mapNotNull { it.player } }
                    val totalPages = (worldPlayers.size - 1) / ITEMS_PER_PAGE + 1
                    if (holder.page < totalPages - 1) {
                        openInviteMenu(player, holder.party, holder.page + 1)
                    }
                }
            }
        }
    }

    private class InviteMenuHolder(val party: Party, val page: Int) : InventoryHolder {
        override fun getInventory(): Inventory {
            throw UnsupportedOperationException("내부 오류: 자체 메뉴 누락")
        }
    }
}