package foo

native trait HasName {
    val name: String
}

fun box(): String {
    assertEquals(10, jsExpression("10"), "Int")
    assertEquals(10.5, jsExpression("10.5"), "Float")
    assertEquals("10", jsExpression("'10'"), "String")
    assertEquals(true, jsExpression("true"), "True")
    assertEquals(false, jsExpression("false"), "False")

    val obj: HasName = jsExpression("{name: 'OBJ'}")
    assertEquals("OBJ", obj.name, "Object")

    assertArrayEquals(array(1, 2, 3), jsCode<Array<Int>>("[1, 2, 3]"))

    assertEquals(null, jsCode<Any>("null"), "Null")
    assertEquals(Unit, jsCode<Any>("undefined"), "Undefined")

    // just checking it parses
    val nan: Float = jsCode("NaN")
    val infinity: Float = jsCode("Infinity")

    return "OK"
}