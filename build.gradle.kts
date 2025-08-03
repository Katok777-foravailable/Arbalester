plugins {
    id("java")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(24))
}

repositories {
    mavenCentral()

    maven {
        name = "papermc"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.20.4-R0.1-SNAPSHOT")
    compileOnly("mysql:mysql-connector-java:8.0.25")
    compileOnly("com.zaxxer:HikariCP:6.3.0")
}

tasks.register<Copy>("jarCopy") {
    from(tasks.jar.get().outputs.files.singleFile)
    into("D:\\Codespace\\Java\\Minecraft\\Servers\\1.21.4\\plugins")
}

tasks.jar {
    finalizedBy("jarCopy")
}