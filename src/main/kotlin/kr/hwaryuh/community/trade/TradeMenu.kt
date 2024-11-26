package kr.hwaryuh.community.trade

import kr.hwaryuh.community.Main
import kr.hwaryuh.community.data.CurrencyButtonConfig
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder
import org.bukkit.inventory.ItemStack
import java.text.NumberFormat
import java.util.*

class TradeMenuHolder(val playerA: Player, val playerB: Player, private val plugin: Main) : InventoryHolder {
    val inventoryA: Inventory
    val inventoryB: Inventory
    var playerAReady = false
    var playerBReady = false
    var completed = false
    var cancelReason: String? = null
    var cancelingPlayer: Player? = null
    var playerACurrency = 0
    var playerBCurrency = 0

    init {
        val balanceA = Main.economy.getBalance(playerA).toInt()
        val balanceB = Main.economy.getBalance(playerB).toInt()
        val titleFormat = plugin.configManager.getMenuTitle("trade-menu")
        val initialTitleA = formatTitle(titleFormat, balanceA, 0, 0)
        val initialTitleB = formatTitle(titleFormat, balanceB, 0, 0)

        inventoryA = Bukkit.createInventory(this, 54, initialTitleA)
        inventoryB = Bukkit.createInventory(this, 54, initialTitleB)
    }

    override fun getInventory(): Inventory {
        throw UnsupportedOperationException("내부 오류: 자체 메뉴 누락")
    }

    fun updateTitle() {
        val balanceA = Main.economy.getBalance(playerA).toInt()
        val balanceB = Main.economy.getBalance(playerB).toInt()
        val titleFormat = plugin.configManager.getMenuTitle("trade-menu")

        val titleA = formatTitle(titleFormat, balanceA, playerACurrency, playerBCurrency)
        val titleB = formatTitle(titleFormat, balanceB, playerBCurrency, playerACurrency)

        (inventoryA.viewers.firstOrNull() as? Player)?.updateInventoryTitle(inventoryA, titleA)
        (inventoryB.viewers.firstOrNull() as? Player)?.updateInventoryTitle(inventoryB, titleB)
    }

    private fun formatTitle(format: String, balance: Int, added: Int, otherAdded: Int): String {
        return format
            .replace("{balance}", formatUnicode(balance, 10))
            .replace("{added}", formatSubscript(added, 8))
            .replace("{other_added}", formatSubscript(otherAdded, 8))
    }

    private fun Player.updateInventoryTitle(inventory: Inventory, title: String) {
        if (inventory == openInventory.topInventory) {
            openInventory.title = title
        }
    }

    private fun formatUnicode(number: Int, width: Int): String {
        val unicodeDigits = "⓪①②③④⑤⑥⑦⑧⑨"
        return number.toString()
            .padStart(width, ' ')
            .map { char -> if (char in '0'..'9') unicodeDigits[char - '0'] else char }
            .joinToString("")
    }

    private fun formatSubscript(number: Int, width: Int): String {
        val subscriptDigits = "₀₁₂₃₄₅₆₇₈₉"
        return number.toString()
            .padStart(width, ' ')
            .map { char -> if (char in '0'..'9') subscriptDigits[char - '0'] else char }
            .joinToString("")
    }
}

class TradeMenu(private val plugin: Main) {
    private lateinit var tradeManager: TradeManager

    private val leftSlots = listOf(19, 20, 21, 28, 29, 30, 37, 38, 39)
    private val rightSlots = listOf(23, 24, 25, 32, 33, 34, 41, 42, 43)
    private val readyButtonASlot = 47
    private val readyButtonBSlot = 51
    val currencyButtonSlots = listOf(18, 27, 36)
    val currencyAmounts = listOf(100000, 10000, 1000)

    private fun formatNumber(number: Int): String {
        return NumberFormat.getNumberInstance(Locale.KOREA).format(number)
    }

    private fun currencyButton(config: CurrencyButtonConfig): ItemStack {
        val button = ItemStack(config.material)
        val meta = button.itemMeta
        meta.displayName(Component.text("+").append(Component.text(formatNumber(config.amount)))
            .color(NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false))
        meta.setCustomModelData(config.customModelData)
        button.itemMeta = meta
        return button
    }

    private fun playerHead(player: Player): ItemStack {
        val head = ItemStack(Material.PLAYER_HEAD)
        val meta = head.itemMeta as? org.bukkit.inventory.meta.SkullMeta
        meta?.owningPlayer = player
        meta?.displayName(Component.text(player.name).color(NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false))
        head.itemMeta = meta
        return head
    }

    fun setTradeManager(manager: TradeManager) {
        this.tradeManager = manager
    }

    fun createTradeMenu(playerA: Player, playerB: Player): TradeMenuHolder {
        val holder = TradeMenuHolder(playerA, playerB, plugin)
        initializeInventory(holder.inventoryA, playerA, playerB)
        initializeInventory(holder.inventoryB, playerB, playerA)
        return holder
    }

    private fun initializeInventory(inventory: Inventory, currentPlayer: Player, otherPlayer: Player) {
        inventory.setItem(readyButtonASlot, readyButton(false))
        inventory.setItem(readyButtonBSlot, readyButton(false))

        inventory.setItem(2, playerHead(currentPlayer))
        inventory.setItem(6, playerHead(otherPlayer))

        val currencyConfigs = plugin.configManager.getCurrencyButtons()
        currencyConfigs.forEachIndexed { index, config ->
            if (index < currencyButtonSlots.size) {
                val slot = currencyButtonSlots[index]
                val button = currencyButton(config)
                inventory.setItem(slot, button)
            }
        }
    }

    fun updateCurrencyDisplay(holder: TradeMenuHolder) {
        holder.updateTitle()
    }

    fun addCurrency(holder: TradeMenuHolder, player: Player, amount: Int) {
        val currentBalance = Main.economy.getBalance(player)
        val currentTradeAmount = if (player == holder.playerA) holder.playerACurrency else holder.playerBCurrency

        if (currentTradeAmount + amount > currentBalance) {
            return
        }

        if (player == holder.playerA) {
            holder.playerACurrency += amount
        } else {
            holder.playerBCurrency += amount
        }
        updateCurrencyDisplay(holder)
    }

    private fun readyButton(isReady: Boolean): ItemStack {
        val config = plugin.configManager.getReadyButtonConfig()
        val state = if (isReady) config.ready else config.notReady
        val button = ItemStack(state.material)
        val meta = button.itemMeta
        meta.displayName(
            if (isReady)
                Component.text("준비 완료")
                    .color(NamedTextColor.GREEN)
                    .decoration(TextDecoration.ITALIC, false)
            else Component.text("교환 준비")
                .color(NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false))
        meta.setCustomModelData(state.customModelData)
        button.itemMeta = meta
        return button
    }

    fun getPlayerItems(holder: TradeMenuHolder, player: Player): List<ItemStack> {
        val inventory = if (player == holder.playerA) holder.inventoryA else holder.inventoryB
        return leftSlots.mapNotNull { inventory.getItem(it) }
    }

    fun getPlayerSlots(holder: TradeMenuHolder, player: Player): List<Int> {
        return when {
            player == holder.playerA && player.openInventory.topInventory == holder.inventoryA -> leftSlots
            player == holder.playerB && player.openInventory.topInventory == holder.inventoryB -> leftSlots
            else -> emptyList()
        }
    }

    fun updateOtherPlayerView(holder: TradeMenuHolder, currentPlayer: Player, currentSlot: Int, item: ItemStack?) {
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
            readyButtonASlot -> readyButtonBSlot
            readyButtonBSlot -> readyButtonASlot
            else -> slot
        }
    }

    fun isPlayerSlot(holder: TradeMenuHolder, player: Player, slot: Int): Boolean {
        val playerSlots = getPlayerSlots(holder, player)
        return slot in playerSlots
    }

    fun isReadyButtonSlot(slot: Int): Boolean {
        return slot == readyButtonASlot
    }

    fun isPlayerHeadSlot(slot: Int): Boolean {
        return slot == 2 || slot == 6
    }

    fun setPlayerReady(holder: TradeMenuHolder, player: Player) {
        if (player == holder.playerA) {
            holder.playerAReady = true
            holder.inventoryA.setItem(readyButtonASlot, readyButton(true))
            holder.inventoryB.setItem(readyButtonBSlot, readyButton(true))
        } else {
            holder.playerBReady = true
            holder.inventoryB.setItem(readyButtonASlot, readyButton(true))
            holder.inventoryA.setItem(readyButtonBSlot, readyButton(true))
        }
    }

    fun areBothPlayersReady(holder: TradeMenuHolder): Boolean {
        return holder.playerAReady && holder.playerBReady
    }

    fun clearPlayerItems(holder: TradeMenuHolder, player: Player) {
        val inventory = if (player == holder.playerA) holder.inventoryA else holder.inventoryB
        val otherInventory = if (player == holder.playerA) holder.inventoryB else holder.inventoryA
        leftSlots.forEach {
            inventory.setItem(it, null)
            otherInventory.setItem(mapSlotToOtherView(it), null)
        }
    }

    fun openInventory(player: Player, holder: TradeMenuHolder) {
        val inventory = if (player == holder.playerA) holder.inventoryA else holder.inventoryB
        player.openInventory(inventory)
    }

    fun getFirstEmptySlot(holder: TradeMenuHolder, player: Player): Int {
        val inventory = when {
            player == holder.playerA && player.openInventory.topInventory == holder.inventoryA -> holder.inventoryA
            player == holder.playerB && player.openInventory.topInventory == holder.inventoryB -> holder.inventoryB
            else -> return -1
        }
        return leftSlots.firstOrNull { inventory.getItem(it) == null } ?: -1
    }

    fun completeTrade(holder: TradeMenuHolder): Boolean {
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

        val economyA = Main.economy.getBalance(holder.playerA)
        val economyB = Main.economy.getBalance(holder.playerB)

        if (economyA < holder.playerACurrency || economyB < holder.playerBCurrency) {
            holder.playerA.sendMessage(Component.text("한 플레이어의 잔액이 부족해 거래가 취소되었습니다.").color(NamedTextColor.RED))
            holder.playerB.sendMessage(Component.text("한 플레이어의 잔액이 부족해 거래가 취소되었습니다.").color(NamedTextColor.RED))
            cancelTrade(holder)
            return false
        }

        Main.economy.withdrawPlayer(holder.playerA, holder.playerACurrency.toDouble())
        Main.economy.depositPlayer(holder.playerB, holder.playerACurrency.toDouble())
        Main.economy.withdrawPlayer(holder.playerB, holder.playerBCurrency.toDouble())
        Main.economy.depositPlayer(holder.playerA, holder.playerBCurrency.toDouble())

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

            val messageA = if (holder.playerACurrency > 0 || holder.playerBCurrency > 0) {
                Component.text("교환이 완료되었습니다. ").color(NamedTextColor.GREEN)
                    .append(Component.text("(+${formatNumber(holder.playerBCurrency)}, -${formatNumber(holder.playerACurrency)})").color(NamedTextColor.GOLD))
            } else {
                Component.text("교환이 완료되었습니다.").color(NamedTextColor.GREEN)
            }

            val messageB = if (holder.playerACurrency > 0 || holder.playerBCurrency > 0) {
                Component.text("교환이 완료되었습니다. ").color(NamedTextColor.GREEN)
                    .append(Component.text("(+${formatNumber(holder.playerACurrency)}, -${formatNumber(holder.playerBCurrency)})").color(NamedTextColor.GOLD))
            } else {
                Component.text("교환이 완료되었습니다.").color(NamedTextColor.GREEN)
            }

            holder.playerA.sendMessage(messageA.color(NamedTextColor.GREEN))
            holder.playerB.sendMessage(messageB.color(NamedTextColor.GREEN))
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

    fun cancelTrade(holder: TradeMenuHolder) {
        returnItems(holder.playerA, holder)
        returnItems(holder.playerB, holder)

        plugin.server.scheduler.runTask(plugin, Runnable {
            holder.playerA.closeInventory()
            holder.playerB.closeInventory()
        })
    }

    private fun returnItems(player: Player, holder: TradeMenuHolder) {
        val items = getPlayerItems(holder, player)
        for (item in items) {
            player.inventory.addItem(item)
        }
        clearPlayerItems(holder, player)
    }
}