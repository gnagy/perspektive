package hu.webhejj.perspektive.hu.webhejj.perspektive

import hu.webhejj.perspektive.ClassDiagram
import hu.webhejj.perspektive.uml.UmlClass
import org.junit.jupiter.api.Test
import java.io.File

class ClassDiagramTest {

    private val targetDir = File("build/ktuml/")

    @Test
    fun kotlinModel() {
        val classDiagram = ClassDiagram()
        classDiagram.scanConfig.include(Regex("kotlin\\.reflect\\.[^.]*"))
        classDiagram.scanPackage("kotlin.reflect")
        classDiagram.write(File(targetDir, "kotlinmodel.plantuml"))
    }

    @Test
    fun umlModel() {
        val classDiagram = ClassDiagram()
        classDiagram.scanKClass(UmlClass::class)
        classDiagram.write(File(targetDir, "umlmodel.plantuml"))
    }
}
