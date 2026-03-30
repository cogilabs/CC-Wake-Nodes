# ComputerCraft Wake Nodes  

<p align="center">
    <a href="https://modrinth.com/mod/ccwake" target="_blank"><img alt="ModRinth badge" title="Get it on ModRinth!" src="https://img.shields.io/badge/-ModRinth-00AF5C?style=for-the-badge&logo=modrinth&logoColor=white"/></a>
    <a href="https://www.curseforge.com/minecraft/mc-mods/computercraft-wake-nodes" target="_blank"><img alt="CurseForge badge" title="Get it on CurseForge!" src="https://img.shields.io/badge/-CurseForge-F16436?style=for-the-badge&logo=curseforge&logoColor=white"/></a>
</p>

ComputerCraft Wake Nodes is a Forge mod for Minecraft 1.20.1 that adds a small chunk-loading network for CC: Tweaked (ComputerCraft) computers, particularly useful for automation setups.

It introduces two ComputerCraft peripherals:

- `wake_node`: attached to a ComputerCraft computer and registered under a string ID, effectively serving as the chunk loader.
- `wake_controller`: used by another computer to list, load, unload, and inspect registered nodes.

The main use case is waking distant computer setups long enough for their chunk to load and tick, allowing their startup logic to run.

It also adds the *Wake Chip*, a crafting component used by the other blocks.

If Create is installed, custom Create recipes are provided for the Wake Chip, Wake Node, and Wake Controller.

## Features

- Register remote wake points by attaching a *Wake Node* to a ComputerCraft computer.
- Load the remote computer's chunk on demand from a *Wake Controller*.
- Load chunks permanently until manual unload, or temporarily with an automatic timeout.
- Persist node registrations across server restarts.
- Keep chunk loading transient: loaded chunks are not restored automatically after a restart.
- Expose a simple ComputerCraft API for both blocks.

## Requirements

- Minecraft 1.20.1
- Forge 47.2.0+
- Java 17
- CC: Tweaked 1.108.4+

## Added Content

### Wake Chip

Crafting component used by the other blocks.

Recipe:

![Wake chip recipe](https://raw.githubusercontent.com/cogilabs/CC-Wake-Nodes/refs/heads/main/readmeSources/wake_chip_recipe.png)

Creative tab: Ingredients

### Wake Node

A thin peripheral block that must be placed directly against a CC: Tweaked computer block.

What it does:

- exposes the `wake_node` peripheral type
- stores a node ID assigned from Lua
- registers the attached computer's dimension and chunk in a persistent registry
- breaks automatically if its supporting computer is removed

Important placement rule:

- placement is refused unless the support block is a ComputerCraft computer block

Recipe:

![Wake Node recipe](https://raw.githubusercontent.com/cogilabs/CC-Wake-Nodes/refs/heads/main/readmeSources/wake_node_recipe.png)

Creative tab: Functional Blocks

### Wake Controller

A peripheral block used by a controller computer to manage registered wake nodes.

What it does:

- exposes the `wake_controller` peripheral type
- lists registered nodes
- loads and unloads node chunks
- reports whether nodes are currently loaded and how long temporary loads have left

Recipe:

![Wake Controller recipe](https://raw.githubusercontent.com/cogilabs/CC-Wake-Nodes/refs/heads/main/readmeSources/wake_controller_recipe.png)

Creative tab: Functional Blocks

## How It Works

1. Place a Wake Node on the face of a CC: Tweaked computer.
2. On that computer, call `setId("some_name")` on the `wake_node` peripheral to register the node.  
   The computer that performs this registration becomes the node owner.
3. Authorize controller computers with `grantController(computerId)`.
4. Place a Wake Controller next to another computer.
5. From the authorized controller computer, call `loadNode("some_name")` or `loadFor("some_name", seconds)`.

The loaded chunk is the computer's chunk, not the Wake Node block's chunk.

Each node has an owner computer and an access control list (ACL) of authorized controller computer IDs.  
Controller methods only operate on nodes the calling computer is authorized to manage.

Node registrations are saved, but active forced chunks are not. After a server restart, nodes still exist in the registry, but none of them remain loaded until requested again.

## ComputerCraft API

### Peripheral Types

- Wake Node peripheral type: `wake_node`
- Wake Controller peripheral type: `wake_controller`

### Wake Node API

Find the peripheral with standard ComputerCraft calls such as:

```lua
local wake = peripheral.find("wake_node")
```
  
Check [the API.md file](https://github.com/cogilabs/CC-Wake-Nodes/blob/main/API.md) on GitHub for a more compact version of the API documentation.

#### `setId(id)`

Registers this Wake Node under the given string ID.

- `id` must be a non-empty string
- on first successful registration, the calling computer becomes the node owner
- if the same ID is already registered on the same node, the call is accepted
- if the ID is already used by another node, the call raises an error

Example:

```lua
local wake = peripheral.find("wake_node")
assert(wake, "wake_node not found")

wake.setId("remote_factory")
```

#### `grantController(computerId)`

Authorizes a controller computer ID to manage this node.

- only the owner computer can call this method
- `computerId` must be a positive integer
- idempotent: granting the same ID twice is accepted

#### `revokeController(computerId)`

Removes a controller computer ID from this node's allowed list.

- only the owner computer can call this method
- idempotent: revoking an ID that is not present is accepted

#### `listControllers()`

Returns a sorted list of authorized controller computer IDs.

#### `getPermissions()`

Returns ownership and controller ACL for this node:

```lua
{
  owner = 17,
  controllers = { 42, 73 }
}
```

#### `getId()`

Returns the current node ID, or `nil` if none has been assigned.

Example:

```lua
print(wake.getId())
```

#### `getChunk()`

Returns a table describing the attached computer's chunk:

```lua
{
  dimension = "minecraft:overworld",
  chunk_x = 12,
  chunk_z = -4
}
```

#### `getInfo()`

Returns a table with the node ID and chunk information:

```lua
{
  id = "remote_factory",
  dimension = "minecraft:overworld",
  chunk_x = 12,
  chunk_z = -4
}
```

If no ID has been assigned yet, `id` is returned as an empty string.

### Wake Controller API

Find the peripheral with standard ComputerCraft calls such as:

```lua
local ctl = peripheral.find("wake_controller")
```

#### `listNodes()`

Returns a list of registered node IDs accessible by the calling controller computer.

Example:

```lua
for _, id in ipairs(ctl.listNodes()) do
  print(id)
end
```

#### `getNodeInfo(id)`

Returns node information for an accessible node.

Raises an error if the node does not exist or if the calling controller is not authorized.

Returned table:

```lua
{
  id = "remote_factory",
  dimension = "minecraft:overworld",
  chunk_x = 12,
  chunk_z = -4,
  loaded = false,
  expires_at = 90
}
```

Notes:

- `loaded` is a boolean
- `expires_at` is only present for temporary loads
- `expires_at` is reported as remaining seconds, not an absolute timestamp

#### `loadNode(id)`

Force-loads the node's chunk indefinitely, until one of these happens:

- `unloadNode(id)` is called
- `unloadAll()` is called
- the server stops

Raises an error if the node does not exist.

Also raises an error if the calling controller is not authorized for this node.

#### `loadFor(id, seconds)`

Force-loads the node's chunk for a limited number of seconds.

- `seconds` must be positive
- `seconds` must not exceed the configured `max_load_duration_seconds`

Raises an error if the node does not exist or the duration is invalid.

Also raises an error if the calling controller is not authorized for this node.

#### `unloadNode(id)`

Unloads the given node if it is currently loaded.

#### `unloadAll()`

Unloads every currently loaded node accessible by the calling controller.

#### `isNodeLoaded(id)`

Returns `true` if the node is currently loaded, otherwise `false`.

Raises an error if the calling controller is not authorized for this node.

#### `getLoadedNodes()`

Returns a list of currently loaded node IDs accessible by the calling controller.

## Example Setup

### Remote Computer

Attach a Wake Node to a computer and register it:

```lua
local wake = peripheral.find("wake_node")
assert(wake, "wake_node not found")

wake.setId("remote_factory")
wake.grantController(42) -- controller computer ID
print(textutils.serialize(wake.getInfo()))
```

### Controller Computer

Load the remote chunk for two minutes:

```lua
local ctl = peripheral.find("wake_controller")
assert(ctl, "wake_controller not found")

print("Known nodes:")
for _, id in ipairs(ctl.listNodes()) do
  print(" - " .. id)
end

ctl.loadFor("remote_factory", 120)
print(textutils.serialize(ctl.getNodeInfo("remote_factory")))
```

## Configuration

Server config keys:

- `chunk_loading.max_loaded_nodes`: maximum number of nodes that may be loaded at the same time. Default: `16`
- `chunk_loading.max_load_duration_seconds`: maximum duration accepted by `loadFor`. Default: `300`
- `chunk_loading.default_load_radius`: chunk radius around the target computer. `0` means only the computer's own chunk. Default: `0`
- `chunk_loading.chunk_ops_per_tick`: maximum number of queued load/unload operations processed each server tick. Lower values smooth spikes but wake nodes more slowly. Default: `3`

Common config keys:

- `logging.enable_node_logs`: enables server log messages for register, load, unload, and expiration events. Default: `true`

## Behavior Notes

- Registrations are shared through a world-level registry.
- Each node has an owner computer ID and an ACL of authorized controller computer IDs.
- Controller methods only operate on nodes the calling controller is authorized to manage.
- If a Wake Node is broken, its registration is removed.
- If the supporting computer is removed, the Wake Node breaks automatically.
- Temporary chunk loads expire on server tick and are cleaned up automatically.
- On server stop, all active chunk tickets are released.

## Sample Scripts

The repository includes example Lua scripts in [`sampleScripts/recallCreateTrains/`](https://github.com/cogilabs/CC-Wake-Nodes/tree/main/sampleScripts/recallCreateTrains) demonstrating how Wake Nodes can wake remote Create station computers and coordinate train recall flows over Rednet.

## License

Code is licensed under the MIT License.

Some bundled assets (textures) are licensed under the ComputerCraft Public License v1.0.0 (CCPL).

See the [LICENSE](https://github.com/cogilabs/CC-Wake-Nodes/blob/main/LICENSE) and [LICENSE-CCPL](https://github.com/cogilabs/CC-Wake-Nodes/blob/main/LICENSE-CCPL.md) files on GitHub for details.
