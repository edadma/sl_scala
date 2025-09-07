package io.github.edadma.sl_scala
package parser

import io.github.edadma.sl_scala.lexer.{Token, SourceLocation}
import scala.collection.mutable.ArrayBuffer

// AST node types for C-compatible implementation
// Using simple case classes that map directly to C structs

// Base AST node - sealed trait for type safety but avoiding pattern matching
sealed trait ASTNode {
  val location: SourceLocation
  
  // Manual visitor pattern - no pattern matching for C compatibility
  def nodeType(): String = this.getClass.getSimpleName
  def isExpression(): Boolean = false
  def isStatement(): Boolean = false
}

// ============================================================================
// EXPRESSIONS - return values
// ============================================================================

abstract class Expression extends ASTNode {
  override def isExpression(): Boolean = true
}

// Literals
case class IntegerLiteral(value: Long, location: SourceLocation) extends Expression

case class FloatLiteral(value: Double, location: SourceLocation) extends Expression

case class StringLiteral(value: String, location: SourceLocation) extends Expression

case class BooleanLiteral(value: Boolean, location: SourceLocation) extends Expression

case class NullLiteral(location: SourceLocation) extends Expression

case class UndefinedLiteral(location: SourceLocation) extends Expression

case class NaNLiteral(location: SourceLocation) extends Expression

case class InfinityLiteral(location: SourceLocation) extends Expression

// Identifiers
case class Identifier(name: String, location: SourceLocation) extends Expression

// Binary expressions
case class BinaryExpression(
  operator: String,
  left: Expression, 
  right: Expression,
  location: SourceLocation
) extends Expression

// Unary expressions  
case class UnaryExpression(
  operator: String,
  operand: Expression,
  isPrefix: Boolean,  // true for ++x, false for x++
  location: SourceLocation
) extends Expression

// Assignment expressions
case class AssignmentExpression(
  target: Expression,
  operator: String,  // =, +=, -=, etc.
  value: Expression,
  location: SourceLocation
) extends Expression

// Ternary conditional
case class TernaryExpression(
  condition: Expression,
  thenExpr: Expression,
  elseExpr: Expression,
  location: SourceLocation
) extends Expression

// Function calls
case class FunctionCallExpression(
  function: Expression,
  arguments: ArrayBuffer[Expression],
  location: SourceLocation
) extends Expression

// Property access (dot notation)
case class PropertyAccessExpression(
  obj: Expression,
  property: String,
  isOptional: Boolean,  // ?. vs .
  location: SourceLocation
) extends Expression

// Array literals
case class ArrayLiteralExpression(
  elements: ArrayBuffer[Expression],
  location: SourceLocation
) extends Expression

// Array/property access using parentheses arr(0) or obj("key")
case class IndexExpression(
  obj: Expression,
  index: Expression,
  location: SourceLocation
) extends Expression

// Object literals
case class ObjectLiteralExpression(
  properties: ArrayBuffer[ObjectProperty],
  location: SourceLocation
) extends Expression

case class ObjectProperty(
  key: String,  // identifier or string
  value: Expression,
  location: SourceLocation
)

// Template literals
case class TemplateLiteralExpression(
  parts: ArrayBuffer[TemplatePart],
  location: SourceLocation
) extends Expression

// Template literal parts
sealed trait TemplatePart {
  val location: SourceLocation
}

case class TemplateTextPart(text: String, location: SourceLocation) extends TemplatePart
case class TemplateVariablePart(name: String, location: SourceLocation) extends TemplatePart  
case class TemplateExpressionPart(expression: Expression, location: SourceLocation) extends TemplatePart

// Range expressions
case class RangeExpression(
  start: Expression,
  end: Expression,
  step: Expression,  // null if no step
  isExclusive: Boolean,  // ..< vs ..
  location: SourceLocation
) extends Expression

// Anonymous function expressions
case class AnonymousFunctionExpression(
  parameters: ArrayBuffer[String],
  body: Expression,
  location: SourceLocation  
) extends Expression

// Block expressions (indented blocks that return values)
case class BlockExpression(
  statements: ArrayBuffer[ASTNode],  // mix of statements and expressions
  location: SourceLocation
) extends Expression

// Control flow expressions
case class IfExpression(
  condition: Expression,
  thenBranch: Expression,
  elifBranches: ArrayBuffer[ElifBranch],
  elseBranch: Expression,  // null if no else
  hasEndMarker: Boolean,   // true if "end if" present
  location: SourceLocation
) extends Expression

case class ElifBranch(
  condition: Expression,
  body: Expression,
  location: SourceLocation
)

case class WhileExpression(
  condition: Expression,
  body: Expression,
  hasEndMarker: Boolean,   // true if "end while" present
  location: SourceLocation
) extends Expression

case class DoWhileExpression(
  body: Expression,
  condition: Expression,
  location: SourceLocation
) extends Expression

case class ForExpression(
  init: ASTNode,      // null, var decl, or expression
  condition: Expression,  // null if empty
  update: Expression,     // null if empty
  body: Expression,
  hasEndMarker: Boolean,  // true if "end for" present
  location: SourceLocation
) extends Expression

case class LoopExpression(
  body: Expression,
  hasEndMarker: Boolean,  // true if "end loop" present
  location: SourceLocation
) extends Expression

case class MatchExpression(
  value: Expression,
  cases: ArrayBuffer[MatchCase],
  defaultCase: Expression,  // null if no default
  hasEndMarker: Boolean,   // true if "end match" present
  location: SourceLocation
) extends Expression

case class MatchCase(
  pattern: Expression,    // In Slate, patterns are expressions
  body: Expression,
  location: SourceLocation
)

// Control flow statements usable as expressions
case class BreakExpression(location: SourceLocation) extends Expression

case class ContinueExpression(location: SourceLocation) extends Expression

case class ReturnExpression(
  value: Expression,  // null if no return value
  location: SourceLocation
) extends Expression

// ============================================================================
// STATEMENTS - do not return values
// ============================================================================

abstract class Statement extends ASTNode {
  override def isStatement(): Boolean = true
}

// Variable declarations
case class VarDeclaration(
  name: String,
  initializer: Expression,  // null if no initializer
  location: SourceLocation
) extends Statement

case class ValDeclaration(
  name: String,
  initializer: Expression,  // always present for val
  location: SourceLocation
) extends Statement

// Function declarations
case class FunctionDeclaration(
  name: String,
  parameters: ArrayBuffer[String],
  body: Expression,
  hasEndMarker: Boolean,  // true if "end <name>" present
  location: SourceLocation
) extends Statement

// Import statements
case class ImportStatement(
  path: ArrayBuffer[String],      // The full import path
  importSpec: ImportSpec,          // What's being imported (wildcard, items, or nothing)
  location: SourceLocation
) extends Statement

sealed trait ImportSpec
case object WildcardSpec extends ImportSpec                              // ._
case class SelectiveSpec(items: ArrayBuffer[ImportItem]) extends ImportSpec  // .{a, b}
case object SimpleSpec extends ImportSpec                                // no suffix

case class ImportItem(
  name: String,
  alias: String,  // null if no alias
  location: SourceLocation
)

// Package statements
case class PackageStatement(
  packagePath: ArrayBuffer[String],
  location: SourceLocation
) extends Statement

// Data type declarations
case class DataDeclaration(
  name: String,
  constructors: ArrayBuffer[DataConstructor],
  methods: ArrayBuffer[FunctionDeclaration],  // methods inside data type
  isPrivate: Boolean,
  hasEndMarker: Boolean,  // true if "end <DataTypeName>" present
  location: SourceLocation
) extends Statement

case class DataConstructor(
  name: String,
  parameters: ArrayBuffer[String],
  isSingleton: Boolean,  // true for case Name, false for case Name() or case Name(params)
  location: SourceLocation
)

// Expression statements (expressions used as statements)
case class ExpressionStatement(
  expression: Expression,
  location: SourceLocation
) extends Statement

// Program root node
case class Program(
  statements: ArrayBuffer[ASTNode],
  location: SourceLocation
) extends ASTNode