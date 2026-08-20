package nikoblackhearted.themes.motes

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.*
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin
import com.fs.starfarer.api.campaign.econ.Industry
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.combat.CombatEngineAPI
import com.fs.starfarer.api.impl.campaign.ids.Factions
import com.fs.starfarer.api.impl.campaign.ids.FleetTypes
import com.fs.starfarer.api.impl.campaign.ids.Industries
import com.fs.starfarer.api.impl.campaign.ids.MemFlags
import com.fs.starfarer.api.impl.campaign.intel.events.BaseOneTimeFactor
import com.fs.starfarer.api.impl.campaign.missions.DelayedFleetEncounter
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithTriggers
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.MarketCMD
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import exerelin.campaign.CovertOpsManager.CovertActionDef
import exerelin.campaign.CovertOpsManager.CovertActionResult
import exerelin.campaign.intel.agents.CovertActionIntel
import lunalib.lunaExtensions.getMarketsCopy
import nikoblackhearted.BHRepHandler
import nikoblackhearted.BHSettings
import nikoblackhearted.FleetHelpers.isCivilian
import nikoblackhearted.FleetHelpers.isOutcast
import nikoblackhearted.locks.LevelLock
import nikoblackhearted.locks.MemoryLock
import nikoblackhearted.themes.BHThemeMainIntel
import nikoblackhearted.themes.crusades.BHMoteCrusadeManager
import nikoblackhearted.themes.motes.combat.BHMoteOnDeathPlugin
import nikoblackhearted.themes.motes.combat.BHMoteRingPlugin
import nikoblackhearted.themes.motes.factors.BHMoteAgentHint
import nikoblackhearted.themes.motes.factors.BHMoteFleetDestructionHint
import nikoblackhearted.themes.motes.factors.BHMoteGenericFactor
import nikoblackhearted.themes.motes.factors.BHMoteMarketActionHint
import org.lazywizard.lazylib.MathUtils.clamp
import org.lwjgl.util.vector.Vector2f
import org.magiclib.kotlin.isPirateFaction
import java.awt.Color
import kotlin.math.roundToInt


class BHMoteThemeIntel: BHThemeMainIntel() {
    companion object {
        const val BASE_CONQUEST_VALUE_PER_SIZE = 20f
        const val VALUABLE_RAID_CREDIT_TO_PROGRESS_MULT = 0.001f
        const val BASE_TACTICAL_BOMBARDMENT_POINTS_PER_SIZE = 5f
        const val BASE_SATURATION_BOMBARDMENT_POINTS_PER_SIZE = 100f
        const val BASE_MAX_SONG_STACKS = 10
        const val MIN_SONG_DECAY_TIME = 88f
        const val MAX_SONG_DECAY_TIME = 92f
        const val STACK_DECAY_MULT = 0.5f
        const val VENGEFUL_AT_PROGRESS_PERCENT = 0.8f
        const val PROGRESS_FRAC_TO_MOTE_RING_MULT = 40f
        const val PROGRESS_FRAC_TO_COMBAT_MOTE_RING_MULT = 1f
        const val PROGRESS_FRAC_TO_MOTE_RING_RESPAWN_MULT = 0.5f

        const val AGENT_DESTABILIZE_BASE_POINTS = 5f
        const val AGENT_SABO_BASE_POINTS = 4f
        const val AGENT_DESTROY_COMM_BASE_POINTS = 1f
        const val AGENT_BASE_REBELLION_POINTS = 10f

        val color = Color(127, 0, 255, 255)

        fun get(): BHMoteThemeIntel? = Global.getSector().intelManager.getFirstIntel(BHMoteThemeIntel::class.java) as? BHMoteThemeIntel
    }

    enum class Stage(val uiName: String) {
        START("") {
            override fun createTooltip(): TooltipMakerAPI.TooltipCreator? {
                return null
            }

            override fun onReached(intel: BHMoteThemeIntel) {
                return
            }
        },

        // enables you to have an ability that spawns a bunch of motes around your fleet
        // (should it be here or later you get the ability to build the industry that makes a market surrounded by motes?)
        // in comibat, surrounds all your ships in motes
        // this one should scale passively based on dread
        MOTE_RINGS("Dancing Stars") {
            override fun createTooltip(): TooltipMakerAPI.TooltipCreator {
                return object : BaseStageTooltip() {
                    override fun createTooltip(
                        tooltip: TooltipMakerAPI,
                        expanded: Boolean,
                        tooltipParam: Any?
                    ) {
                        tooltip.addTitle(uiName)

                        tooltip.addPara(
                            "Grants you an ability which allows you to summon %s in a ring around your fleet. These %s will " +
                            "act as a shield against %s, %s on impact. The %s and %s of your motes %s as this event progresses.",
                            10f,
                            Misc.getHighlightColor(),
                            "motes", "motes", "hostile fleets", "slowing them", "maximum", "regen rate", "increase"
                        ).setHighlightColors(
                            color,
                            color,
                            Misc.getHighlightColor(),
                            Misc.getPositiveHighlightColor(),
                            Misc.getHighlightColor(),
                            Misc.getHighlightColor(),
                            Misc.getPositiveHighlightColor()
                        )

                        tooltip.addPara(
                            "Additionally, grants your %s a %s in-combat. %s.",
                            10f,
                            Misc.getHighlightColor(),
                            "flagship", "very thin mote ring", "This has far weaker scaling"
                        ).setHighlightColors(
                            Misc.getHighlightColor(),
                            Misc.getHighlightColor(),
                            Misc.getNegativeHighlightColor()
                        )
                    }
                }
            }

            override fun onReached(intel: BHMoteThemeIntel) {
                intel.moteRingReached = true
                Global.getSector().characterData.addAbility("BH_moteRing")
            }

            override fun getIcon(): String {
                return "graphics/hullmods/high_volition_attractor.png"
            }
        },

        // enables you to forcefully shoot your motes at a fleet, slowing it and making it blind
        // in combat, gives a mote subsystem
        // also, nex ground ability probably
        MOTE_STRIKE("Starry Shower") {
            override fun createTooltip(): TooltipMakerAPI.TooltipCreator {
                return object : BaseStageTooltip() {
                    override fun createTooltip(
                        tooltip: TooltipMakerAPI,
                        expanded: Boolean,
                        tooltipParam: Any?
                    ) {
                        tooltip.addTitle(uiName)

                        tooltip.addPara(
                            "Grants you an ability which allows you to %s from your %s at %s.",
                            10f,
                            Misc.getHighlightColor(),
                            "launch motes", "mote ring", "hostile fleets"
                        )
                    }
                }
            }

            override fun onReached(intel: BHMoteThemeIntel) {
                intel.moteStrikeReached = true
                Global.getSector().characterData.addAbility("BH_moteStrike")
            }

            override fun getIcon(): String {
                return "graphics/hullmods/quantum_disruptor.png"
            }

        },
        KILL_FLEET("An Opposition") {
            // semi strong kill fleet, easily evadable
            // purpose is to show the sector is becoming wary
            override fun createTooltip(): TooltipMakerAPI.TooltipCreator {
                return object : BaseStageTooltip() {
                    override fun createTooltip(
                        tooltip: TooltipMakerAPI,
                        expanded: Boolean,
                        tooltipParam: Any?
                    ) {
                        tooltip.addTitle(uiName)

                        tooltip.addPara(
                            "A sign of troubles to come.",
                            10f
                        ).color = Misc.getNegativeHighlightColor()
                    }
                }
            }

            override fun onReached(intel: BHMoteThemeIntel) {
                val facId = Factions.LUDDIC_CHURCH
                val playerFleet = Global.getSector().playerFleet

                val enc = DelayedFleetEncounter(
                    null,
                    "BH_moteOpposition"
                )
                enc.setTypes(
                    DelayedFleetEncounter.EncounterType.OUTSIDE_SYSTEM,
                    DelayedFleetEncounter.EncounterType.IN_HYPER_EN_ROUTE,
                )
                if (Global.getSettings().isDevMode) {
                    enc.setDelayNone()
                } else {
                    enc.setDelay(10f, 30f)
                }
                enc.setLocationInnerSector(true, facId)
                enc.setDoNotAbortWhenPlayerFleetTooStrong()

                enc.beginCreate()

                enc.triggerCreateFleet(
                    HubMissionWithTriggers.FleetSize.MEDIUM,
                    HubMissionWithTriggers.FleetQuality.HIGHER,
                    facId,
                    FleetTypes.MERC_PRIVATEER,
                    Vector2f()
                )
                enc.triggerSetFleetCombatFleetPoints(playerFleet.fleetPoints * 1.1f)
                enc.triggerSetFleetAlwaysPursue()
                enc.triggerMakeHostileAndAggressive()
                enc.triggerMakeNoRepImpact()
                enc.triggerOrderFleetInterceptPlayer(true, true)
                enc.triggerSetFleetGenericHailPermanent("BH_moteOpposition")

                enc.endCreate()

                return
            }

            override fun getIcon(): String {
                return Global.getSettings().getSpriteName("events", "stage_unknown_bad")
            }

        },
        UNNERVED("Unnerved") {
            override fun createTooltip(): TooltipMakerAPI.TooltipCreator {
                return object : BaseStageTooltip() {
                    override fun createTooltip(
                        tooltip: TooltipMakerAPI,
                        expanded: Boolean,
                        tooltipParam: Any?
                    ) {
                        tooltip.addTitle(uiName)

                        tooltip.addPara(
                            "Your growing ballet begins to unnerve the simple-minded. Gain a growing %s and %s to %s.",
                            10f,
                            Misc.getNegativeHighlightColor(),
                            "stability", "population growth penalty", "all your colonies"
                        )
                    }
                }
            }

            override fun onReached(intel: BHMoteThemeIntel) {
                intel.unnerved = true
                return
            }

            override fun getIcon(): String {
                return Global.getSettings().getSpriteName("events", "stage_unknown_bad")
            }

        },
        MOTE_MARKETS("Starry Skies") {
            // enables the construction of a structure that spawns motes around your markets
            // bonus ground def and stability and orbital def but very strong
            override fun createTooltip(): TooltipMakerAPI.TooltipCreator {
                return object : BaseStageTooltip() {
                    override fun createTooltip(
                        tooltip: TooltipMakerAPI,
                        expanded: Boolean,
                        tooltipParam: Any?
                    ) {
                        tooltip.addTitle(uiName)

                        tooltip.addPara(
                            "Enables the construction of %s on your colonies, a structure that %s to orbit the colony, as well as " +
                            "suppressing the effects of %s.",
                            10f,
                            Misc.getHighlightColor(),
                            "mote attractors", "invites motes", "Unnerved"
                        ).setHighlightColors(
                            Misc.getHighlightColor(),
                            Misc.getHighlightColor(),
                            Misc.getNegativeHighlightColor()
                        )
                    }
                }
            }

            override fun onReached(intel: BHMoteThemeIntel) {
                Global.getSector().playerFaction.addKnownIndustry(
                    "BH_moteAttractor"
                )

                return
            }
        },
        COMBAT_STRIKE("Friends in the Fire") {
            override fun createTooltip(): TooltipMakerAPI.TooltipCreator {
                return object : BaseStageTooltip() {
                    override fun createTooltip(
                        tooltip: TooltipMakerAPI,
                        expanded: Boolean,
                        tooltipParam: Any?
                    ) {
                        tooltip.addTitle(uiName)

                        tooltip.addPara(
                            "Grants you the ability to %s in combat.",
                            10f,
                            Misc.getHighlightColor(),
                            "control your motes"
                        )
                    }
                }
            }

            override fun onReached(intel: BHMoteThemeIntel) {
                intel.combatMoteStrikeReached = true
                return
            }

        },
        SATBOMB_BONUS("Screaming Songs") {
            override fun createTooltip(): TooltipMakerAPI.TooltipCreator {
                return object : BaseStageTooltip() {
                    override fun createTooltip(
                        tooltip: TooltipMakerAPI,
                        expanded: Boolean,
                        tooltipParam: Any?
                    ) {
                        tooltip.addTitle(uiName)

                        tooltip.addPara(
                            "I hear even more! From the depths of the void of death and ash and decay, " +
                                    "they will finally sing with you! From the ashes, they join the ballet...",
                            10f,
                            Misc.getGrayColor(),
                            Misc.getHighlightColor(),
                        ).italicize()

                        tooltip.addPara(
                            "Receive a stack of %s whenever you perform a %s on a colony %s, one stack per extra size level. " +
                                    "These stacks provide a variety of bonuses, including:",
                            10f,
                            Misc.getHighlightColor(),
                            uiName, "saturation bombardment", "larger than size 3"
                        ).setHighlightColors(
                            Misc.getPositiveHighlightColor(),
                            Misc.getNegativeHighlightColor(),
                            Misc.getHighlightColor()
                        )

                        tooltip.addPara(
                            "   * %s,\n" +
                                    "   * %s,\n" +
                                    "   * %s,\n" +
                                    "And more.",
                            0f,
                            Misc.getHighlightColor(),
                            "Colony stability", "Fleet & Flagship speed", "Improved flagship damage"
                        )

                        tooltip.addPara(
                            "These bonuses last for around %s before a stack will decay. Having multiple stacks will %s per stack. Maximum of %s stacks.",
                            10f,
                            Misc.getPositiveHighlightColor(),
                            "90 days", "increase decay rate by ${((STACK_DECAY_MULT) * 100f).toInt()}%", "$BASE_MAX_SONG_STACKS"
                        ).setHighlightColors(
                            Misc.getHighlightColor(),
                            Misc.getNegativeHighlightColor(),
                            Misc.getHighlightColor()
                        )
                    }

                }
            }

            override fun onReached(intel: BHMoteThemeIntel) {
                intel.screamingSongsActive = true
                Global.getSector().playerPerson.stats.setSkillLevel("BH_moteSkill", 2f)
            }
        },
        CRUSADE_ONE("The Damnable Crusade") {
            // if no colonies, strong kill fleet
            // if colonies, a strong invasion/satbomb on a bunch of your colonies
            // on success, dread is reduced, but stages are kept unlocked
            // lc, probably
            override fun createTooltip(): TooltipMakerAPI.TooltipCreator {
                return object : BaseStageTooltip() {
                    override fun createTooltip(
                        tooltip: TooltipMakerAPI,
                        expanded: Boolean,
                        tooltipParam: Any?
                    ) {
                        tooltip.addTitle(uiName)

                        tooltip.addPara(
                            "Unrest, liberation, and a valiant knight chasing his white whale.",
                            10f
                        ).color = Misc.getNegativeHighlightColor()
                    }
                }
            }

            override fun onReached(intel: BHMoteThemeIntel) {
                BHMoteCrusadeManager.spawnCrusadeOrKillfleet(
                    Global.getSector().playerFleet.fleetPoints * 1.1f,
                    BHMoteCrusadeManager.Stage.ONE
                )
            }

            override fun getIcon(): String {
                return Global.getSettings().getSpriteName("events", "stage_unknown_bad")
            }

        },
        TORMENT_NEXUS("Ballet House") {
            // unlocks the Torment Nexus which passively gives you Screaming Song stacks at the cost of
            // killing!! your!! citizens!!
            override fun createTooltip(): TooltipMakerAPI.TooltipCreator {
                return object : BaseStageTooltip() {
                    override fun createTooltip(
                        tooltip: TooltipMakerAPI,
                        expanded: Boolean,
                        tooltipParam: Any?
                    ) {
                        tooltip.addTitle(uiName)

                        tooltip.addPara(
                            "Unlocks the %s, an %s to the %s that, on top of its normal effects, %s, passively adding %s stacks at the %s.",
                            10f,
                            Misc.getHighlightColor(),
                            "ballet house", "upgrade", "Mote Attractor", "adds your citizens to the chorus of song", "Screaming Song", "cost of their lives"
                        ).setHighlightColors(
                            Misc.getHighlightColor(),
                            Misc.getHighlightColor(),
                            Misc.getHighlightColor(),
                            Misc.getPositiveHighlightColor(),
                            Misc.getPositiveHighlightColor(),
                            Misc.getNegativeHighlightColor()
                        )
                    }
                }
            }

            override fun onReached(intel: BHMoteThemeIntel) {
                Global.getSector().playerFaction.addKnownIndustry(
                    "BH_tormentNexus"
                )
            }
        },
        MOTE_DEATH("From death, Art") {
            // creates mote when you kill a ship, rare chance on a fighter
            // if one of YOUR ships dies it spawns LOTS of motes
            override fun createTooltip(): TooltipMakerAPI.TooltipCreator {
                return object : BaseStageTooltip() {
                    override fun createTooltip(
                        tooltip: TooltipMakerAPI,
                        expanded: Boolean,
                        tooltipParam: Any?
                    ) {
                        tooltip.addTitle(uiName)

                        tooltip.addPara(
                            "When an enemy ship is disabled within range of one of your ships, grants a %s, with larger hull sizes manifesting %s.",
                            10f,
                            Misc.getPositiveHighlightColor(),
                            "small chance for a mote to spawn", "more motes"
                        )

                        tooltip.addPara(
                            "Additionally, spawns a %s whenever one of %s ships are disabled.",
                            10f,
                            Misc.getPositiveHighlightColor(),
                            "mote swarm", "your"
                        ).setHighlightColors(
                            Misc.getPositiveHighlightColor(),
                            Misc.getNegativeHighlightColor()
                        )
                    }
                }
            }

            override fun onReached(intel: BHMoteThemeIntel) {
                intel.moteDeathReached = true
            }

        },
        CRUSADE_TWO("The Infernal Crusade") {
            // much stronger crusade
            // starts rebellions on some of your worlds?
            // invades those worlds?
            // needs special mechanics here, ngl
            override fun createTooltip(): TooltipMakerAPI.TooltipCreator {
                return object : BaseStageTooltip() {
                    override fun createTooltip(
                        tooltip: TooltipMakerAPI,
                        expanded: Boolean,
                        tooltipParam: Any?
                    ) {
                        tooltip.addTitle(uiName)

                        tooltip.addPara(
                            "The second dagger.",
                            10f
                        ).color = Misc.getNegativeHighlightColor()
                    }
                }
            }

            override fun onReached(intel: BHMoteThemeIntel) {
                BHMoteCrusadeManager.spawnCrusadeOrKillfleet(
                    Global.getSector().playerFleet.fleetPoints * 1.2f,
                    BHMoteCrusadeManager.Stage.TWO
                )
            }

            override fun getIcon(): String {
                return Global.getSettings().getSpriteName("events", "stage_unknown_bad")
            }
        },
        // endgame, past here
        ROAMING_MOTES("A Lovely Parade") {
            // once per fight, if your ship dies, siphon hull from nearby ships (your own included) to repair yourself
            override fun createTooltip(): TooltipMakerAPI.TooltipCreator {
                return object : BaseStageTooltip() {
                    override fun createTooltip(
                        tooltip: TooltipMakerAPI,
                        expanded: Boolean,
                        tooltipParam: Any?
                    ) {
                        tooltip.addTitle(uiName)

                        tooltip.addPara(
                            "Your %s will begin spawning %s on colonies with a size %s.",
                            10f,
                            Misc.getHighlightColor(),
                            "mote attractors", "roaming mote swarms", "at or above 5"
                        ).setHighlightColors(
                            Misc.getHighlightColor(),
                            Misc.getPositiveHighlightColor(),
                            Misc.getHighlightColor()
                        )
                    }
                }
            }

            override fun onReached(intel: BHMoteThemeIntel) {
                intel.roamingMotesActive = true
                return
            }
        },
        MOTE_LIFESTEAL("The Eternal Dance") {
            // once per fight, if your ship dies, siphon hull from nearby ships (your own included) to repair yourself
            override fun createTooltip(): TooltipMakerAPI.TooltipCreator {
                return object : BaseStageTooltip() {
                    override fun createTooltip(
                        tooltip: TooltipMakerAPI,
                        expanded: Boolean,
                        tooltipParam: Any?
                    ) {
                        tooltip.addTitle(uiName)

                        tooltip.addPara(
                            "%s, if your flagship would become disabled by damage, %s and %s from nearby ships while dealing %s, prioritizing hostiles over friendlies.",
                            10f,
                            Misc.getHighlightColor(),
                            "Once per combat", "become invincible", "steal hull and armor", "crippling EMP damage"
                        ).setHighlightColors(
                            Misc.getHighlightColor(),
                            Misc.getPositiveHighlightColor(),
                            Misc.getPositiveHighlightColor(),
                            Misc.getPositiveHighlightColor()
                        )
                    }
                }
            }

            override fun onReached(intel: BHMoteThemeIntel) {
                intel.rebirthActive = true
                return
            }
        },
        // the final one
        THE_FINAL_CHARGE("Curtain Call") {
            // the sector doesnt charge at once but you get the final crusade here
            // your biggest world is targetted by an invasion
            // rebellions start sporadically across your entire empire
            // unique fleet, unique mechanics. its a big fight
            // but if you succeed you gain... something rly good? or not? just the end of the event chain?
            override fun createTooltip(): TooltipMakerAPI.TooltipCreator {
                return object : BaseStageTooltip() {
                    override fun createTooltip(
                        tooltip: TooltipMakerAPI,
                        expanded: Boolean,
                        tooltipParam: Any?
                    ) {
                        tooltip.addTitle(uiName)

                        tooltip.addPara(
                            "Unrest rises. A certain hubris rears its ugly head, one last time.",
                            10f
                        ).color = Misc.getNegativeHighlightColor()

                        tooltip.addPara(
                            "BETA NOTE - Spawns invasions, rebellions, and a final encounter with the crusader. " +
                                    "The crusader should have a skill that gives ALL ITS SHIPS one respawn per fight",
                            10f
                        )
                    }
                }
            }

            override fun onReached(intel: BHMoteThemeIntel) {
                BHMoteCrusadeManager.spawnCrusadeOrKillfleet(
                    Global.getSector().playerFleet.fleetPoints * 1.5f,
                    BHMoteCrusadeManager.Stage.THREE
                )
            }

            override fun getIcon(): String {
                return Global.getSettings().getSpriteName("events", "stage_unknown_bad")
            }

        };

        abstract fun createTooltip(): TooltipMakerAPI.TooltipCreator?

        abstract fun onReached(intel: BHMoteThemeIntel)
        open fun getIcon(): String {
            return Global.getSettings().getSpriteName("events", "stage_unknown_neutral")
        }
    }

    enum class FleetType(val fpMult: Float) {
        CIV(1f) {
            override fun createFactor(fp: Int): BaseOneTimeFactor {
                return BHMoteGenericFactor(
                    (fp * fpMult).coerceAtMost(60f).toInt(),
                    "Civilian ships destroyed",
                    "Destroyed $fp FP worth of civilian ships.",
                    "Orphaned eyes scan the skies for any sign."
                )
            }
        },
        OUTCAST(0.1f) {
            override fun createFactor(fp: Int): BaseOneTimeFactor {
                return BHMoteGenericFactor(
                    (fp * fpMult).coerceAtMost(60f).toInt(),
                    "Outcast ships destroyed",
                    "Destroyed $fp FP worth of outcast ships - those who the sector wouldn't mind being ground to dust.",
                    "Blood-stained rustbuckets, a wide birth kept by the scavengers and criminals who understand the danger."
                )
            }
        },
        WAR(0.3f) {
            override fun createFactor(fp: Int): BaseOneTimeFactor {
                return BHMoteGenericFactor(
                    (fp * fpMult).coerceAtMost(60f).toInt(),
                    "War fleets destroyed",
                    "Destroyed $fp FP worth of patrols/warfleets.",
                    "When the starry-flare of your protectors blink out the sky, what do you do? Run. As far and as fast as you can."
                )
            }
        };

        abstract fun createFactor(fp: Int): BaseOneTimeFactor

    }

    var moteRingReached: Boolean = false
    var moteStrikeReached: Boolean = false
    var combatMoteStrikeReached = false
    var fleetwideCombatMotes: Boolean = false
    var screamingSongsActive: Boolean = false
    var moteDeathReached = false
    var unnerved = false
    var rebirthActive = false
    var roamingMotesActive = false
    var currSongStacks = 0
    var maxSongStacks = BASE_MAX_SONG_STACKS
    var extraSongDecayMult = 1f
    val songDecayInterval = IntervalUtil(MIN_SONG_DECAY_TIME, MAX_SONG_DECAY_TIME)

    override fun init() {
        super.init()

        addStage(Stage.START, 0)
        setHideStageWhenPastIt(Stage.START)
        addStage(Stage.MOTE_RINGS, 200)
        locks += LevelLock(300, 3)
        addStage(Stage.MOTE_STRIKE, 400)
        addStage(Stage.KILL_FLEET, 450)
        addStage(Stage.UNNERVED, 550)
        locks += LevelLock(600, 6)
        addStage(Stage.MOTE_MARKETS, 700)
        addStage(Stage.COMBAT_STRIKE, 800)
        addStage(Stage.SATBOMB_BONUS, 1000)
        addStage(Stage.CRUSADE_ONE, 1200)
        locks += MemoryLock(1250, "BHMoteCrusadeOneDefeated", "the crusade is defeated")
        locks += LevelLock(1250, 10)
        addStage(Stage.TORMENT_NEXUS, 1300)
        addStage(Stage.MOTE_DEATH, 1500)
        addStage(Stage.CRUSADE_TWO, 1700)
        locks += MemoryLock(1800, "BHMoteCrusadeTwoDefeated", "the crusade is defeated")
        addStage(Stage.ROAMING_MOTES, 1850)
        locks += LevelLock(2000, 15)
        addStage(Stage.MOTE_LIFESTEAL, 2100)
        addStage(Stage.THE_FINAL_CHARGE, maxProgress)

        addFactor(BHMoteFleetDestructionHint())
        addFactor(BHMoteMarketActionHint())
        if (BHSettings.nexEnabled) {
            addFactor((BHMoteAgentHint()))
        }
    }

    val conditionAddInterval = IntervalUtil(1f, 1.1f)
    override fun advance(amount: Float) {
        super.advance(amount)

        val days = Misc.getDays(amount)
        if (screamingSongsActive) {
            val songDecayMult = getSongDecayMult()
            songDecayInterval.advance(days * songDecayMult)
            if (songDecayInterval.intervalElapsed()) {
                adjustSongStacks(-1)
            }
        }

        if (unnerved || screamingSongsActive) {
            conditionAddInterval.advance(days)
            if (conditionAddInterval.intervalElapsed()) {
                applyConditions()
            }
        }
    }

    private fun applyConditions() {
        for (market in Global.getSector().playerFaction.getMarketsCopy()) {
            if (unnerved) {
                if (!market.hasCondition("BHMoteThemeCondition")) {
                    market.addCondition("BHMoteThemeCondition")
                }
            }
            if (screamingSongsActive) {
                if (!market.hasCondition("BHMoteScreamingCondition")) {
                    market.addCondition("BHMoteScreamingCondition")
                }
            }
        }
    }

    fun adjustSongStacks(adjustment: Int) {
        currSongStacks = clamp(currSongStacks + adjustment, 0, maxSongStacks)

        syncSongStats()
    }

    private fun syncSongStats() {
        val playerFleet = Global.getSector().playerFleet
        playerFleet.stats.fleetwideMaxBurnMod.modifyFlat(
            "BHSingingScreamsBurn",
            currSongStacks.toFloat(),
            "Screaming Songs"
        )
        playerFleet.stats.accelerationMult.unmodify("BHSingingScreamsBurn")
        playerFleet.stats.accelerationMult.modifyFlat(
            "BHSingingScreamsBurn",
            (currSongStacks.toFloat() * 1.2f),
            "Screaming Songs"
        )
    }

    private fun getSongDecayMult(): Float {
        return ((1f + ((currSongStacks - 1f) * STACK_DECAY_MULT)) * extraSongDecayMult)
    }

    override fun addPreEventBar(main: TooltipMakerAPI) {
        val label = main.addPara(
            "How could I have forgotten?! The stars, the eyes, the watching, the MUSIC, THE SOUND, IT MADDENS ME!! MADDENS!! I can barely think... " +
            "and its so... %s...",
            10f,
            Misc.getNegativeHighlightColor(),
            "BEAUTIFUL..."
        )
        label.color = Misc.getGrayColor()
        label.italicize()
        main.addPara(
            "For as long as I could remember, I saw the spots, the stars, the eyes. How would they react... if they saw... too?",
            5f,
            Misc.getGrayColor(),
            Misc.getNegativeHighlightColor()
        ).italicize()
        if (progressFraction >= 0.5f) {
            main.addPara(
                "They all shall dance with me.",
                5f,
                Misc.getGrayColor(),
                Misc.getHighlightColor()
            ).italicize()
        }
    }

    override fun getBarColor(): Color {
        return color
    }

    override fun getTitleColor(mode: IntelInfoPlugin.ListInfoMode?): Color {
        return color
    }

    override fun getBarBracketColor(): Color? {
        return color.brighter()
    }

    override fun getStageIconImpl(stageId: Any?): String? {
        // TODO
        if (stageId !is Stage) return Global.getSettings().getSpriteName("events", "stage_unknown_neutral")
        return stageId.getIcon()
    }

    override fun getStageTooltipImpl(stageId: Any?): TooltipMakerAPI.TooltipCreator? {
        if (stageId !is Stage) return null
        return stageId.createTooltip()
    }

    override fun notifyStageReached(stage: EventStageData?) {
        super.notifyStageReached(stage)
        val entry = stage?.id ?: return
        if (entry !is Stage) return

        entry.onReached(this)
    }

    override fun afterStageDescriptionImpl(main: TooltipMakerAPI) {
        super.afterStageDescriptionImpl(main)

        val rel = (1f - getGlobalRepMax(progressFraction)) * 0.5f
        main.addPara(
            "As you commit atrocities, most human factions will grow to resent you, and have their %s reduced. Currently reduced by %s.",
            10f,
            Misc.getNegativeHighlightColor(),
            "maximum relations", "${(rel * 100f).roundToInt()}%"
        )

        if (screamingSongsActive) {
            main.addPara(
                "You have %s/%s stacks of %s.",
                10f,
                Misc.getHighlightColor(),
                "$currSongStacks", "$maxSongStacks", "screaming songs"
            ).setHighlightColors(
                Misc.getHighlightColor(),
                Misc.getHighlightColor(),
                color
            )
        }
    }

    override fun addBulletPoints(
        info: TooltipMakerAPI?,
        mode: IntelInfoPlugin.ListInfoMode?,
        isUpdate: Boolean,
        tc: Color?,
        initPad: Float
    ) {
        super.addBulletPoints(info, mode, isUpdate, tc, initPad)

        if (!isUpdate) {
            return
        }

        if (listInfoParam is EventStageData) {
            val casted = listInfoParam as EventStageData
            val stage = casted.id as Stage
            info?.addPara(
                "Stage reached: %s",
                0f,
                Misc.getHighlightColor(),
                stage.uiName
            )
            if (!Global.getSettings().isDevMode) {
                /*Global.getSoundPlayer().playUISound(
                    "BH_moteLaughter",
                    1f,
                    1f
                )*/
            }
        }

        if (listInfoParam is ScreamUpdate) {
            val casted = listInfoParam as ScreamUpdate
            info?.setParaOrbitronLarge()
            info?.addPara(
                "THEY SCREAM AND CRY AND DANCE AND SING WITH US~~~~!!",
                10f,
                Misc.getGrayColor(),
                Misc.getGrayColor()
            )?.italicize()
            info?.setParaFontDefault()
            info?.addPara(
                "Gained %s stacks of %s from performing a massacre on %s.",
                10f,
                Misc.getHighlightColor(),
                "${casted.adjustment}", "Screaming Songs", "${casted.market.name}"
            )?.setHighlightColors(
                Misc.getPositiveHighlightColor(),
                color,
                casted.market.faction.baseUIColor
            )
        }
    }

    fun getUnnervedMod(progress: Float = progressFraction): Float {
        val base = 1f

        val mult = 15f * (progress - 0.2f)

        return base * mult
    }

    fun getGlobalRepMax(progress: Float = progressFraction): Float {
        return (1f - (progress * 2f))
    }

    fun getPlayerMaxMotes(): Int {
        if (!moteRingReached) return 0

        return (progressFraction * PROGRESS_FRAC_TO_MOTE_RING_MULT).toInt()
    }

    fun getCombatMaxMotesMult(playerShip: Boolean): Float {
        if (!moteRingReached) return 0f

        var mult = 1f + (progressFraction * PROGRESS_FRAC_TO_COMBAT_MOTE_RING_MULT)
        if (!playerShip) {
            mult *= 0.5f
        }
        return mult.coerceAtLeast(1f)
    }

    fun getPlayerMoteRespawnDelayMult(): Float {
        if (!isStageActive(Stage.MOTE_RINGS)) return 0f

        return 1f - (progressFraction * PROGRESS_FRAC_TO_MOTE_RING_RESPAWN_MULT).toInt()
    }

    fun handleNewRep(progressFraction: Float) {
        BHRepHandler.adjustRepMalus(
            getGlobalRepMax(progressFraction),
            getHatedFactions()
        )
    }

    private fun getHatedFactions(): List<FactionAPI> {
        return Global.getSector().allFactions.filter { !it.isPlayerFaction && it.isShowInIntelTab }
    }

    override fun setProgress(progress: Int) {
        super.setProgress(progress)

        handleNewRep(progressFraction)
        if (progressFraction >= 0.5f && !Global.getSector().memoryWithoutUpdate.getBoolean("\$BHMoteDidOfficerRebellion")) {
            handleOfficerRebellion()
        }
    }

    private fun handleOfficerRebellion() {
        BHMoteOfficerRebellionScript().start()
        Global.getSector().memoryWithoutUpdate.set("\$BHMoteDidOfficerRebellion", true)
    }

    override fun reportBattleOccurred(
        fleet: CampaignFleetAPI?,
        primaryWinner: CampaignFleetAPI?,
        battle: BattleAPI?
    ) {
        if (battle == null) return
        if (!battle.isPlayerInvolved) return

        val involvement = battle.playerInvolvementFraction

        var fpDestroyed = 0f
        var civFpDestroyed = 0f
        var outcastFpDestroyed = 0f
        var warFpDestroyed = 0f
        for (otherFleet in battle.nonPlayerSideSnapshot) {
            if (!otherFleet.faction.isShowInIntelTab) continue
            if (otherFleet.memoryWithoutUpdate.getBoolean(MemFlags.MEMORY_KEY_LOW_REP_IMPACT) || otherFleet.memoryWithoutUpdate.getBoolean(MemFlags.MEMORY_KEY_NO_REP_IMPACT)) return
            val isCiv = otherFleet.isCivilian()
            val isOutcast = otherFleet.isOutcast()
            for (loss in Misc.getSnapshotMembersLost(otherFleet)) {
                val fp = loss.fleetPointCost.toFloat() * involvement
                fpDestroyed += fp

                if (isCiv) {
                    civFpDestroyed += fp
                } else if (isOutcast) {
                    outcastFpDestroyed += fp
                } else {
                    warFpDestroyed += fp
                }
            }
        }

        addFleetDestroyedProgress(civFpDestroyed, FleetType.CIV)
        addFleetDestroyedProgress(outcastFpDestroyed, FleetType.OUTCAST)
        addFleetDestroyedProgress(warFpDestroyed, FleetType.WAR)
    }

    private fun addFleetDestroyedProgress(
        fp: Float,
        type: FleetType
    ) {
        if (fp <= 5f) return

        val factor = type.createFactor(fp.toInt())
        addFactor(factor)
    }

    override fun reportMarketTransferred(
        market: MarketAPI,
        newOwner: FactionAPI,
        oldOwner: FactionAPI,
        playerInvolved: Boolean,
        capture: Boolean
    ) {
        super.reportMarketTransferred(market, newOwner, oldOwner, playerInvolved, capture)

        if (!capture) return
        if (!playerInvolved) return
        if (!newOwner.isPlayerFaction) return

        attemptConqDread(market)
    }

    private fun attemptConqDread(market: MarketAPI) {
        var mult = 1f
        val isOutcast = market.faction.isPirateFaction()
        if (isOutcast) {
            mult *= 0.5f
        }

        val base = (market.size - 2f) * BASE_CONQUEST_VALUE_PER_SIZE
        val final = (base * mult).toInt()
        if (final <= 0) return

        val outcastText = if (isOutcast) " Being outcasts, the sector at large isn't too preterbed." else ""
        val factor = BHMoteGenericFactor(
            final,
            "Conquered ${market.name}",
            "Conquered ${market.name}, a size ${market.size} ${market.faction.displayName} colony.$outcastText",
            "Extermination is unprofitable, not useful, and most important... not very fun. You don't get to see the joy you bring to their faces " +
                    "before they shut their doors in terror. Some day they will all dance with you."
        )
        addFactor(factor)
    }

    override fun reportRaidForValuablesFinishedBeforeCargoShown(
        dialog: InteractionDialogAPI?,
        market: MarketAPI?,
        actionData: MarketCMD.TempData?,
        cargo: CargoAPI?
    ) {
        if (market == null) return
        if (cargo == null) return
        if (market.faction.isPlayerFaction) return

        var mult = 1f
        val isOutcast = market.faction.isPirateFaction()
        if (isOutcast) {
            mult *= 0.25f
        }

        var base = 0f
        for (stack in cargo.stacksCopy) {
            base += (stack.baseValuePerUnit * stack.size) * VALUABLE_RAID_CREDIT_TO_PROGRESS_MULT
        }
        val final = (base * mult).coerceAtMost(50f).toInt()

        val outcastText = if (isOutcast) " Being outcasts, the sector at large isn't too preterbed." else ""
        val factor = BHMoteGenericFactor(
            final,
            "Raided ${market.name} for valuables",
            "Raided ${market.name}, a size ${market.size} ${market.faction.displayName} colony for valuables.$outcastText",
            "New toys for the visionary, new weapons for the ballet."
        )
        addFactor(factor, dialog)
    }

    override fun reportRaidToDisruptFinished(
        dialog: InteractionDialogAPI?,
        market: MarketAPI?,
        actionData: MarketCMD.TempData?,
        industry: Industry?
    ) {
        if (market == null) return
        if (industry == null) return
        if (actionData == null) return
        if (market.faction.isPlayerFaction) return

        var mult = 1f
        val isOutcast = market.faction.isPirateFaction()
        if (isOutcast) {
            mult *= 0.25f
        }

        val base = ((market.size - 2) * 0.3f) + if (industry.spec.hasTag(Industries.TAG_SPACEPORT)) 50f else 10f
        val final = (base * mult).toInt()

        val outcastText = if (isOutcast) " Being outcasts, the sector at large isn't too preterbed." else ""
        val factor = BHMoteGenericFactor(
            final,
            "Disrupted ${industry.currentName} on ${market.name}",
            "Disrupted ${industry.currentName} on ${market.name}, a size ${market.size} ${market.faction.displayName} colony.$outcastText",
            "Smoke and brimstone was all that remained."
        )
        addFactor(factor, dialog)
    }

    override fun reportTacticalBombardmentFinished(
        dialog: InteractionDialogAPI?,
        market: MarketAPI?,
        actionData: MarketCMD.TempData?
    ) {
        if (market == null) return
        if (market.faction.isPlayerFaction) return

        var mult = 1f
        val isOutcast = market.faction.isPirateFaction()
        if (isOutcast) {
            mult *= 0.25f
        }

        val base = BASE_TACTICAL_BOMBARDMENT_POINTS_PER_SIZE * (market.size - 2f)
        val final = (base * mult).toInt()

        val outcastText = if (isOutcast) " Being outcasts, the sector at large isn't too preterbed." else ""
        val factor = BHMoteGenericFactor(
            final,
            "Tactically bombarded ${market.name}",
            "Tactically bombarded ${market.name}, a size ${market.size} ${market.faction.displayName} colony.$outcastText",
            "Knives and blades come out of pockets, delicately dancing without the eyes of guardians."
        )
        addFactor(factor, dialog)
    }

    override fun reportSaturationBombardmentFinished(
        dialog: InteractionDialogAPI?,
        market: MarketAPI?,
        actionData: MarketCMD.TempData?
    ) {
        if (market == null) return
        if (market.faction.isPlayerFaction) return

        val base = BASE_SATURATION_BOMBARDMENT_POINTS_PER_SIZE * (market.size - 3f)
        val final = (base).toInt()
        if (final <= 0f) return

        val factor = BHMoteGenericFactor(
            final,
            "Saturation bombarded ${market.name}",
            "Saturation bombarded ${market.name}, a size ${market.size} ${market.faction.displayName} colony.",
            "Eh-eeeethe giddiness, oh yes oh yes the !music!, from their screams and their beautiful cries you can hear " +
                    "it from here---!!!~~ You spin and dance and cry and sob and laugh and scream---all alongst with their ashes~~~~"
        )
        addFactor(factor, dialog)
        tryMassacreBonus(market, dialog)
    }

    class ScreamUpdate(
        val adjustment: Int,
        val market: MarketAPI
    )

    private fun tryMassacreBonus(market: MarketAPI, dialog: InteractionDialogAPI?) {
        if (!screamingSongsActive) return
        val stacks = getScreamingStacksFrom(market)
        if (stacks <= 0) return

        sendUpdateIfPlayerHasIntel(ScreamUpdate(stacks, market), dialog?.textPanel)
        adjustSongStacks(stacks)
    }

    private fun getScreamingStacksFrom(market: MarketAPI): Int {
        val stacks = market.size - 3
        return stacks.coerceAtLeast(0)
    }

    override fun reportAgentAction(intel: CovertActionIntel) {
        super.reportAgentAction(intel)

        if (!intel.isPlayerInvolved) return

        val result: CovertActionResult = intel.getResult() ?: return
        if (!result.isSuccessful) return

        val def: CovertActionDef = intel.def ?: return
        if (def.id == null) return
        if (def.name == null) return

        var mult = (intel.market.size - 2f)

        var factor: BHMoteGenericFactor? = null

        when (def.id) {
            "destabilizeMarket" -> {
                factor = BHMoteGenericFactor(
                    (AGENT_DESTABILIZE_BASE_POINTS * mult).toInt(),
                    "Agent destabilized ${intel.market.name}",
                    "An agent of your faction destabilized ${intel.market.name}",
                    "Cutting off the head takes a while to grow a new one."
                )
            }

            "sabotageIndustry" -> {
                factor = BHMoteGenericFactor(
                    (AGENT_SABO_BASE_POINTS * mult).toInt(),
                    "Agent sabotaged ${intel.market.name} industries",
                    "An agent of your faction sabotaged ${intel.market.name} infrastructure.",
                    "None of the workers could've predicted what was waiting for them."
                )
            }

            "destroyCommodities" -> {
                factor = BHMoteGenericFactor(
                    (AGENT_DESTROY_COMM_BASE_POINTS * mult).toInt(),
                    "Agent destroyed ${intel.market.name} commodity stocks",
                    "An agent of your faction destroyed ${intel.market.name} commodity stocks",
                    "Let them starve, let them thirst dry. A barren world is one ripe for the picking.."
                )
            }

            "instigateRebellion" -> {
                factor = BHMoteGenericFactor(
                    (AGENT_BASE_REBELLION_POINTS * mult).toInt(),
                    "Agent instigated rebellion on ${intel.market.name}",
                    "An agent of your faction instigated a rebellion on ${intel.market.name}.",
                    "Dancing, fighting, SCREAMING, this is EXACTLY what we want..."
                )
            }
        }

        if (factor == null) return

        addFactor(factor)
    }

    override fun combatInitialized(engine: CombatEngineAPI) {
        if (moteRingReached) {
            engine.addPlugin(BHMoteRingPlugin(this, engine))
        }
        if (moteDeathReached) {
            engine.listenerManager.addListener(BHMoteOnDeathPlugin())
        }
    }
}