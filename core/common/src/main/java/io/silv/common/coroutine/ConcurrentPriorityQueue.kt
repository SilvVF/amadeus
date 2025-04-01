package io.silv.common.coroutine

import io.silv.common.drain
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.isActive
import kotlinx.coroutines.newFixedThreadPoolContext
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

    private var array: Array<T?>? = null
    private var _size: Int = 0
    private val mutex = Mutex()
    private val channel = Channel<Unit>(Channel.UNLIMITED)

    val isEmpty: Boolean
        get() = _size == 0

    suspend fun peek(): T? = mutex.withLock {
        array?.takeUnless { isEmpty }?.get(0)
    }

    suspend fun offer(item: T) = mutex.withLock {
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

        channel.send(Unit)
    }

    suspend fun poll(removeFromChannel: Boolean = true): T? = mutex.withLock {
        val arr = array
        if ((arr == null) || isEmpty) {
            return@withLock null
        }

        val lastIndex = --_size
        val item = arr[0]
        arr[0] = arr[lastIndex]
        arr[lastIndex] = null
        @Suppress("UNCHECKED_CAST")
        (arr as Array<T>).heapifyDown(0, _size, comparator)

        if (removeFromChannel) {
            channel.tryReceive()
        }

        return@withLock item
    }

    suspend fun pollUntilAvailable(): T? {
        val item = poll()
        if (item != null) {
            return item
        } else {
            while(coroutineContext.isActive) {
                channel.receive()

                val item = poll(removeFromChannel = false)
                if (item != null) {
                    return item
                }
            }
            return null
        }
    }

    suspend fun remove(item: T): Boolean = mutex.withLock {
        val arr = array ?: return false
        val index = arr.indexOfFirst { it == item }
        if (index == -1) return false

        val lastIndex = --_size
        if (index != lastIndex) {
            arr[index] = arr[lastIndex]
            arr[lastIndex] = null
            @Suppress("UNCHECKED_CAST")
            (arr as Array<T>).heapifyDown(index, _size, comparator)
        } else {
            arr[lastIndex] = null
        }

        channel.tryReceive()

        return true
    }

    suspend fun clear() = mutex.withLock {
        array = null
        _size = 0
        channel.drain()
    }

    override fun iterator(): Iterator<T> = object : Iterator<T> {
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