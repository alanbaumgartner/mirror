@file:Suppress("unused", "unchecked_cast")

package dev.narcos.mirror

import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.full.starProjectedType
import kotlin.reflect.jvm.javaField

fun ClassLoader.mirror(): Mirror = Mirror.from(this)

fun <T : Any> List<KClass<*>>.typed(clazz: KClass<T>): List<KClass<T>> =
    filter { it.isSubclassOf(clazz) }.map { it as KClass<T> }

fun <T : Any> List<KClass<*>>.typed(clazz: Class<*>): List<KClass<T>> =
    filter { clazz.isAssignableFrom(it.java) }.map { it as KClass<T> }

inline fun <reified T : Any> List<KClass<*>>.typed(): List<KClass<T>> = typed(T::class)

inline fun <reified T : Annotation> List<KClass<*>>.annotatedWith(): List<KClass<*>> =
    filter { it.annotations.any { anno -> anno.annotationClass == T::class } }

fun <T : Any> KProperty<*>.typedOrNull(clazz: KClass<T>): KProperty<T>? {
    return if (returnType.isSubtypeOf(clazz.starProjectedType)) {
        this as KProperty<T>
    } else {
        null
    }
}

inline fun <reified T : Any> KProperty<*>.typedOrNull(): KProperty<T>? = typedOrNull(T::class)

fun <T : Any> KProperty<*>.typed(clazz: KClass<T>): KProperty<T> =
    typedOrNull(clazz)
        ?: throw IllegalArgumentException("Property '${this.name}' in ${this.javaField?.declaringClass}, is not of type ${clazz.qualifiedName}, is ${this.returnType}")

inline fun <reified T : Any> KProperty<*>.typed(): KProperty<T> = typed(T::class)
