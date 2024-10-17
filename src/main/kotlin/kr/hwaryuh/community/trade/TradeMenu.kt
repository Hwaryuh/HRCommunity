package kr.hwaryuh.community.trade

import kr.hwaryuh.community.Main
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder
import org.bukkit.inventory.ItemStack

class TradeInventoryHolder(val playerA: Player, val playerB: Player) : InventoryHolder {
    val inventoryA: Inventory = Bukkit.createInventory(this, 54, "교환: ${playerA.name} | ${playerB.name}")
    val inventoryB: Inventory = Bukkit.createInventory(this, 54, "교환: ${playerB.name} | ${playerA.name}")
    var playerAReady = false
    var playerBReady = false
    var completed = false
    var cancelReason: String? = null
    var cancelingPlayer: Player? = null

    override fun getInventory(): Inventory {
        throw UnsupportedOperationException("내부 오류: 자체 메뉴 누락")
    }
}

class TradeMenu(private val plugin: Main) {
    private lateinit var tradeManager: TradeManager

    private val leftSlots = listOf(10, 11, 12, 19, 20, 21, 28, 29, 30, 37, 38, 39)
    private val rightSlots = listOf(14, 15, 16, 23, 24, 25, 32, 33, 34, 41, 42, 43)
    private val readyButtonSlot = 47
    private val otherReadyButtonSlot = 51

    fun setTradeManager(manager: TradeManager) {
        this.tradeManager = manager
    }

    fun createTradeInventory(playerA: Player, playerB: Player): TradeInventoryHolder {
        val holder = TradeInventoryHolder(playerA, playerB)
        initializeInventory(holder.inventoryA)
        initializeInventory(holder.inventoryB)
        return holder
    }

    private fun initializeInventory(inventory: Inventory) {
        inventory.setItem(readyButtonSlot, readyButton(false))
        inventory.setItem(otherReadyButtonSlot, readyButton(false))
    }

    private fun readyButton(isReady: Boolean): ItemStack {
        val button = ItemStack(if (isReady) Material.LIME_WOOL else Material.RED_WOOL)
        val meta = button.itemMeta
        meta.displayName(
            if (isReady)
                Component.text("준비 완료")
                    .color(NamedTextColor.GREEN)
                    .decoration(TextDecoration.ITALIC, false)
            else Component.text("교환 준비")
                .color(NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false))
        button.itemMeta = meta
        return button
    }

    fun getPlayerItems(holder: TradeInventoryHolder, player: Player): List<ItemStack> {
        val inventory = if (player == holder.playerA) holder.inventoryA else holder.inventoryB
        return leftSlots.mapNotNull { inventory.getItem(it) }
    }

    fun getPlayerSlots(holder: TradeInventoryHolder, player: Player): List<Int> {
        return when {
            player == holder.playerA && player.openInventory.topInventory == holder.inventoryA -> leftSlots
            player == holder.playerB && player.openInventory.topInventory == holder.inventoryB -> leftSlots
            else -> emptyList()
        }
    }

    fun updateOtherPlayerView(holder: TradeInventoryHolder, currentPlayer: Player, currentSlot: Int, item: ItemStack?) {
        val (otherPlayer, otherInventory) = when (currentPlayer) {
            holder.playerA -> holder.playerB to holder.inventoryB
            holder.playerB -> holder.playerA to holder.inventoryA
            else -> return
        }
        val otherSlot = mapSlotToOtherView(currentSlot)
        otherInventory.setItem(otherSlot, item)
    }

    private fun mapSlotToOtherView(slot: Int): Int {
        return when (slot) {
            in leftSlots -> rightSlots[leftSlots.indexOf(slot)]
            in rightSlots -> leftSlots[rightSlots.indexOf(slot)]
            readyButtonSlot -> otherReadyButtonSlot
            otherReadyButtonSlot -> readyButtonSlot
            else -> slot
        }
    }

    fun isPlayerSlot(holder: TradeInventoryHolder, player: Player, slot: Int): Boolean {
        val playerSlots = getPlayerSlots(holder, player)
        return slot in playerSlots
    }

    fun isReadyButtonSlot(slot: Int): Boolean {
        return slot == readyButtonSlot
    }

    fun setPlayerReady(holder: TradeInventoryHolder, player: Player) {
        if (player == holder.playerA) {
            holder.playerAReady = true
            holder.inventoryA.setItem(readyButtonSlot, readyButton(true))
            holder.inventoryB.setItem(otherReadyButtonSlot, readyButton(true))
        } else {
            holder.playerBReady = true
            holder.inventoryB.setItem(readyButtonSlot, readyButton(true))
            holder.inventoryA.setItem(otherReadyButtonSlot, readyButton(true))
        }
    }

    fun areBothPlayersReady(holder: TradeInventoryHolder): Boolean {
        return holder.playerAReady && holder.playerBReady
    }

    fun clearPlayerItems(holder: TradeInventoryHolder, player: Player) {
        val inventory = if (player == holder.playerA) holder.inventoryA else holder.inventoryB
        val otherInventory = if (player == holder.playerA) holder.inventoryB else holder.inventoryA
        leftSlots.forEach {
            inventory.setItem(it, null)
            otherInventory.setItem(mapSlotToOtherView(it), null)
        }
    }

    fun openInventory(player: Player, holder: TradeInventoryHolder) {
        val inventory = if (player == holder.playerA) holder.inventoryA else holder.inventoryB
        player.openInventory(inventory)
    }

    fun getFirstEmptySlot(holder: TradeInventoryHolder, player: Player): Int {
        val inventory = when {
            player == holder.playerA && player.openInventory.topInventory == holder.inventoryA -> holder.inventoryA
            player == holder.playerB && player.openInventory.topInventory == holder.inventoryB -> holder.inventoryB
            else -> return -1
        }
        return leftSlots.firstOrNull { inventory.getItem(it) == null } ?: -1
    }

    fun completeTrade(holder: TradeInventoryHolder): Boolean {
        if (!::tradeManager.isInitialized) {
            plugin.logger.severe("TradeManager is not initialized in TradeMenu!")
            return false
        }
        val playerAItems = getPlayerItems(holder, holder.playerA)
        val playerBItems = getPlayerItems(holder, holder.playerB)

        val playerAHasSpace = hasEnoughSpace(holder.playerA, playerBItems)
        val playerBHasSpace = hasEnoughSpace(holder.playerB, playerAItems)

        if (!playerAHasSpace || !playerBHasSpace) {
            val (fullPlayer, otherPlayer) = if (!playerAHasSpace)
                holder.playerA to holder.playerB else holder.playerB to holder.playerA

            holder.cancelReason = "${fullPlayer.name}의 인벤토리 공간이 부족해 교환이 취소되었습니다."
            fullPlayer.sendMessage(Component.text("인벤토리 공간이 부족해 교환이 취소되었습니다.").color(NamedTextColor.RED))
            otherPlayer.sendMessage(Component.text(holder.cancelReason!!).color(NamedTextColor.RED))

            cancelTrade(holder)
            return false
        }

        clearPlayerItems(holder, holder.playerA)
        clearPlayerItems(holder, holder.playerB)

        for (item in playerAItems) {
            holder.playerB.inventory.addItem(item)
        }
        for (item in playerBItems) {
            holder.playerA.inventory.addItem(item)
        }

        holder.completed = true

        plugin.server.scheduler.runTask(plugin, Runnable {
            holder.playerA.closeInventory()
            holder.playerB.closeInventory()

            holder.playerA.sendMessage(Component.text("교환이 완료되었습니다.").color(NamedTextColor.GREEN))
            holder.playerB.sendMessage(Component.text("교환이 완료되었습니다.").color(NamedTextColor.GREEN))
        })
        tradeManager.endTrade(holder.playerA)
        tradeManager.endTrade(holder.playerB)

        return true
    }

    private fun hasEnoughSpace(player: Player, items: List<ItemStack>): Boolean {
        val inventory = player.inventory
        val emptySlots = inventory.storageContents.count { it == null || it.type == Material.AIR }
        val stackableItems = items.groupBy { it.type }

        var requiredSlots = 0
        for ((_, stack) in stackableItems) {
            val totalAmount = stack.sumOf { it.amount }
            val maxStackSize = stack.first().maxStackSize
            requiredSlots += (totalAmount + maxStackSize - 1) / maxStackSize
        }

        return emptySlots >= requiredSlots
    }

    fun cancelTrade(holder: TradeInventoryHolder) {
        returnItems(holder.playerA, holder)
        returnItems(holder.playerB, holder)

        plugin.server.scheduler.runTask(plugin, Runnable {
            holder.playerA.closeInventory()
            holder.playerB.closeInventory()
        })
    }

    private fun returnItems(player: Player, holder: TradeInventoryHolder) {
        val items = getPlayerItems(holder, player)
        for (item in items) {
            player.inventory.addItem(item)
        }
        clearPlayerItems(holder, player)
    }
}