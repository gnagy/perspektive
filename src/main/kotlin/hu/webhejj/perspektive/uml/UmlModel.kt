package hu.webhejj.perspektive.uml

data class UmlClass(
    val name: UmlName,
    val kind: Kind,
    val isAbstract: Boolean,
    val stereotypes: List<String>,
    val typeParameters: List<UmlTypeParameter>,
    val superClasses: List<UmlInheritance>,
    val members: List<UmlMember>,
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

data class UmlMember(
    val kind: UmlMember.Kind,
    val visibility: UmlVisibility?,
    val name: String,
    val type: UmlName,
    val typeProjections: List<UmlTypeProjection>,
    val parameters: List<String>,
    val isAbstract: Boolean,
    val isStatic: Boolean,
    val cardinality: UmlCardinality,
) {
    enum class Kind {
        PROPERTY,
        METHOD,
        ENUM_VALUE,
    }
}

enum class UmlVisibility {
    PRIVATE,
    PROTECTED,
    PUBLIC,
    INTERNAL,
}

enum class UmlCardinality {
    OPTIONAL,
    SCALAR,
    VECTOR,
}
