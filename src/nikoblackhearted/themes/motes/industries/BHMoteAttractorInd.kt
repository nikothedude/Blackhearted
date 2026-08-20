package nikoblackhearted.themes.motes.industries

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.econ.Industry
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.impl.campaign.econ.impl.BaseIndustry
import com.fs.starfarer.api.impl.campaign.econ.impl.MilitaryBase
import com.fs.starfarer.api.impl.campaign.ids.Commodities
import com.fs.starfarer.api.impl.campaign.ids.Factions
import com.fs.starfarer.api.impl.campaign.ids.Stats
import com.fs.starfarer.api.impl.campaign.ids.Strings
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import nikoblackhearted.BHDelayedExecution
import nikoblackhearted.Mathutils.roundNumTo
import nikoblackhearted.Mathutils.trimHangingZero
import nikoblackhearted.entities.MoteSwarmEntityPlugin
import nikoblackhearted.themes.motes.BHMoteCircleScript
import nikoblackhearted.themes.motes.BHMoteThemeIntel
import org.lazywizard.lazylib.MathUtils
import org.magiclib.kotlin.getMaxIndustries

open class BHMoteAttractorInd: BaseIndustry() {

    companion object {
        const val GROUND_DEFENSE_MULT = 1.1f
        const val MIN_SWARM_SIZE = 5
        const val SWARMS_PER_SIZE = 1

        const val BASE_DAYS_BETWEEN_SWARMS = 23f

        const val ALPHA_RESPAWN_MULT = 1.25f
    }

    lateinit var script: BHMoteCircleScript
    var roamingMotes = HashSet<MoteSwarmEntityPlugin>()
    var madeScript = false

    override fun apply() {
        super.apply(true)

        val size = market.size
        demand(Commodities.MARINES, size - 1)
        demand(Commodities.RARE_METALS, size - 1)
        demand(Commodities.FUEL, size - 1)
        demand(Commodities.VOLATILES, size)
        demand(Commodities.SUPPLIES, size)

        if (currTooltipMode == Industry.IndustryTooltipMode.ADD_INDUSTRY) {
            return
        }

        if (!madeScript && market != null && market.primaryEntity != null) {
            script = BHMoteCircleScript(
                market.primaryEntity,
                100f,
                1
            )
            madeScript = true
        }

        script.maxMotes = getMaxMotes()
        if (aiCoreId == Commodities.ALPHA_CORE) {
            script.respawnDelayMult = (1f * ALPHA_RESPAWN_MULT)
        } else {
            script.respawnDelayMult = 1f
        }

        val deficit = getOurDeficits()
        val extra = if (deficit < 1) "shortages" else ""
        val mult = 1f + ((GROUND_DEFENSE_MULT - 1f) * (deficit))
        market.stats.dynamic.getMod(Stats.GROUND_DEFENSES_MOD).modifyMult(
            id,
            mult,
            "$nameForModifier$extra"
        )
    }

    fun getOurDeficits() = getDeficitMult(
        Commodities.FUEL,
        Commodities.VOLATILES,
        Commodities.SUPPLIES,
        Commodities.RARE_METALS
    )

    fun canSuppress() = getOurDeficits() >= 0.5f

    private fun getMaxMotes(): Int = if (isFunctional) (((market.primaryEntity.radius * 0.035f) * market.size - 2).toInt()) else 0

    override fun unapply() {
        super.unapply()

        class DelayedExecution: BHDelayedExecution(
            IntervalUtil(0f, 0f),
            false,
            true
        ) {
            override fun executeImpl() {
                if (!market.hasIndustry(spec.id)) {
                    if (madeScript) {
                        script.delete()
                    }

                    roamingMotes.forEach { it.delete() }
                    roamingMotes.clear()
                }
            }
        }

        DelayedExecution().start()

        market.stats.dynamic.getMod(Stats.GROUND_DEFENSES_MOD).unmodify(id)
    }

    val swarmSpawnInterval = IntervalUtil(BASE_DAYS_BETWEEN_SWARMS, BASE_DAYS_BETWEEN_SWARMS + 0.1f)
    override fun advance(amount: Float) {
        super.advance(amount)
        if (madeScript) {
            if (!isFunctional) {
                script.stop()
                return
            } else {
                script.start()
            }
        }
        val intel = BHMoteThemeIntel.get() ?: return
        if (roamingMotes == null) roamingMotes = HashSet()
        if (!intel.roamingMotesActive) return
        roamingMotes.removeAll { it.motes.isEmpty() }

        if (roamingMotes.size < getMaxSwarms()) {
            swarmSpawnInterval.advance(Misc.getDays(amount))
            if (swarmSpawnInterval.intervalElapsed()) {
                spawnSwarm()
            }
        }
    }

    private fun spawnSwarm() {
        val params = MoteSwarmEntityPlugin.MoteSwarmParams(
            20,
            spawnSound = "mote_attractor_launch_mote",
            source = market.primaryEntity
        )
        val swarm = market.primaryEntity.containingLocation.addCustomEntity(
            null,
            null,
            "BH_moteSwarm",
            market.faction.id,
            100f,
            0f,
            0f,
            params
        )
        val randSpawnLoc = MathUtils.getRandomPointInCircle(
            market.primaryEntity.location,
            market.primaryEntity.radius
        )
        swarm.setLocation(randSpawnLoc.x, randSpawnLoc.y)
        val plugin = swarm.customPlugin as MoteSwarmEntityPlugin
        BHMoteSwarmRoamingAI(plugin, market.primaryEntity).start()
        roamingMotes += plugin
    }

    fun getMaxSwarms(): Int {
        val bonus = (market.size + 1) - MIN_SWARM_SIZE
        return (bonus * SWARMS_PER_SIZE)
    }

    override fun hasPostDemandSection(hasDemand: Boolean, mode: Industry.IndustryTooltipMode?): Boolean {
        return true
    }

    override fun addPostDemandSection(
        tooltip: TooltipMakerAPI?,
        hasDemand: Boolean,
        mode: Industry.IndustryTooltipMode?
    ) {
        super.addPostDemandSection(tooltip, hasDemand, mode)

        if (tooltip == null) return

        tooltip.addPara(
            "Creates a %s around the planet, aiding in orbital defense.",
            10f,
            Misc.getHighlightColor(),
            "mote swarm"
        )

        tooltip.addPara(
            "Suppresses the effects of the %s condition.",
            10f,
            Misc.getNegativeHighlightColor(),
            "Unnerved"
        )
        if (!canSuppress()) {
            tooltip.addPara(
                "Shortages, however, are allowing unrest and dissent to run rampant.",
                10f
            ).color = Misc.getNegativeHighlightColor()
        }

        val intel = BHMoteThemeIntel.get() ?: return
        if (intel.roamingMotesActive) {
            tooltip.addPara(
                "Additionally, starting at %s, spawns %s that will %s and %s enemy fleets.",
                10f,
                Misc.getHighlightColor(),
                "size $MIN_SWARM_SIZE", "roaming swarms", "hunt", "disable"
            )
        }

        addGroundDefensesImpactSection(tooltip, GROUND_DEFENSE_MULT - 1f)
    }

    override fun isAvailableToBuild(): Boolean {
        return super.isAvailableToBuild() && market.faction.knowsIndustry(spec.id)
    }

    override fun showWhenUnavailable(): Boolean {
        if (!market.faction.knowsIndustry(spec.id)) return false

        return super.showWhenUnavailable()
    }

    override fun isFunctional(): Boolean {
        return super.isFunctional() && market.faction.id == Factions.PLAYER
    }

    override fun canUpgrade(): Boolean {
        return super.canUpgrade() && market.getIndustrySlots() >= 1
    }

    override fun addAlphaCoreDescription(tooltip: TooltipMakerAPI, mode: Industry.AICoreDescriptionMode?) {
        val opad = 10f
        val highlight = Misc.getHighlightColor()

        var pre = "Alpha-level AI core currently assigned. "
        if (mode == Industry.AICoreDescriptionMode.MANAGE_CORE_DIALOG_LIST || mode == Industry.AICoreDescriptionMode.INDUSTRY_TOOLTIP) {
            pre = "Alpha-level AI core. "
        }

        if (mode == Industry.AICoreDescriptionMode.INDUSTRY_TOOLTIP) {
            val coreSpec = Global.getSettings().getCommoditySpec(aiCoreId)
            val text = tooltip.beginImageWithText(coreSpec.getIconName(), 48f)
            text.addPara(
                pre + "Reduces upkeep cost by %s. Reduces demand by %s unit. " +
                        "Increases mote respawn rate by %s.", 0f, highlight,
                "" + ((1f - UPKEEP_MULT) * 100f).toInt() + "%", "" + DEMAND_REDUCTION,
                "${ALPHA_RESPAWN_MULT.roundNumTo(3).trimHangingZero()}x"
            )
            tooltip.addImageWithText(opad)
            return
        }

        tooltip.addPara(
            pre + "Reduces upkeep cost by %s. Reduces demand by %s unit. " +
                    "Increases mote respawn rate by %s.", opad, highlight,
            "" + ((1f - UPKEEP_MULT) * 100f).toInt() + "%", "" + DEMAND_REDUCTION,
            "${ALPHA_RESPAWN_MULT.roundNumTo(3).trimHangingZero()}x"
        )
    }

    fun MarketAPI.getIndustrySlots(): Int = getMaxIndustries() - industries.count { it.isIndustry }
}