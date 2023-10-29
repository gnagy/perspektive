package hu.webhejj.perspektive

import hu.webhejj.perspektive.testmodel.AbstractClass
import hu.webhejj.perspektive.testmodel.GenericDataClass
import hu.webhejj.perspektive.testmodel.GenericDataClass2
import hu.webhejj.perspektive.testmodel.ListContainer
import hu.webhejj.perspektive.testmodel.MapContainer
import hu.webhejj.perspektive.testmodel.SubClass
import hu.webhejj.perspektive.testmodel.TestJavaModel
import org.junit.jupiter.api.Test
import java.io.File

class TestModelTest {

    private val targetDir = File("build/ktuml/")

    @Test
    fun testModel() {
        val classDiagram = ClassDiagram()
        classDiagram.scanKClass(SubClass::class)
        classDiagram.scanKClass(GenericDataClass2::class)
        classDiagram.renderWithPlantUml(File(targetDir, "testModel.plantuml"))
    }

    @Test
    fun testTypeParameters() {
        val classDiagram = ClassDiagram()
        classDiagram.scanKClass(GenericDataClass::class)
        classDiagram.renderWithPlantUml(File(targetDir, "testTypeParameters.plantuml"))
    }

    @Test
    fun testListFields() {
        val classDiagram = ClassDiagram()
        classDiagram.scanKClass(ListContainer::class)
        classDiagram.renderWithPlantUml(File(targetDir, "testListFields.plantuml"))
    }

    @Test
    fun testMapFields() {
        val classDiagram = ClassDiagram()
        classDiagram.scanKClass(MapContainer::class)
        classDiagram.renderWithPlantUml(File(targetDir, "testMapFields.plantuml"))
    }

    @Test
    fun testAbstractClass() {
        val classDiagram = ClassDiagram()
        classDiagram.scanKClass(AbstractClass::class)
        classDiagram.renderWithPlantUml(File(targetDir, "testAbstractClass.plantuml"))
    }

    @Test
    fun testStatic() {
        val classDiagram = ClassDiagram()
        classDiagram.scanKClass(TestJavaModel::class)
        classDiagram.renderWithPlantUml(File(targetDir, "testStatic.plantuml"))
    }
}
