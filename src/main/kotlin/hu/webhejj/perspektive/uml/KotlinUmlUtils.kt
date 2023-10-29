package hu.webhejj.perspektive.uml

import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.KTypeParameter
import kotlin.reflect.KTypeProjection
import kotlin.reflect.KVisibility
import kotlin.reflect.jvm.jvmName

val KType?.umlName: UmlName get() = when (val classifier = this?.classifier) {
    is KClass<*> -> classifier.umlName
    is KTypeParameter -> UmlName("UNKNOWN", classifier.name)
    null -> UmlName("null", "null")
    else -> UmlName(classifier.toString(), "UNKNOWN")
}

val KClass<*>.umlName: UmlName get() {
    return UmlName(jvmName, jvmName.substringAfterLast("."))
}

val KTypeParameter.uml: UmlTypeParameter get() = UmlTypeParameter(name, upperBounds.map { it.umlName })

val KTypeProjection.uml: UmlTypeProjection get() = UmlTypeProjection(listOf(type.umlName))

val KVisibility?.uml: UmlVisibility? get() = when (this) {
    KVisibility.PUBLIC -> UmlVisibility.PUBLIC
    KVisibility.PROTECTED -> UmlVisibility.PROTECTED
    KVisibility.PRIVATE -> UmlVisibility.PRIVATE
    KVisibility.INTERNAL -> UmlVisibility.INTERNAL
    null -> null
}

val KClass<*>.isKotlinClass: Boolean
    get() = this.java.getAnnotation(Metadata::class.java)?.kind == 1
