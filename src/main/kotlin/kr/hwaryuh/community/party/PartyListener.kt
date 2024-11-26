package kr.hwaryuh.community.party

import kr.hwaryuh.community.Main
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent

class PartyListener(private val plugin: Main, private val partyManager: PartyManager) : Listener {

    @EventHandler(priority = EventPriority.LOWEST)
    fun onPlayerQuit(event: PlayerQuitEvent) {
        val player = event.player
        val party = partyManager.getPlayerParty(player)

        if (party != null) {
            try {
                partyManager.handleServerQuit(player)
            } catch (e: Exception) {
                plugin.logger.warning("Error while removing ${player.name} from party on quit: ${e.message}")
                e.printStackTrace()
            }
        }
    }
}