package kr.hwaryuh.community.command

import net.Indyuce.mmocore.api.player.PlayerData
import net.Indyuce.mmocore.party.provided.Party
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player

class PartyTabCompleter : TabCompleter {
    private val subCommands = listOf("생성", "초대", "수락", "거절", "추방", "나가기")

    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<out String>): List<String>? {
        if (sender !is Player) return null

        return when (args.size) {
            1 -> subCommands.filter { it.startsWith(args[0], ignoreCase = true) }
            2 -> when (args[0].lowercase()) {
                "초대" -> getOnlinePlayers(sender).filter { it.startsWith(args[1], ignoreCase = true) }
                "추방" -> getPartyMembers(sender).filter { it.startsWith(args[1], ignoreCase = true) }
                else -> null
            }
            else -> null
        }
    }

    private fun getOnlinePlayers(player: Player): List<String> {
        val playerData = PlayerData.get(player)
        val currentParty = playerData.party as? Party

        return player.server.onlinePlayers
            .filter { it != player && (currentParty == null || !currentParty.hasMember(it)) }
            .map { it.name }
    }

    private fun getPartyMembers(player: Player): List<String> {
        val playerData = PlayerData.get(player)
        val party = playerData.party as? Party ?: return emptyList()

        return party.members
            .filter { it != party.owner }
            .map { it.player.name }
    }
}