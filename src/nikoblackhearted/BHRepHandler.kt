package nikoblackhearted

import com.fs.starfarer.api.campaign.FactionAPI
import com.fs.starfarer.api.impl.campaign.ids.Factions
import exerelin.campaign.DiplomacyManager
import exerelin.campaign.ExerelinReputationAdjustmentResult
import exerelin.utilities.NexUtils
import nikoblackhearted.BHRepListener.Companion.sanitizeRel
import nikoblackhearted.BHRepListener.Companion.setMaxRep

object BHRepHandler {

    const val REP_MOD_ID = "BHRepHandler"

    fun adjustRepMalus(amount: Float, factions: List<FactionAPI>) {
        for (fac in factions) {
            fac.setMaxRep(amount)
            fac.sanitizeRel()
        }
    }
}