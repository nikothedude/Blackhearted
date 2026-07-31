package nikoblackhearted.themes

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.*
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.campaign.listeners.ColonyPlayerHostileActListener
import com.fs.starfarer.api.campaign.listeners.FleetEventListener
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.Nex_MarketCMD
import com.fs.starfarer.api.ui.TooltipMakerAPI
import exerelin.campaign.InvasionRound
import exerelin.campaign.intel.agents.CovertActionIntel
import exerelin.utilities.AgentActionListener
import exerelin.utilities.InvasionListener
import nikoblackhearted.BHHandler
import nikoblackhearted.BHSettings
import nikoblackhearted.themes.motes.BHMoteThemeIntel
import java.awt.Color

abstract class BHThemeMainIntel: BHThemeIntel(), FleetEventListener, ColonyPlayerHostileActListener {
    // of note, all of the main intels should have some sort of progressing dread mechanic.

    var agentListener: BHMainThemeAgentListener? = null
    var invasionListener: BHInvasionListener? = null

    class BHInvasionListener(val BHIntel: BHThemeMainIntel): InvasionListener {
        override fun reportInvadeLoot(
            dialog: InteractionDialogAPI?,
            market: MarketAPI?,
            actionData: Nex_MarketCMD.TempDataInvasion?,
            cargo: CargoAPI?
        ) {
            return
        }

        override fun reportInvasionRound(
            result: InvasionRound.InvasionRoundResult?,
            fleet: CampaignFleetAPI?,
            defender: MarketAPI?,
            atkStr: Float,
            defStr: Float
        ) {
            return
        }

        override fun reportInvasionFinished(
            fleet: CampaignFleetAPI?,
            attackerFaction: FactionAPI?,
            market: MarketAPI?,
            numRounds: Float,
            success: Boolean
        ) {
            return
        }

        override fun reportMarketTransfered(
            market: MarketAPI,
            newOwner: FactionAPI,
            oldOwner: FactionAPI,
            playerInvolved: Boolean,
            isCapture: Boolean,
            factionsToNotify: List<String?>?,
            repChangeStrength: Float
        ) {
            BHIntel.reportMarketTransferred(market, newOwner, oldOwner, playerInvolved, isCapture)
        }

    }


    class BHMainThemeAgentListener(val BHIntel: BHThemeMainIntel): AgentActionListener {
        override fun reportAgentAction(intel: CovertActionIntel?) {
            if (intel == null) return

            BHIntel.reportAgentAction(intel)
        }
    }

    abstract class BaseStageTooltip(): TooltipMakerAPI.TooltipCreator {
        override fun isTooltipExpandable(tooltipParam: Any?): Boolean = false
        override fun getTooltipWidth(tooltipParam: Any?): Float = 400f
    }

    init {
        Global.getSector().listenerManager.addListener(this, false)
        checkNex()
    }

    private fun checkNex() {
        if (BHSettings.nexEnabled) {
            if (agentListener == null) {
                agentListener = BHMainThemeAgentListener(this)
                Global.getSector().listenerManager.addListener(agentListener, false)
            }
            if (invasionListener == null) {
                invasionListener = BHInvasionListener(this)
                Global.getSector().listenerManager.addListener(invasionListener, false)
            }
        }
    }

    override fun getName(): String? {
        return BHHandler.getTheme()!!.getUIName()
    }

    override fun getIcon(): String? {
        return BHHandler.getTheme()!!.getIcon()
    }

    override fun getStageIconSize(stageId: Any?): Float {
        return super.getStageIconSize(stageId) * 0.5f
    }

    override fun notifyEnding() {
        super.notifyEnding()

        Global.getSector().listenerManager.removeListener(this)

        if (agentListener != null) {
            Global.getSector().listenerManager.removeListener(agentListener)
            Global.getSector().listenerManager.removeListener(invasionListener)
        }
    }

    override fun reportFleetDespawnedToListener(
        fleet: CampaignFleetAPI?,
        reason: CampaignEventListener.FleetDespawnReason?,
        param: Any?
    ) {
        return
    }

    open fun reportMarketTransferred(
        market: MarketAPI,
        newOwner: FactionAPI,
        oldOwner: FactionAPI,
        playerInvolved: Boolean,
        capture: Boolean
    ) {
        return
    }

    open fun reportAgentAction(intel: CovertActionIntel) {
        return
    }

}
