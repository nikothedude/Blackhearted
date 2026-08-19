package nikoblackhearted.themes.motes.combat

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.*
import com.fs.starfarer.api.impl.combat.MoteAIScript
import com.fs.starfarer.api.impl.combat.MoteControlScript
import com.fs.starfarer.api.input.InputEventAPI
import com.fs.starfarer.api.util.IntervalUtil
import nikoblackhearted.ShipUtils.getCollisionRadWithModules
import nikoblackhearted.themes.motes.BHMoteThemeIntel
import nikoblackhearted.themes.motes.combat.subsystems.BHMoteStrikeSubSystem
import org.lazywizard.lazylib.MathUtils
import org.lazywizard.lazylib.VectorUtils
import org.lwjgl.util.vector.Vector2f
import org.magiclib.kotlin.setAlpha
import org.magiclib.subsystems.MagicSubsystem
import org.magiclib.subsystems.MagicSubsystemsManager
import kotlin.math.ceil

class BHMoteRingPlugin(val intel: BHMoteThemeIntel, val engine: CombatEngineAPI): BaseEveryFrameCombatPlugin() {

    companion object {
        fun ShipAPI.isSuitableMoteRingTargetFirstStep(considerExistingRing: Boolean = true): Boolean {
            val intel = BHMoteThemeIntel.get() ?: return false
            if (!intel.moteRingReached) return false
            if (!intel.fleetwideCombatMotes && Global.getCombatEngine().playerShip != this) return false
            return owner == 0 && !isAlly && !isHulk && !isFighter && (!considerExistingRing || customData["BHMoteRing"] == null)
        }
    }

    val scripts = HashSet<BHMoteRingPerShipPlugin>()
    val checkInterval = IntervalUtil(0.5f, 0.6f)

    override fun advance(amount: Float, events: List<InputEventAPI?>?) {
        super.advance(amount, events)

        if (engine.isPaused) return

        checkInterval.advance(amount)
        if (!checkInterval.intervalElapsed()) {
            return
        }
        scripts.retainAll { it.isAlive }
        val intel = BHMoteThemeIntel.get() ?: return
        if (!intel.moteRingReached) return
        val flagship = engine.playerShip
        if (intel.fleetwideCombatMotes) {
            for (ship in engine.ships.filter { it.isSuitableMoteRingTargetFirstStep() }) {
                addRing(ship)
            }
        } else {
            if (flagship.isSuitableMoteRingTargetFirstStep()) {
                addRing(flagship)
            }
        }

        if (intel.combatMoteStrikeReached && flagship != null && flagship.customData["BHHasMoteStrikeSS"] != true) {
            if (flagship.isFighter || flagship.isShuttlePod) return
            MagicSubsystemsManager.addSubsystemToShip(
                flagship,
                BHMoteStrikeSubSystem(flagship)
            )
            flagship.setCustomData("BHHasMoteStrikeSS", true)
        }
    }

    private fun addRing(ship: ShipAPI) {
        val script = BHMoteRingPerShipPlugin(ship, engine)
        ship.setCustomData("BHMoteRing", script)
        scripts += script
        engine.addPlugin(script)
    }

    class BHMoteRingPerShipPlugin(val ship: ShipAPI, val engine: CombatEngineAPI): BaseEveryFrameCombatPlugin() {
        companion object {
            fun getNumMotesForShip(ship: ShipAPI): Int {
                if (ship.isHulk || ship.isFighter) return 0

                val intel = BHMoteThemeIntel.get() ?: return 0
                val isPlayer = Global.getCombatEngine().playerShip == ship
                return ceil(((ship.getCollisionRadWithModules() * BASE_COL_RADIUS_TO_MOTES_MULT) * intel.getCombatMaxMotesMult(isPlayer)).coerceAtLeast(1f)).toInt()
            }
            fun getWeaponId(ship: ShipAPI): String {
                return MoteControlScript.MOTELAUNCHER
            }

            fun spawnMote(ship: ShipAPI, passShip: Boolean = true): MissileAPI {

                val engine = Global.getCombatEngine()

                val randOrbitLoc = MathUtils.getRandomPointOnCircumference(
                    ship.location,
                    ship.getCollisionRadWithModules()
                )

                val id = getWeaponId(ship)
                val angle = VectorUtils.getAngle(ship.location, randOrbitLoc)
                val mote = engine.spawnProjectile(
                    if (passShip) ship else null,
                    null,
                    id,
                    randOrbitLoc,
                    angle,
                    null
                ) as MissileAPI
                mote.setWeaponSpec(id)
                mote.missileAI = MoteAIScript(mote)
                mote.activeLayers.remove(CombatEngineLayers.FF_INDICATORS_LAYER)
                mote.empResistance = 10000

                Global.getSoundPlayer().playSound("mote_attractor_launch_mote", 1f, 0.25f, randOrbitLoc, Vector2f())

                //engine.addPlugin(MoteFadeInPlugin(mote, engine))
                engine.spawnMuzzleFlashOrSmoke(
                    ship,
                    randOrbitLoc,
                    mote.weaponSpec,
                    angle
                )
                return mote
            }

            const val BASE_COL_RADIUS_TO_MOTES_MULT = 0.02f
        }

        val spawnInterval = IntervalUtil(0.25f, 1.25f)
        val respawnInterval: IntervalUtil
        val checkInterval = IntervalUtil(0.3f, 0.4f)
        var motesLost = 0
        val motes = HashSet<MissileAPI>()
        var isAlive = true

        init {
            val num = getNumMotesForShip(ship)
            val base = 50f / num
            respawnInterval = IntervalUtil(base, base + 1f)
        }

        override fun advance(amount: Float, events: List<InputEventAPI?>?) {
            super.advance(amount, events)
            if (engine.isPaused) return

            val iterator = motes.iterator()
            while (iterator.hasNext()) {
                val mote = iterator.next()
                if (!engine.isMissileAlive(mote)) {
                    motesLost++
                    MoteControlScript.getSharedData(ship).motes.remove(mote)
                    iterator.remove()
                }
            }

            checkInterval.advance(amount)
            if (checkInterval.intervalElapsed()) {
                if (!ship.isSuitableMoteRingTargetFirstStep(false)) {
                    delete()
                    return
                }
            }

            if (motesLost > 0) {
                respawnInterval.advance(amount)
                if (respawnInterval.intervalElapsed()) {
                    motesLost = (motesLost - 1).coerceAtLeast(0)
                }
            }

            spawnInterval.advance(amount)
            if (spawnInterval.intervalElapsed()) {
                trySpawningMote()
            }
        }

        private fun trySpawningMote() {
            if (motes.size >= getTempMaxMotes()) return

            val mote = spawnMote(ship)

            val data = MoteControlScript.getSharedData(ship)
            data.motes.add(mote)
            motes += mote
        }

        fun getTempMaxMotes(): Int {
            return getNumMotesForShip(ship) - motesLost
        }

        fun delete() {
            motes.forEach { it.flameOut() }
            motes.clear()

            engine.removePlugin(this)

            ship.setCustomData("BHMoteRing", null)
            isAlive = false
            return
        }
    }
}