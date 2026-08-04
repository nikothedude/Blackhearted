package nikoblackhearted.themes.motes.skills

import com.fs.starfarer.api.characters.LevelBasedEffect
import com.fs.starfarer.api.characters.ShipSkillEffect
import com.fs.starfarer.api.combat.MutableShipStatsAPI
import com.fs.starfarer.api.combat.ShipAPI

class BHMoteSkill {
    class Level1: ShipSkillEffect {
        override fun apply(
            stats: MutableShipStatsAPI?,
            hullSize: ShipAPI.HullSize?,
            id: String?,
            level: Float
        ) {
            TODO("Not yet implemented")
        }

        override fun unapply(
            stats: MutableShipStatsAPI?,
            hullSize: ShipAPI.HullSize?,
            id: String?
        ) {
            TODO("Not yet implemented")
        }

        override fun getEffectDescription(level: Float): String? {
            TODO("Not yet implemented")
        }

        override fun getEffectPerLevelDescription(): String? {
            TODO("Not yet implemented")
        }

        override fun getScopeDescription(): LevelBasedEffect.ScopeDescription? {
            TODO("Not yet implemented")
        }
    }
}