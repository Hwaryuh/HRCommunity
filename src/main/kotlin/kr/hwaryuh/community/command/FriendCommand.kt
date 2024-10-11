package kr.hwaryuh.community.command

import kr.hwaryuh.community.Main
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.event.HoverEvent
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class FriendCommand(private val plugin: Main) : CommandExecutor {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage("I hate consoles.")
            return true
        }

        if (args.isEmpty()) {
            handleFriendList(sender)
            return true
        }

        when (args[0]) {
            "추가", "요청", "신청", "cnrk" -> handleAddFriend(sender, args)
            "수락" -> handleAcceptFriend(sender, args)
            "거절" -> handleRejectFriend(sender, args)
            "삭제" -> handleDeleteFriend(sender, args)
            "목록" -> handleFriendList(sender)
            "대기" -> handleFriendRequestList(sender)
            else -> sender.sendMessage(Component.text("알 수 없는 명령어입니다.").color(NamedTextColor.RED))
        }
        return true
    }

    private fun handleAddFriend(sender: Player, args: Array<out String>) {
        if (args.size < 2) {
            sender.sendMessage(Component.text("/친구 추가 [플레이어]").color(NamedTextColor.RED))
            return
        }

        val targetName = args[1]
        val targetPlayer = Bukkit.getPlayer(targetName)

        if (targetPlayer == null) {
            sender.sendMessage(Component.text("${targetName}을(를) 찾을 수 없습니다.").color(NamedTextColor.RED))
            return
        }

        if (plugin.databaseManager.hasFriendRequest(sender.uniqueId, targetPlayer.uniqueId)) {
            sender.sendMessage(Component.text("이미 ${targetPlayer.name}에게 친구 요청을 보냈습니다.").color(NamedTextColor.YELLOW))
            return
        }

        plugin.databaseManager.addFriendRequest(sender.uniqueId, targetPlayer.uniqueId)
        sender.sendMessage(Component.text("${targetPlayer.name}에게 친구 요청을 보냈습니다.").color(NamedTextColor.YELLOW))

        val message = Component.text()
            .append(Component.text("${sender.name}이(가) 친구 요청을 보냈습니다. "))
            .append(Component.text("[✔] ")
                .color(NamedTextColor.GREEN).decorate(TextDecoration.BOLD)
                .clickEvent(ClickEvent.runCommand("/친구 수락 ${sender.name}"))
                .hoverEvent(HoverEvent.showText(Component.text("클릭하여 수락").color(NamedTextColor.GREEN))))
            .append(Component.text("[X]")
                .color(NamedTextColor.RED).decorate(TextDecoration.BOLD)
                .clickEvent(ClickEvent.runCommand("/친구 거절 ${sender.name}"))
                .hoverEvent(HoverEvent.showText(Component.text("클릭하여 거절").color(NamedTextColor.RED))))
            .build()

        targetPlayer.sendMessage(message)
    }

    private fun handleAcceptFriend(player: Player, args: Array<out String>) {
        if (args.size < 2) {
            player.sendMessage(Component.text("/친구 수락 [플레이어]").color(NamedTextColor.RED))
            return
        }

        val targetName = args[1]
        val targetUUID = Bukkit.getPlayerUniqueId(targetName)

        if (targetUUID == null) {
            player.sendMessage(Component.text("${targetName}을(를) 찾을 수 없습니다.").color(NamedTextColor.RED))
            return
        }

        if (!plugin.databaseManager.hasFriendRequest(targetUUID, player.uniqueId)) {
            player.sendMessage(Component.text("${targetName}에게서 온 친구 요청이 없습니다.").color(NamedTextColor.RED))
            return
        }

        plugin.databaseManager.removeFriendRequest(targetUUID, player.uniqueId)
        plugin.databaseManager.addFriend(player.uniqueId, targetUUID)
        plugin.databaseManager.addFriend(targetUUID, player.uniqueId)

        player.sendMessage(Component.text("${targetName}의 친구 요청을 수락했습니다.").color(NamedTextColor.GREEN))
        Bukkit.getPlayer(targetUUID)?.sendMessage(Component.text("${player.name}이(가) 친구 요청을 수락했습니다.").color(NamedTextColor.GREEN))
    }

    private fun handleRejectFriend(player: Player, args: Array<out String>) {
        if (args.size < 2) {
            player.sendMessage(Component.text("/친구 거절 [플레이어]").color(NamedTextColor.YELLOW))
            return
        }

        val targetName = args[1]
        val targetUUID = Bukkit.getPlayerUniqueId(targetName)

        if (targetUUID == null) {
            player.sendMessage(Component.text("${targetName}을(를) 찾을 수 없습니다.").color(NamedTextColor.RED))
            return
        }

        if (!plugin.databaseManager.hasFriendRequest(player.uniqueId, targetUUID)) {
            player.sendMessage(Component.text("${targetName}에게서 온 친구 요청이 없습니다.").color(NamedTextColor.RED))
            return
        }

        plugin.databaseManager.removeFriendRequest(player.uniqueId, targetUUID)

        player.sendMessage(Component.text("${targetName}의 친구 요청을 거절했습니다.").color(NamedTextColor.RED))
        Bukkit.getPlayer(targetUUID)?.sendMessage(Component.text("${player.name}이(가) 친구 요청을 거절했습니다.").color(NamedTextColor.RED))
    }

    private fun handleDeleteFriend(player: Player, args: Array<out String>) {
        if (args.size < 2) {
            player.sendMessage(Component.text("/친구 삭제 [플레이어]").color(NamedTextColor.YELLOW))
            return
        }

        val targetName = args[1]
        val targetUUID = Bukkit.getPlayerUniqueId(targetName)

        if (targetUUID == null) {
            player.sendMessage(Component.text("${targetName}을(를) 찾을 수 없습니다.").color(NamedTextColor.RED))
            return
        }

        if (!plugin.databaseManager.getFriends(player.uniqueId).contains(targetUUID)) {
            player.sendMessage(Component.text("${targetName}은(는) 친구가 아닙니다.").color(NamedTextColor.RED))
            return
        }

        plugin.databaseManager.removeFriend(player.uniqueId, targetUUID)
        plugin.databaseManager.removeFriend(targetUUID, player.uniqueId)

        player.sendMessage(Component.text("${targetName}을(를) 친구 목록에서 삭제했습니다.").color(NamedTextColor.YELLOW))
    }

    private fun handleFriendList(player: Player) {
        plugin.openFriendsList(player)
    }

    private fun handleFriendRequestList(player: Player) {
        val requests = plugin.databaseManager.getFriendRequests(player.uniqueId)
        if (requests.isEmpty()) {
            player.sendMessage(Component.text("받은 친구 요청이 없습니다.").color(NamedTextColor.YELLOW))
            return
        }

        player.sendMessage(Component.text("받은 친구 요청 목록:").color(NamedTextColor.YELLOW))
        for (requesterUUID in requests) {
            val requesterName = Bukkit.getOfflinePlayer(requesterUUID).name ?: "알 수 없음"
            val message = Component.text()
                .append(Component.text("- $requesterName "))
                    .append(Component.text("[✔] ")
                    .color(NamedTextColor.GREEN).decorate(TextDecoration.BOLD)
                    .clickEvent(ClickEvent.runCommand("/친구 수락 $requesterName"))
                    .hoverEvent(HoverEvent.showText(Component.text("클릭하여 수락").color(NamedTextColor.GREEN))))
                .append(Component.text("[X]")
                    .color(NamedTextColor.RED).decorate(TextDecoration.BOLD)
                    .clickEvent(ClickEvent.runCommand("/친구 거절 $requesterName"))
                    .hoverEvent(HoverEvent.showText(Component.text("클릭하여 거절").color(NamedTextColor.RED))))
                .build()
            player.sendMessage(message)
        }
    }
}
