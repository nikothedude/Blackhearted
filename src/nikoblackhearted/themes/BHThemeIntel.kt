package nikoblackhearted.themes

import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin
import com.fs.starfarer.api.impl.campaign.ids.Tags
import com.fs.starfarer.api.impl.campaign.intel.events.BaseEventIntel
import com.fs.starfarer.api.impl.campaign.intel.events.EventFactor
import com.fs.starfarer.api.ui.Alignment
import com.fs.starfarer.api.ui.CustomPanelAPI
import com.fs.starfarer.api.ui.SectorMapAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI.TooltipLocation
import com.fs.starfarer.api.util.Misc
import nikoblackhearted.BHHandler
import nikoblackhearted.locks.ProgressLock
import java.awt.Color
import java.util.*
import kotlin.math.max

/** Not just the main intel. Some themes may have multiple event intels. We will make a second layer of abstraction for the main intel. */
abstract class BHThemeIntel: BaseEventIntel() {
    val locks = HashSet<ProgressLock>()

    init {
        init()
    }

    open fun init() {
        progress = 0
        maxProgress = 2500
    }

    override fun setProgress(progress: Int) {
        val newProgress = (progress.coerceAtMost(getMaxAllowedProgress()))

        super.setProgress(newProgress)
    }

    // ovveride to add ONE function call
    override fun createLargeDescription(panel: CustomPanelAPI, width: Float, height: Float) {
        val opad = 10f
        uiWidth = width

        val main = panel.createUIElement(width, height, true)

        main.setTitleOrbitronVeryLarge()
        main.addTitle(getName(), Misc.getBasePlayerColor())

        addPreEventBar(main)

        val bar = main.addEventProgressBar(this, 80f)
        val barTC = getBarTooltip()
        if (barTC != null) {
            main.addTooltipToPrevious(barTC, TooltipLocation.BELOW, false)
        }

        for (curr in stages) {
            if (curr.progress <= 0) continue  // no icon for "starting" stage

            //if (curr.rollData == null || curr.rollData.equals(RANDOM_EVENT_NONE)) continue;
            if (RANDOM_EVENT_NONE == curr.rollData) continue
            if (curr.wasEverReached && curr.isOneOffEvent && !curr.isRepeatable) continue

            if (curr.hideIconWhenPastStageUnlessLastActive && curr.progress <= progress && getLastActiveStage(true) !== curr) {
                continue
            }

            val data = createDisplayData(curr.id)
            val marker = main.addEventStageMarker(data)
            val xOff = bar.getXCoordinateForProgress(curr.progress.toFloat()) - bar.getPosition().getX()
            marker.getPosition().aboveLeft(bar, data.downLineLength).setXAlignOffset(xOff - data.size / 2f - 1)

            val tc = getStageTooltip(curr.id)
            if (tc != null) {
                main.addTooltipTo(tc, marker, TooltipLocation.LEFT, false)
            }
        }


        // progress indicator
        run {
            val marker = main.addEventProgressMarker(this)
            val xOff = bar.getXCoordinateForProgress(progress.toFloat()) - bar.getPosition().getX()
            marker.getPosition().belowLeft(bar, -getBarProgressIndicatorHeight() * 0.5f - 2)
                .setXAlignOffset(xOff - getBarProgressIndicatorWidth() / 2 - 1)
        }

        main.addSpacer(opad)
        main.addSpacer(opad)
        for (curr in stages) {
            if (curr.wasEverReached && curr.isOneOffEvent && !curr.isRepeatable) continue
            addStageDescriptionWithImage(main, curr.id)
        }


        afterStageDescriptions(main)

        val barW = getBarWidth()
        var factorWidth = (barW - opad) / 2f

        if (withMonthlyFactors() != withOneTimeFactors()) {
            //factorWidth = barW;
            factorWidth = (barW * 0.6f).toInt().toFloat()
        }

        val mFac = main.beginSubTooltip(factorWidth)

        val c = getFactionForUIColors().getBaseUIColor()
        val bg = getFactionForUIColors().getDarkUIColor()
        mFac.addSectionHeading("Monthly factors", c, bg, Alignment.MID, opad).getPosition().setXAlignOffset(0f)

        val strW = 40f
        val rh = 20f
        //rh = 15f;
        mFac.beginTable2(
            getFactionForUIColors(), rh, false, false,
            "Monthly factors", factorWidth - strW - 3,
            "Progress", strW
        )

        for (factor in factors) {
            if (factor.isOneTime()) continue
            if (!factor.shouldShow(this)) continue

            val desc = factor.getDesc(this)
            if (desc != null) {
                mFac.addRowWithGlow(
                    Alignment.LMID, factor.getDescColor(this), desc,
                    Alignment.RMID, factor.getProgressColor(this), factor.getProgressStr(this)
                )
                val t = factor.getMainRowTooltip(this)
                if (t != null) {
                    mFac.addTooltipToAddedRow(t, TooltipLocation.RIGHT, false)
                }
            }
            factor.addExtraRows(mFac, this)
        }


        //mFac.addButton("TEST", new String(), factorWidth, 20f, opad);
        mFac.addTable("None", -1, opad)
        mFac.getPrev().getPosition().setXAlignOffset(-5f)

        main.endSubTooltip()

        val oFac = main.beginSubTooltip(factorWidth)

        oFac.addSectionHeading("Recent one-time factors", c, bg, Alignment.MID, opad).getPosition().setXAlignOffset(0f)

        oFac.beginTable2(
            getFactionForUIColors(), 20f, false, false,
            "One-time factors", factorWidth - strW - 3,
            "Progress", strW
        )

        val reversed: MutableList<EventFactor> = ArrayList<EventFactor>(factors)
        Collections.reverse(reversed)
        for (factor in reversed) {
            if (!factor.isOneTime()) continue
            if (!factor.shouldShow(this)) continue

            val desc = factor.getDesc(this)
            if (desc != null) {
                oFac.addRowWithGlow(
                    Alignment.LMID, factor.getDescColor(this), desc,
                    Alignment.RMID, factor.getProgressColor(this), factor.getProgressStr(this)
                )
                val t = factor.getMainRowTooltip(this)
                if (t != null) {
                    oFac.addTooltipToAddedRow(t, TooltipLocation.LEFT)
                }
            }
            factor.addExtraRows(oFac, this)
        }

        oFac.addTable("None", -1, opad)
        oFac.getPrev().getPosition().setXAlignOffset(-5f)
        main.endSubTooltip()


        val factorHeight = max(mFac.getHeightSoFar(), oFac.getHeightSoFar())
        mFac.setHeightSoFar(factorHeight)
        oFac.setHeightSoFar(factorHeight)


        if (withMonthlyFactors() && withOneTimeFactors()) {
            main.addCustom(mFac, opad * 2f)
            main.addCustomDoNotSetPosition(oFac).getPosition().rightOfTop(mFac, opad)
        } else if (withMonthlyFactors()) {
            main.addCustom(mFac, opad * 2f)
        } else if (withOneTimeFactors()) {
            main.addCustom(oFac, opad * 2f)
        }


        //main.addButton("TEST", new String(), factorWidth, 20f, opad);
        panel.addUIElement(main).inTL(0f, 0f)
    }

    open fun addPreEventBar(main: TooltipMakerAPI) {
        return
    }

    override fun afterStageDescriptions(main: TooltipMakerAPI?) {
        super.afterStageDescriptions(main)
        if (main == null) return

        sanitizeLocks()
        afterStageDescriptionImpl(main)
        addLockSection(main)
    }

    open fun afterStageDescriptionImpl(main: TooltipMakerAPI) {
        return
    }

    open fun addLockSection(main: TooltipMakerAPI) {
        val earliest = getEarliestLock() ?: return
        main.setParaInsigniaLarge()
        main.addPara("LOCKED!", 10f).color = Misc.getNegativeHighlightColor()
        main.setParaFontDefault()
        earliest.addLockText(main)
    }

    fun getEarliestLock(): ProgressLock? = locks.minByOrNull { it.getProgressMax(this) }

    fun getMaxAllowedProgress(): Int {
        sanitizeLocks()
        return locks.minOfOrNull { it.getProgressMax(this) } ?: maxProgress
    }

    fun sanitizeLocks() {
        for (lock in locks.toMutableSet()) {
            if (lock.canRemove(this)) {
                removeLock(lock)
            }
        }
    }

    open fun removeLock(lock: ProgressLock) {
        locks -= lock
    }

    override fun addBulletPoints(
        info: TooltipMakerAPI?,
        mode: IntelInfoPlugin.ListInfoMode?,
        isUpdate: Boolean,
        tc: Color?,
        initPad: Float
    ) {
        if (addEventFactorBulletPoints(info, mode, isUpdate, tc, initPad)) {
            return
        }

        super.addBulletPoints(info, mode, isUpdate, tc, initPad)
    }

    override fun getIntelTags(map: SectorMapAPI?): Set<String?>? {
        val tags = HashSet<String>()
        if (isImportant) {
            tags.add(Tags.INTEL_IMPORTANT)
        }
        tags += BHHandler.INTEL_KEY
        return tags
    }
}