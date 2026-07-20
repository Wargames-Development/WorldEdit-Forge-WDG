# Contributing

Thank you for contributing to WorldEdit Forge WDG Edition. This repository remains derived from the original WorldEdit project and retains its GNU Lesser General Public License terms and upstream attribution.

## Compatibility boundaries

- Target Java 8 source and bytecode.
- Preserve Minecraft 1.7.10 and Forge 10.13.4.1614 compatibility.
- Preserve WorldEdit's public API, command names, permissions, configuration keys, packet identifiers, registry identifiers, NBT keys, and saved behaviour unless an approved change explicitly requires otherwise.
- Avoid broad unrelated rewrites, mass reformatting, or speculative dependency upgrades.
- Keep client-only code out of dedicated-server execution paths.
- For later runtime changes, test both integrated singleplayer behaviour and a dedicated server, including a client join where practical.

## Code style

- Follow the established source style and Oracle Java conventions where they remain applicable.
- Use four spaces for indentation and no tabs.
- Keep lines near the existing 120-column limit when readability permits.
- Add useful Javadocs to public APIs and avoid empty `@param` or `@return` descriptions.
- Do not add `@author` tags.
- Keep implementations efficient and avoid unnecessary duplication.

## Change quality

Keep each contribution bounded and reviewable. Run the relevant tests and repository checks before submission:

```bash
./gradlew verifyRepository
./gradlew :worldedit-core:test
./gradlew clean build
```

Runtime-affecting work should also be checked with `runClient`, a singleplayer join, `runServer`, and a multiplayer join where practical. Include clear reproduction and validation notes with the change.

Use descriptive commit messages, with a concise summary and additional detail after a blank line when needed.
