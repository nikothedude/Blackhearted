package nikoblackhearted

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.FactionSpecAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import exerelin.campaign.backgrounds.BaseCharacterBackground
import exerelin.campaign.backgrounds.CharacterBackgroundIntel
import exerelin.utilities.NexFactionConfig

class BH_backgroundPlugin: BaseCharacterBackground() {

    override fun addTooltipForSelection(
        tooltip: TooltipMakerAPI?,
        factionSpec: FactionSpecAPI?,
        factionConfig: NexFactionConfig?,
        expanded: Boolean?
    ) {
        //super.addTooltipForSelection(tooltip, factionSpec, factionConfig, expanded)
        if (tooltip == null || expanded == null) return

        val hc = Misc.getHighlightColor()
        val tc = Misc.getTextColor()
        val pad = 10f

        val imageTooltip = tooltip.beginImageWithText(spec.iconPath, 40f)
        imageTooltip.addPara(getTitle(factionSpec, factionConfig), 0f, hc, hc)
        imageTooltip.addPara("Hundreds, thousands, than millions will fall before you, raising their hands in terrified, quaking praise. The question is how?", 0f)
        tooltip.addImageWithText(0f)

        if (expanded) {
            tooltip.addPara(
                "Begin an %s. Multiple themes are available, and can be picked by entering the blackhearted event intel.\n" +
                "\n" +
                "Gain points by %s, %s, or other %s - determined by the theme you pick. Gain various bonuses and maluses, such as reduced max reputation with all factions in exchange for faster fleet movement.\n" +
                "\n" +
                "This background may be activated without taking it by the BH_becomeEvil command.",
                10f,
                Misc.getHighlightColor(),
                "evil playthrough", "committing atrocities", "killing civilians", "evil actions", "BH_becomeEvil"
            )
        }

    }

    override fun addTooltipForIntel(
        tooltip: TooltipMakerAPI?,
        factionSpec: FactionSpecAPI?,
        factionConfig: NexFactionConfig?
    ) {
        //super.addTooltipForIntel(tooltip, factionSpec, factionConfig)

        if (tooltip == null) return

        val hc = Misc.getHighlightColor()
        val tc = Misc.getTextColor()
        val pad = 10f

        val imageTooltip = tooltip.beginImageWithText(spec.iconPath, 40f)
        imageTooltip.addPara(getTitle(factionSpec, factionConfig), 0f, hc, hc)
    }

    override fun onNewGameAfterTimePass(factionSpec: FactionSpecAPI?, factionConfig: NexFactionConfig?) {
        super.onNewGameAfterTimePass(factionSpec, factionConfig)

        BHHandler.becomeEvil()
        val intel = Global.getSector().intelManager.getIntel(CharacterBackgroundIntel::class.java).first()
        Global.getSector().intelManager.removeIntel(intel) // we have our own
    }
}