package nikoblackhearted

import com.fs.starfarer.api.Global

object BHSettings {

    var nexEnabled = false
    var graphicsLibEnabled = false

    fun getLoadedMods() {
        nexEnabled = Global.getSettings().modManager.isModEnabled("nexerelin")
        graphicsLibEnabled = Global.getSettings().modManager.isModEnabled("shaderLib")
    }

    fun loadSettings() {

    }

}