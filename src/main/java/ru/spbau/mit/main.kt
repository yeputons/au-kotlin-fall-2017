package ru.spbau.mit

/**
 * That class defines a semigroup structure on a type T.
 * The following property should hold (associativity):
 * for any three values `a`, `b`, and `c` we have
 * `combine(combine(a, b), c)) == combine(a, combine(b, c))`.
 *
 * Examples: addition of two numbers, matrix multiplication.
 *
 * @param T the underlying data type on which the semigroup is defined
 */
interface SemigroupPolicy<T> {
    fun combine(left: T, right: T): T
}

/**
 * A data structure which allows generic range aggregation queries on a array.
 * Given a fixed list `data` of type `T` (`a_0`, `a_1`, ...) and an associative operation
 * `policy` which can combine two instances of type `T` into a single one, this
 * structure can efficiently calculate combination of all values from an arbitrary
 * subsegment of `data`.
 *
 * E.g. if `T` is `Int` and `policy` specifies addition of numbers, then this data
 * structure will efficiently solve Range Sum Query problem.
 *
 * Performance assuming `n` is equal to `data.length`:
 * * Memory consumption:
 * ** Requires between 2n and 4(n-1) instances of type `T` plus O(n) objects.
 * * Performance:
 * ** O(n) calls to `policy.combine` and O(n) object creations during construction
 * ** O(log n) calls to `policy.combine` per request plus O(log n) stack frames
 *
 * @param T type of the data in the list and the return value of aggregation operation
 * @param data the list on which range queries will be performed, should be non-empty
 * @param policy an object which defines how to combine the values in a range
 * @constructor Constructs and initializes a range query solver, calculates all necessary data
 */
class RangeQuerySolver<out T>(data: List<T>, private val policy: SemigroupPolicy<T>) {
    init {
        if (data.isEmpty()) {
            throw IllegalArgumentException("data should be non-empty")
        }
    }

    private val root = createNode(data, policy)
    val size = data.size

    /**
     * Given `left` and `right`, folds all elements of `data` with numbers
     * `left`, `left+1`, ..., `right-1`, `right` (in order) using `policy` and
     * return the result.
     *
     * The segment requested should be non-empty and fully lie inside initial `data`
     * for which the data structure is constructed.
     *
     * @param left left border of the segment requested (incl.)
     * @param right right border of the segment requested (incl.)
     * @return result of combining all elements from `left` to `right` (incl.) with `policy`, in order
     */
    fun getRangeValue(left: Int, right: Int): T {
        if (!(left in 0..right && right < size)) {
            throw IndexOutOfBoundsException("Illegal range query: [$left; {$right}] when size is $size")
        }
        return root.getRangeValue(left, right, policy)
    }

    /**
     * Represents a single node of the segment tree which is responsible for answering
     * range requests for some non-empty subsegment of the initial `data`.
     * The subsegment is always indexed from zero up to node's size minus one,
     * even if that does not correspond to initial positions in `data`.
     *
     * It does not store `policy` to lower memory footprint,
     * `policy` should be passed to all methods manually.
     */
    private interface Node<T> {
        val size: Int

        /**
         * Given a non-empty subsegment of the node, returns result of combining
         * all values in that subsegment.
         *
         * @param left left border of the subsegment (incl.)
         * @param left right border of the subsegment (incl.)
         * @param policy the combining policy used when creating the node
         */
        fun getRangeValue(left: Int, right: Int, policy: SemigroupPolicy<T>): T
    }

    companion object NodesFactory {
        private fun <T> createNode(data: List<T>, policy: SemigroupPolicy<T>): Node<T> {
            assert(data.isNotEmpty())
            return if (data.size == 1) Leaf(data[0]) else InnerNode(data, policy)
        }
    }

    /**
     * Represents a leaf node which contains only a single value.
     */
    private class Leaf<T>(val v: T) : Node<T> {
        override val size = 1

        override fun getRangeValue(left: Int, right: Int, policy: SemigroupPolicy<T>): T {
            assert(left == 0 && right == 0)
            return v
        }
    }

    /**
     * Represents an inner node which contains more than one value.
     * @param data The data list for which the node is responsible
     * @param policy The combining policy for the data, will be used for all child nodes
     * @constructor Create all necessary child nodes for answering range queries
     */
    private class InnerNode<T>(data: List<T>, policy: SemigroupPolicy<T>) : Node<T> {
        init {
            assert(data.size >= 2)
        }

        override val size = data.size
        private val leftNode = createNode(data.subList(0, size / 2), policy)
        private val rightNode = createNode(data.subList(size / 2, size), policy)
        private val total = policy.combine(
                leftNode.getRangeValue(0, leftNode.size - 1, policy),
                rightNode.getRangeValue(0, rightNode.size - 1, policy)
        )

        override fun getRangeValue(left: Int, right: Int, policy: SemigroupPolicy<T>): T {
            assert(left in 0..right && right < size)
            if (left == 0 && right == size - 1) {
                return total
            }
            // Three cases: either the requested segment fully lies inside one of children, or
            // it intersects the border between them.
            val rightNodeShift = leftNode.size
            return when {
                right < leftNode.size -> leftNode.getRangeValue(left, right, policy)
                leftNode.size <= left -> rightNode.getRangeValue(left - rightNodeShift, right - rightNodeShift, policy)
                else -> policy.combine(
                        leftNode.getRangeValue(left, leftNode.size - 1, policy),
                        rightNode.getRangeValue(0, right - rightNodeShift, policy)
                )
            }
        }
    }
}

fun main(args: Array<String>) {
}