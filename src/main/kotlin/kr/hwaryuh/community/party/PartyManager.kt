package kr.hwaryuh.community.party

import kr.hwaryuh.community.Main
import net.Indyuce.mmocore.MMOCore
import net.Indyuce.mmocore.api.player.PlayerData
import net.Indyuce.mmocore.party.provided.MMOCorePartyModule
import net.Indyuce.mmocore.party.provided.Party
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.event.HoverEvent
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Bukkit
import org.bukkit.entity.Player

class PartyManager(plugin: Main, partyInviteManager: PartyInviteManager) {

    private val partyMenu = PartyMenu(plugin, this, partyInviteManager)

    companion object {
        fun leaveParty(playerData: PlayerData, party: Party, isQuit: Boolean = false) {
            try {
                party.removeMember(playerData)

                if (!isQuit) {
                    playerData.player.sendMessage(Component.text("파티를 떠났습니다.").color(NamedTextColor.YELLOW))
                }

                party.members.forEach { member ->
                    if (member.isOnline) {
                        member.player.sendMessage(Component.text("${playerData.player.name}이(가) 파티를 떠났습니다.").color(NamedTextColor.YELLOW))
                    }
                }
            } catch (e: Exception) {
                Bukkit.getLogger().warning("Error in leave party for ${playerData.player.name}: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    fun showPartyInfo(player: Player, playerData: PlayerData) {
        try {
            if (playerData.party == null) {
                sendConfirmationMessage(player)
            } else {
                partyMenu.openPartyMenu(player, playerData)
            }
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

    fun createParty(player: Player, playerData: PlayerData) {
        try {
            if (playerData.party != null) {
                throw IllegalStateException("이미 속한 파티가 있습니다.")
            }

            val partyModule = MMOCore.plugin.partyModule as? MMOCorePartyModule
                ?: throw IllegalStateException("파티 모듈을 찾을 수 없습니다.")

            partyModule.newRegisteredParty(playerData)
                ?: throw IllegalStateException("파티 생성에 실패했습니다.")

            player.sendMessage(Component.text("새로운 파티를 만들었습니다.").color(NamedTextColor.GREEN))
//            InventoryManager.PARTY_VIEW.newInventory(playerData).open() /* MMOCore 파티 메뉴 */
        } catch (e: Exception) {
            player.sendMessage(Component.text("파티 생성 중 오류 발생: ${e.message}").color(NamedTextColor.RED))
        }
    }

    fun leaveParty(player: Player, playerData: PlayerData) {
        try {
            val party = playerData.party as? Party
                ?: throw IllegalStateException("파티에 속해있지 않습니다.")

            leaveParty(playerData, party)
        } catch (e: Exception) {
            player.sendMessage(Component.text("파티 떠나기 중 오류 발생: ${e.message}").color(NamedTextColor.RED))
        }
    }

    fun kickFromParty(player: Player, playerData: PlayerData, args: Array<out String>) {
        try {
            if (args.size < 2) {
                throw IllegalArgumentException("잘못된 명령어입니다. /파티 추방 <닉네임>")
            }

            val party = playerData.party as? Party
                ?: throw IllegalStateException("파티에 속해있지 않습니다.")

            if (party.owner != playerData) {
                throw IllegalStateException("파티원은 사용할 수 없는 기능입니다.")
            }

            val targetPlayer = Bukkit.getPlayer(args[1])
                ?: throw IllegalArgumentException("플레이어를 찾을 수 없습니다: ${args[1]}")

            val targetPlayerData = PlayerData.get(targetPlayer)
            if (!party.hasMember(targetPlayer)) {
                throw IllegalStateException("${targetPlayer.name}은(는) 파티원이 아닙니다.")
            }

            if (targetPlayer == player) {
                throw throw IllegalStateException("자신은 추방할 수 없습니다.")
            }

            party.removeMember(targetPlayerData)
            player.sendMessage(Component.text("${targetPlayer.name}을(를) 파티에서 추방했습니다.").color(NamedTextColor.GREEN))
            targetPlayer.sendMessage(Component.text("파티에서 추방되었습니다.").color(NamedTextColor.RED))

            party.members.forEach { member ->
                if (member.isOnline && member != playerData) {
                    member.player.sendMessage(Component.text("${targetPlayer.name}이(가) 파티에서 추방되었습니다.").color(NamedTextColor.YELLOW))
                }
            }
        } catch (e: Exception) {
            player.sendMessage(Component.text("파티원 추방 중 오류 발생: ${e.message}").color(NamedTextColor.RED))
        }
    }
}