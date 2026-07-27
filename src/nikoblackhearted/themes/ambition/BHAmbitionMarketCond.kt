package nikoblackhearted.themes.ambition

import com.fs.starfarer.api.impl.campaign.econ.BaseMarketConditionPlugin
import com.fs.starfarer.api.impl.campaign.econ.BaseMarketConditionPlugin2

class BHAmbitionMarketCond: BaseMarketConditionPlugin() {

    override fun apply(id: String?) {
        super.apply(id)
    }

    override fun unapply(id: String?) {
        super.unapply(id)
    }

    fun isPlayer() = market.isPlayerOwned

    override fun isTransient(): Boolean {
        return false
    }

}