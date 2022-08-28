import org.junit.jupiter.api.Test
import java.util.function.Supplier
import kotlin.test.assertEquals


class Example1(
    val _foo: String,
    val _bar: Int,
    val _anExample2: Example2,
    val _aListOfExample2: List<Example2>,
    // only one of the below fields should be non-null. The non-null value should be serialized as the "zed" field
    val _zorty: String?,
    val _zort: Int?,
) : SerializableAsMap {

    override fun asMap(): Map<String, *> = spec.makeMap(this)
    override fun schema() = spec.schema

    companion object : Supplier<Schema<Example1>> {

        // This defines both the schema and the map projection
        val spec = objectSpec<Example1> {
            field("foo", StringSchema) { _foo }
            field("bar", IntSchema) { _bar }
            field("anExample2", Example2) { _anExample2 }
            field("aListOfExample2", ListSchema(Example2)) { _aListOfExample2 }
            field("zed", Union2Schema(StringSchema, IntSchema)) {
                if (_zorty != null) {
                    Union2(_zorty, null)
                } else {
                    Union2(null, _zort)
                }
            }
        }

        override fun get() = spec.schema
    }
}

class Example2(
    val a: String,
    val b: String,
) : SerializableAsMap {

    override fun asMap(): Map<String, *> = spec.makeMap(this)
    override fun schema() = spec.schema

    // This defines both the schema and the map projection
    companion object : Supplier<Schema<Example2>> {
        val spec = objectSpec<Example2> {
            field("a", StringSchema) { a }
            field("b", StringSchema) { b }
        }

        override fun get() = spec.schema
    }
}

class ExampleTests {

    @Test
    fun first() {
        val example1 = Example1(
            "foo",
            42,
            Example2("abc", "xyz"),
            listOf(
                Example2("x", "y"),
                Example2("1", "2")
            ),
            "hello",
            null
        )

        val map = example1.asMap()

        // Assert serialization
        assertEquals(
            mapOf(
                "foo" to "foo",
                "bar" to 42,
                "anExample2" to mapOf(
                    "a" to "abc",
                    "b" to "xyz",
                ),
                "aListOfExample2" to listOf(
                    mapOf(
                        "a" to "x",
                        "b" to "y"
                    ),
                    mapOf(
                        "a" to "1",
                        "b" to "2"
                    )
                ),
                "zed" to "hello"
            ), map
        )

        // Assert schema
        assertEquals(
            ObjectSchema<Example1>(
                listOf(
                    FieldSchema("foo", StringSchema),
                    FieldSchema("bar", IntSchema),
                    FieldSchema(
                        "anExample2", ObjectSchema<Example2>(
                            listOf(
                                FieldSchema("a", StringSchema),
                                FieldSchema("b", StringSchema)
                            )
                        )
                    ),
                    FieldSchema("aListOfExample2", ListSchema(Example2)),
                    FieldSchema(
                        "zed", Union2Schema(
                            StringSchema, IntSchema
                        )
                    )
                )
            ),
            example1.schema()
        )
    }
}