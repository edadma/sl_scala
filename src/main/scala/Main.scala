package io.github.edadma.sl_scala

import lexer.{Lexer, TokenType}
import parser.{Parser, Program}

import java.io.File
import scala.io.Source
import scala.util.Using

object Main {
  def main(args: Array[String]): Unit = {
    if (args.length == 0) {
      println("Usage:")
      println("  slate <file.sl>          - Parse and show basic info")
      println("  slate -tokens <file.sl>  - Show tokenization")
      println("  slate -ast <file.sl>     - Show AST")
      println("  slate -script '<code>'   - Parse code from command line")
      System.exit(1)
    }
    
    val showTokens = args.contains("-tokens")
    val showAST = args.contains("-ast") 
    val isScript = args.contains("-script")
    
    val (source, sourceName) = if (isScript) {
      // Find the script argument (the one after -script)
      val scriptIndex = args.indexOf("-script")
      if (scriptIndex >= 0 && scriptIndex + 1 < args.length) {
        (args(scriptIndex + 1), "<script>")
      } else {
        println("Error: -script requires code argument")
        System.exit(1)
        ("", "")  // Never reached
      }
    } else {
      // Find the filename (first non-flag argument)
      val fileName = args.find(!_.startsWith("-")).getOrElse {
        println("Error: No input file specified")
        System.exit(1)
        ""  // Never reached
      }
      val file = new File(fileName)
      if (!file.exists()) {
        println(s"Error: File '$fileName' not found")
        System.exit(1)
      }
      (Using(Source.fromFile(file))(_.mkString).get, fileName)
    }
    
    val lexer = new Lexer(source, sourceName)
    
    if (showTokens) {
      // Show all tokens for debugging
      println(s"=== Tokens for $sourceName ===")
      val tokens = lexer.getAllTokens()
      var lineNum = 1
      tokens.foreach { token =>
        if (token.location.line != lineNum) {
          lineNum = token.location.line
          println()
        }
        print(s"${token.typeName} ")
        if (token.tokenType == TokenType.TOKEN_NEWLINE) println()
      }
      println("\n=== End of tokens ===")
      
      if (lexer.hasErrors()) {
        println("\nErrors found:")
        lexer.getErrors().foreach(println)
      }
    } else if (showAST) {
      // Show AST for debugging
      println(s"=== AST for $sourceName ===")
      val parser = new Parser(lexer, sourceName)
      val result = parser.parseProgram()
      
      if (result.isSuccess()) {
        printAST(result.result, 0)
      } else {
        println(s"Parse error: ${result.errorMessage}")
      }
      
      if (parser.hasErrors()) {
        println("\nParse errors found:")
        parser.getErrors().foreach(println)
      }
      println("=== End of AST ===")
    } else {
      // Parse the file and check for errors
      val parser = new Parser(lexer, sourceName)
      val result = parser.parseProgram()
      
      if (lexer.hasErrors()) {
        println("Lexer errors:")
        lexer.getErrors().foreach(System.err.println)
        System.exit(1)
      } else if (parser.hasErrors()) {
        println("Parser errors:")
        parser.getErrors().foreach(System.err.println)
        System.exit(1)
      } else if (result.isSuccess()) {
        println(s"Successfully parsed $sourceName")
        val program = result.result
        println(s"  ${program.statements.length} top-level statements")
      } else {
        println(s"Parse failed: ${result.errorMessage}")
        System.exit(1)
      }
    }
  }
  
  // Helper method to print AST structure
  def printAST(node: Any, indent: Int): Unit = {
    val prefix = "  " * indent
    
    node match {
      case p: Program =>
        println(s"${prefix}Program(${p.statements.length} statements)")
        p.statements.foreach(printAST(_, indent + 1))
        
      case v: parser.VarDeclaration =>
        println(s"${prefix}VarDeclaration")
        printAST(v.pattern, indent + 1)
        if (v.initializer != null) printAST(v.initializer, indent + 1)
        
      case v: parser.ValDeclaration =>
        println(s"${prefix}ValDeclaration")
        printAST(v.pattern, indent + 1)
        printAST(v.initializer, indent + 1)
        
      case f: parser.FunctionDeclaration =>
        println(s"${prefix}FunctionDeclaration(${f.name}, params: ${f.parameters.mkString(", ")})")
        printAST(f.body, indent + 1)
        
      case e: parser.ExpressionStatement =>
        println(s"${prefix}ExpressionStatement")
        printAST(e.expression, indent + 1)
        
      case p: parser.PackageStatement =>
        println(s"${prefix}PackageStatement(${p.packagePath.mkString(".")})")
        
      case i: parser.ImportStatement =>
        val importSpecStr = i.importSpec match {
          case parser.WildcardSpec => "wildcard"
          case parser.SelectiveSpec(items) => s"selective(${items.length} items)"
          case parser.SimpleSpec => "simple"
        }
        println(s"${prefix}ImportStatement(${i.path.mkString(".")}, spec: $importSpecStr)")
        i.importSpec match {
          case parser.SelectiveSpec(items) =>
            items.foreach { item =>
              val aliasStr = if (item.alias != null) s" => ${item.alias}" else ""
              println(s"${prefix}  ImportItem(${item.name}$aliasStr)")
            }
          case _ => // No items to show for wildcard or simple imports
        }
        
      case d: parser.DataDeclaration =>
        println(s"${prefix}DataDeclaration(${d.name}, private: ${d.isPrivate}, hasEnd: ${d.hasEndMarker})")
        d.constructors.foreach { ctor =>
          val ctorType = if (ctor.isSingleton) "singleton" else "class"
          println(s"${prefix}  Constructor(${ctor.name}, type: $ctorType, params: ${ctor.parameters.mkString(", ")})")
        }
        d.methods.foreach { method =>
          println(s"${prefix}  Method:")
          printAST(method, indent + 2)
        }
        
      case b: parser.BinaryExpression =>
        println(s"${prefix}BinaryExpression(${b.operator})")
        printAST(b.left, indent + 1)
        printAST(b.right, indent + 1)
        
      case u: parser.UnaryExpression =>
        println(s"${prefix}UnaryExpression(${u.operator}, prefix: ${u.isPrefix})")
        printAST(u.operand, indent + 1)
        
      case a: parser.AssignmentExpression =>
        println(s"${prefix}AssignmentExpression(${a.operator})")
        printAST(a.target, indent + 1)
        printAST(a.value, indent + 1)
        
      case f: parser.FunctionCallExpression =>
        println(s"${prefix}FunctionCallExpression(${f.arguments.length} args)")
        printAST(f.function, indent + 1)
        f.arguments.foreach(printAST(_, indent + 1))
        
      case p: parser.PropertyAccessExpression =>
        println(s"${prefix}PropertyAccessExpression(${p.property}, optional: ${p.isOptional})")
        printAST(p.obj, indent + 1)
        
      case a: parser.ArrayLiteralExpression =>
        println(s"${prefix}ArrayLiteralExpression(${a.elements.length} elements)")
        a.elements.foreach(printAST(_, indent + 1))
        
      case o: parser.ObjectLiteralExpression =>
        println(s"${prefix}ObjectLiteralExpression(${o.properties.length} properties)")
        o.properties.foreach { prop =>
          println(s"${prefix}  Property(${prop.key})")
          printAST(prop.value, indent + 2)
        }
        
      case i: parser.Identifier =>
        println(s"${prefix}Identifier(${i.name})")
        
      case il: parser.IntegerLiteral =>
        println(s"${prefix}IntegerLiteral(${il.value})")
        
      case fl: parser.FloatLiteral =>
        println(s"${prefix}FloatLiteral(${fl.value})")
        
      case sl: parser.StringLiteral =>
        println(s"${prefix}StringLiteral(\"${sl.value}\")")
        
      case bl: parser.BooleanLiteral =>
        println(s"${prefix}BooleanLiteral(${bl.value})")
        
      case _: parser.NullLiteral =>
        println(s"${prefix}NullLiteral")
        
      case _: parser.UndefinedLiteral =>
        println(s"${prefix}UndefinedLiteral")
        
      case i: parser.IfExpression =>
        println(s"${prefix}IfExpression(${i.elifBranches.length} elif branches, hasElse: ${i.elseBranch != null}, hasEnd: ${i.hasEndMarker})")
        printAST(i.condition, indent + 1)
        printAST(i.thenBranch, indent + 1)
        i.elifBranches.foreach { elif =>
          println(s"${prefix}  ElifBranch")
          printAST(elif.condition, indent + 2)
          printAST(elif.body, indent + 2)
        }
        if (i.elseBranch != null) {
          println(s"${prefix}  ElseBranch")
          printAST(i.elseBranch, indent + 2)
        }
        
      case w: parser.WhileExpression =>
        println(s"${prefix}WhileExpression(hasEnd: ${w.hasEndMarker})")
        printAST(w.condition, indent + 1)
        printAST(w.body, indent + 1)
        
      case dw: parser.DoWhileExpression =>
        println(s"${prefix}DoWhileExpression")
        printAST(dw.body, indent + 1)
        printAST(dw.condition, indent + 1)
        
      case f: parser.ForExpression =>
        println(s"${prefix}ForExpression(hasEnd: ${f.hasEndMarker})")
        if (f.init != null) {
          println(s"${prefix}  Init:")
          printAST(f.init, indent + 2)
        }
        if (f.condition != null) {
          println(s"${prefix}  Condition:")
          printAST(f.condition, indent + 2)
        }
        if (f.update != null) {
          println(s"${prefix}  Update:")
          printAST(f.update, indent + 2)
        }
        println(s"${prefix}  Body:")
        printAST(f.body, indent + 2)
        
      case l: parser.LoopExpression =>
        println(s"${prefix}LoopExpression(hasEnd: ${l.hasEndMarker})")
        printAST(l.body, indent + 1)
        
      case m: parser.MatchExpression =>
        println(s"${prefix}MatchExpression(${m.cases.length} cases, hasDefault: ${m.defaultCase != null}, hasEnd: ${m.hasEndMarker})")
        println(s"${prefix}  Value:")
        printAST(m.value, indent + 2)
        m.cases.foreach { case_ =>
          println(s"${prefix}  MatchCase:")
          println(s"${prefix}    Pattern:")
          printAST(case_.pattern, indent + 3)
          println(s"${prefix}    Body:")
          printAST(case_.body, indent + 3)
        }
        if (m.defaultCase != null) {
          println(s"${prefix}  DefaultCase:")
          printAST(m.defaultCase, indent + 2)
        }
        
      case b: parser.BlockExpression =>
        println(s"${prefix}BlockExpression(${b.statements.length} statements)")
        b.statements.foreach(printAST(_, indent + 1))
        
      case _: parser.BreakExpression =>
        println(s"${prefix}BreakExpression")
        
      case _: parser.ContinueExpression =>
        println(s"${prefix}ContinueExpression")
        
      case r: parser.ReturnExpression =>
        println(s"${prefix}ReturnExpression")
        if (r.value != null) printAST(r.value, indent + 1)
        
      case t: parser.TernaryExpression =>
        println(s"${prefix}TernaryExpression")
        printAST(t.condition, indent + 1)
        printAST(t.thenExpr, indent + 1)
        printAST(t.elseExpr, indent + 1)
        
      case tl: parser.TemplateLiteralExpression =>
        println(s"${prefix}TemplateLiteralExpression(${tl.parts.length} parts)")
        tl.parts.foreach { part =>
          part match {
            case tp: parser.TemplateTextPart =>
              println(s"${prefix}  TemplateText(\"${tp.text}\")")
            case tv: parser.TemplateVariablePart =>
              println(s"${prefix}  TemplateVariable(${tv.name})")
            case te: parser.TemplateExpressionPart =>
              println(s"${prefix}  TemplateExpression:")
              printAST(te.expression, indent + 2)
          }
        }
        
      // Pattern nodes
      case p: parser.IdentifierPattern =>
        println(s"${prefix}IdentifierPattern(${p.name})")
        
      case p: parser.LiteralPattern =>
        println(s"${prefix}LiteralPattern")
        printAST(p.value, indent + 1)
        
      case p: parser.ConstructorPattern =>
        println(s"${prefix}ConstructorPattern(${p.name}, ${p.args.length} args)")
        p.args.foreach(printAST(_, indent + 1))
        
      case p: parser.ArrayPattern =>
        println(s"${prefix}ArrayPattern(${p.elements.length} elements${if (p.restPattern != null) s", rest: ${p.restPattern}" else ""})")
        p.elements.foreach(printAST(_, indent + 1))
        
      case p: parser.ObjectPattern =>
        println(s"${prefix}ObjectPattern(${p.properties.length} properties)")
        p.properties.foreach(printAST(_, indent + 1))
        
      case p: parser.ObjectPatternProperty =>
        println(s"${prefix}ObjectPatternProperty(${p.key})")
        printAST(p.pattern, indent + 1)
        
      case other =>
        println(s"${prefix}${other.getClass.getSimpleName}")
    }
  }
}