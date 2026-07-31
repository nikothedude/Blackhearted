package nikoblackhearted

import com.fs.starfarer.api.Global

object BHSettings {

    var nexEnabled = false

    fun getLoadedMods() {
        nexEnabled = Global.getSettings().modManager.isModEnabled("nexerelin")
    }

    fun loadSettings() {

    }

}