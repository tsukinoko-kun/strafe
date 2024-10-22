package de.kuerbisskraft.strafe

import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import de.kuerbisskraft.strafe.data.BanConfig
import de.kuerbisskraft.strafe.data.BanData
import de.kuerbisskraft.strafe.data.Config
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.OfflinePlayer
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Pattern
import kotlin.concurrent.timerTask
import kotlin.math.roundToInt

internal class Executioner {
    private val banReasonTexts = hashMapOf<String, String>()
    private val banReasonTimes = hashMapOf<String, Long>()
    private val banReasonTypes = hashMapOf<String, Boolean>()

    private val bannedPlayers = mutableListOf<BanData>()
    private val mutedPlayers = mutableListOf<BanData>()

    private val parseableDuration = Pattern.compile("\\G[0-9]+[dhms]\\z", Pattern.CASE_INSENSITIVE)
    private val parseableReasonId = Pattern.compile("\\G[a-zA-Z0-9_]+\\z")

    private val kickPrefix: String
    private val kickSuffix: String

    private val dateFormat: SimpleDateFormat

    private val gson = GsonBuilder().setPrettyPrinting().create()
    private val configPath = "plugins/strafe/"
    private val configFilePath = configPath + "strafe.json"
    private val configFile = File(configFilePath)

    private var jsonHash = -1

    private val timer = Timer()

    init {
        val p = File(configPath)
        if (!p.exists()) {
            p.mkdirs()
        }

        if (configFile.exists() && !configFile.isDirectory) {
            val json = configFile.readText(Charsets.UTF_8)
            val type = object : TypeToken<Config>() {}.type
            val import: Config = gson.fromJson(json, type)

            dateFormat = SimpleDateFormat(import.dateFormat)

            kickPrefix = import.kickPrefix
            kickSuffix = import.kickSuffix

            for (el in import.banReasons) {
                setBanId(el.id, el.duration, el.text, el.ban)
            }

            for (el in import.bannedPlayers) {
                bannedPlayers.add(el)
            }

            for (el in import.mutedPlayers) {
                mutedPlayers.add(el)
            }
        } else {
            dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm:ss z")

            kickPrefix =
                "${ChatColor.RESET}[${ChatColor.RED}Spielverbot${ChatColor.RESET}] du wurdest vom Spiel ${ChatColor.RED}Ausgeschlossen"
            kickSuffix = ""

            setBanId("3", 3600000L, "Harte Beleidigung", false)
            setBanId("4", 604800000L, "Werbung für Fremdserver", true)
            setBanId("5", 3600000L, "Extremer spam", false)
            setBanId("8", 3600000L, "Provokantes Verhalten", true)
            setBanId("19", 1209600000L, "Rassismus (Hitler Skins, Chatnachrichten)", true)
            setBanId("20", 432000000L, "Automatischer Spam durch Bot", false)
            setBanId("70", 604800000L, "Bann eines Admins", true)
            setBanId("80", 2592000000L, "Bann eines Admins", true)
            setBanId("99", 31536000000L, "Bann eines Admins", true)
        }

        if (!banReasonTexts.containsKey("16")) {
            setBanId("16", 1800000L, "Kurzer Timeout vom Mod", true)
        }
        if (!banReasonTexts.containsKey("85")) {
            setBanId("85", 2592000000L, "Bann aufgrund eines möglichen Account Hacks", true)
        }

        timer.schedule(timerTask {
            saveConfigToDisc()
        }, 5000, 10000)
    }

    // internal

    internal fun cleanUp() {
        timer.cancel()
        timer.purge()
    }

    internal fun askBan(sender: CommandSender, playerName: String, reason: String): Boolean {
        val allow16 = sender.hasPermission("strafe.16")
        val allow85 = sender.hasPermission("strafe.85")
        if (!sender.hasPermission("strafe.ban") && ((reason == "16" && !allow16) || (reason == "85" && !allow85))) {
            sender.sendMessage(
                ChatColor.RED.toString() +
                    when {
                        allow16 -> {
                            "Du darfst nur die ID 16 verwenden"
                        }
                        allow85 -> {
                            "Du darfst nur die ID 85 verwenden"
                        }
                        else -> {
                            "Dir fehlt die nötige Berechtigung"
                        }
                    }
            )
            return false
        }

        val player = Bukkit.getPlayer(playerName)
        if (player == null) {
            sender.sendMessage("${ChatColor.RED}Spieler '$playerName' nicht gefunden")
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
                ban(player, reasonText, Date().time + expireTime, reason)
            } else {
                mute(player, reasonText, Date().time + expireTime, reason)
            }
        ) {
            sender.sendMessage("${ChatColor.GOLD}Spieler '${player.name}' bestraft: ${timeDisplay(expireTime)}")
            return true
        }

        return false
    }

    internal fun askUnban(sender: CommandSender, player: String, reasonId: String): Boolean {
        return if (unban(player, reasonId)) {
            sender.sendMessage("${ChatColor.GREEN}$player entbannt")
            true
        } else {
            sender.sendMessage("${ChatColor.RED}Entbannen von $player mit bann ID $reasonId fehlgeschlagen")
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
                banList.appendLine("${ChatColor.GREEN}  ${p.playerName}${ChatColor.YELLOW}: ${p.reasonId} - ${p.expire}")
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

        var longestLength = 1
        val idKeyList = mutableListOf<String>()
        for (el in banReasonTypes) {
            idKeyList.add(el.key)
            if (el.key.length > longestLength) {
                longestLength = el.key.length
            }
        }

        idList.appendLine("${ChatColor.DARK_GRAY}[${ChatColor.YELLOW}Strafe${ChatColor.DARK_GRAY}] <spielerName> <grundId>")

        for (key in idKeyList.sortedBy { exp -> exp.padStart(longestLength, '0') }) {
            val mode = when (banReasonTypes[key]) {
                true -> "BAN"
                false -> "MUTE"
                else -> return false
            }
            val reason = banReasonTexts[key] ?: return false
            val dur = this.timeDisplay(banReasonTimes[key] ?: return false)
            idList.appendLine(
                "${ChatColor.DARK_RED}ID: ${
                    key.padStart(
                        longestLength,
                        '0'
                    )
                } ${ChatColor.RED}- ${ChatColor.DARK_RED}$mode ${ChatColor.RED}- ${ChatColor.DARK_RED}$reason ${ChatColor.RED}- ${ChatColor.DARK_RED}$dur"
            )
        }

        sender.sendMessage(idList.toString())
        return true
    }

    internal fun askBanIdsStringList(): MutableList<String> {
        val list = mutableListOf<String>()
        for (el in banReasonTypes) {
            list.add(el.key)
        }
        return list
    }

    internal fun askSetBanId(
        sender: CommandSender,
        id: String,
        duration: String,
        reason: String,
        type: String,
        override: Boolean
    ): Boolean {

        if (!parseableReasonId.matcher(id).matches()) {
            sender.sendMessage("${ChatColor.RED}ID nicht erlaubt")
            return false
        }

        if (askBanIdsStringList().contains(id) && !override) {
            sender.sendMessage("${ChatColor.RED}ID bereits vergeben")
            return false
        }

        if (id == "16" || id == "85") {
            sender.sendMessage("${ChatColor.RED}Diese Bann ID kann nicht bearbeitet werden!")
            return false
        }

        val durationMS = parseDuration(duration) ?: run {
            sender.sendMessage("${ChatColor.RED}Dauer im falschen Format")
            return false
        }

        val ban = when (type.toLowerCase()) {
            "ban" -> true
            "mute" -> false
            else -> return false
        }

        setBanId(id, durationMS, reason, ban)
        sender.sendMessage("${ChatColor.GREEN}Bann ID '${id}' gespeichert")
        return true
    }

    internal fun askDeleteBanId(sender: CommandSender, id: String): Boolean {
        if (id == "16" || id == "85") {
            sender.sendMessage("${ChatColor.RED}Diese Bann ID kann nicht gelöscht werden")
            return false
        }

        return if (banReasonTypes.containsKey(id) && banReasonTexts.containsKey(id) && banReasonTimes.containsKey(id)) {
            banReasonTypes.remove(id)
            banReasonTexts.remove(id)
            banReasonTimes.remove(id)
            sender.sendMessage("${ChatColor.RED}Bann ID '${id}' gelöscht")
            true
        } else {
            true
        }
    }

    internal fun applyBan(target: Player): Boolean {
        val id = getPlayerId(target)
        val banData = bannedPlayers.find { it.player == id }

        if (banData != null) {
            val banExpire = banData.expire

            if (banExpire > Date().time) {
                target.kickPlayer(banData.message)
                return true
            } else {
                mutedPlayers.remove(banData)
            }
        }

        return false
    }

    internal fun isPlayerBanned(target: OfflinePlayer): Boolean {
        val id = getPlayerId(target)
        val banData = bannedPlayers.find { it.player == id }

        if (banData != null) {
            val banExpire = banData.expire

            return if (banExpire > Date().time) {
                true
            } else {
                bannedPlayers.remove(banData)
                isPlayerBanned(target)
            }
        }

        return false
    }


    internal fun isPlayerMuted(target: OfflinePlayer): String? {
        val id = getPlayerId(target)

        val muteData = mutedPlayers.find { it.player == id }

        if (muteData != null) {
            val muteExpire = muteData.expire

            if (muteExpire > Date().time) {
                val muteExpireDisplay = dateFormat.format(Date(muteExpire))
                return "${ChatColor.RED}Du bist gemuted bis $muteExpireDisplay"
            } else {
                mutedPlayers.remove(muteData)
            }
        }

        return null
    }

    internal fun getReasonsOfPlayer(target: String): MutableList<String> {
        val reasons = mutableListOf<String>()

        for (el in bannedPlayers) {
            if (el.playerName == target) {
                reasons.add(el.reasonId)
            }
        }

        for (el in mutedPlayers) {
            if (el.playerName == target) {
                reasons.add(el.reasonId)
            }
        }

        return reasons
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
            val time = dateFormat.format(Date(Date().time + banReasonTimes[reasonId]!!))
            return "${kickPrefix}\n${ChatColor.YELLOW}Grund: ${ChatColor.RESET}${banReasonTexts[reasonId]}\n${ChatColor.YELLOW}Ende des Bans: ${ChatColor.RESET} ${time}\n${kickSuffix}"
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

    private fun getPlayerId(player: OfflinePlayer): String {
        return player.uniqueId.toString()
    }

    private fun unban(player: String, reasonId: String): Boolean {
        val playerId = getPlayerId(Bukkit.getOfflinePlayer(player))

        return when (getReasonType(reasonId)) {
            true -> {
                bannedPlayers.removeIf {
                    it.player == playerId
                }
            }

            false -> {
                mutedPlayers.removeIf {
                    it.player == playerId
                }
            }

            else -> false
        }
    }

    private fun parseDuration(durationString: String): Long? {
        if (!parseableDuration.matcher(durationString).matches()) {
            return null
        }

        val lastCharIndex = durationString.count() - 1

        val value = (durationString.substring(0, lastCharIndex)).toLongOrNull() ?: return null

        return value * when (durationString[lastCharIndex]) {
            'd', 'D' -> 86400000L
            'h', 'H' -> 3600000L
            'm', 'M' -> 60000L
            's', 'S' -> 1000L
            else -> return null
        }
    }

    private fun setBanId(reasonId: String, duration: Long, reasonText: String, ban: Boolean) {
        banReasonTexts[reasonId] = reasonText
        banReasonTimes[reasonId] = duration
        banReasonTypes[reasonId] = ban
    }

    private fun saveConfigToDisc() {
        val bans = mutableListOf<BanConfig>()
        for (el in banReasonTypes) {
            val key = el.key
            bans.add(
                BanConfig(
                    key,
                    banReasonTimes[key] ?: continue,
                    banReasonTypes[key] ?: continue,
                    banReasonTexts[key] ?: continue
                )
            )
        }

        val json =
            gson.toJson(Config(dateFormat.toPattern(), bans, bannedPlayers, mutedPlayers, kickPrefix, kickSuffix))
        val newHash = json.hashCode()
        if (newHash != jsonHash) {
            configFile.writeText(json, Charsets.UTF_8)
            jsonHash = newHash
        }
    }
}
