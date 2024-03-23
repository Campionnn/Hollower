
# Hollower

## A Minecraft 1.20.1 Fabric mod for creating routes in the Hypixel Skyblock Crystal Hollows

This project was started due to the lack of mods for creating routes. It allows for very easy and intuitive creating, editing, and exporting of routes while also clearly visualizing them in the world. Many features were heavily inspired by [litematica](https://github.com/maruohon/litematica) and [tweakfork](https://github.com/Andrews54757/tweakfork)

### Requirements
* Minecraft Fabric 1.20.1 https://fabricmc.net/
* Fabric API [CurseForge](https://www.curseforge.com/minecraft/mc-mods/fabric-api) | [GitHub](https://github.com/FabricMC/fabric)
* Cloth Config [CurseForge](https://www.curseforge.com/minecraft/mc-mods/cloth-config) | [GitHub](https://github.com/shedaniel/cloth-config)
* noclip (recommended) [CurseForge](https://www.curseforge.com/minecraft/mc-mods/noclip) | [GitHub](https://github.com/andantet/noclip)

### Download
Download the latest release from the [releases page](https://github.com/Campionnn/Hollower/releases)

### Compiling
* Clone the repository
* Run `./gradlew build` in the root directory
* The compiled jar will be in `build/libs`

### Getting Started
* Install a Fabric 1.20.1 instance
* Put all the above mods into the mods folder
* Follow the instructions in https://github.com/Campionnn/CleanCH to get a world save of a clean Crystal Hollows
* Most route related features will only work while holding a wooden pickaxe
* Press C to open the config menu where many settings can be changed

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
* Selective Rendering:
    * Allows you to enable/disable the rendering certain blocks so you don't get distracted by unnecessary clutter
    * This feature can be very intensive on Minecraft especially at high render distances
    * You will likely experience some stutters while crossing chunk borders because it has to process the newly loaded chunks
    * Because the blocks are only hidden client side, you will run into ghost blocks. I have tried to implement a feature for no clip, but it is a lot more complicated than I initially thought. You can use this [noclip mod](https://www.curseforge.com/minecraft/mc-mods/noclip) until I do this myself

### Planned/Work in Progress
* Implementing no clip natively into the mod
* Save configs to a config file. Right now all settings get reset to the defaults when relaunching game (sorry)
* Menu to save/manage multiple routes so you can easily edit or copy them
* Visualizing reachable gemstones from each node
* Render a plane to visualize where the magma field starts (maybe not necessary)
* Automatically create very optimized routes using given parameters similar to [seafoam](https://astanik.dev/seafoam/) but better (probably far future)
* If you have any suggestions, feel free to open an issue or contact me on discord @campionn