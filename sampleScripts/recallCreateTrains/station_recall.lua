local RECALL_PROTOCOL = "ccwake.recall"
local ACK_PROTOCOL = "ccwake.recall.ack"
local NODE_PREFIX = "RECALL_"
local CONTROLLER_PC_ID = 42

local recalls = {}

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

local function trim(s)
    return (s:gsub("^%s+", ""):gsub("%s+$", ""))
end

local function getTrainId(trainName)
    return trainName:match("^(%d+)%s*%-")
end

local function trainMatches(query, trainName)
    if type(query) ~= "string" or type(trainName) ~= "string" then
        return false
    end

    query = trim(query)
    trainName = trim(trainName)

    if query == trainName then
        return true
    end

    local id = getTrainId(trainName)
    if id and query == id then
        return true
    end

    return false
end

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

local wake = peripheral.find("wake_node")
local station = peripheral.find("Create_Station")

if not wake then
    error("wake_node not found")
end

if not station then
    error("Create_Station not found")
end

if not openAnyModem() then
    error("modem not found")
end

local stationName = station.getStationName()
local nodeId = NODE_PREFIX .. sanitize(stationName)

wake.setId(nodeId)
wake.grantController(CONTROLLER_PC_ID)

print("Wake node id: " .. nodeId)
print("Authorized controller: " .. tostring(CONTROLLER_PC_ID))
print("Station: " .. stationName)

local function handleRecall(senderId, msg)
    if type(msg) ~= "table" then
        return
    end

    if msg.action ~= "recall" then
        return
    end

    if type(msg.train) ~= "string" then
        return
    end

    if type(msg.schedule) ~= "table" then
        return
    end

    recalls[msg.train] = {
        schedule = msg.schedule,
        destination = msg.destination or "?",
        requestedBy = senderId
    }

    print("Recall received for: " .. msg.train)

    rednet.send(senderId, {
        action = "ack_received",
        station = stationName,
        node = nodeId,
        query = msg.train
    }, ACK_PROTOCOL)
end

local function handleCancel(msg)
    if type(msg) ~= "table" then
        return
    end

    if msg.action ~= "cancel" then
        return
    end

    if type(msg.train) ~= "string" then
        return
    end

    if recalls[msg.train] then
        recalls[msg.train] = nil
        print("Recall cancelled for: " .. msg.train)
    end
end

local function networkLoop()
    while true do
        local senderId, msg, protocol = rednet.receive()

        if protocol == RECALL_PROTOCOL and type(msg) == "table" then
            if msg.action == "recall" then
                handleRecall(senderId, msg)
            elseif msg.action == "cancel" then
                handleCancel(msg)
            end
        end
    end
end

local function stationLoop()
    local lastTrainName = nil
    local handledCurrentPresence = false

    while true do
        if station.isTrainPresent() then
            local trainName = station.getTrainName()

            if trainName ~= lastTrainName then
                lastTrainName = trainName
                handledCurrentPresence = false
                print("Train present: " .. trainName)
            end

            if not handledCurrentPresence then
                for query, info in pairs(recalls) do
                    if trainMatches(query, trainName) then
                        print("Recall match: " .. trainName .. " <- " .. query)

                        local ok, err = pcall(function()
                            station.setSchedule(info.schedule)
                        end)

                        if ok then
                            print("Schedule injected to " .. tostring(info.destination))

                            rednet.send(info.requestedBy, {
                                action = "ack_applied",
                                station = stationName,
                                node = nodeId,
                                query = query,
                                train = trainName,
                                destination = info.destination
                            }, ACK_PROTOCOL)

                            recalls[query] = nil
                            handledCurrentPresence = true
                        else
                            print("setSchedule error: " .. tostring(err))

                            rednet.send(info.requestedBy, {
                                action = "ack_error",
                                station = stationName,
                                node = nodeId,
                                query = query,
                                train = trainName,
                                error = tostring(err)
                            }, ACK_PROTOCOL)
                        end

                        break
                    end
                end
            end
        else
            lastTrainName = nil
            handledCurrentPresence = false
        end

        sleep(0.5)
    end
end

parallel.waitForAny(networkLoop, stationLoop)