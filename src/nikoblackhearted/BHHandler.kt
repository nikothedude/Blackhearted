package nikoblackhearted

import com.fs.starfarer.api.Global
import nikoblackhearted.themes.BHThemeMainIntel
import nikoblackhearted.themes.Theme

object BHHandler {
    const val BLACKHEARTED_BOOLEAN_KEY = "\$BH_isBlackhearted"
    const val THEME_KEY = "\$BH_themeKey"
    const val THEME_INTEL_KEY = "\$BH_themeIntelKey"
    const val INTEL_KEY = "Blackhearted"
    const val BUTTON_DATA = "BH_themeData"
    const val THEME_PLUGIN_KEY = "\$BH_themePlugin"

    fun getTheme(): Theme? = Global.getSector().memoryWithoutUpdate[THEME_KEY] as? Theme

    fun isEvil() = Global.getSector().memoryWithoutUpdate.getBoolean(BLACKHEARTED_BOOLEAN_KEY)

    fun becomeEvil() {
        if (isEvil()) return
        Global.getSector().memoryWithoutUpdate[BLACKHEARTED_BOOLEAN_KEY] = true
        BHIntroIntel.create()
    }

    fun setTheme(theme: Theme) {
        Global.getSector().memoryWithoutUpdate[THEME_KEY] = theme
        theme.init()
    }

    fun getThemeIntel(): BHThemeMainIntel? = Global.getSector().memoryWithoutUpdate[THEME_INTEL_KEY] as? BHThemeMainIntel
}