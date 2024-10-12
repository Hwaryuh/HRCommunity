package kr.hwaryuh.community.party

import kr.hwaryuh.community.Main
import net.Indyuce.mmocore.MMOCore
import net.Indyuce.mmocore.api.player.PlayerData
// import net.Indyuce.mmocore.manager.InventoryManager
import net.Indyuce.mmocore.party.provided.Party
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
import kotlin.math.abs

class PartyInviteManager(private val plugin: Main) {
    private val invitations = mutableMapOf<UUID, UUID>()
    private val pendingInvites = mutableMapOf<UUID, BukkitRunnable>()
    private val inviteExpirationTime: Long
        get() = plugin.config.getLong("party.invite-expiration", 30) * 20

    fun inviteToParty(player: Player, playerData: PlayerData, args: Array<out String>) {
        try {
            if (args.size < 2) {
                player.sendMessage(Component.text("/파티 초대 [플레이어]").color(NamedTextColor.RED))
                return
            }

            val party = playerData.party as? Party
                ?: throw IllegalStateException("먼저 파티를 생성해야 합니다.")

            if (party.owner != playerData) {
                throw IllegalStateException("리더만 초대할 수 있습니다.")
            }

            val targetPlayer = Bukkit.getPlayer(args[1])
                ?: throw IllegalArgumentException("플레이어를 찾을 수 없습니다: ${args[1]}")

            if (targetPlayer == player) {
                throw IllegalArgumentException("자신은 초대할 수 없습니다.")
            }

            if (party.members.size >= MMOCore.plugin.configManager.maxPartyPlayers) {
                throw IllegalStateException("파티가 가득 찼습니다.")
            }

            val targetPlayerData = PlayerData.get(targetPlayer)
            if (party.hasMember(targetPlayer)) {
                throw IllegalStateException("${targetPlayer.name}은(는) 이미 같은 파티에 속해 있습니다.")
            }

            val levelDifference = abs(targetPlayerData.level - party.level)
            if (levelDifference > MMOCore.plugin.configManager.maxPartyLevelDifference) {
                throw IllegalStateException("레벨 차이로 인해 초대할 수 없습니다. (차이: $levelDifference)")
            }

            sendCustomInvite(playerData, targetPlayerData)
            player.sendMessage(Component.text("${targetPlayer.name}을(를) 파티에 초대했습니다.").color(NamedTextColor.GREEN))
        } catch (e: Exception) {
            player.sendMessage(Component.text(e.message ?: "파티 초대 중 오류가 발생했습니다.").color(NamedTextColor.RED))
        }
    }

    private fun sendCustomInvite(inviter: PlayerData, target: PlayerData) {
        val inviteMessage = Component.text("${inviter.player.name}이(가) 파티에 초대했습니다. ")
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

        target.player.sendMessage(inviteMessage)
        target.player.playSound(target.player.location, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f)

        invitations[target.uniqueId] = inviter.uniqueId

        val expireTask = object : BukkitRunnable() {
            override fun run() {
                if (invitations.remove(target.uniqueId) != null) {
                    target.player.sendMessage(Component.text("파티 초대가 만료되었습니다.").color(NamedTextColor.YELLOW))
                    inviter.player.sendMessage(Component.text("${target.player.name}에게 보낸 파티 초대가 만료되었습니다.").color(NamedTextColor.YELLOW))
                }
                pendingInvites.remove(target.uniqueId)
            }
        }

        expireTask.runTaskLater(plugin, inviteExpirationTime)
        pendingInvites[target.uniqueId] = expireTask
    }

    fun acceptInvitation(player: Player) {
        try {
            val invitingPlayerId = invitations[player.uniqueId]
                ?: throw IllegalStateException("받은 파티 초대가 없습니다.")

            val invitingPlayer = Bukkit.getPlayer(invitingPlayerId)
                ?: throw IllegalStateException("초대한 플레이어를 찾을 수 없습니다.")

            val invitingPlayerData = PlayerData.get(invitingPlayer)
            val playerData = PlayerData.get(player)

            val party = invitingPlayerData.party as? Party
                ?: throw IllegalStateException("초대한 플레이어의 파티가 존재하지 않습니다.")

            if (party.members.size >= MMOCore.plugin.configManager.maxPartyPlayers) {
                throw IllegalStateException("파티가 가득 찼습니다.")
            }

            party.members.forEach { member ->
                if (member.isOnline) {
                    member.player.sendMessage(Component.text("${player.name}이(가) 파티에 참여했습니다.").color(NamedTextColor.GREEN))
                }
            }

            party.addMember(playerData)
            player.sendMessage(Component.text("파티에 참여했습니다.").color(NamedTextColor.GREEN))
            player.playSound(player.location, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f)

//            InventoryManager.PARTY_VIEW.newInventory(playerData).open()
        } catch (e: Exception) {
            player.sendMessage(Component.text(e.message ?: "파티 참여 중 오류가 발생했습니다.").color(NamedTextColor.RED))
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
            invitingPlayer?.sendMessage(Component.text("${player.name}이(가) 파티 초대를 거절했습니다.").color(NamedTextColor.YELLOW))
        } catch (e: Exception) {
            player.sendMessage(Component.text(e.message ?: "파티 초대 거절 중 오류가 발생했습니다.").color(NamedTextColor.RED))
        } finally {
            invitations.remove(player.uniqueId)
            pendingInvites[player.uniqueId]?.cancel()
            pendingInvites.remove(player.uniqueId)
        }
    }
}