package nikoblackhearted.themes.motes.skills

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.characters.FleetTotalItem
import com.fs.starfarer.api.characters.LevelBasedEffect
import com.fs.starfarer.api.characters.ShipSkillEffect
import com.fs.starfarer.api.combat.BaseCombatLayeredRenderingPlugin
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin
import com.fs.starfarer.api.combat.CombatEngineLayers
import com.fs.starfarer.api.combat.CombatEntityAPI
import com.fs.starfarer.api.combat.DamageType
import com.fs.starfarer.api.combat.MutableShipStatsAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.combat.ViewportAPI
import com.fs.starfarer.api.combat.listeners.HullDamageAboutToBeTakenListener
import com.fs.starfarer.api.impl.campaign.ids.HullMods
import com.fs.starfarer.api.impl.campaign.ids.Personalities.RECKLESS
import com.fs.starfarer.api.input.InputEventAPI
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import nikoblackhearted.BHSettings
import nikoblackhearted.themes.motes.BHMoteThemeIntel
import org.dark.shaders.distortion.DistortionShader
import org.dark.shaders.distortion.RippleDistortion
import org.lazywizard.lazylib.MathUtils
import org.lazywizard.lazylib.VectorUtils
import org.lazywizard.lazylib.combat.CombatUtils.applyForce
import org.lwjgl.util.vector.Vector2f
import java.awt.Color
import java.util.EnumSet
import kotlin.collections.contains

class BHMotesCrusaderSkill {
    class Level1: ShipSkillEffect {
        override fun apply(
            stats: MutableShipStatsAPI?,
            hullSize: ShipAPI.HullSize?,
            id: String?,
            level: Float
        ) {
            val ship = stats?.entity as? ShipAPI ?: return
            if (!ship.hasListenerOfClass(CrusaderSecondLifeListener::class.java)) {
                var listener = CrusaderSecondLifeListener(ship)
                ship.addListener(listener)
            }
        }

        override fun unapply(
            stats: MutableShipStatsAPI?,
            hullSize: ShipAPI.HullSize?,
            id: String?
        ) {
            val ship = stats?.entity as? ShipAPI ?: return
            ship.removeListenerOfClass(CrusaderSecondLifeListener::class.java)
        }

        override fun getEffectDescription(level: Float): String {
            return "a"
        }

        override fun getEffectPerLevelDescription(): String? {
            return null
        }

        override fun getScopeDescription(): LevelBasedEffect.ScopeDescription {
            return LevelBasedEffect.ScopeDescription.PILOTED_SHIP
        }
    }

    class CrusaderSecondLifeListener(var ship: ShipAPI): HullDamageAboutToBeTakenListener {

        var triggered = false

        override fun notifyAboutToTakeHullDamage(
            param: Any?,
            ship: ShipAPI?,
            point: Vector2f?,
            damageAmount: Float
        ): Boolean {
            if (ship == null) return false
            if (ship.variant.hasHullMod(HullMods.PHASE_ANCHOR)) return false

            var custom = ship.customData
            if (triggered) return true
            if ("BHCrusaderRespawn" in custom) return false

            if (ship.hitpoints - damageAmount <= 0) {
                ship.hitpoints = 10f
                triggered = true

                for (weapon in ship.allWeapons) {
                    weapon.repair()
                }
                ship.fluxTracker.stopOverload()
                ship.fluxTracker.currFlux = 0f
                ship.fluxTracker.hardFlux = 0f
                ship.engineController.shipEngines.forEach { it.repair(); it.hitpoints = it.maxHitpoints }

                ship.setCustomData("BHCrusaderRespawn", true)

                val script = CrusaderSecondLifeScript(ship, this)
                Global.getCombatEngine().addPlugin(script)

                /*Global.getSoundPlayer().playSound("rat_bloodstream_trigger", 1f, 2f, ship.location, ship.velocity)
                Global.getSoundPlayer().playSound("system_entropy", 1f, 1.5f, ship.location, ship.velocity)
                Global.getSoundPlayer().playSound("explosion_ship", 1f, 1f, ship.location, ship.velocity)*/
                //Global.getSoundPlayer().playSound("mote_attractor_targeted_ship", 0.6f, 1f, ship.location, ship.velocity)
            }

            if (triggered) {
                return true
            }

            return false
        }
    }

    class CrusaderSecondLifeScript(val ship: ShipAPI, val listener: CrusaderSecondLifeListener): BaseEveryFrameCombatPlugin() {

        companion object {
            val color = Color(239, 191, 4, 255)

            fun pushAwayEntities(focus: ShipAPI, force: Float, minRange: Float, maxRange: Float, friendlyToo: Boolean = true, flameout: Boolean = false) {
                val engine = Global.getCombatEngine()

                for (iter in engine.shipGrid.getCheckIterator(focus.location, maxRange, maxRange)) {
                    val entity = iter as? CombatEntityAPI ?: return
                    if (!friendlyToo && entity.owner == focus.owner) continue

                    val dist = Misc.getDistance(focus.location, entity.location)
                    val effectDist = (dist - minRange).coerceAtMost(0f)
                    val effectMaxRange = maxRange - minRange

                    val adjustedDist = (dist - minRange).coerceAtMost(0f)

                    var effectMult = 1 - (1 / (maxRange / adjustedDist)).coerceAtMost(1f)
                    if (effectMult <= 0f) continue

                    /*val pushDir = VectorUtils.getDirectionalVector(focus.location, iter.location)
                    pushDir.scale(force * effectMult)

                    val vel = entity.velocity.length()
                    if (vel > 100f) {
                        // too fast! slow down
                        entity.velocity.scale((0.4f / effectMult).coerceAtMost(1f))
                    }

                    Vector2f.add(pushDir, entity.velocity, entity.velocity)*/

                    val oldMass = entity.mass
                    entity.mass = oldMass.coerceAtMost(1250f) // we want to shove capitals away
                    applyForce(entity, VectorUtils.getDirectionalVector(focus.location, iter.location), (force * effectMult))
                    entity.mass = oldMass
                    if (flameout && entity is ShipAPI && entity.owner != focus.owner) {
                        val engines = entity.engineController
                        val percentOfEnginesToFlameOut = (0.3f * effectMult)
                        if (percentOfEnginesToFlameOut <= 0.05f) continue
                        val totalEngines = engines.shipEngines.size
                        for (engine in engines.shipEngines.shuffled()) {
                            if ((engines.computeDisabledFraction()) >= percentOfEnginesToFlameOut) break
                            engine.disable()
                        }
                    }
                }

                for (iter in engine.asteroidGrid.getCheckIterator(focus.location, maxRange, maxRange)) {
                    val entity = iter as? CombatEntityAPI ?: return
                    if (!friendlyToo && entity.owner == focus.owner) continue

                    val dist = Misc.getDistance(focus.location, entity.location)
                    val effectDist = (dist - minRange).coerceAtMost(0f)
                    val effectMaxRange = maxRange - minRange

                    val adjustedDist = (dist - minRange).coerceAtMost(0f)

                    var effectMult = 1 - (1 / (maxRange / adjustedDist))
                    if (effectMult <= 0f) continue

                    applyForce(entity, VectorUtils.getDirectionalVector(focus.location, iter.location), (force * effectMult))
                }

                for (projectile in engine.projectiles.filter { Misc.getDistance(focus.location, it.location) <= (minRange * 1.3f)}) {
                    if (projectile.owner == focus.owner) continue

                    engine.removeEntity(projectile) // gone
                }

                if (BHSettings.graphicsLibEnabled) {
                    val ripple = RippleDistortion(focus.location, Misc.ZERO)
                    ripple.intensity = 400f
                    ripple.size = maxRange * 1f
                    ripple.fadeInSize(1.4f)
                    ripple.fadeOutIntensity(0.4f)

                    DistortionShader.addDistortion(ripple)
                }
            }
        }

        enum class Stage(val time: Float) {
            BUILDUP(3f),
            DONE(100f);
        }

        var stage = Stage.BUILDUP
        var interval = IntervalUtil(stage.time, stage.time)

        var done = false

        override fun advance(amount: Float, events: MutableList<InputEventAPI>) {
            val engine = Global.getCombatEngine()
            if (engine.isPaused) return
            if (ship.isHulk || !engine.isEntityInPlay(ship)) {
                engine.removePlugin(this)
                return
            }

            val progress = if (done) 1f else interval.elapsed / interval.intervalDuration

            ship.setJitter(
                this,
                color,
                2f * progress,
                1,
                2f
            )
            ship.isJitterShields = true

            Global.getSoundPlayer().playLoop(
                "system_damper_loop",
                ship,
                0.8f * progress,
                1.5f,
                ship.location,
                Misc.ZERO
            )

            if (!done) {
                interval.advance(amount)
                if (interval.intervalElapsed()) {
                    stage = Stage.entries[stage.ordinal + 1]
                    interval = IntervalUtil(stage.time, stage.time)
                }

                when (stage) {
                    Stage.BUILDUP -> {
                        ship.isHoldFireOneFrame = true
                        ship.velocity.set(0f, 0f)
                    }

                    Stage.DONE -> {
                        pushAwayEntities(
                            ship,
                            2500f,
                            ship.collisionRadius * 4f,
                            ship.collisionRadius * 10f,
                            flameout = true
                        )

                        Global.getSoundPlayer().playSound(
                            "system_nova_burst_explosion",
                            1.1f,
                            2f,
                            ship.location,
                            Misc.ZERO
                        )

                        ship.hitpoints = (ship.maxHitpoints)
                        ship.mutableStats.ballisticRoFMult.modifyMult("BHMoteCrusaderRespawn", 2f)
                        ship.mutableStats.maxSpeed.modifyMult("BHMoteCrusaderRespawn", 3f)
                        ship.mutableStats.acceleration.modifyMult("BHMoteCrusaderRespawn", 3f)
                        ship.mutableStats.deceleration.modifyMult("BHMoteCrusaderRespawn", 3f)
                        ship.mutableStats.turnAcceleration.modifyMult("BHMoteCrusaderRespawn", 3f)
                        ship.mutableStats.maxTurnRate.modifyMult("BHMoteCrusaderRespawn", 3f)
                        ship.captain.setPersonality(RECKLESS)
                        ship.removeListener(listener)
                        done = true
                        return
                    }
                }
            }
        }
    }
}