package de.kuerbisskraft.strafe.data

internal class Config(
    val dateFormat: String,
    val banReasons: List<BanConfig>,
    val bannedPlayers: List<BanData>,
    val mutedPlayers: List<BanData>,
    val kickPrefix: String,
    val kickSuffix: String
)
