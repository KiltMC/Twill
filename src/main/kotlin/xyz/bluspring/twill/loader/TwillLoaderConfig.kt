package xyz.bluspring.twill.loader

import fish.cichlidmc.tinycodecs.api.codec.Codec
import fish.cichlidmc.tinycodecs.api.codec.CompositeCodec
import net.fabricmc.loader.api.FabricLoader
import net.neoforged.neoforgespi.language.IModInfo
import org.apache.maven.artifact.versioning.VersionRange
import xyz.bluspring.knit.loader.mod.ModDependency
import xyz.bluspring.knit.loader.mod.ModEnvironment
import xyz.bluspring.twill.util.enumThrowingFallbackCodec
import xyz.bluspring.twill.util.unboundedMap
import java.nio.file.Path
import java.util.*

@JvmRecord
data class TwillLoaderConfig(
    /**
     * Represents the list of mod IDs that should not be getting resolved by Twill.
     */
    val forceDisabledModIds: List<String> = emptyList(),

    /**
     * Dependency overrides, a Kilt/Twill alternative to https://wiki.fabricmc.net/tutorial:dependency_overrides
     */
    val dependencyOverrides: Map<String, Map<String, ModDependencyOverride>> = emptyMap()
) {
    companion object {
        val PATH: Path = FabricLoader.getInstance().configDir.resolve("twill_overrides.json")
        val CODEC: Codec<TwillLoaderConfig> = CompositeCodec.of(
            Codec.STRING.listOf().optional(emptyList())
                .fieldOf("force_disabled_mods"), TwillLoaderConfig::forceDisabledModIds,
            unboundedMap(Codec.STRING, unboundedMap(Codec.STRING, ModDependencyOverride.CODEC))
                .optional(emptyMap()).fieldOf("dependency_overrides"), TwillLoaderConfig::dependencyOverrides,

            ::TwillLoaderConfig
        ).codec
    }

    data class ModDependencyOverride(
        val version: Optional<VersionRange> = Optional.empty(),
        val type: Optional<ModDependency.Type> = Optional.empty(),
        val side: Optional<ModEnvironment> = Optional.empty(),
        val ordering: Optional<IModInfo.Ordering> = Optional.empty()
    ) {
        companion object {
            val VERSION_CONSTRAINT_CODEC: Codec<VersionRange> = Codec.STRING.xmap(
                VersionRange::createFromVersionSpec,
                VersionRange::toString
            )

            val CODEC: Codec<ModDependencyOverride> = CompositeCodec.of(
                VERSION_CONSTRAINT_CODEC.optional().fieldOf("version"), ModDependencyOverride::version,
                enumThrowingFallbackCodec<ModDependency.Type>().optional().fieldOf("type"), ModDependencyOverride::type,
                enumThrowingFallbackCodec<ModEnvironment>().optional().fieldOf("side"), ModDependencyOverride::side,
                enumThrowingFallbackCodec<IModInfo.Ordering>().optional().fieldOf("ordering"), ModDependencyOverride::ordering,
                ::ModDependencyOverride
            ).codec
        }
    }
}
