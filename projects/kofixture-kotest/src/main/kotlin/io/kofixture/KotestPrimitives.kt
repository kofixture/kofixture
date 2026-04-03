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
import java.util.Locale
import kotlin.time.Instant

val kotestPrimitivesModule =
    fixtureModule {
        register<Int> { Arb.int() }
        register<Long> { Arb.long() }
        register<Double> { Arb.double() }
        register<Float> { Arb.float() }
        register<Boolean> { Arb.boolean() }
        register<String> { Arb.string() }
        register<java.util.UUID> { Arb.uuid() }
        register<Short> { Arb.short() }
        register<Byte> { Arb.byte() }
        register<Char> { Arb.char() }
        register<java.time.ZoneId> { Arb.zoneId() }
        register<Locale> { Arb.locale().map { Locale.of(it) } }
        register<Instant> {
            Arb.kotlinInstant(
                minValue = Instant.parse("1990-01-01T00:00:00Z"),
                maxValue = Instant.parse("2100-01-01T00:00:00Z"),
            )
        }
    }
