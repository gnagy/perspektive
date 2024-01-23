package hu.webhejj.perspektive

import hu.webhejj.perspektive.testmodel.AbstractClass
import hu.webhejj.perspektive.testmodel.ComplexDataClass
import hu.webhejj.perspektive.testmodel.GenericDataClass
import hu.webhejj.perspektive.testmodel.GenericDataClass2
import hu.webhejj.perspektive.testmodel.ListContainer
import hu.webhejj.perspektive.testmodel.MapContainer
import hu.webhejj.perspektive.testmodel.SubClass
import hu.webhejj.perspektive.testmodel.JavaBeanModel
import hu.webhejj.perspektive.testmodel.JavaStaticModel
import org.junit.jupiter.api.Test
import java.io.File

class TestModelTest {
    private val targetDir = File("build/perspektive/")

    @Test
    fun `data class`() {
        val classDiagram = ClassDiagram()
        classDiagram.scanKClass(ComplexDataClass::class)
        classDiagram.renderWithPlantUml(File(targetDir, "data-class.plantuml"))
    }

    @Test
    fun `generic data class`() {
        val classDiagram = ClassDiagram()
        classDiagram.scanKClass(GenericDataClass::class)
        classDiagram.renderWithPlantUml(File(targetDir, "generic-data-class.plantuml"))
    }

    @Test
    fun subclass() {
        val classDiagram = ClassDiagram()
        classDiagram.scanKClass(SubClass::class)
        classDiagram.scanKClass(GenericDataClass2::class)
        classDiagram.renderWithPlantUml(File(targetDir, "subclass.plantuml"))
    }

    @Test
    fun `list container`() {
        val classDiagram = ClassDiagram()
        classDiagram.scanKClass(ListContainer::class)
        classDiagram.renderWithPlantUml(File(targetDir, "list-container.plantuml"))
    }

    @Test
    fun `map container`() {
        val classDiagram = ClassDiagram()
        classDiagram.scanKClass(MapContainer::class)
        classDiagram.renderWithPlantUml(File(targetDir, "map-container.plantuml"))
    }

    @Test
    fun `abstract class`() {
        val classDiagram = ClassDiagram()
        classDiagram.scanKClass(AbstractClass::class)
        classDiagram.renderWithPlantUml(File(targetDir, "abstract-class.plantuml"))
    }

    @Test
    fun `java static`() {
        val classDiagram = ClassDiagram()
        classDiagram.scanKClass(JavaStaticModel::class)
        classDiagram.renderWithPlantUml(File(targetDir, "java-static.plantuml"))
    }

    @Test
    fun `java bean`() {
        val classDiagram = ClassDiagram()
        classDiagram.scanKClass(JavaBeanModel::class)
        classDiagram.renderWithPlantUml(File(targetDir, "java-bean.plantuml"))
    }
}
