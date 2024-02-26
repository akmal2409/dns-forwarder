plugins {
    `kotlin-dsl`
}

group = "io.github.akmal2409.dnsforwarder"
version = "1.0.0-SNAPSHOT"

repositories {
    gradlePluginPortal()
}

dependencies {
    implementation("net.ltgt.gradle:gradle-errorprone-plugin:3.1.0")
    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}
