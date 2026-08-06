package nikoblackhearted.themes.motes.skills

import com.fs.starfarer.api.characters.FleetTotalItem
import com.fs.starfarer.api.characters.FleetTotalSource
import com.fs.starfarer.api.characters.LevelBasedEffect
import com.fs.starfarer.api.characters.ShipSkillEffect
import com.fs.starfarer.api.combat.MutableShipStatsAPI
import com.fs.starfarer.api.combat.ShipAPI

class BHMotesCrusaderSkillFleet {
    class Level1: ShipSkillEffect, FleetTotalSource {
        override fun apply(
            stats: MutableShipStatsAPI?,
            hullSize: ShipAPI.HullSize?,
            id: String?,
            level: Float
        ) {
            val ship = stats?.entity as? ShipAPI ?: return
            if (!ship.hasListenerOfClass(BHMotesCrusaderSkill.CrusaderSecondLifeListener::class.java)) {
                var listener = BHMotesCrusaderSkill.CrusaderSecondLifeListener(ship)
                ship.addListener(listener)
            }
        }

        override fun unapply(
            stats: MutableShipStatsAPI?,
            hullSize: ShipAPI.HullSize?,
            id: String?
        ) {
            val ship = stats?.entity as? ShipAPI ?: return
            ship.removeListenerOfClass(BHMotesCrusaderSkill.CrusaderSecondLifeListener::class.java)
        }

        override fun getEffectDescription(level: Float): String? {
            return "a"
        }

        override fun getEffectPerLevelDescription(): String? {
            return null
        }

        override fun getScopeDescription(): LevelBasedEffect.ScopeDescription? {
            return LevelBasedEffect.ScopeDescription.ALL_COMBAT_SHIPS
        }

        override fun getFleetTotalItem(): FleetTotalItem? {
            return null
        }
    }
}