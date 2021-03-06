package foo

native
trait NativeTrait {
    val foo: String
    fun bar(a: Int): Any

    native("boo")
    fun baz(): String
}

trait Trait : NativeTrait

class Class : NativeTrait {
    override val foo: String = "Class().foo"
    override fun bar(a: Int): Any = "Class().bar($a)"
    override fun baz(): String = "Class().boo()"
}

class AnotherClass : Trait {
    override val foo: String = "AnotherClass().foo"
    override fun bar(a: Int): Any = "AnotherClass().bar($a)"
    override fun baz(): String = "AnotherClass().boo()"
}

fun test<T : NativeTrait>(c: T, className: String) {
    assertEquals("$className().foo", c.foo)
    assertEquals("$className().bar(3)", c.bar(3))
    assertEquals("$className().boo()", c.baz())

    val t: NativeTrait = c
    assertEquals("$className().foo", t.foo)
    assertEquals("$className().bar(3)", t.bar(3))
    assertEquals("$className().boo()", t.baz())
}

fun box(): String {
    test(Class(), "Class")
    test(AnotherClass(), "AnotherClass")

    return "OK"
}
