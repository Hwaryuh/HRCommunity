package kr.hwaryuh.community

import kr.hwaryuh.community.command.FriendCommand
import kr.hwaryuh.community.command.ProfileCommand
import kr.hwaryuh.community.data.DatabaseManager
import kr.hwaryuh.community.event.PlayerProfileEvent
import kr.hwaryuh.community.service.PlayerProfile
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin

class Main : JavaPlugin() {
    lateinit var databaseManager: DatabaseManager
    private lateinit var profileService: PlayerProfile

    override fun onEnable() {
        databaseManager = DatabaseManager(this)
        profileService = PlayerProfile()

        server.pluginManager.registerEvents(PlayerProfileEvent(this), this)

        getCommand("프로필")?.setExecutor(ProfileCommand(this))
        getCommand("친구")?.setExecutor(FriendCommand(this))
    }

    override fun onDisable() {
        // 로컬 저장소의 경우 DB를 닫는 로직은 구현하지 않아도 됨.
    }

    fun openProfileMenu(viewer: Player, target: Player) {
        val inventory = profileService.createProfileInventory(target)
        viewer.openInventory(inventory)
    }
}