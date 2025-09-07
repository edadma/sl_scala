package io.github.edadma.sl_scala
package parser

import io.github.edadma.sl_scala.lexer.{Token, SourceLocation}

// Error codes for C-compatible error handling (no exceptions)
object ErrorCode extends Enumeration {
  type ErrorCode = Value
  
  val ERROR_NONE = Value("No error")
  val ERROR_UNEXPECTED_TOKEN = Value("Unexpected token")
  val ERROR_EXPECTED_TOKEN = Value("Expected token")
  val ERROR_EXPECTED_EXPRESSION = Value("Expected expression")
  val ERROR_EXPECTED_STATEMENT = Value("Expected statement")
  val ERROR_EXPECTED_IDENTIFIER = Value("Expected identifier")
  val ERROR_EXPECTED_SEMICOLON = Value("Expected semicolon")
  val ERROR_EXPECTED_COMMA = Value("Expected comma")
  val ERROR_EXPECTED_PAREN = Value("Expected parenthesis")
  val ERROR_EXPECTED_BRACE = Value("Expected brace")
  val ERROR_EXPECTED_BRACKET = Value("Expected bracket")
  val ERROR_EXPECTED_EQUALS = Value("Expected equals")
  val ERROR_EXPECTED_ARROW = Value("Expected arrow")
  val ERROR_EXPECTED_INDENT = Value("Expected indentation")
  val ERROR_EXPECTED_DEDENT = Value("Expected dedent")
  val ERROR_EXPECTED_NEWLINE = Value("Expected newline")
  val ERROR_EXPECTED_END_MARKER = Value("Expected end marker")
  val ERROR_INVALID_ASSIGNMENT_TARGET = Value("Invalid assignment target")
  val ERROR_INVALID_PATTERN = Value("Invalid pattern")
  val ERROR_DUPLICATE_PARAMETER = Value("Duplicate parameter")
  val ERROR_UNEXPECTED_EOF = Value("Unexpected end of file")
  val ERROR_MISMATCHED_INDENT = Value("Mismatched indentation")
  val ERROR_INVALID_IMPORT = Value("Invalid import statement")
  val ERROR_INVALID_FUNCTION_DECL = Value("Invalid function declaration")
  val ERROR_INVALID_DATA_DECL = Value("Invalid data declaration")
  val ERROR_INVALID_TEMPLATE = Value("Invalid template literal")
  val ERROR_INTERNAL = Value("Internal parser error")
}

import ErrorCode._

// Parse result - C-style error handling
case class ParseResult[T](
  result: T,         // null if error occurred
  error: ErrorCode,
  errorMessage: String = "",
  errorLocation: SourceLocation = null
) {
  def isSuccess(): Boolean = error == ERROR_NONE && result != null
  def isError(): Boolean = !isSuccess()
}

// Parser error reporter
class ParseErrorReporter(fileName: String = "<unknown>") {
  
  private val errors = scala.collection.mutable.ArrayBuffer[String]()
  
  def reportError(error: ErrorCode, message: String, location: SourceLocation): Unit = {
    val fullMessage = if (location != null) {
      s"$fileName:${location.line}:${location.column}: ${error.toString}: $message"
    } else {
      s"$fileName: ${error.toString}: $message"
    }
    errors += fullMessage
  }
  
  def reportError(error: ErrorCode, message: String, token: Token): Unit = {
    reportError(error, message, token.location)
  }
  
  def reportUnexpectedToken(expected: String, actual: Token): Unit = {
    val message = s"Expected $expected, got '${actual.lexeme}'"
    reportError(ERROR_UNEXPECTED_TOKEN, message, actual.location)
  }
  
  def reportExpectedToken(expected: String, actual: Token): Unit = {
    val message = s"Expected $expected, got '${actual.lexeme}'"
    reportError(ERROR_EXPECTED_TOKEN, message, actual.location)
  }
  
  def reportUnexpectedEOF(expected: String): Unit = {
    val message = s"Expected $expected, got end of file"
    reportError(ERROR_UNEXPECTED_EOF, message, null.asInstanceOf[SourceLocation])
  }
  
  def hasErrors(): Boolean = errors.nonEmpty
  
  def getErrors(): Array[String] = errors.toArray
  
  def clearErrors(): Unit = errors.clear()
  
  def getErrorCount(): Int = errors.length
}

// Synchronization points for error recovery
object SynchronizationPoint extends Enumeration {
  type SynchronizationPoint = Value
  
  val STATEMENT_START = Value  // Beginning of statements (var, val, def, etc.)
  val EXPRESSION_START = Value // Beginning of expressions
  val BLOCK_START = Value      // Beginning of blocks (after INDENT)
  val BLOCK_END = Value        // End of blocks (at DEDENT)
  val NEWLINE = Value          // At newlines
  val BRACE = Value            // At braces
  val SEMICOLON = Value        // At semicolons
}