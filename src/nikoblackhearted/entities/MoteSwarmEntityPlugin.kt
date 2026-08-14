package nikoblackhearted.entities

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CampaignEngineLayers
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.RepLevel
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.combat.ViewportAPI
import com.fs.starfarer.api.impl.campaign.BaseCustomEntityPlugin
import com.fs.starfarer.api.impl.campaign.CoreReputationPlugin
import com.fs.starfarer.api.impl.campaign.ids.MemFlags
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import nikoblackhearted.entities.Mote.Companion.translateTowardsAngle
import nikoblackhearted.themes.motes.industries.BHMoteSwarmRoamingAI.Companion.OUTER_BOUND
import org.lazywizard.lazylib.MathUtils
import org.lazywizard.lazylib.VectorUtils
import org.lwjgl.util.vector.Vector2f
import org.magiclib.kotlin.addHitGlow
import java.awt.Color
import kotlin.math.sign

class MoteSwarmEntityPlugin(): BaseCustomEntityPlugin() {

    companion object {
        const val MIN_BUFFER = 100f

        const val GLOBAL_SPEED_MULT = 2f
    }

    data class MoteSwarmParams(
        val numMotes: Int = 15,
        val baseColor: Color = Color(100,165,255,255),
        val spawnSound: String? = null,
        val source: SectorEntityToken? = null
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

        lateinit var maneuverTarget: Vector2f

        init {
            location = MathUtils.getRandomPointInCircle(
                Misc.ZERO,
                swarm.entity.radius - MIN_BUFFER
            )
            retarget()
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

            val newVel = Vector2f(fleet.velocity).scale(0.2f) as Vector2f
            fleet.setVelocity(newVel.x, newVel.y)

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

            if (swarm.params.source?.isPlayerFleet == true) {
                if (fleet.knowsWhoPlayerIs() && !fleet.memoryWithoutUpdate.getBoolean(MemFlags.MEMORY_KEY_NO_REP_IMPACT)) {
                    val impact = CoreReputationPlugin.CustomRepImpact()
                    impact.delta = -0.01f
                    impact.ensureAtWorst = RepLevel.HOSTILE

                    val action = CoreReputationPlugin.RepActionEnvelope(
                        CoreReputationPlugin.RepActions.CUSTOM, impact,
                        null, false
                    )
                    action.reason = "Change caused by esoteric particle impact"
                    Global.getSector().adjustPlayerReputation(
                        action,
                        fleet.faction.id
                    )
                }
            }

            delete()
        }

        fun delete() {
            swarm.removeMote(this)
        }

        var retargetInterval = IntervalUtil(0.03f, 0.04f)

        private fun retarget() {
            val newLoc = MathUtils.getRandomPointInCircle(
                swarm.entity.location,
                swarm.entity.radius,
            )

            maneuverTarget = newLoc
        }

        override fun doMovement(amount: Float) {

            if (retargetInterval == null) retargetInterval = IntervalUtil(0.03f, 0.04f)

            // TODO remove, for save compat 8/12/2026
            if (!this::maneuverTarget.isInitialized) {
                retarget()
            }

            val days = Misc.getDays(amount)
            retargetInterval.advance(days)
            if (retargetInterval.intervalElapsed()) {
                retarget()
            }

            var inRangeTarget: SectorEntityToken? = null
            if (target != null) {
                val dist = MathUtils.getDistance(swarm.entity, target)
                if (dist <= 0f) {
                    inRangeTarget = target
                }
            }

            val spatial = getSpatialLocation()

            var targetSign: Int
            val maneuverAngle = if (inRangeTarget != null) VectorUtils.getAngle(spatial, inRangeTarget.location) else VectorUtils.getAngle(spatial, maneuverTarget)
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
            var speedMult = 1f
            if (inRangeTarget != null) {
                val distTwo = MathUtils.getDistance(swarm.entity.location, target!!.location)
                if (distTwo <= 0.3f) speedMult = 3f
            }
            facing = (Misc.normalizeAngle(facing + (forceTurn * amount * speedMult)))
            location.translateTowardsAngle(
                facing,
                speed * amount * speedMult
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

        for (mote in motes.toList()) {
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
            if (target != null && MathUtils.getDistance(entity.location, target!!.location) <= 1f) {
                entity.setLocation(target!!.location.x, target!!.location.y)
            } else {
                val newLoc = Vector2f(entity.location).translateTowardsAngle(
                    angle,
                    300f * amount
                )
                entity.setLocation(newLoc.x, newLoc.y)
            }
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