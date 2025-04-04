package io.silv.ui

import android.os.Binder
import android.os.Bundle
import android.os.Parcelable
import android.util.Size
import android.util.SizeF
import android.util.SparseArray
import androidx.annotation.MainThread
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.Serializable

private val ACCEPTABLE_CLASSES = arrayOf( // baseBundle
    Boolean::class.javaPrimitiveType,
    BooleanArray::class.java,
    Double::class.javaPrimitiveType,
    DoubleArray::class.java,
    Int::class.javaPrimitiveType,
    IntArray::class.java,
    Long::class.javaPrimitiveType,
    LongArray::class.java,
    String::class.java,
    Array<String>::class.java, // bundle
    Binder::class.java,
    Bundle::class.java,
    Byte::class.javaPrimitiveType,
    ByteArray::class.java,
    Char::class.javaPrimitiveType,
    CharArray::class.java,
    CharSequence::class.java,
    Array<CharSequence>::class.java,
    // type erasure ¯\_(ツ)_/¯, we won't eagerly check elements contents
    ArrayList::class.java,
    Float::class.javaPrimitiveType,
    FloatArray::class.java,
    Parcelable::class.java,
    Array<Parcelable>::class.java,
    Serializable::class.java,
    Short::class.javaPrimitiveType,
    ShortArray::class.java,
    SparseArray::class.java,
    Size::class.java,
    SizeF::class.java
)


class ScreenStateHandle: Serializable {

    fun validateValue(value: Any?): Boolean {
        if (value == null) {
            return true
        }
        for (cl in ACCEPTABLE_CLASSES) {
            if (cl!!.isInstance(value)) {
                return true
            }
        }
        return false
    }

    private val regular = mutableMapOf<String, Any?>()

    @Transient
    private val flows = mutableMapOf<String, MutableStateFlow<Any?>>()

    constructor(initialState: Map<String, Any?>) {
        regular.putAll(initialState)
    }

    /**
     * Associate the given value with the key. The value must have a type that could be stored in
     * [android.os.Bundle]
     *
     * This also sets values for any actives or [kotlinx.coroutines.flow.Flow]s.
     *
     * @param key a key used to associate with the given value.
     * @param value object of any type that can be accepted by Bundle.
     *
     * @throws IllegalArgumentException value cannot be saved in saved state
     */
    @MainThread
    operator fun <T> set(key: String, value: T) {
        if (!validateValue(value)) {
            throw IllegalArgumentException(
                "Can't put value with type ${value!!::class.java} into saved state"
            )
        }
        regular[key] = value
        flows[key]?.value = value
    }

    /**
     * Removes a value associated with the given key. If there is a [StateFlow]
     * associated with the given key, they will be removed as well.
     *
     * All changes to [androidx.lifecycle.LiveData]s or [StateFlow]s previously
     * returned by [getStateFlow] won't be reflected in
     * the saved state. Also that `LiveData` or `StateFlow` won't receive any updates about new
     * values associated by the given key.
     *
     * @param key a key
     * @return a value that was previously associated with the given key.
     */
    @MainThread
    fun <T> remove(key: String): T? {
        @Suppress("UNCHECKED_CAST")
        val latestValue = regular.remove(key) as T?
        flows.remove(key)
        return latestValue
    }

    /**
     * Returns a [StateFlow] that will emit the currently active value associated with the given
     * key.
     *
     * ```
     * val flow = savedStateHandle.getStateFlow(KEY, "defaultValue")
     * ```
     * Since this is a [StateFlow] there will always be a value available which, is why an initial
     * value must be provided. The value of this flow is changed by making a call to [set], passing
     * in the key that references this flow.
     *
     * If there is already a value associated with the given key, the initial value will be ignored.
     *
     * Note: If [T] is an [Array] of [Parcelable] classes, note that you should always use
     * `Array<Parcelable>` and create a typed array from the result as going through process
     * death and recreation (or using the `Don't keep activities` developer option) will result
     * in the type information being lost, thus resulting in a `ClassCastException` if you
     * directly try to collect the result as an `Array<CustomParcelable>`.
     *
     * ```
     * val typedArrayFlow = savedStateHandle.getStateFlow<Array<Parcelable>>(
     *   "KEY"
     * ).map { array ->
     *   // Convert the Array<Parcelable> to an Array<CustomParcelable>
     *   array.map { it as CustomParcelable }.toTypedArray()
     * }
     * ```
     *
     * @param key The identifier for the flow
     * @param initialValue If no value exists with the given `key`, a new one is created
     * with the given `initialValue`.
     */
    @MainThread
    fun <T> getStateFlow(key: String, initialValue: T): StateFlow<T> {
        @Suppress("UNCHECKED_CAST")
        // If a flow exists we should just return it, and since it is a StateFlow and a value must
        // always be set, we know a value must already be available
        return flows.getOrPut(key) {
            // If there is not a value associated with the key, add the initial value, otherwise,
            // use the one we already have.
            if (!regular.containsKey(key)) {
                regular[key] = initialValue
            }
            MutableStateFlow(regular[key]).apply { flows[key] = this }
        }.asStateFlow() as StateFlow<T>
    }


    /**
     * Returns all keys contained in this [ScreenStateHandle]
     *
     * Returned set contains all keys: keys used to get LiveData-s, to set SavedStateProviders and
     * keys used in regular [set].
     */
    @MainThread
    fun keys(): Set<String> = regular.keys



    /**
     * Returns a value associated with the given key.
     *
     * Note: If [T] is an [Array] of [Parcelable] classes, note that you should always use
     * `Array<Parcelable>` and create a typed array from the result as going through process
     * death and recreation (or using the `Don't keep activities` developer option) will result
     * in the type information being lost, thus resulting in a `ClassCastException` if you
     * directly try to assign the result to an `Array<CustomParcelable>` value.
     *
     * ```
     * val typedArray = savedStateHandle.get<Array<Parcelable>>("KEY").map {
     *   it as CustomParcelable
     * }.toTypedArray()
     * ```
     *
     * @param key a key used to retrieve a value.
     */
    @MainThread
    operator fun <T> get(key: String): T? {
        return try {
            @Suppress("UNCHECKED_CAST")
            regular[key] as T?
        } catch (e: ClassCastException) {
            // Instead of failing on ClassCastException, we remove the value from the
            // SavedStateHandle and return null.
            remove<T>(key)
            null
        }
    }
}