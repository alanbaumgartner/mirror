@file:Suppress("unused", "unchecked_cast")

package dev.narcos.mirror

import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

fun ClassLoader.mirror(): Mirror = Mirror.from(this)

fun <T : Any> List<KClass<*>>.typed(clazz: KClass<T>): List<KClass<T>> =
    filter { it.isSubclassOf(clazz) }.map { it as KClass<T> }

fun <T : Any> List<KClass<*>>.typed(clazz: Class<*>): List<KClass<T>> =
    filter { clazz.isAssignableFrom(it.java) }.map { it as KClass<T> }

inline fun <reified T : Any> List<KClass<*>>.typed(): List<KClass<T>> =
    filter { it.isSubclassOf(T::class) }.map { it as KClass<T> }
