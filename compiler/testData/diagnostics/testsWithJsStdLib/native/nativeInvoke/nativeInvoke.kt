// !DIAGNOSTICS: -UNUSED_PARAMETER

native
class A {
    nativeInvoke
    fun foo() {}

    nativeInvoke
    fun invoke(a: String): Int = 0

    class B {
        class C {
            nativeInvoke
            fun foo() {}

            nativeInvoke
            fun invoke(a: String): Int = 0
        }

        object obj {
            nativeInvoke
            fun foo() {}

            nativeInvoke
            fun invoke(a: String): Int = 0
        }

        class object {
            nativeInvoke
            fun foo() {}

            nativeInvoke
            fun invoke(a: String): Int = 0
        }

        val anonymous = object {
            nativeInvoke
            fun foo() {}

            nativeInvoke
            fun invoke(a: String): Int = 0
        }
    }

    object obj {
        nativeInvoke
        fun foo() {}

        nativeInvoke
        fun invoke(a: String): Int = 0
    }

    class object {
        nativeInvoke
        fun foo() {}

        nativeInvoke
        fun invoke(a: String): Int = 0
    }
}

nativeInvoke
fun Int.ext() = 1

nativeInvoke
fun Int.invoke(a: String, b: Int) = "OK"

fun foo() {
    <!NATIVE_ANNOTATIONS_ALLOWED_ONLY_ON_MEMBER_OR_EXTENSION_FUN!>[nativeInvoke]
    fun Int.ext()<!> = 1

    <!NATIVE_ANNOTATIONS_ALLOWED_ONLY_ON_MEMBER_OR_EXTENSION_FUN!>[nativeInvoke]
    fun Int.invoke(a: String, b: Int)<!> = "OK"

    [native]
    class A {
        nativeInvoke
        fun foo() {}

        nativeInvoke
        fun invoke(a: String): Int = 0


        val anonymous = object {
            nativeInvoke
            fun foo() {}

            nativeInvoke
            fun invoke(a: String): Int = 0
        }
    }
}

native
class B {
    <!NATIVE_ANNOTATIONS_ALLOWED_ONLY_ON_MEMBER_OR_EXTENSION_FUN!>nativeInvoke
    val foo<!> = 0

    nativeInvoke
    <!NATIVE_ANNOTATIONS_ALLOWED_ONLY_ON_MEMBER_OR_EXTENSION_FUN!>object Obj<!> {}
}

class C {
    <!NATIVE_ANNOTATIONS_ALLOWED_ONLY_ON_MEMBER_OR_EXTENSION_FUN!>nativeInvoke
    fun foo()<!> {}

    <!NATIVE_ANNOTATIONS_ALLOWED_ONLY_ON_MEMBER_OR_EXTENSION_FUN!>nativeInvoke
    fun bar(a: String)<!> = 0
}

<!NATIVE_ANNOTATIONS_ALLOWED_ONLY_ON_MEMBER_OR_EXTENSION_FUN!>nativeInvoke
fun toplevelFun()<!> {}

<!NATIVE_ANNOTATIONS_ALLOWED_ONLY_ON_MEMBER_OR_EXTENSION_FUN!>nativeInvoke
val toplevelVal<!> = 0

nativeInvoke
class <!NATIVE_ANNOTATIONS_ALLOWED_ONLY_ON_MEMBER_OR_EXTENSION_FUN!>Foo<!> {}