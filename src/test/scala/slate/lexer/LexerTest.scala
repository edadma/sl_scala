package slate.lexer

import io.github.edadma.sl_scala.lexer.{Lexer, TokenType}

object LexerTest {
  
  def main(args: Array[String]): Unit = {
    println("Running Lexer Tests...")
    var passed = 0
    var failed = 0
    
    // Test 1: Basic tokens
    test("Basic tokens") {
      val lexer = new Lexer("var x = 42")
      val tokens = lexer.getAllTokens()
      assert(tokens.length == 5)
      assert(tokens(0).tokenType == TokenType.TOKEN_VAR)
      assert(tokens(1).tokenType == TokenType.TOKEN_IDENTIFIER)
      assert(tokens(1).lexeme == "x")
      assert(tokens(2).tokenType == TokenType.TOKEN_EQUAL)
      assert(tokens(3).tokenType == TokenType.TOKEN_INTEGER)
      assert(tokens(3).value == 42)
      assert(tokens(4).tokenType == TokenType.TOKEN_EOF)
    } match {
      case true => passed += 1; println("✓ Basic tokens")
      case false => failed += 1; println("✗ Basic tokens")
    }
    
    // Test 2: Keywords
    test("Keywords") {
      val lexer = new Lexer("if then else while for def val var")
      val tokens = lexer.getAllTokens()
      assert(tokens(0).tokenType == TokenType.TOKEN_IF)
      assert(tokens(1).tokenType == TokenType.TOKEN_THEN)
      assert(tokens(2).tokenType == TokenType.TOKEN_ELSE)
      assert(tokens(3).tokenType == TokenType.TOKEN_WHILE)
      assert(tokens(4).tokenType == TokenType.TOKEN_FOR)
      assert(tokens(5).tokenType == TokenType.TOKEN_DEF)
      assert(tokens(6).tokenType == TokenType.TOKEN_VAL)
      assert(tokens(7).tokenType == TokenType.TOKEN_VAR)
    } match {
      case true => passed += 1; println("✓ Keywords")
      case false => failed += 1; println("✗ Keywords")
    }
    
    // Test 3: Numbers
    test("Numbers") {
      val lexer = new Lexer("42 3.14 0xFF 1.23e10")
      val tokens = lexer.getAllTokens()
      assert(tokens(0).tokenType == TokenType.TOKEN_INTEGER && tokens(0).value == 42)
      assert(tokens(1).tokenType == TokenType.TOKEN_FLOAT && tokens(1).value.asInstanceOf[Double] == 3.14)
      assert(tokens(2).tokenType == TokenType.TOKEN_INTEGER && tokens(2).value == 255)
      assert(tokens(3).tokenType == TokenType.TOKEN_FLOAT)
    } match {
      case true => passed += 1; println("✓ Numbers")
      case false => failed += 1; println("✗ Numbers")
    }
    
    // Test 4: Strings
    test("Strings") {
      val lexer = new Lexer(""""hello" 'world' "with\nescape"""")
      val tokens = lexer.getAllTokens()
      assert(tokens(0).tokenType == TokenType.TOKEN_STRING && tokens(0).value == "hello")
      assert(tokens(1).tokenType == TokenType.TOKEN_STRING && tokens(1).value == "world")
      assert(tokens(2).tokenType == TokenType.TOKEN_STRING && tokens(2).value == "with\nescape")
    } match {
      case true => passed += 1; println("✓ Strings")
      case false => failed += 1; println("✗ Strings")
    }
    
    // Test 5: Operators
    test("Operators") {
      val lexer = new Lexer("+ - * / // ** == != < <= > >= && || !")
      val tokens = lexer.getAllTokens()
      assert(tokens(0).tokenType == TokenType.TOKEN_PLUS)
      assert(tokens(1).tokenType == TokenType.TOKEN_MINUS)
      assert(tokens(2).tokenType == TokenType.TOKEN_STAR)
      assert(tokens(3).tokenType == TokenType.TOKEN_SLASH)
      assert(tokens(4).tokenType == TokenType.TOKEN_SLASH_SLASH)
      assert(tokens(5).tokenType == TokenType.TOKEN_POWER)
      assert(tokens(6).tokenType == TokenType.TOKEN_EQUAL_EQUAL)
      assert(tokens(7).tokenType == TokenType.TOKEN_BANG_EQUAL)
      assert(tokens(8).tokenType == TokenType.TOKEN_LESS)
      assert(tokens(9).tokenType == TokenType.TOKEN_LESS_EQUAL)
      assert(tokens(10).tokenType == TokenType.TOKEN_GREATER)
      assert(tokens(11).tokenType == TokenType.TOKEN_GREATER_EQUAL)
      assert(tokens(12).tokenType == TokenType.TOKEN_AMP_AMP)
      assert(tokens(13).tokenType == TokenType.TOKEN_PIPE_PIPE)
      assert(tokens(14).tokenType == TokenType.TOKEN_BANG)
    } match {
      case true => passed += 1; println("✓ Operators")
      case false => failed += 1; println("✗ Operators")
    }
    
    // Test 6: Comments
    test("Comments") {
      val lexer = new Lexer("""var x = 42 \ this is a comment
\another comment
var y = 10""")
      val tokens = lexer.getAllTokens()
      assert(tokens(0).tokenType == TokenType.TOKEN_VAR)
      assert(tokens(1).tokenType == TokenType.TOKEN_IDENTIFIER && tokens(1).lexeme == "x")
      assert(tokens(2).tokenType == TokenType.TOKEN_EQUAL)
      assert(tokens(3).tokenType == TokenType.TOKEN_INTEGER)
      assert(tokens(4).tokenType == TokenType.TOKEN_NEWLINE)
      assert(tokens(5).tokenType == TokenType.TOKEN_VAR)
      assert(tokens(6).tokenType == TokenType.TOKEN_IDENTIFIER && tokens(6).lexeme == "y")
    } match {
      case true => passed += 1; println("✓ Comments")
      case false => failed += 1; println("✗ Comments")
    }
    
    // Test 7: Indentation
    test("Indentation") {
      val lexer = new Lexer("""if x > 0
    print(x)
    x = x + 1
print("done")""")
      val tokens = lexer.getAllTokens()
      var i = 0
      assert(tokens(i).tokenType == TokenType.TOKEN_IF); i += 1
      assert(tokens(i).tokenType == TokenType.TOKEN_IDENTIFIER); i += 1
      assert(tokens(i).tokenType == TokenType.TOKEN_GREATER); i += 1
      assert(tokens(i).tokenType == TokenType.TOKEN_INTEGER); i += 1
      assert(tokens(i).tokenType == TokenType.TOKEN_NEWLINE); i += 1
      assert(tokens(i).tokenType == TokenType.TOKEN_INDENT); i += 1
      assert(tokens(i).tokenType == TokenType.TOKEN_IDENTIFIER); i += 1  // print
      assert(tokens(i).tokenType == TokenType.TOKEN_LEFT_PAREN); i += 1
      assert(tokens(i).tokenType == TokenType.TOKEN_IDENTIFIER); i += 1  // x
      assert(tokens(i).tokenType == TokenType.TOKEN_RIGHT_PAREN); i += 1
      assert(tokens(i).tokenType == TokenType.TOKEN_NEWLINE); i += 1
      // Second indented line
      assert(tokens(i).tokenType == TokenType.TOKEN_IDENTIFIER); i += 1  // x
      assert(tokens(i).tokenType == TokenType.TOKEN_EQUAL); i += 1
      assert(tokens(i).tokenType == TokenType.TOKEN_IDENTIFIER); i += 1  // x
      assert(tokens(i).tokenType == TokenType.TOKEN_PLUS); i += 1
      assert(tokens(i).tokenType == TokenType.TOKEN_INTEGER); i += 1
      assert(tokens(i).tokenType == TokenType.TOKEN_NEWLINE); i += 1
      assert(tokens(i).tokenType == TokenType.TOKEN_DEDENT); i += 1
    } match {
      case true => passed += 1; println("✓ Indentation")
      case false => failed += 1; println("✗ Indentation")
    }
    
    // Test 8: Arrow functions
    test("Arrow functions") {
      val lexer = new Lexer("x -> x * 2")
      val tokens = lexer.getAllTokens()
      assert(tokens(0).tokenType == TokenType.TOKEN_IDENTIFIER)
      assert(tokens(1).tokenType == TokenType.TOKEN_ARROW)
      assert(tokens(2).tokenType == TokenType.TOKEN_IDENTIFIER)
      assert(tokens(3).tokenType == TokenType.TOKEN_STAR)
      assert(tokens(4).tokenType == TokenType.TOKEN_INTEGER)
    } match {
      case true => passed += 1; println("✓ Arrow functions")
      case false => failed += 1; println("✗ Arrow functions")
    }
    
    // Test 9: Template literals (basic)
    test("Template literals") {
      val lexer = new Lexer("`Hello $name!`")
      val tokens = lexer.getAllTokens()
      assert(tokens(0).tokenType == TokenType.TOKEN_TEMPLATE_START)
      assert(tokens(1).tokenType == TokenType.TOKEN_TEMPLATE_TEXT && tokens(1).value == "Hello ")
      assert(tokens(2).tokenType == TokenType.TOKEN_TEMPLATE_SIMPLE_VAR && tokens(2).value == "name")
      assert(tokens(3).tokenType == TokenType.TOKEN_TEMPLATE_TEXT && tokens(3).value == "!")
      assert(tokens(4).tokenType == TokenType.TOKEN_TEMPLATE_END)
    } match {
      case true => passed += 1; println("✓ Template literals")
      case false => failed += 1; println("✗ Template literals")
    }
    
    // Test 10: Range operators
    test("Range operators") {
      val lexer = new Lexer("1..10 0..<5")
      val tokens = lexer.getAllTokens()
      assert(tokens(0).tokenType == TokenType.TOKEN_INTEGER)
      assert(tokens(1).tokenType == TokenType.TOKEN_DOT_DOT)
      assert(tokens(2).tokenType == TokenType.TOKEN_INTEGER)
      assert(tokens(3).tokenType == TokenType.TOKEN_INTEGER)
      assert(tokens(4).tokenType == TokenType.TOKEN_DOT_DOT_LESS)
      assert(tokens(5).tokenType == TokenType.TOKEN_INTEGER)
    } match {
      case true => passed += 1; println("✓ Range operators")
      case false => failed += 1; println("✗ Range operators")
    }
    
    // Test 11: Boolean literals
    test("Boolean literals") {
      val lexer = new Lexer("true false null undefined")
      val tokens = lexer.getAllTokens()
      assert(tokens(0).tokenType == TokenType.TOKEN_TRUE && tokens(0).value == true)
      assert(tokens(1).tokenType == TokenType.TOKEN_FALSE && tokens(1).value == false)
      assert(tokens(2).tokenType == TokenType.TOKEN_NULL)
      assert(tokens(3).tokenType == TokenType.TOKEN_UNDEFINED)
    } match {
      case true => passed += 1; println("✓ Boolean literals")
      case false => failed += 1; println("✗ Boolean literals")
    }
    
    // Test 12: Error reporting
    test("Error reporting") {
      val lexer = new Lexer("@invalid")
      val tokens = lexer.getAllTokens()
      assert(tokens(0).tokenType == TokenType.TOKEN_ERROR)
      assert(lexer.hasErrors())
    } match {
      case true => passed += 1; println("✓ Error reporting")
      case false => failed += 1; println("✗ Error reporting")
    }
    
    println(s"\nTest Results: $passed passed, $failed failed")
    if (failed > 0) System.exit(1)
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