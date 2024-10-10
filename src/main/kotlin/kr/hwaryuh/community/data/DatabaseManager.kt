package kr.hwaryuh.community.data

import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
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

    fun addFriend(playerUUID: UUID, friendUUID: UUID) {
        val playerFriends = friendsConfig.getStringList(playerUUID.toString())
        playerFriends.add(friendUUID.toString())
        friendsConfig.set(playerUUID.toString(), playerFriends)
        saveFriendsConfig()
    }

    fun removeFriend(playerUUID: UUID, friendUUID: UUID) {
        val playerFriends = friendsConfig.getStringList(playerUUID.toString())
        playerFriends.remove(friendUUID.toString())
        friendsConfig.set(playerUUID.toString(), playerFriends)
        saveFriendsConfig()
    }

    fun getFriends(playerUUID: UUID): List<UUID> {
        return friendsConfig.getStringList(playerUUID.toString()).map { UUID.fromString(it) }
    }

    fun addFriendRequest(senderUUID: UUID, receiverUUID: UUID) {
        val requests = friendRequestsConfig.getStringList(receiverUUID.toString())
        requests.add(senderUUID.toString())
        friendRequestsConfig.set(receiverUUID.toString(), requests)
        saveFriendRequestsConfig()
    }

    fun removeFriendRequest(senderUUID: UUID, receiverUUID: UUID) {
        val requests = friendRequestsConfig.getStringList(receiverUUID.toString())
        requests.remove(senderUUID.toString())
        friendRequestsConfig.set(receiverUUID.toString(), requests)
        saveFriendRequestsConfig()
    }

    fun hasFriendRequest(senderUUID: UUID, receiverUUID: UUID): Boolean {
        val requests = friendRequestsConfig.getStringList(receiverUUID.toString())
        return requests.contains(senderUUID.toString())
    }

    fun getFriendRequests(playerUUID: UUID): List<UUID> {
        return friendRequestsConfig.getStringList(playerUUID.toString()).map { UUID.fromString(it) }
    }

    private fun saveFriendsConfig() {
        friendsConfig.save(friendsFile)
    }

    private fun saveFriendRequestsConfig() {
        friendRequestsConfig.save(friendRequestsFile)
    }

    fun closeConnection() {
        // No need to close anything for local storage
    }
}