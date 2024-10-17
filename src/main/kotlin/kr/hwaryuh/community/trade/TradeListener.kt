package kr.hwaryuh.community.trade

import kr.hwaryuh.community.Main
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryDragEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.inventory.InventoryAction

class TradeListener(private val plugin: Main) : Listener {
    private lateinit var tradeMenu: TradeMenu
    private lateinit var tradeManager: TradeManager

    fun setTradeMenu(menu: TradeMenu) {
        this.tradeMenu = menu
    }

    fun setTradeManager(manager: TradeManager) {
        this.tradeManager = manager
    }

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
                        when (event.action) {
                            InventoryAction.PICKUP_ALL, InventoryAction.PICKUP_HALF, InventoryAction.PICKUP_ONE, InventoryAction.PICKUP_SOME -> {
                                // 플레이어가 자신의 교환 슬롯에서 아이템을 가져가는 경우
                                plugin.server.scheduler.runTask(plugin, Runnable {
                                    tradeMenu.updateOtherPlayerView(holder, player, clickedSlot, null)
                                })
                            }
                            InventoryAction.PLACE_ALL, InventoryAction.PLACE_ONE, InventoryAction.PLACE_SOME -> {
                                // 플레이어가 자신의 교환 슬롯에 아이템을 놓는 경우
                                plugin.server.scheduler.runTask(plugin, Runnable {
                                    val updatedItem = event.view.topInventory.getItem(clickedSlot)
                                    tradeMenu.updateOtherPlayerView(holder, player, clickedSlot, updatedItem)
                                })
                            }
                            InventoryAction.MOVE_TO_OTHER_INVENTORY -> {
                                // Shift+클릭으로 아이템을 인벤토리로 이동
                                event.isCancelled = true
                                val clickedItem = event.currentItem ?: return
                                player.inventory.addItem(clickedItem)
                                event.currentItem = null
                                tradeMenu.updateOtherPlayerView(holder, player, clickedSlot, null)
                            }
                            else -> {}
                        }
                    }
                } else {
                    // 상대방의 슬롯이나 다른 슬롯을 클릭한 경우
                    event.isCancelled = true
                    if (tradeMenu.isReadyButtonSlot(clickedSlot)) {
                        handleReadyButtonClick(event, player, holder)
                    }
                }
            }
            clickedSlot >= 54 -> {
                // 플레이어 인벤토리에서 교환 창으로 Shift+클릭으로 아이템 이동
                if (event.action == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
                    event.isCancelled = true
                    if (isPlayerReady(holder, player)) {
                        player.sendMessage(Component.text("준비 상태에서는 아이템을 변경할 수 없습니다.").color(NamedTextColor.RED))
                    } else {
                        val clickedItem = event.currentItem ?: return
                        val emptySlot = tradeMenu.getFirstEmptySlot(holder, player)
                        if (emptySlot != -1) {
                            val inventoryView = event.view
                            inventoryView.setItem(emptySlot, clickedItem)
                            event.currentItem = null
                            plugin.server.scheduler.runTask(plugin, Runnable {
                                val updatedItem = inventoryView.topInventory.getItem(emptySlot)
                                tradeMenu.updateOtherPlayerView(holder, player, emptySlot, updatedItem)
                            })
                        } else {
                            player.sendMessage(Component.text("더 이상 아이템을 교환할 수 없습니다.").color(NamedTextColor.YELLOW))
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    fun onInventoryDrag(event: InventoryDragEvent) {
        val inventory = event.inventory
        val holder = inventory.holder as? TradeInventoryHolder ?: return
        val player = event.whoClicked as? Player ?: return

        if (isPlayerReady(holder, player)) {
            event.isCancelled = true
            player.sendMessage(Component.text("준비 상태에서는 아이템을 변경할 수 없습니다.").color(NamedTextColor.RED))
            return
        }

        val allowedSlots = tradeMenu.getPlayerSlots(holder, player)
        if (event.rawSlots.any { it !in allowedSlots && it < 54 }) {
            event.isCancelled = true
            return
        }

        // 드래그된 아이템 처리
        event.newItems.forEach { (slot, item) ->
            if (slot in allowedSlots) {
                tradeMenu.updateOtherPlayerView(holder, player, slot, item)
            }
        }
    }

    private fun isPlayerReady(holder: TradeInventoryHolder, player: Player): Boolean {
        return if (player == holder.playerA) holder.playerAReady else holder.playerBReady
    }

    private fun handleReadyButtonClick(event: InventoryClickEvent, player: Player, holder: TradeInventoryHolder) {
        event.isCancelled = true

        if (!isPlayerReady(holder, player)) {
            tradeMenu.setPlayerReady(holder, player)
            if (tradeMenu.areBothPlayersReady(holder)) {
                if (!tradeMenu.completeTrade(holder)) tradeMenu.cancelTrade(holder)
            }
        } else {
            player.sendMessage(Component.text("이미 준비 상태입니다.").color(NamedTextColor.YELLOW))
        }
    }

    @EventHandler
    fun onInventoryClose(event: InventoryCloseEvent) {
        val inventory = event.inventory
        val holder = inventory.holder as? TradeInventoryHolder ?: return

        if (holder.completed) return

        val closingPlayer = event.player as Player
        val otherPlayer = if (closingPlayer == holder.playerA) holder.playerB else holder.playerA

        if (holder.cancelingPlayer == null) {
            holder.cancelingPlayer = closingPlayer
            closingPlayer.sendMessage(Component.text("교환이 취소되었습니다.").color(NamedTextColor.RED))

            tradeMenu.cancelTrade(holder)
            tradeManager.endTrade(closingPlayer)
            tradeManager.endTrade(otherPlayer)

            if (otherPlayer.openInventory.topInventory.holder == holder) {
                otherPlayer.closeInventory()
            }
            otherPlayer.sendMessage(Component.text("상대방이 교환을 취소했습니다.").color(NamedTextColor.RED))

            plugin.server.scheduler.runTask(plugin, Runnable {
                if (tradeManager.isPlayerTrading(closingPlayer) || tradeManager.isPlayerTrading(otherPlayer)) {
                    tradeManager.forceEndAllTrades()
                    plugin.logger.warning("Forced to end all trades after inventory close.")
                }
            })
        }
    }
}