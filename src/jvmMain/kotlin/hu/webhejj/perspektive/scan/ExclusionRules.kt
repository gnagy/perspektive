package hu.webhejj.perspektive.scan

import kotlin.reflect.KCallable
import kotlin.reflect.KClass

interface ExclusionRule {
    fun test(kClass: KClass<*>): RuleDecision = RuleDecision.CONTINUE
    fun test(kClass: KClass<*>, member: KCallable<*>): RuleDecision = RuleDecision.CONTINUE
}

enum class RuleDecision {
    INCLUDE,
    EXCLUDE,
    CONTINUE,
}

class HideClasses(val patterns: List<Regex>) : ExclusionRule {
    override fun test(kClass: KClass<*>): RuleDecision {
        return if (patterns.any { it.matches(kClass.qualifiedName!!) }) RuleDecision.EXCLUDE else RuleDecision.CONTINUE
    }
}

class HidePackages(
    val includes: List<String> = listOf(),
    val includesRecursive: List<String> = listOf(),
    val excludes: List<String> = listOf(),
    val excludesRecursive: List<String> = listOf(),
) : ExclusionRule {

    override fun test(kClass: KClass<*>): RuleDecision {
        return if (include(kClass)) {
            RuleDecision.INCLUDE
        } else if (exclude(kClass)) {
            RuleDecision.EXCLUDE
        } else {
            RuleDecision.CONTINUE
        }
    }

    fun include(kClass: KClass<*>): Boolean {
        return includes.any {
            it == kClass.java.packageName
        } || includesRecursive.any {
            it == kClass.java.packageName || kClass.java.packageName.startsWith("$it.")
        }
    }

    fun exclude(kClass: KClass<*>): Boolean {
        return excludes.any {
            it == kClass.java.packageName
        } || excludesRecursive.any {
            it == kClass.java.packageName || kClass.java.packageName.startsWith("$it.")
        }
    }
}

class HideDataClassMembers : ExclusionRule {
    override fun test(kClass: KClass<*>, member: KCallable<*>): RuleDecision {
        return if (kClass.isData && (
            member.name in setOf("copy", "toString", "equals", "hashCode") ||
                member.name.matches(Regex("component[0-9][0-9]?"))
            )
        ) {
            RuleDecision.EXCLUDE
        } else {
            RuleDecision.CONTINUE
        }
    }
}

class HideMembers : ExclusionRule {
    override fun test(kClass: KClass<*>, member: KCallable<*>): RuleDecision = RuleDecision.EXCLUDE
}
