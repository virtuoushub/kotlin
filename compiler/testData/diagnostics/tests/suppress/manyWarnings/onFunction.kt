class C {
    suppress("REDUNDANT_NULLABLE", "UNNECESSARY_NOT_NULL_ASSERTION")
    fun foo(): String?? = ""!! <!USELESS_CAST_STATIC_ASSERT_IS_FINE!>as<!> String??
}