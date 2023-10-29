package hu.webhejj.perspektive

import hu.webhejj.perspektive.plantuml.PlantUmlOptions
import hu.webhejj.perspektive.plantuml.PlantUmlWriter
import hu.webhejj.perspektive.scan.ScanConfig
import hu.webhejj.perspektive.uml.UmlCardinality
import hu.webhejj.perspektive.uml.UmlClass
import hu.webhejj.perspektive.uml.UmlInheritance
import hu.webhejj.perspektive.uml.UmlMember
import hu.webhejj.perspektive.uml.UmlName
import hu.webhejj.perspektive.uml.UmlVisibility
import hu.webhejj.perspektive.uml.isKotlinClass
import hu.webhejj.perspektive.uml.uml
import hu.webhejj.perspektive.uml.umlName
import io.github.classgraph.ClassGraph
import java.io.File
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KProperty
import kotlin.reflect.KType
import kotlin.reflect.full.createType
import kotlin.reflect.full.declaredMemberFunctions
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.staticFunctions
import kotlin.reflect.full.staticProperties
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
        val scanResult = ClassGraph()
            .verbose()
            .enableClassInfo()
            .ignoreClassVisibility()
            .acceptPackages(basePackage)
            .scan()

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
                stereotype("value", isFun) +
                stereotype("kotlin", this.isKotlinClass),
            typeParameters = typeParameters.map { it.uml },
            superClasses = umlSuperClasses(),
            members =
            try { declaredMemberProperties.umlProperties(isStatic = false) } catch (e: Throwable) { listOf() } +
                staticProperties.umlProperties(isStatic = true) +
                try { declaredMemberFunctions.umlMethods(this, isStatic = false) } catch (e: Throwable) { listOf() } +
                staticFunctions.umlMethods(this, isStatic = true) +
                umlEnumValues(),
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

    private fun Iterable<KProperty<*>>.umlProperties(isStatic: Boolean): List<UmlMember> {
        return if (scanConfig.skipPropertes) {
            emptyList()
        } else {
            this.map { kProperty ->
                scanKType(kProperty.returnType)
                kProperty.returnType.arguments.mapNotNull { it.type }.forEach { scanKType(it) }

                val itemType: KType = if (safeSubclassOf(kProperty, Iterable::class)) {
                    kProperty.returnType.arguments.getOrNull(0)?.type ?: Any::class.createType()
                } else if (safeSubclassOf(kProperty, Map::class)) {
                    kProperty.returnType.arguments.getOrNull(1)?.type ?: Any::class.createType()
                } else {
                    kProperty.returnType
                }
                val cardinality = if (itemType.isMarkedNullable) {
                    UmlCardinality.OPTIONAL
                } else {
                    UmlCardinality.SCALAR // TODO: vector for collections and maps
                }
                UmlMember(
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
        }
    }

    private fun Iterable<KFunction<*>>.umlMethods(kClass: KClass<*>, isStatic: Boolean): List<UmlMember> {
        return if (scanConfig.skipMethods) {
            emptyList()
        } else {
            this
                .filter { f -> scanConfig.isAllowed(kClass, f) }
                .map { kFunction ->
                    UmlMember(
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
        }
    }

    private fun KClass<*>.umlEnumValues(): List<UmlMember> {
        return if (isSubclassOf(Enum::class) && java.enumConstants != null) {
            val values: Array<Enum<*>> = java.enumConstants as Array<Enum<*>>
            values.map {
                UmlMember(
                    kind = UmlMember.Kind.ENUM_VALUE,
                    visibility = UmlVisibility.PUBLIC,
                    name = it.name,
                    type = UmlName("", ""),
                    typeProjections = listOf(),
                    parameters = listOf(),
                    isAbstract = false,
                    isStatic = false,
                    cardinality = UmlCardinality.SCALAR,
                )
            }
        } else {
            emptyList()
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

    fun renderWithPlantUml(file: File, renderingOptions: PlantUmlOptions = PlantUmlOptions()) {
        PlantUmlWriter().also {
            it.write(file, this, renderingOptions)
            it.renderPng(file)
        }
    }
}
