package de.kuerbisskraft.strafe

import org.bukkit.Bukkit
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

    fun onTabComplete(command: Command, args: Array<out String>): MutableList<String> {
        val argsSize = args.size
        when (command.name) {
            "ban" -> {
                when (argsSize) {
                    1 -> {
                        val playerNames = mutableListOf<String>()
                        for (player in Bukkit.getOnlinePlayers()) {
                            playerNames.add(player.name)
                        }
                        return playerNames
                    }

                    2 -> {
                        return executioner.askBanIdsStringList()
                    }
                }
            }

            "unban" -> {
                when (argsSize) {
                    1 -> {
                        val playerNames = mutableListOf<String>()
                        for (player in Bukkit.getOnlinePlayers()) {
                            if (executioner.isPlayerBanned(player) || executioner.isPlayerMuted(player) != null) {
                                playerNames.add(player.name)
                            }
                        }
                        for (player in Bukkit.getOfflinePlayers()) {
                            if (executioner.isPlayerBanned(player) || executioner.isPlayerMuted(player) != null) {
                                playerNames.add(player.name ?: continue)
                            }
                        }
                        return playerNames
                    }

                    2 -> {
                        return executioner.getReasonsOfPlayer(args[0])
                    }
                }
            }

            "banedit", "banadd" -> {
                when (argsSize) {
                    1 -> return executioner.askBanIdsStringList()
                    2 -> return mutableListOf("5m", "10m", "30m", "1h", "2h", "1d", "2d")
                    3 -> return mutableListOf("ban", "mute")
                    4 -> return mutableListOf("spaming", "beleidigung", "cheating")
                }
            }

            "bandelete" -> {
                if (argsSize == 1) {
                    return executioner.askBanIdsStringList()
                }
            }
        }

        return mutableListOf()
    }
}
