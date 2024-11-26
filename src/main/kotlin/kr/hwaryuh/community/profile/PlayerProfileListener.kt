package kr.hwaryuh.community.profile

import kr.hwaryuh.community.Main
import kr.hwaryuh.community.party.PartyMenu
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.player.PlayerInteractEntityEvent

class PlayerProfileListener(private val plugin: Main) : Listener {

    @EventHandler
    fun onPlayerInteract(event: PlayerInteractEntityEvent) {
        val player = event.player
        val clickedEntity = event.rightClicked

        if (player.isSneaking && clickedEntity is Player) {
            event.isCancelled = true
            plugin.openProfileMenu(player, clickedEntity, false, PreviousMenuType.NONE)
        }
    }

    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        val inventory = event.inventory
        val holder = inventory.holder

        if (holder is ProfileMenuHolder) {
            val clickedSlot = event.rawSlot
            event.isCancelled = true

            when (clickedSlot) {
                53 -> handleFriendship(event, holder)
                45 -> handleBackButton(event, holder)
            }
        }
    }

    private fun handleFriendship(event: InventoryClickEvent, holder: ProfileMenuHolder) {
        val player = event.whoClicked as Player
        val target = holder.target

        when (event.currentItem?.type) {
            Material.GHAST_TEAR -> {
                if (plugin.databaseManager.areFriends(player.uniqueId, target.uniqueId)) {
                    try {
                        plugin.friendsManager.deleteFriend(player, target.uniqueId)
                        player.closeInventory()
                        when (holder.previousMenu) {
                            PreviousMenuType.PARTY -> plugin.getService(PartyMenu::class.java)?.openPartyMenu(player)
                            PreviousMenuType.FRIENDS -> plugin.openFriendsList(player)
                            PreviousMenuType.NONE -> {}
                        }
                    } catch (e: IllegalStateException) {
                        player.sendMessage(Component.text(e.message ?: "알 수 없는 오류가 발생했습니다.").color(NamedTextColor.RED))
                    }
                }
            }
            Material.EMERALD -> {
                if (!plugin.databaseManager.areFriends(player.uniqueId, target.uniqueId)) {
                    try {
                        plugin.friendsManager.addFriend(player, target as Player)
                        plugin.openProfileMenu(player, target, holder.fromMenu, holder.previousMenu)
                    } catch (e: IllegalStateException) {
                        player.sendMessage(Component.text(e.message ?: "알 수 없는 오류가 발생했습니다.").color(NamedTextColor.RED))
                    }
                }
            }
            else -> {}
        }
    }

    private fun handleBackButton(event: InventoryClickEvent, holder: ProfileMenuHolder) {
        if (event.currentItem?.type == Material.ARROW && holder.fromMenu) {
            val player = event.whoClicked as Player
            player.closeInventory()
            when (holder.previousMenu) {
                PreviousMenuType.PARTY -> plugin.getService(PartyMenu::class.java)?.openPartyMenu(player)
                PreviousMenuType.FRIENDS -> plugin.openFriendsList(player)
                PreviousMenuType.NONE -> {}
            }
        }
    }
}