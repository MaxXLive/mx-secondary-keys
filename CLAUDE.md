# MX Secondary Keys – notes for Claude

Client-only Fabric mod for Minecraft 26.3 (Java 25, Mojang names, no obfuscation). Package `net.maxxlive.secondarykeys`.
It adds a second key to every `KeyMapping`. The user writes in German; answer in German. Code, comments, logs and docs are in English.

## Build and test

- Build: `./gradlew build` → `build/libs/MXSecondaryKeys-26.3-dev.jar` (CI passes `-PreleaseVersion=26.3-patch-N`).
- Test client: `./gradlew runClient` (run dir `run/`, secondary keys in `run/config/secondarykeys.txt`).
- The user's CurseForge instance: `C:\Users\max\curseforge\minecraft\Instances\26.3 Fabric\mods`. Copy the jar there only
  when asked, and remove the old MXSecondaryKeys jar first.
- No sources jar for Minecraft is available. To read vanilla code, decompile classes from
  `~/.gradle/caches/fabric-loom/minecraftMaven/net/minecraft/minecraft-merged-deobf/26.3/*.jar` with Vineflower
  (in `~/.gradle/caches/modules-2/files-2.1/org.vineflower`). Do this in the scratchpad, not in the repo.
- 26.x uses SDL3: key codes are SDL scancodes (`InputConstants.KEY_K` = 14). Rendering methods are called `extract*`
  (`extractRenderState`, `extractContent`, `GuiGraphicsExtractor`), and the current screen is `minecraft.gui.screen()` / `gui.setScreen(...)`.

## Architecture

- `mixin/KeyMappingMixin`: the core of the mod.
  - Stores the secondary key in a `@Unique` field, exposed through the duck interface `SecondaryKeyHolder`.
  - On `resetMapping` it also registers each mapping in vanilla's `MAP` under its secondary key, so `click()` and `set()` reach it.
  - It replaces `set()` so that releasing one key keeps the action down while the other key is held, and extends `setAll()` and the `matches*` methods.
- `mixin/ControlsScreenMixin`: adds the "Secondary Keybinds..." button below the Mouse Settings / Key Binds row.
- `screen/SecondaryKeyBindsScreen` + `SecondaryKeyBindsList`: the combined editor (name | primary | secondary | reset). They are modelled on vanilla `KeyBindsScreen` / `KeyBindsList`.
- `SecondaryKeyStore`: reads and writes `config/secondarykeys.txt`. It is loaded on `CLIENT_STARTED`, once every mod's key mappings exist, and saved when the screen closes.
- `SecondaryKeys`: entrypoint (open-screen key K) and static helpers.

## Rules

- Never write secondary keys into `options.txt`. Primary bindings must stay valid when the mod is removed.
- After a change to `KeyMappingMixin`, test in `runClient` that the game starts. Mixins use `defaultRequire = 1`, so a bad
  injection target crashes on class load. `ControlsScreen` only loads when the Controls menu is opened.
- Commits use Conventional Commits (`feat:`, `fix:` …). The release changelog is generated from them, see `.github/scripts/release-info.sh`.
