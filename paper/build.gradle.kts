plugins { java }

group = "dev.caydenhatch"
version = "1.1.0"

repositories { maven("https://repo.papermc.io/repository/maven-public/") }

dependencies { compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT") }

java { toolchain.languageVersion.set(JavaLanguageVersion.of(21)) }
tasks.withType<JavaCompile>().configureEach { options.release.set(21) }
tasks.jar { archiveBaseName.set("CombatSync-Paper") }
