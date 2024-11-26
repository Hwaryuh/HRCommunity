package kr.hwaryuh.community.command

import kr.hwaryuh.community.Main
import kr.hwaryuh.community.party.PartyManager
import kr.hwaryuh.community.party.PartyInviteManager
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player

class PartyCommand(private val plugin: Main, private val partyManager: PartyManager, private val inviteManager: PartyInviteManager) : CommandExecutor, TabCompleter {
    private val subCommands = listOf("생성", "초대", "수락", "거절", "추방", "나가기")

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) return true

        try {
            when {
                args.isEmpty() -> partyManager.showPartyInfo(sender)
                args[0] in listOf("생성", "todtjd") -> partyManager.createParty(sender)
                args[0] in listOf("초대", "cheo") -> {
                    if (args.size < 2) {
                        sender.sendMessage(Component.text("잘못된 명령어입니다. /파티 초대 <플레이어>").color(NamedTextColor.RED))
                        return true
                    }
                    val targetPlayer = sender.server.getPlayer(args[1])
                    if (targetPlayer == null) {
                        sender.sendMessage(Component.text("플레이어를 찾을 수 없습니다: ${args[1]}").color(NamedTextColor.RED))
                        return true
                    }
                    inviteManager.inviteToParty(sender, targetPlayer)
                }
                args[0] == "수락" -> inviteManager.acceptInvitation(sender)
                args[0] == "거절" -> inviteManager.declineInvitation(sender)
                args[0] == "추방" -> {
                    if (args.size < 2) {
                        sender.sendMessage(Component.text("잘못된 명령어입니다. /파티 추방 <플레이어>").color(NamedTextColor.RED))
                        return true
                    }
                    val targetPlayer = sender.server.getPlayer(args[1])
                    if (targetPlayer == null) {
                        sender.sendMessage(Component.text("플레이어를 찾을 수 없습니다: ${args[1]}").color(NamedTextColor.RED))
                        return true
                    }
                    partyManager.kickFromParty(sender, targetPlayer)
                }
                args[0] in listOf("나가기", "떠나기", "skrkrl") -> partyManager.leaveParty(sender)
                else -> sender.sendMessage(
                    Component.text("잘못된 명령어입니다. /파티 <생성|초대|수락|거절|추방|나가기>")
                        .color(NamedTextColor.RED)
                )
            }
        } catch (e: Exception) {
            sender.sendMessage(
                Component.text("예기치 않은 오류가 발생했습니다.").color(NamedTextColor.RED)
            )
            plugin.logger.warning("파티 시스템 오류: ${e.message}")
            e.printStackTrace()
        }

        return true
    }

    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<out String>): List<String>? {
        if (sender !is Player) return null

        return when (args.size) {
            1 -> subCommands.filter { it.startsWith(args[0], ignoreCase = true) }
            2 -> when (args[0]) {
                "초대" -> getOnlinePlayers(sender).filter { it.startsWith(args[1], ignoreCase = true) }
                "추방" -> getPartyMembers(sender).filter { it.startsWith(args[1], ignoreCase = true) }
                else -> null
            }
            else -> null
        }
    }

    private fun getOnlinePlayers(player: Player): List<String> {
        val currentParty = partyManager.getPlayerParty(player)

        return player.server.onlinePlayers
            .filter { it != player && (currentParty == null || !currentParty.hasPlayer(it)) }
            .map { it.name }
    }

    private fun getPartyMembers(player: Player): List<String> {
        val party = partyManager.getPlayerParty(player) ?: return emptyList()

        return party.players
            .filter { !party.isLeader(it) }
            .map { it.name }
    }
}