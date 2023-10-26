package hu.webhejj.perspektive

import hu.webhejj.perspektive.uml.umlName
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KProperty
import kotlin.reflect.KType

class ScanConfig {

    val scanBlacklist: MutableList<(KClass<*>) -> Boolean> = mutableListOf(
        { c: KClass<*> -> c.qualifiedName?.startsWith("kotlin.") ?: false },
        { c: KClass<*> -> c.qualifiedName?.startsWith("java.") ?: false },
        { c: KClass<*> -> c.qualifiedName?.startsWith("org.springframework.") ?: false },
        { c: KClass<*> -> c.qualifiedName?.startsWith("com.fasterxml.") ?: false },
        { c: KClass<*> -> c.qualifiedName?.endsWith("Test") ?: false },
    )

    val dataClassMethodBlacklist: List<(KFunction<*>) -> Boolean> = listOf(
        { f: KFunction<*> -> f.name == "copy" },
        { f: KFunction<*> -> f.name == "toString" },
        { f: KFunction<*> -> f.name == "equals" },
        { f: KFunction<*> -> f.name == "hashCode" },
        { f: KFunction<*> -> f.name.matches(Regex("component[0-9][0-9]?")) },
    )

    val fieldWhiteList: MutableList<(KProperty<*>) -> Boolean> = mutableListOf(
        { p: KProperty<*> -> !p.returnType.umlName.qualified.startsWith("hu.webhejj.perspektive") },
    )

    fun isAllowed(kClass: KClass<*>) = !scanBlacklist.any { it(kClass) }
    fun isAllowed(kType: KType?) = kType != null && kType.classifier is KClass<*> && isAllowed(kType.classifier as KClass<*>)
    fun isAllowed(kClass: KFunction<*>) = !dataClassMethodBlacklist.any { it(kClass) }
    fun isField(kClass: KProperty<*>) = fieldWhiteList.any { it(kClass) }

    fun exclude(kClass: KClass<*>) {
        scanBlacklist.add { c: KClass<*> -> c.qualifiedName == kClass.qualifiedName }
    }

    fun exclude(regex: Regex) {
        scanBlacklist.add { c: KClass<*> -> c.qualifiedName?.matches(regex) ?: false }
    }
}
