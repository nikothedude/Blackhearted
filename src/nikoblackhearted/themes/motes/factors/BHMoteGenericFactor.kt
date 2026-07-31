package nikoblackhearted.themes.motes.factors

import com.fs.starfarer.api.impl.campaign.intel.events.BaseEventIntel
import com.fs.starfarer.api.impl.campaign.intel.events.BaseFactorTooltip
import com.fs.starfarer.api.impl.campaign.intel.events.BaseOneTimeFactor
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc

class BHMoteGenericFactor(points: Int, val tooltipName: String, val desc: String, val flavor: String) : BaseOneTimeFactor(points) {
    override fun getDesc(intel: BaseEventIntel?): String {
        return tooltipName
    }

    override fun getMainRowTooltip(intel: BaseEventIntel?): TooltipMakerAPI.TooltipCreator? {
        return object : BaseFactorTooltip() {
            override fun createTooltip(tooltip: TooltipMakerAPI, expanded: Boolean, tooltipParam: Any?) {
                tooltip.addPara(
                    desc,
                    10f
                )
                if (flavor.isNotEmpty()) {
                    tooltip.addPara(
                        flavor, 10f,
                        Misc.getGrayColor(), Misc.getHighlightColor()
                    ).italicize()
                }
            }
        }
    }
}