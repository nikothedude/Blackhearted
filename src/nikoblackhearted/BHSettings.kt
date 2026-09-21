package nikoblackhearted

import com.fs.starfarer.api.Global
import lunalib.lunaSettings.LunaSettings

object BHSettings {

    const val BASE_POINT_MULT = 1f

    const val MOD_ID = "nikoblackhearted"
    const val GLOBAL_MULT = 0.75f

    var nexEnabled = false
    var graphicsLibEnabled = false

    var pointGenMult = 1f

    fun getLoadedMods() {
        nexEnabled = Global.getSettings().modManager.isModEnabled("nexerelin")
        graphicsLibEnabled = Global.getSettings().modManager.isModEnabled("shaderLib")
        pointGenMult = LunaSettings.getFloat(MOD_ID, "BH_pointGenMult")!!
    }

    fun loadSettings() {

    }

}