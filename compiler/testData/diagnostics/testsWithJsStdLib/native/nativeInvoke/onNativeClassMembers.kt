// !DIAGNOSTICS: -UNUSED_PARAMETER

native
class A {
    nativeInvoke
    fun foo() {}

    nativeInvoke
    fun invoke(a: String): Int = 0

    <!NATIVE_ANNOTATIONS_ALLOWED_ONLY_ON_MEMBER_OR_EXTENSION_FUN!>nativeInvoke
    val foo<!> = 0

    nativeInvoke
    <!NATIVE_ANNOTATIONS_ALLOWED_ONLY_ON_MEMBER_OR_EXTENSION_FUN!>object Obj<!> {}

    class object {
        nativeInvoke
        fun foo() {}

        nativeInvoke
        fun invoke(a: String): Int = 0
    }
}