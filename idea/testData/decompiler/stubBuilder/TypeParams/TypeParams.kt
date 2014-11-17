package test

class TypeParams<in T1 : Any, out T2, T3 : (Int) -> Int, T4, T5 : Any?, T6 : T5, T7 : Any> where T1 : Any?, T1 : Int?, T1 : Int, T2 : String, T7 : T6 {

    fun useParams(p1: T1, p2: T2, p3: T3, p4: T4, P5: T5) {
    }

    fun useParamsInOtherOrder(p1: T3, p2: T3, p3: T1, p4: T5, P5: T1) {
    }

    fun useParamsInTypeArg(p1: List<T1>, p2: Map<T2?, T3?>, p3: (T4).(T1, T2, T3) -> T5) {

    }

    fun <G1, G2, G3> withOwnParams(p1: G1, p2: G2, p3: G3, p4: T1, p5: T2) {
    }

    fun <G1 : Any?, G2 : G1, G3> withOwnParamsAndTypeConstraints(p1: G1, p2: G2, p3: G3, p4: T1, p5: T2) where G3 : G1, G3 : String, G3 : String? {
    }
}