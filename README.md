# Create: Electro Utilities

A [Create](https://github.com/Creators-of-Create/Create) /
[Create: Electro Energetics](https://github.com/george8188625/Create-Electro-Energetics) (EE) add-on for
**Minecraft 1.21.1 / NeoForge**.

Adds power-line infrastructure that plugs into EE's wiring system:

- **Utility Pole** — an 8×8×16 wooden pole in every vanilla wood, placeable vertically or horizontally,
  removed with an axe.
  - **Crossarm** — a top attachment that spans an extra half block each way (3 blocks wide, Create-track
    style) and hosts three evenly-spaced EE wire connectors. Stackable vertically. Toggles between
    *centered* and *offset* with a wrench. Accepts EE's utility pole hangar.
  - **Utility Pole Label** — a pressed brass tag (Mechanical Press: brass nugget → label) that applies a
    short (≤5 char) text label to a pole.
- **Substation Pole** — a thinner 4×4 shaft-style multiblock (arrow-guided placement, max 16 tall) that
  relays a redstone signal from its base straight to its top. Its model extends to visually connect to an
  adjacent Create Redstone Link.

## Building

NeoForge 1.21.1 targets **Java 21**, and Gradle itself must *run* on Java 21 (not a newer JVM).

```bash
JAVA_HOME=/usr/lib/jvm/temurin-21-jdk-amd64 ./gradlew build
```

(Adjust the path to your Java 21 install. If your system default `java` is already 21, you can omit the
`JAVA_HOME` prefix.)

### Dependencies

- **Create**, **Ponder**, **Flywheel**, **Registrate** — resolved from Maven automatically.
- **Create: Electro Energetics** — supplied as a local jar in [`libs/`](libs/). To update it, drop the new
  release jar in `libs/` and bump `ee_version` in `gradle.properties`.

To run the dev client (`runClient`), you additionally need EE's own soft dependencies installed if you use
the features that rely on them; EE only *hard*-requires Create.

## License

MIT
