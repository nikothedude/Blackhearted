package nikoblackhearted.themes.motes.factors

import com.fs.starfarer.api.impl.campaign.intel.events.BaseEventIntel
import com.fs.starfarer.api.impl.campaign.intel.events.BaseFactorTooltip
import com.fs.starfarer.api.impl.campaign.intel.events.BaseOneTimeFactor
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI.TooltipCreator
import com.fs.starfarer.api.util.Misc

class BHMoteAgentHint: BaseOneTimeFactor(0) {
    init {
        timestamp = 0
    }

    override fun shouldShow(intel: BaseEventIntel?): Boolean {
        return true
    }

    override fun getDesc(intel: BaseEventIntel?): String? {
        return "Operatives"
    }

    override fun getMainRowTooltip(intel: BaseEventIntel?): TooltipCreator {
        return object : BaseFactorTooltip() {
            override fun createTooltip(tooltip: TooltipMakerAPI, expanded: Boolean, tooltipParam: Any?) {
                tooltip.addPara(
                    "Sending an %s to perform %s will %s progress.",
                    0f,
                    Misc.getHighlightColor(),
                    "operative", "malicious acts", "somewhat increase"
                )
                tooltip.addPara(
                    "Generally speaking, the more %s and %s the act is, the more dread you accrue.",
                    10f,
                    Misc.getNegativeHighlightColor(),
                    "impactful", "malicious"
                )
            }
        }
    }
}