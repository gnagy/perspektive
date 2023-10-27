package hu.webhejj.perspektive.uml

data class UmlClass(
    val name: UmlName,
    val kind: Kind,
    val isAbstract: Boolean,
    val stereotypes: List<String>,
    val typeParameters: List<UmlTypeParameter>,
    val superClasses: List<UmlInheritance>,
    val properties: List<UmlProperty>,
    val methods: List<UmlMethod>,
) {
    enum class Kind {
        INTERFACE,
        CLASS,
        DATA_CLASS,
        ENUM,
    }
}

data class UmlInheritance(
    val type: UmlName,
    val typeProjections: List<UmlTypeProjection>,
)

data class UmlTypeParameter(
    val name: String,
    val upperBounds: List<UmlName>,
)

data class UmlTypeProjection(
    val types: List<UmlName>,
)

data class UmlName(
    val qualified: String,
    val simple: String,
)

data class UmlProperty(
    val visibility: UmlVisibility?,
    val name: String,
    val type: UmlName,
    val isAbstract: Boolean,
    val typeProjections: List<UmlTypeProjection>,
    val cardinality: UmlCardinality,
)

data class UmlMethod(
    val visibility: UmlVisibility?,
    val name: String,
    val returnType: UmlName,
    val returnTypeProjections: List<UmlTypeProjection>,
    val parameters: List<String>,
    val isAbstract: Boolean,
)

enum class UmlVisibility {
    PRIVATE,
    PROTECTED,
    PUBLIC,
}

enum class UmlCardinality {
    OPTIONAL,
    SCALAR,
    VECTOR,
}
