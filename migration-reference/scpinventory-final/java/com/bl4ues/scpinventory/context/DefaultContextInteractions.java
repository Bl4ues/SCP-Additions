package com.bl4ues.scpinventory.context;

public final class DefaultContextInteractions {
    public static final String CONFIG = """
{
  "_comment": "Context interaction prompts for SCP Inventory. Copy entries from examples into interactions and adjust anchor.position as a local 0..1 block coordinate. position [0.5,0.5,0.05] means centered on the front face before rotation.",
  "interactions": [
    {
      "type": "block",
      "id": "minecraft:oak_door",
      "range": 2.8,
      "priority": 30,
      "action": "Open",
      "name": "Door",
      "icon": "hand",
      "anchor": {
        "position": [
          0.813,
          0.082,
          0.213
        ],
        "rotateWith": "auto"
      },
      "click": {
        "face": "front"
      },
      "input": {
        "allowE": true,
        "allowRightClick": true
      },
      "useItem": "hand",
      "text": {
        "action": "Open",
        "nameMode": "manual",
        "name": "Door",
        "showAction": true,
        "showName": true
      }
    },
    {
      "type": "block",
      "id": "minecraft:lever",
      "range": 2.25,
      "priority": 30,
      "action": "Use",
      "name": "Lever",
      "anchor": {
        "position": [
          0.512,
          0.375,
          0.512
        ],
        "rotateWith": "auto"
      },
      "click": {
        "face": "front"
      },
      "input": {
        "allowE": true,
        "allowRightClick": true
      },
      "useItem": "hand",
      "text": {
        "action": "Use",
        "nameMode": "manual",
        "name": "Lever",
        "showAction": true,
        "showName": true
      }
    },
    {
      "type": "block",
      "id": "minecraft:chest",
      "range": 2.6,
      "priority": 70,
      "action": "Open",
      "name": "Chest",
      "anchor": {
        "position": [
          0.5,
          0.58,
          0.08
        ],
        "rotateWith": "auto"
      },
      "click": {
        "face": "front"
      },
      "input": {
        "allowE": true,
        "allowRightClick": true
      }
    },
    {
      "type": "block",
      "id": "minecraft:bell",
      "range": 2.25,
      "priority": 30,
      "useItem": "hand",
      "text": {
        "action": "Ring",
        "nameMode": "manual",
        "name": "Bell",
        "showAction": true,
        "showName": true
      },
      "anchor": {
        "position": [
          0.489,
          0.65,
          0.519
        ],
        "rotateWith": "none"
      },
      "input": {
        "allowE": true,
        "allowRightClick": true
      },
      "click": {
        "face": "front"
      }
    },
    {
      "type": "block",
      "id": "minecraft:oak_trapdoor",
      "range": 2.25,
      "priority": 30,
      "useItem": "hand",
      "text": {
        "action": "Open",
        "nameMode": "manual",
        "name": "Oak Trapdoor",
        "showAction": true,
        "showName": true
      },
      "anchor": {
        "position": [
          0.098,
          0.188,
          0.494
        ],
        "rotateWith": "none"
      },
      "input": {
        "allowE": true,
        "allowRightClick": true
      },
      "click": {
        "face": "front"
      }
    },
    {
      "type": "block",
      "id": "minecraft:brewing_stand",
      "range": 2.25,
      "priority": 30,
      "useItem": "hand",
      "text": {
        "action": "Use",
        "nameMode": "manual",
        "name": "Brewing Stand",
        "showAction": true,
        "showName": true
      },
      "anchor": {
        "position": [
          0.5,
          0.5,
          0.5
        ],
        "rotateWith": "none"
      },
      "input": {
        "allowE": true,
        "allowRightClick": true
      },
      "click": {
        "face": "front"
      }
    },
    {
      "type": "block",
      "id": "minecraft:crafting_table",
      "range": 2.25,
      "priority": 30,
      "useItem": "hand",
      "text": {
        "action": "Use",
        "nameMode": "manual",
        "name": "Crafting Table",
        "showAction": true,
        "showName": true
      },
      "anchor": {
        "position": [
          0.5,
          1.0,
          0.5
        ],
        "rotateWith": "none"
      },
      "input": {
        "allowE": true,
        "allowRightClick": true
      },
      "click": {
        "face": "front"
      }
    },
    {
      "type": "block",
      "id": "minecraft:furnace",
      "range": 2.25,
      "priority": 30,
      "useItem": "hand",
      "text": {
        "action": "Use",
        "nameMode": "manual",
        "name": "Furnace",
        "showAction": true,
        "showName": true
      },
      "anchor": {
        "position": [
          1.0,
          0.157,
          0.493
        ],
        "rotateWith": "none"
      },
      "input": {
        "allowE": true,
        "allowRightClick": true
      },
      "click": {
        "face": "front"
      }
    },
    {
      "type": "block",
      "id": "minecraft:stonecutter",
      "range": 2.25,
      "priority": 30,
      "useItem": "hand",
      "text": {
        "action": "Use",
        "nameMode": "manual",
        "name": "Stonecutter",
        "showAction": true,
        "showName": true
      },
      "anchor": {
        "position": [
          0.488,
          0.563,
          0.485
        ],
        "rotateWith": "none"
      },
      "input": {
        "allowE": true,
        "allowRightClick": true
      },
      "click": {
        "face": "front"
      }
    },
    {
      "type": "block",
      "id": "minecraft:hopper",
      "range": 2.25,
      "priority": 30,
      "useItem": "hand",
      "text": {
        "action": "Use",
        "nameMode": "manual",
        "name": "Hopper",
        "showAction": true,
        "showName": true
      },
      "anchor": {
        "position": [
          0.5,
          0.5,
          0.5
        ],
        "rotateWith": "none"
      },
      "input": {
        "allowE": true,
        "allowRightClick": true
      },
      "click": {
        "face": "front"
      }
    },
    {
      "type": "block",
      "id": "minecraft:anvil",
      "range": 2.25,
      "priority": 30,
      "useItem": "hand",
      "text": {
        "action": "Use",
        "nameMode": "manual",
        "name": "Anvil",
        "showAction": true,
        "showName": true
      },
      "anchor": {
        "position": [
          0.525,
          1.0,
          0.505
        ],
        "rotateWith": "none"
      },
      "input": {
        "allowE": true,
        "allowRightClick": true
      },
      "click": {
        "face": "front"
      }
    }
  ],
  "examples": [
    {
      "type": "block",
      "id": "minecraft:stone_button",
      "range": 2.25,
      "action": "Use",
      "name": "Button",
      "anchor": {
        "position": [
          0.5,
          0.5,
          0.05
        ],
        "rotateWith": "auto"
      },
      "click": {
        "face": "front"
      }
    },
    {
      "type": "entity",
      "id": "minecraft:villager",
      "range": 2.0,
      "action": "Talk",
      "nameMode": "auto"
    }
  ]
}
""";

    private DefaultContextInteractions() {
    }
}
