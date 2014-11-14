class Annotations {

    a fun f() {
    }

    b(E.E1) val c: Int = 1

    a b(E.E2) fun g() {
    }
}

annotation class a

annotation class b(val e: E)

enum class E { E1 E2 }