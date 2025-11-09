package hu.webhejj.perspektive.python

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import java.io.File
import kotlin.test.Test

class PythonAstJsonReaderTest {

    val objectMapper = jacksonObjectMapper()
    val astFile = File("/Users/greg/Dev/git/perspektive/src/test/resources/python/ast.json")

    @Test
    fun test() {

        val ast = objectMapper.readValue<JsonNode>(astFile)

        val body: List<JsonNode> = ast.required("body")

        body.forEach { statement ->
            val type = statement.required<String>("_type")
            when(type) {
                "Import", "ImportFrom" -> {} // ignore
                "Assign" -> variable(statement)
                "FunctionDef" -> function(statement)
                else -> println("Unhandled statement type: $type")
            }

        }
        expectThat(ast) {
            get { get("_type") }.isEqualTo("Module")
        }
    }

    private fun variable(node: JsonNode) {
        val targets: List<JsonNode> = node.required("targets")
        // val value = type.required<JsonNode>("value").required<String>("value")

        targets.forEach { target ->
            val id = target.required<String>("id")
            println("  var: $id")
        }
    }

    private fun function(node: JsonNode) {
        val name: String = node.required("name")
        println("  fun: $name")
    }
}

typealias JsonNode = Map<String, Any?>
inline fun <reified T: Any> JsonNode.required(key: String): T {
    val value = get(key) ?: error("Property not found: $key")
    return value as T
}

data class AstNode(val jsonNode: JsonNode): JsonNode by jsonNode {
    val type get() = required<String>("_type")
}


