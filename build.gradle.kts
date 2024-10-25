plugins {
    kotlin("jvm") version "2.0.20"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "kr.hwaryuh"
version = "0.8-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://oss.sonatype.org/content/groups/public/")

    maven("https://nexus.phoenixdevt.fr/repository/maven-public") // MMO API
    maven("https://jitpack.io") // Vault
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.20.1-R0.1-SNAPSHOT")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    compileOnly("io.lumine:MythicLib-dist:1.6.2-SNAPSHOT") // MMOLib 1.6 or 1.6.2
    compileOnly("net.Indyuce:MMOCore-API:1.12.1-SNAPSHOT") // MMOCore
    compileOnly("com.github.MilkBowl:VaultAPI:1.7.1")
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
