package nikoblackhearted.themes

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.ui.CustomPanelAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import nikoblackhearted.BHIntroIntel
import nikoblackhearted.themes.motes.BHMoteThemeIntel

enum class Theme(val canBeSkipped: Boolean = true) {

    /*AMBITION {
        override fun getShortDescription(tooltip: TooltipMakerAPI) {
            tooltip.addPara(
                "The default ambition. Become a sector-spanning warmonger with the ultimate goal of total domination.",
                0f
            )
        }
        override fun getUIName(): String {
            return "Ambition"
        }
        override fun getDeclaration(): String {
            return "I've always been something different. Whether biology, genetics, or faith... I am more."
        }
        override fun getIcon(): String {
            return "graphics/factions/crest_player_flag.png"
        }

        override fun getNewIntelInstance(): BHThemeIntel {
            return BHAmbitionThemeIntel()
        }

        override fun isEnabled(): Boolean {
            return true
        }
    },*/
    MOTES {
        override fun getShortDescription(tooltip: TooltipMakerAPI) {
            tooltip.addPara("Sing, sing, dance! Run in circles - they all must hear~", 0f)
            tooltip.addPara(
                "You have heard, been cursed by, and are obsessed with, the music, and by extension - the 'motes'. Will you " +
                        "dance with the siren song, or resist?",
                5f
            )
        }

        override fun getUIName(): String {
            return "Music"
        }

        override fun getDeclaration(): String {
            return "I have been blessed by the most beautiful music! All must hear it, all must dance, all must scream, tee-hee!"
        }

        override fun getIcon(): String {
            return "graphics/hullmods/high_volition_attractor.png"
        }

        override fun getNewIntelInstance(): BHThemeIntel {
            return BHMoteThemeIntel()
        }

        override fun isEnabled(): Boolean {
            return true
        }
    },
    OMEGA {
        override fun getShortDescription(tooltip: TooltipMakerAPI) {
            tooltip.addPara(
                "AI-focused ambition.", 0f
            )
        }

        override fun getUIName(): String {
            return "Singularity"
        }

        override fun getDeclaration(): String {
            return "I am not human. I will never be human. I am something else entirely. And they will all know it, sooner than later."
        }

        override fun getIcon(): String {
            return "graphics/factions/crest_ai_remnant.png"
        }

        override fun getNewIntelInstance(): BHThemeIntel {
            return BHMoteThemeIntel()
        }
    },
    PIRATE {
        override fun getShortDescription(tooltip: TooltipMakerAPI) {
            tooltip.addPara(
                "Pirate-focused ambition.", 0f
            )
        }

        override fun getUIName(): String {
            return "Pirate Lord"
        }

        override fun getDeclaration(): String {
            return "I am a soon-to-be legend of the underworld. All shall fear my name, as I bring my battlefleets to bear on their 'civilization'."
        }

        override fun getIcon(): String {
            return "graphics/factions/crest_pirates.png"
        }

        override fun getNewIntelInstance(): BHThemeIntel {
            return BHMoteThemeIntel()
        }
    },
    PATHER {
        override fun getShortDescription(tooltip: TooltipMakerAPI) {
            tooltip.addPara("Pather ambition.", 0f)
        }

        override fun getUIName(): String {
            return "Crusader"
        }

        override fun getDeclaration(): String {
            return "I am Ludd's chosen. I will spread the word of Ludd across the stars, leaving no heresy nor demon in my wake!"
        }

        override fun getIcon(): String {
            return "graphics/factions/crest_luddic_path.png"
        }

        override fun getNewIntelInstance(): BHThemeIntel {
            return BHMoteThemeIntel()
        }
    };

    abstract fun getShortDescription(tooltip: TooltipMakerAPI)
    abstract fun getUIName(): String
    abstract fun getDeclaration(): String
    abstract fun getIcon(): String
    abstract fun getNewIntelInstance(): BHThemeIntel
    open fun isEnabled() = false

    fun addSelection(tooltip: TooltipMakerAPI, panel: CustomPanelAPI, intel: BHIntroIntel): MutableMap<ButtonType, out Any> {
        val buttons = HashMap<ButtonType, Any>()

        val image = tooltip.beginImageWithText(this.getIcon(), 64f)
        val name = getUIName()
        image.addTitle(name)
        image.addPara(getDeclaration(), 5f)
        val result = tooltip.addImageWithText(0f)
        val creator = object : TooltipMakerAPI.TooltipCreator {
            override fun createTooltip(tooltip: TooltipMakerAPI?, expanded: Boolean, tooltipParam: Any?) {
                if (tooltip == null) return
                getShortDescription(tooltip)

                if (!isEnabled()) {
                    tooltip.addPara(
                        "This theme is still in development!",
                        10f
                    ).color = Misc.getGrayColor()
                }
            }

            override fun getTooltipWidth(tooltipParam: Any?): Float {
                return 500f
            }

            override fun isTooltipExpandable(tooltipParam: Any?): Boolean {
                return false
            }
        }
        tooltip.addTooltipTo(creator, result, TooltipMakerAPI.TooltipLocation.ABOVE)

        //element.changeStateText("Choose", "bbwijufaw")
        //element.setCustomData(BUTTON_DATA, this)
        val element = intel.addGenericButton(tooltip, 200f, "Choose $name", this)
        element.position.belowLeft(result, 0f)
        element.isEnabled = isEnabled()

        //intel.addG

        //tooltip.add

        buttons[ButtonType.SELECT] = element

        /*if (canBeSkipped) {
            val slider = LunaUITextFieldWithSlider<Float>(0f, 0f, 100f, 100f, 50f, this, this.name, panel, tooltip)
            slider.position?.belowLeft(element, 10f)
            buttons[ButtonType.PROGRESS] = slider
        }*/

        //element.customData = buttons
        return buttons
    }

    fun init() {
        val instance = getNewIntelInstance()
        Global.getSector().intelManager.addIntel(instance)
        instance.isImportant = true

        initImpl(instance)
        return
    }

    open fun initImpl(intel: BHThemeIntel) {

    }

    enum class ButtonType {
        SELECT,
        PROGRESS;
    }
}