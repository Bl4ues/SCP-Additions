package com.bl4ues.scpclassifieddirective.client.gui;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.block.entity.TeslaTerminalBlockEntity;
import com.bl4ues.scpclassifieddirective.network.TeslaTerminalButtonMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Keeps the Tesla terminal's authored CRT workflow alive after the interaction
 * screen closes. The physical monitor is a world object, so dismissing the
 * camera focus must not dismiss popups or freeze timed terminal transitions.
 */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class TeslaTerminalVisualStateClient {
    private static final ResourceLocation SCREEN_ON = screen("1");
    private static final ResourceLocation SCREEN_STANDBY_DISABLE = screen("2");
    private static final ResourceLocation SCREEN_OFF = screen("3");
    private static final ResourceLocation SCREEN_STANDBY_ENABLE = screen("4");
    private static final ResourceLocation SCREEN_CREDENTIAL_PROMPT = screen("5");
    private static final ResourceLocation SCREEN_INVALID_CREDENTIALS = screen("6");
    private static final ResourceLocation SCREEN_AUTH_SUCCESS = screen("7");
    private static final ResourceLocation SCREEN_OVERRIDE_WARNING = screen("8");
    private static final ResourceLocation SCREEN_OVERRIDE_STANDBY = screen("9");
    private static final ResourceLocation SCREEN_OVERRIDE_ENGAGED = screen("10");
    private static final ResourceLocation SCREEN_ON_OVERRIDE = screen("11");
    private static final ResourceLocation SCREEN_AUXILIARY_OFFLINE = screen("12");

    private static final Map<Key, SavedState> STATES = new HashMap<>();

    private TeslaTerminalVisualStateClient() { }

    static Snapshot take(Level level, BlockPos pos) {
        if (level == null || pos == null) return null;
        Key key = key(level, pos);
        SavedState state = STATES.remove(key);
        if (state == null) return null;
        advance(level, pos, state, level.getGameTime());
        return state.snapshot();
    }

    static void store(Level level, BlockPos pos, Snapshot snapshot) {
        if (level == null || pos == null || snapshot == null) return;
        SavedState state = new SavedState(snapshot, level.getGameTime());
        STATES.put(key(level, pos), state);
        if (STATES.size() > 256) {
            Iterator<Key> iterator = STATES.keySet().iterator();
            while (STATES.size() > 192 && iterator.hasNext()) {
                iterator.next();
                iterator.remove();
            }
        }
    }

    public static DisplayView view(Level level, BlockPos pos,
            TeslaTerminalBlockEntity terminal) {
        if (level == null || pos == null) return null;
        SavedState state = STATES.get(key(level, pos));
        if (state == null) return null;
        advance(level, pos, state, level.getGameTime());
        reconcileTerminalSnapshot(state, terminal);
        return display(state);
    }

    @SubscribeEvent
    public static void clientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft minecraft = Minecraft.getInstance();
        Level level = minecraft.level;
        if (level == null) {
            STATES.clear();
            return;
        }
        long now = level.getGameTime();
        ResourceKey<Level> dimension = level.dimension();
        Iterator<Map.Entry<Key, SavedState>> iterator = STATES.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Key, SavedState> entry = iterator.next();
            if (!entry.getKey().dimension.equals(dimension)) continue;
            BlockPos pos = entry.getKey().pos;
            if (level.hasChunkAt(pos)) {
                if (!(level.getBlockEntity(pos) instanceof TeslaTerminalBlockEntity terminal)) {
                    iterator.remove();
                    continue;
                }
                reconcileAuxiliary(entry.getValue(), terminal.auxiliaryPowerOnline());
            }
            advance(level, pos, entry.getValue(), now);
        }
    }

    private static void advance(Level level, BlockPos pos, SavedState state,
            long now) {
        long elapsedLong = Math.max(0L, now - state.lastTick);
        state.lastTick = now;
        int elapsed = (int) Math.min(Integer.MAX_VALUE, elapsedLong);
        while (elapsed > 0 && state.visualTimer > 0) {
            if (elapsed < state.visualTimer) {
                state.visualTimer -= elapsed;
                return;
            }
            elapsed -= state.visualTimer;
            state.visualTimer = 0;
            finishTimedState(level, pos, state);
        }
    }

    private static void finishTimedState(Level level, BlockPos pos,
            SavedState state) {
        if (state.visualState == TeslaTerminalScreen.VisualState.AUTH_SUCCESS) {
            beginAuthorizedAction(level, pos, state, state.pendingAction);
            return;
        }
        if (state.visualState == TeslaTerminalScreen.VisualState.STANDBY_DISABLE
                || state.visualState == TeslaTerminalScreen.VisualState.STANDBY_ENABLE) {
            resetToMain(state);
            return;
        }
        if (state.visualState == TeslaTerminalScreen.VisualState.OVERRIDE_STANDBY) {
            state.visualState = TeslaTerminalScreen.VisualState.OVERRIDE_ENGAGED;
            state.visualTimer = 0;
        }
    }

    private static void beginAuthorizedAction(Level level, BlockPos pos,
            SavedState state, TeslaTerminalScreen.PendingAction action) {
        if (action == TeslaTerminalScreen.PendingAction.DISABLE_GATES) {
            applyLocalAction(state, action);
            sendAction(pos, action);
            playHeadSound("turningoff");
            state.visualState = TeslaTerminalScreen.VisualState.STANDBY_DISABLE;
            state.visualTimer = 185;
        } else if (action == TeslaTerminalScreen.PendingAction.ENABLE_GATES) {
            applyLocalAction(state, action);
            sendAction(pos, action);
            playHeadSound("turningon");
            state.visualState = TeslaTerminalScreen.VisualState.STANDBY_ENABLE;
            state.visualTimer = 185;
        } else if (action == TeslaTerminalScreen.PendingAction.OVERRIDE_ON) {
            state.visualState = TeslaTerminalScreen.VisualState.OVERRIDE_WARNING;
            state.visualTimer = 0;
            playBlockSound(level, pos, "popup", 1.0F, 1.5F);
        } else if (action == TeslaTerminalScreen.PendingAction.OVERRIDE_OFF) {
            applyLocalAction(state, action);
            sendAction(pos, action);
            resetToMain(state);
        } else {
            resetToMain(state);
        }
    }

    private static void applyLocalAction(SavedState state,
            TeslaTerminalScreen.PendingAction action) {
        if (action == TeslaTerminalScreen.PendingAction.ENABLE_GATES) {
            state.displayedTeslaGatesEnabled = true;
        } else if (action == TeslaTerminalScreen.PendingAction.DISABLE_GATES) {
            state.displayedTeslaGatesEnabled = false;
            state.displayedManualOverride = false;
        } else if (action == TeslaTerminalScreen.PendingAction.OVERRIDE_ON) {
            state.displayedTeslaGatesEnabled = true;
            state.displayedManualOverride = true;
        } else if (action == TeslaTerminalScreen.PendingAction.OVERRIDE_OFF) {
            state.displayedManualOverride = false;
        }
    }

    private static void sendAction(BlockPos pos,
            TeslaTerminalScreen.PendingAction action) {
        int id = switch (action) {
            case ENABLE_GATES -> 0;
            case DISABLE_GATES -> 1;
            case OVERRIDE_ON -> 3;
            case OVERRIDE_OFF -> 4;
            default -> -1;
        };
        if (id >= 0) {
            ScpClassifiedDirectiveMod.PACKET_HANDLER.sendToServer(
                    new TeslaTerminalButtonMessage(id, pos.getX(), pos.getY(), pos.getZ()));
        }
    }

    private static void reconcileAuxiliary(SavedState state, boolean auxiliary) {
        if (state.lastAuxiliaryPowerOnline == auxiliary) return;
        state.lastAuxiliaryPowerOnline = auxiliary;
        state.authenticated = false;
        resetToMain(state);
    }

    private static void reconcileTerminalSnapshot(SavedState state,
            TeslaTerminalBlockEntity terminal) {
        if (terminal == null) return;
        reconcileAuxiliary(state, terminal.auxiliaryPowerOnline());
        if (state.visualState != TeslaTerminalScreen.VisualState.MAIN
                || state.visualTimer > 0
                || state.pendingAction != TeslaTerminalScreen.PendingAction.NONE) {
            return;
        }
        state.displayedTeslaGatesEnabled = terminal.teslaGatesEnabled();
        state.displayedManualOverride = terminal.manualOverride();
        if (state.displayedManualOverride) {
            state.displayedTeslaGatesEnabled = true;
            state.visualState = TeslaTerminalScreen.VisualState.OVERRIDE_ENGAGED;
        }
    }

    private static void resetToMain(SavedState state) {
        state.visualState = TeslaTerminalScreen.VisualState.MAIN;
        state.pendingAction = TeslaTerminalScreen.PendingAction.NONE;
        state.visualTimer = 0;
    }

    private static DisplayView display(SavedState state) {
        ResourceLocation base = switch (state.visualState) {
            case STANDBY_DISABLE -> SCREEN_STANDBY_DISABLE;
            case STANDBY_ENABLE -> SCREEN_STANDBY_ENABLE;
            default -> state.displayedManualOverride ? SCREEN_ON_OVERRIDE
                    : state.displayedTeslaGatesEnabled ? SCREEN_ON : SCREEN_OFF;
        };
        ResourceLocation overlay = !state.lastAuxiliaryPowerOnline
                ? SCREEN_AUXILIARY_OFFLINE
                : switch (state.visualState) {
                    case CREDENTIAL_PROMPT -> SCREEN_CREDENTIAL_PROMPT;
                    case INVALID_CREDENTIALS -> SCREEN_INVALID_CREDENTIALS;
                    case AUTH_SUCCESS -> SCREEN_AUTH_SUCCESS;
                    case OVERRIDE_WARNING -> SCREEN_OVERRIDE_WARNING;
                    case OVERRIDE_STANDBY -> SCREEN_OVERRIDE_STANDBY;
                    case OVERRIDE_ENGAGED -> SCREEN_OVERRIDE_ENGAGED;
                    default -> null;
                };
        return new DisplayView(base, overlay, state.authenticated);
    }

    private static void playBlockSound(Level level, BlockPos pos,
            String soundId, float pitch, float volume) {
        if (level == null) return;
        SoundEvent sound = ForgeRegistries.SOUND_EVENTS.getValue(
                new ResourceLocation(ScpClassifiedDirectiveMod.MODID, soundId));
        if (sound != null) {
            level.playLocalSound(pos.getX() + 0.5D, pos.getY() + 0.5D,
                    pos.getZ() + 0.5D, sound, SoundSource.BLOCKS,
                    volume, pitch, false);
        }
    }

    private static void playHeadSound(String soundId) {
        SoundEvent sound = ForgeRegistries.SOUND_EVENTS.getValue(
                new ResourceLocation(ScpClassifiedDirectiveMod.MODID, soundId));
        if (sound != null) {
            Minecraft.getInstance().getSoundManager().play(new SimpleSoundInstance(
                    sound.getLocation(), SoundSource.AMBIENT, 1.0F, 1.0F,
                    RandomSource.create(), false, 0,
                    SoundInstance.Attenuation.NONE, 0.0D, 0.0D, 0.0D, true));
        }
    }

    private static Key key(Level level, BlockPos pos) {
        return new Key(level.dimension(), pos.immutable());
    }

    private static ResourceLocation screen(String id) {
        return new ResourceLocation(ScpClassifiedDirectiveMod.MODID,
                "textures/screens/" + id + ".png");
    }

    record Snapshot(TeslaTerminalScreen.VisualState visualState,
            TeslaTerminalScreen.PendingAction pendingAction, int visualTimer,
            boolean authenticated, boolean displayedTeslaGatesEnabled,
            boolean displayedManualOverride, boolean lastAuxiliaryPowerOnline) { }

    public record DisplayView(ResourceLocation base, ResourceLocation overlay,
            boolean authenticated) { }

    private record Key(ResourceKey<Level> dimension, BlockPos pos) { }

    private static final class SavedState {
        private TeslaTerminalScreen.VisualState visualState;
        private TeslaTerminalScreen.PendingAction pendingAction;
        private int visualTimer;
        private boolean authenticated;
        private boolean displayedTeslaGatesEnabled;
        private boolean displayedManualOverride;
        private boolean lastAuxiliaryPowerOnline;
        private long lastTick;

        private SavedState(Snapshot snapshot, long lastTick) {
            this.visualState = snapshot.visualState;
            this.pendingAction = snapshot.pendingAction;
            this.visualTimer = snapshot.visualTimer;
            this.authenticated = snapshot.authenticated;
            this.displayedTeslaGatesEnabled = snapshot.displayedTeslaGatesEnabled;
            this.displayedManualOverride = snapshot.displayedManualOverride;
            this.lastAuxiliaryPowerOnline = snapshot.lastAuxiliaryPowerOnline;
            this.lastTick = lastTick;
        }

        private Snapshot snapshot() {
            return new Snapshot(visualState, pendingAction, visualTimer,
                    authenticated, displayedTeslaGatesEnabled,
                    displayedManualOverride, lastAuxiliaryPowerOnline);
        }
    }
}
