package test

import kotlin.platform.platformStatic

class PlatformStaticClass {
    class object {
        platformStatic
        fun inClassObject<T>() {}
    }

    platformStatic
    fun inClass<T>() {}
}

