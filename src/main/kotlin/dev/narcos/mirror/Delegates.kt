package dev.narcos.mirror

import kotlin.properties.ReadOnlyProperty
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KProperty

class ReflectionDelegate<T>(
    private val target: KProperty<T>,
    private vararg val owner: Any?,
) : ReadOnlyProperty<Any?, T> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): T {
        return target.getter.call(*owner)
    }
}

class MutableReflectionDelegate<T>(
    private val target: KMutableProperty<T>,
    private vararg val owner: Any?,
) : ReadWriteProperty<Any?, T> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): T {
        return target.getter.call(*owner)
    }

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        target.setter.call(*owner, value)
    }
}

fun <T> reflect(target: KProperty<T>, owner: Any? = null): ReadOnlyProperty<Any?, T> {
    if (owner == null) {
        return ReflectionDelegate(target)
    }
    return ReflectionDelegate(target, owner)
}

fun <T> reflect(target: KMutableProperty<T>, owner: Any? = null): ReadWriteProperty<Any?, T> {
    if (owner == null) {
        return MutableReflectionDelegate(target)
    }
    return MutableReflectionDelegate(target, owner)
}
