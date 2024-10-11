package kr.hwaryuh.community.command

import kr.hwaryuh.community.Main
import kr.hwaryuh.community.profile.PreviousMenuType
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class ProfileCommand(private val plugin: Main) : CommandExecutor {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage("I hate consoles.")
            return true
        }

        if (args.isEmpty()) {
            plugin.openProfileMenu(sender, sender, false, PreviousMenuType.NONE)
            return true
        }

        val targetName = args[0]
        val targetPlayer = Bukkit.getPlayer(targetName)

        if (targetPlayer == null) {
            sender.sendMessage("§c${targetName}을(를) 찾을 수 없습니다.")
            return true
        }

        plugin.openProfileMenu(sender, targetPlayer, false, PreviousMenuType.NONE)
        return true
    }
}