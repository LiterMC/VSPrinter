# VSPrinter

[![Modrinth Downloads](https://img.shields.io/modrinth/dt/vsprinter?color=4&label=Downloads&logo=modrinth)](https://modrinth.com/mod/vsprinter/versions)
[![CurseForge Downloads](https://cf.way2muchnoise.eu/vsprinter.svg)](https://www.curseforge.com/minecraft/mc-mods/vsprinter/files/all)

## Compats

### ComputerCraft

When filming area contains any kind of computer from `CC: Tweaked`, the special file `/.vsprinter` can affect the filming process.  
It uses Java `.properties` file formmat.

If the file exists and `blueprint` is `true`, Quantum Film can save & copy the files inside the computer.

> [!WARNING]
> Peripherals may not be ready when ship just cloned. It is recommend to wait at least 0.1s before find & access peripherals in the startup script.

#### Include paths

Defined by `includes=<path1>, <path2>, ..., <path N>`

If the key is not defined, the root mount will be saved.
If the key is defined, only the paths lists will be saved.

#### Ignore paths

Defined by `ignores=<path1>, <path2>, ..., <path N>`

#### On printed computer

- `printed` value will be `true`
- `parent-ship-id` will be the printer's ship's ID (if exists)
- `parent-ship` will be the printer's ship's name (if exists)

### Create

Item consumtion will also respect blocks that implemented `com.simibubi.create.content.schematics.requirement.ISpecialBlockItemRequirement`.

## Credits

- Textures were made by the great [@PacificCyan](https://modrinth.com/user/PacificCyan)
