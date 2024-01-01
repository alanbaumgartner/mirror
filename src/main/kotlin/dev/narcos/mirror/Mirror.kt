@file:Suppress("unused")

package dev.narcos.mirror

import java.io.Closeable
import java.net.URL
import kotlin.reflect.KClass

sealed interface ClassPathResource {
    val name: String
    val classLoader: ClassLoader

    companion object {
        fun of(name: String, loader: ClassLoader): ClassPathResource {
            return if (name.endsWith(".class")) {
                ClassInfo(name.toClassName(), loader)
            } else {
                ResourceInfo(name, loader)
            }
        }

        private fun String.toClassName(): String =
            removeSuffix(".class").replace('/', '.')
    }
}

data class ResourceInfo(
    override val name: String,
    override val classLoader: ClassLoader,
) : ClassPathResource {
    fun load(): URL? {
        return classLoader.getResource(name)
    }
}

data class ClassInfo(
    override val name: String,
    override val classLoader: ClassLoader,
) : ClassPathResource {
    fun load(): KClass<*> {
        return classLoader.loadClass(name).kotlin
    }
}

class Mirror private constructor(
    private val classLoader: ClassLoader,
    private val classPathResources: MutableSet<ClassPathResource> = mutableSetOf(),
) : Closeable {

    val resources: List<ResourceInfo>
        get() = classPathResources.filterIsInstance<ResourceInfo>()

    val classes: List<ClassInfo>
        get() = classPathResources.filterIsInstance<ClassInfo>()

    fun loadResources(): List<URL> {
        return resources.mapNotNull(ResourceInfo::load)
    }

    fun loadClasses(onError: (Throwable) -> Unit = {}): List<KClass<*>> {
        return classes.mapNotNull {
            runCatching { it.load() }
                .onFailure(onError)
                .getOrNull()
        }
    }

    companion object {
        fun from(classLoader: ClassLoader): Mirror {
            return Mirror(classLoader, classLoader.getClassPathResources().toMutableSet())
        }
    }

    override fun close() {
        classPathResources.clear()
        if (classLoader is Closeable) {
            classLoader.close()
        }
    }
}
