package kr.hwaryuh.community.party

import kr.hwaryuh.community.Main
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.event.HoverEvent
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Bukkit
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitRunnable
import java.util.UUID

class PartyInviteManager(private val plugin: Main, private val partyManager: PartyManager) {
    private val invitations = mutableMapOf<UUID, UUID>()
    private val pendingInvites = mutableMapOf<UUID, BukkitRunnable>()

    fun inviteToParty(player: Player, targetPlayer: Player) {
        try {
            val party = partyManager.getPlayerParty(player)
                ?: throw IllegalStateException("먼저 파티를 생성해야 합니다.")

            if (!party.isLeader(player)) {
                throw IllegalStateException("리더만 초대할 수 있습니다.")
            }

            if (targetPlayer == player) {
                throw IllegalArgumentException("자신은 초대할 수 없습니다.")
            }

            if (party.getMemberCount() >= 8) {
                throw IllegalStateException("파티가 가득 찼습니다.")
            }

            if (party.hasPlayer(targetPlayer)) {
                throw IllegalStateException("${targetPlayer.name}은(는) 이미 같은 파티에 속해 있습니다.")
            }

            if (partyManager.getPlayerParty(targetPlayer) != null) {
                throw IllegalStateException("${targetPlayer.name}은(는) 이미 다른 파티에 속해 있습니다.")
            }

            sendCustomInvite(player, targetPlayer)
            player.sendMessage(Component.text("${targetPlayer.name}을(를) 파티에 초대했습니다.").color(NamedTextColor.GREEN))
        } catch (e: Exception) {
            player.sendMessage(Component.text(e.message ?: "알 수 없는 오류가 발생했습니다.").color(NamedTextColor.RED))
        }
    }

    private fun sendCustomInvite(inviter: Player, target: Player) {
        val inviteMessage = Component.text("${inviter.name}이(가) 파티에 초대했습니다. ")
            .color(NamedTextColor.YELLOW)
            .append(
                Component.text("[✔]")
                    .color(NamedTextColor.GREEN).decorate(TextDecoration.BOLD)
                    .clickEvent(ClickEvent.runCommand("/파티 수락"))
                    .hoverEvent(HoverEvent.showText(Component.text("클릭하여 수락").color(NamedTextColor.GREEN)))
            )
            .append(Component.text(" "))
            .append(
                Component.text("[X]")
                    .color(NamedTextColor.RED).decorate(TextDecoration.BOLD)
                    .clickEvent(ClickEvent.runCommand("/파티 거절"))
                    .hoverEvent(HoverEvent.showText(Component.text("클릭하여 거절").color(NamedTextColor.RED)))
            )

        target.sendMessage(inviteMessage)
        target.playSound(target.location, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f)

        invitations[target.uniqueId] = inviter.uniqueId

        val expireTask = object : BukkitRunnable() {
            override fun run() {
                if (invitations.remove(target.uniqueId) != null) {
                    target.sendMessage(Component.text("파티 초대가 만료되었습니다.").color(NamedTextColor.YELLOW))
                    inviter.sendMessage(
                        Component.text("${target.name}에게 보낸 파티 초대가 만료되었습니다.")
                            .color(NamedTextColor.YELLOW)
                    )
                }
                pendingInvites.remove(target.uniqueId)
            }
        }

        expireTask.runTaskLater(plugin, getInviteExpirationTime())
        pendingInvites[target.uniqueId] = expireTask
    }

    fun acceptInvitation(player: Player) {
        try {
            val invitingPlayerId = invitations[player.uniqueId]
                ?: throw IllegalStateException("받은 파티 초대가 없습니다.")

            val invitingPlayer = Bukkit.getPlayer(invitingPlayerId)
                ?: throw IllegalStateException("초대한 플레이어를 찾을 수 없습니다.")

            val party = partyManager.getPlayerParty(invitingPlayer)
                ?: throw IllegalStateException("초대한 플레이어의 파티가 존재하지 않습니다.")

            partyManager.acceptInvitation(player, party)
        } catch (e: Exception) {
            player.sendMessage(Component.text(e.message ?: "알 수 없는 오류가 발생했습니다.").color(NamedTextColor.RED))
        } finally {
            invitations.remove(player.uniqueId)
            pendingInvites[player.uniqueId]?.cancel()
            pendingInvites.remove(player.uniqueId)
        }
    }

    fun declineInvitation(player: Player) {
        try {
            val invitingPlayerId = invitations[player.uniqueId]
                ?: throw IllegalStateException("받은 파티 초대가 없습니다.")

            val invitingPlayer = Bukkit.getPlayer(invitingPlayerId)
            player.sendMessage(Component.text("파티 초대를 거절했습니다.").color(NamedTextColor.YELLOW))
            invitingPlayer?.sendMessage(
                Component.text("${player.name}이(가) 파티 초대를 거절했습니다.")
                    .color(NamedTextColor.YELLOW)
            )
        } catch (e: Exception) {
            player.sendMessage(Component.text(e.message ?: "알 수 없는 오류가 발생했습니다.").color(NamedTextColor.RED))
        } finally {
            invitations.remove(player.uniqueId)
            pendingInvites[player.uniqueId]?.cancel()
            pendingInvites.remove(player.uniqueId)
        }
    }

    private fun getInviteExpirationTime(): Long {
        return plugin.config.getLong("party.invite-expiration", 30) * 20
    }
}