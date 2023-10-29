package hu.webhejj.perspektive.plantuml

import hu.webhejj.perspektive.ClassDiagram
import hu.webhejj.perspektive.uml.UmlCardinality
import hu.webhejj.perspektive.uml.UmlClass
import hu.webhejj.perspektive.uml.UmlMember
import hu.webhejj.perspektive.uml.UmlTypeProjection
import hu.webhejj.perspektive.uml.UmlVisibility
import net.sourceforge.plantuml.FileFormat
import net.sourceforge.plantuml.FileFormatOption
import net.sourceforge.plantuml.SourceStringReader
import java.io.File
import java.io.PrintWriter

class PlantUmlWriter {

    fun write(
        file: File,
        classDiagram: ClassDiagram,
        options: PlantUmlOptions = PlantUmlOptions(),
    ) {
        file.parentFile.mkdirs()
        file.printWriter().use { write(it, classDiagram, options) }
    }

    fun renderPng(
        plantumlFile: File,
        pngFile: File = File(plantumlFile.absolutePath.removeSuffix(".plantuml") + ".png"),
        scale: Double = 1.0,
    ) {
        val reader = SourceStringReader(plantumlFile.readText())
        pngFile.outputStream().use {
            reader.outputImage(it, FileFormatOption(FileFormat.PNG).withScale(scale))
        }
    }

    private fun write(
        output: PrintWriter,
        classDiagram: ClassDiagram,
        renderingOptions: PlantUmlOptions = PlantUmlOptions(),
    ) {
        output.println("@startuml\n")

        if (renderingOptions.direction == PlantUmlOptions.Direction.LEFT_TO_RIGHT) {
            output.println("\nleft to right direction\n")
        }

        classDiagram.umlClasses.forEach { umlClass ->

            beginClassBlock(umlClass, output)
            writeInlineProperties(umlClass, classDiagram, output)
            writeEnumValues(umlClass, output)
            writeMethods(umlClass, output)
            endClassBlock(output)

            writeSuperClasses(umlClass, output)
            output.println()
            writeConnectedProperties(umlClass, classDiagram, output)
            output.println()
            writeConnectedGenericParameterTypes(umlClass, output)
            output.println()
        }
        output.println("@enduml")
        output.flush()
    }

    private fun beginClassBlock(umlClass: UmlClass, output: PrintWriter) {
        val abstract = if (umlClass.isAbstract && umlClass.kind in setOf(UmlClass.Kind.CLASS, UmlClass.Kind.CLASS)) {
            "abstract "
        } else {
            ""
        }

        val kind = when (umlClass.kind) {
            UmlClass.Kind.CLASS, UmlClass.Kind.DATA_CLASS -> "class"
            UmlClass.Kind.ENUM -> "enum"
            UmlClass.Kind.INTERFACE -> "interface"
        }

        val spot = if (umlClass.kind == UmlClass.Kind.DATA_CLASS) {
            "<< (D,orchid) >>"
        } else {
            ""
        }

        val generics = if (umlClass.typeParameters.isEmpty()) {
            ""
        } else {
            umlClass.typeParameters.joinToString(separator = ",", prefix = "<", postfix = ">") { it.name }
        }

        val stereotypes = if (umlClass.stereotypes.isEmpty()) "" else umlClass.stereotypes.joinToString(prefix = "<< ", postfix = " >>")

        output.println("$abstract$kind ${umlClass.name.qualified}$generics $spot $stereotypes {")
    }

    private fun writeInlineProperties(
        umlClass: UmlClass,
        classDiagram: ClassDiagram,
        output: PrintWriter,
    ) {
        umlClass.members
            .filter { it.kind == UmlMember.Kind.PROPERTY }
            .filter { prop -> classDiagram.umlClasses.none { it.name == prop.type } }
            .forEach { prop ->
                val abstract = if (prop.isAbstract) "{abstract} " else ""
                val static = if (prop.isStatic) "{static} " else ""
                output.print("    $abstract$static${prop.visibility.plantumlPrefix}${prop.name}: ${prop.type.simple}${genericsString(prop.typeProjections)}")
                if (prop.cardinality == UmlCardinality.OPTIONAL) {
                    output.println("?")
                } else {
                    output.println()
                }
            }
    }

    private fun writeEnumValues(umlClass: UmlClass, output: PrintWriter) {
        umlClass.members
            .filter { it.kind == UmlMember.Kind.ENUM_VALUE }
            .forEach {
                output.println("    ${it.name}")
            }
    }

    private fun writeMethods(umlClass: UmlClass, output: PrintWriter) {
        umlClass.members
            .filter { it.kind == UmlMember.Kind.METHOD }
            .forEach {
                val abstract = if (it.isAbstract) "{abstract} " else ""
                val static = if (it.isStatic) "{static} " else ""
                val generics = genericsString(it.typeProjections)
                output.println("    $abstract$static${it.visibility.plantumlPrefix}${it.name}(${it.parameters.joinToString()}): ${it.type.simple}$generics")
            }
    }

    private fun endClassBlock(output: PrintWriter) {
        output.println("}\n")
    }

    private fun writeSuperClasses(umlClass: UmlClass, output: PrintWriter) {
        umlClass.superClasses.forEach { umlInheritance ->
            val genericsString = genericsString(umlInheritance.typeProjections).let {
                if (it.isBlank()) {
                    ""
                } else {
                    ": $it"
                }
            }
            output.println("${umlInheritance.type.qualified} <|-- ${umlClass.name.qualified}$genericsString")
        }
    }

    private fun writeConnectedProperties(
        umlClass: UmlClass,
        classDiagram: ClassDiagram,
        output: PrintWriter,
    ) {
        umlClass.members
            .filter { it.kind == UmlMember.Kind.PROPERTY }
            .filter { prop -> classDiagram.umlClasses.any { it.name == prop.type } }
            .forEach { prop ->
                val cardinality = when (prop.cardinality) {
                    UmlCardinality.OPTIONAL -> "\"0..1\""
                    UmlCardinality.SCALAR -> "\"1\""
                    UmlCardinality.VECTOR -> "\"*\""
                }
                val static = if (prop.isStatic) "<< static >> " else ""
                val generics = genericsString(prop.typeProjections, true)
                output.println("${umlClass.name.qualified} o-- $cardinality ${prop.type.qualified}: $static ${prop.name}$generics")
            }
    }

    private fun writeConnectedGenericParameterTypes(umlClass: UmlClass, output: PrintWriter) {
        umlClass.typeParameters.forEach { param ->
            param.upperBounds
                .filter { ub -> ub.qualified != "java.lang.Object" }
                .forEach { ub ->
                    output.println("${umlClass.name.qualified} ..> ${ub.qualified}: <${param.name}>")
                }
        }
    }
}

private fun genericsString(typeProjections: List<UmlTypeProjection>, splitLines: Boolean = false) =
    if (typeProjections.isEmpty()) {
        ""
    } else {
        val newline = if (splitLines) "\\n" else ""
        typeProjections.joinToString(
            separator = ",",
            prefix = "$newline<",
            postfix = ">",
        ) { p -> p.types.joinToString { it.simple } }
    }

private val UmlVisibility?.plantumlPrefix: String
    get() = when (this) {
        UmlVisibility.PUBLIC -> "+"
        UmlVisibility.PROTECTED -> "#"
        UmlVisibility.PRIVATE -> "-"
        UmlVisibility.INTERNAL -> "~"
        null -> ""
    }
