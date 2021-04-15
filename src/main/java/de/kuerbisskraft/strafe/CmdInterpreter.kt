package de.kuerbisskraft.strafe

import org.bukkit.command.Command
import org.bukkit.command.CommandSender

internal class CmdInterpreter(private val executioner: Executioner) {
    internal fun onCommand(sender: CommandSender, command: Command, args: Array<out String>): Boolean {
        val argsSize = args.size
        when (command.name) {
            "ban" -> {
                if (args.isEmpty()) {
                    return executioner.askBanIdList(sender)
                } else
                if (argsSize >= 2) {
                    return executioner.askBan(sender, args[0], args[1])
                }
            }

            "unban" -> {
                if (argsSize >= 2) {
                    return executioner.askUnban(sender, args[0], args[1])
                }
            }

            "banlist" -> {
                return executioner.askBanList(sender)
            }

            "banedit", "banadd" -> {
                if (argsSize >= 2) {
                    return executioner.askSetBanId(sender, args[0], args[1])
                }
            }

            "bandelete" -> {
                if (args.isNotEmpty()) {
                    return executioner.askDeleteBanId(sender, args[0])
                }
            }
        }

        return false
    }
}
