dependencies {
    implementation("org.eclipse.jetty:jetty-webapp:9.4.26.v20200117") // FIXME
    implementation("com.github.danielnorberg:rut:v1.0.1")
    implementation("org.picocontainer:picocontainer:2.15")
    implementation("io.github.cdimascio:dotenv-kotlin:6.2.2")
    implementation("io.github.classgraph:classgraph:4.8.138")
    implementation("com.github.simbiose:Encryption:2.0.1")
    implementation("org.slf4j:slf4j-api:2.0.0-alpha5")
    implementation("de.mkammerer:argon2-jvm:2.11")
    implementation("uy.kohesive.klutter:klutter-core:3.0.0")
    implementation("org.apache.qpid:qpid-jms-client:1.5.0")
    implementation("org.simplejavamail:simple-java-mail:6.7.5")
    implementation("com.github.alpas:url-signer:0.1.7")
    implementation("com.github.freva:ascii-table:1.2.0")

    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.0")
    api("org.eclipse.jetty:jetty-server:9.4.26.v20200117") // FIXME
    api("com.zaxxer:HikariCP:5.0.0")
    api("org.atteo:evo-inflector:1.3")
    api("io.pebbletemplates:pebble:3.1.5")
    api("com.github.ajalt.clikt:clikt:3.3.0")
    api("com.github.ajalt:mordant:1.2.1")
    api("io.github.microutils:kotlin-logging:2.1.21")
    api("me.liuwj.ktorm:ktorm-core:2.6.1") // FIXME
    api("me.liuwj.ktorm:ktorm-jackson:2.6.1") // FIXME
    api("me.liuwj.ktorm:ktorm-support-mysql:2.6.1") // FIXME
    api("me.liuwj.ktorm:ktorm-support-postgresql:2.6.1") // FIXME
    api("me.liuwj.ktorm:ktorm-support-sqlite:2.6.1") // FIXME
    api("com.github.marlonlom:timeago:4.0.3")
    api("com.fasterxml.jackson.module:jackson-module-kotlin:2.13.1")
    api("com.github.javafaker:javafaker:1.0.2")
    api("com.github.kittinunf.fuel:fuel:2.3.1")
    api("com.github.kittinunf.fuel:fuel-coroutines:2.3.1")
    api("com.github.kittinunf.fuel:fuel-jackson:2.3.1")

    testApi("org.xerial:sqlite-jdbc:3.36.0.3")
}
