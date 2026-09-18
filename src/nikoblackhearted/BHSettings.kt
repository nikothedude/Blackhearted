package nikoblackhearted

import com.fs.starfarer.api.Global

object BHSettings {

    const val BASE_POINT_MULT = 0.25f

    var nexEnabled = false
    var graphicsLibEnabled = false

    fun getLoadedMods() {
        nexEnabled = Global.getSettings().modManager.isModEnabled("nexerelin")
        graphicsLibEnabled = Global.getSettings().modManager.isModEnabled("shaderLib")
    }

    fun loadSettings() {

    }

}