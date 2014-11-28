package test

annotation class JustString(val string: String)

annotation class StringArray(val stringArray: Array<String>)

JustString("kotlin")
class C1

StringArray(array("java", ""))
class C2
