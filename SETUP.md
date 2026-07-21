# Development setup

WorldEdit-Forge-WDG is a legacy Minecraft Forge 1.7.10 project. Use a **64-bit Java 8 JDK** for every Gradle, IDE, client, and server development task.

## 1. Select Java 8

Confirm the active JDK before running Gradle:

```bash
java -version
javac -version
```

Both commands should report Java 8, commonly shown as `1.8.0_xxx`.

### macOS

List installed JDKs:

```bash
/usr/libexec/java_home -V
```

Select Java 8 for the current terminal:

```bash
export JAVA_HOME="$(${JAVA_HOME:-/usr/libexec/java_home} -v 1.8)"
export PATH="$JAVA_HOME/bin:$PATH"
```

For the user's normal checkout:

```bash
cd /Users/rhysh/Documents/GitHub/WorldEdit-Forge-WDG
```

On Apple Silicon, use a Java 8 distribution and architecture that work with the legacy Minecraft/Forge native libraries. If the selected Java 8 runtime requires Rosetta, launch the development terminal or IDE accordingly.

### Windows

Set `JAVA_HOME` to the Java 8 JDK directory and place `%JAVA_HOME%\bin` before newer Java installations in `PATH`. Verify with:

```bat
java -version
javac -version
```

### Linux

Select the installed Java 8 JDK using your distribution's alternatives mechanism or export `JAVA_HOME` directly, then verify both commands report Java 8.

## 2. Use the Gradle wrapper

Do not install or invoke a system Gradle version. Use:

```bash
./gradlew --version
```

or on Windows:

```bat
gradlew.bat --version
```

The wrapper uses Gradle 4.5 with the maintained ForgeGradle 1.2 compatibility fork (1.2-1.1.1). The first invocation downloads Gradle and build dependencies.

During workspace generation, ForgeGradle may print a `makeStart` warning mentioning `-source 1.6`. That warning belongs to ForgeGradle's generated launcher bootstrap, not the WorldEdit modules. Both modules and their Java compile tasks are explicitly enforced as Java 8 and checked by `verifyRepository`.

## 3. Prepare the Forge workspace

macOS/Linux:

```bash
./gradlew clean setupDecompWorkspace
```

Always use the checked-in wrapper (`./gradlew` or `gradlew.bat`) rather than a separately installed `gradle` command. To generate IntelliJ IDEA project files after workspace setup, run:

```bash
./gradlew setupDecompWorkspace idea
```

Windows:

```bat
gradlew.bat clean setupDecompWorkspace
```

Then verify the repository foundation:

```bash
./gradlew verifyRepository
```

## 4. IntelliJ IDEA

Generate the complete legacy Gradle/Forge IntelliJ workspace:

```bash
./gradlew cleanIdea idea
```

This creates `WorldEdit-Forge-WDG.ipr` and `WorldEdit-Forge-WDG.iws` in the repository root, plus one `.iml` module file for each subproject. These files are generated locally and intentionally ignored by Git.

Open `WorldEdit-Forge-WDG.ipr` in IntelliJ IDEA. You may alternatively open the repository directory or root `build.gradle` and import it as a Gradle project. Configure both the project SDK and Gradle JVM to Java 8; do not let IntelliJ silently select Java 17, 21, or another modern runtime for Gradle.

Use the Gradle tool window or the IntelliJ terminal to execute `:worldedit-forge:runClient` and `:worldedit-forge:runServer`. Generated Forge run configurations may also be used when IntelliJ exposes them.

## 5. Eclipse

Generate Eclipse metadata with:

```bash
./gradlew eclipse
```

Import the existing projects into a workspace using a Java 8 JDK. The generated `eclipse/` runtime directory and IDE metadata are intentionally ignored by Git.

## 6. Build

```bash
./gradlew clean build
```

The installable JAR is produced in:

```text
worldedit-forge/build/libs/
```

Use the artifact ending in `-dist.jar`, for example:

```text
WorldEdit-Forge-WDG-6.1.2_X1-dist.jar
```

The `-dist.jar` is the shaded Forge distribution and is reobfuscated for installation. The `-dev`, `-unshaded-dev`, `-sources`, and `-javadoc` files are development artifacts.

## 7. Run development Minecraft

Client:

```bash
./gradlew runClient
```

Dedicated server:

```bash
./gradlew runServer
```

The generated game files are kept under the Forge runtime directory and are ignored by Git.

ForgeMultipart is an optional WorldEdit compatibility target. Its development API is compile-only, so a normal WorldEdit client/server smoke does not load ForgeMultipart, CodeChickenLib, or CodeChickenCore. Test multipart compatibility separately with a complete matching mod set when that integration is changed.

## WDG schematic tile-entity policy

The generated Forge development configuration is normally located at:

```text
worldedit-forge/eclipse/config/worldedit/worldedit.properties
```

The server-authoritative default is:

```properties
wdg-schematic-tile-entity-policy=preserve
```

Supported values are exactly `preserve` and `strip`. Preserve writes complete
available tile-entity NBT. Strip writes an empty WDG `TileEntities` list while
retaining blocks, metadata, origin, offset, and copied entities. Direct
`//copy`, `//paste`, and legacy `.schematic` behaviour are unchanged.

Override the configured default for one WDG save with:

```text
//schem save -p wdg <filename>
//schem save -s wdg <filename>
```

The `wdgschem` alias works identically. The switches are mutually exclusive and
are rejected for legacy schematic saves. On dedicated servers the dedicated
server configuration is authoritative; integrated servers use their generated
server-side WorldEdit configuration. Restart the development client or server
after changing the generated property.

## WDG version behaviour

The base version comes from the root `gradle.properties` file:

```text
6.1.2_X<modBuildNumber>
```

A successful release-producing invocation (`build`, `assemble`, `jar`, `shadowJar`, or `reobfShadowJar`) calculates one next build number for the whole invocation and writes it back once after success. It never increments once per module.

The following do not increment the number:

- `clean`
- `setupDecompWorkspace` and other workspace setup tasks
- `idea` or `eclipse`
- tests and repository verification
- dependency inspection
- `runClient`
- `runServer`

Add an optional branch suffix with:

```bash
./gradlew clean build -Pbranch=dev
```

That produces a version like:

```text
6.1.2_X1-dev
```

When the repository is built from an exported archive without `.git`, the build uses a safe `no_git_id` internal manifest value rather than failing or exposing an unresolved token.
