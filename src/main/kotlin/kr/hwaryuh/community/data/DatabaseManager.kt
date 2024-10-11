package kr.hwaryuh.community.data

import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
// import java.sql.Connection
// import java.sql.DriverManager
import java.util.UUID

class DatabaseManager(private val plugin: JavaPlugin) {
    private val friendsFile: File
    private val friendRequestsFile: File
    private val friendsConfig: YamlConfiguration
    private val friendRequestsConfig: YamlConfiguration

    init {
        friendsFile = File(plugin.dataFolder, "friends.yml")
        friendRequestsFile = File(plugin.dataFolder, "friend_requests.yml")
        friendsConfig = YamlConfiguration.loadConfiguration(friendsFile)
        friendRequestsConfig = YamlConfiguration.loadConfiguration(friendRequestsFile)
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
}