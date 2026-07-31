package nikoblackhearted.themes.motes.industries

import com.fs.starfarer.api.campaign.econ.Industry
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc

class BHTormentNexusInd: BHMoteAttractorInd() {
    override fun addPostDemandSection(
        tooltip: TooltipMakerAPI?,
        hasDemand: Boolean,
        mode: Industry.IndustryTooltipMode?
    ) {
        super.addPostDemandSection(tooltip, hasDemand, mode)

        if (tooltip == null) return

        tooltip.addPara(
            "Additionally, %s in exchange for %s stacks. Can %s and %s.",
            10f,
            Misc.getHighlightColor(),
            "decreases population size", "Screaming Song", "lower colony size", "decivilize"
        ).setHighlightColors(
            Misc.getNegativeHighlightColor(),
            Misc.getPositiveHighlightColor(),
            Misc.getNegativeHighlightColor(),
            Misc.getNegativeHighlightColor()
        )
    }
}