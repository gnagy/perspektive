package hu.webhejj.perspektive

import hu.webhejj.perspektive.plantuml.PlantUmlOptions
import hu.webhejj.perspektive.plantuml.PlantUmlWriter
import hu.webhejj.perspektive.scan.ScanConfig
import hu.webhejj.perspektive.uml.KotlinReflectionToUmlMapper
import hu.webhejj.perspektive.uml.UmlClass
import hu.webhejj.perspektive.uml.UmlInheritance
import hu.webhejj.perspektive.uml.UmlMember
import io.github.classgraph.ClassGraph
import org.slf4j.LoggerFactory
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

    private val logger = LoggerFactory.getLogger(javaClass)!!
    private val mapper = KotlinReflectionToUmlMapper()
    private val scannedQualifiedNames = mutableSetOf<String>()
    val umlClasses = mutableSetOf<UmlClass>()

    fun scanPackage(basePackage: String) {
        val scanResult = ClassGraph()
            // .verbose()
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
            logger.debug("Skipping KType $kType")
            false
        }
    }

    fun scanKClass(kClass: KClass<*>): Boolean {
        val qualifiedName = kClass.qualifiedName
        return if (qualifiedName == null) {
            logger.debug("Skipping local / anonymous class $kClass")
            false
        } else {
            if (!scannedQualifiedNames.contains(qualifiedName) && scanConfig.isAllowed(kClass)) {
                logger.info("Scanning KClass $kClass")
                scannedQualifiedNames.add(qualifiedName)
                return try {
                    val umlInheritances = scanSuperClasses(kClass)
                    val umlMembers = scanMembers(kClass)
                    val umlClass = mapper.toUmlClass(kClass, umlInheritances, umlMembers)
                    umlClasses.add(umlClass)
                    kClass.sealedSubclasses.forEach { scanKClass(it) }
                    true
                } catch (e: UnsupportedOperationException) {
                    logger.warn("Unsupported KClass $kClass: ${e.message}")
                    false
                }
            } else {
                logger.debug("Skipping KClass $kClass")
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
        val umlMembers = kClass.declaredMemberProperties.filter { scanConfig.isAllowed(kClass, it) }.map { scanKProperty(it, isStatic = false) } +
            kClass.staticProperties.filter { scanConfig.isAllowed(kClass, it) }.map { scanKProperty(it, isStatic = true) } +
            kClass.declaredMemberFunctions.filter { scanConfig.isAllowed(kClass, it) }.map { mapper.toMethodMember(it, isStatic = false) } +
            kClass.staticFunctions.filter { scanConfig.isAllowed(kClass, it) }.map { mapper.toMethodMember(it, isStatic = true) } +
            kClass.enumValues().map { mapper.toEnumMember(it) }
        // try { kClass.declaredMemberFunctions.umlMethods(kClass, isStatic = false) } catch (e: Throwable) { listOf() } +

        return collapseJavaBeanProperties(umlMembers)
    }

    private fun scanKProperty(kProperty: KProperty<*>, isStatic: Boolean): UmlMember {
        scanKType(kProperty.returnType)
        kProperty.returnType.arguments.mapNotNull { it.type }.forEach { scanKType(it) }

        return mapper.toPropertyMember(kProperty, isStatic = isStatic)
    }

    private fun collapseJavaBeanProperties(members: List<UmlMember>): List<UmlMember> {
        val mutableMembers = members.toMutableList()
        members
            .filter { it.kind == UmlMember.Kind.PROPERTY }
            .forEach { umlMember ->
                val isGetterName = "is${umlMember.name.capitalize()}"
                val getterName = "get${umlMember.name.capitalize()}"
                val setterName = "set${umlMember.name.capitalize()}"
                members
                    .find { it.kind == UmlMember.Kind.METHOD && it.name in listOf(isGetterName, getterName) }
                    ?.also {
                        mutableMembers.remove(it)
                        umlMember.stereotypes.add("get")
                    }
                members
                    .find { it.kind == UmlMember.Kind.METHOD && it.name == setterName }
                    ?.also {
                        mutableMembers.remove(it)
                        umlMember.stereotypes.add("set")
                    }
                }
        return mutableMembers.toList()
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
