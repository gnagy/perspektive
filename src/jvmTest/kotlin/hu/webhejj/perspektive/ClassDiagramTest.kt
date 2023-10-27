package hu.webhejj.perspektive.hu.webhejj.perspektive

import hu.webhejj.perspektive.ClassDiagram
import hu.webhejj.perspektive.ScanConfig
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
        classDiagram.write(File(targetDir, "kotlinModel.plantuml"))
    }

    @Test
    fun kotlinInheritance() {
        val classDiagram = ClassDiagram(
            scanConfig = ScanConfig(
                skipMethods = true,
                skipPropertes = true,
            ),
        )
        classDiagram.scanConfig.include(Regex("kotlin\\.reflect\\.[^.]*"))
        classDiagram.scanPackage("kotlin.reflect")
        classDiagram.write(File(targetDir, "kotlinInheritance.plantuml"))
    }

    @Test
    fun umlModel() {
        val classDiagram = ClassDiagram()
        classDiagram.scanKClass(UmlClass::class)
        classDiagram.write(File(targetDir, "umlModel.plantuml"))
    }
}
