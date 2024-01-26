# FTB Team Bases

**Note: the mod is currently in early pre-alpha. Many features are not yet implemented.**

## Overview

FTB Team Bases is a replacement in MC 1.20.1 for earlier base management mods, including FTB Team Dimensions, FTB Team Islands and FTB Dungeons. It will ultimately include functionality from all of those. Planned functionality includes:

* Creating bases in a static shared dimension (like FTB Team Islands; dimension ID is `ftbteambases:bases` by default)
* Creating bases in a private per-team dimension (like FTB Team Dimensions)
* Creating bases from pregenerated region files, using off-thread copying and relocating of template MCA files into the target dimension (suitable for huge bases, spanning 1 or more entire 512x512-block regions)
* Creating bases by putting down a Jigsaw block and running jigsaw generation over several ticks (like FTB Dungeons - suitable for large dynamically generated bases)
* Creating bases by pasting a single NBT structure (suitable for small bases)

The mod is driven by _base definition_ files, which are JSON files in a custom datapack (`<mod-id>/ftbteambases/*.json`). Each definition file is a template from which a live team base is created. Every team base has a 1:1 association with an FTB Teams team; when FTB Team Bases is loaded, teams can _only_ be created by creating a base, and when a team is disbanded, players will be ejected from that base, and the base archived.

Three default base definition files are shipped with the mod, although none will be displayed as GUI options when creating a base, unless you're running the mod in a development instance (from an IDE), or you've set `show_dev_mode` to **true** in the client config.

## Getting Started

### Setting up a Base Definition

To set up a region-relocating base (the only type properly supported at current v0.0.1 code):

Create a base definition JSON file, using this as a start point:

```json
{
  "id": "ftbteambases:my_base_type",
  "description": "Some description text",
  "preview_image": "ftbteambases:textures/spawn/default.png",
  "construction": {
    "pregen_template": "my_template"
  },
  "dimension": {
    "private": false
  }
}
```

This file goes in a datapack in `ftbteambases/ftb_base_definitions/my_base_type.json`

Several fields will need to be customised:
* `id` is a unique template ID. It should match the filename you're using.
* `description` is free-form text which should be kept short
* `preview_image` is a resource location for a texture to display in the selection GUI. Typically, you'd use a screenshot of the base.
* `pregen_template` defines where the template MCA files which will be copied/relocated can be found. More on this below.
* `private` should stay as false, meaning the base will be copied into the shared dimension (`ftbteambases:bases` by default). Private dynamically generated dimensions aren't properly supported yet.

Now you need to put your template MCA files in a directory within your Minecraft server instance. In the above example (looking at `pregen_template`), your MCA files go in `<instance>/ftbteambases/pregen/my_template/`. You can put multiple MCA files in here if you want to, although they should ideally be contiguous. Example of a 4-region template:

```
$ ls ftbteambases/pregen/my_template/
r.0.0.mca  r.0.1.mca  r.1.0.mca  r.1.1.mca
```

**Note:** only region MCA files are copied and relocated right now. Entity and POI MCA files are not supported yet, but will be in the future.

### Setting up the Overworld Lobby

Like FTB Team Dimensions, this mod creates a __lobby__ where new players are sent to (or can return to with `/ftbteambases lobby`). This is created in the overworld the first time it's loaded (i.e. when a new server is started).

By default, the lobby structure NBT is `ftbteambases:lobby`, a resource location for a datapack NBT structure. You can override the default `ftbteambases/structures/lobby.nbt` file via datapack, or change the location in server config with the `lobby_structure_location` config setting. The lobby structure must include a Structure Block in data mode, with the metadata string `spawn_point`. This block will be used as the player spawn point on initial connection (and will be replaced with air when the structure is placed into the overworld).

This is fine for small lobbies, but if you want a very large complex lobby, it may be advisable to do a pregenerated region instead. In this case, put your pregenerated MCA file into `ftbteambases/pregen_initial/region/`. If you do this, you must also change the `lobby_spawn` config setting so that the player is spawned in the right place in the lobby on initial join.

## Creating a Base

There are two ways to do this:

1. Use the `/ftbteambases create <base-definition>` command. This needs admin privileges, so regular players would likely run this via a command block. The base definition is the JSON file you created above, e.g. `/ftbteambases create ftbteambases:my_base_type`
2. Set up a portal structure, using the `ftbteambases:portal` block. This block can only be obtained with the `/give` command, but can be used to build a portal structure in your lobby (although it looks like a Nether portal block, it doesn't need obsidian around it - feel free to create any shape and surrounding you like). When a player walks into this portal block, they are either teleported to their base (if they have one), or presented with a selection GUI to create a base, based on the currently-existing base definitions.

Note that base creation can take a few seconds, especially if there are multiple region files to copied/relocated from the templates to the live dimension. Players get a progress indicator while their base is being prepared (preparation should not cause any server lag).

* When a base is created, a team is automatically created for the player.
* If a team is disbanded, the base is archived, and any players in the base are sent back to the lobby. (In the future, commands will be added to either restore or purge archived bases)
* If a player joins an existing team, they are automatically sent to the team's base. If they leave the team, they are sent back to the lobby.

## Commands

* `/ftbteambases create <base-definition>` - see above
* `/ftbteambases home` - go to your team base spawn point
* `/ftbteambases lobby` - go back to the lobby
* `/ftbteambases list` - list all known team bases. The `[Show]` and `[Visit]` links in the resulting text can be clicked to show base details, or teleport to the base (admin privileges required)
* `/ftbteambases show <base-id>` - show base details. `<base-id>` is in fact an existing FTB Team shortname
* `/ftbteambases visit <base-id>` - teleport to a base spawn. `<base-id>` is in fact an existing FTB Team shortname

## Config Files

* `config/ftbteambases-server.snbt` - server side config file
* `config/ftbteambases-client.snbt` - client side config file