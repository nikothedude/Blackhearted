package nikoblackhearted.themes

class BHRepeatableIntel: BHThemeIntel() {
    override fun setProgress(progress: Int) {
        super.setProgress(progress)

        if (this.progress >= maxProgress) {
            reset()
        }
    }

    fun reset() {
        progress = 0
    }
}