package nikoblackhearted

import com.fs.starfarer.api.campaign.FactionAPI
import com.fs.starfarer.api.impl.campaign.ids.Factions
import exerelin.campaign.DiplomacyManager
import exerelin.campaign.ExerelinReputationAdjustmentResult
import exerelin.utilities.NexUtils

object BHRepHandler {

    const val REP_MOD_ID = "BHRepHandler"

    fun adjustRepMalus(amount: Float, factions: List<FactionAPI>) {
        for (fac in factions) {
            // TODO - do this in a way that doesnt use nex
            val mod = DiplomacyManager.getManager().getMaxRelationshipMod(fac.id, Factions.PLAYER)
            mod.modifyFlat(REP_MOD_ID, amount)
        }
    }

}