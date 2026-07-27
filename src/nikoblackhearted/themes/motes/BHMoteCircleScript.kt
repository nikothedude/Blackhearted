package nikoblackhearted.themes.motes

import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.impl.campaign.ids.Factions
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import nikoblackhearted.BHBaseNikoScript
import nikoblackhearted.entities.MoteSwarmEntityPlugin
import org.lazywizard.lazylib.MathUtils
import org.lazywizard.lazylib.VectorUtils

class BHMoteCircleScript(
    val entity: SectorEntityToken,
    val orbitRadius: Float,
    val moteNumOverride: Int? = null,
    val respawnDelayMult: Float = 1f,
    val paramsOverride: MoteSwarmEntityPlugin.MoteSwarmParams? = null,
    val radiusLower: Float = 60f,
    val radiusUpper: Float = 80f,
): BHBaseNikoScript() {

    var swarms = HashSet<MoteSwarmOrbit>()

    init {
        addOrRemoveSwarms()
    }

    class MoteSwarmOrbit(
        val swarm: MoteSwarmEntityPlugin,
        var angle: Float,
        val orbitFocus: SectorEntityToken,
        val backwards: Boolean,
        val speed: Float
    ) {
        companion object {
            const val BASE_SPEED = 10f
            const val ORBIT_SIZE_SPEED_DIVISOR = 500f
        }

        fun advance(amount: Float, orbitSize: Float) {
            angle = (Misc.normalizeAngle(angle + getAngleIncr(amount, orbitSize)))
            val entity = swarm.getEntityExternal() ?: return
            val newLoc = MathUtils.getPointOnCircumference(
                orbitFocus.location,
                orbitSize,
                angle
            )
            entity.setLocation(newLoc.x, newLoc.y)
        }

        fun getAngleIncr(amount: Float, orbitSize: Float): Float {
            val sign = if (backwards) -1f else 1f
            return (speed * amount) / (orbitSize / ORBIT_SIZE_SPEED_DIVISOR) * sign
        }
    }

    private fun addOrRemoveSwarms() {
        val idealSwarms = getMaxSwarms()
        while (swarms.size != idealSwarms) {
            if (swarms.size < idealSwarms) {
                val params = paramsOverride ?: MoteSwarmEntityPlugin.MoteSwarmParams(
                    1,
                )
                val swarm = entity.containingLocation.addCustomEntity(
                    null,
                    null,
                    "BH_moteSwarm",
                    entity.faction.id,
                    MathUtils.getRandomNumberInRange(radiusLower, radiusUpper),
                    0f,
                    0f,
                    params
                )
                val target = MathUtils.getRandomPointOnCircumference(
                    entity.location,
                    getDistForCircumference()
                )
                swarm.setLocation(target.x, target.y)
                val data = MoteSwarmOrbit(
                    swarm.customPlugin as MoteSwarmEntityPlugin,
                    VectorUtils.getAngle(
                        entity.location,
                        swarm.location
                    ),
                    entity,
                    MathUtils.getRandom().nextFloat() >= 0.5f,
                    MoteSwarmOrbit.BASE_SPEED * MathUtils.getRandomNumberInRange(0.9f, 1.1f)
                )
                swarms += data
            } else {
                val rand = swarms.randomOrNull() ?: return
                rand.swarm.delete()
                swarms -= rand
            }
        }
    }

    private fun getDistForCircumference(): Float {
        return entity.radius + orbitRadius
    }

    val respawnDelay = IntervalUtil(1f, 1.1f)
    var motesLost = 0

    private fun getMaxSwarms(): Int {
        val base = moteNumOverride ?: 50
        return base - motesLost
    }

    override fun startImpl() {
        entity.addScript(this)
    }

    override fun stopImpl() {
        entity.removeScript(this)
    }

    override fun runWhilePaused(): Boolean = false

    override fun advance(amount: Float) {
        for (swarm in swarms) {
            swarm.advance(amount, orbitRadius + entity.radius)
        }

        respawnDelay.advance(Misc.getDays(amount * respawnDelayMult))
        if (respawnDelay.intervalElapsed()) {
            motesLost = (motesLost--).coerceAtLeast(0)
        }
    }

}