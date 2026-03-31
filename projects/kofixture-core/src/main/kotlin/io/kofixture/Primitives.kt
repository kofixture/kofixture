package io.kofixture

import io.kotest.property.Arb
import io.kotest.property.arbitrary.boolean
import io.kotest.property.arbitrary.byte
import io.kotest.property.arbitrary.char
import io.kotest.property.arbitrary.double
import io.kotest.property.arbitrary.float
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.kotlinInstant
import io.kotest.property.arbitrary.locale
import io.kotest.property.arbitrary.long
import io.kotest.property.arbitrary.map
import io.kotest.property.arbitrary.short
import io.kotest.property.arbitrary.string
import io.kotest.property.arbitrary.uuid
import io.kotest.property.arbitrary.zoneId
import java.time.ZoneId
import java.util.Locale
import java.util.UUID
import kotlin.reflect.KType
import kotlin.reflect.typeOf
import kotlin.time.Instant

internal val defaultGenerators: Map<KType, Generator<*>> =
    buildMap {
        put(typeOf<Int>(), Arb.int().toGenerator())
        put(typeOf<Long>(), Arb.long().toGenerator())
        put(typeOf<Double>(), Arb.double().toGenerator())
        put(typeOf<Float>(), Arb.float().toGenerator())
        put(typeOf<Boolean>(), Arb.boolean().toGenerator())
        put(typeOf<String>(), Arb.string().toGenerator())
        put(typeOf<UUID>(), Arb.uuid().toGenerator())
        put(typeOf<Short>(), Arb.short().toGenerator())
        put(typeOf<Byte>(), Arb.byte().toGenerator())
        put(typeOf<Char>(), Arb.char().toGenerator())
        put(typeOf<ZoneId>(), Arb.zoneId().toGenerator())
        put(typeOf<Locale>(), Arb.locale().map { Locale.of(it) }.toGenerator())
        put(
            typeOf<Instant>(),
            Arb
                .kotlinInstant(
                    minValue = Instant.parse("1990-01-01T00:00:00Z"),
                    maxValue = Instant.parse("2100-01-01T00:00:00Z"),
                ).toGenerator(),
        )
    }
