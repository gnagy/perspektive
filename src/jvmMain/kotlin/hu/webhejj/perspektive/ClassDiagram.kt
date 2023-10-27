package hu.webhejj.perspektive

import hu.webhejj.perspektive.plantuml.PlantUmlWriter
import hu.webhejj.perspektive.plantuml.RenderingOptions
import hu.webhejj.perspektive.uml.UmlCardinality
import hu.webhejj.perspektive.uml.UmlClass
import hu.webhejj.perspektive.uml.UmlInheritance
import hu.webhejj.perspektive.uml.UmlMethod
import hu.webhejj.perspektive.uml.UmlName
import hu.webhejj.perspektive.uml.UmlProperty
import hu.webhejj.perspektive.uml.UmlVisibility
import hu.webhejj.perspektive.uml.uml
import hu.webhejj.perspektive.uml.umlName
import io.github.classgraph.ClassGraph
import java.io.File
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.KProperty1
import kotlin.reflect.KType
import kotlin.reflect.full.declaredMemberFunctions
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.jvm.jvmErasure

class ClassDiagram(
    val scanConfig: ScanConfig = ScanConfig(),
) {

    private val scannedQualifiedNames = mutableSetOf<String>()
    val umlClasses = mutableSetOf<UmlClass>()

    fun scanKType(kType: KType): Boolean {
        return if (kType.classifier is KClass<*>) {
            scanKClass(kType.classifier as KClass<*>)
        } else {
            println("Skipping KType $kType")
            false
        }
    }

    fun scanKClass(kClass: KClass<*>): Boolean {
        val qualifiedName = kClass.qualifiedName
        return if (qualifiedName == null) {
            println("Skipping local / anonymous class $kClass")
            false
        } else {
            if (!scannedQualifiedNames.contains(qualifiedName) && scanConfig.isAllowed(kClass)) {
                println("Scanning KClass $kClass")
                scannedQualifiedNames.add(qualifiedName)
                return try {
                    val umlClass = kClass.umlClass()
                    umlClasses.add(umlClass)
                    kClass.supertypes.forEach { scanKType(it) }
                    kClass.sealedSubclasses.forEach { scanKClass(it) }
                    true
                } catch (e: UnsupportedOperationException) {
                    println("Unsupported KClass $kClass: ${e.message}")
                    false
                }
            } else {
                println("Skipping KClass $kClass")
                false
            }
        }
    }

    fun scanPackage(basePackage: String) {
        val scanResult = ClassGraph().verbose().acceptPackages(basePackage).scan()
        scanResult.allClasses.forEach { classInfo ->
            val kClass = classInfo.loadClass().kotlin
            scanKClass(kClass)
        }
    }

    private fun KClass<*>.umlClass(): UmlClass {
        return UmlClass(
            name = this.umlName,
            kind = when {
                java.isInterface -> UmlClass.Kind.INTERFACE
                isData -> UmlClass.Kind.DATA_CLASS
                isSubclassOf(Enum::class) -> UmlClass.Kind.ENUM
                else -> UmlClass.Kind.CLASS
            },
            isAbstract = isAbstract,
            stereotypes =
            stereotype("open", isOpen) +
                stereotype("sealed", isSealed) +
                stereotype("companion", isCompanion) +
                stereotype("functional", isFun) +
                stereotype("value", isFun),
            typeParameters = typeParameters.map { it.uml },
            superClasses = umlSuperClasses(),
            properties = declaredMemberProperties.map { it.umlProperty() } + umlEnumValues(),
            methods = umlMethods(),
        )
    }

    private fun stereotype(name: String, present: Boolean): List<String> {
        return if (present) listOf(name) else listOf()
    }

    private fun KClass<*>.umlSuperClasses(): List<UmlInheritance> {
        return supertypes
            .filter { supertype -> scanConfig.isAllowed(supertype.classifier as KClass<*>) }
            .map { UmlInheritance(it.umlName, it.arguments.map { arg -> arg.uml }) }
    }

    private fun KClass<*>.umlEnumValues(): List<UmlProperty> {
        return if (isSubclassOf(Enum::class) && java.enumConstants != null) {
            val values: Array<Enum<*>> = java.enumConstants as Array<Enum<*>>
            values.map { UmlProperty(UmlVisibility.PUBLIC, it.name, UmlName("", ""), false, listOf(), UmlCardinality.SCALAR) }
        } else {
            emptyList()
        }
    }

    private fun KProperty1<out Any, *>.umlProperty(): UmlProperty {
        val kProperty = this

        scanKType(returnType)
        kProperty.returnType.arguments.forEach { scanKType(it.type!!) } // TODO !!

        val itemType = if (safeSubclassOf(kProperty, Iterable::class)) {
            kProperty.returnType.arguments[0].type!! // TODO !!
        } else if (safeSubclassOf(kProperty, Map::class)) {
            kProperty.returnType.arguments[1].type!! // TODO !!
        } else {
            kProperty.returnType
        }
        val cardinality = if (itemType.isMarkedNullable) {
            UmlCardinality.OPTIONAL
        } else {
            UmlCardinality.SCALAR // TODO: vector for collections and maps
        }
        return umlProperty(kProperty, itemType, cardinality)
    }

    private fun umlProperty(kProperty: KProperty<*>, type: KType, cardinality: UmlCardinality) = UmlProperty(
        visibility = kProperty.visibility.uml,
        name = kProperty.name,
        type = type.umlName,
        isAbstract = kProperty.isAbstract,
        typeProjections = type.arguments.map { it.uml },
        cardinality = cardinality,
    )

    private fun KClass<*>.umlMethods(): List<UmlMethod> {
        return declaredMemberFunctions
            .filter { f -> scanConfig.isAllowed(f) || !this.isData }
            .map { kFunction ->
                UmlMethod(
                    visibility = kFunction.visibility.uml,
                    name = kFunction.name,
                    returnType = kFunction.returnType.umlName,
                    returnTypeProjections = kFunction.returnType.arguments.map { it.uml },
                    // dropping first method parameter (`this` reference)
                    parameters = kFunction.parameters.drop(1).map { it.name ?: "" },
                    isAbstract = kFunction.isAbstract,
                )
            }
    }

    private fun safeSubclassOf(kProperty: KProperty<*>, base: KClass<*>): Boolean {
        return try {
            kProperty.returnType.jvmErasure.isSubclassOf(base)
        } catch (e: Throwable) {
            // kotlin.reflect.jvm.internal.KotlinReflectionInternalError
            false
        }
    }

    fun write(file: File, renderingOptions: RenderingOptions = RenderingOptions()) {
        file.parentFile.mkdirs()
        PlantUmlWriter().also {
            it.write(file, this, renderingOptions)
            it.renderPng(file, File(file.absolutePath.removeSuffix(".plantuml") + ".png"))
        }
    }
}
