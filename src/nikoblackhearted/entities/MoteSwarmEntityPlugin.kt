package nikoblackhearted.entities

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CampaignEngineLayers
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.combat.ViewportAPI
import com.fs.starfarer.api.graphics.SpriteAPI
import com.fs.starfarer.api.impl.campaign.BaseCustomEntityPlugin
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import nikoblackhearted.entities.Mote.Companion.translateTowardsAngle
import org.lazywizard.lazylib.MathUtils
import org.lazywizard.lazylib.VectorUtils
import org.lwjgl.opengl.GL11
import org.lwjgl.util.vector.Vector2f
import org.magiclib.kotlin.addHitGlow
import org.magiclib.kotlin.fadeAndExpire
import java.awt.Color
import java.util.HashSet
import kotlin.math.cos
import kotlin.math.sign
import kotlin.math.sin

class MoteSwarmEntityPlugin(): BaseCustomEntityPlugin() {

    companion object {
        const val MIN_BUFFER = 100f
        const val TURNAROUND_MAX_MULt = 1.45f

        const val GLOBAL_SPEED_MULT = 2f
    }

    data class MoteSwarmParams(
        val numMotes: Int = 15,
        val baseColor: Color = Color(100,165,255,255),
        val spawnSound: String? = null
    )

    var fadingOut = false
    var fadeTime = 0f
    val motes = HashSet<SwarmingMote>()
    var target: SectorEntityToken? = null
        set(value) {
            motes.forEach { it.target = value }
            field = value
        }
    val pursuitTime = IntervalUtil(7f, 7.2f)
    class SwarmingMote(val swarm: MoteSwarmEntityPlugin, color: Color): Mote(color) {
        val speed = 30f * GLOBAL_SPEED_MULT
        // if near the edge of the radius, scale turnrate based on prox using a buffer var
        var turnRate: Float = 70f * GLOBAL_SPEED_MULT
        var facing: Float = MathUtils.getRandomNumberInRange(0f, 360f)
        var target: SectorEntityToken? = null

        init {
            location = MathUtils.getRandomPointInCircle(
                Misc.ZERO,
                swarm.entity.radius - MIN_BUFFER
            )
        }

        val collisionInterval = IntervalUtil(0.1f, 0.11f)
        override fun advance(amount: Float) {
            if (!swarm.entity.containingLocation.isCurrentLocation) return
            super.advance(amount)
            collisionInterval.advance(amount)
            if (collisionInterval.intervalElapsed()) {
                checkCollision()
            }
        }

        private fun checkCollision() {
            if (swarm.fadeTime != 0f) return
             for (fleet in swarm.entity.containingLocation.fleets) {
                if (fleet != target) {
                    if (swarm.entity.faction != null && !fleet.faction.isHostileTo(swarm.entity.faction)) {
                        continue
                    }
                }

                val loc = getSpatialLocation()
                val dist = MathUtils.getDistance(fleet, loc)
                if (dist > 0f) continue

                impact(fleet)
            }
        }

        fun impact(fleet: CampaignFleetAPI) {
            val loc = getSpatialLocation()
            swarm.entity.containingLocation.addHitGlow(
                loc,
                Misc.ZERO,
                spriteOne.height * 1.5f,
                1f,
                color
            )
            Global.getSoundPlayer().playSound(
                "mote_attractor_impact_normal",
                1f,
                1f,
                loc,
                Misc.ZERO
            )

            fleet.stats.addTemporaryModFlat(
                1f,
                "${this}_1",
                -1f,
                fleet.stats.fleetwideMaxBurnMod
            )
            fleet.stats.addTemporaryModFlat(
                1f,
                "${this}_2",
                -200f,
                fleet.stats.sensorRangeMod
            )
            fleet.stats.addTemporaryModFlat(
                5f,
                "${this}_3",
                200f,
                fleet.stats.detectedRangeMod
            )

            for (member in fleet.views) {
                member.setJitter(
                    0.1f,
                    1f,
                    color,
                    3,
                    5f
                )
            }

            delete()
        }

        fun delete() {
            swarm.removeMote(this)
        }

        override fun doMovement(amount: Float) {
            var inRangeTarget: SectorEntityToken? = null
            if (target != null) {
                val dist = MathUtils.getDistance(swarm.entity, target)
                if (dist <= 0f) {
                    inRangeTarget = target
                }
            }

            var incr = if (inRangeTarget != null) 0f else (turnRate) * sign(MathUtils.getRandom().nextFloat() - 0.5f)
            incr *= MathUtils.getRandomNumberInRange(0.7f, 1.3f)

            val spatial = getSpatialLocation()

            val dist = MathUtils.getDistance(
                spatial,
                swarm.entity.location
            )
            //val externalDist = (dist - swarm.entity.radius).coerceAtLeast(0f)
            val distFromRadius = (swarm.entity.radius - dist)

            val bufferMult = ((distFromRadius) / MIN_BUFFER).coerceAtMost(1f).coerceAtLeast(0f)
            if (bufferMult < 1f || inRangeTarget != null) {
                var targetSign: Int
                val baseTurn = if (inRangeTarget == null) ((turnRate * TURNAROUND_MAX_MULt) * (1 - bufferMult)) else turnRate
                val maneuverTarget = if (inRangeTarget != null) VectorUtils.getAngle(spatial, inRangeTarget!!.location) else VectorUtils.getAngle(spatial, swarm.entity.location)
                if (facing > 180) {
                    val inverted = facing - 180f // dont normalize this.
                    val targetIsLeft = !(maneuverTarget < facing && maneuverTarget > inverted)
                    targetSign = (if (targetIsLeft) 1 else -1)
                } else {
                    val inverted = facing + 180f
                    val targetIsLeft = maneuverTarget > facing && maneuverTarget < inverted
                    targetSign = (if (targetIsLeft) 1 else -1)
                }
                val forceTurn = baseTurn * targetSign
                incr += forceTurn
            }

            facing = (Misc.normalizeAngle(facing + (incr * amount)))

            location.translateTowardsAngle(
                facing,
                speed * amount
            )

        }

        override fun getSpatialLocation(): Vector2f = Vector2f(location).translate(swarm.entity.location.x, swarm.entity.location.y)
        override fun getAlphaMult(): Float {
            if (swarm.fadeTime == 0f) return 1f
            if (swarm.fadingOut) {
                return (swarm.fadeTime)
            }
            return 1f - (swarm.fadeTime)
        }
    }

    private fun removeMote(mote: SwarmingMote) {
        motes -= mote
        if (motes.isEmpty()) {
            delete()
        }
    }

    lateinit var params: MoteSwarmParams
    override fun init(entity: SectorEntityToken?, pluginParams: Any?) {
        super.init(entity, pluginParams)

        var pluginParams = pluginParams
        if (pluginParams !is MoteSwarmParams) {
            pluginParams = MoteSwarmParams()
        }

        fadeTime = 1f

        params = pluginParams

        var motesLeft = params.numMotes
        while (motesLeft-- > 0) {
            motes += SwarmingMote(this, params.baseColor)
        }
    }

    var didSound = false
    override fun advance(amount: Float) {
        super.advance(amount)

        if (!didSound && params.spawnSound != null) {
            Global.getSoundPlayer().playSound(
                params.spawnSound,
                1f,
                1f,
                entity?.location,
                Misc.ZERO
            )
            didSound = true
        }

        for (mote in motes) {
            mote.advance(amount)
        }

        if (target != null) {
            pursuitTime.advance(Misc.getDays(amount))
            if (pursuitTime.intervalElapsed()) {
                if (fadeTime == 0f) {
                    fadeOut()
                }
            }

            val angle = VectorUtils.getAngle(entity.location, target!!.location)
            entity.facing = angle
            val newLoc = Vector2f(entity.location).translateTowardsAngle(
                angle,
                300f * amount
            )
            entity.setLocation(newLoc.x, newLoc.y)
        }

        if (fadeTime > 0f) {
            fadeTime = (fadeTime - amount).coerceAtLeast(0f)
            if (fadeTime == 0f && fadingOut) {
                delete()
                return
            }
        }
    }

    override fun render(layer: CampaignEngineLayers?, viewport: ViewportAPI?) {
        super.render(layer, viewport)

        if (viewport == null || !viewport.isNearViewport(
                entity.location,
                entity.radius + 500f
            )) {
            return
        }

        for (mote in motes) {
            mote.render()
        }
    }

    fun delete() {
        entity.containingLocation.removeEntity(entity)
    }

    fun fadeOut() {
        fadingOut = true
        fadeTime = 1f
    }

    fun getEntityExternal(): SectorEntityToken? {
        return entity!!
    }

}