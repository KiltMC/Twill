package xyz.bluspring.twill.loader

import java.io.InputStream
import java.net.URI
import java.nio.file.Path

@JvmRecord
data class BuiltinModDefinition(
    val path: List<Path>,
    val tomlUri: URI?,
    val tomlStream: () -> InputStream,
)
