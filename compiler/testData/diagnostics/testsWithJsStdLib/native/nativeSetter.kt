// !DIAGNOSTICS: -UNUSED_PARAMETER

native
class A {
    nativeSetter
    fun set(a: String, v: Any?): Any? = null

    nativeSetter
    fun put(a: Number, v: String) {}

    nativeSetter
    fun foo(a: Int, v: String) {}
}

nativeSetter
fun Int.set(a: String, v: Int) {}

nativeSetter
fun Int.set2(a: Number, v: String?) = "OK"

nativeSetter
fun Int.set3(a: Double, v: String?) = "OK"

native
class B {
    <!NATIVE_X_ALLOWED_ONLY_ON_MEMBER_OR_EXTENSION_FUN!>nativeSetter
    val foo<!> = 0

    nativeSetter
    <!NATIVE_X_ALLOWED_ONLY_ON_MEMBER_OR_EXTENSION_FUN!>object Obj<!> {}
}

class C {
    <!NATIVE_X_ALLOWED_ONLY_ON_MEMBER_OR_EXTENSION_FUN, NATIVE_INDEXER_WRONG_PARAMETER_COUNT!>nativeSetter
    fun set(): Any?<!> = null

    <!NATIVE_X_ALLOWED_ONLY_ON_MEMBER_OR_EXTENSION_FUN, NATIVE_INDEXER_WRONG_PARAMETER_COUNT!>nativeSetter
    fun set(<!NATIVE_INDEXER_KEY_SHOULD_BE_STRING_OR_NUMBER!>a: A<!>): Any?<!> = null

    <!NATIVE_X_ALLOWED_ONLY_ON_MEMBER_OR_EXTENSION_FUN, NATIVE_INDEXER_WRONG_PARAMETER_COUNT!>nativeSetter
    fun set(a: String, v: Any, v2: Any)<!> {}
}

<!NATIVE_INDEXER_WRONG_PARAMETER_COUNT!>nativeSetter
fun Int.set(<!NATIVE_INDEXER_KEY_SHOULD_BE_STRING_OR_NUMBER!>a: A<!>): Int?<!> = 1

<!NATIVE_INDEXER_WRONG_PARAMETER_COUNT!>nativeSetter
fun Int.set2(): String?<!> = "OK"

<!NATIVE_INDEXER_WRONG_PARAMETER_COUNT!>nativeSetter
fun Int.set3(<!NATIVE_INDEXER_KEY_SHOULD_BE_STRING_OR_NUMBER!>a: A<!>, b: Int, c: Any?)<!> {}


<!NATIVE_X_ALLOWED_ONLY_ON_MEMBER_OR_EXTENSION_FUN, NATIVE_INDEXER_WRONG_PARAMETER_COUNT!>nativeSetter
fun toplevelFun(): Any<!> = 0

<!NATIVE_X_ALLOWED_ONLY_ON_MEMBER_OR_EXTENSION_FUN!>nativeSetter
val toplevelVal<!> = 0

nativeSetter
class <!NATIVE_X_ALLOWED_ONLY_ON_MEMBER_OR_EXTENSION_FUN!>Foo<!> {}