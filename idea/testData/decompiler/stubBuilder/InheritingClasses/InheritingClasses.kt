package a

class InheritingClasses {
    open class A
    open class B : A()

    trait C
    trait D : C

    trait E
    class G : B(), C, D, E
}