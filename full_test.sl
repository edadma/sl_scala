package com.example.app

import math._
import utils.{max, min => minimum}
import io.File
import collections

private data BinaryTree
    case Leaf()
    case Node(left, right)
    
    def size() =
        match this
            case Leaf -> 0
            case Node(l, r) -> 1 + l.size() + r.size()
end BinaryTree

data Shape
    case Circle(radius)
    case Rectangle(width, height)
    case Square()
    case Point

var x = 10

if x > 5 then "big" else "small"

if x > 5 then 
    "big"
else
    "small"

if x > 5
    "big"
else
    "small"

if x > 5
    "big"
elif x < 3
    "small"
else
    "medium"
end if

val result = match x
    case 0 -> "zero"
    case 1 -> "one"
    default "other"

while x > 0
    print(x)
    x = x - 1

for var i = 0; i < 5; i += 1
    print(i)

while x > 0 do
    print(x)
    x = x - 1
end while

for var i = 0; i < 5; i += 1 do print(i)

for var i = 0; i < 5; i += 1 do
    print(i)
end for

do print("once") while false

do
    print("at least once")
    x += 1
while x < 15

loop
    if should_exit() then break
    process()

loop
    if should_exit() then break
    process()
end loop

\ Test typeof operator
var y = 42
print(typeof y)
print(typeof "hello")
print(typeof [1, 2, 3])
print(typeof {key: "value"})

\ Test with expressions
print(typeof (x + y))
print(typeof x > 5)
