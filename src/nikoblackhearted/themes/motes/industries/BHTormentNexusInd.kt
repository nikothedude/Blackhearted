package nikoblackhearted.themes.motes.industries

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.BaseCustomDialogDelegate
import com.fs.starfarer.api.campaign.CustomDialogDelegate.CustomDialogCallback
import com.fs.starfarer.api.campaign.comm.CommMessageAPI
import com.fs.starfarer.api.campaign.econ.Industry
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.campaign.econ.MarketImmigrationModifier
import com.fs.starfarer.api.campaign.listeners.BaseIndustryOptionProvider
import com.fs.starfarer.api.campaign.listeners.DialogCreatorUI
import com.fs.starfarer.api.campaign.listeners.IndustryOptionProvider
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin
import com.fs.starfarer.api.impl.campaign.intel.MessageIntel
import com.fs.starfarer.api.impl.campaign.intel.deciv.DecivTracker
import com.fs.starfarer.api.impl.campaign.population.CoreImmigrationPluginImpl
import com.fs.starfarer.api.impl.campaign.population.PopulationComposition
import com.fs.starfarer.api.ui.CustomPanelAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import lunalib.backend.ui.components.LunaUITextFieldWithSlider
import nikoblackhearted.DialogUtils.getChildrenCopy
import nikoblackhearted.themes.motes.BHMoteThemeIntel
import org.magiclib.kotlin.getImmigrationPlugin
import org.magiclib.kotlin.getMarketSizeProgress

class BHTormentNexusInd: BHMoteAttractorInd(), MarketImmigrationModifier {

    companion object {
        const val DECREMENT_TO_PER_DAY_PROGRESS = 0.5f
    }

    var currentDecrement = 0f
    // 0-1
    var progressToNextStack = 0f

    override fun addPostDemandSection(
        tooltip: TooltipMakerAPI?,
        hasDemand: Boolean,
        mode: Industry.IndustryTooltipMode?
    ) {
        super.addPostDemandSection(tooltip, hasDemand, mode)

        if (tooltip == null) return

        tooltip.addPara(
            "Additionally, %s in exchange for %s stacks. Can %s and %s.",
            10f,
            Misc.getHighlightColor(),
            "decreases population size", "Screaming Song", "lower colony size", "decivilize"
        ).setHighlightColors(
            Misc.getNegativeHighlightColor(),
            Misc.getPositiveHighlightColor(),
            Misc.getNegativeHighlightColor(),
            Misc.getNegativeHighlightColor()
        )

        tooltip.addPara(
            "Current population growth malus: %s. Current stack generation per day: %s.",
            10f,
            Misc.getHighlightColor(),
            "${currentDecrement.toInt()}", "${getStackGen(currentDecrement, 1f)}"
        ).setHighlightColors(
            Misc.getNegativeHighlightColor(),
            Misc.getPositiveHighlightColor()
        )

        if (progressToNextStack > 0f) {
            tooltip.addPara(
                "%s of a stack is currently stored.",
                10f,
                Misc.getPositiveHighlightColor(),
                "${(progressToNextStack * 100f).toInt()}%"
            )
        }
    }

    override fun apply() {
        super.apply()

        TormentNexusOptAdder.init()
        market.addImmigrationModifier(this)
    }

    override fun unapply() {
        super.unapply()

        market.removeImmigrationModifier(this)
    }

    override fun advance(amount: Float) {
        super.advance(amount)

        if (!isFunctional) {
            return
        }

        progressTorment(amount)
    }

    private fun progressTorment(amount: Float) {
        val days = Misc.getDays(amount)
        val incr = getStackGen(currentDecrement, days)
        adjustStackQueue(incr)

        checkSize()
    }

    fun getStackGen(dec: Float, days: Float): Float {
        return ((currentDecrement * 0.01f) * DECREMENT_TO_PER_DAY_PROGRESS) * days
    }

    private fun adjustStackQueue(incr: Float) {
        progressToNextStack += incr
        while (progressToNextStack > 1) {
            progressToNextStack--
            BHMoteThemeIntel.get()?.adjustSongStacks(1)
        }
    }

    private fun checkSize() {
        if (currentDecrement <= 0f) return

        val plugin = market.getImmigrationPlugin()
        val pop = market.population
        val inc = market.incoming
        val min: Float = plugin.getWeightForMarketSize(market.size.toFloat())
        val max: Float = plugin.getWeightForMarketSize((market.size + 1).toFloat())
        val newWeight: Float = pop.weightValue + inc.weightValue

        if (newWeight < min) {
            val newSize = market.size - 1f

            if (newSize < 3) {
                destroyMarket()
            } else {
                val message = MessageIntel(currentName + " at " + market.name, Misc.getBasePlayerColor())
                CoreImmigrationPluginImpl.reduceMarketSize(market)
                message.icon = "graphics/hullmods/high_volition_attractor.png"
                message.addLine("Size reduced to %s", Misc.getTextColor(), arrayOf("${market.size}"),  Misc.getNegativeHighlightColor())
                message.setSound(BaseIntelPlugin.getSoundMajorPosting())
                Global.getSector().campaignUI.addMessage(message, CommMessageAPI.MessageClickAction.COLONY_INFO, market)

                pop.setWeight(plugin.getWeightForMarketSize(market.size + 1f))
                pop.normalize()
            }
        }
    }

    private fun destroyMarket() {
        val message = MessageIntel(currentName + " at " + market.name, Misc.getBasePlayerColor())
        message.icon = "graphics/hullmods/high_volition_attractor.png"
        message.addLine("Colony destroyed", Misc.getNegativeHighlightColor())
        message.setSound(BaseIntelPlugin.getSoundColonyThreat())
        Global.getSector().campaignUI.addMessage(message)

        DecivTracker.decivilize(market, false)
    }

    override fun modifyIncoming(
        market: MarketAPI?,
        incoming: PopulationComposition?
    ) {
        if (market == null || incoming == null) return

        if (market != this.market) return

        incoming.weight.modifyFlat(
            this.spec.id,
            -currentDecrement,
            "$nameForModifier"
        )
    }

    class TormentNexusOptAdder: BaseIndustryOptionProvider() {
        companion object {
            fun init() {
                if (!Global.getSector().listenerManager.hasListener(this)) {
                    Global.getSector().listenerManager.addListener(this, true)
                }
            }
        }

        override fun isUnsuitable(ind: Industry?, allowUnderConstruction: Boolean): Boolean {
            return (ind !is BHTormentNexusInd)
        }

        override fun getIndustryOptions(ind: Industry?): List<IndustryOptionProvider.IndustryOptionData?>? {
            if (ind == null) return null
            if (isUnsuitable(ind, true)) return null

            val opt = IndustryOptionProvider.IndustryOptionData(
                "Change conversion rate...",
                "BHTormentNexusConversionOpt",
                ind,
                this
            )
            opt.color = BHMoteThemeIntel.color

            return arrayListOf(opt)
        }

        override fun createTooltip(
            opt: IndustryOptionProvider.IndustryOptionData?,
            tooltip: TooltipMakerAPI?,
            width: Float
        ) {
            if (opt == null || tooltip == null) return

            when (opt.id) {
                "BHTormentNexusConversionOpt" -> {
                    tooltip.addPara(
                        "Adjust the %s to be %s, granting %s at %s",
                        10f,
                        Misc.getNegativeHighlightColor(),
                        "number of citizens", "added to the dance", "Screaming Song stacks", "cost of their lives"
                    ).setHighlightColors(
                        Misc.getHighlightColor(),
                        BHMoteThemeIntel.color,
                        Misc.getPositiveHighlightColor(),
                        Misc.getNegativeHighlightColor()
                    )
                }
            }
        }

        override fun optionSelected(
            opt: IndustryOptionProvider.IndustryOptionData?,
            ui: DialogCreatorUI?
        ) {
            if (opt == null || ui == null) return

            if (opt.id == "BHTormentNexusConversionOpt") {
                ui.showDialog(
                    TormentNexusDialogDelegate.WIDTH,
                    TormentNexusDialogDelegate.HEIGHT,
                    TormentNexusDialogDelegate(opt.ind as BHTormentNexusInd)
                )
            }
        }

        override fun addToIndustryTooltip(
            ind: Industry?,
            mode: Industry.IndustryTooltipMode?,
            tooltip: TooltipMakerAPI?,
            width: Float,
            expanded: Boolean
        ) {
            return
        }
    }

    class TormentNexusDialogDelegate(val ind: BHTormentNexusInd): BaseCustomDialogDelegate() {
        companion object {
            const val HEIGHT = 80f
            const val WIDTH = 650f
        }

        var basePanel: CustomPanelAPI? = null
        var panel: CustomPanelAPI? = null


        // mostly taken from indevo's ChangelingIndustryDialogueDelegate
        override fun createCustomDialog(panel: CustomPanelAPI?, callback: CustomDialogCallback?) {
            if (panel == null || callback == null) return
            basePanel = panel

            regenerateDialog(callback)
            this.callback = callback
        }
        var callback: CustomDialogCallback? = null

        fun regenerateDialog(callback: CustomDialogCallback) {
            val oldPanel = panel
            if (oldPanel != null) {
                for (entry in oldPanel.getChildrenCopy()) {
                    oldPanel.removeComponent(entry)
                }
                basePanel!!.removeComponent(oldPanel)
            }
            // this panel code is taken from indevo's petmanagerdelegatecode, we want stuff to updaet when the button is pressed
            panel = Global.getSettings().createCustom(basePanel!!.position.width, basePanel!!.position.height, null)
            /*val secondPanel = Global.getSettings().createCustom(basePanel!!.position.width * 0.8f, basePanel!!.position.height * 0.4f, null)
            val contextTooltip = secondPanel.createUIElement(secondPanel!!.position.width, secondPanel.position.height, false)
            contextTooltip.addPara(
                "test", 5f
            )
            secondPanel.addUIElement(contextTooltip).aboveLeft(panel, 0f)*/

            val panelTooltip = panel!!.createUIElement(
                WIDTH,
                HEIGHT, true)

            val slider = LunaUITextFieldWithSlider(
                ind.currentDecrement.toInt(),
                0f,
                50f,
                WIDTH * 0.8f,
                HEIGHT * 0.6f,
                "BH_TormentNexusSlider", "BH_TormentNexusSlider", panel!!, panelTooltip
            )
            slider.onNotHeld { events ->
                if (slider.value?.toFloat() != ind.currentDecrement) {
                    ind.currentDecrement = slider.value!!.toFloat()
                    regenerateDialog(callback)
                }
            }
            slider.position!!.inTMid(0f)
            panelTooltip.addPara(
                "Current pop growth malus: %s. Current Stack generation per day: %s",
                10f,
                Misc.getNegativeHighlightColor(),
                "${ind.currentDecrement.toInt()}", "${ind.getStackGen(ind.currentDecrement, 1f)}"
            ).setHighlightColors(
                Misc.getNegativeHighlightColor(),
                Misc.getPositiveHighlightColor()
            )

            basePanel!!.addComponent(panel!!)
            panel!!.addUIElement(panelTooltip).inTMid(0f)
        }

    }
}