package nikoblackhearted

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.PersonImportance
import com.fs.starfarer.api.characters.FullName
import com.fs.starfarer.api.characters.PersonAPI
import com.fs.starfarer.api.impl.campaign.ids.Factions
import com.fs.starfarer.api.impl.campaign.ids.Ranks
import com.fs.starfarer.api.impl.campaign.ids.Skills

object BHPeople {

    const val IMPORTANT_PEOPLE_MEM_ID = "\$BH_importantPeople"
    const val MOTES_CRUSADER = "BH_moteCrusader"

    fun getImportantPeople(): HashMap<String, PersonAPI> {
        if (Global.getSector().memoryWithoutUpdate[IMPORTANT_PEOPLE_MEM_ID] == null) {
            Global.getSector().memoryWithoutUpdate[IMPORTANT_PEOPLE_MEM_ID] = HashMap<String, PersonAPI>()
        }
        return Global.getSector().memoryWithoutUpdate[IMPORTANT_PEOPLE_MEM_ID] as HashMap<String, PersonAPI>
    }

    fun createCharacters() {
        val importantPeople = getImportantPeople()

        if (importantPeople[MOTES_CRUSADER] == null) {
            val crusader = Global.getSector().getFaction(Factions.LUDDIC_CHURCH).createRandomPerson(FullName.Gender.MALE)
            crusader.name = FullName("Alexander", "Fullblood", FullName.Gender.MALE)
            crusader.portraitSprite = "graphics/portraits/portrait17.png"
            crusader.importance = PersonImportance.VERY_HIGH
            crusader.postId = Ranks.POST_FLEET_COMMANDER
            crusader.rankId = Ranks.KNIGHT_CAPTAIN
            crusader.relToPlayer.rel = -0.5f

            crusader.stats.level = 7
            crusader.stats.setSkillLevel(Skills.BALLISTIC_MASTERY, 2f)
            crusader.stats.setSkillLevel(Skills.IMPACT_MITIGATION, 2f)
            crusader.stats.setSkillLevel(Skills.TARGET_ANALYSIS, 2f)
            crusader.stats.setSkillLevel(Skills.DAMAGE_CONTROL, 2f)
            crusader.stats.setSkillLevel(Skills.HELMSMANSHIP, 2f)
            crusader.stats.setSkillLevel(Skills.FIELD_MODULATION, 2f)
            crusader.stats.setSkillLevel(Skills.MISSILE_SPECIALIZATION, 2f)
            //todo - better rank and post

            Global.getSector().importantPeople.addPerson(crusader)
            importantPeople[MOTES_CRUSADER] = crusader
        }
    }
}