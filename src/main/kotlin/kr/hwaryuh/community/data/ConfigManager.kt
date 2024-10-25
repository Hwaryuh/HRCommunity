package kr.hwaryuh.community.data

import kr.hwaryuh.community.Main
import org.bukkit.Material
import org.bukkit.configuration.ConfigurationSection

class ConfigManager(private val plugin: Main) {
    private val menuTitles = mutableMapOf<String, String>()
    private val currencyButtons = mutableListOf<CurrencyButtonConfig>()
    private lateinit var readyButtonConfig: ReadyButtonConfig

    fun loadConfig() {
        plugin.saveDefaultConfig()
        plugin.reloadConfig()
        loadMenuTitles()
        loadTradeMenuConfig()
    }

    private fun loadMenuTitles() {
        val config = plugin.config
        menuTitles["friends-list"] = config.getString("menu-titles.friends-list") ?: "친구 목록"
        menuTitles["party-invite"] = config.getString("menu-titles.party-invite") ?: "누구를 초대할까요?"
        menuTitles["party-menu"] = config.getString("menu-titles.party-menu") ?: "{owner}의 파티 ({current}/{max})"
        menuTitles["profile"] = config.getString("menu-titles.profile") ?: "{player}의 프로필"
        menuTitles["offline-profile"] = config.getString("menu-titles.offline-profile") ?: "{player}의 프로필"
        menuTitles["trade-menu"] = config.getString("menu-titles.trade-menu") ?: "교환 메뉴 {balance} {added} {other_added}"
    }

    private fun loadTradeMenuConfig() {
        val config = plugin.config
        val tradeMenuSection = config.getConfigurationSection("trade-menu") ?: return

        loadCurrencyButtons(tradeMenuSection.getConfigurationSection("currency-buttons"))
        loadReadyButton(tradeMenuSection.getConfigurationSection("ready-button"))
    }

    private fun loadCurrencyButtons(section: ConfigurationSection?) {
        currencyButtons.clear()
        section?.let { buttonSection ->
            for (key in buttonSection.getKeys(false)) {
                val buttonConfig = buttonSection.getConfigurationSection(key) ?: continue

                val amount = key.split("_").getOrNull(1)?.toIntOrNull() ?: continue

                val material = Material.valueOf(buttonConfig.getString("material")?.uppercase() ?: "GOLD_NUGGET")
                val customModelData = buttonConfig.getInt("custom-model-data")

                currencyButtons.add(CurrencyButtonConfig(amount, material, customModelData))
            }
        }
    }

    private fun loadReadyButton(section: ConfigurationSection?) {
        val notReadySection = section?.getConfigurationSection("not-ready")
        val readySection = section?.getConfigurationSection("ready")

        val notReadyMaterial = Material.valueOf(notReadySection?.getString("material")?.uppercase() ?: "GHAST_TEAR")
        val notReadyCustomModelData = notReadySection?.getInt("custom-model-data") ?: 10000

        val readyMaterial = Material.valueOf(readySection?.getString("material")?.uppercase() ?: "GHAST_TEAR")
        val readyCustomModelData = readySection?.getInt("custom-model-data") ?: 10001

        readyButtonConfig = ReadyButtonConfig(
            ReadyButtonState(notReadyMaterial, notReadyCustomModelData),
            ReadyButtonState(readyMaterial, readyCustomModelData)
        )
    }

    fun getMenuTitle(key: String): String {
        return menuTitles[key] ?: "메뉴"
    }

    fun getCurrencyButtons(): List<CurrencyButtonConfig> {
        return currencyButtons
    }

    fun getReadyButtonConfig(): ReadyButtonConfig {
        return readyButtonConfig
    }

    fun reload() {
        plugin.reloadConfig()
        loadMenuTitles()
        loadTradeMenuConfig()
    }
}

data class CurrencyButtonConfig(val amount: Int, val material: Material, val customModelData: Int)

data class ReadyButtonConfig(val notReady: ReadyButtonState, val ready: ReadyButtonState)

data class ReadyButtonState(val material: Material, val customModelData: Int)