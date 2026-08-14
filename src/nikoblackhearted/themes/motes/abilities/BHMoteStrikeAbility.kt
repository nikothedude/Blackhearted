package nikoblackhearted.themes.motes.abilities

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.impl.campaign.abilities.BaseDurationAbility
import com.fs.starfarer.api.impl.campaign.abilities.BaseToggleAbility
import com.fs.starfarer.api.impl.campaign.abilities.GoDarkAbility
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import nikoblackhearted.BHDelayedExecution
import nikoblackhearted.themes.motes.BHMoteCircleScript
import nikoblackhearted.themes.motes.BHMoteThemeIntel
import org.lazywizard.lazylib.MathUtils
import org.lwjgl.util.vector.Vector2f
import java.awt.Color

class BHMoteStrikeAbility: BaseDurationAbility() {

    companion object {
        const val MAX_RANGE = 2000f
    }
    override fun applyEffect(amount: Float, level: Float) {
        return
    }

    fun canFire(): Boolean {
        val ring = getRing() ?: return false
        return (ring.swarms.isNotEmpty())
    }

    private fun getRing(): BHMoteCircleScript? {
        val ability = getAbility() ?: return null
        return ability.script
    }

    private fun getAbility(): BHMoteRingAbility? {
        return (fleet.getAbility("BH_moteRing")) as? BHMoteRingAbility
    }

    override fun pressButton() {
        if (isUsable && !turnedOn) {
            if (fleet.isPlayerFleet) {
                if (BHMoteStrikeKeypressListener.get(false)?.active != true) {
                    if (!canFire()) {
                        Global.getSector().campaignUI.messageDisplay.addMessage(
                            "No motes to launch",
                            Misc.getNegativeHighlightColor()
                        )
                        Global.getSoundPlayer().playUISound("ui_button_disabled_pressed", 1f, 1f)
                        return
                    }

                    val soundId = onSoundUI
                    if (soundId != null) {
                        if (PLAY_UI_SOUNDS_IN_WORLD_SOURCES) {
                            Global.getSoundPlayer()
                                .playSound(soundId, 1f, 1f, Global.getSoundPlayer().listenerPos, Vector2f())
                        } else {
                            Global.getSoundPlayer().playUISound(soundId, 0.7f, 1f)
                        }
                    }

                    class DelayedScript : BHDelayedExecution(
                        IntervalUtil(0f, 0f),
                        useDays = false,
                        runIfPaused = true
                    ) {
                        override fun executeImpl() {
                            BHMoteStrikeKeypressListener.get(true)!!.activate(this@BHMoteStrikeAbility)
                            Global.getSector().campaignUI.messageDisplay.addMessage(
                                "Click a fleet to launch a mote",
                                Misc.getHighlightColor()
                            )
                        }
                    }

                    DelayedScript().start()
                } else {
                    BHMoteStrikeKeypressListener.get(false)?.deactivate(true)
                }
            }
        }
    }

    var currTarget: SectorEntityToken? = null
    override fun activateImpl() {
        if (currTarget == null) return
        val ring = getRing() ?: return
        val rand = ring.swarms.randomOrNull() ?: return
        // TODO emp arc here. yes, campaign emp arc.
        rand.swarm.target = currTarget

        Global.getSoundPlayer().playSound(
            "mote_attractor_targeted_ship",
            1f,
            1f,
            fleet.location,
            Misc.ZERO
        )

        ring.removeSwarm(rand, true)

        fleet.views.forEach { it.setJitter(
            0.5f,
            0.5f,
            rand.swarm.motes.randomOrNull()?.color ?: Color.RED,
            3,
            3f
        ) }
        if (currTarget is CampaignFleetAPI) {
            (currTarget as CampaignFleetAPI).views.forEach {
                it.setJitter(
                    0.5f,
                    0.5f,
                    rand.swarm.motes.randomOrNull()?.color ?: Color.RED,
                    3,
                    3f
                )
            }
        }
    }

    fun forceActivation() {
        activateImpl()
    }

    override fun deactivateImpl() {
        cleanupImpl()
        fleet.stats.detectedRangeMod.unmodify(spec.id)
    }

    override fun cleanupImpl() {
        return
    }

    override fun createTooltip(tooltip: TooltipMakerAPI, expanded: Boolean) {
        val gray = Misc.getGrayColor()
        val highlight = Misc.getHighlightColor()

        if (!Global.CODEX_TOOLTIP_MODE) {
            tooltip.addTitle(spec.name)
        } else {
            tooltip.addSpacer(-10f)
        }

        tooltip.addPara(
            "Enables you to take control of your %s and %s.",
            10f,
            Misc.getHighlightColor(),
            "mote ring", "send motes to attack fleets"
        )

        tooltip.addPara(
            "Uses %s per attack.",
            10f,
            Misc.getHighlightColor(),
            "one mote"
        )

        tooltip.addPara(
            "Cannot be used past %s.",
            10f,
            Misc.getNegativeHighlightColor(),
            "${MAX_RANGE.toInt()}su*"
        )

        tooltip.addPara("*2000 units = 1 map grid cell", gray, 10f)
    }

    override fun hasTooltip(): Boolean {
        return true
    }

    fun canTargetEntity(target: SectorEntityToken): Boolean {
        if (target !is CampaignFleetAPI) return false

        if (target.isStationMode) return false

        val dist = MathUtils.getDistance(fleet, target)
        if (dist > MAX_RANGE) return false

        return true
    }
}