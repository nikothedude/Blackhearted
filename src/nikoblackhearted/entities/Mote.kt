package nikoblackhearted.entities

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.graphics.SpriteAPI
import org.lwjgl.opengl.GL11
import org.lwjgl.util.vector.Vector2f
import java.awt.Color
import kotlin.math.cos
import kotlin.math.sign
import kotlin.math.sin

abstract class Mote(val color: Color) {

    companion object {
        /// ai gen function, but i did research and understand precisely what it is doing
        fun Vector2f.translateTowardsAngle(angle: Float, factor: Float): Vector2f {
            val radians = Math.toRadians(angle.toDouble())
            // cosine - this is the x coordinate of a point on the perimiter of a circle of size 0 at radian.
            // a radian... hard to explain. look it up. visual aid
            val incrX = factor * cos(radians).toFloat()
            // sine - this is the y coordinate
            val incrY = factor * sin(radians).toFloat()

            // imagine a circle. put a point on it. draw a line up from the horizontal center of the circle til it intersects with the point
            // then, from the origin of that line, draw one to the absolute center of the circle
            // the first line is sin
            // the second is cos

            // point is, when given radians, cos gives the x coord of a point at z angle, and sin gives the y
            return translate(incrX, incrY)
        }

        fun getPermanentMoteSprite(color: Color): List<SpriteAPI> {
            val sprites = ArrayList<SpriteAPI>()

            val spriteOne = Global.getSettings().getSprite("graphics/fx/hit_glow.png")
            val spriteTwo = Global.getSettings().getSprite("graphics/fx/hit_glow.png")
            val spriteThree = Global.getSettings().getSprite("graphics/fx/hit_glow.png")

            spriteOne.setBlendFunc(
                GL11.GL_SRC_ALPHA,
                1
            )
            spriteTwo.setBlendFunc(
                GL11.GL_SRC_ALPHA,
                1
            )
            spriteThree.setBlendFunc(
                GL11.GL_SRC_ALPHA,
                1
            )

            spriteOne.color = color
            spriteTwo.color = color
            spriteThree.color = Color.WHITE

            spriteOne.setSize(30f, 30f)
            spriteTwo.setSize(13f, 13f)
            spriteThree.setSize(5f, 5f)

            spriteOne.alphaMult = 0.4f
            spriteTwo.alphaMult = 0.4f

            sprites.add(0, spriteOne)
            sprites.add(1, spriteTwo)
            sprites.add(2, spriteThree)
            return sprites
        }
    }

    lateinit var location: Vector2f

    @Transient
    lateinit var spriteOne: SpriteAPI
    @Transient
    lateinit var spriteTwo: SpriteAPI
    @Transient
    lateinit var spriteThree: SpriteAPI

    init {
        initializeSprites()
    }

    private fun initializeSprites() {
        val sprites = getPermanentMoteSprite(color)

        spriteOne = sprites[0]
        spriteTwo = sprites[1]
        spriteThree = sprites[2]
    }

    open fun advance(amount: Float) {
        doMovement(amount)
    }

    abstract fun doMovement(amount: Float)
    abstract fun getSpatialLocation(): Vector2f

    fun render() {
        if (!this::spriteOne.isInitialized || spriteOne == null) {
            initializeSprites()
        }
        val loc = getSpatialLocation()

        val oldAlphaOne = spriteOne.alphaMult
        val oldAlphaTwo = spriteTwo.alphaMult
        val oldAlphaThree = spriteThree.alphaMult

        spriteOne.alphaMult *= getAlphaMult()
        spriteOne.alphaMult *= getAlphaMult()
        spriteOne.alphaMult *= getAlphaMult()

        spriteOne.renderAtCenter(loc.x, loc.y)
        spriteTwo.renderAtCenter(loc.x, loc.y)
        spriteThree.renderAtCenter(loc.x, loc.y)

        spriteOne.alphaMult = oldAlphaOne
        spriteOne.alphaMult = oldAlphaTwo
        spriteOne.alphaMult = oldAlphaThree
    }

    open fun getAlphaMult(): Float {
        return 1f
    }
}