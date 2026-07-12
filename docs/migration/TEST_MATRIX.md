# SCP Additions 3.0 test matrix

Every migration phase must pass `clean build` and a client startup smoke test before the next phase begins.

## Foundation

- [ ] `./gradlew clean build`
- [ ] dedicated server reaches normal startup
- [ ] client reaches the main menu
- [ ] existing Additions world opens without missing-registry warnings
- [ ] new `modules.json` is generated with valid defaults
- [ ] malformed module configuration falls back safely without crashing startup

## Inventory core

- [ ] capability attaches to new players
- [ ] capability persists through death according to intended rules
- [ ] capability survives logout and login
- [ ] capability synchronizes to the client
- [ ] pickup routing handles every configured item category
- [ ] full custom inventory does not duplicate or delete items
- [ ] dropped items remain recoverable
- [ ] keys remain available to shared access checks
- [ ] ammunition and weapon mirrors remain synchronized
- [ ] with custom inventory enabled, real coins are stored only in the capability
- [ ] with custom inventory enabled, coin pickup creates no vanilla mirror
- [ ] with custom inventory disabled, coin pickup goes to vanilla inventory
- [ ] pre-migration vanilla coins have an explicit migration or compatibility path
- [ ] switching inventory mode does not automatically merge, copy or duplicate balances

## SCP-294 currency integration

- [ ] with custom inventory enabled, coin button detects capability currency
- [ ] with custom inventory enabled, SCP-294 ignores vanilla coin stacks
- [ ] with custom inventory enabled, inserting a coin removes exactly one real coin from the capability
- [ ] with custom inventory disabled, coin button detects vanilla inventory currency
- [ ] with custom inventory disabled, SCP-294 ignores capability currency
- [ ] with custom inventory disabled, inserting a coin removes exactly one real vanilla coin
- [ ] capability and vanilla balances are never added together
- [ ] inserting a coin creates no mirror or duplicate stack
- [ ] the inserted coin is represented by the machine slot/state exactly once
- [ ] a successful coin-consuming drink removes exactly one inserted coin
- [ ] a non-coin-consuming drink preserves the inserted coin
- [ ] an unknown/out-of-range request does not silently delete the inserted coin
- [ ] closing and reopening the GUI cannot duplicate the inserted coin
- [ ] logout, death and chunk unload cannot duplicate currency
- [ ] changing inventory mode while a coin is inserted has a deterministic, duplication-safe result

## Usable-item sessions

- [ ] item enters the temporary vanilla slot exactly once
- [ ] item returns to its original custom slot
- [ ] switching slots cannot duplicate the item
- [ ] closing the screen cannot duplicate the item
- [ ] death cannot duplicate the item
- [ ] logout cannot duplicate the item
- [ ] dropping the active item has the intended result
- [ ] a full destination inventory has a safe fallback

## GUI and HUD

- [ ] inventory opens and closes normally
- [ ] every tab renders
- [ ] context menu actions reach the server
- [ ] codex entries render
- [ ] custom fonts load
- [ ] HUD scales correctly at common GUI scales
- [ ] disabling inventory prevents replacement behavior cleanly
- [ ] `hud.enabled=true` and `custom_health_enabled=true` render the custom health bar
- [ ] custom health HUD cancels only vanilla player hearts, not armor, hunger, air or mount health
- [ ] `custom_health_enabled=false` restores vanilla hearts and removes only the custom health row
- [ ] `hud.enabled=false` restores vanilla hearts and hides both custom rows without disabling stamina gameplay
- [ ] creative and spectator modes never render custom health or stamina rows
- [ ] changing to creative resets client stamina state without interfering with creative sprint
- [ ] stamina drains from 100 to 0 over approximately five seconds of moving sprint
- [ ] stamina waits 20 ticks after sprint expenditure before regenerating
- [ ] stamina regenerates from 0 to 100 over approximately five seconds
- [ ] holding sprint while exhausted does not allow regeneration or repeated sprint bursts
- [ ] server enforcement prevents a client from continuing to sprint at zero stamina
- [ ] the eight-tick exhausted sprint lock matches the standalone behavior
- [ ] disabling stamina immediately restores vanilla sprint behavior and hides the stamina row
- [ ] configured `NO_STAMINA` hand and armor items force stamina to zero
- [ ] future SCP Inventory equipment can register a blocker source without changing stamina code
- [ ] health damage flash lasts and fades according to the original one-second timing
- [ ] `horror_movement_enabled=true` applies base walk speed `0.055` in survival
- [ ] committed sprint input applies base speed `0.110` before the vanilla sprint multiplier
- [ ] exhausted stamina prevents the horror movement controller from re-enabling sprint
- [ ] creative and spectator retain vanilla movement speed and sprint behavior
- [ ] `horror_movement_enabled=false` restores base movement speed `0.100`
- [ ] logout and respawn clear stale horror-sprint input state

## Keycards and shared item access

- [ ] readers detect keycards in vanilla inventory
- [ ] readers detect keycards in custom key slots
- [ ] readers reject insufficient clearance
- [ ] Tesla terminal detects security credentials in both inventories
- [ ] no item is counted twice by compatibility sources

## SCP-173 and blink

- [ ] entity and spawn egg register under `scp_additions`
- [ ] model, translucent texture and static GeckoLib animation load
- [ ] scare, horror, rattle, scrape, death and neck-snap sounds load
- [ ] blink HUD appears only after confirmed visual contact within 20 blocks
- [ ] blink HUD remains during the intended 200-tick paranoia window
- [ ] automatic blink interval, blackout and post-blink cover match the standalone mod
- [ ] holding the Blink key keeps the eyes closed and synchronizes to the server
- [ ] Eye Sore accelerates blink drain by the intended multiplier
- [ ] SCP-173 freezes when any valid observer has visual confirmation
- [ ] transparent blocks do not falsely occlude SCP-173
- [ ] solid occluding blocks prevent observation
- [ ] client interpolation cannot slide SCP-173 while observed
- [ ] blink transitions trigger immediate server-side movement reevaluation
- [ ] direct pursuit, path fallback and side-step fallback match the standalone implementation
- [ ] frozen air fall, frozen water sink and moving water sink behave correctly
- [ ] contact-only neck snap resolves through the custom damage type
- [ ] routine spawn checks every 6000 player ticks with the configured probability
- [ ] routine spawn prefers front positions and validates full hitbox clearance
- [ ] only one SCP-173 routine instance exists globally
- [ ] routine-spawned SCP-173 remains inactive until observed
- [ ] routine-spawned SCP-173 despawns after 400 unseen ticks when no player is close
- [ ] spawn rattle and movement scrape pulses play at the intended cadence
- [ ] durability is 1730 health, 80 armor, 40 toughness and full knockback resistance
- [ ] incoming damage is reduced to 2% with a minimum of 0.25
- [ ] configured mobs and `#minecraft:raiders` can observe and be targeted
- [ ] disabling natural spawn stops new routine spawns
- [ ] disabling SCP-173 behavior freezes existing instances and clears the blink threat HUD

## SCP-131

- [ ] both variants register under `scp_additions`
- [ ] both spawn eggs appear in the SCP creative tab after SCP-079 and before SCP-173
- [ ] both variants render at the intended scale
- [ ] glow masks render correctly
- [ ] idle animation and voice sounds load
- [ ] right-clicking starts nearby SCP-131 followers but never stops them
- [ ] custom SCP-styled notice appears instead of an actionbar message
- [ ] holding physical G for 20 ticks sends the server stop request
- [ ] G stops all owned SCP-131 followers within the original range
- [ ] following state and owner persist across save/reload
- [ ] SCP-131-B follows a nearby idle SCP-131-A
- [ ] nearby SCP-131 entities run to and continuously observe SCP-173
- [ ] no natural spawn is introduced unintentionally
- [ ] no SCP-131 gameplay toggle is generated in `modules.json`

## Facility

- [ ] every migrated block has a valid item model
- [ ] every custom model resolves its textures
- [ ] facility blocks and props remain available without a gameplay toggle
- [ ] Tesla Gate and Tesla Recharge inactive states render alpha as translucent, not black
- [ ] door collision changes correctly across all animation states
- [ ] doors cannot become permanently desynchronized
- [ ] original and mirrored Unity buttons both operate doors independently
- [ ] clicking the right half of the supporting block places a functional left-side mirrored button
- [ ] manually paired original/mirrored buttons preserve their own geometry through every state
- [ ] mirrored buttons emit both weak and direct redstone while opening/open
- [ ] redstone heavy-door opening reaches the fully open endpoint in exactly 24 ticks (1.2 seconds)
- [ ] redstone heavy-door closing reaches the fully closed endpoint in exactly 24 ticks (1.2 seconds)
- [ ] default, yellow and black heavy doors share the same exact timing
- [ ] direct-use wooden door timings remain unchanged
- [ ] legacy Additions buttons still load in old worlds
- [ ] keycard readers still use Additions clearance levels

## Release compatibility

- [ ] clean 2.x world backup opens in 3.0
- [ ] existing block entities retain data
- [ ] existing SCP-294 configuration remains readable
- [ ] existing SCP-914 configuration remains readable
- [ ] client and dedicated server connect with identical configs
- [ ] no standalone SCP Inventory or Extra Blocks JAR is required
