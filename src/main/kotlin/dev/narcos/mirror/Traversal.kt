package dev.narcos.mirror

import java.io.File
import java.io.IOException
import java.net.MalformedURLException
import java.net.URISyntaxException
import java.net.URL
import java.net.URLClassLoader
import java.nio.file.Files
import java.nio.file.Path
import java.util.jar.JarFile

private val JAVA_CLASS_PATH = System.getProperty("java.class.path")

private typealias ClassMap = MutableMap<ClassLoader, MutableList<String>>

private fun ClassMap.merge(other: ClassMap): ClassMap {
    other.forEach { (t, u) ->
        this.getOrPut(t) { mutableListOf() }.addAll(u)
    }
    return this
}

internal fun ClassLoader.getClassPathResources(): Set<ClassPathResource> {
    return this.getClassPathEntries()
        .entries
        .asSequence()
        .map { (file, classLoader) -> file.traverse(classLoader) }
        .reduce(ClassMap::merge)
        .flatMap { (classLoader, files) ->
            files.map { name -> ClassPathResource.of(name, classLoader) }
        }
        .toSet()
}

private fun File.traverse(classLoader: ClassLoader): ClassMap {
    return when {
        this.isDirectory -> {
            val currentPath: MutableSet<File> = mutableSetOf()
            currentPath.add(this.canonicalFile)
            this.traverseDirectory(classLoader, "", currentPath)
        }

        else -> this.traverseJar(classLoader)
    }
}

private fun File.traverseJar(classLoader: ClassLoader): ClassMap {
    return this.toJarFile()?.use { jarFile ->
        val classes = jarFile.classes().toMutableList()
        return mutableMapOf(classLoader to classes)
    } ?: mutableMapOf()
}

private fun File.toJarFile(): JarFile? {
    return try {
        JarFile(this)
    } catch (e: IOException) {
        null
    }
}

private fun JarFile.classes(): List<String> {
    return this.entries()
        .toList()
        .mapNotNull { entry ->
            if (entry.isDirectory || entry.name == JarFile.MANIFEST_NAME) {
                null
            } else {
                entry.name
            }
        }
}

private fun File.traverseDirectory(
    classLoader: ClassLoader,
    packagePrefix: String,
    currentPath: MutableSet<File>,
): ClassMap {
    return Files.newDirectoryStream(this.toPath())
        .map(Path::toFile)
        .mapNotNull {
            when {
                it.isDirectory -> {
                    val deref = it.canonicalFile
                    if (currentPath.add(deref)) {
                        val map = it.traverseDirectory(classLoader, "$packagePrefix${it.name}/", currentPath)
                        currentPath.remove(deref)
                        map
                    } else {
                        null
                    }
                }

                else -> {
                    val resourceName = packagePrefix + it.name
                    if (resourceName != JarFile.MANIFEST_NAME) {
                        mutableMapOf(classLoader to mutableListOf(resourceName))
                    } else {
                        null
                    }
                }
            }
        }
        .reduce(ClassMap::merge)
}

private fun ClassLoader?.getClassPathEntries(): Map<File, ClassLoader> {
    if (this == null) {
        return mapOf()
    }

    val parentEntries = this.parent.getClassPathEntries()

    val entries = this.getClassLoaderUrls()
        .filter { it.protocol == "file" }
        .associate { it.toFile() to this }

    return parentEntries + entries
}

private fun ClassLoader.getClassLoaderUrls(): List<URL> {
    return when (this) {
        is URLClassLoader -> this.urLs.toList()
        ClassLoader.getSystemClassLoader() -> getJavaClassPathUrls()
        else -> listOf()
    }
}

private fun getJavaClassPathUrls(): List<URL> {
    return JAVA_CLASS_PATH.split(File.pathSeparator)
        .filter { it.isNotBlank() }
        .mapNotNull {
            try {
                File(it).toURI().toURL()
            } catch (e: SecurityException) {
                URL("file", null, File(it).absolutePath)
            } catch (e: MalformedURLException) {
                null
            }
        }
}

private fun URL.toFile(): File {
    return try {
        File(this.toURI())
    } catch (e: URISyntaxException) {
        File(this.path)
    }
}
