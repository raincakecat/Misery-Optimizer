# Misery Optimizer 1.4.3 for Minecraft 1.21.11

Misery Optimizer is a Fabric client mod that makes crystal PvP, anchors, pearls, and explosion terrain respond immediately on high-latency connections. It works without a server plugin. The optional **MiseryOptimizerPaper** plugin adds server feedback for more precise reconciliation.

## Install and settings

Install Fabric Loader **0.18.1 or newer** for **Minecraft 1.21.11** and Fabric API for 1.21.11. Put `MiseryOptimizer-Fabric-1.21.11-1.4.3.jar` in your client `mods` folder and run Java 21 or newer. For server feedback, put `MiseryOptimizer-Paper-1.21.11-1.1.0.jar` in a Paper 1.21.11 server's `plugins` folder. The mod supports Loader 0.18.1 through the current 0.19.5 release; it does not support other Minecraft versions.

Keep only one Misery Optimizer client JAR in the profile. If a launcher still fails, collect that launcher's complete game log or crash report; a loader incompatibility message, mixin error, and missing Fabric API require different fixes.

Press **O** in game to open the settings screen. Rebind it through **Options → Controls → Key Binds → Misery Optimizer → Open Misery Optimizer settings**. Minecraft saves the key assignment in `options.txt`; the settings below save to `config/tung.json` in the game directory:

- Crystal break prediction
- Immediate crystal explosion sound and particle
- Crystal placement preview
- Anchor sound and particle feedback
- Ender-pearl projectile preview and adjustable server handoff (0–9 ticks)
- Explosion block preview
- Early hotbar synchronization before mining
- Faster local correction of rejected block placement and breaking when the Paper plugin is installed
- Local eating animation completion at the normal item duration
- Maximum number of previewed blocks (128–768)
- Extra prediction time (0–2000 ms beyond the ping-based timeout)

Pearl projectile preview is off by default. All other previews and the early slot sync are enabled by default. Existing `config/tung.json` settings remain in effect; turn off pearl preview in the menu if that file already has it enabled. Turning off the explosion block preview restores any still-pending local blocks immediately.

## What happens in play

| Action | Client behavior |
| --- | --- |
| Place an end crystal | A non-targetable visual crystal appears immediately. The server's crystal replaces it, or it disappears when its prediction expires. |
| Break an end crystal | Your targeted crystal is hidden immediately, stops blocking your crosshair, and plays a local explosion effect. The server's removal confirms it; if rejected, it reappears. |
| Use a charged anchor outside the Nether | The central explosion sound and particle play immediately. The matching server effect is suppressed to avoid a duplicate. |
| Throw an ender pearl | A local visual projectile appears immediately. When the server projectile spawns, the visible pearl blends toward it over the configured handoff ticks. The server still controls collision and teleportation. |
| Explode terrain with your crystal or anchor | The client estimates which nearby blocks will break and temporarily shows them as air. Server block packets confirm actual changes. Unconfirmed blocks restore after the prediction timeout. |
| Change hotbar slot and start mining | A pending selected-slot packet is sent before the first mining packet. Minecraft already does this before attacks and item use. |
| Eat food | The client ends the local eating animation after the item's normal use duration, even if a high-latency server has not yet sent completion. The server still controls hunger, item count, healing, and effects. |
| Place or break ordinary blocks | Minecraft already predicts local placement and completed breaking. With the Paper plugin, canceled actions can be restored from the saved client block state as soon as the rejection arrives. |

The block preview uses Minecraft-style explosion rays with a middle-strength estimate. The server's random ray strengths, protection plugins, and other server changes can produce a different block list. Blocks directly supporting or intersecting your player remain untouched by the preview. **Other temporary holes affect the client world, including local collision and targeting, until server packets or the timeout correct them.** If that tradeoff is unwanted, turn off *Explosion block preview* in the menu. Damage, knockback, fire, inventory authority, and pearl teleportation remain server controlled. The mod does not reduce real ping or server tick time.

## What it does not cover

- It does not change actual crystal or anchor explosion calculations, damage, knockback, terrain destruction, or fire. The block preview is visual and can temporarily disagree with the server.
- It does not speed up Paper ticks, chunk loading, networking, or the player's measured ping. A slow server can still delay confirmation and other players' actions.
- It does not alter hit registration, attack cooldown, reach, critical hits, shield behavior, totem activation, or PvP rules.
- It does not predict a pearl's collision or teleport destination. Its early projectile is visual.
- It does not grant food effects, hunger, healing, or an item before server confirmation. The food feature only concerns the local eating animation.
- It does not make ordinary block placement or breaking faster on the server. Minecraft already predicts the local block change; the optional Paper plugin helps undo a rejected change sooner.
- It does not predict explosions from TNT, beds, creepers, or other sources. Explosion previews cover end crystals and charged respawn anchors used outside the Nether.

## Optional Paper feedback

When MiseryOptimizerPaper is installed, the server sends its actual crystal and anchor explosion block list to nearby clients with this mod. The client corrects its local preview to match and still uses ordinary Minecraft block updates as final confirmation. The server also reports canceled crystal hits, anchor uses, crystal placements, pearl throws, ordinary block placement, block breaking, and food consumption so the client can remove or restore a rejected preview sooner. The plugin caps custom feedback bandwidth during explosion spam. It changes no vanilla damage, knockback, explosion terrain, item use, or action ordering. It does not lower ping or optimize Paper's explosion computation. A server event canceled by another plugin may still have an effect that its plugin reports differently; normal packets remain authoritative.

## Verification

`./gradlew clean build` passes the unit tests and produces the remapped installation JAR. Live Fabric 1.21.11 client tests of the 1.4 implementation against Paper 1.21.11 build 132 verified:

- A crystal attack entered the hidden state and received the server's removal.
- A charged anchor previewed nearby blocks and server updates cleared the prediction. A glowstone charge did not trigger an explosion preview; the next charged-anchor click did.
- A preview with no explosion temporarily hid 33 blocks and restored them after timeout.
- A crystal placement showed a local ghost and removed it when the server crystal spawned.
- A pearl showed a local ghost and handed off to the server pearl, including through proxies adding 200 ms and 600 ms of delay in each direction.
- The settings screen opened and its controls wrote the config file.

The companion plugin also loaded on Paper 1.21.11 build 132. A protocol test client received a version 2, 262-block anchor explosion packet from a real anchor use. In a separate Paper session, a test protection plugin canceled ordinary placement, breaking, and golden-apple consumption: the protocol test client received rejection packets for each; server blocks remained unchanged. The 1.4 Fabric client opened the config screen and exercised crystal, anchor, food-use, ordinary block, and pearl code paths against the live server without a mixin failure. The pearl blend completed under both delay settings. Eating started its normal 32-tick timer, but the visual early-finish branch was not separately observed. Client-side visual restoration after a plugin cancels ordinary blocks has not been manually inspected. These are functional checks, not a server-performance benchmark or a guarantee of compatibility with every server plugin.

## Build

Run `./gradlew clean build` with JDK 21 or newer. Install `build/libs/misery-optimizer-fabric-1.4.3.jar` as `MiseryOptimizer-Fabric-1.21.11-1.4.3.jar`; the `-sources.jar` is for developers.

The 1.4.3 source builds and passes tests with both Fabric Loader 0.18.1 and 0.19.5. The packaged release JAR is built against 0.18.1. These build checks do not establish compatibility with every launcher or other installed mod.

License: CC0-1.0.
