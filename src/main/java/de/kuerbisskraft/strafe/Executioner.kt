package de.kuerbisskraft.strafe

import javafx.scene.input.DataFormat
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

internal class Executioner {
    private val banReasonTexts = hashMapOf<String, String>()
    private val banReasonTimes = hashMapOf<String, Long>()
    private val banReasonTypes = hashMapOf<String, Boolean>()

    private val bannedPlayers = mutableListOf<BanData>()
    private val mutedPlayers = mutableListOf<BanData>()

    private val dataFormat = SimpleDateFormat("dd.MM.yyyy HH:mm:ss z")

    init {
        banReasonTexts["3"] = "Harte Beleidigung"
        banReasonTimes["3"] = 3600000L // 1h
        banReasonTypes["3"] = false // mute

        banReasonTexts["4"] = "Werbung für Fremdserver"
        banReasonTimes["4"] = 604800000L // 1d
        banReasonTypes["4"] = true // ban

        banReasonTexts["5"] = "Extremer spam"
        banReasonTimes["5"] = 3600000L // 1h
        banReasonTypes["5"] = false // mute

        banReasonTexts["8"] = "Provokantes Verhalten"
        banReasonTimes["8"] = 3600000L // 1h
        banReasonTypes["8"] = true // ban

        banReasonTexts["16"] = "Kurzer Timeout vom Mod"
        banReasonTimes["16"] = 1800000L // 30m
        banReasonTypes["16"] = true // ban

        banReasonTexts["19"] = "Rassismus (Hitler Skins, Chatnachrichten)"
        banReasonTimes["19"] = 1209600000L // 14d
        banReasonTypes["19"] = true // ban

        banReasonTexts["20"] = "Automatischer Spam durch Bot"
        banReasonTimes["20"] = 432000000L // 5d
        banReasonTypes["20"] = false // mute

        banReasonTexts["70"] = "Ban eines Admins"
        banReasonTimes["70"] = 604800000L // 7d
        banReasonTypes["70"] = true // ban

        banReasonTexts["80"] = "Ban eines Admins"
        banReasonTimes["80"] = 2592000000L // 30d
        banReasonTypes["80"] = true // ban

        banReasonTexts["99"] = "Ban eines Admins"
        banReasonTimes["99"] = 31536000000L // 365d
        banReasonTypes["99"] = true // ban
    }

    // internal

    internal fun askBan(sender: CommandSender, playerName: String, reason: String): Boolean {
        val player = Bukkit.getPlayer(playerName)
        if (player == null) {
            sender.sendMessage("${ChatColor.RED}Spieler '$player' nicht gefunden")
            return false
        }

        val reasonText = getReasonText(reason)
        val expireTime = getReasonTime(reason)
        val resonType = getReasonType(reason)
        if (reasonText == null || expireTime == null || resonType == null) {
            sender.sendMessage("${ChatColor.RED}Invalider Grund '$reason'")
            return false
        }

        if (
            if (resonType) {
                ban(player, reason, Date().time + expireTime, reason)
            } else {
                mute(player, reason, Date().time + expireTime, reason)
            }
        ) {
            sender.sendMessage("${ChatColor.GOLD}Spieler '${player.name}' bestraft: ${timeDisplay(expireTime)}")
            return true
        }

        return false
    }

    internal fun askUnban(sender: CommandSender, player: String, reasonId: String): Boolean {
        return if (unban(player, reasonId)) {
            sender.sendMessage("$player entbannt")
            true
        } else {
            sender.sendMessage("entbannen fehlgeschlagen")
            false
        }
    }

    internal fun askBanList(sender: CommandSender): Boolean {
        val banList = StringBuilder()
        banList.appendLine("${ChatColor.YELLOW}${ChatColor.BOLD}Aktuell gebannt: ${ChatColor.RESET}")
        if (this.bannedPlayers.isEmpty()) {
            banList.appendLine("${ChatColor.GREEN}  keine")
        } else {
            for (p in bannedPlayers) {
                banList.appendLine("${ChatColor.GREEN}  ${p.playerName}${ChatColor.YELLOW}: ${p.message}")
            }
        }
        banList.appendLine("${ChatColor.YELLOW}${ChatColor.BOLD}Aktuell gemuted: ${ChatColor.RESET}")
        if (this.mutedPlayers.isEmpty()) {
            banList.appendLine("${ChatColor.GREEN}  keine")
        } else {
            for (p in mutedPlayers) {
                banList.appendLine("${ChatColor.GREEN}  ${p.playerName}${ChatColor.YELLOW}: ${p.message}")
            }
        }

        sender.sendMessage(banList.toString())

        return true
    }

    internal fun askBanIdList(sender: CommandSender): Boolean {
        val idList = StringBuilder()
        for (el in banReasonTexts) {
            val key = el.key
            val mode = when (banReasonTypes[key]) {
                true -> "BAN"
                false -> "MUTE"
                else -> return false
            }
            val reason = banReasonTexts[key] ?: return false
            idList.appendLine("${ChatColor.DARK_RED}ID: $key ${ChatColor.RED}- ${ChatColor.DARK_RED}$mode ${ChatColor.RED}- ${ChatColor.DARK_RED}$reason")
        }
        sender.sendMessage(idList.toString())
        return true
    }

    internal fun askSetBanId(sender: CommandSender, id: String, reason: String): Boolean {
        return false
    }

    internal fun askDeleteBanId(sender: CommandSender, id: String): Boolean {
        return false
    }

    internal fun applyBan(target: Player): Boolean {
        val id = getPlayerId(target)
        val banData = bannedPlayers.find {
            return it.player == id
        }

        if (banData != null) {
            val banExpire = banData.expire

            if (banExpire > Date().time) {
                target.kickPlayer(banData.message)
                return true
            } else {
                Bukkit.broadcastMessage("${target.name} ban is expired")
                mutedPlayers.remove(banData)
            }
        } else {
            Bukkit.broadcastMessage("${target.name} banData is null")
        }

        return false
    }

    internal fun isBanned(target: Player): Boolean {
        val id = getPlayerId(target)
        val banData = bannedPlayers.find {
            return it.player == id
        }

        if (banData != null) {
            val banExpire = banData.expire

            if (banExpire > Date().time) {
                return true
            } else {
                bannedPlayers.remove(banData)
            }
        }

        return false
    }

    internal fun isPlayerMuted(target: Player): String? {
        val id = getPlayerId(target)

        val muteData = mutedPlayers.find { it.player == id }

        if (muteData != null) {
            val muteExpire = muteData.expire

            if (muteExpire > Date().time) {
                val muteExpireDisplay = dataFormat.format(Date(muteExpire))
                return "${ChatColor.RED}Du bist gemuted bis $muteExpireDisplay"
            } else {
                mutedPlayers.remove(muteData)
            }
        }

        return null
    }

    // private

    private fun ban(player: Player, reason: String, until: Long, reasonId: String): Boolean {
        bannedPlayers.add(BanData(getPlayerId(player), player.name, until, reason, reasonId))
        player.kickPlayer(reason)
        return true
    }

    private fun mute(player: Player, reason: String, until: Long, reasonId: String): Boolean {
        mutedPlayers.add(BanData(getPlayerId(player), player.name, until, reason, reasonId))
        return true
    }

    private fun getReasonText(reasonId: String): String? {
        if (banReasonTexts.containsKey(reasonId) && banReasonTimes.containsKey(reasonId)) {
            return "${banReasonTexts[reasonId]}\n${timeDisplay(banReasonTimes[reasonId]!!)}"
        }

        return null
    }

    private fun getReasonTime(reasonId: String): Long? {
        if (banReasonTimes.containsKey(reasonId)) {
            return banReasonTimes[reasonId]
        }
        return null
    }

    private fun getReasonType(reasonId: String): Boolean? {
        if (banReasonTypes.containsKey(reasonId)) {
            return banReasonTypes[reasonId]
        }
        return null
    }

    private fun timeDisplay(time: Long): String {
        val unit: String
        return "${
            when {
                (time > 86400000) -> {
                    unit = "Tage"
                    time / 86400000.0
                }

                (time > 3600000) -> {
                    unit = "Stunden"
                    time / 3600000.0
                }

                (time > 60000) -> {
                    unit = "Minuten"
                    time / 60000.0
                }

                else -> {
                    unit = "Sekunden"
                    time / 1000.0
                }
            }.roundToInt()
        } $unit"
    }

    private fun getPlayerId(player: Player): String {
        return player.uniqueId.toString()
    }

    private fun unban(player: String, reasonId: String): Boolean {
        val playerId = getPlayerId(Bukkit.getPlayer(player) ?: return false)

        return when (getReasonType(reasonId)) {
            true -> {
                bannedPlayers.removeIf {
                    it.player == playerId
                }
                true
            }

            false -> {
                mutedPlayers.removeIf {
                    it.player == playerId
                }
                true
            }

            else -> false
        }
    }
}
