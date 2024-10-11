package kr.hwaryuh.community.event

import kr.hwaryuh.community.Main
import kr.hwaryuh.community.service.PartyMenu
import kr.hwaryuh.community.service.ProfileInventoryHolder
import net.Indyuce.mmocore.api.player.PlayerData
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.player.PlayerInteractEntityEvent
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.event.HoverEvent
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration

class PlayerProfileEvent(private val plugin: Main) : Listener {

    @EventHandler
    fun onPlayerInteract(event: PlayerInteractEntityEvent) {
        val player = event.player
        val clickedEntity = event.rightClicked

        if (player.isSneaking && clickedEntity is Player) {
            event.isCancelled = true
            plugin.openProfileMenu(player, clickedEntity, false)
        }
    }

    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        val inventory = event.inventory
        val holder = inventory.holder

        if (holder is ProfileInventoryHolder) {
            val clickedSlot = event.rawSlot

            if (clickedSlot in listOf(0, 8, 9, 17, 18, 27) || clickedSlot >= 54) event.isCancelled = true
            if (event.isShiftClick && clickedSlot >= 54) event.isCancelled = true
            if (event.hotbarButton != -1 && clickedSlot < 54) event.isCancelled = true

            val player = event.whoClicked as Player
            val target = holder.target

            when {
                // 친구 삭제 버튼
                clickedSlot == 53 && event.currentItem?.type == Material.GHAST_TEAR -> {
                    event.isCancelled = true
                    if (plugin.databaseManager.areFriends(player.uniqueId, target.uniqueId)) {
                        plugin.databaseManager.removeFriend(player.uniqueId, target.uniqueId)
                        plugin.databaseManager.removeFriend(target.uniqueId, player.uniqueId)
                        player.sendMessage("§e${target.name}을(를) 친구 목록에서 삭제했습니다.")
                        player.closeInventory()

                        if (holder.fromMenu) {
                            plugin.openFriendsList(player)
                        }
                    }
                }
                // 친구 추가 버튼
                clickedSlot == 53 && event.currentItem?.type == Material.EMERALD -> {
                    event.isCancelled = true
                    if (!plugin.databaseManager.areFriends(player.uniqueId, target.uniqueId)) {
                        if (plugin.databaseManager.hasFriendRequest(player.uniqueId, target.uniqueId)) {
                            player.sendMessage("§e이미 ${target.name}에게 친구 요청을 보냈습니다.")
                        } else {
                            plugin.databaseManager.addFriendRequest(player.uniqueId, target.uniqueId)
                            player.sendMessage("§a${target.name}에게 친구 요청을 보냈습니다.")

                            val message = Component.text()
                                .append(Component.text("${player.name}이(가) 친구 요청을 보냈습니다. "))
                                .append(Component.text("[✔] ")
                                    .color(NamedTextColor.GREEN).decorate(TextDecoration.BOLD)
                                    .clickEvent(ClickEvent.runCommand("/친구 수락 ${player.name}"))
                                    .hoverEvent(HoverEvent.showText(Component.text("클릭하여 수락").color(NamedTextColor.GREEN))))
                                .append(Component.text("[X]")
                                    .color(NamedTextColor.RED).decorate(TextDecoration.BOLD)
                                    .clickEvent(ClickEvent.runCommand("/친구 거절 ${player.name}"))
                                    .hoverEvent(HoverEvent.showText(Component.text("클릭하여 거절").color(NamedTextColor.RED))))
                                .build()

                            target.sendMessage(message)
                        }
                        player.closeInventory()
                    }
                }
                // 뒤로 가기 버튼
                clickedSlot == 45 && event.currentItem?.type == Material.ARROW && holder.fromMenu -> {
                    event.isCancelled = true
                    player.closeInventory()
                    val playerData = PlayerData.get(player)
                    plugin.getService(PartyMenu::class.java)?.openPartyMenu(player, playerData)
                }
            }
        }
    }
}