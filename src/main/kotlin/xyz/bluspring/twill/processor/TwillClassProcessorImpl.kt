package xyz.bluspring.twill.processor

import de.zonlykroks.massasmer.MassASMTransformer
import de.zonlykroks.massasmer.filter.Filters
import kotlinx.io.IOException
import net.neoforged.fml.classloading.transformation.ClassHierarchyRecomputationContext
import net.neoforged.fml.classloading.transformation.ClassProcessorAuditLog
import net.neoforged.fml.classloading.transformation.ClassTransformer
import net.neoforged.neoforgespi.transformation.ClassProcessorIds
import net.neoforged.neoforgespi.transformation.ProcessorName
import xyz.bluspring.twill.loader.TwillLoader

class TwillClassProcessorImpl : Runnable {
    private val auditTrail = ClassProcessorAuditLog()
    private val classTransformer: ClassTransformer by lazy {
        ClassTransformer(TwillLoader.classProcessors, auditTrail)
    }

    private object KnotClassLoaderAccess {
        private val knotClassLoader = Class.forName("net.fabricmc.loader.impl.launch.knot.KnotClassLoader")
        private val findLoadedClassFwd = knotClassLoader.getDeclaredMethod("findLoadedClassFwd", String::class.java)
        val classLoader: ClassLoader = TwillLoader::class.java.classLoader

        fun findLoadedClass(name: String): Class<*>? {
            return findLoadedClassFwd.invoke(this.classLoader, name) as? Class<*>?
        }
    }

    // Otherwise, we end up recursively transforming things, and that's not good.
    private val disallowedPrefixes = listOf(
        "net.fabricmc.api",
        "net.fabricmc.loader",
        "net.neoforged.accesstransformer",
        "net.neoforged.fml",
        "net.neoforged.jarjar",
        "net.neoforged.mergetool",
        "net.neoforged.neoforgespi",
        "net.neoforged.spi",
        "kotlin",
        "kotlinx",
        "org.jetbrains.kotlin",
        "xyz.bluspring.twill.processor",
    )

    override fun run() {
        MassASMTransformer.register("twill:neoforge/class_processor", Filters.not {
            disallowedPrefixes.any { prefix -> it.startsWith("$prefix.") }
        }) { className, classBytes ->
            this.maybeTransformClassBytes(classBytes, className, null)
        }
    }

    private fun maybeTransformClassBytes(classBytes: ByteArray, className: String, upToTransformer: String?): ByteArray {
        return this.classTransformer.transform(classBytes, className, upToTransformer?.let(ProcessorName::parse), object : ClassHierarchyRecomputationContext {
            override fun findLoadedClass(className: String): Class<*>? {
                return KnotClassLoaderAccess.findLoadedClass(className)
            }

            override fun upToFrames(className: String): ByteArray? {
                val context = ClassProcessorIds.COMPUTING_FRAMES.toString()
                // Copy of ModuleClassLoader.getMaybeTransformedClassBytes, except
                // not modularized.
                var bytes = ByteArray(0)
                var suppressed: Throwable? = null

                try {
                    KnotClassLoaderAccess.classLoader.getResourceAsStream("${className.replace('.', '/')}.class")
                        .use { inputStream ->
                            if (inputStream != null) {
                                bytes = inputStream.readAllBytes()
                            }
                        }
                } catch (e: IOException) {
                    suppressed = e
                }

                val maybeTransformed = maybeTransformClassBytes(bytes, className, context)
                if (maybeTransformed.isEmpty()) {
                    val e = ClassNotFoundException(className)
                    if (suppressed != null)
                        e.addSuppressed(suppressed)
                    throw e
                }

                return maybeTransformed
            }

            override fun locateParentClass(className: String): Class<*>? {
                return Class.forName(className, false, KnotClassLoaderAccess.classLoader.parent)
            }
        })
    }
}
