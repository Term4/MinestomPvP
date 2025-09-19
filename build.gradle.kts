plugins {
    java
    `maven-publish`
}

group = "io.github.togar2"

java {
    toolchain.languageVersion = JavaLanguageVersion.of(21)
    withSourcesJar()
    withJavadocJar()
}

repositories {
    mavenCentral()
}

dependencies {
    compileOnly("net.minestom:minestom:2025.07.10b-1.21.7")
    testImplementation("net.minestom:minestom:2025.07.10b-1.21.7")
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
        }
    }
}

tasks.register<Copy>("copyToProject") {
    from(tasks.jar)
    into("D:\\minecraft-test-server\\minestom\\minestom-test\\libs")
    rename { "MinestomPvP.jar" }
}

tasks.build {
    finalizedBy("copyToProject")
}