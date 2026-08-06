package nikoblackhearted

import com.fs.starfarer.api.combat.ShipAPI
import org.lazywizard.lazylib.MathUtils

object ShipUtils {
    fun ShipAPI.getFurthestModule(): ShipAPI {
        var moduleWithMaxDist: ShipAPI = this
        var maxDist = 0f

        for (module in childModulesCopy) {
            val dist = MathUtils.getDistance(location, module.location)
            if (dist > maxDist) {
                moduleWithMaxDist = module
                maxDist = dist
            }
        }

        return moduleWithMaxDist
    }

    fun ShipAPI.getCollisionRadWithModules(): Float {
        val furthest = getFurthestModule()
        if (furthest == this) return collisionRadius

        return (collisionRadius + furthest.collisionRadius)
    }
}