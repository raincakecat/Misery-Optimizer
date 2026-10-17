# Modrinth upload sheet: Misery Optimizer 1.4.7

## Minecraft 26.3 build

Upload [`jars/MiseryOptimizer-Fabric-26.3-1.5.1.jar`](jars/MiseryOptimizer-Fabric-26.3-1.5.1.jar) as a **separate Fabric version** numbered `1.5.1`, targeting **Minecraft 26.3 only**. It requires Fabric Loader 0.19.5 or newer, Fabric API for 26.3, and Java 25 or newer. Mod Menu is optional. SHA-256: `4E5048A3F83B81CDCFF2DCBB64EA27FDB0AD3AE6B896FD97FCC61021B237067E`.

The 26.3 source builds, passes its test task, and reached the main menu in a muted Fabric development client. Gameplay features have not yet been exercised in a 26.3 world. The optional Paper plugin below targets 1.21.11 only.

## Upload file and version settings

- Project type: Mod
- Name: Misery Optimizer
- Version number: `1.4.7`
- Version title: `Misery Optimizer 1.4.7`
- Version type: Release
- Game version: **Minecraft 1.21.11 only**
- Loader: **Fabric only**
- Environment: **Client required; server unsupported**
- Main file: [`jars/MiseryOptimizer-Fabric-1.21.11-1.4.7.jar`](jars/MiseryOptimizer-Fabric-1.21.11-1.4.7.jar)
- SHA-256: `636C532E32394A2B62B883804AEEFA5C108EBA7A2EEC156FC3706159BE1185BA`
- Required dependency: Fabric API for Minecraft 1.21.11
- Optional dependency: Mod Menu for Minecraft 1.21.11 (opens the settings screen)
- Runtime requirements: Fabric Loader 0.18.1 or newer; Java 21 or newer
- License: All Rights Reserved (ARR)

Upload the Fabric JAR as the main file. The Paper plugin is optional server software with different functionality and should have its own plugin project or version, not be attached as an additional file to this Fabric version.

## Suggested summary

Client-side previews for crystal PvP, anchors, pearls, and eating on high-latency servers.

## Suggested description

Misery Optimizer previews some combat actions on your client before the server confirms them. It can show crystal placement and break effects, charged respawn-anchor feedback outside the Nether, an optional visual ender-pearl projectile, optional explosion terrain outlines, and the end of the local eating animation. Press **O** to change settings, or use **Mod Menu → Misery Optimizer → Configure** if Mod Menu is installed.

Pearl prediction and terrain preview are **off by default**. The **Performance...** page has four additional optional switches, also off by default: adaptive local explosion effects, a frame budget for terrain scans, adaptive prediction timeout, and a diagnostics screen. If you enable terrain preview, it uses outlines by default so the preview does not change block collision or targeting. The server still decides damage, knockback, block destruction, food effects, and pearl teleportation. This mod does not reduce ping or server tick time.

Install the Fabric JAR on the client with Fabric API. The optional Paper plugin improves correction of rejected actions and explosion previews; the client mod works without it. Keep only one Misery Optimizer client JAR in the `mods` folder.

## Suggested 1.4.7 / 1.5.1 changelog

- Added four optional performance switches: adaptive explosion effects, a terrain-scan frame budget, adaptive prediction timing, and diagnostics. All default to off.
- Added a Mod Menu configuration entry; the **O** key still works without Mod Menu.
- Queued terrain scans stop if exact server feedback arrives before they finish.

## Verification and publishing notes

- The 1.4.7 and 1.5.1 source builds pass their test suites. Earlier 1.21.11 builds were checked with Fabric Loader 0.18.1 and 0.19.5; an earlier 26.3 build reached the main menu. The new performance options have not yet been exercised in a live world.
- The [GitHub repository](https://github.com/raincakecat/Misery-Optimizer) is public and can be used as the source link. Current releases are marked All Rights Reserved; earlier CC0 versions in Git history retain their original permissions.
- This suggested description and changelog were written with AI assistance. If used on the project page, select Modrinth's **AI-generated text** disclosure. Review whether **AI-generated code** or **AI-generated assets** disclosures also apply to the project, and do not use any AI-generated image as the icon or a gallery image. Modrinth may reject projects that are primarily AI-generated.
- The mod changes multiplayer combat feedback. Review Modrinth's [cheats and hacks rule](https://modrinth.com/legal/rules) before submitting; the optional Paper plugin is not a server-side opt-out. Approval is Modrinth's decision.

Modrinth references: [Content Rules](https://modrinth.com/legal/rules), [Content Disclosures](https://support.modrinth.com/en/articles/16567675-content-disclosures), [AI guidance](https://support.modrinth.com/en/articles/16551575-disclosure-and-usage-of-ai), and [Additional Files](https://support.modrinth.com/en/articles/8793363-additional-files).
