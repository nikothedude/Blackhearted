package nikoblackhearted

import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.impl.campaign.ids.FleetTypes
import com.fs.starfarer.api.impl.campaign.ids.MemFlags
import org.magiclib.kotlin.isPirateFaction
import org.magiclib.kotlin.isScavenger
import org.magiclib.kotlin.isTrader

object FleetHelpers {
    fun CampaignFleetAPI.isCivilian(): Boolean {
        return (this.isTrader() || this.memoryWithoutUpdate["\$fleetType"] == "exerelinMiningFleet" || this.isScavenger() || this.memoryWithoutUpdate.getBoolean(MemFlags.ACADEMY_FLEET) || this.memoryWithoutUpdate.getBoolean(MemFlags.SHRINE_PILGRIM_FLEET))
    }

    fun CampaignFleetAPI.isOutcast(): Boolean {
        return (faction.isPirateFaction())
    }
}