plugins {
    kotlin("jvm") version "2.0.20"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "kr.hwaryuh"
version = "0.2-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://oss.sonatype.org/content/groups/public/")
    maven("https://nexus.phoenixdevt.fr/repository/maven-public") // MMO API
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.20.1-R0.1-SNAPSHOT")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    compileOnly("net.Indyuce:MMOItems-API:6.10-SNAPSHOT") // MMOItems 6.9.4 or 6.10
    compileOnly("io.lumine:MythicLib-dist:1.6.2-SNAPSHOT") // MMOLib 1.6 or 1.6.2
    compileOnly("net.Indyuce:MMOCore-API:1.12.1-SNAPSHOT") // MMOCore
}

val targetJavaVersion = 17
kotlin {
    jvmToolchain(targetJavaVersion)
}

tasks.build {
    dependsOn("shadowJar")
}

tasks.processResources {
    val props = mapOf("version" to version)
    inputs.properties(props)
    filteringCharset = "UTF-8"
    filesMatching("plugin.yml") {
        expand(props)
    }
}
