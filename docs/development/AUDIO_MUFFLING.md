# Equipment Audio Muffling

SCP Additions 3.0.6 applies client-side listener-perception effects without changing the player's audio sliders.

## Diegetic boundary

Only sounds that exist inside the game world are muffled. Blocks, entities, players, weather, ambient world sources, jukebox records, doors, machines, footsteps, attacks, and similar positioned gameplay audio are diegetic.

Non-diegetic soundtrack music and interface audio remain clear. In implementation terms, `SoundSource.MUSIC` and `SoundSource.MASTER` are excluded, while `SoundSource.RECORDS` remains included because a jukebox is an audible object in the world.

## Hazmat Suit

While the complete Hazmat Suit is equipped, diegetic audio receives a strong low-pass filter and volume reduction. The filter strength is `0.60`, increased from the initial `0.42` implementation.

The following authored sounds are considered internal to the mask and remain clear when the Hazmat Suit is the only active source of muffling:

```text
scp_additions:hazmat_equip
scp_additions:hazmat_remove
scp_additions:hazmat_breathing
```

## SCP-714

SCP-714 begins with subtle auditory suppression and increases continuously with the server-authoritative exposure progress. At 120 seconds, diegetic gain reaches zero, silencing the perceived world while leaving non-diegetic soundtrack music clear.

The submitted `scp_additions:scp_714` soundtrack is streamed in `SoundSource.MUSIC`, is relative to the local listener, and is created only on the affected client. Other players cannot hear it as a positioned world sound. It plays once per exposure and is not looped or restarted because the authored file already matches the effect timing.

If exposure ends early, the ring is removed, the player dies, or the client leaves the world, the soundtrack fades out over 40 ticks (two seconds) instead of stopping abruptly. A new exposure starts a fresh soundtrack immediately, even if a previous fade was still finishing.

SCP-714 still affects the Hazmat Suit's internal breathing and equipment sounds because those sounds represent bodily perception rather than soundtrack music. When both items are active, the Hazmat supplies the initial fixed muffling while SCP-714 progressively suppresses the remaining diegetic and mask-internal audio.

## Audio backend

The implementation uses the OpenAL `ALC_EXT_EFX` low-pass filter on active Minecraft sound channels, including sounds that began before the equipment state changed. New static and streaming channels are registered as they start and receive the current filter state.

Selective category-aware muffling cannot be reproduced safely by lowering the global listener gain. When OpenAL EFX is unavailable, muffling is disabled rather than incorrectly muting soundtrack and interface audio. The client writes this warning once:

```text
OpenAL EFX is unavailable; selective SCP equipment muffling is disabled
```

## QA checklist

- Equip and remove the Hazmat Suit while a looping ambient or machine sound is already playing;
- Confirm diegetic world sounds are strongly muffled while breathing remains clear;
- Confirm soundtrack music and interface clicks remain clear;
- Confirm a jukebox record remains muffled as a diegetic source;
- Start new block, entity, weather, record, machine, and player sounds while the suit is equipped;
- Equip SCP-714 and confirm its soundtrack begins once and remains centered on the affected listener;
- Remove SCP-714 early and confirm a two-second fade-out;
- Re-equip during that fade and confirm the new soundtrack starts immediately;
- Listen at the beginning, middle, near 120 seconds, and during the five-second coma grace period;
- Confirm diegetic audio is silent at 120 seconds while the authored soundtrack remains clear;
- Combine SCP-714 with the Hazmat Suit and confirm breathing also fades out;
- Reload resources or change audio devices and confirm channels do not become permanently filtered;
- Test on a backend without EFX when available and confirm music/UI remain unaffected.
