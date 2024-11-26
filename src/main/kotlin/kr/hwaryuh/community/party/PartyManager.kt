package kr.hwaryuh.community.party

import kr.hwaryuh.community.Main
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.event.HoverEvent
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Sound
import org.bukkit.entity.Player

class PartyManager(private val plugin: Main) {
    private lateinit var partyInviteManager: PartyInviteManager
    private lateinit var partyMenu: PartyMenu
    private val parties = mutableMapOf<Player, MythicPartyBridge>()

    fun setPartyInviteManager(manager: PartyInviteManager) {
        this.partyInviteManager = manager
        this.partyMenu = PartyMenu(plugin, this, partyInviteManager)
    }

    fun showPartyInfo(player: Player) {
        try {
            val party = parties[player]
            if (party == null) sendConfirmationMessage(player) else partyMenu.openPartyMenu(player)
        } catch (e: Exception) {
            player.sendMessage(Component.text("파티 정보 표시 중 오류 발생: ${e.message}").color(NamedTextColor.RED))
        }
    }

    private fun sendConfirmationMessage(player: Player) {
        val message = Component.text("파티가 없습니다. 생성할까요? ")
            .color(NamedTextColor.YELLOW)
            .append(
                Component.text("[✔]")
                    .color(NamedTextColor.GREEN).decorate(TextDecoration.BOLD)
                    .clickEvent(ClickEvent.runCommand("/파티 생성"))
                    .hoverEvent(HoverEvent.showText(Component.text("클릭하여 파티 생성").color(NamedTextColor.GREEN)))
            )

        player.sendMessage(message)
    }

    fun createParty(player: Player) {
        try {
            if (parties[player] != null) {
                throw IllegalStateException("이미 속한 파티가 있습니다.")
            }

            val party = MythicPartyBridge(player, plugin)
            parties[player] = party
            player.sendMessage(Component.text("새로운 파티를 만들었습니다.").color(NamedTextColor.GREEN))
        } catch (e: Exception) {
            player.sendMessage(Component.text("파티 생성 중 오류 발생: ${e.message}").color(NamedTextColor.RED))
        }
    }

    private fun handlePartyJoin(player: Player, party: MythicPartyBridge) {
        party.addPlayer(player)
        // 파티 맵에 새로운 멤버도 추가
        parties[player] = party
    }

    fun acceptInvitation(player: Player, party: MythicPartyBridge) {
        try {
            if (party.getMemberCount() >= 8) {
                throw IllegalStateException("파티가 가득 찼습니다.")
            }

            handlePartyJoin(player, party)
            player.sendMessage(Component.text("파티에 합류했습니다.").color(NamedTextColor.GREEN))
            player.playSound(player.location, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f)
        } catch (e: Exception) {
            player.sendMessage(Component.text("파티 참여 중 오류가 발생했습니다: ${e.message}").color(NamedTextColor.RED))
        }
    }

    private fun handlePartyLeave(player: Player, party: MythicPartyBridge, isQuit: Boolean = false) {
        try {
            if (!isQuit) {
                player.sendMessage(Component.text("파티를 떠났습니다.").color(NamedTextColor.YELLOW))
            }

            party.removePlayer(player)

            if (party.getMemberCount() == 0) {
                party.disband()
            }

            parties.entries.removeIf { it.value == party && it.key == player }
        } catch (e: Exception) {
            plugin.logger.warning("Error in leave party for ${player.name}: ${e.message}")
            e.printStackTrace()
        }
    }

    fun leaveParty(player: Player) {
        try {
            val party = getPlayerParty(player) ?: throw IllegalStateException("파티에 속해있지 않습니다.")
            handlePartyLeave(player, party)
        } catch (e: Exception) {
            player.sendMessage(Component.text("파티 떠나기 중 오류 발생: ${e.message}").color(NamedTextColor.RED))
        }
    }

    fun handleServerQuit(player: Player) {
        val party = getPlayerParty(player) ?: return
        handlePartyLeave(player, party, true)
    }

    fun handleLeaveOnMenu(player: Player) {
        val party = getPlayerParty(player) ?: return
        handlePartyLeave(player, party)
    }

    fun kickFromParty(leader: Player, target: Player) {
        try {
            val party = getPlayerParty(leader)
                ?: throw IllegalStateException("파티에 속해있지 않습니다.")

            if (!party.isLeader(leader)) {
                throw IllegalStateException("파티원은 사용할 수 없는 기능입니다.")
            }

            if (!party.hasPlayer(target)) {
                throw IllegalStateException("${target.name}은(는) 파티원이 아닙니다.")
            }

            if (target == leader) {
                throw IllegalStateException("자신은 추방할 수 없습니다.")
            }

            handlePartyLeave(target, party)
            leader.sendMessage(Component.text("${target.name}을(를) 파티에서 추방했습니다.").color(NamedTextColor.GREEN))
            target.sendMessage(Component.text("파티에서 추방되었습니다.").color(NamedTextColor.RED))
        } catch (e: Exception) {
            leader.sendMessage(Component.text("파티원 추방 중 오류 발생: ${e.message}").color(NamedTextColor.RED))
        }
    }

    fun getPlayerParty(player: Player): MythicPartyBridge? {
        return parties.values.find { it.hasPlayer(player) }
    }
}