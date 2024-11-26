package kr.hwaryuh.community.friends

import kr.hwaryuh.community.Main
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.event.HoverEvent
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.UUID

class FriendsManager(private val plugin: Main) {

    fun addFriend(sender: Player, target: Player) {
        if (plugin.databaseManager.areFriends(sender.uniqueId, target.uniqueId)) {
            throw IllegalStateException("이미 ${target.name}와(과) 친구입니다.")
        }

        if (plugin.databaseManager.hasFriendRequest(sender.uniqueId, target.uniqueId)) {
            throw IllegalStateException("이미 ${target.name}에게 친구 요청을 보냈습니다.")
        }

        plugin.databaseManager.addFriendRequest(sender.uniqueId, target.uniqueId)
        sender.sendMessage(Component.text("${target.name}에게 친구 요청을 보냈습니다.").color(NamedTextColor.GREEN))
        target.sendMessage(friendRequestMessage(sender.name))
    }

    fun acceptFriend(player: Player, targetUUID: UUID) {
        if (!plugin.databaseManager.hasFriendRequest(targetUUID, player.uniqueId)) {
            val targetName = Bukkit.getOfflinePlayer(targetUUID).name
            throw IllegalStateException("${targetName}에게 받은 친구 요청이 없습니다.")
        }

        val targetName = Bukkit.getOfflinePlayer(targetUUID).name
        plugin.databaseManager.removeFriendRequest(targetUUID, player.uniqueId)
        plugin.databaseManager.addFriend(player.uniqueId, targetUUID)
        plugin.databaseManager.addFriend(targetUUID, player.uniqueId)

        player.sendMessage(Component.text("${targetName}의 친구 요청을 수락했습니다.").color(NamedTextColor.GREEN))
        Bukkit.getPlayer(targetUUID)?.sendMessage(
            Component.text("${player.name}이(가) 친구 요청을 수락했습니다.").color(NamedTextColor.GREEN)
        )
    }

    fun rejectFriend(player: Player, targetUUID: UUID) {
        if (!plugin.databaseManager.hasFriendRequest(targetUUID, player.uniqueId)) {
            val targetName = Bukkit.getOfflinePlayer(targetUUID).name
            throw IllegalStateException("${targetName}에게 받은 친구 요청이 없습니다.")
        }

        val targetName = Bukkit.getOfflinePlayer(targetUUID).name
        plugin.databaseManager.removeFriendRequest(targetUUID, player.uniqueId)

        player.sendMessage(Component.text("${targetName}의 친구 요청을 거절했습니다.").color(NamedTextColor.YELLOW))
        Bukkit.getPlayer(targetUUID)?.sendMessage(
            Component.text("${player.name}이(가) 친구 요청을 거절했습니다.").color(NamedTextColor.RED)
        )
    }

    fun deleteFriend(player: Player, targetUUID: UUID) {
        if (!plugin.databaseManager.areFriends(player.uniqueId, targetUUID)) {
            val targetName = Bukkit.getOfflinePlayer(targetUUID).name
            throw IllegalStateException("${targetName}와(과) 친구가 아닙니다.")
        }

        val targetName = Bukkit.getOfflinePlayer(targetUUID).name
        plugin.databaseManager.removeFriend(player.uniqueId, targetUUID)
        plugin.databaseManager.removeFriend(targetUUID, player.uniqueId)

        player.sendMessage(Component.text("${targetName}을(를) 친구 목록에서 삭제했습니다.").color(NamedTextColor.YELLOW))
        Bukkit.getPlayer(targetUUID)?.sendMessage(
            Component.text("${player.name}이(가) 친구 목록에서 삭제했습니다.").color(NamedTextColor.YELLOW)
        )
    }

    fun showFriendList(player: Player) {
        plugin.openFriendsList(player)
    }

    fun showFriendRequestList(player: Player) {
        val requests = plugin.databaseManager.getFriendRequests(player.uniqueId)
        if (requests.isEmpty()) {
            player.sendMessage(Component.text("받은 친구 요청이 없습니다.").color(NamedTextColor.YELLOW))
            return
        }

        player.sendMessage(Component.text("받은 친구 요청 목록:").color(NamedTextColor.YELLOW))
        for (requesterUUID in requests) {
            val requesterName = Bukkit.getOfflinePlayer(requesterUUID).name ?: "알 수 없음"
            val message = friendRequestResponseMessage(requesterName)
            player.sendMessage(message)
        }
    }

    private fun friendRequestMessage(senderName: String): Component {
        return Component.text()
            .append(Component.text("${senderName}이(가) 친구 요청을 보냈습니다. "))
            .append(Component.text("[✔] ")
                .color(NamedTextColor.GREEN)
                .decorate(TextDecoration.BOLD)
                .clickEvent(ClickEvent.runCommand("/친구 수락 $senderName"))
                .hoverEvent(HoverEvent.showText(Component.text("클릭하여 수락").color(NamedTextColor.GREEN))))
            .append(Component.text("[X]")
                .color(NamedTextColor.RED)
                .decorate(TextDecoration.BOLD)
                .clickEvent(ClickEvent.runCommand("/친구 거절 $senderName"))
                .hoverEvent(HoverEvent.showText(Component.text("클릭하여 거절").color(NamedTextColor.RED))))
            .build()
    }

    private fun friendRequestResponseMessage(requesterName: String): Component {
        return Component.text()
            .append(Component.text("- $requesterName "))
            .append(Component.text("[✔] ")
                .color(NamedTextColor.GREEN)
                .decorate(TextDecoration.BOLD)
                .clickEvent(ClickEvent.runCommand("/친구 수락 $requesterName"))
                .hoverEvent(HoverEvent.showText(Component.text("클릭하여 수락").color(NamedTextColor.GREEN))))
            .append(Component.text("[X]")
                .color(NamedTextColor.RED)
                .decorate(TextDecoration.BOLD)
                .clickEvent(ClickEvent.runCommand("/친구 거절 $requesterName"))
                .hoverEvent(HoverEvent.showText(Component.text("클릭하여 거절").color(NamedTextColor.RED))))
            .build()
    }
}