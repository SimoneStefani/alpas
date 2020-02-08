package dev.alpas.ozone

import me.liuwj.ktorm.entity.Entity
import me.liuwj.ktorm.schema.TypeReference
import java.time.Instant
import kotlin.reflect.jvm.jvmErasure

interface Ozone<E : Ozone<E>> : Entity<E> {
    abstract class Of<E : Ozone<E>> : TypeReference<E>() {
        /**
         * Overload the `invoke` operator, creating an Ozone object just like there is a constructor.
         */
        @Suppress("UNCHECKED_CAST")
        operator fun invoke(): E {
            return Entity.create(referencedKotlinType.jvmErasure) as E
        }

        /**
         * Overload the `invoke` operator, creating an Ozone object and call the [init] function.
         */
        inline operator fun invoke(init: E.() -> Unit): E {
            return invoke().apply(init)
        }
    }
}
