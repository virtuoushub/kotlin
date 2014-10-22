class Test(foo: Any?, bar: Any?) {
    val foo = foo ?: this
    private val bar = bar ?: this
}