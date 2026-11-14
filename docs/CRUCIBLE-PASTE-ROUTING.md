# Paste command routing on Crucible

The server log confirms that the WDG file loads successfully. Subsequent `//paste` requests appear in Minecraft's incoming-command log but do not reach WorldEdit's command logger. The unknown-command response therefore occurs before the paste operation executes; it is not a schematic decoding exception.

Previously ClipboardCommands exposed only the internal `/paste` alias, requiring a double slash in chat. CommandWrapper used that slash-prefixed alias as its primary native command name. The supplied Crucible dev-280 CraftSimpleCommandMap bytecode routes ModCustomCommand entries back through the native command manager; this makes slash-prefixed native command names fragile across the additional dispatch layer. The exact component producing the user's modern-style unknown-command text was not identified from the supplied log alone.

X10 exposes `paste` as the primary native command, retains `/paste` for double-slash compatibility, and adds `wdgpaste` as a distinctive fallback. All three aliases point to the same callable, flags and worldedit.clipboard.paste permission. The schematic-load success message now recommends `/paste` or `/wdgpaste`.

Use:

```text
/schem load wdg bunker-e930-1.7.10.wdgschem
/wdgpaste -a
```

`/paste -a` is also supported. Neither command performs a world paste until explicitly issued by the player. X9's clipboard-coordinate, active world-height and list-pagination fixes are retained.

Regression coverage builds the actual ClipboardCommands dispatcher and Forge CommandWrapper, verifies the slashless primary command and all aliases, and checks that they share the original permission and callable. Full Gradle build, Checkstyle, core and Forge tests must pass before distributing the jar. After installing X10, the user confirmed that the bunker pasted successfully on the affected live server.
