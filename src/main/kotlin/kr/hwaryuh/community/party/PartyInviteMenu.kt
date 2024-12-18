package kr.hwaryuh.community.party

import kr.hwaryuh.community.Main
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

class PartyInviteHolder(val party: MythicPartyBridge, val page: Int) : InventoryHolder {
    override fun getInventory(): Inventory {
        throw UnsupportedOperationException("내부 오류: 자체 메뉴 누락")
    }
}

class PartyInviteMenu(
    private val plugin: Main,
    private val partyInviteManager: PartyInviteManager
) : Listener {

    private val ITEMS_PER_PAGE = 45

    fun openInviteMenu(player: Player, party: MythicPartyBridge, page: Int = 0) {
        val title = plugin.configManager.getMenuTitle("party-invite")
        val inventory = Bukkit.createInventory(PartyInviteHolder(party, page), 54, title)

        val worldPlayers = player.world.players.filter {
            it != player && !party.hasPlayer(it)
        }
        val totalPages = (worldPlayers.size - 1) / ITEMS_PER_PAGE + 1

        val startIndex = page * ITEMS_PER_PAGE
        val endIndex = minOf(startIndex + ITEMS_PER_PAGE, worldPlayers.size)

        for (i in startIndex until endIndex) {
            val invitePlayer = worldPlayers[i]
            val playerHead = playerHead(invitePlayer)
            inventory.addItem(playerHead)
        }

        inventory.setItem(49, backButton())

        if (page > 0) {
            inventory.setItem(45, navigationButton("이전", Material.ARROW))
        }
        if (page < totalPages - 1) {
            inventory.setItem(53, navigationButton("다음", Material.ARROW))
        }

        player.openInventory(inventory)
    }

    private fun playerHead(player: Player): ItemStack {
        val item = ItemStack(Material.PLAYER_HEAD)
        val meta = item.itemMeta as SkullMeta

        meta.owningPlayer = player
        meta.displayName(Component.text(player.name)
            .color(NamedTextColor.YELLOW)
            .decoration(TextDecoration.ITALIC, false))

        val lore = mutableListOf<Component>()
        lore.add(Component.text("클릭하여 초대하기")
            .color(NamedTextColor.GRAY)
            .decoration(TextDecoration.ITALIC, false))

        meta.lore(lore)
        item.itemMeta = meta
        return item
    }

    private fun backButton(): ItemStack {
        val item = ItemStack(Material.BARRIER)
        val meta = item.itemMeta
        meta.displayName(Component.text("뒤로가기")
            .color(NamedTextColor.GRAY)
            .decoration(TextDecoration.ITALIC, false))
        item.itemMeta = meta
        return item
    }

    private fun navigationButton(text: String, material: Material): ItemStack {
        val item = ItemStack(material)
        val meta = item.itemMeta
        meta.displayName(Component.text(text)
            .color(NamedTextColor.YELLOW)
            .decoration(TextDecoration.ITALIC, false))
        item.itemMeta = meta
        return item
    }

    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        val holder = event.inventory.holder
        if (holder is PartyInviteHolder) {
            event.isCancelled = true
            val player = event.whoClicked as? Player ?: return
            val party = holder.party

            when (event.rawSlot) {
                in 0 until 45 -> {
                    val clickedItem = event.currentItem
                    if (clickedItem?.type == Material.PLAYER_HEAD) {
                        val skullMeta = clickedItem.itemMeta as? SkullMeta
                        val invitePlayer = skullMeta?.owningPlayer?.player
                        if (invitePlayer != null) {
                            partyInviteManager.inviteToParty(player, invitePlayer)
                            player.closeInventory()
                        }
                    }
                }
                45 -> {
                    if (holder.page > 0) {
                        openInviteMenu(player, party, holder.page - 1)
                    }
                }
                49 -> {
                    player.closeInventory()
                    (plugin.getService(PartyMenu::class.java))?.openPartyMenu(player)
                }
                53 -> {
                    val worldPlayers = player.world.players.filter {
                        it != player && !party.hasPlayer(it)
                    }
                    val totalPages = (worldPlayers.size - 1) / ITEMS_PER_PAGE + 1
                    if (holder.page < totalPages - 1) {
                        openInviteMenu(player, party, holder.page + 1)
                    }
                }
            }
        }
    }
}