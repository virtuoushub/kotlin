fun <T> foo(array: Array<T>): Array<T> = array

fun box(): String {
    val a1: Array<out Int> = Array(3, { it })
    val b1: Array<out Int> = foo(a1)
    val c1 = foo(a1)

    val a2: Array<in Int> = Array(3, { it })
    val b2: Array<in Int> = foo(a2)
    val c2 = foo(a2)

    return if (b1[0] == c1[0] && b2[0] == c2[0]) "OK" else "fail"
}
