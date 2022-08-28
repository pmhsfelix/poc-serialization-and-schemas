import java.util.function.Supplier

/**
 * Base interface for all schema types, i.e., types that represent a schema.
 */
interface Schema<T> {
    // TODO add function to serialize the concrete schema into a JSON representation
    // Each concrete schema implementation will know how to do this
}

/*
 * Schema objects for primitive types.
 */
object StringSchema : Schema<String>
object IntSchema : Schema<Int>
// TODO add more of these

/**
 * Schema for an object type.
 */
data class ObjectSchema<T>(
    val fields: List<FieldSchema>
) : Schema<T>

/**
 * Schema for an object field.
 */
data class FieldSchema(
    val name: String,
    val type: Schema<*>,
    val isOptional: Boolean = false,
)

/**
 * Schema for a list type
 */
data class ListSchema<T>(
    val valueSchema: Schema<T>,
) : Schema<List<T>> {
    constructor(supplier: Supplier<Schema<T>>) : this(supplier.get())
}

/**
 * Interface implemented by all unions types, allowing retrieving the concrete value
 */
interface Union {
    val value: Any?
}

/**
 * A data type to hold union of two different types.
 * When serializing, these unions type get erased and only the contained value
 * appears in the serialization.
 */
class Union2<T1, T2> (
    val v1: T1?,
    val v2: T2?,
): Union {
    override val value: Any?
        get() = v1 ?: v2
}

/**
 * The schema for an Union2
 */
data class Union2Schema<T1, T2>(
    val s1: Schema<T1>,
    val s2: Schema<T2>
): Schema<Union2<T1,T2>>

/**
 * Base interface for types that are serialized as an object
 * and have an associated schema.
 */
interface SerializableAsMap {
    fun asMap(): Map<String, *>
    fun schema(): Schema<*>
}

/**
 * An object specification (for the lack of a better name), simultaneously defines:
 * - The object schema
 * - A function projecting a T instnace into a map
 * See the tests for examples of how this is used
 */
class ObjectSpec<T>(
    private val fields: List<FieldSpec<T, *>>,
) {
    val schema = ObjectSchema<T>(
        fields.map { FieldSchema(it.fieldSchema.name, it.fieldSchema.type, it.fieldSchema.isOptional) }
    )

    fun makeMap(instance: T): Map<String, Any?> = fields.associate {
        it.fieldSchema.name to serialize(it.projector(instance))
    }

    companion object {
        private fun serialize(value: Any?): Any? =
            when(value) {
                is SerializableAsMap -> value.asMap()
                // The union type gets erased
                is Union -> serialize(value.value)
                is List<*> -> value.map {serialize(it)}
                else -> value
            }
    }
}

class FieldSpec<T, F>(
    val fieldSchema: FieldSchema,
    val projector: T.() -> F,
)

/**
 * A builder for ObjectSpec instances
 */
fun <T> objectSpec(block: ObjectSpecScope<T>.() -> Unit): ObjectSpec<T> {
    val scope = ObjectSpecScope<T>()
    scope.block()
    return ObjectSpec(
        scope.fields
    )
}

/**
 * The builder scope available on the builder block
 */
class ObjectSpecScope<T> {

    val fields = mutableListOf<FieldSpec<T, *>>()

    fun <F> field(name: String, schema: Schema<F>, projector: T.() -> F) {
        fields.add(
            FieldSpec(FieldSchema(name, schema), projector)
        )
    }

    fun <F> field(name: String, schemaSupplier: Supplier<Schema<F>>, projector: T.() -> F) {
        field(name, schemaSupplier.get(), projector)
    }
}
