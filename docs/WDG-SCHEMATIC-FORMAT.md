# WDG Schematic Format Version 1

## Purpose

WDG schematic files provide a Forge 1.7.10 clipboard format whose authoritative
block identity is the complete Forge registry name rather than the numeric block
ID assigned by one installation. The extension is `.wdgschem`; command aliases
are `wdg` and `wdgschem`.

This is a WDG-specific format. It is not a Sponge schematic and does not claim
compatibility with modern WorldEdit schematic formats.

## Container and root

Files are compressed NBT written through WorldEdit's JNBT implementation.

* Root compound name: `WDGSchematic`
* `Version`: integer `1`
* Unknown versions are rejected. Readers must not guess a future schema.

## Version 1 schema

```text
WDGSchematic (Compound)
├── Version          Int       = 1
├── TileEntityPolicy String    optional: preserve or strip
├── Width            Int       > 0
├── Height           Int       > 0
├── Length           Int       > 0
├── WEOriginX        Int       region minimum X
├── WEOriginY        Int       region minimum Y
├── WEOriginZ        Int       region minimum Z
├── WEOffsetX        Int       minimum X minus clipboard origin X
├── WEOffsetY        Int       minimum Y minus clipboard origin Y
├── WEOffsetZ        Int       minimum Z minus clipboard origin Z
├── Palette          List<String>
├── Blocks           IntArray  palette indexes
├── Data             ByteArray legacy metadata values 0 through 15
├── TileEntities     List<Compound>
└── Entities         List<Compound>
```

The validated volume is `Width * Height * Length`, calculated using `long`
before array allocation. `Blocks` and `Data` must both have exactly that length.
The array index for relative block coordinates is:

```text
index = y * Width * Length + z * Width + x
```

## Block palette and metadata

Palette entries are complete, case-preserving, namespaced Forge registry names such
as `minecraft:stone` or `examplemod:machine_block`. The writer inserts names in
first-encounter order while iterating the clipboard region. Repeated names reuse
the same palette entry.

Numeric block IDs are not stored as authoritative identity. During load, each
palette name is resolved through the active `BlockRegistryNameResolver`, so a
block saved as numeric ID 300 may load as numeric ID 812 when both installations
map those IDs to the same registry name.

Legacy metadata is stored separately in `Data`. Registry names never contain a
metadata suffix and are not interpreted as modern blockstate syntax.

## Tile entities and preservation policy

New version 1 files include the optional root string `TileEntityPolicy`. Its
canonical values are `preserve` and `strip`. Files created before this tag was
introduced remain valid; a missing tag is interpreted as `preserve` because
those writers always preserved available tile-entity NBT. A present tag with the
wrong NBT type, an empty value, or an unsupported value is malformed.
The format version remains `1`.

With `preserve`, each tile entity is stored as its complete compound. The writer
preserves all available fields and replaces only `x`, `y`, and `z` with relative
block coordinates. This includes nested compounds, lists, byte arrays, integer
arrays, inventories, slot numbers, stack counts, item NBT, progress values,
ownership data, energy or fluid values, and mod-specific GUI-backed
configuration captured by the active world adapter. No key allowlist, denylist,
or selective filtering is applied.

With `strip`, the required `TileEntities` list remains present but is empty. The
block registry palette, metadata, dimensions, origin, offset, and `Entities` list
are unchanged. Strip mode removes only tile-entity NBT; it does not remove copied
entities. The source clipboard and source world are not mutated. On paste, Forge
or Minecraft may create a fresh default empty tile entity for the placed block.
Data omitted by strip mode cannot be recovered during load.

On load, preserved compounds are attached to matching blocks. The existing Forge
paste path rewrites destination coordinates before creating the tile entity. Tile
NBT is never attached when that block's registry name was missing and the block
was replaced with air. A stripped file is valid and naturally provides no tile
NBT to attach.

## Entities

Each entity compound preserves the available entity NBT and contains:

* `id`: non-empty entity type string
* `Pos`: exactly three doubles relative to the region minimum
* `Rotation`: exactly two floats in yaw, pitch order

Entities are only present when the clipboard itself contains them, such as after
`//copy -e`. This format does not change default copy or paste behaviour.

## Missing blocks

A syntactically valid palette name that is absent from the destination registry
is replaced with the destination installation's `minecraft:air`. Unique missing
registry names are collected once, logged once, and reported to the player with
a bounded list. No legacy alias or numeric fallback is attempted.

Malformed names, invalid indexes, unsupported metadata, invalid dimensions,
invalid tile/entity records, and unsupported versions fail the load rather than
being guessed or partially accepted.
