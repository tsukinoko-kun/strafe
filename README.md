# strafe

Minecraft Plugin

Version: Bukkit 1.15.2

[Download](https://github.com/Frank-Mayer/strafe/releases/latest)

[Dokumentation für Datumsformat](https://docs.oracle.com/javase/7/docs/api/java/text/SimpleDateFormat.html)

`./plugins/strafe/strafe.json`

```jsonc
{
  "dateFormat": "dd.MM.yyyy HH:mm:ss z", // Datumsformat
  "banReasons": [ // Liste der Bann Gründe
    {
      "id": "99", // Identifiziert den Grund
      "duration": 31536000000, // Dauer in Millisekunden
      "ban": true, // true -> ban    false -> mute
      "text": "Ban eines Admins" // Text, welcher dem Spieler angezeigt werden soll
    },
    {
      "id": "666",
      "duration": 20000,
      "ban": true,
      "text": "weil du doof bist!"
    }
  ],
  "bannedPlayers": [ // Liste der gebannten Spieler
    {
      "player": "92298abb-7ff4-3682-b9bd-2b556d2feb7e", // UUID des Spielers
      "playerName": "der_kuerbiss", // Nickname des Spielers
      "expire": 1618652194880, // Ablaufzeitpunkt in Millisekunden
      "message": "§r[§cSpielverbot§r] du wurdest vom Spiel §cAusgeschlossen\n§eGrund: §rweil du doof bist!\n§eEnde des Bans: §r 17.04.2021 11:36:34 MESZ\nhttps://kuerbisskraft.de", // Formatierte Kick-Meldung
      "reasonId": "666" // Grund ID
    }
  ],
  "mutedPlayers": [], // Liste gemuteter Spieler (gleiches Format wie beim Bann)
  "kickPrefix": "§r[§cSpielverbot§r] du wurdest vom Spiel §cAusgeschlossen", // Präfix für die Kick-Meldung
  "kickSuffix": "https://kuerbisskraft.de" // Suffix für die Kick-Meldung
}
```
