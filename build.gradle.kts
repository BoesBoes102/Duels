plugins {
    id("java")
    id("com.gradleup.shadow") version "9.2.2"
}

group = "com.boes"
version = "1.0.3"

repositories {
    maven("https://jitpack.io")
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.aikar.co/content/groups/aikar/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.8-R0.1-SNAPSHOT")
    compileOnly("com.fastasyncworldedit:FastAsyncWorldEdit-Core:2.6.3")
    compileOnly("com.fastasyncworldedit:FastAsyncWorldEdit-Bukkit:2.6.3")
    implementation("com.google.code.gson:gson:2.13.2")
    implementation("co.aikar:acf-paper:0.5.1-SNAPSHOT")
    compileOnly("com.github.MilkBowl:VaultAPI:1.7") {
        exclude(group = "org.bukkit", module = "bukkit")
    }
}

tasks.shadowJar {
    relocate("co.aikar.commands", "com.boes.duels.shade.acf")
    relocate("co.aikar.locales", "com.boes.duels.shade.locales")
    archiveClassifier.set("")
}

tasks.build {
    dependsOn(tasks.shadowJar)
}
