package kr.hwaryuh.community.command

import kr.hwaryuh.community.Main
import kr.hwaryuh.community.service.FriendsList
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.event.HoverEvent
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class FriendCommand(private val plugin: Main) : CommandExecutor {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage("I hate consoles")
            return true
        }

        if (args.isEmpty()) {
            sender.sendMessage("§6/친구 <목록|추가|삭제|수락|거절> [플레이어]")
            return true
        }

        when (args[0].toLowerCase()) {
            "추가" -> handleAddFriend(sender, args)
            "수락" -> handleAcceptFriend(sender)
            "거절" -> handleRejectFriend(sender)
            "삭제" -> handleDeleteFriend(sender, args)
            "목록" -> handleFriendList(sender)
            else -> sender.sendMessage("§c알 수 없는 명령어입니다. §6/친구 <목록|추가|삭제|수락|거절> [플레이어]")
        }
        return true
    }

    private fun handleAddFriend(sender: Player, args: Array<out String>) {
        if (args.size < 2) {
            sender.sendMessage("§6/친구 추가 [플레이어]")
            return
        }

        val targetName = args[1]
        val targetPlayer = Bukkit.getPlayer(targetName)

        if (targetPlayer == null) {
            sender.sendMessage("§c${targetName}을(를) 찾을 수 없습니다.")
            return
        }

        if (plugin.databaseManager.hasFriendRequest(sender.uniqueId, targetPlayer.uniqueId)) {
            sender.sendMessage("§6이미 ${targetPlayer.name}에게 친구 요청을 보냈습니다.")
            return
        }

        plugin.databaseManager.addFriendRequest(sender.uniqueId, targetPlayer.uniqueId)
        sender.sendMessage("§a${targetPlayer.name}에게 친구 요청을 보냈습니다.")

        val message = Component.text()
            .append(Component.text("${sender.name}이(가) 친구 요청을 보냈습니다. "))
            .append(Component.text("[수락] ")
                .color(NamedTextColor.GREEN)
                .clickEvent(ClickEvent.runCommand("/친구 수락"))
                .hoverEvent(HoverEvent.showText(Component.text("클릭하여 수락"))))
            .append(Component.text("[거절]")
                .color(NamedTextColor.RED)
                .clickEvent(ClickEvent.runCommand("/친구 거절"))
                .hoverEvent(HoverEvent.showText(Component.text("클릭하여 거절"))))
            .build()

        targetPlayer.sendMessage(message)
    }

    private fun handleAcceptFriend(player: Player) {
        val senderUUID = plugin.databaseManager.getFriendRequests(player.uniqueId).firstOrNull()
        if (senderUUID == null) {
            player.sendMessage("§6수락할 친구 요청이 없습니다.")
            return
        }

        val sender = Bukkit.getPlayer(senderUUID)
        plugin.databaseManager.removeFriendRequest(senderUUID, player.uniqueId)
        plugin.databaseManager.addFriend(player.uniqueId, senderUUID)
        plugin.databaseManager.addFriend(senderUUID, player.uniqueId)

        player.sendMessage("§a친구 요청을 수락했습니다.")
        sender?.sendMessage("§a${player.name}이(가) 친구 요청을 수락했습니다.")
    }

    private fun handleRejectFriend(player: Player) {
        val senderUUID = plugin.databaseManager.getFriendRequests(player.uniqueId).firstOrNull()
        if (senderUUID == null) {
            player.sendMessage("§6거절할 친구 요청이 없습니다.")
            return
        }

        val sender = Bukkit.getPlayer(senderUUID)
        plugin.databaseManager.removeFriendRequest(senderUUID, player.uniqueId)

        player.sendMessage("§c친구 요청을 거절했습니다.")
        sender?.sendMessage("§c${player.name}이(가) 친구 요청을 거절했습니다.")
    }private fun handleDeleteFriend(player: Player, args: Array<out String>) {
        if (args.size < 2) {
            player.sendMessage("§6/친구 삭제 [플레이어]")
            return
        }

        val targetName = args[1]
        val targetUUID = Bukkit.getPlayerUniqueId(targetName)

        if (targetUUID == null) {
            player.sendMessage("§c${targetName}을(를) 찾을 수 없습니다.")
            return
        }

        if (!plugin.databaseManager.getFriends(player.uniqueId).contains(targetUUID)) {
            player.sendMessage("§c${targetName}은(는) 친구가 아닙니다.")
            return
        }

        plugin.databaseManager.removeFriend(player.uniqueId, targetUUID)
        plugin.databaseManager.removeFriend(targetUUID, player.uniqueId)

        player.sendMessage("§6${targetName}을(를) 친구 목록에서 삭제했습니다.")

//        val targetPlayer = Bukkit.getPlayer(targetUUID)
//        targetPlayer?.sendMessage("${player.name}이(가) 당신을 친구 목록에서 삭제했습니다.")
    }

    private fun handleFriendList(player: Player) {
        FriendsList(plugin, player).open()
    }
}