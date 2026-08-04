package nikoblackhearted

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin
import com.fs.starfarer.api.input.InputEventAPI

class BHCombatPlugin: BaseEveryFrameCombatPlugin() {
    val engine = Global.getCombatEngine()

    override fun advance(amount: Float, events: List<InputEventAPI?>?) {
        if (engine.isPaused || engine.isSimulation) return

        val intel = BHHandler.getThemeIntel()
        intel.combatInitialized(engine)

        engine.removePlugin(this)
    }

}