package foo

native trait HasName {
    val name: String
}

fun box(): String {
    assertEquals(10, jsCode<Int>("10"), "Int")
    assertEquals(10.5, jsCode<Float>("10.5"), "Float")
    assertEquals("10", jsCode<String>("'10'"), "String")
    assertEquals(true, jsCode<Boolean>("true"), "True")
    assertEquals(false, jsCode<Boolean>("false"), "False")

    val obj: HasName = jsCode("{name: 'OBJ'}")
    assertEquals("OBJ", obj.name, "Object")

    assertArrayEquals(array(1, 2, 3), jsCode<Array<Int>>("[1, 2, 3]"))

    assertEquals(null, jsCode<Any>("null"), "Null")
    assertEquals(Unit, jsCode<Any>("undefined"), "Undefined")

    // just checking it parses
    val nan: Float = jsCode("NaN")
    val infinity: Float = jsCode("Infinity")

    return "OK"
}