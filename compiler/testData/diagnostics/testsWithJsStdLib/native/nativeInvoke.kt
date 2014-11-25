// !DIAGNOSTICS: -UNUSED_PARAMETER

native
class A {
    nativeInvoke
    fun foo() {}

    nativeInvoke
    fun invoke(a: String): Int = 0
}

nativeInvoke
fun Int.ext() = 1

nativeInvoke
fun Int.invoke(a: String, b: Int) = "OK"

native
class B {
    <!NATIVE_X_ALLOWED_ONLY_ON_MEMBER_OR_EXTENSION_FUN!>nativeInvoke
    val foo<!> = 0

    nativeInvoke
    <!NATIVE_X_ALLOWED_ONLY_ON_MEMBER_OR_EXTENSION_FUN!>object Obj<!> {}
}

class C {
    <!NATIVE_X_ALLOWED_ONLY_ON_MEMBER_OR_EXTENSION_FUN!>nativeInvoke
    fun foo()<!> {}

    <!NATIVE_X_ALLOWED_ONLY_ON_MEMBER_OR_EXTENSION_FUN!>nativeInvoke
    fun bar(a: String)<!> = 0
}

<!NATIVE_X_ALLOWED_ONLY_ON_MEMBER_OR_EXTENSION_FUN!>nativeInvoke
fun toplevelFun()<!> {}

<!NATIVE_X_ALLOWED_ONLY_ON_MEMBER_OR_EXTENSION_FUN!>nativeInvoke
val toplevelVal<!> = 0

nativeInvoke
class <!NATIVE_X_ALLOWED_ONLY_ON_MEMBER_OR_EXTENSION_FUN!>Foo<!> {}
