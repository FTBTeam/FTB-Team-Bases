# FTB Team Bases

## Overview

FTB Team Bases is a replacement in MC 1.20.1 for earlier base management mods, including FTB Team Dimensions, FTB Team Islands and FTB Dungeons, and includes functionality from all of those:

* Creating bases in a static shared dimension (like FTB Team Islands; dimension ID is `ftbteambases:bases` by default)
* Creating bases in a private per-team dimension (like FTB Team Dimensions)
* Creating bases from pregenerated region files, using off-thread copying and relocating of template MCA files into the target dimension (suitable for huge bases, spanning 1 or more entire 512x512-block regions). 
  * Note: this is only suitable for void dimensions, or non-void dimensions with a fixed seed, so the pregenerated regions fit properly with existing worldgen.
* Creating bases by putting down a Jigsaw block and running jigsaw generation over several ticks (like FTB Dungeons - suitable for large dynamically generated bases)
* Creating bases by pasting a single NBT structure (suitable for small bases)

The mod is driven by _base definition_ files, which are JSON files in a custom datapack (`<mod-id>/ftb_base_definitions/*.json`). Each definition file is a template from which live team bases are created. Every live team base has a 1:1 association with an FTB Teams team; when FTB Team Bases is loaded, teams can _only_ be created by creating a base, and when a team is disbanded, players will be ejected from that base, and the base will be archived.

Four default base definition files are shipped with the mod, although none of these will be displayed as GUI options when creating a base, unless you're running the mod in a development instance (from an IDE), or you've set `show_dev_mode` to **true** in the client config.

## Getting Started

### Setting up a Base Definition

To set up a pregenerated base with region-relocation into the default shared dimension, use this as a start point:

```json5
{
  "id": "ftbteambases:my_base_type",
  "description": "Some description text",
  "preview_image": "ftbteambases:textures/spawn/default.png",
  "construction": {
    "pregen_template": "my_template",
    "structure_sets": [ ]
  },
  "dimension": {
    "private": false
  }
}
```

To set up a pregenerated base in a private dimension, with no region relocation, just change the `dimension` section above:

```json5
{
  //...
  "dimension": {
    "private": true,
    "dimension_type": "ftbteambases:default"
  }
}
```

To set up a single-structure base in the shared dimension:

```json5
{
  "id": "ftbteambases:my_base_type",
  "description": "Some description text",
  "preview_image": "ftbteambases:textures/spawn/default.png",
  "construction": {
    "structure_location": "minecraft:village/plains/houses/plains_medium_house_1",
    "y_pos": 64,
    "include_entities": false
  },
  "spawn_offset": [ 0, -6, 0 ],
  "dimension": {
    "private": false
  }
}
```

This file goes in a datapack in `ftbteambases/ftb_base_definitions/my_base_type.json` (the filename matches the `id` field in the JSON).

Several fields will need to be customised:
* `id` is a unique template ID. It should match the filename you're using.
* `description` is free-form text which should be kept short
* `preview_image` is a resource location for a texture to display in the selection GUI. Typically, you'd use a screenshot of the base.
* `private` determines whether the base will be created in the shared dimension (`ftbteambases:bases` by default), or if a new dynamic dimension should be created for the base.
  * When `private` is true, the `dimension_type` field specifies the [dimension type](https://minecraft.fandom.com/wiki/Custom_dimension#Dimension_type) for the new dimension. `ftbteambases:default` works in many cases, but you're free to use a custom type here.
  * When `private` is false, the `dimension_id` field specifies the dimension which will be used. By default, this `ftbteambases:bases` (a void-world datapack dimension supplied by this mod); you could change this to e.g. `minecraft:overworld` here
* `spawn_offset` is an offset for the default player spawn position; defaults to [0,0,0] if omitted.
  * The default position is typically at the center of the region(s), and at a Y position of the world's surface.
  * In the third example above, the player would spawn on top of the village house roof if this offset were not specified. 

Fields in the `construction` section depend on the type of base you're constructing.

For pregenerated bases:
* `pregen_template` defines where the template MCA files which will be copied/relocated can be found, for pregenerated bases. More on this below.
* `structure_sets` is a list of structure set ID's defining which structures may be added to the pregenerated terrain

For single-structure bases:
* `structure_location` defines the single NBT structure which will be pasted into the world.
* `y_pos` determines the Y position at which the structure will be pasted (the X/Z position is at the center of the region)
  * If omitted, the structure will be pasted at the world surface; for void dimensions, it's recommended to include this setting.
* `include_entities` controls whether any entities saved in the NBT structure will be added to the world; defaults to false.

#### Pregen Templates
Now you need to put your template MCA files in a directory within your top-level Minecraft server instance: `<instance>/ftbteambases/pregen/<template-name>/`, with `region/`, `entity/` and `poi/` subdirectories. Region MCA files *must* be present, but entity and POI files are optional.

In the above example (looking at the `pregen_template` field), your region MCA files go in `<instance>/ftbteambases/pregen/my_template/region/`. You can put multiple MCA files in here if you want to; it's recommended (but not required) that they are contiguous regions. Example of a 4-region template:

```
$ ls ftbteambases/pregen/my_template/region
r.0.0.mca  r.0.1.mca  r.1.0.mca  r.1.1.mca
```

**Note:** only region MCA files are fully supported right now. Entity and POI MCA files (`entity/` and `/poi` subdirectories) are copied for private dimensions, but not supported for relocation in the shared dimension. 

These files are copied directly into the dimension folder for private (non-relocated) dimensions, but processed and renamed when relocated into a shared dimension. For this reason, creating a pregen base in a private dimension is much faster than creating one in a shared dimension.

### Setting up the Overworld Lobby

Like FTB Team Dimensions, this mod creates a __lobby__ where new players are sent to (or can return to with `/ftbteambases lobby`). This is created in the overworld the first time the world is loaded (i.e. when a new server is started).

By default, the lobby structure NBT is `ftbteambases:lobby`, a resource location for a datapack NBT structure. You can override the default `ftbteambases/structures/lobby.nbt` file via datapack, or change the location in server config with the `lobby_structure_location` config setting. The lobby structure must include a Structure Block in data mode, with the metadata string `spawn_point`. This block will be used as the player spawn point on initial connection (and will be replaced with air when the structure is placed into the overworld).

This is fine for small lobbies, but if you want a very large complex lobby, it may be advisable to do a pregenerated region instead. In this case, put your pregenerated MCA file into `ftbteambases/pregen_initial/region/`. If you do this, you must also change the `lobby_spawn` [config](#config-files) setting so that the player is spawned in the right place in the lobby on initial join.

#### Custom Dimension for the Lobby

By default, the lobby structure is spawned in the overworld, and new players are sent there when they first log in to a server (or create a new SSP world). It might be desirable to have a custom dimension for your lobby, e.g. if your modpack has visiting the Overworld as a later-on goal. This is supported, but a little care is needed:

1. You will need a custom dimension for your lobby, and it must be a static dimension, defined via datapack. FTB Team Bases provides such a dimension: `ftbteambases:lobby`. This is a void dimension made of nothing but air, into which the lobby structure is pasted. If you want to use an alternative dimension, you must provide one via datapack. It is also possible to use the Nether or the End here, although this will take extra work to preserve game progression...
2. The lobby dimension is defined in server config with the `lobby_dimension` setting in the `lobby` section. However, since this needs to be set _before_ the server is started (and levels are initially created), it must be defined in the mod's default config, and this default config must be provided with the modpack. Edit `defaultconfigs/ftbteambases/ftbteambases-server.snbt`, and add the following (possibly combining with any other default config settings you want to add here):

```
# Default config file that will be copied to saves/New World/serverconfig/ftbteambases-server.snbt if it doesn't exist!
# Just copy any values you wish to override in here!

{
  lobby: {
    lobby_dimension: "ftbteambases:lobby"
  }
}
```

3. Once the world is created, you should _not_ modify this config setting, or it may lead to players being teleported into the wrong dimension, and likely death by suffocation!

When players first log in to the server (or create an SSP world), they will be immediately teleported to the lobby structure in the target dimension. FTB Team Bases doesn't provide a method to return to the overworld, so that will need to be arranged separately (e.g. a command block, or some modded solution).

## Creating a Base

There are two ways to do this:

1. Use the `/ftbteambases create <base-definition>` command. This needs admin privileges, so regular players would likely run this via a command block. The base definition is the JSON file you created above, e.g. `/ftbteambases create ftbteambases:my_base_type`
2. Set up a portal structure, using the `ftbteambases:portal` block. This block can only be obtained with the `/give` command, but can be used to build a portal structure in your lobby (although it looks like a Nether portal block, it doesn't need obsidian around it - feel free to create any shape and surrounding you like). When a player walks into this portal block, they are either teleported to their base (if they have one), or presented with a selection GUI to create a base, based on the currently-existing base definitions.

Note that base creation can take a few seconds, especially if there are multiple region files to be copied/relocated from the templates to the live dimension. Players get a progress indicator while their base is being prepared (preparation should not cause any noticeable server lag).

* When a base is created, an FTB Teams team is automatically created for the player.
* If a team is disbanded, the base is archived, and any players in the base are sent back to the lobby.
* If a player joins an existing team, they are automatically sent to the team's base. If they leave the team, they are sent back to the lobby.

## Commands

### Players
* `/ftbteambases home` - teleport to your team base spawn point
* `/ftbteambases lobby` - teleport back to the lobby

### Admins
* `/ftbteambases create <base-definition>` - create a base - see [above](#creating-a-base)
* `/ftbteambases list` - list all known team bases. 
  * The **[Show]** and **[Visit]** "buttons" in the resulting text can be clicked to show base details, or teleport to the base (admin privileges required)
* `/ftbteambases show <base-id>` - show base details. `<base-id>` is in fact an existing FTB Teams shortname
* `/ftbteambases visit <base-id>` - teleport to a base spawn. `<base-id>` is in fact an existing FTB Teams shortname
* `/ftbteambases visit` - open a GUI showing all live bases and optionally all archived bases with some performance info, and the option to visit
* `/ftbteambases nether-visit` - go to the Nether at the point a Nether portal for this team would take you
* `/ftbteambases archive list` - show all archived bases.
* `/ftbteambases archive restore <name>` - restore the named archive
  * This can also be done by clicking the **[Restore]** "button" beside the base in `archive list` output
* `/ftbteambases purge id <archive-id>` - schedule an archived base for permanent deletion; `<archive_id>` can be found from the `archive list` output
  * This can also be done by clicking the **[Purge]** "button" beside the base in `archive list` output
* `/ftbteambases purge older <days>` - schedule all archived bases which were archived at least `<days>` days ago for deletion
* `/ftbteambases purge cancel_all` - unschedule all pending base purges
* `/ftbteambases purge cancel <archive-id>` - unschedule a specific pending base purge

Bases scheduled for purge will be _permanently_ deleted on the next server restart. A separate server backup system is recommended if you use this!

## Config Files

* `config/ftbteambases-server.snbt` - server side config file
* `config/ftbteambases-client.snbt` - client side config file

## Support

- For **Modpack** issues, please go here: https://go.ftb.team/support-modpack
- For **Mod** issues, please go here: https://go.ftb.team/support-mod-issues
- Just got a question? Check out our Discord: https://go.ftb.team/discord

## Licence

All Rights Reserved to Feed The Beast Ltd. Source code is `visible source`, please see our [LICENSE.md](/LICENSE.md) for more information. Any Pull Requests made to this mod must have the CLA (Contributor Licence Agreement) signed and agreed to before the request will be considered.

## Keep up to date

[![](https://cdn.feed-the-beast.com/assets/socials/icons/social-discord.webp)](https://go.ftb.team/discord) [![](https://cdn.feed-the-beast.com/assets/socials/icons/social-github.webp)](https://go.ftb.team/github) [![](https://cdn.feed-the-beast.com/assets/socials/icons/social-twitter-x.webp)](https://go.ftb.team/twitter) [![](https://cdn.feed-the-beast.com/assets/socials/icons/social-youtube.webp)](https://go.ftb.team/youtube) [![](https://cdn.feed-the-beast.com/assets/socials/icons/social-twitch.webp)](https://go.ftb.team/twitch) [![](https://cdn.feed-the-beast.com/assets/socials/icons/social-instagram.webp)](https://go.ftb.team/instagram) [![](https://cdn.feed-the-beast.com/assets/socials/icons/social-facebook.webp)](https://go.ftb.team/facebook) [![](https://cdn.feed-the-beast.com/assets/socials/icons/social-tiktok.webp)](https://go.ftb.team/tiktok)
