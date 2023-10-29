package hu.webhejj.perspektive.uml

import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KProperty
import kotlin.reflect.KType
import kotlin.reflect.full.createType
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.jvm.jvmErasure

class KotlinReflectionToUmlMapper {

    fun toUmlClass(kClass: KClass<*>, superClasses: List<UmlInheritance>, members: List<UmlMember>): UmlClass {
        return UmlClass(
            name = kClass.umlName,
            kind = when {
                kClass.java.isInterface -> UmlClass.Kind.INTERFACE
                kClass.isData -> UmlClass.Kind.DATA_CLASS
                kClass.isSubclassOf(Enum::class) -> UmlClass.Kind.ENUM
                else -> UmlClass.Kind.CLASS
            },
            isAbstract = kClass.isAbstract,
            stereotypes =
            stereotype("open", kClass.isOpen) +
                stereotype("sealed", kClass.isSealed) +
                stereotype("companion", kClass.isCompanion) +
                stereotype("functional", kClass.isFun) +
                stereotype("value", kClass.isFun) +
                stereotype("kotlin", kClass.isKotlinClass),
            typeParameters = kClass.typeParameters.map { it.uml },
            superClasses = superClasses,
            members = members,
        )
    }

    private fun stereotype(name: String, present: Boolean): List<String> {
        return if (present) listOf(name) else listOf()
    }

    fun toInheritance(kType: KType): UmlInheritance {
        return UmlInheritance(kType.umlName, kType.arguments.map { arg -> arg.uml })
    }

    fun toPropertyMember(kProperty: KProperty<*>, isStatic: Boolean): UmlMember {
        val itemType: KType = if (safeSubclassOf(kProperty.returnType, Iterable::class)) {
            kProperty.returnType.arguments.getOrNull(0)?.type ?: Any::class.createType()
        } else if (safeSubclassOf(kProperty.returnType, Map::class)) {
            kProperty.returnType.arguments.getOrNull(1)?.type ?: Any::class.createType()
        } else {
            kProperty.returnType
        }
        val cardinality = if (itemType.isMarkedNullable) {
            UmlCardinality.OPTIONAL
        } else if (isJavaVectorType(kProperty.returnType)) {
            UmlCardinality.VECTOR
        } else {
            UmlCardinality.SCALAR
        }
        return UmlMember(
            kind = UmlMember.Kind.PROPERTY,
            visibility = kProperty.visibility.uml,
            name = kProperty.name,
            type = itemType.umlName,
            typeProjections = itemType.arguments.map { it.uml },
            parameters = listOf(),
            isAbstract = kProperty.isAbstract,
            isStatic = isStatic,
            cardinality = cardinality,
        )
    }

    private fun isJavaVectorType(kType: KType): Boolean {
        return safeSubclassOf(kType, Iterable::class) || safeSubclassOf(kType, Map::class)
    }

    private fun safeSubclassOf(kType: KType, base: KClass<*>): Boolean {
        return try {
            kType.jvmErasure.isSubclassOf(base)
        } catch (e: Throwable) {
            // kotlin.reflect.jvm.internal.KotlinReflectionInternalError
            false
        }
    }

    fun toMethodMember(kFunction: KFunction<*>, isStatic: Boolean): UmlMember {
        return UmlMember(
            kind = UmlMember.Kind.METHOD,
            visibility = kFunction.visibility.uml,
            name = kFunction.name,
            type = kFunction.returnType.umlName,
            typeProjections = kFunction.returnType.arguments.map { it.uml },
            // dropping first method parameter (`this` reference)
            parameters = kFunction.parameters.drop(1).map { it.name ?: "" },
            isAbstract = kFunction.isAbstract,
            isStatic = isStatic,
            cardinality = UmlCardinality.SCALAR, // TODO
        )
    }

    fun toEnumMember(enum: Enum<*>): UmlMember {
        return UmlMember(
            kind = UmlMember.Kind.ENUM_VALUE,
            visibility = UmlVisibility.PUBLIC,
            name = enum.name,
            type = UmlName("", ""),
            typeProjections = listOf(),
            parameters = listOf(),
            isAbstract = false,
            isStatic = false,
            cardinality = UmlCardinality.SCALAR,
        )
    }
}
