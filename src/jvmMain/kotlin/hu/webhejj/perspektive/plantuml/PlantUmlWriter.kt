package hu.webhejj.perspektive.plantuml

import hu.webhejj.perspektive.ClassDiagram
import hu.webhejj.perspektive.uml.UmlCardinality
import hu.webhejj.perspektive.uml.UmlClass
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
        renderingOptions: RenderingOptions = RenderingOptions(),
    ) {
        file.parentFile.mkdirs()
        file.printWriter().use { write(it, classDiagram, renderingOptions) }
    }

    fun write(
        output: PrintWriter,
        classDiagram: ClassDiagram,
        renderingOptions: RenderingOptions = RenderingOptions(),
    ) {
        output.println("@startuml\n")

        if (renderingOptions.direction == Direction.LEFT_TO_RIGHT) {
            output.println("\nleft to right direction\n")
        }

        classDiagram.umlClasses.forEach { umlClass ->
            val kind = when (umlClass.kind) {
                UmlClass.Kind.CLASS, UmlClass.Kind.DATA_CLASS -> "class"
                UmlClass.Kind.ENUM -> "enum"
                UmlClass.Kind.INTERFACE -> "interface"
            }
            val generics = if (umlClass.typeParameters.isEmpty()) {
                ""
            } else {
                umlClass.typeParameters.joinToString(separator = ",", prefix = "<", postfix = ">") { it.name }
            }

            output.println("$kind ${umlClass.name.qualified}$generics {")

            umlClass.properties
                .filter { prop -> classDiagram.umlClasses.none { it.name == prop.type } }
                .forEach { prop ->
                    output.print("    ${prop.name}: ${prop.type.simple}${genericsString(prop.typeProjections)}")
                    if (prop.cardinality == UmlCardinality.OPTIONAL) {
                        output.println("?")
                    } else {
                        output.println()
                    }
                }

            output.println()
            umlClass.methods.forEach {
                val generics = genericsString(it.returnTypeProjections)
                output.println("    ${it.visibility.plantumlPrefix}${it.name}(${it.parameters.joinToString()}): ${it.returnType.simple}$generics")
            }

            output.println("}\n")

            umlClass.superClasses.forEach {
                val genericsString = genericsString(it.typeProjections).let {
                    if (it.isBlank()) {
                        ""
                    } else {
                        ": $it"
                    }
                }
                output.println("${it.type.qualified} <|-- ${umlClass.name.qualified}$genericsString")
            }

            umlClass.properties
                .filter { prop -> classDiagram.umlClasses.any { it.name == prop.type } }
                .forEach { prop ->
                    val cardinality = when (prop.cardinality) {
                        UmlCardinality.OPTIONAL -> "\"0..1\""
                        UmlCardinality.SCALAR -> "\"1\""
                        UmlCardinality.VECTOR -> "\"*\""
                    }
                    val generics = genericsString(prop.typeProjections, true)
                    output.println("${umlClass.name.qualified} o-- $cardinality ${prop.type.qualified}: ${prop.name}$generics")
                }

            output.println()

            // generic parameters
            umlClass.typeParameters.forEach { param ->
                param.upperBounds.forEach { ub ->
                    output.println("${umlClass.name.qualified} ..> ${ub.qualified}: <${param.name}>")
                }
            }
            output.println()
        }
        output.println("@enduml")
        output.flush()
    }

    fun renderPng(plantumlFile: File, pngFile: File, scale: Double = 1.0) {
        val reader = SourceStringReader(plantumlFile.readText())
        pngFile.outputStream().use {
            reader.outputImage(it, FileFormatOption(FileFormat.PNG).withScale(scale))
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
            null -> ""
        }
}
