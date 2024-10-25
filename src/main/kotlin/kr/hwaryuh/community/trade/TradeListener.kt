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
import org.bukkit.event.player.PlayerDropItemEvent

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
        val holder = inventory.holder as? TradeMenuHolder ?: return
        val player = event.whoClicked as? Player ?: return
        val clickedSlot = event.rawSlot

        if (event.action == InventoryAction.DROP_ONE_SLOT || event.action == InventoryAction.DROP_ALL_SLOT) {
            event.isCancelled = true
            return
        }

        when {
            clickedSlot in 0 until 54 -> {
                if (tradeMenu.isPlayerHeadSlot(clickedSlot)) {
                    event.isCancelled = true
                    return
                }
                if (tradeMenu.isPlayerSlot(holder, player, clickedSlot)) {
                    if (isPlayerReady(holder, player)) {
                        event.isCancelled = true
                    } else {
                        when (event.action) {
                            InventoryAction.PICKUP_ALL, InventoryAction.PICKUP_HALF, InventoryAction.PICKUP_ONE, InventoryAction.PICKUP_SOME -> {
                                event.isCancelled = true
                            }
                            InventoryAction.PLACE_ALL, InventoryAction.PLACE_ONE, InventoryAction.PLACE_SOME -> {
                                // +
                                if (event.view.topInventory.getItem(clickedSlot) != null) {
                                    event.isCancelled = true
                                    return
                                }
                                plugin.server.scheduler.runTask(plugin, Runnable {
                                    val updatedItem = event.view.topInventory.getItem(clickedSlot)
                                    tradeMenu.updateOtherPlayerView(holder, player, clickedSlot, updatedItem)
                                })
                            }
                            InventoryAction.SWAP_WITH_CURSOR -> {
                                event.isCancelled = true
                            }
                            InventoryAction.MOVE_TO_OTHER_INVENTORY -> {
                                event.isCancelled = true
                            }
                            else -> {
                                event.isCancelled = true
                            }
                        }
                    }
                } else {
                    event.isCancelled = true
                    when {
                        tradeMenu.isReadyButtonSlot(clickedSlot) -> {
                            handleReadyButtonClick(event, player, holder)
                        }
                        clickedSlot in tradeMenu.currencyButtonSlots -> {
                            if (isPlayerReady(holder, player)) {
                            } else {
                                val amount = tradeMenu.currencyAmounts[tradeMenu.currencyButtonSlots.indexOf(clickedSlot)]
                                tradeMenu.addCurrency(holder, player, amount)
                            }
                        }
                    }
                }
            }
            clickedSlot >= 54 -> {
                if (event.action == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
                    if (isPlayerReady(holder, player)) {
                        event.isCancelled = true
                    } else {
                        val clickedItem = event.currentItem ?: return
                        val emptySlot = tradeMenu.getFirstEmptySlot(holder, player)
                        if (emptySlot != -1) {
                            event.isCancelled = true
                            val inventoryView = event.view
                            inventoryView.setItem(emptySlot, clickedItem)
                            event.currentItem = null
                            plugin.server.scheduler.runTask(plugin, Runnable {
                                val updatedItem = inventoryView.topInventory.getItem(emptySlot)
                                tradeMenu.updateOtherPlayerView(holder, player, emptySlot, updatedItem)
                            })
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    fun onInventoryDrag(event: InventoryDragEvent) {
        val inventory = event.inventory
        val holder = inventory.holder as? TradeMenuHolder ?: return
        val player = event.whoClicked as? Player ?: return

        if (isPlayerReady(holder, player)) {
            event.isCancelled = true
            return
        }

        val allowedSlots = tradeMenu.getPlayerSlots(holder, player)

        // +
        val hasExistingItems = event.rawSlots.any { slot ->
            slot < 54 && slot in allowedSlots && inventory.getItem(slot) != null
        }

        if (hasExistingItems || event.rawSlots.any { it !in allowedSlots && it < 54 }) {
            event.isCancelled = true
            if (hasExistingItems) {
                player.sendMessage(Component.text("X").color(NamedTextColor.RED))
            }
            return
        }

        event.newItems.forEach { (slot, item) ->
            if (slot in allowedSlots) {
                tradeMenu.updateOtherPlayerView(holder, player, slot, item)
            }
        }
    }

    private fun isPlayerReady(holder: TradeMenuHolder, player: Player): Boolean {
        return if (player == holder.playerA) holder.playerAReady else holder.playerBReady
    }

    private fun handleReadyButtonClick(event: InventoryClickEvent, player: Player, holder: TradeMenuHolder) {
        event.isCancelled = true

        if (!isPlayerReady(holder, player)) {
            tradeMenu.setPlayerReady(holder, player)
            if (tradeMenu.areBothPlayersReady(holder)) {
                if (!tradeMenu.completeTrade(holder)) tradeMenu.cancelTrade(holder)
            }
        }
    }

    @EventHandler
    fun onInventoryClose(event: InventoryCloseEvent) {
        val inventory = event.inventory
        val holder = inventory.holder as? TradeMenuHolder ?: return

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

    @EventHandler
    fun onPlayerDropItem(event: PlayerDropItemEvent) {
        if (tradeManager.isPlayerTrading(event.player)) event.isCancelled = true
    }
}