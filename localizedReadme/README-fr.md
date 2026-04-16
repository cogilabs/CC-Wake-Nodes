# ComputerCraft Wake Nodes

<p align="center"><img alt="Banner" title="Banner" src="https://raw.githubusercontent.com/cogilabs/CC-Wake-Nodes/refs/heads/main/readmeSources/banner.png?raw=true"></p>

<p align="center">
		<a href="https://modrinth.com/mod/ccwake" target="_blank"><img alt="ModRinth badge" title="Get it on ModRinth!" src="https://img.shields.io/badge/-ModRinth-00AF5C?style=for-the-badge&logo=modrinth&logoColor=white"/></a>
		<a href="https://www.curseforge.com/minecraft/mc-mods/computercraft-wake-nodes" target="_blank"><img alt="CurseForge badge" title="Get it on CurseForge!" src="https://img.shields.io/badge/-CurseForge-F16436?style=for-the-badge&logo=curseforge&logoColor=white"/></a>
</p>

[→ In English](/README.md)  

## Table des matières

 - [Introduction](#introduction)
 - [Fonctionnalités](#fonctionnalites)
 - [Prérequis](#prerequis)
 - [Contenu ajouté](#contenu-ajoute)
 - [Comment ça marche](#comment-ca-marche)
 - [API ComputerCraft](#api-computercraft)
 - [Exemple de configuration](#exemple-de-configuration)
 - [Configuration](#configuration)
 - [Remarques](#remarques)
 - [Exemples de scripts](#exemples-de-scripts)
 - [Licence](#licence)

## Introduction

ComputerCraft Wake Nodes est un mod Forge pour Minecraft 1.20.1 qui ajoute un petit réseau de chargement de chunks pour les ordinateurs CC: Tweaked (ComputerCraft), particulièrement utile pour des montages d'automatisation.

Il introduit trois périphériques ComputerCraft :

- `wake_node` : attaché à un ordinateur ComputerCraft et enregistré sous un ID de chaîne, servant essentiellement de chargeur de chunk.
- `wake_node_advanced` : une version améliorée de `wake_node` qui permet de choisir la zone chargée (1×1, 3×3 ou 5×5 chunks).
- `wake_controller` : utilisé par un autre ordinateur pour lister, charger, décharger et inspecter les noeuds enregistrés.

Le cas d'utilisation principal est de réveiller des installations d'ordinateurs distants assez longtemps pour que leur chunk se charge et s'exécute (tick), permettant à leur logique de démarrage de s'exécuter.

Il ajoute également le Wake Chip, un composant utilisé pour crafter les autres blocs.

Si Create est installé, des recettes Create personnalisées sont fournies pour le Wake Chip, la Wake Node, l'Advanced Wake Node et le Wake Controller.

## Fonctionnalités

- Enregistrer des points de réveil à distance en attachant une Wake Node à un ordinateur ComputerCraft.
- Charger le chunk de l'ordinateur distant à la demande depuis un Wake Controller.
- Charger des chunks de manière permanente jusqu'à déchargement manuel, ou temporairement avec un délai automatique.
- Persister les enregistrements de noeuds entre les redémarrages du serveur.
- Garder le chargement de chunk transitoire : les chunks chargés ne sont pas restaurés automatiquement après un redémarrage.
- Exposer une API ComputerCraft simple pour les deux blocs.

## Prérequis

- Minecraft 1.20.1
- Forge 47.2.0+
- Java 17
- CC: Tweaked 1.108.4+

## Contenu ajouté

### Wake Chip

Composant d'artisanat utilisé par les autres blocs.

Recette :

![Recette Wake chip](https://raw.githubusercontent.com/cogilabs/CC-Wake-Nodes/refs/heads/main/readmeSources/wake_chip_recipe.png)

Onglet Créatif : Ingredients

### Wake Node

Un bloc périphérique fin qui doit être placé directement contre un bloc ordinateur CC: Tweaked.

Ce qu'il fait :

- expose le type de périphérique `wake_node`
- stocke un ID de noeud assigné depuis Lua
- enregistre la dimension et le chunk de l'ordinateur attaché dans un registre persistant
- se casse automatiquement si l'ordinateur support est retiré

Règle de placement importante :

- le placement est refusé sauf si le bloc support est un bloc ordinateur ComputerCraft

Recette :

![Recette Wake Node](https://raw.githubusercontent.com/cogilabs/CC-Wake-Nodes/refs/heads/main/readmeSources/wake_node_recipe.png)

Onglet Créatif : Functional Blocks

### Advanced Wake Node

Une Wake Node améliorée qui permet de choisir la zone chargée autour de l'ordinateur attaché :

- **1×1** — 1 chunk (identique à une Wake Node basique)
- **3×3** — 9 chunks
- **5×5** — 25 chunks

Elle hérite de toutes les fonctionnalités de la Wake Node basique et ajoute les méthodes `setRange()`, `getRange()` et `listAvailableRanges()`.

Recette (même forme que la Wake Node, mais avec de la netherite au lieu du fer) :

![Recette Advanced Wake Node](https://raw.githubusercontent.com/cogilabs/CC-Wake-Nodes/refs/heads/main/readmeSources/wake_node_advanced_recipe.png)

Onglet Créatif : Functional Blocks

### Wake Controller

Un bloc périphérique utilisé par un ordinateur contrôleur pour gérer les Wake Nodes enregistrés.

Ce qu'il fait :

- expose le type de périphérique `wake_controller`
- liste les noeuds enregistrés
- charge et décharge les chunks des noeuds
- rapporte si les noeuds sont actuellement chargés et combien de temps il reste pour les chargements temporaires

Recette :

![Recette Wake Controller](https://raw.githubusercontent.com/cogilabs/CC-Wake-Nodes/refs/heads/main/readmeSources/wake_controller_recipe.png)

Onglet Créatif : Functional Blocks

## Comment ça marche

1. Placez une Wake Node sur la face d'un ordinateur CC: Tweaked.
2. Sur cet ordinateur, appelez `setId("some_name")` sur le périphérique `wake_node` pour enregistrer le noeud.  
	 L'ordinateur qui effectue cet enregistrement devient le propriétaire du noeud.
3. Autorisez des ordinateurs contrôleurs avec `grantController(computerId)`.
4. Placez un Wake Controller à côté d'un autre ordinateur.
5. Depuis l'ordinateur contrôleur autorisé, appelez `loadNode("some_name")` ou `loadFor("some_name", seconds)`.

Le chunk chargé est le chunk de l'ordinateur, pas le chunk du bloc Wake Node.

Chaque noeud a un ordinateur propriétaire et une liste de contrôle d'accès (ACL) d'IDs d'ordinateurs contrôleurs autorisés.  
Les méthodes contrôleur n'opèrent que sur les noeuds que l'ordinateur appelant est autorisé à gérer.

Les enregistrements de noeuds sont sauvegardés, mais les chunks forcés actifs ne le sont pas. Après un redémarrage du serveur, les noeuds existent toujours dans le registre, mais aucun d'entre eux ne reste chargé jusqu'à ce qu'on le demande à nouveau.

## API ComputerCraft

### Types de périphériques

- Type de périphérique Wake Node : `wake_node`
- Type de périphérique Advanced Wake Node : `wake_node_advanced`
- Type de périphérique Wake Controller : `wake_controller`

### API Wake Node

Trouvez le périphérique avec les appels standard ComputerCraft tels que :

```lua
local wake = peripheral.find("wake_node")
```
  
Consultez [le fichier API.md](https://github.com/cogilabs/CC-Wake-Nodes/blob/main/API.md) sur GitHub pour une version plus compacte de la documentation de l'API.

#### `setId(id)`

Enregistre cette Wake Node sous l'ID de chaîne donné.

- `id` doit être une chaîne non vide
- à la première inscription réussie, l'ordinateur appelant devient le propriétaire du noeud
- si le même ID est déjà enregistré sur le même noeud, l'appel est accepté
- si l'ID est déjà utilisé par un autre noeud, l'appel déclenche une erreur

Exemple :

```lua
local wake = peripheral.find("wake_node")
assert(wake, "wake_node not found")

wake.setId("remote_factory")
```

#### `grantController(computerId)`

Autorise un ID d'ordinateur contrôleur à gérer ce noeud.

- seul l'ordinateur propriétaire peut appeler cette méthode
- `computerId` doit être un entier positif
- idempotent : accorder le même ID deux fois est accepté

#### `revokeController(computerId)`

Supprime un ID d'ordinateur contrôleur de la liste autorisée de ce noeud.

- seul l'ordinateur propriétaire peut appeler cette méthode
- idempotent : révoquer un ID qui n'est pas présent est accepté

#### `listControllers()`

Retourne une liste triée des IDs d'ordinateurs contrôleurs autorisés.

#### `getPermissions()`

Retourne la propriété et l'ACL du noeud :

```lua
{
	owner = 17,
	controllers = { 42, 73 }
}
```

#### `getId()`

Retourne l'ID de noeud actuel, ou `nil` si aucun n'a été assigné.

Exemple :

```lua
print(wake.getId())
```

#### `getChunk()`

Retourne une table décrivant le chunk de l'ordinateur attaché :

```lua
{
	dimension = "minecraft:overworld",
	chunk_x = 12,
	chunk_z = -4
}
```

#### `getInfo()`

Retourne une table avec l'ID du noeud et les informations de chunk :

```lua
{
	id = "remote_factory",
	dimension = "minecraft:overworld",
	chunk_x = 12,
	chunk_z = -4
}
```

Si aucun ID n'a encore été assigné, `id` est retourné comme chaîne vide.

### API Advanced Wake Node

L'Advanced Wake Node expose le type de périphérique `wake_node_advanced`. Elle hérite de toutes les méthodes de la Wake Node basique et ajoute les suivantes :

```lua
local wake = peripheral.find("wake_node_advanced")
```

#### `setRange(size)`

Définit la zone chargée autour de l'ordinateur.

- `size` doit être `1`, `3` ou `5`
- seul l'ordinateur propriétaire peut appeler cette méthode
- si le noeud est actuellement chargé, le jeu de chunks est mis à jour en direct (sauf si désactivé par la config)
- déclenche une erreur si la valeur dépasse le maximum serveur ou si l'appelant n'est pas le propriétaire

Exemple :

```lua
wake.setRange(5) -- charger une zone 5×5 (25 chunks)
```

#### `getRange()`

Retourne la taille actuelle du range (`1`, `3` ou `5`).

```lua
print(wake.getRange()) -- 3
```

#### `listAvailableRanges()`

Retourne un tableau des valeurs de range autorisées selon la configuration du serveur.

```lua
local ranges = wake.listAvailableRanges()
-- { 1, 3, 5 }
```

#### `getInfo()` (surchargé)

Retourne la même table que le `getInfo()` basique avec deux champs supplémentaires :

```lua
{
	id = "remote_factory",
	dimension = "minecraft:overworld",
	chunk_x = 12,
	chunk_z = -4,
	range = 3,
	loaded_chunks = 9
}
```

### API Wake Controller

Trouvez le périphérique avec les appels standard ComputerCraft tels que :

```lua
local ctl = peripheral.find("wake_controller")
```

#### `listNodes()`

Retourne une liste d'IDs de noeuds enregistrés accessibles par l'ordinateur contrôleur appelant.

Exemple :

```lua
for _, id in ipairs(ctl.listNodes()) do
	print(id)
end
```

#### `getNodeInfo(id)`

Retourne les informations du noeud pour un noeud accessible.

Déclenche une erreur si le noeud n'existe pas ou si le contrôleur appelant n'est pas autorisé.

Table retournée :

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

Remarques :

- `loaded` est un booléen
- `expires_at` n'est présent que pour les chargements temporaires
- `expires_at` est rapporté en secondes restantes, pas en timestamp absolu
- pour les Advanced Wake Nodes, la table inclut aussi `range` (1, 3 ou 5) et `loaded_chunks` (1, 9 ou 25)

#### `loadNode(id)`

Force le chargement du chunk du noeud indéfiniment, jusqu'à ce que l'une des actions suivantes se produise :

- `unloadNode(id)` est appelé
- `unloadAll()` est appelé
- le serveur s'arrête

Déclenche une erreur si le noeud n'existe pas.

Déclenche également une erreur si le contrôleur appelant n'est pas autorisé pour ce noeud.

#### `loadFor(id, seconds)`

Force le chargement du chunk du noeud pour un nombre limité de secondes.

- `seconds` doit être positif
- `seconds` ne doit pas dépasser `max_load_duration_seconds` configuré

Déclenche une erreur si le noeud n'existe pas ou si la durée est invalide.

Déclenche également une erreur si le contrôleur appelant n'est pas autorisé pour ce noeud.

#### `unloadNode(id)`

Décharge le noeud donné s'il est actuellement chargé.

#### `unloadAll()`

Décharge tous les noeuds actuellement chargés accessibles par le contrôleur appelant.

#### `isNodeLoaded(id)`

Retourne `true` si le noeud est actuellement chargé, sinon `false`.

Déclenche une erreur si le contrôleur appelant n'est pas autorisé pour ce noeud.

#### `getLoadedNodes()`

Retourne une liste d'IDs de noeuds actuellement chargés accessibles par le contrôleur appelant.

## Exemple de configuration

### Ordinateur distant

Attachez une Wake Node à un ordinateur et enregistrez-la :

```lua
local wake = peripheral.find("wake_node")
assert(wake, "wake_node not found")

wake.setId("remote_factory")
wake.grantController(42) -- ID de l'ordinateur contrôleur
print(textutils.serialize(wake.getInfo()))
```

### Ordinateur contrôleur

Chargez le chunk distant pendant deux minutes :

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

Clés de configuration serveur :

- `chunk_loading.max_loaded_nodes` : nombre maximum de noeuds qui peuvent être chargés en même temps. Par défaut : `16`
- `chunk_loading.max_load_duration_seconds` : durée maximale acceptée par `loadFor`. Par défaut : `300`
- `chunk_loading.default_load_radius` : rayon en chunks autour de l'ordinateur cible. `0` signifie uniquement le chunk propre de l'ordinateur. Par défaut : `0`
- `chunk_loading.chunk_ops_per_tick` : nombre maximal d'opérations de mise en file (chargement/déchargement) traitées chaque tick serveur. Des valeurs plus faibles lissent les pics mais réveillent les noeuds plus lentement. Par défaut : `3`
- `advanced_wake_node.enabled` : active le bloc Advanced Wake Node. Par défaut : `true`
- `advanced_wake_node.default_range` : range par défaut pour les Advanced Wake Nodes nouvellement placées (1, 3 ou 5). Par défaut : `3`
- `advanced_wake_node.max_range` : range maximum autorisé (1, 3 ou 5). Par défaut : `5`
- `advanced_wake_node.allow_range_change_while_loaded` : autorise le changement de range pendant que le noeud est chargé. Par défaut : `true`

Clés de configuration communes :

- `logging.enable_node_logs` : active les messages de log serveur pour les événements d'enregistrement, de chargement, de déchargement et d'expiration. Par défaut : `true`

## Remarques

- Les enregistrements sont partagés via un registre au niveau du monde.
- Chaque noeud a un ID d'ordinateur propriétaire et une ACL d'IDs d'ordinateurs contrôleurs autorisés.
- Les méthodes contrôleur n'opèrent que sur les noeuds que le contrôleur appelant est autorisé à gérer.
- Si une Wake Node est cassée, son enregistrement est supprimé.
- Si l'ordinateur support est retiré, la Wake Node se casse automatiquement.
- Les chargements temporaires de chunks expirent sur le tick du serveur et sont nettoyés automatiquement.
- À l'arrêt du serveur, tous les tickets de chunk actifs sont libérés.

## Exemples de scripts

Le dépôt inclut des scripts Lua d'exemple dans [`sampleScripts/recallCreateTrains/`](https://github.com/cogilabs/CC-Wake-Nodes/tree/main/sampleScripts/recallCreateTrains) démontrant comment les Wake Nodes peuvent réveiller des ordinateurs reliés à des gares Create distants et coordonner les flux de rappel de trains via Rednet.

## Licence

Le code est sous licence MIT.

Certaines ressources incluses (textures) sont sous la ComputerCraft Public License v1.0.0 (CCPL).

Voir les fichiers [LICENSE](https://github.com/cogilabs/CC-Wake-Nodes/blob/main/LICENSE) et [LICENSE-CCPL](https://github.com/cogilabs/CC-Wake-Nodes/blob/main/LICENSE-CCPL.md) pour plus de détails.

