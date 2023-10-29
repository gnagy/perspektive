package hu.webhejj.perspektive.scan

import kotlin.reflect.KCallable
import kotlin.reflect.KClass

class ScanConfig(
    val skipPropertes: Boolean = false,
    val skipMethods: Boolean = false,
    val exclusionRules: MutableList<ExclusionRule> = mutableListOf(
        HidePackages(excludesRecursive = listOf("java", "javax", "kotlin")),
        HideDataClassMembers(),
    ),
) {

    fun isAllowed(kClass: KClass<*>): Boolean {
        for (rule in exclusionRules) {
            when (rule.test(kClass)) {
                RuleDecision.INCLUDE -> return true
                RuleDecision.EXCLUDE -> return false
                RuleDecision.CONTINUE -> {}
            }
        }
        return true
    }
    fun isAllowed(kClass: KClass<*>, member: KCallable<*>): Boolean {
        for (rule in exclusionRules) {
            when (rule.test(kClass, member)) {
                RuleDecision.INCLUDE -> return true
                RuleDecision.EXCLUDE -> return false
                RuleDecision.CONTINUE -> {}
            }
        }
        return true
    }
}
