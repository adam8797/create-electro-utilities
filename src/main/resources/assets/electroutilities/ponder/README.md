# Ponder schematics

Each Ponder scene is driven by a structure schematic (`.nbt`) that lives in this folder. The Java
storyboards are in `client/ponder/EUPonderScenes.java`; the plugin that binds them is
`client/ponder/EUPonderPlugin.java`. **Until the `.nbt` files below exist, the scenes open but show an
empty base plate** — the storyboard text still displays.

## Expected files & layouts

Each storyboard sets its own base-plate size (`configureBasePlate(0, 0, size)`), with the plate at y=0 and
blocks starting at y=1. Grid coordinates below are `(x, y, z)` as used by `util.grid().at(...)`. The base
plate size is not fixed — change the `configureBasePlate(...)` size in `EUPonderScenes.java` and the
`pointAt` coords together if you want a different footprint.

| File | Plate | Bound to | Assumed layout |
|------|-------|----------|----------------|
| `utility_pole.nbt` | 5×5 | all `*_utility_pole` items | A utility pole 3 tall at column `(2, 2)` → blocks at `(2,1,2)`, `(2,2,2)`, `(2,3,2)`. |
| `crossarm.nbt` | 5×5 | `utility_pole_crossarm` | A utility pole at `(2, 1..3, 2)` with a crossarm applied across the top (arm blocks at `(1,3,2)` and `(3,3,2)`). |
| `substation_pole.nbt` | 7×7 | all wood `*_substation_pole` items | Two poles: a connector pole at column `(2, 3)` (connector at `(2,3,3)`), and the articulated switch pole at column `(4, 3)` — pole `(4,1..2,3)`, high-voltage switch at `(4,3,3)`, lever at its base `(4,1,2)`. |

If you change a layout, update the `util.grid().at(...)` / `pointAt(...)` positions in `EUPonderScenes.java`
to match.

## How to author an `.nbt`

Ponder schematics are ordinary Minecraft structure templates:

1. In a creative world, build the layout on a flat area.
2. Save it with a **Structure Block** (Save mode): set a name, set the size/anchor to capture the build, and
   click Save. It writes to `<world>/generated/minecraft/structures/<name>.nbt`.
3. Copy that file here as `assets/electroutilities/ponder/<file>.nbt` using the names in the table above.
4. Reload resources (F3+T) or restart; open the item and press **W** (Ponder) to view the scene.

Tip: keep builds small and centered on the 5×5 plate so the camera framing matches the storyboard.
