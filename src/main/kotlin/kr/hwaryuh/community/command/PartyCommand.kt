package kr.hwaryuh.community.command

import kr.hwaryuh.community.Main
import kr.hwaryuh.community.party.PartyManager
import kr.hwaryuh.community.party.PartyInviteManager
import net.Indyuce.mmocore.api.player.PlayerData
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class PartyCommand(private val plugin: Main, private val partyManager: PartyManager, private val inviteManager: PartyInviteManager) : CommandExecutor {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage("I hate consoles.")
            return true
        }

        val playerData = PlayerData.get(sender)

        try {
            when {
                args.isEmpty() -> partyManager.showPartyInfo(sender, playerData)
                args[0] == "생성" -> partyManager.createParty(sender, playerData)
                args[0] in listOf("초대", "cheo") -> inviteManager.inviteToParty(sender, playerData, args)
                args[0] == "수락" -> inviteManager.acceptInvitation(sender)
                args[0] == "거절" -> inviteManager.declineInvitation(sender)
                args[0] == "추방" -> partyManager.kickFromParty(sender, playerData, args)
                args[0] in listOf("나가기", "skrkrl") -> partyManager.leaveParty(sender, playerData)
                else -> sender.sendMessage(Component.text("알 수 없는 명령어입니다. /파티 <생성 | 초대 | 수락 | 거절 | 추방 | 나가기>").color(NamedTextColor.RED))
            }
        } catch (e: Exception) {
            sender.sendMessage(Component.text("파티 관련 작업 중 오류가 발생했습니다: ${e.message}").color(NamedTextColor.RED))
            plugin.logger.warning("파티 관련 작업 중 오류 발생: ${e.message}")
            e.printStackTrace()
        }

        return true
    }
}