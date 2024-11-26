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

    maven("https://jitpack.io") // Vault
    maven("https://nexus.phoenixdevt.fr/repository/maven-public") // MythicLib, MMOCore
    maven("https://maven.aestrus.io/releases") // Mythic Dungeons
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/") // PlaceholderAPI
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    compileOnly("io.papermc.paper:paper-api:1.20.1-R0.1-SNAPSHOT")

    compileOnly("com.github.MilkBowl:VaultAPI:1.7.1")
    compileOnly("io.lumine:MythicLib-dist:1.6.2-SNAPSHOT")
    compileOnly("net.Indyuce:MMOCore-API:1.13.1-SNAPSHOT")
    compileOnly("net.playavalon:MythicDungeons:1.3.0-SNAPSHOT")
    compileOnly("me.clip:placeholderapi:2.11.6")
}

kotlin { jvmToolchain(17) }

tasks {
    build { dependsOn(shadowJar) }
    processResources {
        filteringCharset = "UTF-8"
        filesMatching("plugin.yml") { expand("version" to version) }
    }
}
