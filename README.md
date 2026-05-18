Structurebuddy is a Minecraft library mod for the Neoforge modloader
which adds additional utilities for worldgen structures,
including a faster jigsaw assembler which allows structure pieces with random sizes

To depend on structurebuddy, add the following to your build.gradle:

```gradle
repositories {
	maven {url = "https://maven.commoble.net"}
}

dependencies {
	jarJar(implementation("net.commoble.structurebuddy:structurebuddy:${structurebuddy_version}"))
}
```

For a list of available structurebuddy versions, check the maven:
https://maven.commoble.net/net/commoble/structurebuddy/structurebuddy/
