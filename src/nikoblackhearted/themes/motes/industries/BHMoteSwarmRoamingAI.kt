package nikoblackhearted.themes.motes.industries

import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import nikoblackhearted.BHBaseNikoScript
import nikoblackhearted.entities.Mote.Companion.translateTowardsAngle
import nikoblackhearted.entities.MoteSwarmEntityPlugin
import org.lazywizard.lazylib.MathUtils
import org.lazywizard.lazylib.VectorUtils
import org.lwjgl.util.vector.Vector2f
import kotlin.math.sign

class BHMoteSwarmRoamingAI(val swarm: MoteSwarmEntityPlugin, val source: SectorEntityToken): BHBaseNikoScript() {

    companion object {
        const val OUTER_BOUND = 9000f
        const val MIN_BUFFER = 500f
        const val TURNAROUND_MAX_MULT = 1.45f
        const val ATTACK_RANGE = 2000f
    }

    var turnRate = 20f

    override fun startImpl() {
        swarm.getEntityExternal()?.addScript(this)
    }

    override fun stopImpl() {
        swarm.getEntityExternal()?.removeScript(this)
    }

    override fun runWhilePaused(): Boolean = false

    lateinit var maneuverTarget: Vector2f
    init {
        retarget()
    }
    val retargetInterval = IntervalUtil(0.03f, 0.04f)
    override fun advance(amount: Float) {
        if (swarm.target != null) {
            delete()
            return
        }
        val days = Misc.getDays(amount)
        retargetInterval.advance(days)
        if (retargetInterval.intervalElapsed()) {
            retarget()
        }
        val entity = swarm.getEntityExternal() ?: return
        val facing = entity.facing

        var targetSign: Int
        val maneuverAngle = VectorUtils.getAngle(entity.location, maneuverTarget)
        if (facing > 180) {
            val inverted = facing - 180f // dont normalize this.
            val targetIsLeft = !(maneuverAngle < facing && maneuverAngle > inverted)
            targetSign = (if (targetIsLeft) 1 else -1)
        } else {
            val inverted = facing + 180f
            val targetIsLeft = maneuverAngle > facing && maneuverAngle < inverted
            targetSign = (if (targetIsLeft) 1 else -1)
        }
        val forceTurn = turnRate * targetSign

        entity.facing = (Misc.normalizeAngle(facing + (forceTurn * amount)))

        entity.location.translateTowardsAngle(
            entity.facing,
            100f * amount
        )

        scanForTargets()
    }

    private fun retarget() {
        val newLoc = MathUtils.getRandomPointInCircle(
            source.location,
            OUTER_BOUND + source.radius,
        )

        maneuverTarget = newLoc
    }

    private fun scanForTargets() {
        val entity = swarm.getEntityExternal() ?: return
        for (fleet in entity.containingLocation.fleets.shuffled()) {
            if (!fleet.faction.isHostileTo(entity.faction)) continue
            val dist = MathUtils.getDistance(fleet, entity)
            if (dist <= ATTACK_RANGE) {
                swarm.target = fleet
                delete()
                return
            }

        }
    }
}