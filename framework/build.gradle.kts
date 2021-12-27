import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.jetbrains.kotlin.jvm")
}


group = "dev.alpas"

repositories {
    mavenCentral()
    maven("https://jitpack.io")
}

dependencies {
    api("org.jetbrains.kotlin:kotlin-stdlib:1.6.10")
    api("org.jetbrains.kotlin:kotlin-reflect:1.6.10")
    testImplementation("org.junit.jupiter:junit-jupiter:5.8.2")
    testImplementation("io.mockk:mockk:1.12.1")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

//tasks.withType<KotlinCompileTest> {
//    kotlinOptions.jvmTarget = "1.8"
//}

tasks.withType<Test> {
    useJUnitPlatform()
}

apply(from = "alpas.gradle.kts")
