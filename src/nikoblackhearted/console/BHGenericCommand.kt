package nikoblackhearted.console

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CampaignEngineLayers
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.combat.ViewportAPI
import com.fs.starfarer.api.graphics.SpriteAPI
import com.fs.starfarer.api.impl.campaign.BaseCustomEntityPlugin
import com.fs.starfarer.api.impl.campaign.ids.Factions
import com.fs.starfarer.api.util.Misc
import nikoblackhearted.entities.MoteSwarmEntityPlugin
import nikoblackhearted.themes.motes.BHMoteCircleScript
import org.lazywizard.console.BaseCommand
import org.lazywizard.lazylib.MathUtils
import org.lazywizard.lazylib.VectorUtils
import org.lwjgl.opengl.GL11
import org.lwjgl.util.vector.Vector2f
import org.magiclib.kotlin.addGlowyParticle
import java.awt.Color
import java.util.*
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class BHGenericCommand: BaseCommand {

    override fun runCommand(
        p0: String,
        p1: BaseCommand.CommandContext
    ): BaseCommand.CommandResult {

        //val entity = Global.getSector().economy.getMarket("jangala").primaryEntity

        BHMoteCircleScript(
            Global.getSector().playerFleet,
            200f,
            6,
            7f,
            MoteSwarmEntityPlugin.MoteSwarmParams(1),
            5f,
            5f
        ).start()

        return BaseCommand.CommandResult.SUCCESS
    }
}