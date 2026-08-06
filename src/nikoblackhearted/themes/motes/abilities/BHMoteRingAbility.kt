package nikoblackhearted.themes.motes.abilities

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.impl.campaign.abilities.BaseToggleAbility
import com.fs.starfarer.api.impl.campaign.abilities.GoDarkAbility
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import nikoblackhearted.themes.motes.BHMoteCircleScript
import nikoblackhearted.themes.motes.BHMoteThemeIntel

class BHMoteRingAbility: BaseToggleAbility() {

    companion object {
        const val PER_MOTE_SENSOR_INCREASE = 35
    }

    lateinit var script: BHMoteCircleScript

    override fun init(id: String?, entity: SectorEntityToken?) {
        super.init(id, entity)

        script = BHMoteCircleScript(
            fleet,
            100f
        )
    }

    override fun activateImpl() {
        script.start()
    }

    override fun advance(amount: Float) {
        super.advance(amount)

        val days = Misc.getDays(amount)

        if (!script.started) {
            script.regenerateMotes(days)
        }

        script.maxMotes = BHMoteThemeIntel.get()!!.getPlayerMaxMotes()
        script.respawnDelayMult = BHMoteThemeIntel.get()!!.getPlayerMoteRespawnDelayMult()
    }

    override fun applyEffect(amount: Float, level: Float) {
        if (level > 0f) {
            Global.getSoundPlayer().playLoop(
                "mote_attractor_loop",
                fleet,
                1f,
                (0.5f * level),
                fleet.location,
                Misc.ZERO
            )
        }

        fleet.stats.detectedRangeMod.modifyFlat(
            spec.id,
            ((script.getMaxSwarms() * PER_MOTE_SENSOR_INCREASE) * level),
            "${spec.name} (${script.getMaxSwarms()} motes)"
        )

        return
    }

    override fun deactivateImpl() {
        cleanupImpl()
    }

    override fun cleanupImpl() {
        fleet.stats.detectedRangeMod.unmodify(spec.id)
        script.stop()
    }

    override fun createTooltip(tooltip: TooltipMakerAPI, expanded: Boolean) {
        val gray = Misc.getGrayColor()
        val highlight = Misc.getHighlightColor()

        var status = " (off)"
        if (turnedOn) {
            status = " (on)"
        }

        if (!Global.CODEX_TOOLTIP_MODE) {
            val title = tooltip.addTitle(spec.name + status)
            title.highlightLast(status)
            title.setHighlightColor(gray)
        } else {
            tooltip.addSpacer(-10f)
        }

        val pad = 10f

        tooltip.addPara(
            "Dance with me, dance with me, play to the tune and strike at our foes~",
            pad,
            Misc.getGrayColor(),
            Misc.getHighlightColor()
        ).italicize()

        tooltip.addPara(
            "Summons a ring of %s around your fleet that %s, %s and %s while also %s.",
            pad,
            Misc.getHighlightColor(),
            "motes", "strike nearby hostile fleets", "slowing them", "reducing their sensor range", "increasing sensor profile"
        ).setHighlightColors(
            BHMoteThemeIntel.color,
            Misc.getPositiveHighlightColor(),
            Misc.getHighlightColor(),
            Misc.getHighlightColor(),
            Misc.getHighlightColor(),
            Misc.getHighlightColor()
        )

        tooltip.addPara(
            "The highly volatile particles will also %s by %s based on how many are present.",
            pad,
            Misc.getNegativeHighlightColor(),
            "increase your sensor profile", "$PER_MOTE_SENSOR_INCREASE"
        )

        val currMax = script.getMaxSwarms()

        tooltip.addPara(
            "Your fleet can currently support %s motes, up to a max of %s. This can be increased via %s.",
            pad,
            Misc.getHighlightColor(),
            "$currMax", "${script.maxMotes}", "progressing your madness"
        ).setHighlightColors(
            Misc.getHighlightColor(),
            Misc.getHighlightColor(),
            BHMoteThemeIntel.color
        )

        tooltip.addPara(
            "Motes %s.",
            pad,
            Misc.getHighlightColor(),
            "slowly replenish over time"
        )

        // TODO - make it so the LC gets SCARED

        addIncompatibleToTooltip(tooltip, expanded)
    }

    override fun hasTooltip(): Boolean {
        return true
    }
}