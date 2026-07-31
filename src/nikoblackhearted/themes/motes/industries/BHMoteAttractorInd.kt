package nikoblackhearted.themes.motes.industries

import com.fs.starfarer.api.campaign.econ.Industry
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.impl.campaign.econ.impl.BaseIndustry
import com.fs.starfarer.api.impl.campaign.ids.Commodities
import com.fs.starfarer.api.impl.campaign.ids.Stats
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import nikoblackhearted.BHDelayedExecution
import nikoblackhearted.themes.motes.BHMoteCircleScript

open class BHMoteAttractorInd: BaseIndustry() {

    companion object {
        const val GROUND_DEFENSE_MULT = 1.1f
    }

    lateinit var script: BHMoteCircleScript
    var madeScript = false

    override fun apply() {
        super.apply(true)

        val size = market.size
        demand(Commodities.MARINES, size - 1)
        demand(Commodities.RARE_METALS, size - 1)
        demand(Commodities.FUEL, size - 1)
        demand(Commodities.VOLATILES, size)
        demand(Commodities.SUPPLIES, size)

        if (!isFunctional) {
            script.stop()
            return
        }

        if (!madeScript && market != null && market.primaryEntity != null) {
            script = BHMoteCircleScript(
                market.primaryEntity,
                100f,
                1,
                respawnDelayMult = 0.4f
            )
            script.start()
            madeScript = true
        }
        script.maxMotes = getMaxMotes()

        market.stats.dynamic.getMod(Stats.GROUND_DEFENSES_MOD).modifyMult(
            id,
            GROUND_DEFENSE_MULT,
            nameForModifier
        )
    }

    private fun getMaxMotes(): Int = (((market.primaryEntity.radius * 0.035f) * market.size - 2).toInt())

    override fun unapply() {
        super.unapply()

        class DelayedExecution: BHDelayedExecution(
            IntervalUtil(0f, 0f),
            false,
            true
        ) {
            override fun executeImpl() {
                if (!market.hasIndustry(spec.id)) {
                    script.delete()
                }
            }
        }

        DelayedExecution().start()

        market.stats.dynamic.getMod(Stats.GROUND_DEFENSES_MOD).unmodify(id)
    }

    override fun hasPostDemandSection(hasDemand: Boolean, mode: Industry.IndustryTooltipMode?): Boolean {
        return true
    }

    override fun addPostDemandSection(
        tooltip: TooltipMakerAPI?,
        hasDemand: Boolean,
        mode: Industry.IndustryTooltipMode?
    ) {
        super.addPostDemandSection(tooltip, hasDemand, mode)

        if (tooltip == null) return

        tooltip.addPara(
            "Creates a %s around the planet, aiding in orbital defense.",
            10f,
            Misc.getHighlightColor(),
            "mote swarm"
        )

        tooltip.addPara(
            "Suppresses the effects of the %s condition.",
            10f,
            Misc.getNegativeHighlightColor(),
            "Unnerved"
        )

        addGroundDefensesImpactSection(tooltip, GROUND_DEFENSE_MULT - 1f)
    }
}