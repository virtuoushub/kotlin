// !DIAGNOSTICS: -UNUSED_PARAMETER

native
class A {
    nativeGetter
    fun get(a: String): Any? = null

    nativeGetter
    fun take(a: Number): String? = null

    nativeGetter
    fun foo(a: Double): String? = null
}

nativeGetter
fun Int.get(a: String): Int? = 1

nativeGetter
fun Int.get2(a: Number): String? = "OK"

nativeGetter
fun Int.get3(a: Int): String? = "OK"

native
class B {
    <!NATIVE_X_ALLOWED_ONLY_ON_MEMBER_OR_EXTENSION_FUN!>nativeGetter
    val foo<!> = 0

    nativeGetter
    <!NATIVE_X_ALLOWED_ONLY_ON_MEMBER_OR_EXTENSION_FUN!>object Obj<!> {}
}

class C {
    <!NATIVE_X_ALLOWED_ONLY_ON_MEMBER_OR_EXTENSION_FUN, NATIVE_INDEXER_WRONG_PARAMETER_COUNT!>nativeGetter
    fun get(): Any?<!> = null

    <!NATIVE_X_ALLOWED_ONLY_ON_MEMBER_OR_EXTENSION_FUN!>nativeGetter
    fun get(<!NATIVE_INDEXER_KEY_SHOULD_BE_STRING_OR_NUMBER!>a: A<!>): Any?<!> = null

    <!NATIVE_X_ALLOWED_ONLY_ON_MEMBER_OR_EXTENSION_FUN!>nativeGetter
    fun <!NATIVE_GETTER_RETURN_TYPE_SHOULD_BE_NULLABLE!>foo<!>(a: Int)<!> {}

    <!NATIVE_X_ALLOWED_ONLY_ON_MEMBER_OR_EXTENSION_FUN!>nativeGetter
    fun bar(a: String): <!NATIVE_GETTER_RETURN_TYPE_SHOULD_BE_NULLABLE!>Int<!><!> = 0
}

nativeGetter
fun Int.get(<!NATIVE_INDEXER_KEY_SHOULD_BE_STRING_OR_NUMBER!>a: A<!>): Int? = 1

<!NATIVE_INDEXER_WRONG_PARAMETER_COUNT!>nativeGetter
fun Int.get2(): String?<!> = "OK"

<!NATIVE_INDEXER_WRONG_PARAMETER_COUNT!>nativeGetter
fun Int.get3(<!NATIVE_INDEXER_KEY_SHOULD_BE_STRING_OR_NUMBER!>a: A<!>, b: Int, c: Any?): String?<!> = "OK"


<!NATIVE_X_ALLOWED_ONLY_ON_MEMBER_OR_EXTENSION_FUN, NATIVE_INDEXER_WRONG_PARAMETER_COUNT!>nativeGetter
fun toplevelFun(): <!NATIVE_GETTER_RETURN_TYPE_SHOULD_BE_NULLABLE!>Any<!><!> = 0

<!NATIVE_X_ALLOWED_ONLY_ON_MEMBER_OR_EXTENSION_FUN!>nativeGetter
val toplevelVal<!> = 0

nativeGetter
class <!NATIVE_X_ALLOWED_ONLY_ON_MEMBER_OR_EXTENSION_FUN!>Foo<!> {}