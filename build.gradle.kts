plugins {
    id("java")
    id("application")
}

group = "io.github.akmal2409"
version = "1.0-SNAPSHOT"

val logbackVersion = "1.4.14"

repositories {
    mavenCentral()
}

java {
    version = 21
}

application {
    mainClass = "io.github.akmal2409.dnsforwarder.server.Server"
}

dependencies {
    implementation("org.slf4j:slf4j-api:2.0.11")
    implementation("ch.qos.logback:logback-core:$logbackVersion")
    implementation("ch.qos.logback:logback-classic:$logbackVersion")

    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.assertj:assertj-core:3.25.2")
    testImplementation("org.mockito:mockito-core:5.10.0")
    testImplementation("org.mockito:mockito-junit-jupiter:5.10.0")
}

tasks.test {
    useJUnitPlatform()
}
