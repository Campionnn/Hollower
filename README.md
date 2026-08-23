
# Hollower

## A Fabric mod for creating routes in the Hypixel Skyblock Crystal Hollows

This port supports Minecraft 1.21.11, 26.1.2, and 26.2.

This project was started due to the lack of mods for creating routes. It allows for very easy and intuitive creating, editing, and exporting of routes while also clearly visualizing them in the world. Many features were heavily inspired by [litematica](https://github.com/maruohon/litematica) and [tweakfork](https://github.com/Andrews54757/tweakfork)

### Requirements
* A Fabric instance for Minecraft 1.21.11, 26.1.2, or 26.2: https://fabricmc.net/
* Fabric API [Modrinth](https://modrinth.com/mod/fabric-api) | [CurseForge](https://www.curseforge.com/minecraft/mc-mods/fabric-api) | [GitHub](https://github.com/FabricMC/fabric)

### Download
Download the latest release from the [releases page](https://github.com/Campionnn/Hollower/releases)

### Compiling
* Clone the repository
* Use Java 25
* Run `./gradlew chiseledBuild` in the root directory
* The three compiled JAR files will be in `build/libs`

### Getting Started
* Install a supported Fabric instance
* Put all the above mods into the mods folder
* Follow the instructions in https://github.com/Campionnn/CleanCH to get a world save of a clean Crystal Hollows
* Most route related features will only work while holding a wooden pickaxe
* Press C to open the config menu where many settings can be changed
* Config, noclip, and selective rendering keys are listed under Hollower in Minecraft Controls
* Press N to toggle noclip in a local world. Use the movement keys, jump, and sneak to fly. Hold sprint to move faster.
* Fullbright is enabled by default for local worlds and can be disabled in the config menu.

### Exporting a Route
* Press C to open the config menu.
* On the Route tab, click Export Route.
* Select one of these formats in the export screen:
    * [Waypointer](https://github.com/ethanrjs/waypointer) (Recommended)
    * [SkyHanni](https://github.com/hannibal002/SkyHanni)
    * [Skyblocker](https://github.com/SkyblockerMod/Skyblocker)
* The selected route is copied to your clipboard.
* Waypointer exports use the v8 `WP:` [wire format](https://github.com/ethanrjs/waypointer/blob/main/CODEC.md).
* Waypointer and Skyblocker exports set the zone to Crystal Hollows automatically.

### Noclip (N)
* Noclip only works in local worlds. It does not activate on remote servers.
* The mod applies noclip to both the local player and the integrated server player.
* The mod reapplies the noclip collision state after each vanilla player-tick reset. This prevents block collisions and position corrections from pushing the player out of blocks.
* Move outside solid blocks before you disable noclip.
* The implementation follows the player-tick method used by the [noclip mod linked by the original project](https://github.com/dvitski/noclip).

### Features
* Place nodes by pressing the Use Item/Place Block key
* Delete nodes by pressing the Attack/Destroy key
* Select a node by pressing the Pick Block key
* While a node is a selected:
    * Creating a new node will insert it after the selected one
    * Holding the Nudge Key and scrolling will move the node towards or away from the direction you are facing
    * Holding the Swap Order Key and selecting another node will swap the positions of the two nodes
* Scrolling while holding the Swap Order Key will rotate the order of all the nodes (to change the location of the first node)
* While holding shift, press the Use Item/Place Block key to teleport on top of the block you are looking at (Etherwarp)
* Route Management:
    * Save and load multiple routes with folder organization
    * Rename, delete, and move routes between folders
    * Access via the config menu Route tab > Routes... button
* Route Optimizer:
    * Reorder your route's nodes into a shorter, straighter path without moving them
    * Customize cost parameters: horizontal/vertical distance scaling, turn penalties, closed-loop vs one-way traversal, and pin-first-node options
    * Shows estimated improvement percentage and supports undo
* Selective Rendering:
    * Allows you to enable/disable the rendering certain blocks so you don't get distracted by unnecessary clutter
    * In a local world hidden blocks are also passable, so you can walk and look straight through them without needing noclip
    * Hidden blocks still block light, so a hidden wall leaves the space behind it dark. Leave fullbright on
* Remove fog so you can see as far as your render distance

### Planned/Work in Progress
* Visualizing reachable gemstones from each node
* Render a plane to visualize where the magma field starts (maybe not necessary)
* Automatically generate optimized routes from scratch using given parameters similar to [seafoam](https://astanik.dev/seafoam/) but better (probably far future)
* Highlight selected nodes in the route a different color for organization
* Commands for route management
* Keybind/command to set block below current position and add it to the route
* Render a 9x9 chunk border around the center of the route to make sure all veins are within render distance
* If you have any suggestions, feel free to open an issue or contact me on discord @campionn
