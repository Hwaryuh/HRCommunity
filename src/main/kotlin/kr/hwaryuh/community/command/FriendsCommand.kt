package kr.hwaryuh.community.command

import kr.hwaryuh.community.Main
import kr.hwaryuh.community.friends.FriendsManager
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player

class FriendsCommand(private val plugin: Main, private val friendsManager: FriendsManager) : CommandExecutor, TabCompleter {
    private val subCommands = listOf("추가", "수락", "거절", "삭제", "목록", "대기")

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) { return true }

        if (args.isEmpty()) {
            friendsManager.showFriendList(sender)
            return true
        }

        try {
            when (args[0]) {
                "추가", "요청", "신청", "cnrk" -> friendsManager.addFriend(sender, args)
                "수락" -> friendsManager.acceptFriend(sender, args)
                "거절" -> friendsManager.rejectFriend(sender, args)
                "삭제" -> friendsManager.deleteFriend(sender, args)
                "목록" -> friendsManager.showFriendList(sender)
                "대기" -> friendsManager.showFriendRequestList(sender)
                else -> sender.sendMessage(Component.text("알 수 없는 명령어입니다.").color(NamedTextColor.RED))
            }
        } catch (e: Exception) {
            sender.sendMessage(Component.text("친구 관련 작업 중 오류 발생: ${e.message}").color(NamedTextColor.RED))
            plugin.logger.warning("친구 관련 작업 중 오류 발생: ${e.message}")
            e.printStackTrace()
        }

        return true
    }

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