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
- [ ] disabling HUD removes only visual overlays
- [ ] disabling stamina restores vanilla movement behavior immediately

## Keycards and shared item access

- [ ] readers detect keycards in vanilla inventory
- [ ] readers detect keycards in custom key slots
- [ ] readers reject insufficient clearance
- [ ] Tesla terminal detects security credentials in both inventories
- [ ] no item is counted twice by compatibility sources

## SCP-173 and blink

- [ ] entity registers under `scp_additions`
- [ ] model, texture, animation and sounds load
- [ ] blink input and HUD function
- [ ] SCP-173 respects line of sight and blink state
- [ ] neck-snap damage type resolves
- [ ] disabling natural spawn stops new natural spawns
- [ ] disabling behavior does not corrupt existing entities

## SCP-131

- [ ] both variants register and render
- [ ] glow masks render correctly
- [ ] animations and sounds load
- [ ] no natural spawn is introduced unintentionally
- [ ] explicitly spawned entities remain loadable when behavior is disabled

## Facility

- [ ] every migrated block has a valid item model
- [ ] every custom model resolves its textures
- [ ] door collision changes correctly across all animation states
- [ ] doors cannot become permanently desynchronized
- [ ] preferred Unity buttons operate doors correctly
- [ ] legacy Additions buttons still load in old worlds
- [ ] keycard readers still use Additions clearance levels
- [ ] facility-disabled configuration suppresses active behavior without missing registries

## Release compatibility

- [ ] clean 2.x world backup opens in 3.0
- [ ] existing block entities retain data
- [ ] existing SCP-294 configuration remains readable
- [ ] existing SCP-914 configuration remains readable
- [ ] client and dedicated server connect with identical configs
- [ ] no standalone SCP Inventory or Extra Blocks JAR is required
