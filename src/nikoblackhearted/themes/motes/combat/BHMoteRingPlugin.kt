package nikoblackhearted.themes.motes.combat

import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin
import com.fs.starfarer.api.combat.CombatEngineAPI
import com.fs.starfarer.api.input.InputEventAPI
import nikoblackhearted.themes.motes.BHMoteThemeIntel

class BHMoteRingPlugin(val intel: BHMoteThemeIntel, val engine: CombatEngineAPI): BaseEveryFrameCombatPlugin() {
    override fun advance(amount: Float, events: List<InputEventAPI?>?) {
        super.advance(amount, events)

        if (engine.isPaused) return


    }
}