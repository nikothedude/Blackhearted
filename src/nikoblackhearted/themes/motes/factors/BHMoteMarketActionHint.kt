package nikoblackhearted.themes.motes.factors

import com.fs.starfarer.api.impl.campaign.intel.events.BaseEventIntel
import com.fs.starfarer.api.impl.campaign.intel.events.BaseFactorTooltip
import com.fs.starfarer.api.impl.campaign.intel.events.BaseOneTimeFactor
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI.TooltipCreator
import com.fs.starfarer.api.util.Misc
import nikoblackhearted.BHSettings

class BHMoteMarketActionHint: BaseOneTimeFactor(0) {
    init {
        timestamp = 0
    }

    override fun shouldShow(intel: BaseEventIntel?): Boolean {
        return true
    }

    override fun getDesc(intel: BaseEventIntel?): String? {
        return "Population centers attacked"
    }

    override fun getMainRowTooltip(intel: BaseEventIntel?): TooltipCreator {
        return object : BaseFactorTooltip() {
            override fun createTooltip(tooltip: TooltipMakerAPI, expanded: Boolean, tooltipParam: Any?) {
                tooltip.addPara(
                    "%s and %s will progress this event.",
                    0f,
                    Misc.getHighlightColor(),
                    "Raiding", "bombarding"
                )

                tooltip.addPara(
                    "%s to disrupt a %s will add %s, while a %s will %s your dread.",
                    10f,
                    Misc.getHighlightColor(),
                    "Raiding", "spaceport", "extra progress", "saturation bombardment", "massively spike"
                ).setHighlightColors(
                    Misc.getHighlightColor(),
                    Misc.getHighlightColor(),
                    Misc.getPositiveHighlightColor(),
                    Misc.getNegativeHighlightColor(),
                    Misc.getPositiveHighlightColor()
                )

                if (BHSettings.nexEnabled) {
                    tooltip.addPara(
                        "%s and %s a colony will also add progress, but to a lesser extent than more malicious acts.",
                        10f,
                        Misc.getHighlightColor(),
                        "Invading", "Conquering"
                    )
                }
            }
        }
    }
}