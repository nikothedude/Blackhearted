package nikoblackhearted.themes.motes.conditions

import com.fs.starfarer.api.impl.campaign.econ.BaseMarketConditionPlugin
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import nikoblackhearted.themes.motes.BHMoteThemeIntel

class BHMoteScreamingCondition: BaseMarketConditionPlugin() {
    override fun apply(id: String?) {
        super.apply(id)

        if (!isOnValidMarket()) return
        val stacks = getStacks()
        if (stacks <= 0) return

        market.stability.modifyFlat(
            "${id}_stab",
            stacks.toFloat(),
            name
        )
    }

    override fun unapply(id: String?) {
        super.unapply(id)

        market.stability.unmodify("${id}_stab")
    }

    override fun showIcon(): Boolean {
        return super.showIcon() && isOnValidMarket() && getStacks() > 0
    }

    fun getStacks(): Int = BHMoteThemeIntel.get()?.currSongStacks ?: 0

    fun isOnValidMarket() = market.faction.isPlayerFaction

    override fun createTooltipAfterDescription(tooltip: TooltipMakerAPI?, expanded: Boolean) {
        super.createTooltipAfterDescription(tooltip, expanded)

        if (tooltip == null) return

        tooltip.addPara(
            "Improves %s based on how many %s stacks you have.",
            10f,
            Misc.getPositiveHighlightColor(),
            "stability", "Screaming Songs"
        )

        tooltip.addPara(
            "You have %s stacks, improving %s by %s.",
            10f,
            Misc.getHighlightColor(),
            "${getStacks()}", "stability", "${getStacks()}"
        ).setHighlightColors(
            Misc.getHighlightColor(),
            Misc.getHighlightColor(),
            Misc.getPositiveHighlightColor()
        )
    }
}