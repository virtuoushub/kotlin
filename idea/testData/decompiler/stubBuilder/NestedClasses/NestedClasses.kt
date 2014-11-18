package test

class NestedClasses<TOuter> {
    fun f() {
    }

    private class Nested<TN> {
        fun f(p1: TN) {
        }

        public class NN<TNN> {
            fun f(p1: TNN) {
            }
        }

        inner class NI<TNI : TN> {
            fun f(p1: TN, p2: TNI) {
            }
        }
    }

    public inner class Inner<TI : TOuter> {
        fun f() {
        }

        private inner class II {
            fun f() {
            }
        }
    }
}
