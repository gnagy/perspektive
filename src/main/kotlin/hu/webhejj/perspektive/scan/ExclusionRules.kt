package hu.webhejj.perspektive.scan

import kotlin.reflect.KCallable
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

interface ExclusionRule {
    fun test(kClass: KClass<*>): RuleDecision = RuleDecision.CONTINUE
    fun test(kClass: KClass<*>, member: KCallable<*>): RuleDecision = RuleDecision.CONTINUE
}

enum class RuleDecision {
    INCLUDE,
    EXCLUDE,
    CONTINUE,
}

class ClassRule(
    val patterns: List<Regex>,
    val decision: RuleDecision,
) : ExclusionRule {
    override fun test(kClass: KClass<*>): RuleDecision {
        return if (patterns.any { it.matches(kClass.qualifiedName!!) }) decision else RuleDecision.CONTINUE
    }
}

class PackageRule(
    val packages: List<String> = listOf(),
    val decision: RuleDecision,
    val recursive: Boolean = false,
) : ExclusionRule {
    override fun test(kClass: KClass<*>): RuleDecision {
        return if (packages.any { it == kClass.java.packageName || (recursive && kClass.java.packageName.startsWith("$it.")) }) {
            decision
        } else {
            RuleDecision.CONTINUE
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

class HideEnumClassMembers : ExclusionRule {
    override fun test(kClass: KClass<*>, member: KCallable<*>): RuleDecision {
        return if (kClass.isSubclassOf(Enum::class) && member.name in setOf("valueOf", "values", "entries")
        ) {
            RuleDecision.EXCLUDE
        } else {
            RuleDecision.CONTINUE
        }
    }
}

class HideAllMembers : ExclusionRule {
    override fun test(kClass: KClass<*>, member: KCallable<*>): RuleDecision = RuleDecision.EXCLUDE
}
