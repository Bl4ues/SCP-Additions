# SCP-294 legacy drinks to port

This file is a reminder list for rebuilding SCP-294 defaults as config-driven drink definitions.

Runtime config path created by the mod:

```text
config/scpadditions/294drinks.json
```

Repository template path:

```text
config/scpadditions/294drinks.json
```

Schema draft:

```json
{
  "version": 1,
  "matching": {
    "allow_partial": true,
    "fuzzy_threshold": 0.66
  },
  "drinks": [
    {
      "id": "scp_additions:coffee",
      "enabled": true,
      "aliases": ["black coffee"],
      "result": {
        "item": "scp_additions:cup_of_coffee",
        "count": 1
      },
      "delay_ticks": 40,
      "sound": "scp_additions:scp294pouring",
      "consumes_coin": true,
      "actionbar": "Dispensing black coffee...",
      "cup_color": "#2B1608",
      "placeholder_cup_texture": "scp_additions:item/scp294_colored_cup_placeholder",
      "effects": [
        {
          "id": "minecraft:speed",
          "duration": 200,
          "amplifier": 0,
          "ambient": false,
          "visible": true,
          "show_icon": true
        }
      ]
    }
  ]
}
```

Matching behavior:

- Case-insensitive.
- Punctuation-insensitive.
- Exact aliases win first.
- Partial aliases are allowed by default: typing `coffee` can match `black coffee`.
- Fuzzy typo matching is enabled by `fuzzy_threshold`.
- If multiple drinks are close enough, the first/best option by JSON order wins.
- SCP-294 returns out of range only when no configured drink is close enough to the request.

Special legacy behavior to port:

- Empty output / empty cup aliases: `air`, `nothing`, `hl3`, `half life 3`, `emptiness`, `vacuum`, `cup` -> `scp_additions:empty_cup`, sound `scp_additions:scp294emptycup`.
- Normal drink sound: `scp_additions:scp294pouring`.
- Failed match sound: `scp_additions:scp294outofrange`.

Legacy output items to rebuild as defaults:

```text
scp_additions:cup_of_coffee
scp_additions:cup_of_alcohol
scp_additions:ethanol
scp_additions:spirit
scp_additions:vodka
scp_additions:aloe
scp_additions:cactus
scp_additions:amnesia
scp_additions:anti_energy
scp_additions:aqua_regia
scp_additions:beer
scp_additions:lager
scp_additions:corrosive_black
scp_additions:bleach
scp_additions:blood
scp_additions:blood_of_christ
scp_additions:grimace_shake
scp_additions:quantum
scp_additions:carbon
scp_additions:cassis_fanta
scp_additions:carrot
scp_additions:champagne
scp_additions:chim
scp_additions:cider
scp_additions:apple_cider
scp_additions:pear_cider
scp_additions:chocolate
scp_additions:cocaine
scp_additions:coconut
scp_additions:cola
scp_additions:cold
scp_additions:cosmopolitan
scp_additions:courage
scp_additions:curry
scp_additions:death
scp_additions:eggs
scp_additions:neutronium
scp_additions:energy_drink
scp_additions:espresso
scp_additions:estus
scp_additions:champion
scp_additions:fear
scp_additions:feces
scp_additions:feces_and_blood
scp_additions:gin
scp_additions:glass
scp_additions:gold_c
scp_additions:grog
scp_additions:happiness
scp_additions:heroin
scp_additions:morphine
scp_additions:honey
scp_additions:hot
scp_additions:tea
scp_additions:corrosive_acid
scp_additions:ice_cream
scp_additions:frozen_yogurt
scp_additions:yogurt
scp_additions:ink
scp_additions:insulin
scp_additions:ipecac
scp_additions:iron_c
```

Notes for next pass:

- `cup_color`, `placeholder_cup_texture`, `actionbar`, and `effects` are already parsed and written into the dispensed stack NBT under `Scp294Drink`.
- The dynamic colored cup item/model still needs to be implemented later to consume those NBT values visually and mechanically.
- Some legacy drinks had additional effects/death/explosion behavior in item-use procedures rather than in the SCP-294 dispensing procedure.
- The exact legacy alias list can still be recovered from Git history before the hardcoded procedures were replaced.
