package nikoblackhearted.themes.motes

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.impl.campaign.RuleBasedInteractionDialogPluginImpl
import nikoblackhearted.BHBaseNikoScript

class BHMoteOfficerRebellionScript: BHBaseNikoScript() {
    override fun startImpl() {
        Global.getSector().addScript(this)
    }

    override fun stopImpl() {
        Global.getSector().removeScript(this)
    }

    override fun runWhilePaused(): Boolean = false

    override fun advance(amount: Float) {
        if (Global.getSector().campaignUI.isShowingDialog) return

        Global.getSector().campaignUI.showInteractionDialog(
            RuleBasedInteractionDialogPluginImpl("BHMoteOfficerRebellionInit"),
            Global.getSector().playerFleet
        )
        Global.getSector().memoryWithoutUpdate["\$BHMoteDidOfficerRebellion"] = true
        delete()
    }
}