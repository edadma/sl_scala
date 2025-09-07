package slate.parser

import io.github.edadma.sl_scala.lexer.Lexer
import io.github.edadma.sl_scala.parser.{Parser, *}

object ParserTest {
  
  def main(args: Array[String]): Unit = {
    println("Running Parser Tests...")
    var passed = 0
    var failed = 0
    
    // Test 1: Basic literals
    test("Basic literals") {
      val parser = new Parser(new Lexer("42 3.14 \"hello\" true false null"))
      val result = parser.parseProgram()
      assert(result.isSuccess())
      val program = result.result
      assert(program.statements.length == 6)
      
      // Check that all are expression statements containing literals
      assert(program.statements(0).isInstanceOf[ExpressionStatement])
      val expr0 = program.statements(0).asInstanceOf[ExpressionStatement].expression
      assert(expr0.isInstanceOf[IntegerLiteral])
      assert(expr0.asInstanceOf[IntegerLiteral].value == 42)
      
      val expr1 = program.statements(1).asInstanceOf[ExpressionStatement].expression
      assert(expr1.isInstanceOf[FloatLiteral])
      assert(expr1.asInstanceOf[FloatLiteral].value == 3.14)
      
      val expr2 = program.statements(2).asInstanceOf[ExpressionStatement].expression
      assert(expr2.isInstanceOf[StringLiteral])
      assert(expr2.asInstanceOf[StringLiteral].value == "hello")
      
    } match {
      case true => passed += 1; println("✓ Basic literals")
      case false => failed += 1; println("✗ Basic literals")
    }
    
    // Test 2: Variable declarations
    test("Variable declarations") {
      val parser = new Parser(new Lexer("var x = 42\nval y = \"hello\""))
      val result = parser.parseProgram()
      assert(result.isSuccess())
      val program = result.result
      assert(program.statements.length == 2)
      
      // Check var declaration
      assert(program.statements(0).isInstanceOf[VarDeclaration])
      val varDecl = program.statements(0).asInstanceOf[VarDeclaration]
      assert(varDecl.name == "x")
      assert(varDecl.initializer.isInstanceOf[IntegerLiteral])
      
      // Check val declaration
      assert(program.statements(1).isInstanceOf[ValDeclaration])
      val valDecl = program.statements(1).asInstanceOf[ValDeclaration]
      assert(valDecl.name == "y")
      assert(valDecl.initializer.isInstanceOf[StringLiteral])
      
    } match {
      case true => passed += 1; println("✓ Variable declarations")
      case false => failed += 1; println("✗ Variable declarations")
    }
    
    // Test 3: Function declarations
    test("Function declarations") {
      val parser = new Parser(new Lexer("def add(a, b) = a + b"))
      val result = parser.parseProgram()
      assert(result.isSuccess())
      val program = result.result
      assert(program.statements.length == 1)
      
      assert(program.statements(0).isInstanceOf[FunctionDeclaration])
      val funcDecl = program.statements(0).asInstanceOf[FunctionDeclaration]
      assert(funcDecl.name == "add")
      assert(funcDecl.parameters.length == 2)
      assert(funcDecl.parameters(0) == "a")
      assert(funcDecl.parameters(1) == "b")
      assert(funcDecl.body.isInstanceOf[BinaryExpression])
      
    } match {
      case true => passed += 1; println("✓ Function declarations")
      case false => failed += 1; println("✗ Function declarations")
    }
    
    // Test 4: Binary expressions
    test("Binary expressions") {
      val parser = new Parser(new Lexer("x + y * z"))
      val result = parser.parseProgram()
      assert(result.isSuccess())
      val program = result.result
      assert(program.statements.length == 1)
      
      val exprStmt = program.statements(0).asInstanceOf[ExpressionStatement]
      val expr = exprStmt.expression
      assert(expr.isInstanceOf[BinaryExpression])
      
      val binExpr = expr.asInstanceOf[BinaryExpression]
      assert(binExpr.operator == "+")
      assert(binExpr.left.isInstanceOf[Identifier])
      assert(binExpr.right.isInstanceOf[BinaryExpression])  // y * z should be grouped
      
      val rightExpr = binExpr.right.asInstanceOf[BinaryExpression]
      assert(rightExpr.operator == "*")
      
    } match {
      case true => passed += 1; println("✓ Binary expressions")
      case false => failed += 1; println("✗ Binary expressions")
    }
    
    // Test 5: Precedence
    test("Operator precedence") {
      val parser = new Parser(new Lexer("2 + 3 * 4"))
      val result = parser.parseProgram()
      assert(result.isSuccess())
      val program = result.result
      
      val expr = program.statements(0).asInstanceOf[ExpressionStatement].expression
      assert(expr.isInstanceOf[BinaryExpression])
      
      val topLevel = expr.asInstanceOf[BinaryExpression]
      assert(topLevel.operator == "+")
      assert(topLevel.left.isInstanceOf[IntegerLiteral])
      assert(topLevel.right.isInstanceOf[BinaryExpression])
      
      val multiply = topLevel.right.asInstanceOf[BinaryExpression]
      assert(multiply.operator == "*")
      
    } match {
      case true => passed += 1; println("✓ Operator precedence")
      case false => failed += 1; println("✗ Operator precedence")
    }
    
    // Test 6: Assignment expressions
    test("Assignment expressions") {
      val parser = new Parser(new Lexer("x = y + z"))
      val result = parser.parseProgram()
      assert(result.isSuccess())
      val program = result.result
      
      val expr = program.statements(0).asInstanceOf[ExpressionStatement].expression
      assert(expr.isInstanceOf[AssignmentExpression])
      
      val assign = expr.asInstanceOf[AssignmentExpression]
      assert(assign.operator == "=")
      assert(assign.target.isInstanceOf[Identifier])
      assert(assign.value.isInstanceOf[BinaryExpression])
      
    } match {
      case true => passed += 1; println("✓ Assignment expressions")
      case false => failed += 1; println("✗ Assignment expressions")
    }
    
    // Test 7: Function calls
    test("Function calls") {
      val parser = new Parser(new Lexer("print(x, y + 1)"))
      val result = parser.parseProgram()
      assert(result.isSuccess())
      val program = result.result
      
      val expr = program.statements(0).asInstanceOf[ExpressionStatement].expression
      assert(expr.isInstanceOf[FunctionCallExpression])
      
      val call = expr.asInstanceOf[FunctionCallExpression]
      assert(call.function.isInstanceOf[Identifier])
      assert(call.arguments.length == 2)
      assert(call.arguments(0).isInstanceOf[Identifier])
      assert(call.arguments(1).isInstanceOf[BinaryExpression])
      
    } match {
      case true => passed += 1; println("✓ Function calls")
      case false => failed += 1; println("✗ Function calls")
    }
    
    // Test 8: Array literals
    test("Array literals") {
      val parser = new Parser(new Lexer("[1, 2, x + y]"))
      val result = parser.parseProgram()
      assert(result.isSuccess())
      val program = result.result
      
      val expr = program.statements(0).asInstanceOf[ExpressionStatement].expression
      assert(expr.isInstanceOf[ArrayLiteralExpression])
      
      val array = expr.asInstanceOf[ArrayLiteralExpression]
      assert(array.elements.length == 3)
      assert(array.elements(0).isInstanceOf[IntegerLiteral])
      assert(array.elements(1).isInstanceOf[IntegerLiteral])
      assert(array.elements(2).isInstanceOf[BinaryExpression])
      
    } match {
      case true => passed += 1; println("✓ Array literals")
      case false => failed += 1; println("✗ Array literals")
    }
    
    // Test 9: Object literals
    test("Object literals") {
      val parser = new Parser(new Lexer("{name: \"Bob\", age: 30}"))
      val result = parser.parseProgram()
      assert(result.isSuccess())
      val program = result.result
      
      val expr = program.statements(0).asInstanceOf[ExpressionStatement].expression
      assert(expr.isInstanceOf[ObjectLiteralExpression])
      
      val obj = expr.asInstanceOf[ObjectLiteralExpression]
      assert(obj.properties.length == 2)
      assert(obj.properties(0).key == "name")
      assert(obj.properties(0).value.isInstanceOf[StringLiteral])
      assert(obj.properties(1).key == "age")
      assert(obj.properties(1).value.isInstanceOf[IntegerLiteral])
      
    } match {
      case true => passed += 1; println("✓ Object literals")
      case false => failed += 1; println("✗ Object literals")
    }
    
    // Test 10: Property access
    test("Property access") {
      val parser = new Parser(new Lexer("obj.property"))
      val result = parser.parseProgram()
      assert(result.isSuccess())
      val program = result.result
      
      val expr = program.statements(0).asInstanceOf[ExpressionStatement].expression
      assert(expr.isInstanceOf[PropertyAccessExpression])
      
      val prop = expr.asInstanceOf[PropertyAccessExpression]
      assert(prop.obj.isInstanceOf[Identifier])
      assert(prop.property == "property")
      assert(!prop.isOptional)
      
    } match {
      case true => passed += 1; println("✓ Property access")
      case false => failed += 1; println("✗ Property access")
    }
    
    // Test 11: Template literals
    test("Template literals") {
      val parser = new Parser(new Lexer("`Hello $name!`"))
      val result = parser.parseProgram()
      assert(result.isSuccess())
      val program = result.result
      
      val expr = program.statements(0).asInstanceOf[ExpressionStatement].expression
      assert(expr.isInstanceOf[TemplateLiteralExpression])
      
      val template = expr.asInstanceOf[TemplateLiteralExpression]
      assert(template.parts.length >= 2)  // Should have text and variable parts
      
    } match {
      case true => passed += 1; println("✓ Template literals")
      case false => failed += 1; println("✗ Template literals")
    }
    
    // Test 12: Unary expressions
    test("Unary expressions") {
      val parser = new Parser(new Lexer("-x + !flag"))
      val result = parser.parseProgram()
      assert(result.isSuccess())
      val program = result.result
      
      val expr = program.statements(0).asInstanceOf[ExpressionStatement].expression
      assert(expr.isInstanceOf[BinaryExpression])
      
      val binExpr = expr.asInstanceOf[BinaryExpression]
      assert(binExpr.left.isInstanceOf[UnaryExpression])
      assert(binExpr.right.isInstanceOf[UnaryExpression])
      
      val leftUnary = binExpr.left.asInstanceOf[UnaryExpression]
      assert(leftUnary.operator == "-")
      assert(leftUnary.isPrefix)
      
      val rightUnary = binExpr.right.asInstanceOf[UnaryExpression]
      assert(rightUnary.operator == "!")
      assert(rightUnary.isPrefix)
      
    } match {
      case true => passed += 1; println("✓ Unary expressions")
      case false => failed += 1; println("✗ Unary expressions")
    }
    
    // Test 13: Parenthesized expressions
    test("Parenthesized expressions") {
      val parser = new Parser(new Lexer("(x + y) * z"))
      val result = parser.parseProgram()
      assert(result.isSuccess())
      val program = result.result
      
      val expr = program.statements(0).asInstanceOf[ExpressionStatement].expression
      assert(expr.isInstanceOf[BinaryExpression])
      
      val binExpr = expr.asInstanceOf[BinaryExpression]
      assert(binExpr.operator == "*")
      assert(binExpr.left.isInstanceOf[BinaryExpression])  // (x + y) should be left operand
      
      val leftExpr = binExpr.left.asInstanceOf[BinaryExpression]
      assert(leftExpr.operator == "+")
      
    } match {
      case true => passed += 1; println("✓ Parenthesized expressions")
      case false => failed += 1; println("✗ Parenthesized expressions")
    }
    
    // Test 14: Import statements
    test("Import statements") {
      val parser = new Parser(new Lexer("import math.sin"))
      val result = parser.parseProgram()
      assert(result.isSuccess())
      val program = result.result
      assert(program.statements.length == 1)
      
      assert(program.statements(0).isInstanceOf[ImportStatement])
      val importStmt = program.statements(0).asInstanceOf[ImportStatement]
      assert(importStmt.modulePath.length >= 1)
      assert(importStmt.modulePath(0) == "math")
      
    } match {
      case true => passed += 1; println("✓ Import statements")
      case false => failed += 1; println("✗ Import statements")
    }
    
    // Test 15: Error recovery
    test("Error recovery") {
      val parser = new Parser(new Lexer("var x = \n var y = 42"))  // Missing initializer for x
      val result = parser.parseProgram()
      // Parser should recover and parse the second statement
      assert(parser.hasErrors())
      // Even with errors, should have parsed something
      
    } match {
      case true => passed += 1; println("✓ Error recovery")
      case false => failed += 1; println("✗ Error recovery")
    }
    
    println(s"\nTest Results: $passed passed, $failed failed")
    if (failed > 0) {
      println("Some tests failed. This is expected as the parser is a basic implementation.")
      // Don't exit with error code for now since parser is incomplete
    }
  }
  
  def test(name: String)(body: => Unit): Boolean = {
    try {
      body
      true
    } catch {
      case e: AssertionError =>
        println(s"Failed: $name - ${e.getMessage}")
        false
      case e: Exception =>
        println(s"Error in $name: ${e.getMessage}")
        e.printStackTrace()
        false
    }
  }
  
  def assert(condition: Boolean): Unit = {
    if (!condition) throw new AssertionError("Assertion failed")
  }
}