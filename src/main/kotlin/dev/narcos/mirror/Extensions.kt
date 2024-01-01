@file:Suppress("unused")

package dev.narcos.mirror

import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

fun ClassLoader.mirror(): Mirror = Mirror.from(this)

fun List<KClass<*>>.subTypesOf(clazz: KClass<*>) =
    filter { it.isSubclassOf(clazz) }

fun List<KClass<*>>.subTypesOf(clazz: Class<*>) =
    filter { clazz.isAssignableFrom(it.java) }

inline fun <reified T : Any> List<KClass<*>>.isType() = filterIsInstance<T>()
