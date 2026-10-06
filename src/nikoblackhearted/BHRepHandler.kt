package nikoblackhearted

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.FactionAPI
import com.fs.starfarer.api.impl.campaign.ids.Factions
import com.fs.starfarer.api.util.Misc
import exerelin.campaign.AllianceManager
import exerelin.campaign.DiplomacyManager
import exerelin.campaign.ExerelinReputationAdjustmentResult
import exerelin.utilities.NexUtils
import nikoblackhearted.BHRepListener.Companion.sanitizeRel
import nikoblackhearted.BHRepListener.Companion.setMaxRep

object BHRepHandler {

    const val REP_MOD_ID = "BHRepHandler"

    fun adjustRepMalus(amount: Float, factions: List<FactionAPI>) {
        val commed = Misc.getCommissionFaction()
        for (fac in factions) {
            if (fac == commed && !BHSettings.alliedHostilitiesOk) continue
            if (BHSettings.nexEnabled && !BHSettings.alliedHostilitiesOk) {
                val alliance = AllianceManager.getPlayerAlliance(true)
                if (alliance != null) {
                    if (alliance.membersCopy.contains(fac.id)) continue
                }
            }
            fac.setMaxRep(amount)
            fac.sanitizeRel()
        }
    }
}