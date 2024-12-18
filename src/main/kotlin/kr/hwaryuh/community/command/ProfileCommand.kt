package kr.hwaryuh.community.command

import kr.hwaryuh.community.Main
import kr.hwaryuh.community.profile.PreviousMenuType
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player

class ProfileCommand(private val plugin: Main) : CommandExecutor, TabCompleter {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) return true

        if (args.isEmpty()) {
            plugin.openProfileMenu(sender, sender, false, PreviousMenuType.NONE)
            return true
        }

        val target = args[0]
        val targetPlayer = Bukkit.getPlayer(target)

        if (targetPlayer == null) {
            sender.sendMessage(Component.text("${target}을(를) 찾을 수 없습니다").color(NamedTextColor.RED))
            return true
        }

        plugin.openProfileMenu(sender, targetPlayer, false, PreviousMenuType.NONE)
        return true
    }

    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<out String>): List<String>? {
        if (sender !is Player) return null

        return when {
            args.size == 1 -> getOnlinePlayers(sender).filter { it.startsWith(args[0], ignoreCase = true) }
            else -> null
        }
    }

    private fun getOnlinePlayers(player: Player): List<String> {
        return player.world.players.map { it.name }
    }
}