package hu.webhejj.perspektive.testmodel

open class RootClass {
    class NestedClass

    val simpleNullableField: Int? = null
    val objectNullableField: TestDataClass? = null
    val anonymousClassField = object {
        val field = "value"
    }

    fun testFunction(argument: Int): String {
        return "test $argument"
    }
}

class SubClass : RootClass() {
    val subClassField = "subClassField"
}

data class TestDataClass(
    val simpleScalarField: String,
    val simpleListField: List<String>,
    val simpleMapField: Map<String, String>,
    val objectScalarField: SimpleDataClass,
    val objectMapField: Map<String, SimpleDataClass>,
    val objectListField: List<SimpleDataClass>,
    val genericObjectField: GenericDataClass<Int, String?>,
    val enumField: TestEnum,
)

data class SimpleDataClass(
    val field: String,
)

data class GenericDataClass<K, V : String?>(
    val key: K,
    val value: V,
)

data class GenericDataClass2<T : RootClass>(
    val field: T,
)

enum class TestEnum {
    ONE,
    TWO,
    THREE,
}

data class ListContainer(
    val simpleList: List<String>,
    val objectList: List<SimpleDataClass>,
    val optionalObjectList: List<SimpleDataClass>?,
)

data class MapContainer(
    val simpleMap: Map<Int, String>,
    val objectMap: Map<Int, SimpleDataClass>,
    val optionalObjectMap: Map<Int, SimpleDataClass>?,
)
