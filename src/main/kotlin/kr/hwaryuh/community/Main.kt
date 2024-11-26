package kr.hwaryuh.community

import kr.hwaryuh.community.command.*
import kr.hwaryuh.community.data.ConfigManager
import kr.hwaryuh.community.data.DatabaseManager
import kr.hwaryuh.community.friends.FriendsListMenu
import kr.hwaryuh.community.friends.FriendsManager
import kr.hwaryuh.community.party.*
import kr.hwaryuh.community.comp.PlaceholderAPIHook
import kr.hwaryuh.community.profile.PlayerProfileListener
import kr.hwaryuh.community.profile.PreviousMenuType
import kr.hwaryuh.community.profile.PlayerProfile
import kr.hwaryuh.community.trade.TradeListener
import kr.hwaryuh.community.trade.TradeManager
import kr.hwaryuh.community.trade.TradeMenu
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.milkbowl.vault.economy.Economy
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.plugin.RegisteredServiceProvider
import org.bukkit.plugin.java.JavaPlugin

class Main : JavaPlugin() {
    companion object {
        lateinit var economy: Economy
            private set
    }

    lateinit var databaseManager: DatabaseManager
    lateinit var configManager: ConfigManager

    lateinit var playerProfile: PlayerProfile
    lateinit var friendsManager: FriendsManager
    private lateinit var friendsListMenu: FriendsListMenu

    private lateinit var partyInviteManager: PartyInviteManager
    private lateinit var partyManager: PartyManager
    private lateinit var partyMenu: PartyMenu
    private lateinit var partyInviteMenu: PartyInviteMenu

    private lateinit var tradeManager: TradeManager
    private lateinit var tradeMenu: TradeMenu
    private lateinit var tradeListener: TradeListener

    override fun onEnable() {
        initializeCore()
        initializeManagers()
        registerEvents()
        registerCommands()

        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) PlaceholderAPIHook().register()
    }

    override fun onDisable() {
        // databaseManager.closeConnection()
    }

    private fun initializeCore() {
        saveDefaultConfig()
        configManager = ConfigManager(this).apply {
            loadConfig()
        }
        databaseManager = DatabaseManager(this)

        if (!setupEconomy()) {
            logger.severe("Vault를 찾을 수 없습니다.")
            return
        }
    }

    private fun initializeManagers() {
        tradeMenu = TradeMenu(this)
        tradeManager = TradeManager(this)
        tradeListener = TradeListener(this).apply {
            setTradeManager(tradeManager)
            setTradeMenu(tradeMenu)
        }
        tradeManager.setTradeMenu(tradeMenu)
        tradeMenu.setTradeManager(tradeManager)

        playerProfile = PlayerProfile(this)
        friendsListMenu = FriendsListMenu(this)
        friendsManager = FriendsManager(this)

        partyManager = PartyManager(this)
        partyInviteManager = PartyInviteManager(this, partyManager)
        partyManager.setPartyInviteManager(partyInviteManager)
        partyMenu = PartyMenu(this, partyManager, partyInviteManager)
        partyInviteMenu = PartyInviteMenu(this, partyInviteManager)
    }

    private fun registerEvents() {
        server.pluginManager.apply {
            registerEvents(PlayerProfileListener(this@Main), this@Main)
            registerEvents(friendsListMenu, this@Main)
            registerEvents(PartyListener(this@Main, partyManager), this@Main)
            registerEvents(partyMenu, this@Main)
            registerEvents(partyInviteMenu, this@Main)
            registerEvents(tradeListener, this@Main)
        }
    }

    private fun registerCommands() {
        getCommand("hcmu")?.apply {
            setExecutor(this@Main)
            tabCompleter = this@Main
        }

        getCommand("profile")?.apply {
            setExecutor(ProfileCommand(this@Main))
            tabCompleter = ProfileCommand(this@Main)
        }

        getCommand("friends")?.apply {
            setExecutor(FriendsCommand(this@Main, friendsManager))
            tabCompleter = FriendsCommand(this@Main, friendsManager)
        }

        getCommand("party")?.apply {
            setExecutor(PartyCommand(this@Main, partyManager, partyInviteManager))
            tabCompleter = PartyCommand(this@Main, partyManager, partyInviteManager)
        }

        getCommand("trade")?.apply {
            setExecutor(TradeCommand(tradeManager))
            tabCompleter = TradeCommand(tradeManager)
        }
    }

    private fun setupEconomy(): Boolean {
        if (server.pluginManager.getPlugin("Vault") == null) return false
        val rsp: RegisteredServiceProvider<Economy> = server.servicesManager.getRegistration(Economy::class.java) ?: return false
        economy = rsp.provider

        return true
    }

    fun openProfileMenu(viewer: Player, target: Player, fromMenu: Boolean, previousMenu: PreviousMenuType) {
        val inventory = playerProfile.profileMenu(viewer, target, fromMenu, previousMenu)
        viewer.openInventory(inventory)
    }

    fun openOfflineProfileMenu(viewer: Player, target: OfflinePlayer, fromMenu: Boolean, previousMenu: PreviousMenuType) {
        val inventory = playerProfile.offlineProfileMenu(viewer, target, fromMenu, previousMenu)
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
            if (!sender.hasPermission("hcmu.reload")) {
                sender.sendMessage(Component.text("알 수 없는 명령어입니다.").color(NamedTextColor.RED))
                return true
            }
            reloadConfig()
            databaseManager.reload()
            configManager.reload()
            sender.sendMessage("F5...")
            return true
        }
        return false
    }

    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<out String>): MutableList<String> {
        if (sender !is Player || !sender.hasPermission("hcmu.reload")) {
            return mutableListOf()
        }

        if (command.name.equals("hcmu", ignoreCase = true) && args.size == 1) {
            return mutableListOf("reload")
                .filter { it.startsWith(args[0], ignoreCase = true) }
                .toMutableList()
        }
        return mutableListOf()
    }
}