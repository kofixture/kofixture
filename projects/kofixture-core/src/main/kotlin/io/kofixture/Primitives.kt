package io.kofixture

import java.time.ZoneId
import java.util.Locale
import java.util.UUID
import kotlin.random.Random
import kotlin.reflect.KType
import kotlin.reflect.typeOf
import kotlin.time.Instant

private val defaultLocalePool = Locale.getAvailableLocales().filter { it.language.isNotBlank() }
private val defaultZoneIdPool = ZoneId.getAvailableZoneIds().sorted()
private val stringAlphabet = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
private val minInstantMillis = Instant.parse("1990-01-01T00:00:00Z").toEpochMilliseconds()
private val maxInstantMillis = Instant.parse("2100-01-01T00:00:00Z").toEpochMilliseconds()

private fun randomString(
    minLength: Int = 1,
    maxLength: Int = 24,
): String {
    val length = Random.nextInt(minLength, maxLength + 1)
    return buildString(length) {
        repeat(length) {
            append(stringAlphabet.random())
        }
    }
}

internal val defaultGenerators: Map<KType, Generator<*>> =
    buildMap {
        put(typeOf<Int>(), Generator { Random.nextInt() })
        put(typeOf<Long>(), Generator { Random.nextLong() })
        put(typeOf<Double>(), Generator { Random.nextDouble(-1_000_000.0, 1_000_000.0) })
        put(typeOf<Float>(), Generator { Random.nextDouble(-1_000_000.0, 1_000_000.0).toFloat() })
        put(typeOf<Boolean>(), Generator { Random.nextBoolean() })
        put(typeOf<String>(), Generator { randomString() })
        put(typeOf<UUID>(), Generator { UUID.randomUUID() })
        put(typeOf<Short>(), Generator { Random.nextInt(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt() + 1).toShort() })
        put(typeOf<Byte>(), Generator { Random.nextInt(Byte.MIN_VALUE.toInt(), Byte.MAX_VALUE.toInt() + 1).toByte() })
        put(typeOf<Char>(), Generator { stringAlphabet.random() })
        put(typeOf<ZoneId>(), Generator { ZoneId.of(defaultZoneIdPool.random()) })
        put(typeOf<Locale>(), Generator { defaultLocalePool.random() })
        put(typeOf<Instant>(), Generator { Instant.fromEpochMilliseconds(Random.nextLong(minInstantMillis, maxInstantMillis + 1)) })
    }
