package nikoblackhearted.themes

import nikoblackhearted.BHHandler

abstract class BHThemeMainIntel: BHThemeIntel() {
    // of note, all of the main intels should have some sort of progressing dread mechanic.

    override fun getName(): String? {
        return BHHandler.getTheme()!!.name
    }

    override fun getIcon(): String? {
        return BHHandler.getTheme()!!.getIcon()
    }
}