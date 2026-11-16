# Zyroofficial Mod

Client-side Fabric mod for Minecraft Java **26.2**: money tracker, auction tracker, profit HUD and popup alerts.

**Version 0.2.0 = Phase 2: Money Tracker.** Auction Tracker is Phase 3, full HUD and popups Phase 4, premium GUI Phase 5.

## Requirements (verified for 26.2)

- Java **25**
- Fabric Loader **0.19.3+**
- Fabric API **0.155.2+26.2** (or newer 26.2 build)
- Gradle **9.5.1**, Fabric Loom **1.17**

## Build the mod jar

**Without installing anything:** push this folder to a GitHub repo, open **Actions > Build Zyroofficial Mod > Run workflow**, then download the `zyroofficial-mod` artifact. The jar inside is `zyroofficial-mod-0.2.0.jar` (not the `-sources` one).

**On your computer:** install JDK 25, then in this folder:

```
gradle wrapper --gradle-version 9.5.1
./gradlew build          (Windows: gradlew.bat build)
```

The jar is `build/libs/zyroofficial-mod-0.2.0.jar`. To test in a dev client: `./gradlew runClient`.

## Install

Put the jar (and Fabric API for 26.2, if your launcher does not bundle it) in your mods folder. In Lunar Client: Mods window > drag the jar in.

## Keybinds (Options > Controls > Key Binds > Zyroofficial Mod)

| Action | Default |
|---|---|
| Open Zyroofficial menu | Right Shift |
| Show / hide HUD | H |
| Open Auction Tracker (Phase 3) | O |
| Open Money Statistics | M |
| Move HUD elements (Phase 4) | J |

## Money Tracker (Phase 2)

Open with **M** (or Menu > Money Statistics).

- **Statistics:** total received, sent, won, lost, net profit, transaction volume, incoming and outgoing counts, biggest incoming and biggest outgoing.
  - Net profit = received + won - sent - lost - tax.
  - Volume = received + sent + won + lost (tax is not counted).
  - Incoming / outgoing counts and biggest values use *Received* / *Sent* entries only.
- **Add Entry:** record a Received, Sent, Won, Lost or Tax entry by hand. Amounts accept `1500`, `1,500.50`, `$1500`, `2.5K`, `3M`, `1.2B`.
- **Minimum amount:** set it on the stats screen. Anything below it is ignored (manual and automatic).
- **Reset All:** click twice (second click within 5 seconds). This erases all statistics **and** the history.
- **History:** search (player, note, type, amount), filter by type and by source (manual / chat), sort (newest, oldest, largest, smallest), paging, delete single entries. `*` marks entries detected from chat.

## Automatic chat detection

**No chat formats are built in.** The mod does not guess how any server words its messages. You add rules once you know the real lines.

Open **Auto-Detect** on the stats screen:

- **Recent Messages** shows the latest server system messages (memory only, never saved). Press **Make Rule** on a real line to start a rule from it.
- **Manage Rules**: add, edit, enable/disable, delete. First matching rule wins.
- **Rule editor**: write a regular expression using `{amount}` (required) and `{player}` (optional). A live tester shows whether your pasted sample line matches and what amount and player it extracts.
- **Per-type switches** (received, sent, won, lost, tax) and a **master switch**.
- **Duplicate filter:** ignores an identical line arriving again within N milliseconds (default 1500).
- **Player name pattern** and **amount pattern**: the regexes substituted for `{player}` and `{amount}`; editable.

Only server **system messages** are read. Player chat is ignored on purpose, so another player cannot type a fake "payment" line to corrupt your stats.

Illustration only (not a real server format): `^{player} gave you {amount}$` matches `Steve gave you $1,250`.

## Needs checking on first build

These three 26.2 API names were not confirmed from a page that lists them. If the build fails, they are the first suspects:

1. `ClientReceiveMessageEvents.GAME` (chat receive event, `Component message, boolean overlay`)
2. `EditBox` constructor `(Font, x, y, width, height, Component)` and `setHint`, `setResponder`
3. Import paths such as `net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper`

## Data location

`.minecraft/config/zyroofficial/`: `config.json`, `transactions.json`, `chat-detection.json`.
