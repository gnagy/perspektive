package hu.webhejj.perspektive

import hu.webhejj.perspektive.plantuml.PlantUmlWriter
import hu.webhejj.perspektive.plantuml.RenderingOptions
import hu.webhejj.perspektive.uml.UmlCardinality
import hu.webhejj.perspektive.uml.UmlClass
import hu.webhejj.perspektive.uml.UmlInheritance
import hu.webhejj.perspektive.uml.UmlMembership
import hu.webhejj.perspektive.uml.UmlMethod
import hu.webhejj.perspektive.uml.UmlName
import hu.webhejj.perspektive.uml.UmlProperty
import hu.webhejj.perspektive.uml.uml
import hu.webhejj.perspektive.uml.umlName
import io.github.classgraph.ClassGraph
import java.io.File
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.KProperty1
import kotlin.reflect.KType
import kotlin.reflect.KTypeProjection
import kotlin.reflect.full.declaredMemberFunctions
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.jvm.jvmErasure

class ClassDiagram(
    private val scanConfig: ScanConfig = ScanConfig(),
) {

    private val scanned = mutableSetOf<String>()
    val umlClasses = mutableSetOf<UmlClass>()

    fun scanTypes(kType: KType): Boolean {
        return if (kType.classifier is KClass<*>) {
            scanTypes(kType.classifier as KClass<*>)
        } else {
            false
        }
    }

    fun scanTypes(kClass: KClass<*>): Boolean {
        val qualifiedName = kClass.qualifiedName
        return if (qualifiedName != null && !scanned.contains(qualifiedName) && scanConfig.isAllowed(kClass)) {
            println("Scanning $kClass")
            scanned.add(qualifiedName)
            val umlClass = kClass.umlClass()
            if (umlClass != null) {
                umlClasses.add(umlClass)
                kClass.supertypes.forEach { scanTypes(it) }
                kClass.sealedSubclasses.forEach { scanTypes(it) }
                true
            } else {
                false
            }
        } else {
            println("Skipping $kClass")
            false
        }
    }

    fun scanPackage(basePackage: String) {
        val scanResult = ClassGraph().verbose().acceptPackages(basePackage).scan()
        scanResult.allClasses.forEach { classInfo ->
            val kClass = classInfo.loadClass().kotlin
            scanTypes(kClass)
        }
    }

    private fun KClass<*>.umlClass(): UmlClass? {
        return try {
            UmlClass(
                name = this.umlName,
                kind = when {
                    java.isInterface -> UmlClass.Kind.INTERFACE
                    isData -> UmlClass.Kind.DATA_CLASS
                    isSubclassOf(Enum::class) -> UmlClass.Kind.ENUM
                    else -> UmlClass.Kind.CLASS
                },
                typeParameters = typeParameters.map { it.uml },
                superClasses = umlSuperClasses(),
                properties = declaredMemberProperties.map { it.umlProperty() } + umlEnumValues(),
                methods = umlMethods(),
            )
        } catch (e: UnsupportedOperationException) {
            println("Unsupported $this: ${e.message}")
            null
        }
    }

    private fun KClass<*>.umlSuperClasses(): List<UmlInheritance> {
        return supertypes
            .filter { supertype -> scanConfig.isAllowed(supertype.classifier as KClass<*>) }
            .map { UmlInheritance(it.umlName, it.arguments.map { arg -> arg.uml }) }
    }

    private fun KClass<*>.umlEnumValues(): List<UmlProperty> {
        return if (isSubclassOf(Enum::class)) {
            val values: Array<Enum<*>> = java.enumConstants as Array<Enum<*>>
            values.map { UmlProperty(it.name, UmlName("", ""), listOf(), UmlMembership.FIELD, UmlCardinality.SCALAR) }
        } else {
            emptyList()
        }
    }

    private fun KProperty1<out Any, *>.umlProperty(): UmlProperty {
        val kProperty = this

        return if (isCollection(kProperty)) {
            val itemType: KTypeProjection = kProperty.returnType.arguments[0]
            val typeProjections = itemType.type?.arguments?.map { it.uml } ?: emptyList()

            scanTypes(itemType.type!!)

            if (scanConfig.isAllowed(itemType.type)) {
                // TODO: uml type parameters
                if (kProperty.returnType.arguments.size == 1) {
                    UmlProperty(kProperty.name, itemType.type.umlName, typeProjections, UmlMembership.RELATIONSHIP, UmlCardinality.VECTOR)
                } else {
                    UmlProperty("MAP??? ${kProperty.name}", itemType.type.umlName, typeProjections, UmlMembership.RELATIONSHIP, UmlCardinality.VECTOR)
                }
            } else {
                umlProperty(kProperty, UmlMembership.FIELD, UmlCardinality.VECTOR)
            }
        } else if (scanConfig.isField(kProperty)) {
            scanTypes(kProperty.returnType)
            val membership = if (umlClasses.find { it.name == kProperty.returnType.umlName } == null) {
                UmlMembership.FIELD
            } else {
                UmlMembership.RELATIONSHIP
            }
            val cardinality = if (kProperty.returnType.isMarkedNullable) {
                UmlCardinality.OPTIONAL
            } else {
                UmlCardinality.SCALAR
            }
            umlProperty(kProperty, membership, cardinality)
        } else {
            val cardinality = if (kProperty.returnType.isMarkedNullable) {
                UmlCardinality.OPTIONAL
            } else {
                UmlCardinality.SCALAR
            }

            scanTypes(kProperty.returnType)
            if (scanConfig.isAllowed(kProperty.returnType)) {
                umlProperty(kProperty, UmlMembership.RELATIONSHIP, cardinality)
            } else {
                umlProperty(kProperty, UmlMembership.FIELD, UmlCardinality.VECTOR)
            }
        }
    }

    private fun umlProperty(kProperty: KProperty1<out Any, *>, membership: UmlMembership, cardinality: UmlCardinality) = UmlProperty(
        name = kProperty.name,
        type = kProperty.returnType.umlName,
        typeProjections = kProperty.returnType.arguments.map { it.uml },
        membership = membership,
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
                )
            }
    }

    private fun isCollection(kProperty: KProperty<*>): Boolean {
        return try {
            kProperty.returnType.jvmErasure.isSubclassOf(Iterable::class)
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
