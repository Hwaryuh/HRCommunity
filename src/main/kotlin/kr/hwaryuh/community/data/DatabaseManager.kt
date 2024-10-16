package kr.hwaryuh.community.data

import kr.hwaryuh.community.Main
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
// import java.sql.Connection
// import java.sql.DriverManager
import java.util.UUID

class DatabaseManager(private val plugin: Main) {
    private lateinit var friendsFile: File
    private lateinit var friendRequestsFile: File
    private lateinit var friendsConfig: YamlConfiguration
    private lateinit var friendRequestsConfig: YamlConfiguration

    init {
        loadConfigs()
    }

    @Synchronized
    fun addFriend(playerUUID: UUID, friendUUID: UUID) {
        addToList(friendsConfig, playerUUID.toString(), friendUUID.toString())
        addToList(friendsConfig, friendUUID.toString(), playerUUID.toString())
        saveConfig(friendsConfig, friendsFile)
    }

    @Synchronized
    fun removeFriend(playerUUID: UUID, friendUUID: UUID) {
        removeFromList(friendsConfig, playerUUID.toString(), friendUUID.toString())
        removeFromList(friendsConfig, friendUUID.toString(), playerUUID.toString())
        saveConfig(friendsConfig, friendsFile)
    }

    fun getFriends(playerUUID: UUID): List<UUID> {
        return friendsConfig.getStringList(playerUUID.toString()).mapNotNull {
            try {
                UUID.fromString(it)
            } catch (e: IllegalArgumentException) {
                plugin.logger.warning("Invalid UUID found in friends list: $it")
                null
            }
        }
    }

    fun areFriends(playerUUID1: UUID, playerUUID2: UUID): Boolean {
        return friendsConfig.getStringList(playerUUID1.toString()).contains(playerUUID2.toString())
    }

    @Synchronized
    fun addFriendRequest(senderUUID: UUID, receiverUUID: UUID, message: String? = null) {
        val requests = friendRequestsConfig.getConfigurationSection(receiverUUID.toString())
            ?: friendRequestsConfig.createSection(receiverUUID.toString())
        requests.set(senderUUID.toString(), mapOf(
            "time" to System.currentTimeMillis(),
            "message" to (message ?: "")
        ))
        saveConfig(friendRequestsConfig, friendRequestsFile)
    }

    @Synchronized
    fun removeFriendRequest(senderUUID: UUID, receiverUUID: UUID) {
        val requests = friendRequestsConfig.getConfigurationSection(receiverUUID.toString()) ?: return
        requests.set(senderUUID.toString(), null)
        saveConfig(friendRequestsConfig, friendRequestsFile)
    }

    fun hasFriendRequest(senderUUID: UUID, receiverUUID: UUID): Boolean {
        val requests = friendRequestsConfig.getConfigurationSection(receiverUUID.toString()) ?: return false
        return requests.contains(senderUUID.toString())
    }

    fun getFriendRequests(playerUUID: UUID): List<UUID> {
        return friendRequestsConfig.getStringList(playerUUID.toString()).mapNotNull {
            try {
                UUID.fromString(it)
            } catch (e: IllegalArgumentException) {
                plugin.logger.warning("잘못된 UUID 형식: $it")
                null
            }
        }
    }

    private fun addToList(config: YamlConfiguration, key: String, value: String) {
        val list = config.getStringList(key)
        if (!list.contains(value)) {
            list.add(value)
            config.set(key, list)
        }
    }

    private fun removeFromList(config: YamlConfiguration, key: String, value: String) {
        val list = config.getStringList(key)
        if (list.remove(value)) {
            config.set(key, list)
        }
    }

    private fun saveConfig(config: YamlConfiguration, file: File) {
        try {
            config.save(file)
        } catch (e: Exception) {
            plugin.logger.severe("Failed to save config file: ${file.name}")
            e.printStackTrace()
        }
    }

    private fun loadConfigs() {
        friendsFile = File(plugin.dataFolder, "friends.yml")
        friendRequestsFile = File(plugin.dataFolder, "friend_requests.yml")
        friendsConfig = YamlConfiguration.loadConfiguration(friendsFile)
        friendRequestsConfig = YamlConfiguration.loadConfiguration(friendRequestsFile)
    }

    fun reload() {
        loadConfigs()
    }
}