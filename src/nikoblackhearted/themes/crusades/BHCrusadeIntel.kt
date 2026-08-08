package nikoblackhearted.themes.crusades

import com.fs.starfarer.api.campaign.FactionAPI
import com.fs.starfarer.api.campaign.econ.MarketAPI
import exerelin.campaign.intel.groundbattle.GroundBattleIntel.BattleOutcome
import exerelin.campaign.intel.invasion.InvasionIntel
import exerelin.utilities.StringHelper

class BHCrusadeIntel(attacker: FactionAPI, from: MarketAPI, target: MarketAPI, fp: Float, orgDur: Float) : InvasionIntel(attacker, from, target, fp, orgDur) {
    override fun getName(): String {
        var useDeployedText = outcome == OffensiveOutcome.SUCCESS
        useDeployedText =
            useDeployedText and (groundBattle != null && groundBattle.getOutcome() != BattleOutcome.ATTACKER_VICTORY)

        if (useDeployedText) {
            var base = StringHelper.getString("nex_fleetIntel", "title")
            base = StringHelper.substituteToken(base, "\$action", actionName, true)
            base = StringHelper.substituteToken(base, "\$market", getTarget().name)

            return "$base - Liberation"
        }
        return "Liberation: ${target.name}"
    }

    override fun getDescString(): String {
        return "The vile policies practiced by \$theTargetFaction have not gone unnoticed. Voices cry for help, and finally, someone listens. \n" +
            "\n" +
            "\$TheFaction \$isOrAre launching a liberation campaign against \$market in the \$location, held by \$theTargetFaction. The liberation forces are projected to be \$strDesc and are likely comprised of \$numFleets \$fleetsStr."
    }

    override fun terminateEvent(outcome: OffensiveOutcome?) {
        if (outcome == OffensiveOutcome.NO_LONGER_HOSTILE) return
        super.terminateEvent(outcome)
    }

}