package nikoblackhearted

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.BaseCampaignEventListener
import com.fs.starfarer.api.campaign.FactionAPI
import org.lazywizard.lazylib.MathUtils

class BHRepListener: BaseCampaignEventListener(false) {

    companion object {
        const val MAX_REP_MEMKEY = "\$BH_maxRepMemkey"

        fun FactionAPI.getMaxRep(): Float {
            var max = memoryWithoutUpdate[MAX_REP_MEMKEY] as? Float
            if (max == null) {
                setMaxRep(1f)
                max = 1f
            }
            return max
        }

        fun FactionAPI.setMaxRep(max: Float) {
            memoryWithoutUpdate[MAX_REP_MEMKEY] = MathUtils.clamp(max, -1f, 1f)
        }

        fun FactionAPI.sanitizeRel() {
            val max = getMaxRep()
            relToPlayer.rel = relToPlayer.rel.coerceAtMost(max)
        }
    }

    override fun reportPlayerReputationChange(faction: String?, delta: Float) {
        super.reportPlayerReputationChange(faction, delta)

        if (faction == null || delta == 0f) return

        val realFac = Global.getSector().getFaction(faction) ?: return
        realFac.sanitizeRel()
    }

}