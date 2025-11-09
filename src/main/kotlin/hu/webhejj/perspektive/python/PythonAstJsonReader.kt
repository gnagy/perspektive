package hu.webhejj.perspektive.python

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.nio.file.Path

class PythonAstJsonReader {

    val objectMapper = jacksonObjectMapper()

    fun read(path: Path) {
        val ast =objectMapper.readTree(path.toFile())
        ast.elements().forEach {

        }
    }
}

fun main() {
    val reader = PythonAstJsonReader()
}
