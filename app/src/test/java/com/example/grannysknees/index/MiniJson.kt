package com.example.grannysknees.index

import java.math.BigDecimal

/**
 * Minimal, dependency-free JSON reader for the contract fixtures.
 *
 * It exists so the fixture tests need no third-party JSON library and no change
 * to the Gradle version catalogue. It parses the JSON subset used by the
 * fixtures into plain Kotlin values:
 *
 * - object  -> [LinkedHashMap] (insertion order preserved)
 * - array   -> [List]
 * - string  -> [String]
 * - number  -> [BigDecimal] (exact decimal from the source token)
 * - boolean -> [Boolean]
 * - null    -> `null`
 *
 * Numbers are kept as [BigDecimal] so fixture comparisons stay exact, with no
 * floating-point tolerance.
 */
object MiniJson {

    fun parse(text: String): Any? {
        val parser = Parser(text)
        parser.skipWhitespace()
        val value = parser.readValue()
        parser.skipWhitespace()
        require(parser.atEnd()) { "Trailing content at index ${parser.index}" }
        return value
    }

    private class Parser(private val s: String) {
        var index = 0

        fun atEnd(): Boolean = index >= s.length

        fun skipWhitespace() {
            while (index < s.length && s[index].isJsonWhitespace()) index++
        }

        fun readValue(): Any? {
            skipWhitespace()
            require(index < s.length) { "Unexpected end of input" }
            return when (s[index]) {
                '{' -> readObject()
                '[' -> readArray()
                '"' -> readString()
                't', 'f' -> readBoolean()
                'n' -> readNull()
                else -> readNumber()
            }
        }

        private fun readObject(): LinkedHashMap<String, Any?> {
            expect('{')
            val result = LinkedHashMap<String, Any?>()
            skipWhitespace()
            if (peek() == '}') {
                index++
                return result
            }
            while (true) {
                skipWhitespace()
                val key = readString()
                skipWhitespace()
                expect(':')
                result[key] = readValue()
                skipWhitespace()
                when (val c = nextChar()) {
                    ',' -> continue
                    '}' -> break
                    else -> error("Expected ',' or '}' but found '$c' at index ${index - 1}")
                }
            }
            return result
        }

        private fun readArray(): List<Any?> {
            expect('[')
            val result = ArrayList<Any?>()
            skipWhitespace()
            if (peek() == ']') {
                index++
                return result
            }
            while (true) {
                result.add(readValue())
                skipWhitespace()
                when (val c = nextChar()) {
                    ',' -> continue
                    ']' -> break
                    else -> error("Expected ',' or ']' but found '$c' at index ${index - 1}")
                }
            }
            return result
        }

        private fun readString(): String {
            expect('"')
            val sb = StringBuilder()
            while (true) {
                require(index < s.length) { "Unterminated string" }
                when (val c = s[index++]) {
                    '"' -> return sb.toString()
                    '\\' -> {
                        require(index < s.length) { "Unterminated escape" }
                        when (val e = s[index++]) {
                            '"' -> sb.append('"')
                            '\\' -> sb.append('\\')
                            '/' -> sb.append('/')
                            'b' -> sb.append('\b')
                            'f' -> sb.append('\u000C')
                            'n' -> sb.append('\n')
                            'r' -> sb.append('\r')
                            't' -> sb.append('\t')
                            'u' -> {
                                val hex = s.substring(index, index + 4)
                                sb.append(hex.toInt(16).toChar())
                                index += 4
                            }
                            else -> error("Invalid escape '\\$e'")
                        }
                    }
                    else -> sb.append(c)
                }
            }
        }

        private fun readBoolean(): Boolean =
            when {
                s.startsWith("true", index) -> { index += 4; true }
                s.startsWith("false", index) -> { index += 5; false }
                else -> error("Invalid literal at index $index")
            }

        private fun readNull(): Any? {
            require(s.startsWith("null", index)) { "Invalid literal at index $index" }
            index += 4
            return null
        }

        private fun readNumber(): BigDecimal {
            val start = index
            while (index < s.length && s[index] in NUMBER_CHARS) index++
            require(index > start) { "Invalid number at index $start" }
            return BigDecimal(s.substring(start, index))
        }

        private fun expect(c: Char) {
            val actual = nextChar()
            require(actual == c) { "Expected '$c' but found '$actual' at index ${index - 1}" }
        }

        private fun nextChar(): Char {
            require(index < s.length) { "Unexpected end of input" }
            return s[index++]
        }

        private fun peek(): Char {
            require(index < s.length) { "Unexpected end of input" }
            return s[index]
        }

        private fun Char.isJsonWhitespace(): Boolean =
            this == ' ' || this == '\n' || this == '\r' || this == '\t'

        private companion object {
            const val NUMBER_CHARS = "-+0123456789.eE"
        }
    }
}
