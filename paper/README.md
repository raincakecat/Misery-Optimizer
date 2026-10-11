# MiseryOptimizerPaper 1.1.0

Optional Paper 1.21.11 companion for the Misery Optimizer 1.4 Fabric client mod.

## Install

Run Paper 1.21.11 with Java 21 or newer. Put `MiseryOptimizer-Paper-1.21.11-1.1.0.jar` in the server `plugins` folder and restart. Players install the Fabric client mod and Fabric API. The client mod still works when this plugin is absent.

The plugin uses Paper's normal combat and explosion code. It sends feedback on `tung:feedback` to clients that register the channel:

- Exact server block lists for crystal and charged-anchor explosions, allowing the client to correct its speculative terrain preview sooner.
- Rejection notices when another plugin cancels a crystal hit, charged-anchor use, crystal placement, ender-pearl use/launch, ordinary block placement, block breaking, or food consumption.

`plugins/MiseryOptimizerPaper/config.yml` controls explosion feedback, the maximum block-list size, the recipient range, and a per-player custom-feedback byte budget per tick. Explosions larger than the configured limit or remaining budget omit the feedback packet; ordinary Minecraft updates still arrive. The defaults are 2048 blocks, 96 blocks of range, and 16 KiB per player per tick. Only the client's configured maximum number of blocks are previewed early.

This is a responsiveness and reconciliation companion, not a server tick optimizer. It avoids constructing explosion feedback when no compatible player is nearby and caps feedback traffic during crystal spam. It does not alter damage, knockback, terrain destruction, fire, inventories, teleportation, or action ordering. It cannot eliminate network round-trip time. If an unrelated plugin mutates an explosion block list after the MONITOR event, its final server block packets still decide the result.

## Verification

`./gradlew clean build` with Java 21 produces the plugin JAR. Paper 1.21.11 build 132 loaded and enabled the plugin. A protocol test client registered the feedback channel, activated a charged respawn anchor in the Overworld, and received an 814-byte explosion feedback packet with 262 block positions. With a test protection plugin, ordinary placement, breaking, and golden-apple consumption were canceled and emitted rejection packets. Server blocks remained unchanged. The Fabric client also connected to the live Paper server and exercised its crystal, anchor, block, food-use, and pearl code paths. Client-side visual restoration after an ordinary block cancellation has not been manually inspected.

Current releases: All Rights Reserved. Earlier CC0 versions keep their original permissions.
