package io.silv.common.coroutine

import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.coroutineContext

/**
 *  Priority queue backed by a mutex.
 *  All methods are safe to concurrently.
 *  [original source](https://github.com/badoo/Reaktive/blob/665a29646ecfd0e41b0337366b217a6391de0196/reaktive/src/commonMain/kotlin/com/badoo/reaktive/utils/queue/PriorityQueue.kt#L4)
 */
class ConcurrentPriorityQueue<T>(
    private val comparator: Comparator<in T>
) : Iterable<T> {

    private val mutex = Mutex()

    private var array: Array<T?>? = null
    private var _size: Int = 0
    val isEmpty: Boolean get() = _size == 0

    suspend fun peek(): T? =
        mutex.withLock { array?.takeUnless { isEmpty }?.get(0) }

    /**
     * @param item the item to add to the queue
     */
    suspend fun offer(item: T) {
        mutex.withLock {
            var arr: Array<T?>? = array
            if (arr == null) {
                arr = newArray()
            } else if (_size == arr.size) {
                arr = arr.copyOf(_size * 2)
            }
            array = arr

            val lastIndex = _size++
            arr[lastIndex] = item
            @Suppress("UNCHECKED_CAST")
            (arr as Array<T>).heapifyUp(lastIndex, comparator)
        }
    }

    /**
     * @param item the item to remove from the queue
     * @return true if the item was removed false if the item doesn't exist or couldn't be removed.
     */
    suspend fun remove(item: T): Boolean {
        mutex.withLock {
            val arr = array

            if ((arr == null) || isEmpty) {
                return false
            }

            val itemIndex = arr.indexOf(item).takeIf { it != -1 } ?: return false

            val lastIndex = --_size

            arr[itemIndex] = arr[lastIndex]
            arr[lastIndex] = null

            @Suppress("UNCHECKED_CAST")
            (arr as Array<T>).heapifyDown(itemIndex, _size, comparator)

            return true
        }
    }

    /**
     * @return the item with the highest priority.
     */
    suspend fun poll(): T? {
        mutex.withLock {
            val arr = array
            if ((arr == null) || isEmpty) {
                return null
            }

            val lastIndex = --_size
            val item = arr[0]
            arr[0] = arr[lastIndex]
            arr[lastIndex] = null
            @Suppress("UNCHECKED_CAST")
            (arr as Array<T>).heapifyDown(0, _size, comparator)

            return item
        }
    }

    /**
     * Suspends until poll returns a non null value.
     * This respects cancellation through [ensureActive].
     *
     * @return first item returned from [poll]
     */
    suspend fun await(): T {
        while (true) {
            coroutineContext.ensureActive()

            val element = poll()
            if(element != null) {
                return element
            }

            delay(1)
        }
    }

    /**
     * clear all items from the queue
     */
    suspend fun clear() {
        mutex.withLock {
            array = null
            _size = 0
        }
    }

    /**
     * The doc is derived from Java PriorityQueue.
     *
     * Returns an iterator over the elements in this queue. The
     * iterator does not return the elements in any particular order.
     *
     * @return an iterator over the elements in this queue
     */
    override fun iterator(): Iterator<T> =
        object : Iterator<T> {
            private var index = 0

            override fun hasNext(): Boolean = index < _size

            @Suppress("UNCHECKED_CAST")
            override fun next(): T {
                val arr = array?.takeIf { index < _size } ?: throw NoSuchElementException()

                return arr[index++] as T
            }
        }

    private companion object {
        private const val INITIAL_CAPACITY = 8

        @Suppress("UNCHECKED_CAST")
        private fun <T> newArray(): Array<T?> = arrayOfNulls<Any?>(INITIAL_CAPACITY) as Array<T?>

        private fun <T> Array<T>.heapifyDown(index: Int, actualSize: Int, comparator: Comparator<in T>) {
            val leftChildIndex = index * 2 + 1
            if (leftChildIndex >= actualSize) {
                return
            }

            val rightChildIndex = leftChildIndex + 1

            val childIndex =
                if (rightChildIndex >= actualSize) {
                    leftChildIndex
                } else {
                    val leftChildValue = get(leftChildIndex)
                    val rightChildValue = get(rightChildIndex)
                    if (comparator.compare(leftChildValue, rightChildValue) < 0) leftChildIndex else rightChildIndex
                }

            if (comparator.compare(get(childIndex), get(index)) < 0) {
                swap(index, childIndex)
                heapifyDown(childIndex, actualSize, comparator)
            }
        }

        private fun <T> Array<T>.heapifyUp(index: Int, comparator: Comparator<in T>) {
            val parentIndex = if (index % 2 == 0) index / 2 - 1 else index / 2
            if (parentIndex < 0) {
                return
            }

            if (comparator.compare(get(parentIndex), get(index)) > 0) {
                swap(index, parentIndex)
                heapifyUp(parentIndex, comparator)
            }
        }

        private fun <T> Array<T>.swap(first: Int, second: Int) {
            val temp = get(first)
            set(first, get(second))
            set(second, temp)
        }
    }
}