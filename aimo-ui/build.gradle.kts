
plugins {
    id("org.ivcode.gradle-www") version "0.1.0-SNAPSHOT"
    id("org.ivcode.gradle-publish") version "0.1-SNAPSHOT"
}

www {
    resources = "${projectDir}/dist"
}

tasks.named<Copy>("www-CopyResources") {
    dependsOn("build-resources")
}

tasks.named("processResources") {
    dependsOn("www-CopyResources")
}

tasks.register<Exec>("build-resources") {
    group = "build"
    description = "Run npm run build in the www directory"

    val isWindows = System.getProperty("os.name").lowercase().contains("windows")
    if (isWindows) {
        commandLine("cmd", "/c", "npm install && npm run build")
    } else{
        commandLine("sh", "-c", "npm install && npm run build")
    }
}
