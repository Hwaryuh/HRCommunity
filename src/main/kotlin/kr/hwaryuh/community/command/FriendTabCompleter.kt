package kr.hwaryuh.community.command

import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player

class FriendTabCompleter : TabCompleter {
    private val subCommands = listOf("추가", "수락", "거절", "삭제", "목록", "대기")

    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<out String>): List<String>? {
        if (sender !is Player) return null

        return when (args.size) {
            1 -> subCommands.filter { it.startsWith(args[0], ignoreCase = true) }
            2 -> when (args[0]) {
                "추가", "cnrk", "요청", "신청", "수락", "거절", "삭제" -> {
                    Bukkit.getOnlinePlayers()
                        .filter { it.name != sender.name }
                        .map { it.name }
                        .filter { it.startsWith(args[1], ignoreCase = true) }
                }
                else -> null
            }
            else -> null
        }
    }
}