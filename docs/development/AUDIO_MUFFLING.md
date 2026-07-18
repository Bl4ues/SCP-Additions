# Equipment Audio Muffling

SCP Additions 3.0.6 applies client-side listener-perception effects without changing the player's audio sliders.

## Hazmat Suit

While the complete Hazmat Suit is equipped, ordinary world audio receives a strong low-pass filter and volume reduction. The filter strength is `0.60`, increased from the initial `0.42` implementation. This includes ambient sounds, weather, blocks, entities, music, records, and interface sounds.

The following authored sounds are considered internal to the mask and remain clear when the Hazmat Suit is the only active source of muffling:

```text
scp_additions:hazmat_equip
scp_additions:hazmat_remove
scp_additions:hazmat_breathing
```

## SCP-714

SCP-714 begins with a subtle auditory suppression and increases continuously with the server-authoritative exposure progress. At 120 seconds, the effective gain reaches zero and the player hears complete silence.

SCP-714 also affects the Hazmat Suit's internal sounds. When both items are active, the Hazmat supplies the initial fixed muffling while SCP-714 progressively suppresses all remaining audio, including breathing.

Removing either item restores its contribution smoothly instead of changing volume instantly.

## Audio backend

The primary implementation uses the OpenAL `ALC_EXT_EFX` low-pass filter on active Minecraft sound channels, including sounds that began before the equipment state changed. New static and streaming channels are registered as they start and receive the current filter state.

When OpenAL EFX is unavailable, the client falls back to listener-volume fading. The fallback preserves progression and complete silence at the end of SCP-714's two-minute sequence, but cannot reproduce true frequency filtering.

The fallback writes this warning once:

```text
OpenAL EFX is unavailable; SCP equipment audio will use volume fading without low-pass filtering
```

## QA checklist

- Equip and remove the Hazmat Suit while a looping ambient or machine sound is already playing;
- Confirm world sounds are strongly muffled while breathing remains clear;
- Start new block, entity, weather, music, record, and interface sounds while the suit is equipped;
- Equip SCP-714 and listen at the beginning, middle, near 120 seconds, and during the five-second coma grace period;
- Confirm silence is complete at 120 seconds;
- Remove SCP-714 before death and confirm audio returns smoothly;
- Combine SCP-714 with the Hazmat Suit and confirm breathing also fades out;
- Reload resources or change audio devices and confirm channels do not become permanently muted;
- Test on a backend without EFX when available and verify the volume-only fallback.
