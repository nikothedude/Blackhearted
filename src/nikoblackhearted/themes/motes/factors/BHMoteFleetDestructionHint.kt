package nikoblackhearted.themes.motes.factors

import com.fs.starfarer.api.impl.campaign.intel.events.BaseEventIntel
import com.fs.starfarer.api.impl.campaign.intel.events.BaseFactorTooltip
import com.fs.starfarer.api.impl.campaign.intel.events.BaseOneTimeFactor
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI.TooltipCreator
import com.fs.starfarer.api.util.Misc

class BHMoteFleetDestructionHint: BaseOneTimeFactor(0) {
    init {
        timestamp = 0
    }

    override fun shouldShow(intel: BaseEventIntel?): Boolean {
        return true
    }

    override fun getDesc(intel: BaseEventIntel?): String? {
        return "Fleets destroyed"
    }

    override fun getMainRowTooltip(intel: BaseEventIntel?): TooltipCreator {
        return object : BaseFactorTooltip() {
            override fun createTooltip(tooltip: TooltipMakerAPI, expanded: Boolean, tooltipParam: Any?) {
                tooltip.addPara(
                    "Destroying ships, especially %s, will slowly progress this event.",
                    0f,
                    Misc.getHighlightColor(),
                    "civilians"
                )
            }
        }
    }
}