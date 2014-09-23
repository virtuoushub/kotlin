fun <T> foo(array: Array<Array<T>>): Array<Array<T>> = array

fun box(): String {
    val a1: Array<Array<out Int>> = Array(3, { Array(1, { it }) })
    val b1: Array<Array<out Int>> = foo(a1)
    val c1 = foo(a1)

    val a2: Array<Array<in Int>> = Array(3, { Array(1, { it }) })
    val b2: Array<Array<in Int>> = foo(a2)
    val c2 = foo(a2)

    return if (b1[0][0] == c1[0][0] && b2[0][0] == c2[0][0]) "OK" else "fail"
}
