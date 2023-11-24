package hu.webhejj.perspektive

import hu.webhejj.perspektive.plantuml.PlantUmlOptions
import hu.webhejj.perspektive.plantuml.PlantUmlWriter
import hu.webhejj.perspektive.scan.ScanConfig
import hu.webhejj.perspektive.uml.KotlinReflectionToUmlMapper
import hu.webhejj.perspektive.uml.UmlClass
import hu.webhejj.perspektive.uml.UmlInheritance
import hu.webhejj.perspektive.uml.UmlMember
import io.github.classgraph.ClassGraph
import java.io.File
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.KType
import kotlin.reflect.full.declaredMemberFunctions
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.staticFunctions
import kotlin.reflect.full.staticProperties

class ClassDiagram(
    val scanConfig: ScanConfig = ScanConfig(),
) {

    private val mapper = KotlinReflectionToUmlMapper()
    private val scannedQualifiedNames = mutableSetOf<String>()
    val umlClasses = mutableSetOf<UmlClass>()

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

    fun scanKType(kType: KType): Boolean {
        return if (kType.classifier is KClass<*> && scanConfig.isAllowed(kType.classifier as KClass<*>)) {
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
                    val umlInheritances = scanSuperClasses(kClass)
                    val umlMembers = scanMembers(kClass)
                    val umlClass = mapper.toUmlClass(kClass, umlInheritances, umlMembers)
                    umlClasses.add(umlClass)
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

    private fun scanSuperClasses(kClass: KClass<*>): List<UmlInheritance> {
        return kClass.supertypes
            .filter {
                scanConfig.isAllowed(it.classifier as KClass<*>)
            }
            .map {
                scanKType(it)
                mapper.toInheritance(it)
            }
    }

    private fun scanMembers(kClass: KClass<*>): List<UmlMember> {
        return kClass.declaredMemberProperties.filter { scanConfig.isAllowed(kClass, it) }.map { scanKProperty(it, isStatic = false) } +
            kClass.staticProperties.filter { scanConfig.isAllowed(kClass, it) }.map { scanKProperty(it, isStatic = true) } +
            kClass.declaredMemberFunctions.filter { scanConfig.isAllowed(kClass, it) }.map { mapper.toMethodMember(it, isStatic = false) } +
            kClass.staticFunctions.filter { scanConfig.isAllowed(kClass, it) }.map { mapper.toMethodMember(it, isStatic = true) } +
            kClass.enumValues().map { mapper.toEnumMember(it) }
        // try { kClass.declaredMemberFunctions.umlMethods(kClass, isStatic = false) } catch (e: Throwable) { listOf() } +
    }

    private fun scanKProperty(kProperty: KProperty<*>, isStatic: Boolean): UmlMember {
        scanKType(kProperty.returnType)
        kProperty.returnType.arguments.mapNotNull { it.type }.forEach { scanKType(it) }

        return mapper.toPropertyMember(kProperty, isStatic = isStatic)
    }

    fun renderWithPlantUml(file: File, renderingOptions: PlantUmlOptions = PlantUmlOptions()) {
        PlantUmlWriter().also {
            it.write(file, this, renderingOptions)
            it.renderSvg(file)
        }
    }
}

fun KClass<*>.enumValues(): List<Enum<*>> {
    return if (this.isSubclassOf(Enum::class) && java.enumConstants != null) {
        java.enumConstants.toList() as List<Enum<*>>
    } else {
        emptyList()
    }
}
