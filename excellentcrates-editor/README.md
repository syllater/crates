# ExcellentCrates Editor

Een Minecraft Paper 1.21+ plugin die het bewerken van ExcellentCrates percentages een stuk makkelijker maakt.

## Features

- **GUI Editor** - Interactieve inventory-based editor voor het bewerken van weights en percentages
- **Command Line Tools** - Snelle aanpassingen via console of chat
- **Auto-Balance** - Automatisch equal percentages verdelen
- **Scale Tool** - Schaal een rarity naar een specifiek percentage

## Commands

| Command | Aliases | Description |
|---------|---------|-------------|
| `/ce` | `/crateseditor` | Open de GUI editor |
| `/cp <crate>` | `/cratespercentages` | Bekijk crate percentages |
| `/cb <crate> [rarity]` | `/cratesbalance` | Balance alle weights |
| `/cs <crate> <rarity> <%>` | `/cratesscale` | Schaal rarity naar % |

### Subcommands

#### `/ce list` - Lijst alle crates
#### `/ce info <crate>` - Toon crate details met percentages
#### `/ce setweight <crate> <reward> <weight>` - Zet reward weight
#### `/ce setrarity <crate> <reward> <rarity>` - Verplaats reward naar andere rarity
#### `/ce balance <crate> [rarity]` - Balance weights
#### `/ce scale <crate> <rarity> <percentage>` - Schaal rarity
#### `/ce reload` - Herlaad crate data

## Permissions

| Permission | Description |
|------------|-------------|
| `crateseditor.use` | Toegang tot alle editor functies |

## Build

```bash
mvn clean package
```

De JAR comes in `target/excellentcrates-editor-1.0.0.jar`

## Installatie

1. Download de JAR
2. Plaats in je server's `plugins/` folder
3. Herstart de server
4. Zorg dat ExcellentCrates correct geïnstalleerd is
5. Gebruik `/ce` om de GUI te openen

## GUI Editor Uitleg

### Hoofdmenu
- Klik op een crate om deze te bewerken
- Pagination voor grote crate lijsten

### Crate Editor
- Rarity items tonen naam, weight, en chance %
- "Edit Rewards" opent de reward editor
- "Balance All Weights" maakt alle percentages gelijk
- "Refresh" herlaadt de crate data

### Rarity Editor
- Toont alle rewards binnen de rarity
- Gesorteerd op percentage (hoogste eerst)
- "Balance Rewards" maakt rewards in deze rarity gelijk
- Kleurcodering: goud ≥10%, zilver ≥1%, koper ≥0.1%

### Reward Editor
- +/- 1 en +/- 0.5 buttons voor fijne controle
- Reset naar standaard 10
- Live percentage preview

## Percentage Berekening

ExcellentCrates gebruikt een weight-systeem:

```
Rarity Chance = (RarityWeight / TotalRarityWeight) * 100%
Reward Chance = (RewardWeight / TotalRewardWeightInRarity) * RarityChance
Total Chance  = RewardChance * RarityChance / 100
```

### Voorbeeld
```
Rarities:
- Common: weight 70 (70% chance)
- Rare: weight 25 (25% chance)  
- Legendary: weight 5 (5% chance)

Common Rewards:
- Item A: weight 50 (50% van common = 35% totaal)
- Item B: weight 50 (50% van common = 35% totaal)
```

## Support

Voor issues of feature requests, maak een ticket aan op GitHub.
