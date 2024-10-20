package kr.hwaryuh.community.command

import kr.hwaryuh.community.trade.TradeManager
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player

class TradeCommand(private val tradeManager: TradeManager) : CommandExecutor, TabCompleter {
    private val alias = listOf("교환", "거래", "ryghks", "rjfo")

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) return true

        if (label == "trade" || label in alias) {
            if (args.isEmpty()) {
                sender.sendMessage(Component.text("잘못된 명령어입니다. $label <닉네임> 또는 $label <수락/거절> <닉네임>").color(NamedTextColor.RED))
                return true
            }
        }

        when (args[0]) {
            "수락" -> {
                if (args.size < 2) {
                    sender.sendMessage(Component.text("잘못된 명령어입니다. /교환 수락 <닉네임>").color(NamedTextColor.RED))
                    return true
                }
                tradeManager.acceptTradeRequest(sender, args[1])
            }
            "거절" -> {
                if (args.size < 2) {
                    sender.sendMessage(Component.text("잘못된 명령어입니다. /교환 거절 <닉네임>").color(NamedTextColor.RED))
                    return true
                }
                tradeManager.rejectTradeRequest(sender, args[1])
            }
            else -> tradeManager.sendTradeRequest(sender, args[0])
        }
        return true
    }

    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<out String>): MutableList<String>? {
        if (sender !is Player) return null

        return when (args.size) {
            1 -> Bukkit.getOnlinePlayers().map { it.name }.filter { it.startsWith(args[0], true) }.toMutableList()
            2 -> when (args[0]) {
                "수락", "거절" -> Bukkit.getOnlinePlayers().map { it.name }.filter { it.startsWith(args[1], true) }.toMutableList()
                else -> null
            }
            else -> null
        }
    }
}