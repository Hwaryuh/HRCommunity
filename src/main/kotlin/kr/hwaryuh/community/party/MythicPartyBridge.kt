package kr.hwaryuh.community.party

import net.playavalon.mythicdungeons.api.party.IDungeonParty
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.playavalon.mythicdungeons.MythicDungeons
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import java.util.UUID

class MythicPartyBridge(leader: Player, private val plugin: Plugin) : IDungeonParty {
    private var leader: UUID = leader.uniqueId
    private val playerUUIDs = mutableListOf<UUID>()
    private val onlinePlayerUUIDs = mutableListOf<UUID>()

    init {
        playerUUIDs.add(leader.uniqueId)
        onlinePlayerUUIDs.add(leader.uniqueId)
        initDungeonParty(plugin)
    }

    override fun addPlayer(player: Player) {
        if (!playerUUIDs.contains(player.uniqueId)) {
            playerUUIDs.add(player.uniqueId)
            onlinePlayerUUIDs.add(player.uniqueId)

            val mythicPlayer = MythicDungeons.inst().getMythicPlayer(player)
            mythicPlayer.dungeonParty = this

            broadcastPartyMessage(Component.text("${player.name}이(가) 파티에 참가했습니다.").color(NamedTextColor.GREEN))
        }
    }

    override fun removePlayer(player: Player) {
        if (playerUUIDs.remove(player.uniqueId)) {
            onlinePlayerUUIDs.remove(player.uniqueId)

            val mythicPlayer = MythicDungeons.inst().getMythicPlayer(player)
            mythicPlayer.dungeonParty = null

            broadcastPartyMessage(Component.text("${player.name}이(가) 파티를 떠났습니다.").color(NamedTextColor.YELLOW))

            if (player.uniqueId == leader) {
                assignNewLeader()
            }
        }
    }

    override fun getPlayers(): List<Player> {
        return playerUUIDs.mapNotNull { uuid ->
            Bukkit.getPlayer(uuid)
        }
    }

    override fun getLeader(): OfflinePlayer {
        return Bukkit.getOfflinePlayer(leader)
    }

    override fun hasPlayer(player: Player): Boolean {
        return playerUUIDs.contains(player.uniqueId)
    }

    fun isLeader(player: Player): Boolean {
        return player.uniqueId == leader
    }

    fun getMemberCount(): Int {
        return playerUUIDs.size
    }

    fun getOnlineMemberCount(): Int {
        return onlinePlayerUUIDs.size
    }

    fun setPlayerOnline(player: Player, online: Boolean) {
        if (online) {
            if (!onlinePlayerUUIDs.contains(player.uniqueId)) {
                onlinePlayerUUIDs.add(player.uniqueId)
                broadcastPartyMessage(Component.text("${player.name}이(가) 접속했습니다.").color(NamedTextColor.GREEN))
            }
        } else {
            if (onlinePlayerUUIDs.remove(player.uniqueId)) {
                broadcastPartyMessage(Component.text("${player.name}이(가) 오프라인이 되었습니다.").color(NamedTextColor.YELLOW))
                if (player.uniqueId == leader && onlinePlayerUUIDs.isNotEmpty()) {
                    assignNewLeader()
                }
            }
        }
    }

    fun broadcastPartyMessage(message: Component) {
        players.forEach { player ->
            player.sendMessage(message)
        }
    }

    fun sendChatMessage(sender: Player, message: String) {
        val formatted = Component.text("[파티] ")
            .color(NamedTextColor.AQUA)
            .append(Component.text(sender.name)
                .color(NamedTextColor.YELLOW))
            .append(Component.text(": ")
                .color(NamedTextColor.WHITE))
            .append(Component.text(message)
                .color(NamedTextColor.WHITE))

        broadcastPartyMessage(formatted)
    }

    private fun assignNewLeader() {
        // 온라인 멤버 중에서 새로운 리더 선택
        val newLeaderUUID = onlinePlayerUUIDs.firstOrNull() ?: return

        if (newLeaderUUID != leader) {
            leader = newLeaderUUID

            val newLeader = Bukkit.getPlayer(newLeaderUUID)
            if (newLeader != null) {
                broadcastPartyMessage(
                    Component.text("${newLeader.name}이(가) 새로운 리더입니다.")
                        .color(NamedTextColor.GREEN)
                )

                val mythicPlayer = MythicDungeons.inst().getMythicPlayer(newLeader)
                mythicPlayer.dungeonParty = this
            }
        }
    }

    fun disband() {
        val disbandMessage = Component.text("파티가 해산되었습니다.").color(NamedTextColor.RED)
        broadcastPartyMessage(disbandMessage)

        val playersToRemove = players.toList() // 복사본 생성
        playersToRemove.forEach { player ->
            removePlayer(player)
        }

        playerUUIDs.clear()
        onlinePlayerUUIDs.clear()
    }
}