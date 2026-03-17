ComputerCraft Wake Nodes - API Reference

Peripheral types
- `wake_node` — peripheral attached to a ComputerCraft computer.
- `wake_controller` — peripheral used by a controller computer.

Wake Node API

- `setId(id)`
  - Parameters: `id` (non-empty string)
  - Effect: Registers this Wake Node under the given ID in the world registry.
  - Errors: raises if the ID is already used by another node.
  - Example: `wake.setId("remote_factory")`

- `getId()`
  - Returns: the current `id` (string) or `nil` if none assigned.
  - Example: `print(wake.getId())`

- `getChunk()`
  - Returns: a table `{ dimension = "minecraft:...", chunk_x = X, chunk_z = Z }` describing the attached computer's chunk.
  - Example: `local c = wake.getChunk()`

- `getInfo()`
  - Returns: a table `{ id = "...", dimension = "...", chunk_x = X, chunk_z = Z }`.
  - If no ID assigned, `id` is an empty string.

- `grantController(computerId)`
  - Parameters: `computerId` (positive integer)
  - Effect: authorizes a controller computer ID to manage this node.
  - Requirements: only the node owner may call this method.
  - Example: `wake.grantController(42)`

- `revokeController(computerId)`
  - Parameters: `computerId` (positive integer)
  - Effect: removes a controller computer ID from the node's ACL.
  - Requirements: only the node owner may call this method.

- `listControllers()`
  - Returns: an array of authorized controller computer IDs for this node.

- `getPermissions()`
  - Returns: a table describing ownership and controllers for the node, e.g.:
    `{ owner = 17, controllers = { 42, 73 } }`.

Notes on ownership:

- On first successful `setId(...)` call the calling computer becomes the node owner.
- Nodes created by earlier versions without an owner remain accessible until claimed; the first owner-sensitive call will claim such a legacy node.

Wake Controller API

- `listNodes()`
  - Returns: an array of registered node IDs accessible by the calling controller computer.
  - Example: `for _, id in ipairs(ctl.listNodes()) do print(id) end`

- `getNodeInfo(id)`
  - Parameters: `id` (string)
  - Returns: a table describing the node if it exists and the calling controller is authorized; otherwise raises an error.
  - Returned table:
    `{ id = "...", dimension = "...", chunk_x = X, chunk_z = Z, loaded = BOOL, expires_at = N }`
    - `loaded`: boolean whether the chunk is currently forced loaded.
    - `expires_at`: remaining seconds for a temporary load (present only for temporary loads).

- `loadNode(id)`
  - Parameters: `id` (string)
  - Effect: force-loads the node's chunk indefinitely (until `unloadNode`, `unloadAll`, or server stop).
  - Errors: raises if the node does not exist or if the calling controller is not authorized for this node.

- `loadFor(id, seconds)`
  - Parameters: `id` (string), `seconds` (number > 0, ≤ configured `max_load_duration_seconds`)
  - Effect: force-loads the node's chunk for a limited time.
  - Errors: raises if the node does not exist, duration is invalid, or the calling controller is not authorized.

- `unloadNode(id)`
  - Parameters: `id` (string)
  - Effect: releases the chunk ticket if the node is currently loaded.
  - Errors: raises if the node does not exist or the calling controller is not authorized.

- `unloadAll()`
  - Effect: unloads every currently loaded node accessible by the calling controller.

- `isNodeLoaded(id)`
  - Parameters: `id` (string)
  - Returns: `true` if the node is loaded, otherwise `false`.
  - Errors: raises if the calling controller is not authorized for this node.

- `getLoadedNodes()`
  - Returns: an array of IDs for currently loaded nodes accessible by the calling controller.

Quick notes
- Node registrations are persisted across world restarts, but forced chunk tickets are not.
- If a Wake Node is broken (or its supporting computer removed), its registration is removed.
- Load durations and limits are controlled by mod configuration (e.g. `max_loaded_nodes`, `max_load_duration_seconds`).

Security notes
- Each node has an owner computer ID and an ACL of authorized controller computer IDs.
- Controller-side methods (`loadNode`, `loadFor`, `unloadNode`, `getNodeInfo`, `isNodeLoaded`) require the calling controller's computer ID to be either the owner or present in the node's controller list.
- `listNodes()` and `getLoadedNodes()` only return nodes the calling controller may access.

Short examples
- Register a node:
  ```lua
  local wake = peripheral.find("wake_node")
  wake.setId("remote_factory")
  ```

- Temporarily load a node from a controller:
  ```lua
  local ctl = peripheral.find("wake_controller")
  ctl.loadFor("remote_factory", 120)
  ```
