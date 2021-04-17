package de.kuerbisskraft.strafe

import org.bukkit.Bukkit.getPluginManager
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.AsyncPlayerChatEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.plugin.java.JavaPlugin

class Strafe : JavaPlugin(), CommandExecutor, TabCompleter, Listener {
    private val executioner = Executioner()
    private val cmdInterpreter = CmdInterpreter(executioner)

    override fun onEnable() {
        getPluginManager().registerEvents(this, this)
        super.onEnable()
    }

    override fun onDisable() {
        executioner.cleanUp()
        super.onDisable()
    }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        return cmdInterpreter.onCommand(sender, command, args)
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>
    ): MutableList<String> {
        return cmdInterpreter.onTabComplete(command, args)
    }

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        if (executioner.applyBan(event.player)) {
            event.joinMessage = null
        }
    }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        if (executioner.isPlayerBanned(event.player)) {
            event.quitMessage = null
        }
    }

    @EventHandler
    fun onChat(event: AsyncPlayerChatEvent) {
        val muted = executioner.isPlayerMuted(event.player)
        if (muted != null) {
            event.player.sendMessage(muted)
            event.isCancelled = true
        }
    }
}
