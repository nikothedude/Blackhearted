package nikoblackhearted.console

import nikoblackhearted.BHHandler
import org.lazywizard.console.BaseCommand
import org.lazywizard.console.Console

class BHAdjustProgress: BaseCommand {
    override fun runCommand(
        args: String,
        context: BaseCommand.CommandContext
    ): BaseCommand.CommandResult {
        val intel = BHHandler.getThemeIntel()
        if (intel == null) {
            Console.showMessage("You aren't evil!")
            return BaseCommand.CommandResult.ERROR
        }

        if (args.isEmpty()) {
            return BaseCommand.CommandResult.BAD_SYNTAX
        }

        val adjust = args.toFloatOrNull() ?: return BaseCommand.CommandResult.BAD_SYNTAX

        intel.progress = (intel.progress + adjust.toInt())

        return BaseCommand.CommandResult.SUCCESS
    }
}