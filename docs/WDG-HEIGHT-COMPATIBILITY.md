# WDG clipboard coordinates, extended height and schematic listing

The WDG v1 reader previously constructed a default CuboidRegion, which clamps Y to 0..255 without a world. Valid detached clipboard coordinates below zero or above 255 were clipped before block loading. The converted bunker starts at Y=-11, u-72 at -34 and has at -34, producing `Failed to set clipboard block at 0,0,0` in X8. These values describe saved clipboard coordinates, not permission to place blocks below the world's floor.

The reader now explicitly constructs an unclamped clipboard region. The existing CuboidRegion constructors retain their clamping behavior for world selections. Geometry, origin, paste offsets, tile NBT and the WDG v1 format stay intact.

ForgeWorld now reports its maximum Y from the active Minecraft World's getHeight() minus one. EFR Plus extends the Overworld provider's height through that API. This avoids an EFR dependency, a global cached height, or assuming every dimension is extended. Real-world edits still obey the destination world's floor and ceiling.

`/schem list 2` now selects page 2. `/schem list -p 2` remains supported, and an explicit -p takes precedence when both forms are supplied. The page count rounds up correctly without adding an empty page for exact multiples of nine. A missing schematic directory reports no schematics instead of throwing a null-pointer exception.

## Verification

- Original X8 distribution reproduced the exact bunker load failure using the three converted schematic fixtures.
- WDG tests load every block, metadata value and all 272 tile entities in the actual three files against the saved installed-pack block registry, checking dimensions and paste offsets.
- Detached coordinate tests cover minima -34, -11, 250, 384 and 500 and round-trip serialization.
- Forge adapter tests cover heights 256, 384, 400, 512 and a generic 1024, including two different worlds. EFR's current supported configurable range is 384..512; the generic 1024 fixture does not expand EFR support.
- Actual command-dispatch tests cover positional page, -p, precedence, sort flags, aliases, defaults, invalid page bounds and non-integer input, with exactly 18 files.
- EFR's existing CrucibleHeightBytecodeContracts passed against the supplied unchanged Crucible dev-280 jar and the built EFR 3.6.0_X9 production jar. This verifies EFR's specific interaction-height transformer, not a complete modpack boot or a live WorldEdit paste.

The updated WorldEdit build is intended for installation in the server's mods directory. Keep the supplied Crucible jar and the existing height configuration. Keep EFR client/server versions compatible with that configuration. No full live Crucible server session was run during this fix; restart and load/paste one schematic to confirm the complete installed pack.
