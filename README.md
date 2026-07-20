# WorldEdit Forge WDG Edition

WorldEdit-Forge-WDG is the Wargames Development Group-maintained Forge 1.7.10 fork of the original WorldEdit project. This repository preserves WorldEdit's established editing behaviour, public API, package names, command surface, configuration, and saved formats while providing a maintained Java 8 development foundation for the WDG environment.

This foundation targets:

- Minecraft Java Edition 1.7.10
- Minecraft Forge 10.13.4.1614
- Java Development Kit 8

The public WorldEdit identity is retained. The Forge metadata identifier remains `WorldEdit`, the displayed mod name remains **WorldEdit**, and the source continues to use the original `com.sk89q` packages. WDG branding is limited to the repository, documentation, build version, and artifact filename.

## Repository structure

WorldEdit remains a multi-module Gradle project:

- `worldedit-core` contains the shared WorldEdit implementation and API.
- `worldedit-forge` contains the Forge 1.7.10 integration and produces the playable mod.

The Forge distribution shades the required core code and libraries into the installable artifact; the modules must not be flattened or built independently as unrelated projects.

## Building

Start with [SETUP.md](SETUP.md) for Java and IDE preparation, then use [COMPILING.md](COMPILING.md) for the validated build commands.

The installable artifact is produced under:

```text
worldedit-forge/build/libs/
```

Use the reobfuscated shaded JAR named like:

```text
WorldEdit-Forge-WDG-6.1.2_X<build>-dist.jar
```

Do not install the `-dev`, `-unshaded-dev`, `-sources`, or `-javadoc` artifacts into a normal Minecraft instance.

## Versioning

WDG builds use the existing WorldEdit 6.1.2 line with an `_X` build suffix, for example `6.1.2_X1`. A branch label can be added for local builds with `-Pbranch=dev`, producing a version such as `6.1.2_X1-dev`.

A successful release-producing Gradle invocation such as `build`, `assemble`, `jar`, `shadowJar`, or `reobfShadowJar` advances `modBuildNumber` once for the whole multi-module build. Setup, IDE generation, tests, verification, `runClient`, and `runServer` do not increment it.

## Upstream project and licence

WorldEdit was created by sk89q and has been developed by the WorldEdit team and contributors, now associated with the EngineHub project. Wargames Development Group maintains this Forge 1.7.10 edition as an additional fork; it does not replace or erase upstream authorship.

WorldEdit is free software licensed under the GNU Lesser General Public License version 3 or, where stated in existing source notices, any later version. See [LICENSE.txt](LICENSE.txt) and the copyright headers retained throughout the source tree.

Useful upstream references:

- [EngineHub](https://enginehub.org/)
- [WorldEdit documentation](https://worldedit.enginehub.org/)
- [WorldEdit source](https://github.com/EngineHub/WorldEdit)

## Contributing

Read [CONTRIBUTING.md](CONTRIBUTING.md) before submitting changes. Keep changes bounded and preserve Minecraft 1.7.10, Forge 1614, Java 8, and WorldEdit compatibility unless an approved change explicitly requires otherwise.
