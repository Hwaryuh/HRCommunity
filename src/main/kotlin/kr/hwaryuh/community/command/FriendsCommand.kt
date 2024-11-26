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
        if (sender !is Player) return true

        try {
            when {
                args.isEmpty() -> friendsManager.showFriendList(sender)
                args[0] in listOf("추가", "요청", "신청", "cnrk") -> handleAddFriend(sender, args)
                args[0] == "수락" -> handleAcceptFriend(sender, args)
                args[0] == "거절" -> handleRejectFriend(sender, args)
                args[0] == "삭제" -> handleDeleteFriend(sender, args)
                args[0] == "목록" -> friendsManager.showFriendList(sender)
                args[0] == "대기" -> friendsManager.showFriendRequestList(sender)
                else -> sender.sendMessage(Component.text("알 수 없는 명령어입니다.").color(NamedTextColor.RED))
            }
        } catch (e: IllegalArgumentException) {
            sender.sendMessage(Component.text(e.message ?: "잘못된 명령어입니다.").color(NamedTextColor.RED))
        } catch (e: Exception) {
            sender.sendMessage(Component.text("예기치 않은 오류가 발생했습니다.").color(NamedTextColor.RED))
            plugin.logger.warning("친구 시스템 오류: ${e.message}")
            e.printStackTrace()
        }

        return true
    }

    private fun handleAddFriend(sender: Player, args: Array<out String>) {
        if (args.size < 2) {
            throw IllegalArgumentException("잘못된 명령어입니다. /친구 추가 <닉네임>")
        }

        val targetName = args[1]
        val targetPlayer = Bukkit.getPlayer(targetName)
            ?: throw IllegalArgumentException("${targetName}을(를) 찾을 수 없습니다.")

        if (sender.uniqueId == targetPlayer.uniqueId) {
            throw IllegalArgumentException("자신에게 친구 요청을 보낼 수 없습니다.")
        }

        friendsManager.addFriend(sender, targetPlayer)
    }

    private fun handleAcceptFriend(sender: Player, args: Array<out String>) {
        if (args.size < 2) {
            throw IllegalArgumentException("잘못된 명령어입니다. /친구 수락 <닉네임>")
        }

        val targetName = args[1]
        val targetUUID = Bukkit.getPlayerUniqueId(targetName)
            ?: throw IllegalArgumentException("${targetName}을(를) 찾을 수 없습니다.")

        friendsManager.acceptFriend(sender, targetUUID)
    }

    private fun handleRejectFriend(sender: Player, args: Array<out String>) {
        if (args.size < 2) {
            throw IllegalArgumentException("잘못된 명령어입니다. /친구 거절 <닉네임>")
        }

        val targetName = args[1]
        val targetUUID = Bukkit.getPlayerUniqueId(targetName)
            ?: throw IllegalArgumentException("${targetName}을(를) 찾을 수 없습니다.")

        friendsManager.rejectFriend(sender, targetUUID)
    }

    private fun handleDeleteFriend(sender: Player, args: Array<out String>) {
        if (args.size < 2) {
            throw IllegalArgumentException("잘못된 명령어입니다. /친구 삭제 <닉네임>")
        }

        val targetName = args[1]
        val targetUUID = Bukkit.getPlayerUniqueId(targetName)
            ?: throw IllegalArgumentException("${targetName}을(를) 찾을 수 없습니다.")

        friendsManager.deleteFriend(sender, targetUUID)
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