package hu.webhejj.perspektive.hu.webhejj.perspektive

import hu.webhejj.perspektive.ClassDiagram
import hu.webhejj.perspektive.scan.HideMembers
import hu.webhejj.perspektive.scan.HidePackages
import hu.webhejj.perspektive.uml.UmlClass
import org.junit.jupiter.api.Test
import java.io.File

class ClassDiagramTest {

    private val targetDir = File("build/ktuml/")

    @Test
    fun kotlinModel() {
        val classDiagram = ClassDiagram()
        classDiagram.scanConfig.exclusionRules.add(HidePackages(includes = listOf("kotlin.reflect")))
        classDiagram.scanPackage("kotlin.reflect")
        classDiagram.renderWithPlantUml(File(targetDir, "kotlinModel.plantuml"))
    }

    @Test
    fun kotlinInheritance() {
        val classDiagram = ClassDiagram()
        classDiagram.scanConfig.exclusionRules.add(HidePackages(includes = listOf("kotlin.reflect"), excludes = listOf("kotlin.reflect.jvm.internal")))
        classDiagram.scanConfig.exclusionRules.add(HideMembers())
        classDiagram.scanPackage("kotlin.reflect")
        classDiagram.renderWithPlantUml(File(targetDir, "kotlinInheritance.plantuml"))
    }

    @Test
    fun umlModel() {
        val classDiagram = ClassDiagram()
        classDiagram.scanKClass(UmlClass::class)
        classDiagram.renderWithPlantUml(File(targetDir, "umlModel.plantuml"))
    }
}
