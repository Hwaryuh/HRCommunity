package kr.hwaryuh.community.data

import kr.hwaryuh.community.Main

class ConfigManager(private val plugin: Main) {
    private val menuTitles = mutableMapOf<String, String>()

    fun loadConfig() {
        plugin.saveDefaultConfig()
        plugin.reloadConfig()
        loadMenuTitles()
    }

    private fun loadMenuTitles() {
        val config = plugin.config
        menuTitles["friends-list"] = config.getString("menu-titles.friends-list") ?: "친구 목록"
        menuTitles["party-invite"] = config.getString("menu-titles.party-invite") ?: "누구를 초대할까요?"
        menuTitles["party-menu"] = config.getString("menu-titles.party-menu") ?: "{owner}의 파티 ({current}/{max})"
        menuTitles["profile"] = config.getString("menu-titles.profile") ?: "{player}의 프로필"
        menuTitles["offline-profile"] = config.getString("menu-titles.offline-profile") ?: "{player}의 프로필"
    }

    fun getMenuTitle(key: String): String {
        return menuTitles[key] ?: "메뉴"
    }

    fun reload() {
        plugin.reloadConfig()
        loadMenuTitles()
    }
}