As a real use case, this script is used to recall trains from the [Create](https://www.curseforge.com/minecraft/mc-mods/create) mod.

To use it, you need a "depot" computer with a wake controller and a modem attached, and one or several "station" computers with wake nodes and modems attached. Ideally, the "station" computers should be attached to every station the train passes through.

The "depot" computer is the one used to recall the trains. On it, you will need [a schedule in TXT format](https://github.com/Creators-of-Create/Create/wiki/Train-Schedule-(ComputerCraft)), here called `recall_schedule.txt`. I preferred using a schedule instead of simply specifying the depot station(s), because it allows me to add CRN-specific instructions at the beginning. You will also need the script `depot_recall.lua`.

On every "station" computer, you will need `station_recall.lua`, and it absolutely needs to run on startup. To do this, create a `startup` file containing the following line:

`shell.run("station_recall")`

In `station_recall.lua`, set `CONTROLLER_PC_ID` to the ComputerCraft ID of your depot controller computer. The station script registers its wake node and grants that controller permission to manage it.

You will need to run `station_recall` at least once so the wake node can be registered.

To recall a train, run `depot_recall` with the train name as argument. For example, to recall the train named "test", run:

`depot_recall test`

If, like me, your train names are prefixed with a unique four-digit number, you can simply use that number as the argument. For example:

`depot_recall 1234`