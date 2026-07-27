package nikoblackhearted.console

import nikoblackhearted.BHHandler
import org.lazywizard.console.BaseCommand
import org.lazywizard.console.Console

class BHBecomeBlackhearted: BaseCommand {
    override fun runCommand(
        args: String,
        context: BaseCommand.CommandContext
    ): BaseCommand.CommandResult {

        if (BHHandler.isEvil()) {
            Console.showMessage("You're already a horrible person!")
            return BaseCommand.CommandResult.SUCCESS
        }

        BHHandler.becomeEvil()

        Console.showMessage("You feel a sense of dread. You should check your intel.")

        return BaseCommand.CommandResult.SUCCESS
    }
}