package nikoblackhearted.ruleCMD

import com.fs.starfarer.api.GameState
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin
import com.fs.starfarer.api.util.Misc
import nikoblackhearted.BHBaseNikoScript

class BHMusicToggle: BaseCommandPlugin() {
    override fun execute(
        ruleId: String?,
        dialog: InteractionDialogAPI?,
        params: MutableList<Misc.Token>?,
        memoryMap: MutableMap<String, MemoryAPI>?
    ): Boolean {
        if (dialog == null || params == null || memoryMap == null) return false

        var musicId: String? = params[0].getString(memoryMap)
        if (musicId == "") musicId = null
        val toggleMode = params[1].getBoolean(memoryMap)
        val useDelayedScript = if (params.size >= 3) params[2].getBoolean(memoryMap) else false

        if (useDelayedScript) {
            if (musicId == null && !toggleMode) {
                BHDelayedMusicClearScript().start()
            } else if (musicId != null) {
                BHDelayedMusicScript(musicId).start()
            }
            return true
        }

        Global.getSoundPlayer().setSuspendDefaultMusicPlayback(toggleMode)
        Global.getSoundPlayer().playCustomMusic(1, 1, musicId, true)
        return true
    }

    class BHDelayedMusicScript(val musicId: String): BHBaseNikoScript() {
        var timesRan = 0
        override fun startImpl() {
            Global.getSector().addScript(this)
        }

        override fun stopImpl() {
            Global.getSector().removeScript(this)
        }

        override fun runWhilePaused(): Boolean {
            return true
        }

        override fun advance(amount: Float) {
            if (timesRan++ < 2) return
            Global.getSoundPlayer().playCustomMusic(1, 1, musicId, true)
            delete()
        }
    }

    class BHDelayedMusicClearScript(): BHBaseNikoScript() {
        override fun startImpl() {
            Global.getSector().addScript(this)
        }

        override fun stopImpl() {
            Global.getSector().removeScript(this)
        }

        override fun runWhilePaused(): Boolean {
            return false
        }

        override fun advance(amount: Float) {
            if (Global.getCurrentState() != GameState.CAMPAIGN) return

            Global.getSoundPlayer().playCustomMusic(1, 1, null, false)
            Global.getSoundPlayer().setSuspendDefaultMusicPlayback(false)
            delete()
        }
    }
}