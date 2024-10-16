package kr.hwaryuh.community.trade

import kr.hwaryuh.community.Main
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.inventory.InventoryDragEvent

class TradeListener(private val plugin: Main, private val tradeMenu: TradeMenu, private val tradeManager: TradeManager) : Listener {

    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        val inventory = event.inventory
        val holder = inventory.holder as? TradeInventoryHolder ?: return
        val player = event.whoClicked as? Player ?: return
        val clickedSlot = event.rawSlot

        when {
            clickedSlot in 0 until 54 -> {
                if (tradeMenu.isPlayerSlot(holder, player, clickedSlot)) {
                    if (isPlayerReady(holder, player)) {
                        event.isCancelled = true
                        player.sendMessage(Component.text("준비 상태에서는 아이템을 변경할 수 없습니다.").color(NamedTextColor.RED))
                    } else {
                        handlePlayerSlotClick(event, player, clickedSlot, holder)
                    }
                } else if (tradeMenu.isReadyButtonSlot(clickedSlot)) {
                    handleReadyButtonClick(event, player, holder)
                } else {
                    event.isCancelled = true
                }
            }
            clickedSlot >= 54 -> {
                if (isPlayerReady(holder, player)) {
                    event.isCancelled = true
                    player.sendMessage(Component.text("준비 상태에서는 아이템을 변경할 수 없습니다.").color(NamedTextColor.RED))
                } else {
                    handlePlayerInventoryClick(event, player, holder)
                }
            }
        }
    }

    private fun isPlayerReady(holder: TradeInventoryHolder, player: Player): Boolean {
        return if (player == holder.playerA) holder.playerAReady else holder.playerBReady
    }

    private fun handlePlayerSlotClick(event: InventoryClickEvent, player: Player, clickedSlot: Int, holder: TradeInventoryHolder) {
        val clickedItem = event.currentItem

        if (clickedItem != null) {
            event.currentItem = null
            player.inventory.addItem(clickedItem)
            tradeMenu.updateOtherPlayerView(holder, player, clickedSlot, null)
        }
    }

    private fun handleReadyButtonClick(event: InventoryClickEvent, player: Player, holder: TradeInventoryHolder) {
        event.isCancelled = true

        if (!isPlayerReady(holder, player)) { tradeMenu.setPlayerReady(holder, player)
            if (tradeMenu.areBothPlayersReady(holder)) {
                if (!tradeMenu.completeTrade(holder)) tradeMenu.cancelTrade(holder)
            }
        } else {
            player.sendMessage(Component.text("이미 준비 상태입니다.").color(NamedTextColor.YELLOW))
        }
    }

    private fun handlePlayerInventoryClick(event: InventoryClickEvent, player: Player, holder: TradeInventoryHolder) {
        val clickedItem = event.currentItem ?: return

        if (tradeMenu.setPlayerItem(holder, player, clickedItem)) {
            event.currentItem = null
        } else {
            player.sendMessage(Component.text("더 이상 아이템을 교환할 수 없습니다.").color(NamedTextColor.YELLOW))
        }
    }

    @EventHandler
    fun onInventoryDrag(event: InventoryDragEvent) {
        val inventory = event.inventory
        val holder = inventory.holder as? TradeInventoryHolder ?: return
        val player = event.whoClicked as? Player ?: return

        if (isPlayerReady(holder, player) || event.rawSlots.any { it in 0 until 54 }) {
            event.isCancelled = true
            if (isPlayerReady(holder, player)) {
                player.sendMessage(Component.text("준비 상태에서는 아이템을 변경할 수 없습니다.").color(NamedTextColor.RED))
            }
        }
    }

    @EventHandler
    fun onInventoryClose(event: InventoryCloseEvent) {
        val inventory = event.inventory
        val holder = inventory.holder as? TradeInventoryHolder ?: return

        if (holder.completed) return

        val closingPlayer = event.player as Player
        val otherPlayer = if (closingPlayer == holder.playerA) holder.playerB else holder.playerA

        if (holder.cancelReason == null) {
            closingPlayer.sendMessage(Component.text("교환이 취소되었습니다.").color(NamedTextColor.RED))
            otherPlayer.sendMessage(Component.text("상대방이 교환을 취소했습니다.").color(NamedTextColor.RED))
        }

        tradeMenu.cancelTrade(holder)
        tradeManager.endTrade(closingPlayer)
    }
}