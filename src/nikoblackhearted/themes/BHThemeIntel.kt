package nikoblackhearted.themes

import com.fs.starfarer.api.impl.campaign.ids.Tags
import com.fs.starfarer.api.impl.campaign.intel.events.BaseEventIntel
import com.fs.starfarer.api.ui.SectorMapAPI
import nikoblackhearted.BHHandler
import nikoblackhearted.locks.ProgressLock

/** Not just the main intel. Some themes may have multiple event intels. We will make a second layer of abstraction for the main intel. */
abstract class BHThemeIntel: BaseEventIntel() {
    val locks = HashSet<ProgressLock>()

    init {
        init()
    }

    open fun init() {
        progress = 0
        maxProgress = 1000
    }

    override fun setProgress(progress: Int) {
        val newProgress = (progress.coerceAtMost(getMaxAllowedProgress()))

        super.setProgress(newProgress)
    }

    fun getMaxAllowedProgress(): Int {
        sanitizeLocks()
        return locks.minOfOrNull { it.getProgressMax(this) } ?: maxProgress
    }

    fun sanitizeLocks() {
        for (lock in locks.toMutableSet()) {
            if (lock.canRemove(this)) {
                removeLock(lock)
            }
        }
    }

    open fun removeLock(lock: ProgressLock) {
        locks -= lock
    }

    override fun getIntelTags(map: SectorMapAPI?): Set<String?>? {
        val tags = HashSet<String>()
        if (isImportant) {
            tags.add(Tags.INTEL_IMPORTANT)
        }
        tags += BHHandler.INTEL_KEY
        return tags
    }
}