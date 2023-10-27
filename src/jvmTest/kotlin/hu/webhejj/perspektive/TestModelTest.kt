package hu.webhejj.perspektive

import hu.webhejj.perspektive.testmodel.GenericDataClass
import hu.webhejj.perspektive.testmodel.GenericDataClass2
import hu.webhejj.perspektive.testmodel.ListContainer
import hu.webhejj.perspektive.testmodel.MapContainer
import hu.webhejj.perspektive.testmodel.SubClass
import org.junit.jupiter.api.Test
import java.io.File

class TestModelTest {

    private val targetDir = File("build/ktuml/")

    @Test
    fun testModel() {
        val classDiagram = ClassDiagram()
        classDiagram.scanKClass(SubClass::class)
        classDiagram.scanKClass(GenericDataClass2::class)
        classDiagram.write(File(targetDir, "testModel.plantuml"))
    }

    @Test
    fun testTypeParameters() {
        val classDiagram = ClassDiagram()
        classDiagram.scanKClass(GenericDataClass::class)
        classDiagram.write(File(targetDir, "testTypeParameters.plantuml"))
    }

    @Test
    fun testListFields() {
        val classDiagram = ClassDiagram()
        classDiagram.scanKClass(ListContainer::class)
        classDiagram.write(File(targetDir, "testListFields.plantuml"))
    }

    @Test
    fun testMapFields() {
        val classDiagram = ClassDiagram()
        classDiagram.scanKClass(MapContainer::class)
        classDiagram.write(File(targetDir, "testMapFields.plantuml"))
    }
}
