package kr.hwaryuh.community

import kr.hwaryuh.community.command.*
import kr.hwaryuh.community.data.DatabaseManager
import kr.hwaryuh.community.event.PlayerProfileEvent
import kr.hwaryuh.community.party.PartyInviteManager
import kr.hwaryuh.community.party.PartyListener
import kr.hwaryuh.community.party.PartyManager
import kr.hwaryuh.community.service.PartyMenu
import kr.hwaryuh.community.service.FriendsList
import kr.hwaryuh.community.service.PartyInviteMenu
import kr.hwaryuh.community.service.PlayerProfile
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin

class Main : JavaPlugin() {
    lateinit var databaseManager: DatabaseManager
    private lateinit var profileService: PlayerProfile
    private lateinit var friendsList: FriendsList
    private lateinit var partyInviteManager: PartyInviteManager
    private lateinit var partyManager: PartyManager
    private lateinit var partyMenu: PartyMenu
    private lateinit var partyInviteMenu: PartyInviteMenu

    override fun onEnable() {
        saveDefaultConfig()

        databaseManager = DatabaseManager(this)
        profileService = PlayerProfile(this)
        friendsList = FriendsList(this)
        partyInviteManager = PartyInviteManager(this)
        partyManager = PartyManager(this, partyInviteManager)
        partyMenu = PartyMenu(this, partyManager, partyInviteManager)
        partyInviteMenu = PartyInviteMenu(this, partyInviteManager)

        server.pluginManager.registerEvents(PlayerProfileEvent(this), this)
        server.pluginManager.registerEvents(friendsList, this)
        server.pluginManager.registerEvents(PartyListener(this), this)
        server.pluginManager.registerEvents(partyMenu, this)
        server.pluginManager.registerEvents(partyInviteMenu, this)

        getCommand("프로필")?.setExecutor(ProfileCommand(this))

        getCommand("친구")?.setExecutor(FriendCommand(this))
        getCommand("친구")?.tabCompleter = FriendTabCompleter()

        getCommand("파티")?.setExecutor(PartyCommand(this, partyManager, partyInviteManager))
        getCommand("파티")?.tabCompleter = PartyTabCompleter(partyManager)

        getCommand("hcmu")?.setExecutor(this)
        getCommand("hcmu")?.tabCompleter = this
    }

    override fun onDisable() {
    }

    fun openProfileMenu(viewer: Player, target: Player, fromMenu: Boolean) {
        val inventory = profileService.createProfileInventory(viewer, target, fromMenu)
        viewer.openInventory(inventory)
    }

    fun openFriendsList(player: Player) {
        friendsList.open(player)
    }

    fun <T : Any> getService(clazz: Class<T>): T? {
        return when (clazz) {
            PartyMenu::class.java -> partyMenu as T
            PartyInviteMenu::class.java -> partyInviteMenu as T
            else -> null
        }
    }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (args.isNotEmpty() && args[0].equals("reload", ignoreCase = true)) {
            reloadConfig()
            sender.sendMessage("F5...")
            return true
        }
        return false
    }

    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<out String>): MutableList<String> {
        if (command.name.equals("hcmu", ignoreCase = true)) {
            if (args.size == 1) {
                return mutableListOf("reload").filter { it.startsWith(args[0], ignoreCase = true) }.toMutableList()
            }
        }
        return mutableListOf()
    }
}