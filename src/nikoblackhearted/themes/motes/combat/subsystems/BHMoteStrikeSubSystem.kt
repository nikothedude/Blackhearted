package nikoblackhearted.themes.motes.combat.subsystems

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.*
import com.fs.starfarer.api.combat.EmpArcEntityAPI.EmpArcParams
import com.fs.starfarer.api.impl.combat.MoteControlScript
import com.fs.starfarer.api.impl.combat.MoteControlScript.getSharedData
import com.fs.starfarer.api.input.InputEventAPI
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import nikoblackhearted.themes.motes.combat.BHMoteRingPlugin.BHMoteRingPerShipPlugin
import org.lwjgl.util.vector.Vector2f
import org.magiclib.subsystems.MagicSubsystem
import java.awt.Color

class BHMoteStrikeSubSystem(ship: ShipAPI): MagicSubsystem(ship) {
    override fun getBaseActiveDuration(): Float {
        return 0f
    }

    override fun getBaseCooldownDuration(): Float {
        return 0.2f
    }

    val interval = IntervalUtil(1f, 3.1f)

    override fun shouldActivateAI(amount: Float): Boolean {
        if (ship != Global.getCombatEngine().playerShip) return false
        interval.advance(amount)
        if (!interval.intervalElapsed()) return false
        return getLockTarget(ship.mouseTarget) != null
    }

    override fun onActivate() {
        super.onActivate()

        val mouseTarget = ship.mouseTarget
        val target = getLockTarget(mouseTarget)

        val shared = getSharedData(ship)
        if (target == null) {
            shared.attractorTarget = Vector2f(mouseTarget)
            shared.attractorRemaining = 10f
        } else {
            shared.attractorLock = target
            shared.attractorTarget = target.location
            shared.attractorRemaining = 20f

            // need to do this in a script because when the ship is phased, the charge-in time of the system (0.1s)
            // is not enough for the jitter to come to full effect (which requires 0.1s "normal" time)
            Global.getCombatEngine().addPlugin(
                createTargetJitterPlugin(
                    target,
                    0.5f, 0.5f,
                    MoteControlScript.getJitterColor(ship)
                )
            )

            val engine = Global.getCombatEngine()
            val params = EmpArcParams()

            params.segmentLengthMult = 8f
            params.zigZagReductionFactor = 0.15f

            params.brightSpotFullFraction = 0.5f
            params.brightSpotFadeFraction = 0.5f

            val dist = Misc.getDistance(ship.location, target.location)
            params.flickerRateMult = 0.6f - dist / 3000f
            if (params.flickerRateMult < 0.3f) {
                params.flickerRateMult = 0.3f
            }
            val arc = engine.spawnEmpArc(
                ship, ship.location, ship, target,
                DamageType.ENERGY,
                0f,
                0f,  // emp
                100000f,  // max range
                "mote_attractor_targeted_ship",
                40f,  // thickness
                //new Color(100,165,255,255),
                MoteControlScript.getEMPColor(ship),
                Color(255, 255, 255, 255),
                params
            ) as EmpArcEntityAPI
            arc.setTargetToShipCenter(ship.location, target)
            arc.coreWidthOverride = 30f
            arc.setSingleFlickerMode(true)
        }
    }

    //	public int getMaxMotes() {
    //		return MAX_MOTES;
    //	}
    protected fun createTargetJitterPlugin(
        target: ShipAPI,
        `in`: Float,
        out: Float,
        jitterColor: Color?
    ): EveryFrameCombatPlugin {
        return object : BaseEveryFrameCombatPlugin() {
            var elapsed: Float = 0f
            override fun advance(amount: Float, events: MutableList<InputEventAPI?>?) {
                if (Global.getCombatEngine().isPaused) return

                elapsed += amount


                var level = 0f
                if (elapsed < `in`) {
                    level = elapsed / `in`
                } else if (elapsed < `in` + out) {
                    level = 1f - (elapsed - `in`) / out
                    level *= level
                } else {
                    Global.getCombatEngine().removePlugin(this)
                    return
                }


                if (level > 0) {
                    val jitterLevel = level
                    val maxRangeBonus = 50f
                    val jitterRangeBonus = jitterLevel * maxRangeBonus
                    target.setJitterUnder(this, jitterColor, jitterLevel, 10, 0f, jitterRangeBonus)
                    target.setJitter(this, jitterColor, jitterLevel, 4, 0f, 0 + jitterRangeBonus)
                }
            }
        }
    }

    fun getMoteScript(): BHMoteRingPerShipPlugin? = ship.customData["BHMoteRing"] as? BHMoteRingPerShipPlugin

    override fun getRange(): Float {
        if (ship == null) return 3000f
        return ship.mutableStats.systemRangeBonus.computeEffective(3000f)
    }

    fun getLockTarget(loc: Vector2f): ShipAPI? {
        for (other in Global.getCombatEngine().ships) {
            if (other.isFighter) continue
            if (other.owner == ship.owner) continue
            if (other.isHulk) continue
            if (!other.isTargetable) continue

            var dist = Misc.getDistance(ship.location, other.location)
            if (dist > range) continue

            dist = Misc.getDistance(loc, other.location)
            if (dist < other.collisionRadius + 50f) {
                return other
            }
        }
        return null
    }

    override fun advance(amount: Float, isPaused: Boolean) {
        super.advance(amount, isPaused)

        if (isPaused) return

        val data = getSharedData(ship)
        if (data.attractorLock != null) {
            data.attractorTarget = Vector2f(data.attractorLock.location)
        }
        if (getMoteScript()?.motes?.isNotEmpty() != true) {
            data.attractorLock = null
            data.attractorTarget = null
            data.attractorRemaining = 0f
        }
    }

    override fun getDisplayText(): String {
        return "Mote Strike"
    }

    override fun getExtraInfoText(): String {
        val mouseTarget = ship.mouseTarget
        val target = getLockTarget(mouseTarget)
        if (target == null) return "NO TARGET"

        val motes = getMoteScript()?.motes?.size ?: 0
        return "$motes motes"
    }
}