# Compiling WorldEdit Forge WDG Edition

This repository requires a **Java 8 JDK**. A JRE alone is not sufficient, and newer Java versions should not be used to run this legacy ForgeGradle toolchain.

WorldEdit-Forge-WDG contains two modules:

- `worldedit-core`
- `worldedit-forge`

The Gradle wrapper downloads Gradle 4.5 on first use. ForgeGradle then downloads and prepares the Minecraft 1.7.10 / Forge 10.13.4.1614 development workspace. The first setup can take considerably longer than later builds.

ForgeGradle's generated `makeStart` launcher may emit a legacy `-source 1.6` warning during workspace preparation. This does not describe the WorldEdit module compiler target; `worldedit-core`, `worldedit-forge`, and their Java compile tasks are set to Java 8.

The build pins Checkstyle 5.7, matching the repository's original Gradle 2.0 toolchain, and supplies the root `basedir` property required by the retained WorldEdit Checkstyle configuration. This prevents Gradle 4.5 from silently switching the legacy rules to Checkstyle 6.19.

## Workspace setup

macOS, Linux, and other Unix-like systems:

```bash
./gradlew clean setupDecompWorkspace
```

Use the repository wrapper, not a system-installed `gradle` executable. After the Forge workspace has been prepared, generate the complete IntelliJ IDEA project with:

```bash
./gradlew cleanIdea idea
```

The root project generates `WorldEdit-Forge-WDG.ipr` and `WorldEdit-Forge-WDG.iws`; the two subprojects generate their `.iml` module files. All generated IDE metadata is ignored by Git.

Windows Command Prompt or PowerShell:

```bat
gradlew.bat clean setupDecompWorkspace
```

## Repository verification

```bash
./gradlew verifyRepository
```

This checks the required two-module layout, Java 8 configuration, target Minecraft and Forge versions, central version properties, generated `mcmod.info`, wrapper tracking rules, and obvious macOS archive contamination.

## Normal build

macOS and Linux:

```bash
./gradlew clean build
```

Windows:

```bat
gradlew.bat clean build
```

A successful release-producing build increments the committed `modBuildNumber` once for the entire Gradle invocation. Both modules and every artifact generated in that invocation use the same resolved version.

## Focused validation tasks

```bash
./gradlew :worldedit-core:test
./gradlew :worldedit-forge:compileJava
./gradlew :worldedit-forge:processResources
./gradlew :worldedit-forge:shadowJar
```

Running `shadowJar` directly is a release-producing invocation and therefore advances the build number once when it succeeds. Tests, `verifyRepository`, setup, IDE generation, `runClient`, and `runServer` do not increment it.

## Output files

Core/API artifacts are written to:

```text
worldedit-core/build/libs/
```

Forge artifacts are written to:

```text
worldedit-forge/build/libs/
```

The normal installable Forge mod is the reobfuscated shaded artifact:

```text
WorldEdit-Forge-WDG-6.1.2_X<build>-dist.jar
```

Do not install these development artifacts into a normal modpack:

- `*-dev.jar`
- `*-unshaded-dev.jar`
- `*-sources.jar`
- `*-javadoc.jar`

## Development runs

After workspace setup:

```bash
./gradlew runClient
./gradlew runServer
```

These commands require user-side runtime validation. They do not advance the WDG build number.

The development runtime intentionally excludes ForgeMultipart and its CodeChicken dependencies. WorldEdit retains the ForgeMultipart API as a compile-only optional integration, preventing an incomplete third-party mod stack from breaking ordinary client/server smokes.

## CI-style workspace

For a non-interactive compile environment, ForgeGradle also provides:

```bash
./gradlew clean setupCIWorkspace build
```

Use this only where the normal decompiled development workspace is not needed.


The build retains the legacy WorldEdit Checkstyle rules and pins Checkstyle 5.7, matching the original Gradle 2.0 build.

The legacy Shadow plugin and ForgeGradle reobfuscator share a pinned ASM 9.4 build-script runtime. This prevents Shadow's historical ASM 5 dependency from overriding the newer ASM API required by ForgeGradle's SpecialSource implementation.
