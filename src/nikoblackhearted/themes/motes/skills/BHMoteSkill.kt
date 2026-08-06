package nikoblackhearted.themes.motes.skills

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.characters.LevelBasedEffect.ScopeDescription
import com.fs.starfarer.api.characters.ShipSkillEffect
import com.fs.starfarer.api.combat.BaseCombatLayeredRenderingPlugin
import com.fs.starfarer.api.combat.CombatEngineLayers
import com.fs.starfarer.api.combat.DamageType
import com.fs.starfarer.api.combat.EmpArcEntityAPI
import com.fs.starfarer.api.combat.MutableShipStatsAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.combat.ViewportAPI
import com.fs.starfarer.api.combat.listeners.HullDamageAboutToBeTakenListener
import com.fs.starfarer.api.impl.campaign.ids.HullMods
import com.fs.starfarer.api.util.FaderUtil
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import nikoblackhearted.themes.motes.BHMoteThemeIntel
import org.lwjgl.util.vector.Vector2f
import java.awt.Color
import java.util.EnumSet

class BHMoteSkill {

    companion object {
        const val FLAGSHIP_DAMAGE_MULT_PER_STACK = 0.2f
        const val FLAGSHIP_SPEED_MULT_PER_STACK = 0.1f
    }

    class Level1: ShipSkillEffect {
        override fun apply(
            stats: MutableShipStatsAPI?,
            hullSize: ShipAPI.HullSize?,
            id: String?,
            level: Float
        ) {
            if (stats == null) return

            val intel = BHMoteThemeIntel.get() ?: return
            val stacks = intel.currSongStacks
            if (stacks <= 0) return

            val mult = 1f + (FLAGSHIP_DAMAGE_MULT_PER_STACK * stacks)
            val speedMult = 1f + (FLAGSHIP_SPEED_MULT_PER_STACK * stacks)

            stats.ballisticWeaponDamageMult.modifyMult(id, mult, "Screaming Songs")
            stats.energyWeaponDamageMult.modifyMult(id, mult, "Screaming Songs")
            stats.missileWeaponDamageMult.modifyMult(id, mult, "Screaming Songs")

            stats.maxSpeed.modifyMult(id, speedMult, "Screaming Songs")
            stats.acceleration.modifyMult(id, speedMult, "Screaming Songs")
            stats.deceleration.modifyMult(id, speedMult, "Screaming Songs")
            stats.turnAcceleration.modifyMult(id, speedMult, "Screaming Songs")
            stats.maxTurnRate.modifyMult(id, speedMult, "Screaming Songs")
        }

        override fun unapply(
            stats: MutableShipStatsAPI?,
            hullSize: ShipAPI.HullSize?,
            id: String?
        ) {
            if (stats == null) return

            stats.ballisticWeaponDamageMult.unmodify(id)
            stats.energyWeaponDamageMult.unmodify(id)
            stats.missileWeaponDamageMult.unmodify(id)
            stats.maxSpeed.unmodify(id)
            stats.acceleration.unmodify(id)
            stats.deceleration.unmodify(id)
            stats.turnAcceleration.unmodify(id)
            stats.maxTurnRate.unmodify(id)
        }

        override fun getEffectDescription(level: Float): String {
            return "+${(FLAGSHIP_DAMAGE_MULT_PER_STACK * 100f).toInt()}%/${(FLAGSHIP_SPEED_MULT_PER_STACK * 100f).toInt()}% damage dealt/engine performance per Screaming Song stack"
        }

        override fun getEffectPerLevelDescription(): String? {
            return null
        }

        override fun getScopeDescription(): ScopeDescription {
            return ScopeDescription.PILOTED_SHIP
        }
    }

    class Level2: ShipSkillEffect {
        override fun apply(
            stats: MutableShipStatsAPI?,
            hullSize: ShipAPI.HullSize?,
            id: String?,
            level: Float
        ) {
            if (stats == null) return

            if (BHMoteThemeIntel.get()?.rebirthActive != true) return

            var ship = stats.entity
            if (ship is ShipAPI) {
                if (!ship.hasListenerOfClass(BHSecondLifeListener::class.java)) {
                    var listener = BHSecondLifeListener(ship)
                    ship.addListener(listener)
                    Global.getCombatEngine()?.addLayeredRenderingPlugin(listener)
                }
            }
        }

        override fun unapply(
            stats: MutableShipStatsAPI?,
            hullSize: ShipAPI.HullSize?,
            id: String?
        ) {
            return
        }

        override fun getEffectDescription(level: Float): String {
            return "If unlocked, enables one respawn per fight"
        }

        override fun getEffectPerLevelDescription(): String? {
            return null
        }

        override fun getScopeDescription(): ScopeDescription {
            return ScopeDescription.PILOTED_SHIP
        }
    }

    class BHSecondLifeListener(var ship: ShipAPI) : BaseCombatLayeredRenderingPlugin(),
        HullDamageAboutToBeTakenListener {

        enum class Stage(val time: Float) {
            SLOWDOWN(1.5f),
            INTERMISSION(0.5f),
            DRAIN(6f),
            OUT(1f),
            DONE(100f);
        }
        var stage = Stage.SLOWDOWN
        var interval = IntervalUtil(stage.time, stage.time)

        var triggered = false
        var done = false

        init {
            Global.getSettings().loadTexture("graphics/BHBlackScreen.png")
            Global.getSettings().loadTexture("graphics/BHVignette.png")
        }

        var black = Global.getSettings().getSprite("graphics/BHBlackScreen.png")
        var vignette = Global.getSettings().getSprite("graphics/BHVignette.png")

        override fun notifyAboutToTakeHullDamage(param: Any?, ship: ShipAPI?, point: Vector2f?, damageAmount: Float): Boolean {
            if (ship != Global.getCombatEngine().playerShip) return false

            if (ship.variant.hasHullMod(HullMods.PHASE_ANCHOR)) return false

            var custom = Global.getCombatEngine().customData
            if (triggered) return true
            if ("BHMoteRebirthDone" in custom) return false

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

                /*Global.getSoundPlayer().playSound("rat_bloodstream_trigger", 1f, 2f, ship.location, ship.velocity)
                Global.getSoundPlayer().playSound("system_entropy", 1f, 1.5f, ship.location, ship.velocity)
                Global.getSoundPlayer().playSound("explosion_ship", 1f, 1f, ship.location, ship.velocity)*/
                Global.getSoundPlayer().playSound("mote_attractor_targeted_ship", 0.6f, 1f, ship.location, ship.velocity)
            }

            if (triggered) {
                return true
            }

            return false
        }

        override fun advance(amount: Float) {
            if (!triggered) return

            val engine = Global.getCombatEngine()
            val time = engine.timeMult.modified
            val finalAmount = if (time == 0f) 0.1f else (amount / time)
            interval.advance(finalAmount)
            if (interval.intervalElapsed()) {
                stage = Stage.entries[stage.ordinal + 1]
                interval = IntervalUtil(stage.time, stage.time)
            }

            when (stage) {
                Stage.SLOWDOWN -> {
                    val progress = interval.elapsed / interval.intervalDuration
                    engine.timeMult.modifyMult(this.toString(), (1f - (progress)).coerceAtLeast(0.01f))
                    black.alphaMult = ((progress * 0.7f))
                    vignette.alphaMult = ((progress))
                }
                Stage.INTERMISSION -> {
                    return
                }
                Stage.DRAIN -> {
                    doDrain(finalAmount)
                }
                Stage.OUT -> {
                    val progress = interval.elapsed / interval.intervalDuration
                    engine.timeMult.modifyMult(this.toString(), progress)
                    black.alphaMult = (1f - progress).coerceAtMost(0.8f)
                    vignette.alphaMult = (1f - progress)
                }
                Stage.DONE -> {
                    ship.removeListener(this)
                    done = true
                    return
                }
            }

        }

        override fun isExpired(): Boolean {
            return done
        }

        private fun doDrain(amount: Float) {
            if (ship.hitpoints >= ship.maxHitpoints) {
                stage = Stage.OUT
                interval = IntervalUtil(stage.time, stage.time)
                return
            }
            val targets = getTargets()
            if (targets.isEmpty()) return

            for (target in targets) {
                drain(target, amount)
            }
        }

        val drainArcInterval = IntervalUtil(0.1f, 0.12f)
        private fun drain(target: ShipAPI, amount: Float) {
            val params = EmpArcEntityAPI.EmpArcParams()
            params.segmentLengthMult = 8f
            params.zigZagReductionFactor = 0.15f

            params.brightSpotFullFraction = 0.5f
            params.brightSpotFadeFraction = 0.5f

            params.flickerRateMult = 0.1f

            drainArcInterval.advance(amount)
            val ourSource = ship.exactBounds.segments.randomOrNull()?.p2 ?: ship.location
            var theirSource = target.allWeapons.randomOrNull()?.slot?.computePosition(target) ?: target.location
            //val theirSource = target.location
            if (drainArcInterval.intervalElapsed()) {
                val arc = Global.getCombatEngine().spawnEmpArcVisual(
                    theirSource ?: target.location,
                    target,
                    ourSource,
                    ship,
                    2f,
                    BHMoteThemeIntel.color.brighter(),
                    BHMoteThemeIntel.color,
                    params
                )
                arc.setSingleFlickerMode(true)
                Global.getSoundPlayer().playSound(
                    "mote_attractor_launch_mote",
                    1f,
                    0.2f,
                    target.location,
                    Misc.ZERO
                )
            }

            val subtracted = 1000f * amount
            val old = target.hitpoints
            val hpIncr = (old - (target.hitpoints - subtracted))
            //target.hitpoints = (target.hitpoints - subtracted).coerceAtLeast(1f)
            val ourOld = ship.hitpoints
            ship.hitpoints = (ship.hitpoints + hpIncr).coerceAtMost(ship.maxHitpoints).coerceAtLeast(1f)
            val armorIncr = (ship.hitpoints - ourOld) * 0.5f
            val cell = ship.armorGrid.getCellAtLocation(ourSource)
            if (cell != null) {
                ship.armorGrid.setArmorValue(cell[0], cell[1], (ship.armorGrid.getArmorValue(cell[0], cell[1]) + armorIncr))
            }

            Global.getCombatEngine().applyDamage(
                target,
                theirSource,
                subtracted,
                DamageType.ENERGY,
                subtracted * 5f,
                true,
                false,
                ship,
                false
            )
        }

        private fun getTargets(): HashSet<ShipAPI> {
            val engine = Global.getCombatEngine()
            val grid = engine.shipGrid.getCheckIterator(ship.location, 2000f, 2000f)
            val enemies = HashSet<ShipAPI>()
            val allies = HashSet<ShipAPI>()
            while (grid.hasNext()) {
                val ship = grid.next() as ShipAPI
                if (ship.isFighter || ship.isHulk) continue
                if (ship.owner == 0) allies += ship else enemies += ship
            }

            val listToUse = if (enemies.isEmpty()) allies else enemies
            return listToUse
        }

        override fun getActiveLayers(): EnumSet<CombatEngineLayers> {
            return EnumSet.of(CombatEngineLayers.JUST_BELOW_WIDGETS, CombatEngineLayers.BELOW_PLANETS)
        }

        override fun getRenderRadius(): Float {
            return 1000000f
        }

        override fun render(layer: CombatEngineLayers?, viewport: ViewportAPI?) {
            if (layer == null || viewport == null) return

            if (triggered) {

                if (layer == CombatEngineLayers.BELOW_PLANETS) {
                    black.color = Color(0, 0, 0)
                    black.setSize(viewport.visibleWidth, viewport.visibleHeight)
                    black.render(viewport.llx, viewport.lly)
                }

                if (layer == CombatEngineLayers.JUST_BELOW_WIDGETS) {
                    vignette.color = Color(40, 0, 80)

                    val offset = 10
                    vignette.setSize(viewport.visibleWidth + offset, viewport.visibleHeight + offset)
                    vignette.render(viewport.llx - (offset * 0.5f), viewport.lly - (offset * 0.5f))
                }
            }
        }
    }
}