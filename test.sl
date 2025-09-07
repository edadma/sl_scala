package a.b.c

\ Test Slate program for lexer
var x = 42
val message = "Hello, World!"

\ Function definition
def greet(name) = 
    print(`Hello, $name!`)
    if name == "Alice" then
        print("Special greeting for Alice")
    else
        print("Regular greeting")

\ Control flow with indentation
if x > 0 then
    print("x is positive")
    while x > 0
        print(x)
        x = x - 1
    print("Done counting")

\ Array and object literals
val arr = [1, 2, 3, 4, 5]
var obj = {name: "Bob", age: 30}

\ Range and operators
for var i = 0; i < 10; i += 1 do
    print(i)

\ Template literals
val msg = `The value is ${x * 2}`

\ Boolean and special values
var flag = true
var empty = null
var notDef = undefined

\ Match expression
match x
    case 0 -> "zero"
    case 1 -> "one"
    default "other"