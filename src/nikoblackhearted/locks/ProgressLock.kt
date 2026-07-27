package nikoblackhearted.locks

import com.fs.starfarer.api.ui.TooltipMakerAPI
import nikoblackhearted.themes.BHThemeIntel

abstract class ProgressLock {
    abstract fun getProgressMax(intel: BHThemeIntel): Int
    abstract fun canRemove(intel: BHThemeIntel): Boolean
    abstract fun addLockText(info: TooltipMakerAPI)
}