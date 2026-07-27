package nikoblackhearted.locks

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import nikoblackhearted.themes.BHThemeIntel

class LevelLock(val max: Int, val tilLevel: Int): ProgressLock() {
    override fun getProgressMax(intel: BHThemeIntel): Int {
        return max
    }

    override fun canRemove(intel: BHThemeIntel): Boolean {
        return Global.getSector().playerStats.level >= tilLevel
    }

    override fun addLockText(info: TooltipMakerAPI) {
        info.addPara(
            "Progress locked to %s until you reach level %s.",
            5f,
            Misc.getHighlightColor(),
            "$max", "$tilLevel"
        )
    }
}