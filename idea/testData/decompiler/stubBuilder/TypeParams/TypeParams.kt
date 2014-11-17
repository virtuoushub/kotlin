package test

class TypeParams<T1: Any, T2, T3: (Int) -> Int, T4, T5: Any?> where T1: Any?, T1: Int?, T1: Int, T2: String {

    fun useParams(p1: T1, p2: T2, p3: T3, p4: T4, P5: T5) {
    }

}