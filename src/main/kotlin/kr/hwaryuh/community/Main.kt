package kr.hwaryuh.community

import kr.hwaryuh.community.command.*
import kr.hwaryuh.community.data.DatabaseManager
import kr.hwaryuh.community.friends.FriendsListMenu
import kr.hwaryuh.community.party.*
import kr.hwaryuh.community.profile.PlayerProfileListener
import kr.hwaryuh.community.profile.PreviousMenuType
import kr.hwaryuh.community.profile.PlayerProfile
import org.bukkit.OfflinePlayer
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin

class Main : JavaPlugin() {
    lateinit var databaseManager: DatabaseManager
    lateinit var playerProfile: PlayerProfile
    private lateinit var friendsListMenu: FriendsListMenu
    private lateinit var partyInviteManager: PartyInviteManager
    private lateinit var partyManager: PartyManager
    private lateinit var partyMenu: PartyMenu
    private lateinit var partyInviteMenu: PartyInviteMenu

    override fun onEnable() {
        saveDefaultConfig()

        databaseManager = DatabaseManager(this)
        playerProfile = PlayerProfile(this)
        friendsListMenu = FriendsListMenu(this)
        partyInviteManager = PartyInviteManager(this)
        partyManager = PartyManager(this, partyInviteManager)
        partyMenu = PartyMenu(this, partyManager, partyInviteManager)
        partyInviteMenu = PartyInviteMenu(this, partyInviteManager)

        server.pluginManager.registerEvents(PlayerProfileListener(this), this)
        server.pluginManager.registerEvents(friendsListMenu, this)
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
        // databaseManager.closeConnection()
    }

    fun openProfileMenu(viewer: Player, target: Player, fromMenu: Boolean, previousMenu: PreviousMenuType) {
        val inventory = playerProfile.createProfileInventory(viewer, target, fromMenu, previousMenu)
        viewer.openInventory(inventory)
    }

    fun openOfflineProfileMenu(viewer: Player, target: OfflinePlayer, fromMenu: Boolean, previousMenu: PreviousMenuType) {
        val inventory = playerProfile.createOfflineProfileInventory(viewer, target, fromMenu, previousMenu)
        viewer.openInventory(inventory)
    }

    fun openFriendsList(player: Player) {
        friendsListMenu.open(player)
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