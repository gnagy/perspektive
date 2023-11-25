package hu.webhejj.perspektive.hu.webhejj.perspektive

import hu.webhejj.perspektive.ClassDiagram
import hu.webhejj.perspektive.scan.HideAllMembers
import hu.webhejj.perspektive.scan.PackageRule
import hu.webhejj.perspektive.scan.RuleDecision
import hu.webhejj.perspektive.uml.UmlClass
import org.junit.jupiter.api.Test
import java.io.File

class ClassDiagramTest {

    private val targetDir = File("build/perspektive/")

    @Test
    fun kotlinModel() {
        val classDiagram = ClassDiagram()
        classDiagram.scanConfig.exclusionRules.add(PackageRule(packages = listOf("kotlin.reflect"), decision = RuleDecision.INCLUDE))
        classDiagram.scanPackage("kotlin.reflect")
        classDiagram.renderWithPlantUml(File(targetDir, "kotlinModel.plantuml"))
    }

    @Test
    fun kotlinInheritance() {
        val classDiagram = ClassDiagram()
        classDiagram.scanConfig.exclusionRules.add(PackageRule(packages = listOf("kotlin.reflect"), decision = RuleDecision.INCLUDE))
        classDiagram.scanConfig.exclusionRules.add(PackageRule(packages = listOf("kotlin.reflect.jvm.internal"), decision = RuleDecision.EXCLUDE))
        classDiagram.scanConfig.exclusionRules.add(HideAllMembers())
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
