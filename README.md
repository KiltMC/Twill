# Twill
A small development tool for NeoForge mod developers to work with Fabric. Inspired by Sinytra's [Launchpad](https://modrinth.com/mod/launchpad) project,
which is a similar utility but for Fabric in NeoForge.

Twill is also the new NeoForge mod loader component of [Kilt](https://modrinth.com/mod/kilt), and as such is an essential component
towards it.

Twill provides NeoForge's [FancyModLoader](https://github.com/neoforged/FancyModLoader), and is intended to
replicate its behaviour on Fabric and other potential platforms.

Twill is also significantly smaller in scope compared to Kilt, which allows it to update much faster and for more
versions of Minecraft.

Twill is **not** Kilt, it does not bring the NeoForge API with it, only the FML and EventBus aspect. As such, mods that require
the NeoForge API will not be supported here without Kilt.

## FML Aspects that require Kilt
Due to how FML is designed, several aspects of Twill are unfortunately reliant on Kilt (or alternatively, using Fabric API).
Here's a quick list:
- Registries are not unfrozen automatically, that is handled by both Fabric API and Kilt.
- OpenGL version feature requests are not available, but are handled by Kilt automatically.
- Configuration syncing is not handled, [Forge Config API Port](https://modrinth.com/mod/forge-config-api-port) however will handle this for you.
