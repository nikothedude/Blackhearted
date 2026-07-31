package nikoblackhearted

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc

abstract class BHDelayedExecution(
    val interval: IntervalUtil,
    var useDays: Boolean = true,
    var runIfPaused: Boolean = false
): BHBaseNikoScript() {
    override fun startImpl() {
        Global.getSector().addScript(this)
    }

    override fun stopImpl() {
        Global.getSector().removeScript(this)
    }

    override fun runWhilePaused(): Boolean = runIfPaused

    override fun advance(amount: Float) {
        var amount = amount
        if (useDays) amount = Misc.getDays(amount)

        interval.advance(amount)
        if (interval.intervalElapsed()) {
            execute()
        }
    }

    open fun execute() {
        executeImpl()
        delete()
    }

    abstract fun executeImpl()
}