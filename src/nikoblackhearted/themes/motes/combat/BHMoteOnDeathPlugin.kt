package nikoblackhearted.themes.motes.combat

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin
import com.fs.starfarer.api.combat.CombatEntityAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI
import com.fs.starfarer.api.combat.listeners.DamageListener
import com.fs.starfarer.api.combat.listeners.DamageTakenModifier
import com.fs.starfarer.api.impl.combat.MoteAIScript
import com.fs.starfarer.combat.entities.Ship
import nikoblackhearted.Mathutils.prob
import org.lwjgl.util.vector.Vector2f

class BHMoteOnDeathPlugin: DamageListener {

    companion object {
        val hullSizeToNum = hashMapOf(
            Pair(ShipAPI.HullSize.FIGHTER, 1),
            Pair(ShipAPI.HullSize.FRIGATE, 1),
            Pair(ShipAPI.HullSize.DESTROYER, 3),
            Pair(ShipAPI.HullSize.CRUISER, 4),
            Pair(ShipAPI.HullSize.CAPITAL_SHIP, 6),
        )
        const val RANGE = 3000f
    }

    override fun reportDamageApplied(
        source: Any?,
        target: CombatEntityAPI?,
        result: ApplyDamageResultAPI?
    ) {
        if (target !is ShipAPI) return
        if (!target.isHulk) return
        if (target.customData["BH_didMoteDeathSpawn"] == true) return
        if (target.originalOwner == 0) {
            spawnMotesPlayer(target)
        } else if (target.originalOwner == 1) {
            val checkIterator = Global.getCombatEngine().shipGrid.getCheckIterator(target.location, RANGE, RANGE)
            while (checkIterator.hasNext()) {
                val ship = checkIterator.next() as? ShipAPI ?: continue
                if (ship.isAlly || ship.owner != 0) continue
                if (ship.isHulk || ship.isFighter) continue

                spawnMotesOn(target, false)
            }
        }

        target.setCustomData("BH_didMoteDeathSpawn", true)
    }

    private fun spawnMotesPlayer(target: ShipAPI) {
        spawnMotesOn(target, true)
    }

    private fun spawnMotesOn(target: ShipAPI, wasPlayerShip: Boolean) {
        if (target.isFighter) {
            if (wasPlayerShip) return
            if (!prob(20)) return
        }
        val nextTarget = getNearestTarget(target.location) ?: return

        var moteSpawns = hullSizeToNum[target.hullSize] ?: return
        if (wasPlayerShip) moteSpawns *= 3

        while (moteSpawns-- > 0) {
            val mote = BHMoteRingPlugin.BHMoteRingPerShipPlugin.spawnMote(target, false)
            mote.owner = 0
            mote.missileAI = BHDeathMoteScript(mote)
            val ai = mote.unwrappedMissileAI as BHDeathMoteScript
            ai.target = nextTarget
        }
    }

    private fun getNearestTarget(source: Vector2f): ShipAPI? {
        val checkIterator = Global.getCombatEngine().shipGrid.getCheckIterator(source, 5000f, 5000f)
        val ships = HashSet<ShipAPI>()
        while (checkIterator.hasNext()) {
            val ship = checkIterator.next() as? ShipAPI ?: continue
            ships += ship
        }
        for (ship in ships.shuffled()) {
            if (ship.isFighter || ship.isAlly || ship.owner == 0) continue
            if (ship.isHulk) continue

            return ship
        }
        return null
    }
}