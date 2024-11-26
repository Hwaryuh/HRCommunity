package kr.hwaryuh.community.comp

import me.clip.placeholderapi.expansion.PlaceholderExpansion
import org.bukkit.OfflinePlayer

class PlaceholderAPIHook : PlaceholderExpansion() {
    override fun getIdentifier(): String = "hcmu"

    override fun getAuthor(): String = "useemeonascreen"

    override fun getVersion(): String = "0.1"

    override fun persist(): Boolean = true

    override fun onRequest(player: OfflinePlayer?, params: String): String? {
        TODO()
    }
}