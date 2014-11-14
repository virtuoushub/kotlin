package foo.TopLevelMembers

fun funWithBlockBody() {
}

private fun funWithExprBody() = 3

private fun funWithParams(c: Int) {
}

public val immutable: Double = 0.0

public var mutable: Float = 0.0f

public val String.ext: String
    get() = this

public fun Int.ext(): Int = this + 3