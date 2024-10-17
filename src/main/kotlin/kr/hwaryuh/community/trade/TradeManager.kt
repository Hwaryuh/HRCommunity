package kr.hwaryuh.community.trade

import kr.hwaryuh.community.Main
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class TradeManager(private val plugin: Main) {
    private lateinit var tradeMenu: TradeMenu

    private val tradeRequests = ConcurrentHashMap<UUID, UUID>()
    private val activeTrades = ConcurrentHashMap<UUID, UUID>()
    private val endingTrades = mutableSetOf<UUID>()

    fun setTradeMenu(menu: TradeMenu) {
        this.tradeMenu = menu
    }

    fun sendTradeRequest(sender: Player, targetName: String) {
        val target = Bukkit.getPlayer(targetName)
        if (target == null) {
            sender.sendMessage(Component.text("플레이어를 찾을 수 없습니다.").color(NamedTextColor.RED))
            return
        }

        if (sender == target) {
            sender.sendMessage(Component.text("자신과 교환할 수 없습니다.").color(NamedTextColor.RED))
            return
        }

        if (isPlayerTrading(sender) || isPlayerTrading(target)) {
            forceEndAllTrades()
            sender.sendMessage(Component.text("이전 거래 상태가 제대로 정리되지 않아 모든 거래를 초기화했습니다. 다시 시도해주세요.").color(NamedTextColor.YELLOW))
            return
        }

        if (tradeRequests.containsKey(target.uniqueId)) {
            sender.sendMessage(Component.text("이미 해당 플레이어에게 교환 요청이 진행 중입니다.").color(NamedTextColor.RED))
            return
        }

        tradeRequests[target.uniqueId] = sender.uniqueId
        sender.sendMessage(Component.text("${target.name}에게 교환 요청을 보냈습니다.").color(NamedTextColor.GREEN))

        val acceptComponent = Component.text("[✔]")
            .color(NamedTextColor.GREEN)
            .decorate(TextDecoration.BOLD)
            .clickEvent(ClickEvent.runCommand("/교환 수락 ${sender.name}"))
        val rejectComponent = Component.text("[X]")
            .color(NamedTextColor.RED)
            .decorate(TextDecoration.BOLD)
            .clickEvent(ClickEvent.runCommand("/교환 거절 ${sender.name}"))

        target.sendMessage(Component.text("${sender.name}이(가) 교환을 요청했습니다. ")
            .append(acceptComponent)
            .append(Component.space())
            .append(rejectComponent))

        Bukkit.getScheduler().runTaskLater(plugin, Runnable {
            if (tradeRequests[target.uniqueId] == sender.uniqueId) {
                tradeRequests.remove(target.uniqueId)
                sender.sendMessage(Component.text("${target.name}에게 보낸 교환 요청이 만료되었습니다.").color(NamedTextColor.YELLOW))
                target.sendMessage(Component.text("${sender.name}의 교환 요청이 만료되었습니다.").color(NamedTextColor.YELLOW))
            }
        }, 20L * 30)
    }

    fun acceptTradeRequest(player: Player, senderName: String) {
        val sender = Bukkit.getPlayer(senderName)
        if (sender == null) {
            player.sendMessage(Component.text("요청한 플레이어를 찾을 수 없습니다.").color(NamedTextColor.RED))
            return
        }

        if (tradeRequests[player.uniqueId] != sender.uniqueId) {
            player.sendMessage(Component.text("해당 플레이어의 교환 요청이 없습니다.").color(NamedTextColor.RED))
            return
        }

        if (isPlayerTrading(player) || isPlayerTrading(sender)) {
            player.sendMessage(Component.text("이미 교환 중인 플레이어입니다.").color(NamedTextColor.RED))
            sender.sendMessage(Component.text("이미 교환 중인 플레이어입니다.").color(NamedTextColor.RED))
            tradeRequests.remove(player.uniqueId)
            return
        }

        tradeRequests.remove(player.uniqueId)
        activeTrades[player.uniqueId] = sender.uniqueId
        activeTrades[sender.uniqueId] = player.uniqueId

        val holder = tradeMenu.createTradeInventory(sender, player)
        tradeMenu.openInventory(sender, holder)
        tradeMenu.openInventory(player, holder)

        // player.sendMessage(Component.text("교환 요청을 수락했습니다.").color(NamedTextColor.GREEN))
        // sender.sendMessage(Component.text("${player.name}이(가) 교환 요청을 수락했습니다.").color(NamedTextColor.GREEN))
    }

    fun rejectTradeRequest(player: Player, senderName: String) {
        val sender = Bukkit.getPlayer(senderName)
        if (sender == null) {
            player.sendMessage(Component.text("요청한 플레이어를 찾을 수 없습니다.").color(NamedTextColor.RED))
            return
        }

        if (tradeRequests[player.uniqueId] != sender.uniqueId) {
            player.sendMessage(Component.text("해당 플레이어의 교환 요청이 없습니다.").color(NamedTextColor.RED))
            return
        }

        tradeRequests.remove(player.uniqueId)
        player.sendMessage(Component.text("교환 요청을 거절했습니다.").color(NamedTextColor.YELLOW))
        sender.sendMessage(Component.text("${player.name}이(가) 교환 요청을 거절했습니다.").color(NamedTextColor.RED))
    }

    fun endTrade(player: Player) {
        if (endingTrades.add(player.uniqueId)) {
            val otherPlayerUUID = activeTrades.remove(player.uniqueId)
            if (otherPlayerUUID != null) {
                activeTrades.remove(otherPlayerUUID)
                tradeRequests.remove(otherPlayerUUID)
                endingTrades.remove(otherPlayerUUID)
            }
            tradeRequests.remove(player.uniqueId)

            plugin.logger.info("Trade ended for ${player.name}. Active trades: ${activeTrades.size}, Trade requests: ${tradeRequests.size}")

            endingTrades.remove(player.uniqueId)
        }
    }

    fun isPlayerTrading(player: Player): Boolean {
        return activeTrades.containsKey(player.uniqueId)
    }

    fun forceEndAllTrades() {
        activeTrades.clear()
        tradeRequests.clear()
        plugin.logger.info("All trades forcibly ended. Active trades: ${activeTrades.size}, Trade requests: ${tradeRequests.size}")
    }
}