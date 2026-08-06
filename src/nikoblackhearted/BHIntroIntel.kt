package nikoblackhearted

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin
import com.fs.starfarer.api.impl.campaign.ids.Tags
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin
import com.fs.starfarer.api.ui.Alignment
import com.fs.starfarer.api.ui.CustomPanelAPI
import com.fs.starfarer.api.ui.IntelUIAPI
import com.fs.starfarer.api.ui.SectorMapAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import nikoblackhearted.BHHandler
import nikoblackhearted.themes.Theme

// The core. You pick your option here, and read on what it is youre doing
class BHIntroIntel: BaseIntelPlugin() {

    companion object {
        fun create() {
            val intel = BHIntroIntel()
            Global.getSector().intelManager.addIntel(intel)
        }

        fun get(): IntelInfoPlugin? = Global.getSector().intelManager.getFirstIntel(BHIntroIntel::class.java)
    }

    override fun hasLargeDescription(): Boolean {
        return true
    }

    override fun createLargeDescription(panel: CustomPanelAPI?, width: Float, height: Float) {
        if (panel == null) return

        val mode = BHHandler.getTheme()
        if (mode == null) {
            createChoice(panel, width, height)
            return
        }
    }

    private fun createChoice(panel: CustomPanelAPI, width: Float, height: Float) {
        val main = panel.createUIElement(width, height, true)

        main.setTitleOrbitronVeryLarge()
        main.addTitle("A delightful dream", Misc.getBasePlayerColor())

        main.addPara(
            "The starry, winking sky looms above you as your feet wander the craggy ground beneath. The ivory gates, tall and frigid, " +
            "loom behind you.",
            0f
        ).color = Misc.getGrayColor()
        main.addPara(
            "The bleed from your torn feet pools, forming a thin trail behind you. And yet, the pain only drives you further, further, towards the distant light, " +
            "shining, promising.",
            0f
        ).color = Misc.getGrayColor()
        main.addPara(
            "It bathes you, suffuses you with a distant power you have not felt in ages. Your memory clears - the air stills, then swirls - the fabric of " +
            "dream and realspace sunder and bend and crackle and break-",
            0f,
            Misc.getHighlightColor(),
        ).color = Misc.getGrayColor()
        main.addPara(
            "The light reaches out... it touches you, undoing the seams around your mouth, begging you to speak, asking you the question you've forgotten yourself, asking you, asking you...",
            5f
        ).color = Misc.getGrayColor()

        main.addPara(
            "WHAT ARE YOU?",
            0f
        ).color = Misc.getStoryOptionColor()

        main.addSpacer(5f)
        main.addSectionHeading(
            "Choose your destiny",
            Alignment.MID,
            0f
        )
        main.addSpacer(5f)

        for (entry in Theme.entries) {
            val buttons = entry.addSelection(main, panel, this)
            //button.onClick { this.themeSelected(button.getCustomData(BHHandler.BUTTON_DATA) as Theme, main) }
            main.addSpacer(10f)
        }

        panel.addUIElement(main).inTL(0f, 0f)
    }

    fun themeSelected(theme: Theme) {
        BHHandler.setTheme(theme)
        Global.getSoundPlayer().playUISound(
            "BH_choseDestiny",
            1f,
            1f
        )
        endImmediately()
    }

    override fun buttonPressConfirmed(buttonId: Any?, ui: IntelUIAPI?) {
        super.buttonPressConfirmed(buttonId, ui)

        if (buttonId == null) return
        if (ui == null) return

        if (buttonId is Theme) {
            themeSelected(buttonId)
            ui.recreateIntelUI()
        }
    }

    override fun getName(): String? {
        val name = BHHandler.getTheme()?.getUIName() ?: "Choose your ambition"
        return name
    }

    override fun getIntelTags(map: SectorMapAPI?): Set<String?>? {
        val tags = HashSet<String>()
        if (isImportant) {
            tags.add(Tags.INTEL_IMPORTANT)
        }
        tags += BHHandler.INTEL_KEY
        return tags
    }

    override fun getIcon(): String? {
        val name = BHHandler.getTheme()?.getIcon() ?: "graphics/icons/markets/darkness.png"
        return name
    }

}