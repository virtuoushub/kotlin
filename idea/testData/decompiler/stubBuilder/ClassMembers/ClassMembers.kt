package test

abstract class ClassMembers {
    val foo = 3
    fun bar(): Int {
        return 3
    }

    open fun openFun() {
    }

    abstract fun abstractFun()

    open val openVal = 3

    abstract var abstractVar: Int
}