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
