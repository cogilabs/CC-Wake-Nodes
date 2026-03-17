local RECALL_PROTOCOL = "ccwake.recall"
local ACK_PROTOCOL = "ccwake.recall.ack"
local NODE_PREFIX = "RECALL_"

local SCHEDULE_FILE = "recall_schedule.txt"
local WAKE_SECONDS = 120
local BOOT_DELAY = 4

local function openAnyModem()
    local found = false
    peripheral.find("modem", function(name)
        if not rednet.isOpen(name) then
            rednet.open(name)
        end
        found = true
    end)
    return found
end

local function startsWith(s, prefix)
    return type(s) == "string" and s:sub(1, #prefix) == prefix
end

local function sanitize(name)
    name = name:lower()
    name = name:gsub("%*", "")
    name = name:gsub("[^%w%s%-_]", "")
    name = name:gsub("%s+", "_")
    name = name:gsub("_+", "_")
    name = name:gsub("^_", "")
    name = name:gsub("_$", "")
    return name
end

local function loadSchedule(path)
    if not fs.exists(path) then
        error("File not found: " .. path)
    end

    local f = fs.open(path, "r")
    local data = f.readAll()
    f.close()

    local schedule = textutils.unserialize(data)
    if type(schedule) ~= "table" then
        error("Invalid schedule in " .. path)
    end

    return schedule
end

local function getDestination(schedule)
    local dest = "?"
    pcall(function()
        for _, entry in ipairs(schedule.entries or {}) do
            if entry.instruction
                and entry.instruction.id == "create:destination"
                and entry.instruction.data
                and entry.instruction.data.text then
                dest = entry.instruction.data.text
                return
            end
        end
    end)
    return dest
end

local function getRecallNodes(ctl)
    local nodes = ctl.listNodes()
    local result = {}

    for _, id in ipairs(nodes or {}) do
        if startsWith(id, NODE_PREFIX) then
            table.insert(result, id)
        end
    end

    table.sort(result)
    return result
end

local function contains(tbl, value)
    for _, v in ipairs(tbl) do
        if v == value then
            return true
        end
    end
    return false
end

local function getSelfNodeId()
    local wake = peripheral.find("wake_node")
    if not wake then
        return nil
    end

    local id = NODE_PREFIX .. "depot_" .. tostring(os.getComputerID())
    wake.setId(id)
    return id
end

local function usage()
    print("Usage:")
    print('depot_recall "<train or id>"')
end

local function broadcastCancel(trainQuery)
    rednet.broadcast({
        action = "cancel",
        train = trainQuery
    }, RECALL_PROTOCOL)
end

local args = { ... }

if #args < 1 then
    usage()
    return
end

local trainQuery = table.concat(args, " ")

local ctl = peripheral.find("wake_controller")
if not ctl then
    error("wake_controller not found")
end

if not openAnyModem() then
    error("modem not found")
end

local schedule = loadSchedule(SCHEDULE_FILE)
local destination = getDestination(schedule)
local targetNodes = getRecallNodes(ctl)

local selfNodeId = getSelfNodeId()
if selfNodeId and not contains(targetNodes, selfNodeId) then
    table.insert(targetNodes, 1, selfNodeId)
end

print("Train to recall: " .. trainQuery)
print("Destination: " .. destination)
print("")

if #targetNodes == 0 then
    print("No nodes " .. NODE_PREFIX .. "* found.")
    return
end

print("Target nodes:")
for _, nodeId in ipairs(targetNodes) do
    print(" - " .. nodeId)
end

print("")
for _, nodeId in ipairs(targetNodes) do
    local ok, resultOrErr = pcall(function()
        return ctl.loadFor(nodeId, WAKE_SECONDS)
    end)

    if ok then
        print("[WAKE] " .. nodeId)
    else
        print("[ERR ] wake " .. nodeId .. ": " .. tostring(resultOrErr))
    end
end

print("")
print("Waiting for nodes to boot (" .. BOOT_DELAY .. "s)...")
sleep(BOOT_DELAY)

local msg = {
    action = "recall",
    train = trainQuery,
    destination = destination,
    schedule = schedule
}

rednet.broadcast(msg, RECALL_PROTOCOL)
print("Recall broadcast sent.")
print("")

local activeAckWindow = WAKE_SECONDS - BOOT_DELAY
if activeAckWindow < 1 then
    activeAckWindow = 1
end

local deadline = os.clock() + activeAckWindow
local gotApplied = false

while os.clock() < deadline do
    local timeout = math.max(0, deadline - os.clock())
    local senderId, reply, protocol = rednet.receive(ACK_PROTOCOL, timeout)

    if senderId and type(reply) == "table" then
        if reply.action == "ack_received" then
            print(("[ACK ] received by %s (%s) for %s"):format(
                tostring(reply.station), tostring(reply.node), tostring(reply.query)
            ))

        elseif reply.action == "ack_applied" then
            print(("[DONE] %s rerouted %s to %s"):format(
                tostring(reply.station), tostring(reply.train), tostring(reply.destination)
            ))

            if not gotApplied then
                gotApplied = true
                broadcastCancel(trainQuery)
                print("[CANCEL] recall cancelled on all stations")
            end

        elseif reply.action == "ack_error" then
            print(("[ERR ] %s on %s : %s"):format(
                tostring(reply.station), tostring(reply.train), tostring(reply.error)
            ))
        end
    else
        break
    end
end

print("")
broadcastCancel(trainQuery)
if gotApplied then
    print("End of ACK window, final cancel sent.")
else
    print("End of ACK window, cleanup cancel sent.")
end