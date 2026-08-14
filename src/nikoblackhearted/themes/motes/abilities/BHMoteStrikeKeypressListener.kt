package nikoblackhearted.themes.motes.abilities

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.CoreUITabId
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.campaign.listeners.CampaignInputListener
import com.fs.starfarer.api.input.InputEventAPI
import com.fs.starfarer.api.input.InputEventMouseButton
import com.fs.starfarer.api.input.InputEventType
import com.fs.starfarer.api.util.Misc
import com.fs.starfarer.campaign.CampaignEngine
import com.fs.starfarer.campaign.CampaignState
import com.fs.state.AppDriver
import org.lwjgl.input.Keyboard

class BHMoteStrikeKeypressListener: CampaignInputListener {

    companion object {
        fun get(withUpdate: Boolean): BHMoteStrikeKeypressListener? {
            var listener = Global.getSector().memoryWithoutUpdate["\$BH_moteStrikeListener"] as? BHMoteStrikeKeypressListener
            if (listener == null && withUpdate) {
                listener = BHMoteStrikeKeypressListener()
                Global.getSector().memoryWithoutUpdate["\$BH_moteStrikeListener"] = listener
            }
            return listener
        }
    }

    var active = false

    override fun getListenerInputPriority(): Int {
        return 0
    }

    override fun processCampaignInputPreCore(events: List<InputEventAPI?>?) {
        if (!active) return

        val ui = Global.getSector().campaignUI

        if (ui.currentInteractionDialog != null) {
            deactivate(false)
            return
        }

        for (input in events!!) {
            if (input == null || input.isConsumed) continue

            if (input.isKeyboardEvent && input.eventValue == Keyboard.KEY_ESCAPE || input.eventValue == Keyboard.KEY_LCONTROL) {
                deactivate(true)
                return
            }

            if (input.eventType == InputEventType.MOUSE_DOWN && input.eventValue == InputEventMouseButton.LEFT) {
                //input.consume() // always consume it so we dont move

                val engine = CampaignEngine.getInstance() ?: return
                var target: SectorEntityToken? = engine.mousedOverEntity

                if (target != null) {
                    if (plugin?.canTargetEntity(target) == true) {
                        input.consume()

                        if (plugin?.canFire() == true) {
                            plugin?.currTarget = target as CampaignFleetAPI?
                            plugin?.forceActivation()
                            plugin?.currTarget = null

                            /*Global.getSector().campaignUI.messageDisplay.addMessage(
                            "Missile away!",
                            Misc.getNegativeHighlightColor()
                        )*/
                        } else {
                            Global.getSector().campaignUI.messageDisplay.addMessage(
                                "Cannot launch mote",
                                Misc.getNegativeHighlightColor()
                            )
                        }

                        if (plugin?.canFire() != true) {
                            deactivate(false)
                        }
                    } else {
                        Global.getSoundPlayer().playUISound("ui_button_disabled_pressed", 1f, 1f)
                    }
                }

                return
            }
        }
    }

    override fun processCampaignInputPreFleetControl(events: List<InputEventAPI?>?) {
        return
    }

    override fun processCampaignInputPostCore(events: List<InputEventAPI?>?) {
        return
    }

    var plugin: BHMoteStrikeAbility? = null
    fun activate(plugin: BHMoteStrikeAbility) {
        active = true
        this.plugin = plugin
        Global.getSector().listenerManager.addListener(this, true)
    }
    fun deactivate(withMessage: Boolean) {
        active = false
        plugin = null
        if (withMessage) {
            Global.getSector().campaignUI.messageDisplay.addMessage(
                "Control released",
                Misc.getNegativeHighlightColor()
            )
        }
        Global.getSector().listenerManager.removeListener(this)
    }

}