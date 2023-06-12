import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val ktor_version = "1.3.2"
plugins {
    kotlin("jvm") version "1.7.10"
    application
}

group = "org.mk"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    google()
    maven(url = "https://dl.bintray.com/kotlin/ktor")
    jcenter()
}

dependencies {
    implementation("io.ktor:ktor-server-core:$ktor_version")
    implementation("io.ktor:ktor-server-netty:$ktor_version")
    implementation("org.slf4j:slf4j-api:1.7.5")
    implementation("org.slf4j:slf4j-simple:1.6.4")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

application {
    mainClass.set("MainKt")
    //todo:remove this
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=true")
}