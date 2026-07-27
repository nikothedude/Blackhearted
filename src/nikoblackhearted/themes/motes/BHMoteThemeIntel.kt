package nikoblackhearted.themes.motes

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.ui.CustomPanelAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import nikoblackhearted.locks.LevelLock
import nikoblackhearted.themes.BHThemeMainIntel

class BHMoteThemeIntel: BHThemeMainIntel() {
    companion object {
        fun get(): BHThemeMainIntel? = Global.getSector().intelManager.getFirstIntel(BHMoteThemeIntel::class.java) as? BHThemeMainIntel
    }

    enum class Stage {
        START,
    }

    override fun init() {
        super.init()

        locks += LevelLock(100, 3)
        locks += LevelLock(300, 6)
        locks += LevelLock(500, 10)
        locks += LevelLock(700, 15)

        addStage(Stage.START, 0)
    }

    override fun createLargeDescription(panel: CustomPanelAPI?, width: Float, height: Float) {
        super.createLargeDescription(panel, width, height)
    }

    override fun createSmallDescription(info: TooltipMakerAPI?, width: Float, height: Float) {
        super.createSmallDescription(info, width, height)
    }
}