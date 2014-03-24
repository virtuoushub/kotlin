package foo

data class A(val value: Int)

fun box(): String {
    assertEquals(-1, jsCode("-1"), "- (unary)")
    assertEquals(1, jsCode("+'1'"), "+ (unary)")

    assertEquals(1, jsCode("2 - 1"), "- (binary)")
    assertEquals(3, jsCode("2 + 1"), "+ (binary)")
    assertEquals(10, jsCode("2 * 5"), "*")
    assertEquals(5, jsCode("10 / 2"), "/")
    assertEquals(1, jsCode("11 % 2"), "%")

    assertEquals(36, jsCode("9 << 2"), "<<")
    assertEquals(2, jsCode("9 >> 2"), ">>")
    assertEquals(1073741821, jsCode("-9 >>> 2"), ">>>")
    assertEquals(0, jsCode("0 & 1"), "&")
    assertEquals(1, jsCode("0 | 1"), "|")
    assertEquals(1, jsCode("0 ^ 1"), "^")
    assertEquals(-2, jsCode("~1"), "~")

    var i = 2
    assertEquals(1, jsCode("--i"), "-- prefix")
    assertEquals(1, jsCode("i--"), "-- postfix (1)")
    assertEquals(0, jsCode("i"), "-- postfix (0)")
    assertEquals(1, jsCode("++i"), "++ prefix")
    assertEquals(1, jsCode("i++"), "++ postfix (1)")
    assertEquals(2, jsCode("i"), "++ postfix (0)")

    assertEquals(true , jsCode("true || false"), "||")
    assertEquals(false , jsCode("true && false"), "&&")
    assertEquals(false , jsCode("!true"), "!")

    assertEquals(true , jsCode("1 < 2"), "<")
    assertEquals(false, jsCode("1 > 2"), ">")
    assertEquals(true, jsCode("1 <= 2"), "<=")
    assertEquals(false, jsCode("1 >= 2"), ">=")

    assertEquals(false, jsCode("2 === '2'"), "===")
    assertEquals(true, jsCode("2 !== '2'"), "!==")
    assertEquals(true, jsCode("2 == '2'"), "==")
    assertEquals(false, jsCode("2 != '2'"), "!=")
    
    assertEquals("odd", jsCode("(1 % 2 === 0)?'even':'odd'"), "?:")
    assertEquals("even", jsCode("(4 % 2 === 0)?'even':'odd'"), "?:")
    assertEquals(3, jsCode("1,2,3"), ", (comma)")

    var j = 0
    assertEquals(1, jsCode("j = 1"), "=")
    assertEquals(3, jsCode("j += 2"), "+=")
    assertEquals(2, jsCode("j -= 1"), "-=")
    assertEquals(14, jsCode("j *= 7"), "*=")
    assertEquals(7, jsCode("j /= 2"), "/=")
    assertEquals(1, jsCode("j %= 2"), "%=")

    assertEquals(Unit, jsCode("(void 0)"), "void")
    assertEquals(true, jsCode("'key' in {'key': 10}"), "in")
    assertEquals("string", jsCode("typeof 'str'"), "typeof")
    assertEquals(A(2), jsCode("new _.foo.A(2)"), "new")
    assertEquals(true, jsCode("new String('str') instanceof String"), "instanceof")

    var s: Any = jsCode("({key: 10})")
    assertEquals(Unit, jsCode("delete s.key, s.key"), "delete")

    return "OK"
}