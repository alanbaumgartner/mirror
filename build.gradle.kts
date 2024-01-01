plugins {
    kotlin("jvm") version "1.9.21"
    id("maven-publish")
}

group = "dev.narcos.mirror"
version = "0.1.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.9.22")
}

tasks.test {
    useJUnitPlatform()
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}

kotlin {
    jvmToolchain(8)
}