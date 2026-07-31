package nikoblackhearted.themes.ambition

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.BattleAPI
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.CargoAPI
import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.campaign.econ.Industry
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.MarketCMD
import com.fs.starfarer.api.ui.CustomPanelAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import nikoblackhearted.locks.LevelLock
import nikoblackhearted.themes.BHThemeMainIntel

class BHAmbitionThemeIntel: BHThemeMainIntel() {

    companion object {
        fun get(): BHThemeMainIntel? = Global.getSector().intelManager.getFirstIntel(BHAmbitionThemeIntel::class.java) as? BHThemeMainIntel
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

    override fun createSmallDescription(info: TooltipMakerAPI?, width: Float, height: Float) {
        super.createSmallDescription(info, width, height)
    }

    override fun reportBattleOccurred(
        fleet: CampaignFleetAPI?,
        primaryWinner: CampaignFleetAPI?,
        battle: BattleAPI?
    ) {
        TODO("Not yet implemented")
    }

    override fun reportRaidForValuablesFinishedBeforeCargoShown(
        dialog: InteractionDialogAPI?,
        market: MarketAPI?,
        actionData: MarketCMD.TempData?,
        cargo: CargoAPI?
    ) {
        TODO("Not yet implemented")
    }

    override fun reportRaidToDisruptFinished(
        dialog: InteractionDialogAPI?,
        market: MarketAPI?,
        actionData: MarketCMD.TempData?,
        industry: Industry?
    ) {
        TODO("Not yet implemented")
    }

    override fun reportTacticalBombardmentFinished(
        dialog: InteractionDialogAPI?,
        market: MarketAPI?,
        actionData: MarketCMD.TempData?
    ) {
        TODO("Not yet implemented")
    }

    override fun reportSaturationBombardmentFinished(
        dialog: InteractionDialogAPI?,
        market: MarketAPI?,
        actionData: MarketCMD.TempData?
    ) {
        TODO("Not yet implemented")
    }

}