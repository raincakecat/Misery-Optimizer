# Modrinth upload sheet: Misery Optimizer 1.4.6

## Upload file and version settings

- Project type: Mod
- Name: Misery Optimizer
- Version number: `1.4.6`
- Version title: `Misery Optimizer 1.4.6`
- Version type: Release
- Game version: **Minecraft 1.21.11 only**
- Loader: **Fabric only**
- Environment: **Client required; server unsupported**
- Main file: [`jars/MiseryOptimizer-Fabric-1.21.11-1.4.6.jar`](jars/MiseryOptimizer-Fabric-1.21.11-1.4.6.jar)
- SHA-256: `89A3391324A4067278A9D48ED02737DD33BBC4B981258118FBF97F6D13F4CFCD`
- Required dependency: Fabric API for Minecraft 1.21.11
- Runtime requirements: Fabric Loader 0.18.1 or newer; Java 21 or newer
- License: CC0-1.0

Upload the Fabric JAR as the main file. The Paper plugin is optional server software with different functionality and should have its own plugin project or version, not be attached as an additional file to this Fabric version.

## Suggested summary

Client-side previews for crystal PvP, anchors, pearls, and eating on high-latency servers.

## Suggested description

Misery Optimizer previews some combat actions on your client before the server confirms them. It can show crystal placement and break effects, charged respawn-anchor feedback outside the Nether, an optional visual ender-pearl projectile, optional explosion terrain outlines, and the end of the local eating animation. Press **O** to change settings.

Pearl prediction and terrain preview are **off by default**. If you enable terrain preview, it uses outlines by default so the preview does not change block collision or targeting. The server still decides damage, knockback, block destruction, food effects, and pearl teleportation. This mod does not reduce ping or server tick time.

Install the Fabric JAR on the client with Fabric API. The optional Paper plugin improves correction of rejected actions and explosion previews; the client mod works without it. Keep only one Misery Optimizer client JAR in the `mods` folder.

## Suggested 1.4.6 changelog

- Terrain preview and its orange outlines are off by default for new configurations.
- Existing saved settings are respected. If terrain preview was previously saved as on, press **O** and turn it off once.
- Includes the 1.4.5 fix for a crash when a world loads with the terrain outline renderer.

## Verification and publishing notes

- The 1.4.6 source builds and passes tests on Fabric Loader 0.18.1 and 0.19.5. Its installable JAR was built against 0.18.1.
- The 1.4.5 rendering code entered a world on both loader versions. In a muted client on 0.18.1, a crystal explosion produced orange outlines that cleared after confirmation. Version 1.4.6 only changes the terrain-preview default; no new live game test was run for 1.4.6.
- The [GitHub repository](https://github.com/raincakecat/Misery-Optimizer) is currently **private**. Do not present it as a public source or issue link unless you choose to make it public.
- This suggested description and changelog were written with AI assistance. If used on the project page, select Modrinth's **AI-generated text** disclosure. Review whether **AI-generated code** or **AI-generated assets** disclosures also apply to the project, and do not use any AI-generated image as the icon or a gallery image. Modrinth may reject projects that are primarily AI-generated.
- The mod changes multiplayer combat feedback. Review Modrinth's [cheats and hacks rule](https://modrinth.com/legal/rules) before submitting; the optional Paper plugin is not a server-side opt-out. Approval is Modrinth's decision.

Modrinth references: [Content Rules](https://modrinth.com/legal/rules), [Content Disclosures](https://support.modrinth.com/en/articles/16567675-content-disclosures), [AI guidance](https://support.modrinth.com/en/articles/16551575-disclosure-and-usage-of-ai), and [Additional Files](https://support.modrinth.com/en/articles/8793363-additional-files).
