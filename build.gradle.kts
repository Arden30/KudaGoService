plugins {
    kotlin("jvm") version "2.0.0"
    kotlin("plugin.serialization") version "2.0.20"
    id("io.freefair.lombok") version "8.1.0"
}

group = "arden.java"
version = "1.0-SNAPSHOT"
val ktorVersion: String by project
val kotlinxVersion: String by project
val logbackVersion: String by project
val csvVersion: String by project
val dotenvVersion: String by project

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")
    implementation("ch.qos.logback:logback-classic:$logbackVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$kotlinxVersion")
    implementation("com.jsoizo:kotlin-csv-jvm:$csvVersion")
    implementation("io.github.cdimascio:dotenv-kotlin:$dotenvVersion")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(21)
}