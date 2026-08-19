# TWINGS

TWINGS is a simple yet powerful particle cosmetics Spigot plugin. While its main
purpose is just to give you a couple of fancy wings on your back, there's
**much** more:

- 🪽 Create custom wings with pixel patterns
- 🎨 **[Wing Designer](https://jasonholweg.de/twings/)** — paint wings in the
  browser with a live 3D preview, export ready-to-use wing files
- 📦 6 preset wings included out of the box
- 🌐 `/wings import <url>` — install wings or images straight from a link
- 🖼️ Create wings from pictures (`/wings create`)
- 🗂️ Category GUI with all created wings
- ⏱️ Give players wings temporarily or until death
- 🧩 Easy API for developers
- 🕊️ Animated wings (flapping / rotating)

## Version 3.0

Version 3.0 is a full rewrite for **Minecraft 1.20.5 – 26.2** (Spigot/Paper):

- Fixes the performance issues of 2.x (task leaks on reload, disk I/O in the
  click/render paths, unbounded memory growth, console spam)
- Wing files from 2.x keep working unchanged — old particle names like
  `REDSTONE` are translated automatically
- Player data (`wings_equiped.yml`) is migrated automatically
- Timed wings now survive restarts
- Tab completion for all commands

## Build

Requires JDK 21+ and Maven:

```
mvn clean package
```

The jar lands in `target/TWINGS-<version>.jar`.

## Commands

| Command | Description |
|---|---|
| `/wings` | Opens the wing menu |
| `/wings equip [wing]` | Equip a wing (clickable list without argument) |
| `/wings add [wing]` | Add a wing to the equipped ones |
| `/wings unequip` | Unequip everything |
| `/wings list` | List all wings you may use |
| `/wings give <player> <wing> <time\|untildeath>` | Grant wings temporarily (admin) |
| `/wings import <url> [name]` | Import a wing file or image from a link (admin) |
| `/wings create` | Create a wing from a picture in `plugins/TWINGS/pictures/` (admin) |
| `/wings preview` / `/wings edit` | Place world previews / live-edit a wing file (admin) |
| `/wings reload` | Reload configs and wings (admin, console-capable) |

## Links

**[Wing Designer](https://jasonholweg.de/twings/)** — design wings in the browser.

**[SpigotMC](https://www.spigotmc.org/resources/82088/)** — official resource page.

**[Discord](https://discord.gg/yn6kxKq88H)** — support server.
