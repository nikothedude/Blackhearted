package nikoblackhearted.themes.motes.conditions

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.campaign.econ.MarketImmigrationModifier
import com.fs.starfarer.api.impl.campaign.econ.BaseMarketConditionPlugin
import com.fs.starfarer.api.impl.campaign.population.PopulationComposition
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import nikoblackhearted.Mathutils.roundNumTo
import nikoblackhearted.Mathutils.trimHangingZero
import nikoblackhearted.themes.motes.BHMoteThemeIntel
import nikoblackhearted.themes.motes.industries.BHMoteAttractorInd
import sound.int

class BHMoteThemeCondition: BaseMarketConditionPlugin(), MarketImmigrationModifier {

    companion object {
        const val BASE_STAB_MALUS = 1
        const val BASE_POP_MALUS = 3f
    }

    override fun apply(id: String?) {
        super.apply(id)

        if (!isOnValidMarket()) return
        if (isSuppresed()) return

        market.stability.modifyFlat(
            "${id}_stab",
            -(getStabMalus().toFloat()),
            name
        )

        market.addTransientImmigrationModifier(this)
    }

    override fun unapply(id: String?) {
        super.unapply(id)

        market.stability.unmodify("${id}_stab")

        market.removeTransientImmigrationModifier(this)
    }

    fun getStabMalus(): Int {
        val intel = BHMoteThemeIntel.get() ?: return 0
        val mult = intel.getUnnervedMod(intel.progressFraction)

        return (BASE_STAB_MALUS * mult).toInt() + 1
    }

    fun getPopMalus(): Float {
        val intel = BHMoteThemeIntel.get() ?: return 0f
        val mult = intel.getUnnervedMod(intel.progressFraction)

        return (BASE_POP_MALUS * mult)
    }

    override fun showIcon(): Boolean {
        return super.showIcon() && isOnValidMarket()
    }

    fun isOnValidMarket() = market.faction.isPlayerFaction
    fun isSuppresed(): Boolean {
        val ind = market.industries.firstOrNull { it.isFunctional && it.spec.hasTag("BHMoteUnrestSuppressor") } as? BHMoteAttractorInd ?: return false
        return ind.canSuppress()
    }

    override fun createTooltipAfterDescription(tooltip: TooltipMakerAPI?, expanded: Boolean) {
        super.createTooltipAfterDescription(tooltip, expanded)

        if (tooltip == null) return

        if (isSuppresed()) {
            tooltip.addPara(
                "However, the presence of a %s on %s is doing a truly %s job at beating back these silly thoughts, and " +
                "attracting tourists to join the %s.",
                10f,
                Misc.getHighlightColor(),
                "mote attractor", "${market.name}", "wonderful", "ballet"
            ).setHighlightColors(
                Misc.getHighlightColor(),
                market.faction.baseUIColor,
                Misc.getHighlightColor(),
                BHMoteThemeIntel.color
            )
        } else {
            tooltip.addPara(
                "Currently reducing stability and population growth by %s and %s, respectively.",
                10f,
                Misc.getNegativeHighlightColor(),
                "${getStabMalus()}", "${getPopMalus().roundNumTo(1).trimHangingZero()}"
            )
        }
    }

    override fun modifyIncoming(
        market: MarketAPI?,
        incoming: PopulationComposition?
    ) {
        if (market != this.market || incoming == null) return

        incoming.weight.modifyFlat(
            "${market.id}_pop",
            -getPopMalus(),
            name
        )
    }
}