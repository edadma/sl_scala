# Slate Language Grammar

## Overview
Slate is a dynamically-typed programming language with expression-oriented syntax. Control flow constructs are expressions that return values, while declarations and imports are statements that do not return values.

## Lexical Elements

### Comments
```
comment        ::= '\' [^\r\n]*
```

### Literals
```
number         ::= integer | float | hexadecimal
integer        ::= [0-9]+
float          ::= [0-9]+ '.' [0-9]+
hexadecimal    ::= '0x' [0-9a-fA-F]+
string         ::= '"' ([^"\\] | '\\' .)* '"'
                 | "'" ([^'\\] | '\\' .)* "'"
boolean        ::= 'true' | 'false'
null           ::= 'null'
undefined      ::= 'undefined'
```

### Identifiers
```
identifier     ::= [a-zA-Z_][a-zA-Z0-9_]*
```

### Keywords
```
keywords       ::= 'var' | 'val' | 'def' | 'if' | 'then' | 'elif' | 'else'
                 | 'while' | 'do' | 'for' | 'loop' | 'break' | 'continue'
                 | 'return' | 'import' | 'package' | 'private' | 'match' | 'case' | 'default' | 'data'
                 | 'true' | 'false' | 'null' | 'undefined' | 'NaN' | 'Infinity'
                 | 'and' | 'or' | 'not' | 'in' | 'instanceof' | 'mod' | 'step' | 'end' | 'typeof'
```

## Expressions

### Primary Expressions
```
primary        ::= identifier
                 | number
                 | string
                 | template_literal
                 | boolean
                 | null
                 | undefined
                 | NaN
                 | Infinity
                 | '(' expression ')'
                 | array_literal
                 | object_literal
                 | indented_block
                 | anonymous_function
                 | if_expression
                 | while_expression
                 | do_while_expression
                 | for_expression
                 | loop_expression
                 | match_expression
                 | break_expression
                 | continue_expression
                 | return_expression
```

### Literals
```
array_literal     ::= '[' (expression (',' expression)*)? ']'
object_literal    ::= '{' (property (',' property)*)? '}'
property          ::= identifier ':' expression
                    | string ':' expression
template_literal  ::= '`' template_part* '`'
template_part     ::= template_text             // raw text content
                    | '$' identifier            // simple variable interpolation
                    | '${' expression '}'       // expression interpolation
template_text     ::= [^`$\\]*                 // text content (no backticks, dollars, or backslashes)
                    | '\\' .                   // escaped characters
```

### Anonymous Functions
```
anonymous_function ::= identifier '->' expression                  // single parameter
                     | '(' ')' '->' expression                    // no parameters
                     | '(' parameters ')' '->' expression         // multiple parameters
```

### Control Flow Expressions
```
if_expression  ::= 'if' expression 'then' expression elif_part* else_part?
                 | 'if' expression ['then'] indented_block elif_part* else_part? ['end' 'if']

elif_part      ::= 'elif' expression 'then' expression
                 | 'elif' expression ['then'] indented_block

else_part      ::= 'else' expression

while_expression ::= 'while' expression 'do' expression
                   | 'while' expression indented_block ['end' 'while']

do_while_expression ::= 'do' expression 'while' expression
                      | 'do' indented_block 'while' expression

for_expression ::= 'for' (var_decl | expression)? ';' expression? ';' expression?
                   ('do' expression | indented_block ['end' 'for'])

loop_expression ::= 'loop' expression
                  | 'loop' indented_block ['end' 'loop']

match_expression ::= 'match' expression indented_match_block ['end' 'match']

indented_match_block ::= NEWLINE INDENT
                        match_arm+
                        (default_arm)?
                        DEDENT

match_arm      ::= 'case' pattern guard? '->' expression
default_arm    ::= 'default' expression
pattern        ::= identifier                           // Simple binding (x)
                 | literal                             // Literal pattern (42, "hello", true, null)
                 | identifier ':' identifier          // Type pattern (x: Number)
                 | identifier '(' pattern_list ')'     // Constructor pattern (Node(l, r))
                 | '[' pattern_list ']'                // Array destructuring ([first, second])
                 | '[' pattern_list ',' '...' identifier ']'  // Array with rest (...rest)
                 | '{' object_pattern_list '}'         // Object destructuring ({name, age})
guard          ::= 'if' expression                     // Pattern guard (if x > 5)
pattern_list   ::= pattern (',' pattern)*
object_pattern_list ::= object_pattern (',' object_pattern)*
object_pattern ::= identifier                          // Property shorthand ({name})
                 | identifier ':' pattern             // Property with pattern ({x: coord})

literal        ::= number | string | boolean | 'null' | 'undefined' | 'NaN' | 'Infinity'
```

### Special Expressions (parsed as statements but usable in expression contexts)
```
break_expression    ::= 'break'
continue_expression ::= 'continue'  
return_expression   ::= 'return' expression?
```

### Postfix Expressions
```
postfix        ::= primary postfix_op*
postfix_op     ::= '(' arguments? ')'                     // function call or array access
                 | '.' identifier                        // property access
                 | '?.' identifier                       // optional chaining
arguments      ::= expression (',' expression)*
```

### Unary Expressions
```
unary          ::= postfix
                 | unary_op unary
unary_op       ::= '+' | '-' | '!' | 'not' | '~' | '++' | '--' | 'typeof'
```

### Binary Expressions (by precedence, highest to lowest)
```
power          ::= unary ('**' unary)*                   // right-associative
multiplicative ::= power (('*' | '/' | '//' | '%' | 'mod') power)*
additive       ::= multiplicative (('+' | '-') multiplicative)*
shift          ::= additive (('<<' | '>>' | '>>>') additive)*
range          ::= shift (('..' | '..<') shift ['step' shift])?
bitwise_and    ::= range ('&' range)*
bitwise_xor    ::= bitwise_and ('^' bitwise_and)*
bitwise_or     ::= bitwise_xor ('|' bitwise_xor)*
relational     ::= bitwise_or (('<' | '<=' | '>' | '>=' | 'in' | 'instanceof') bitwise_or)*
equality       ::= relational (('==' | '!=') relational)*
logical_and    ::= equality (('&&' | 'and') equality)*
logical_or     ::= logical_and (('||' | 'or') logical_and)*
null_coalesce  ::= logical_or ('??' logical_or)*
```

### Range Expressions
```
range_expr     ::= expression '..' expression               // inclusive range
                 | expression '..<' expression              // exclusive range  
                 | expression '..' expression 'step' expression    // with step
                 | expression '..<' expression 'step' expression   // exclusive with step
```

### Ternary Conditional
```
ternary        ::= null_coalesce ('?' ternary ':' ternary)?
```

### Assignment Expressions
```
assignment     ::= ternary assignment_op assignment
                 | ternary
assignment_op  ::= '=' | '+=' | '-=' | '*=' | '/=' | '//=' | '%=' | '**='
                 | '<<=' | '>>=' | '>>>=' | '&=' | '^=' | '|='
                 | '&&=' | '||=' | '??='
```

### Expression Definition
```
expression     ::= assignment
                 | anonymous_function
```

## Statements

Statements are language constructs that do not return values. They are used for declarations, imports, and other side effects.

### Variable Declarations
```
var_decl       ::= 'var' pattern ('=' expression)?
val_decl       ::= 'val' pattern '=' expression
```

### Function Declarations
```
function_decl  ::= 'def' identifier '(' parameters? ')' '=' expression
                 | 'def' identifier '(' parameters? ')' '=' indented_block ['end' identifier]
parameters     ::= identifier (',' identifier)*
```

### Import Statements
```
import_stmt    ::= 'import' module_path '._'                      // wildcard import
                 | 'import' module_path '.{' import_list '}'      // selective import
                 | 'import' module_path                           // simple import

module_path    ::= identifier ('.' identifier)*
import_list    ::= import_item (',' import_item)*
import_item    ::= identifier ('=>' identifier)?                  // with optional alias
```

### Package Statements
```
package_stmt   ::= 'package' package_path
package_path   ::= identifier ('.' identifier)*
```

### Data Type Declarations
```
data_decl      ::= ['private'] 'data' identifier
                   (NEWLINE INDENT (data_constructor | method_decl)* DEDENT)?
                   ['end' identifier]

data_constructor ::= 'case' identifier                    // singleton constructor (case object)
                   | 'case' identifier '(' parameters ')' // class constructor with parameters
                   | 'case' identifier '(' ')'            // class constructor, empty parameters

method_decl    ::= function_decl                          // methods inside data declarations
```

### Statement Types
```
statement      ::= var_decl
                 | val_decl  
                 | function_decl
                 | import_stmt
                 | package_stmt
                 | data_decl
                 | expression_stmt

expression_stmt ::= expression
```

### Block and Program Structure
```
indented_block ::= NEWLINE INDENT block DEDENT

block          ::= (statement | expression)*

program        ::= block
```

## End Markers

Slate supports optional end markers for constructs that use indented blocks:

**Supported end markers:**
- **Control flow**: `end if`, `end while`, `end for`, `end loop`, `end match`
- **Function declarations**: `end <function_name>`  
- **Data declarations**: `end <DataTypeName>`

**Rules:**
- Only available with indented blocks (not single-line forms)
- `do...while` loops don't support end markers (self-delimiting)

## Language Semantics

### Expression vs Statement
- **Expressions** return values: control flow, arithmetic, literals, function calls
- **Statements** do not return values: declarations, imports, expression statements

### Block Expression Semantics
**Indented blocks are first-class expressions** that return:
1. The value of the last expression in the block, OR
2. `undefined` if the block ends with a statement, OR  
3. `undefined` if the block is empty

### Scoping Rules
- Function parameters create local scope
- Variable declarations create local scope within current block
- Blocks create new local scopes
- Variable shadowing is allowed

### Module System
- Each `.sl` file is a module with namespace-based isolation
- **Import forms**: `import math._` (wildcard), `import math.{sin, cos}` (selective), `import math` (namespace)

## Operator Precedence (highest to lowest)

1. **Postfix**: `()` `.` `?.` `++` `--`
2. **Unary**: `+` `-` `!` `not` `~` `++` `--` `typeof` (prefix)
3. **Power**: `**` (right-associative)
4. **Multiplicative**: `*` `/` `//` `%` `mod`
5. **Additive**: `+` `-`
6. **Shift**: `<<` `>>` `>>>`
7. **Range**: `..` `..<` (with optional `step`)
8. **Bitwise AND**: `&`
9. **Bitwise XOR**: `^`
10. **Bitwise OR**: `|`
11. **Relational**: `<` `<=` `>` `>=` `in` `instanceof`
12. **Equality**: `==` `!=`
13. **Logical AND**: `&&` `and`
14. **Logical OR**: `||` `or`
15. **Null Coalescing**: `??`
16. **Ternary Conditional**: `? :`
17. **Assignment**: `=` `+=` `-=` `*=` `/=` `//=` `%=` `**=` `<<=` `>>=` `>>>=` `&=` `^=` `|=` `&&=` `||=` `??=`

## Related Documents

- **Built-in Functions**: See `slate-builtins.md` for all available built-in functions including math, I/O, type inspection, and collection factory functions.
- **Type System**: See `slate-specification.md` for detailed type system documentation including value types, object model, memory management, and type coercion rules.
- **Usage Examples**: See `slate-examples.md` for comprehensive examples of all Slate language constructs and usage patterns.

---

This grammar specification defines Slate's expression-oriented syntax where control flow constructs are expressions that return values, while declarations and imports are statements.