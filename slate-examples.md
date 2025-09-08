# Slate Language Examples

This document provides comprehensive examples of Slate language constructs and usage patterns. For the formal grammar specification, see `slate-grammar.md`.

## Basic Syntax

### Comments
```slate
\ This is a single-line comment
\ Comments use backslash, not //
```

### Variable Declarations
```slate
\ Simple variable declarations are statements - they don't return values
var mutable = 5          \ Mutable variable (can be reassigned)
val constant = 10        \ Immutable variable (cannot be reassigned)

\ Type coercion examples
var number = 42          \ int32
var bigNum = 999999999999999999  \ auto-promotes to bigint
var decimal = 3.14       \ float64
var text = "Hello, World!"
var flag = true
var empty = null
var notSet = undefined

\ Array destructuring
var [first, second] = [1, 2]           \ first = 1, second = 2
val [head, ...tail] = [1, 2, 3, 4]     \ head = 1, tail = [2, 3, 4]
var [a, , c] = [1, 2, 3]               \ a = 1, c = 3 (skip middle)

\ Object destructuring
val {name, age} = person                \ Extract name and age properties
var {x: coord, y: yCoord} = point       \ Rename during destructuring
val {name} = user                       \ Extract just name property

\ Nested destructuring
var [first, {name, age}] = [42, {name: "Alice", age: 30}]
val {user: {profile: {email}}} = data   \ Deep destructuring
```

### Function Declarations
```slate
\ Simple functions
def add(a, b) = a + b
def multiply(x, y) = x * y

\ Functions with block bodies
def complex(x) =
    val temp = x * 2     \ Using val for immutable local variables
    temp + 10

\ Function with end marker
def factorial(n) =
    if n <= 1 then
        1
    else
        n * factorial(n - 1)
end factorial
```

### Array Operations
```slate
\ Array creation and access
var arr = [1, 2, 3, 4, 5]
print(arr(0))       \ Prints: 1 (uses parentheses, not brackets)
print(arr(2))       \ Prints: 3

\ Mixed-type arrays
var mixed = [42, "hello", true, null, [1, 2]]
print(mixed(1))     \ Prints: "hello"
print(mixed(4)(0))  \ Prints: 1 (nested access)
```

### Object Operations
```slate
\ Object creation and property access
var person = {
    name: "Alice",
    age: 30,
    city: "New York"
}

\ Static property access
print(person.name)      \ Prints: "Alice"
print(person.age)       \ Prints: 30

\ Dynamic property access
var prop = "city"
print(person(prop))     \ Prints: "New York"

\ Nested objects
var company = {
    name: "Tech Corp",
    location: {
        street: "123 Main St",
        city: "San Francisco",
        state: "CA"
    },
    employees: 50
}

print(company.location.street)  \ Prints: "123 Main St"
```

## Control Flow Expressions

### If Expressions
```slate
\ Simple if expression
var result = if x > 0 then "positive" else "non-positive"

\ If expression with indented blocks
var result2 = if x > 0
    print("positive!")
    "positive"
else
    "non-positive"

\ Multi-branch if with elif
var category = if x > 10
    "large"
elif x > 5
    "medium"
else
    "small"

\ If expression with end marker
var status = if condition
    processData()
    updateState()
    "completed"
end if

\ If with no else returns undefined when condition is false
var maybe = if false then "value"  \ maybe gets undefined
```

### While Loops
```slate
\ Simple while loop
var i = 0
while i < 5
    print(i)
    i += 1

\ While with explicit do
while condition do processItem()

\ While with end marker
var count = 0
while count < 10
    print("Processing item " + count)
    count += 1
end while
```

### Do-While Loops
```slate
\ Simple do-while
do
    print("This executes at least once")
while false

\ Do-while with single expression
do print("once") while false

\ Do-while with block
var x = 0
do
    print("x is " + x)
    x += 1
while x < 3
```

### For Loops
```slate
\ Traditional for loop
for var i = 0; i < 10; i += 1
    print(i)

\ For loop with end marker
for var i = 0; i < 5; i += 1 do
    print("Number: " + i)
end for

\ For loop with single expression
for var i = 0; i < 3; i += 1 do print(i)
```

### Loop Expression
```slate
\ Infinite loop with break
loop
    val input = readInput()
    if input == "quit" then break
    processInput(input)

\ Loop with end marker
loop
    if shouldExit() then break
    process()
end loop
```

## Pattern Matching

### Basic Match Expressions
```slate
\ Simple literal matching
var result = match x
    case 0 -> "zero"
    case 1 -> "one"
    case 2 -> "two"
    default "other number"

\ Variable binding
match value
    case x -> print("The value is: " + x)
    default print("No value")
```

### Advanced Pattern Matching
```slate
\ Type patterns with guards
match value
    case x: Number if x > 0 -> "positive number"
    case x: Number if x < 0 -> "negative number"
    case x: Number -> "zero"
    case s: String if s.length > 0 -> "non-empty string"
    case s: String -> "empty string"
    default "other type"

\ Array destructuring
match data
    case [first, second] -> `First: ${first}, Second: ${second}`
    case [head, ...tail] -> `Head: ${head}, Tail has ${tail.length} items`
    case [] -> "empty array"
    default "not an array"

\ Object destructuring
match person
    case {name, age} -> `${name} is ${age} years old`
    case {name} -> `Name: ${name}, age unknown`
    case {} -> "empty object"
    default "not an object"

\ Constructor patterns (for data types)
match tree
    case Leaf -> 0
    case Node(left, right) -> 1 + left.size() + right.size()
    default "not a tree"

\ Complex nested patterns
match data
    case {user: {name, profile: {age}}} if age > 18 -> 
        `Adult user: ${name}`
    case {user: {name, profile: {age}}} -> 
        `Minor user: ${name}`
    case [Node(Leaf, right), value] if value > 0 -> 
        "complex nested pattern"
    case Point(x: coord, y: coord) if coord > 0 -> 
        "point in positive quadrant"
    default "no match"

\ Match with end marker
val classification = match data
    case x: Number if x > 100 -> "large"
    case x: Number if x > 10 -> "medium"  
    case x: Number -> "small"
    default "not a number"
end match
```

## Block Expressions

### Basic Block Usage
```slate
\ Block as expression (returns last expression value)
val result = 
    var x = 5      \ Statement - doesn't contribute to block value
    var y = 10     \ Statement - doesn't contribute to block value
    x + y          \ Expression - block evaluates to 15

\ Block as function argument
print(
    var temp = 100
    temp / 2       \ Block evaluates to 50
)

\ Block in arithmetic operations
val sum = 10 + 
    var a = 5
    var b = 3
    a * b          \ Block evaluates to 15, so sum = 25
```

### Nested Blocks
```slate
val nested =
    val outer = 10
    
        val inner = 5
        outer + inner     \ Inner block evaluates to 15

val complex =
    var result = 0
    
        var temp = 5
        result = temp * 2
    
    result + 
        var bonus = 3
        bonus * 2         \ Final result is 16
```

## Template Literals

### Basic String Interpolation
```slate
val name = "Alice"
val age = 30
val message = `Hello, ${name}! You are ${age} years old.`

\ Simple variable interpolation
val greeting = `Hello, $name!`

\ Expression interpolation
val calculation = `The result is ${5 * 10 + 2}`
```

### Complex Template Usage
```slate
val user = {name: "Bob", score: 95}
val report = `
    User Report:
    Name: ${user.name}
    Score: ${user.score}
    Grade: ${if user.score >= 90 then "A" else "B"}
    Status: ${user.score > 80 ? "Passing" : "Needs improvement"}
`
```

## Data Types

### Basic Data Declarations
```slate
\ Simple data type with different constructor types
data Shape
    case Point                    \ Singleton (case object equivalent)
    case Circle(radius)           \ Constructor with parameter
    case Rectangle(width, height) \ Constructor with multiple parameters
    case Square()                 \ Empty constructor (for mutable instances)
end Shape

\ Usage
val origin = Shape.Point
val circle = Shape.Circle(5.0)
val rect = Shape.Rectangle(10, 20)
val square = Shape.Square()
```

### Data Types with Methods
```slate
data BinaryTree
    case Leaf
    case Node(left, right)
    
    def size() =
        match this
            case Leaf -> 0
            case Node(l, r) -> 1 + l.size() + r.size()
    
    def contains(value) =
        match this
            case Leaf -> false
            case Node(l, r) -> 
                if this.value == value then
                    true
                else
                    l.contains(value) or r.contains(value)
end BinaryTree

\ Usage
val tree = BinaryTree.Node(
    BinaryTree.Leaf,
    BinaryTree.Node(BinaryTree.Leaf, BinaryTree.Leaf)
)
print(tree.size())  \ Prints: 2
```

### Private Data Types
```slate
private data Counter
    case Instance(value)
    
    def increment() =
        match this
            case Instance(v) -> Counter.Instance(v + 1)
    
    def getValue() =
        match this
            case Instance(v) -> v
end Counter
```

## Anonymous Functions

### Basic Anonymous Functions
```slate
\ Single parameter
val square = x -> x * x
print(square(5))  \ Prints: 25

\ Multiple parameters
val add = (a, b) -> a + b
print(add(3, 4))  \ Prints: 7

\ No parameters
val getMessage = () -> "Hello, World!"
print(getMessage())  \ Prints: "Hello, World!"
```

### Anonymous Functions with Blocks
```slate
val complexCalculation = (x, y) ->
    val temp1 = x * 2
    val temp2 = y * 3
    temp1 + temp2

val processor = data ->
    print("Processing: " + data)
    if data.isValid() then
        data.process()
    else
        data.reject()
```

## Module System

### Package Declarations
```slate
package com.example.myapp

\ ... rest of file content
```

### Import Statements
```slate
\ Wildcard import
import math._
\ Now you can use: sin(x), cos(x), PI, etc.

\ Selective import with aliases
import utils.{max, min => minimum, average}
\ Now you can use: max(a, b), minimum(a, b), average(arr)

\ Simple import
import io.File
\ Now you can use: File.read(path), File.write(path, content)

\ Namespace import
import collections
\ Now you can use: collections.List(), collections.Map(), etc.
```

### Module Structure Example
```slate
\ File: math_utils.sl
package utils.math

def factorial(n) =
    if n <= 1 then 1 else n * factorial(n - 1)

def fibonacci(n) =
    if n <= 1 then n else fibonacci(n - 1) + fibonacci(n - 2)

val PI = 3.14159

\ File: main.sl  
package main

import utils.math.{factorial, PI}
import utils.math

print(factorial(5))        \ Uses imported function
print(PI * 2)             \ Uses imported constant
print(math.fibonacci(10)) \ Uses namespace import
```

## Range Expressions

### Basic Ranges
```slate
\ Inclusive range
val range1 = 1..10        \ [1, 2, 3, 4, 5, 6, 7, 8, 9, 10]

\ Exclusive range  
val range2 = 1..<10       \ [1, 2, 3, 4, 5, 6, 7, 8, 9]

\ Range with step
val evens = 0..10 step 2  \ [0, 2, 4, 6, 8, 10]
val odds = 1..10 step 2   \ [1, 3, 5, 7, 9]

\ Descending range
val countdown = 10..1 step -1  \ [10, 9, 8, 7, 6, 5, 4, 3, 2, 1]
```

### Range Usage
```slate
\ Iterating over ranges (when for-each loops are implemented)
for item in 1..5
    print(item)

\ Range in match expressions
match value
    case x if x in 1..10 -> "single digit"
    case x if x in 10..100 -> "double digit" 
    default "other"
```

## Advanced Features

### Operator Precedence Examples
```slate
\ Demonstrating operator precedence
val result1 = 2 + 3 * 4        \ 14 (multiplication first)
val result2 = (2 + 3) * 4      \ 20 (parentheses override)
val result3 = 2 ** 3 ** 2      \ 512 (power is right-associative: 2^(3^2))
val result4 = x?.property?.method()  \ Optional chaining
val result5 = x ?? y ?? z      \ Null coalescing (left-to-right)
```

### Ternary Conditional
```slate
\ Basic ternary
val result = condition ? "true value" : "false value"

\ Nested ternary
val grade = score >= 90 ? "A" : 
           score >= 80 ? "B" :
           score >= 70 ? "C" : "F"

\ Ternary in expressions
val message = `You ${passed ? "passed" : "failed"} the test`
```

### Complex Assignment Operations
```slate
var x = 10
x += 5      \ x = 15
x *= 2      \ x = 30  
x //= 4     \ x = 7 (floor division)
x **= 2     \ x = 49 (exponentiation)

var arr = [1, 2, 3]
arr(0) += 10  \ arr becomes [11, 2, 3]

var obj = {count: 5}
obj.count *= 2  \ obj.count becomes 10
```

## Error Handling Patterns

### Using Match for Error Handling
```slate
def safeDivide(a, b) =
    if b == 0 then
        {error: "Division by zero"}
    else
        {result: a / b}

val result = safeDivide(10, 2)
match result
    case {result: value} -> print("Result: " + value)
    case {error: message} -> print("Error: " + message)
    default print("Unexpected result format")
```

### Using Undefined Checks
```slate
def processUser(user) =
    if user == undefined then
        return "No user provided"
    
    if user.name == undefined then
        return "User has no name"
        
    `Processing user: ${user.name}`
```

This comprehensive examples document shows practical usage patterns for all major Slate language features while keeping the grammar document focused on pure syntax rules.