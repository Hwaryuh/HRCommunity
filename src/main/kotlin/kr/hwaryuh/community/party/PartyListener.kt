package kr.hwaryuh.community.party

import kr.hwaryuh.community.Main
import net.Indyuce.mmocore.api.player.PlayerData
import net.Indyuce.mmocore.party.provided.Party
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent

class PartyListener(private val plugin: Main) : Listener {

    @EventHandler(priority = EventPriority.LOWEST)
    fun onPlayerQuit(event: PlayerQuitEvent) {
        val player = event.player
        val playerData = PlayerData.get(player)

        if (playerData == null) {
            plugin.logger.warning("PlayerData is null for ${player.name} on quit. Skipping party cleanup.")
            return
        }

        val party = playerData.party as? Party
        if (party != null) {
            try {
                PartyManager.leaveParty(playerData, party, true)
            } catch (e: Exception) {
                plugin.logger.warning("Error while removing ${player.name} from party on quit: ${e.message}")
                e.printStackTrace()
            }
        }
    }
}