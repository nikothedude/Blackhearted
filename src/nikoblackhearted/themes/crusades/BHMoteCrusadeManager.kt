package nikoblackhearted.themes.crusades

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.BattleAPI
import com.fs.starfarer.api.campaign.CampaignEventListener
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.campaign.listeners.FleetEventListener
import com.fs.starfarer.api.impl.campaign.RuleBasedInteractionDialogPluginImpl
import com.fs.starfarer.api.impl.campaign.ids.Factions
import com.fs.starfarer.api.impl.campaign.ids.FleetTypes
import com.fs.starfarer.api.impl.campaign.missions.DelayedFleetEncounter
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithTriggers
import com.fs.starfarer.api.impl.campaign.missions.hub.MissionTrigger
import exerelin.campaign.fleets.InvasionFleetManager
import exerelin.campaign.intel.invasion.InvasionIntel
import exerelin.campaign.intel.rebellion.RebellionCreator
import exerelin.campaign.intel.rebellion.RebellionIntel
import nikoblackhearted.BHBaseNikoScript
import nikoblackhearted.BHPeople
import nikoblackhearted.BHSettings
import nikoblackhearted.themes.motes.BHMoteThemeIntel
import org.lwjgl.util.vector.Vector2f
import org.magiclib.kotlin.getFactionMarkets
import org.magiclib.kotlin.getMarketsInLocation
import org.magiclib.kotlin.isMilitary

object BHMoteCrusadeManager {

    const val FAC_ID = Factions.LUDDIC_CHURCH

    enum class Stage(val memFlag: String) {
        ONE("\$BHMoteCrusadeOneDefeated") {
            override fun getQuality(): HubMissionWithTriggers.FleetQuality {
                return HubMissionWithTriggers.FleetQuality.SMOD_1
            }

            override fun getHailId(): String = "BHMoteCrusaderHailOne"
            override fun getExtraTargets(): Int = 1
            override fun getInvasionFPMult(): Float = 1.1f
            override fun getFlagshipVariant(): String = "eradicator_Assault"
        },
        TWO("\$BHMoteCrusadeTwoDefeated") {
            override fun getQuality(): HubMissionWithTriggers.FleetQuality {
                return HubMissionWithTriggers.FleetQuality.SMOD_2
            }

            override fun getHailId(): String = "BHMoteCrusaderHailTwo"
            override fun getExtraTargets(): Int = 3
            override fun getInvasionFPMult(): Float = 1.2f
            override fun getFlagshipVariant(): String = "retribution_Standard"

            override fun onFleetCreated(fleet: CampaignFleetAPI) {
                super.onFleetCreated(fleet)

                BHPeople.getImportantPeople()[BHPeople.MOTES_CRUSADER]?.stats?.setSkillLevel("BHMotesCrusaderSkill", 1f)
            }
        },
        THREE("\$BHMoteCrusadeThreeDefeated") {
            override fun getQuality(): HubMissionWithTriggers.FleetQuality {
                return HubMissionWithTriggers.FleetQuality.SMOD_3
            }

            override fun getHailId(): String = "BHMoteCrusaderHailThree"
            override fun getExtraTargets(): Int = 5
            override fun getInvasionFPMult(): Float = 1.3f

            override fun onFleetCreated(fleet: CampaignFleetAPI) {
                super.onFleetCreated(fleet)

                BHPeople.getImportantPeople()[BHPeople.MOTES_CRUSADER]?.stats?.setSkillLevel("BHMotesCrusaderSkillFleet", 2f)
                fleet.addEventListener(CrusadeFinalFleetDeathListener())
            }

            override fun getFlagshipVariant(): String = "invictus_Standard"

            inner class CrusadeFinalFleetDeathListener(): FleetEventListener {
                override fun reportFleetDespawnedToListener(
                    fleet: CampaignFleetAPI?,
                    reason: CampaignEventListener.FleetDespawnReason?,
                    param: Any?
                ) {
                    CrusadeFinalFleetDoneScript().start()
                    Global.getSector()
                    fleet?.removeEventListener(this)
                }

                override fun reportBattleOccurred(
                    fleet: CampaignFleetAPI?,
                    primaryWinner: CampaignFleetAPI?,
                    battle: BattleAPI?
                ) {
                    return
                }
            }
        };

        abstract fun getQuality(): HubMissionWithTriggers.FleetQuality
        abstract fun getHailId(): String
        abstract fun getExtraTargets(): Int
        abstract fun getInvasionFPMult(): Float
        open fun onFleetCreated(fleet: CampaignFleetAPI) {
            fleet.addEventListener(CrusadeMainFleetDeathListener(this))
        }
        abstract fun getFlagshipVariant(): String

        class CrusadeMainFleetDeathListener(val stage: Stage): FleetEventListener {
            override fun reportFleetDespawnedToListener(
                fleet: CampaignFleetAPI?,
                reason: CampaignEventListener.FleetDespawnReason?,
                param: Any?
            ) {
                Global.getSector().memoryWithoutUpdate[stage.memFlag] = true
                fleet?.removeEventListener(this)
            }

            override fun reportBattleOccurred(
                fleet: CampaignFleetAPI?,
                primaryWinner: CampaignFleetAPI?,
                battle: BattleAPI?
            ) {
                return
            }

        }

        class CrusadeFinalFleetDoneScript(): BHBaseNikoScript() {
            override fun startImpl() {
                Global.getSector().addScript(this)
            }

            override fun stopImpl() {
                Global.getSector().removeScript(this)
            }

            override fun runWhilePaused(): Boolean = true

            override fun advance(amount: Float) {
                if (Global.getSector().campaignUI.isShowingDialog) return

                Global.getSector().campaignUI.showInteractionDialog(
                    RuleBasedInteractionDialogPluginImpl("BHMoteFinalRewardInit"),
                    Global.getSector().playerFleet
                )
                Global.getSector().memoryWithoutUpdate["\$BHMoteDidFinalReward"] = true

                BHMoteThemeIntel.get()?.fleetwideCombatMotes = true
                BHMoteThemeIntel.get()?.maxSongStacks = 100
                BHMoteThemeIntel.get()?.extraSongDecayMult = 0.5f
                Global.getSector().playerStats.points += 1

                Global.getSector().removeScript(this)
            }
        }

    }
    fun spawnCrusadeOrKillfleet(baseFP: Float, stage: Stage) {
        val markets = Global.getSector().playerFaction.getFactionMarkets()
        if (markets.isEmpty()) {
            spawnKillFleet(baseFP, stage)
        } else {
            spawnCrusade(baseFP, markets, stage)
        }
    }

    private fun spawnKillFleet(baseFP: Float, stage: Stage) {
        val enc = DelayedFleetEncounter(
            null,
            stage.name
        )
        enc.setTypes(
            DelayedFleetEncounter.EncounterType.OUTSIDE_SYSTEM,
            DelayedFleetEncounter.EncounterType.IN_HYPER_EN_ROUTE,
            DelayedFleetEncounter.EncounterType.JUMP_IN_NEAR_PLAYER,
        )
        if (Global.getSettings().isDevMode) {
            enc.setDelayNone()
        } else {
            enc.setDelay(10f, 30f)
        }
        enc.setLocationAnywhere(true, FAC_ID)
        enc.setDoNotAbortWhenPlayerFleetTooStrong()

        enc.beginCreate()

        enc.triggerCreateFleet(
            HubMissionWithTriggers.FleetSize.LARGE,
            stage.getQuality(),
            FAC_ID,
            FleetTypes.PATROL_LARGE, // TODO bespoke fleet type
            Vector2f()
        )
        enc.triggerSetFleetCombatFleetPoints(baseFP)
        enc.triggerSetFleetAlwaysPursue()
        enc.triggerFleetAllowJump()
        enc.triggerMakeHostileAndAggressive()
        enc.triggerMakeNoRepImpact()
        enc.triggerOrderFleetInterceptPlayer(true, true)
        enc.triggerSetFleetGenericHailPermanent(stage.getHailId())
        enc.triggerCustomAction(TestAction(stage))
        enc.triggerSetFleetCommander(BHPeople.getImportantPeople()[BHPeople.MOTES_CRUSADER])
        enc.triggerFleetSetFlagship(stage.getFlagshipVariant())
        enc.triggerFleetMakeImportantPermanent("\$BHCrusadeFleet")

        enc.endCreate()
    }

    class TestAction(val stage: Stage) : MissionTrigger.TriggerAction {
        override fun doAction(context: MissionTrigger.TriggerActionContext?) {
            if (context == null) return
            val fleet = context.fleet ?: return
            stage.onFleetCreated(fleet)
        }
    }

    private fun spawnCrusade(
        baseFP: Float,
        markets: MutableList<MarketAPI>,
        stage: Stage
    ) {
        val capital = markets.maxByOrNull { it.size } ?: return
        val capitalSys = capital.containingLocation
        val capitalMarkets = capitalSys.getMarketsInLocation(Factions.PLAYER)

        if (BHSettings.nexEnabled) {
            val invasionSource = Global.getSector().getFaction(Factions.INDEPENDENT).getFactionMarkets()
                    .filter { it.isMilitary() }.randomOrNull() ?: Global.getSector()
                    .getFaction(Factions.INDEPENDENT).getFactionMarkets().random()
            val firstIntel = RebellionCreator.getInstance().createRebellion(
                capital,
                Factions.INDEPENDENT,
                true
            )
            if (firstIntel != null) {
                firstIntel.rebelStrength = (firstIntel.govtStrength * 1.2f).coerceAtMost(1000f)
                firstIntel.sendUpdateIfPlayerHasIntel(null, false, false)
            }

            val copy = markets.toHashSet()
            var extra = stage.getExtraTargets()
            while (extra-- > 0 && copy.isNotEmpty()) {
                val rand = copy.randomOrNull() ?: continue
                copy -= rand
                if (!RebellionIntel.isOngoing(rand)) {
                    val intel = RebellionCreator.getInstance().createRebellion(
                        rand,
                        Factions.INDEPENDENT,
                        true
                    )
                    if (intel != null) {
                        intel.sendUpdateIfPlayerHasIntel(null, false, false)
                        intel.rebelStrength = (intel.govtStrength * 1.2f).coerceAtMost(1000f)
                    }
                }

                // spawn fleet
                var fp = InvasionFleetManager.getWantedFleetSize(
                    Global.getSector().getFaction(Factions.INDEPENDENT),
                    rand,
                    0.2f,
                    false
                )
                fp *= InvasionFleetManager.getInvasionSizeMult(Factions.INDEPENDENT)
                fp *= stage.getInvasionFPMult()

                //fp *= MathUtils.getRandomNumberInRange(0.8f, 1.2f);
                val intel = InvasionIntel(Global.getSector().getFaction(Factions.INDEPENDENT), invasionSource, rand, fp, 1f)
                intel.isAbortIfNonHostile = false
                intel.init()
            }
        }

        spawnKillFleet(
            baseFP,
            stage
        )
    }
}