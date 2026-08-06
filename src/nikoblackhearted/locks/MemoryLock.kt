package nikoblackhearted.locks

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import nikoblackhearted.themes.BHThemeIntel

class MemoryLock(val max: Int, val flag: String, val text: String): ProgressLock() {
    override fun getProgressMax(intel: BHThemeIntel): Int = max

    override fun canRemove(intel: BHThemeIntel): Boolean = Global.getSector().memoryWithoutUpdate.getBoolean("\$$flag")

    override fun addLockText(info: TooltipMakerAPI) {
        info.addPara(
            "Progress locked to %s until %s.",
            10f,
            Misc.getHighlightedOptionColor(),
            "$max", text
        )
    }
}