package com.bl4ues.scpclassifieddirective.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.fml.ModList;
import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.compat.SimpleVoiceChatPresence;
import com.bl4ues.scpclassifieddirective.data.Scp914SkinManager;
import com.bl4ues.scpclassifieddirective.network.DeathVoiceRosterPacket;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Voice-call presentation embedded into the death-screen personnel feed. */
public final class SimpleVoiceChatDeathScreenUi {
    private static final ResourceLocation MICROPHONE = new ResourceLocation(
            "voicechat", "textures/icons/microphone.png");
    private static final ResourceLocation MICROPHONE_OFF = new ResourceLocation(
            "voicechat", "textures/icons/microphone_off.png");
    private static final ResourceLocation MICROPHONE_WHISPER = new ResourceLocation(
            "voicechat", "textures/icons/microphone_whisper.png");

    private static final int MIC_SIZE = 16;
    private static final int MIC_BOX_SIZE = 24;
    private static final int HEAD_MIN_SIZE = 18;
    private static final int HEAD_MAX_SIZE = 30;
    private static final int HEAD_GAP = 6;
    private static final int DETACHED_MARGIN = 16;
    private static final int DETACHED_ICON_RESERVE = 18;
    private static final int DETACHED_MIC_GAP = 8;
    private static final int DEAD_CALL_IDLE_ALPHA = 92;
    private static final int DEAD_CALL_MUTED_ALPHA = 58;
    private static final int MUTED_RED = 0xFFD45C62;
    private static final int TALKING_BORDER = 0xFFE8E9EA;
    private static final int IDLE_BORDER = 0x705E656C;
    private static final String KLEIDERS_MOD_ID = "kleiders_custom_renderer";

    private static final Map<String, CachedSkin> CUSTOM_SKINS = new HashMap<>();
    private static boolean reflectionFailureLogged;

    private SimpleVoiceChatDeathScreenUi() {
    }

    public static boolean visible() {
        return SimpleVoiceChatPresence.installed()
                && SimpleVoiceChatCompatibilityClientState.known()
                && SimpleVoiceChatCompatibilityClientState.installed()
                && SimpleVoiceChatCompatibilityClientState.enabled();
    }

    /**
     * The MineZero feed is deliberately removed after its team-wipe static sting,
     * but the dead call itself still exists. Keep its controls alive independently
     * once the final living personnel target is gone.
     */
    public static boolean detachedVisible(ScpDeathScreen screen) {
        return visible() && screen != null && MineZeroClientState.allDead()
                && !MineZeroSpectateClient.active();
    }

    public static void render(ScpDeathScreen screen, GuiGraphics graphics,
            int mouseX, int mouseY, float alpha) {
        if (!visible() || screen == null || graphics == null
                || alpha <= 0.001F) {
            return;
        }

        Bounds feed = feed(screen);
        renderDeadRoster(screen, graphics, feed, mouseX, mouseY, alpha);
        renderOwnMicrophone(graphics, feed, alpha);
    }

    /**
     * Renders the same dead-call hierarchy after a team wipe, migrating it from
     * the removed personnel-feed rail into the lower-left corner while the death
     * report recenters. The local mute control remains the lowest element.
     */
    public static void renderDetached(ScpDeathScreen screen, GuiGraphics graphics,
            int mouseX, int mouseY, float alpha) {
        if (!detachedVisible(screen) || graphics == null || alpha <= 0.001F) {
            return;
        }
        DetachedLayout layout = detachedLayout(screen);
        if (layout == null) return;
        renderDetachedRoster(graphics, layout, mouseX, mouseY, alpha);
        renderOwnMicrophoneAt(graphics, layout.micX(), layout.micY(), alpha);
    }

    /**
     * Handles the microphone button before the personnel feed consumes the same
     * click as a camera drag.
     */
    public static boolean handleMousePressed(ScpDeathScreen screen,
            double mouseX, double mouseY) {
        if (!visible() || screen == null) return false;
        Bounds feed = feed(screen);
        int x = feed.left + 8;
        int y = feed.bottom - MIC_BOX_SIZE - 8;
        if (mouseX < x || mouseX >= x + MIC_BOX_SIZE
                || mouseY < y || mouseY >= y + MIC_BOX_SIZE) {
            return false;
        }
        toggleLocalMute();
        return true;
    }

    public static boolean handleDetachedMousePressed(ScpDeathScreen screen,
            double mouseX, double mouseY) {
        if (!detachedVisible(screen)) return false;
        DetachedLayout layout = detachedLayout(screen);
        if (layout == null) return false;
        int x = layout.micX();
        int y = layout.micY();
        if (mouseX < x || mouseX >= x + MIC_BOX_SIZE
                || mouseY < y || mouseY >= y + MIC_BOX_SIZE) {
            return false;
        }
        toggleLocalMute();
        return true;
    }

    private static void renderDeadRoster(ScpDeathScreen screen,
            GuiGraphics graphics, Bounds feed, int mouseX, int mouseY,
            float uiAlpha) {
        List<DeathVoiceRosterPacket.Participant> roster = orderedRoster();
        if (roster.isEmpty()) return;

        int cardRight = finalCardRight(screen);
        int railWidth = feed.left - cardRight;
        if (railWidth < HEAD_MIN_SIZE + 16) return;

        int headSize = Mth.clamp(railWidth - 18,
                HEAD_MIN_SIZE, HEAD_MAX_SIZE);
        int headX = feed.left - headSize - 9;
        if (headX < cardRight + 7) {
            headX = cardRight + Math.max(5,
                    (railWidth - headSize) / 2);
        }

        int availableHeight = Math.max(0, feed.bottom - feed.top - 8);
        int maxVisible = Math.max(1,
                (availableHeight + HEAD_GAP) / (headSize + HEAD_GAP));
        int count = Math.min(maxVisible, roster.size());
        String hoveredName = null;

        for (int i = 0; i < count; i++) {
            DeathVoiceRosterPacket.Participant participant = roster.get(i);
            int y = feed.bottom - headSize - 7
                    - i * (headSize + HEAD_GAP);
            if (y < feed.top) break;

            boolean muted = isParticipantMuted(participant.id());
            boolean talking = !muted && isParticipantTalking(participant.id());
            float portraitBrightness = talking ? 1.0F
                    : muted ? DEAD_CALL_MUTED_ALPHA / 255.0F
                    : DEAD_CALL_IDLE_ALPHA / 255.0F;

            graphics.fill(headX - 2, y - 2,
                    headX + headSize + 2, y + headSize + 2,
                    withAlpha(talking ? TALKING_BORDER : IDLE_BORDER, uiAlpha));
            graphics.fill(headX - 1, y - 1,
                    headX + headSize + 1, y + headSize + 1,
                    withAlpha(0xC006090C, uiAlpha));

            HeadTexture texture = headTexture(participant);
            renderHead(graphics, texture, headX, y, headSize, uiAlpha);

            // Dimming the two skin layers by lowering shader alpha separately
            // makes semi-transparent hat/outer-layer pixels effectively vanish.
            // Composite the complete head first, then dim it as one image so the
            // second layer remains visible whether the player is talking or idle.
            if (portraitBrightness < 0.999F) {
                graphics.fill(headX, y, headX + headSize, y + headSize,
                        withAlpha(0xFF06090C,
                                (1.0F - portraitBrightness) * uiAlpha));
            }

            if (muted) {
                drawMuteSlash(graphics, headX, y, headSize, uiAlpha);
            }

            if (mouseX >= headX - 2 && mouseX < headX + headSize + 2
                    && mouseY >= y - 2 && mouseY < y + headSize + 2) {
                hoveredName = participant.name();
            }
        }

        if (hoveredName != null && !hoveredName.isBlank()) {
            graphics.renderTooltip(Minecraft.getInstance().font,
                    Component.literal(hoveredName), mouseX, mouseY);
        }
    }

    private static void renderDetachedRoster(GuiGraphics graphics,
            DetachedLayout layout, int mouseX, int mouseY, float uiAlpha) {
        List<DeathVoiceRosterPacket.Participant> roster = orderedRoster();
        if (roster.isEmpty()) return;

        int count = Math.min(layout.maxVisible(), roster.size());
        String hoveredName = null;
        for (int i = 0; i < count; i++) {
            DeathVoiceRosterPacket.Participant participant = roster.get(i);
            int y = layout.deadBaseY()
                    - i * (layout.headSize() + HEAD_GAP);
            if (y < layout.top()) break;

            boolean muted = isParticipantMuted(participant.id());
            boolean talking = !muted && isParticipantTalking(participant.id());
            float portraitBrightness = talking ? 1.0F
                    : muted ? DEAD_CALL_MUTED_ALPHA / 255.0F
                    : DEAD_CALL_IDLE_ALPHA / 255.0F;

            graphics.fill(layout.headX() - 2, y - 2,
                    layout.headX() + layout.headSize() + 2,
                    y + layout.headSize() + 2,
                    withAlpha(talking ? TALKING_BORDER : IDLE_BORDER, uiAlpha));
            graphics.fill(layout.headX() - 1, y - 1,
                    layout.headX() + layout.headSize() + 1,
                    y + layout.headSize() + 1,
                    withAlpha(0xC006090C, uiAlpha));

            HeadTexture texture = headTexture(participant);
            renderHead(graphics, texture, layout.headX(), y,
                    layout.headSize(), uiAlpha);
            if (portraitBrightness < 0.999F) {
                graphics.fill(layout.headX(), y,
                        layout.headX() + layout.headSize(),
                        y + layout.headSize(),
                        withAlpha(0xFF06090C,
                                (1.0F - portraitBrightness) * uiAlpha));
            }
            if (muted) {
                drawMuteSlash(graphics, layout.headX(), y,
                        layout.headSize(), uiAlpha);
            }
            if (mouseX >= layout.headX() - 2
                    && mouseX < layout.headX() + layout.headSize() + 2
                    && mouseY >= y - 2
                    && mouseY < y + layout.headSize() + 2) {
                hoveredName = participant.name();
            }
        }

        if (hoveredName != null && !hoveredName.isBlank()) {
            graphics.renderTooltip(Minecraft.getInstance().font,
                    Component.literal(hoveredName), mouseX, mouseY);
        }
    }

    private static List<DeathVoiceRosterPacket.Participant> orderedRoster() {
        List<DeathVoiceRosterPacket.Participant> source =
                DeathVoiceRosterClient.participants();
        if (source.isEmpty()) return List.of();

        Minecraft minecraft = Minecraft.getInstance();
        UUID self = minecraft.player == null ? null : minecraft.player.getUUID();
        List<DeathVoiceRosterPacket.Participant> ordered = new ArrayList<>(source.size());
        if (self != null) {
            for (DeathVoiceRosterPacket.Participant participant : source) {
                if (self.equals(participant.id())) {
                    ordered.add(participant);
                    break;
                }
            }
        }
        for (DeathVoiceRosterPacket.Participant participant : source) {
            if (self == null || !self.equals(participant.id())) {
                ordered.add(participant);
            }
        }
        return ordered;
    }

    private static void renderOwnMicrophone(GuiGraphics graphics,
            Bounds feed, float uiAlpha) {
        renderOwnMicrophoneAt(graphics, feed.left + 8,
                feed.bottom - MIC_BOX_SIZE - 8, uiAlpha);
    }

    private static void renderOwnMicrophoneAt(GuiGraphics graphics,
            int x, int y, float uiAlpha) {
        boolean muted = localMuted() || localVoiceUnavailable();
        boolean whispering = !muted && localWhispering();
        boolean talking = !muted && localTalking();
        ResourceLocation icon = muted ? MICROPHONE_OFF
                : whispering ? MICROPHONE_WHISPER : MICROPHONE;

        int border = muted ? MUTED_RED
                : talking ? TALKING_BORDER : 0x9A596169;
        graphics.fill(x, y, x + MIC_BOX_SIZE, y + MIC_BOX_SIZE,
                withAlpha(0xB006090C, uiAlpha));
        graphics.fill(x, y, x + MIC_BOX_SIZE, y + 1,
                withAlpha(border, uiAlpha));
        graphics.fill(x, y + MIC_BOX_SIZE - 1,
                x + MIC_BOX_SIZE, y + MIC_BOX_SIZE,
                withAlpha(border, uiAlpha));
        graphics.fill(x, y, x + 1, y + MIC_BOX_SIZE,
                withAlpha(border, uiAlpha));
        graphics.fill(x + MIC_BOX_SIZE - 1, y,
                x + MIC_BOX_SIZE, y + MIC_BOX_SIZE,
                withAlpha(border, uiAlpha));

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F,
                uiAlpha * (talking ? 1.0F : 0.86F));
        int iconX = x + (MIC_BOX_SIZE - MIC_SIZE) / 2;
        int iconY = y + (MIC_BOX_SIZE - MIC_SIZE) / 2;
        graphics.blit(icon, iconX, iconY, MIC_SIZE, MIC_SIZE,
                0.0F, 0.0F, MIC_SIZE, MIC_SIZE, MIC_SIZE, MIC_SIZE);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
    }

    private static void renderHead(GuiGraphics graphics, HeadTexture skin,
            int x, int y, int size, float alpha) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F,
                Mth.clamp(alpha, 0.0F, 1.0F));
        graphics.blit(skin.location(), x, y, size, size,
                8.0F, 8.0F, 8, 8, skin.width(), skin.height());
        graphics.blit(skin.location(), x, y, size, size,
                40.0F, 8.0F, 8, 8, skin.width(), skin.height());
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
    }

    private static void drawMuteSlash(GuiGraphics graphics, int x, int y,
            int size, float alpha) {
        int color = withAlpha(MUTED_RED, alpha);
        for (int i = 0; i < size; i++) {
            int px = x + i;
            int py = y + size - 1 - i;
            graphics.fill(px, py, Math.min(x + size, px + 2),
                    Math.min(y + size, py + 2), color);
        }
    }

    private static HeadTexture headTexture(
            DeathVoiceRosterPacket.Participant participant) {
        HeadTexture override = customSkin(participant.skinOverride());
        if (override != null) return override;

        Minecraft minecraft = Minecraft.getInstance();
        PlayerInfo info = minecraft.getConnection() == null ? null
                : minecraft.getConnection().getPlayerInfo(participant.id());
        if (info != null) {
            return new HeadTexture(info.getSkinLocation(), 64, 64);
        }
        return new HeadTexture(
                DefaultPlayerSkin.getDefaultSkin(participant.id()), 64, 64);
    }

    private static HeadTexture customSkin(String fileName) {
        if (fileName == null || fileName.isBlank()
                || !ModList.get().isLoaded(KLEIDERS_MOD_ID)) {
            return null;
        }
        Path path = Scp914SkinManager.resolveSkin(fileName);
        if (path == null) return null;

        try {
            long modified = Files.getLastModifiedTime(path).toMillis();
            CachedSkin existing = CUSTOM_SKINS.get(fileName);
            if (existing != null && existing.modified() == modified) {
                return existing.texture();
            }

            NativeImage image;
            try (InputStream stream = Files.newInputStream(path)) {
                image = NativeImage.read(stream);
            }
            int width = image.getWidth();
            int height = image.getHeight();
            if (width != 64 || (height != 64 && height != 32)) {
                image.close();
                return null;
            }

            String hash = Integer.toUnsignedString((fileName.toLowerCase()
                    + ":" + modified).hashCode(), 16);
            ResourceLocation id = new ResourceLocation(ScpClassifiedDirectiveMod.MODID,
                    "dynamic/death_voice_heads/" + hash);
            Minecraft.getInstance().getTextureManager().register(
                    id, new DynamicTexture(image));
            HeadTexture texture = new HeadTexture(id, width, height);
            CUSTOM_SKINS.put(fileName, new CachedSkin(modified, texture));
            return texture;
        } catch (Exception exception) {
            return null;
        }
    }

    private static boolean isParticipantTalking(UUID playerId) {
        if (playerId == null) return false;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null && playerId.equals(minecraft.player.getUUID())) {
            return localTalking();
        }

        // Dead-player voice is routed through a deterministic static channel.
        // Query that channel directly because SVC 2.6.20's public remote
        // isTalking(UUID) implementation discards the TalkCache return value.
        return talkCacheTalking(deadCallChannel(playerId))
                || talkCacheTalking(playerId);
    }

    private static boolean isParticipantMuted(UUID playerId) {
        if (playerId == null) return false;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null && playerId.equals(minecraft.player.getUUID())) {
            return localMuted() || localVoiceUnavailable();
        }
        return playerVolumeMuted(playerId) || remoteUnavailable(playerId);
    }

    private static UUID deadCallChannel(UUID speaker) {
        return UUID.nameUUIDFromBytes((ScpClassifiedDirectiveMod.MODID
                + ":dead_voice:" + speaker).getBytes(StandardCharsets.UTF_8));
    }

    private static boolean talkCacheTalking(UUID id) {
        try {
            Object client = invokeStatic(
                    "de.maxhenkel.voicechat.voice.client.ClientManager",
                    "getClient");
            if (client == null) return false;
            Object cache = client.getClass().getMethod("getTalkCache")
                    .invoke(client);
            return Boolean.TRUE.equals(cache.getClass()
                    .getMethod("isTalking", UUID.class).invoke(cache, id));
        } catch (ReflectiveOperationException | LinkageError
                | RuntimeException exception) {
            logReflectionFailure(exception);
            return false;
        }
    }

    private static boolean localTalking() {
        try {
            Object client = invokeStatic(
                    "de.maxhenkel.voicechat.voice.client.ClientManager",
                    "getClient");
            if (client == null) return false;
            Object mic = client.getClass().getMethod("getMicThread").invoke(client);
            return mic != null && Boolean.TRUE.equals(mic.getClass()
                    .getMethod("isTalking").invoke(mic));
        } catch (ReflectiveOperationException | LinkageError
                | RuntimeException exception) {
            logReflectionFailure(exception);
            return false;
        }
    }

    private static boolean localWhispering() {
        try {
            Object client = invokeStatic(
                    "de.maxhenkel.voicechat.voice.client.ClientManager",
                    "getClient");
            if (client == null) return false;
            Object mic = client.getClass().getMethod("getMicThread").invoke(client);
            return mic != null && Boolean.TRUE.equals(mic.getClass()
                    .getMethod("isWhispering").invoke(mic));
        } catch (ReflectiveOperationException | LinkageError
                | RuntimeException exception) {
            logReflectionFailure(exception);
            return false;
        }
    }

    private static Object playerStateManager()
            throws ReflectiveOperationException {
        return invokeStatic(
                "de.maxhenkel.voicechat.voice.client.ClientManager",
                "getPlayerStateManager");
    }

    private static boolean localMuted() {
        try {
            Object manager = playerStateManager();
            return manager != null && Boolean.TRUE.equals(manager.getClass()
                    .getMethod("isMuted").invoke(manager));
        } catch (ReflectiveOperationException | LinkageError
                | RuntimeException exception) {
            logReflectionFailure(exception);
            return false;
        }
    }

    private static boolean localVoiceUnavailable() {
        try {
            Object manager = playerStateManager();
            if (manager == null) return true;
            boolean disabled = Boolean.TRUE.equals(manager.getClass()
                    .getMethod("isDisabled").invoke(manager));
            boolean disconnected = Boolean.TRUE.equals(manager.getClass()
                    .getMethod("isDisconnected").invoke(manager));
            return disabled || disconnected;
        } catch (ReflectiveOperationException | LinkageError
                | RuntimeException exception) {
            logReflectionFailure(exception);
            return false;
        }
    }

    private static boolean remoteUnavailable(UUID id) {
        try {
            Object manager = playerStateManager();
            if (manager == null) return false;
            boolean disabled = Boolean.TRUE.equals(manager.getClass()
                    .getMethod("isPlayerDisabled", UUID.class)
                    .invoke(manager, id));
            boolean disconnected = Boolean.TRUE.equals(manager.getClass()
                    .getMethod("isPlayerDisconnected", UUID.class)
                    .invoke(manager, id));
            return disabled || disconnected;
        } catch (ReflectiveOperationException | LinkageError
                | RuntimeException exception) {
            logReflectionFailure(exception);
            return false;
        }
    }

    private static boolean playerVolumeMuted(UUID id) {
        try {
            Class<?> clientClass = Class.forName(
                    "de.maxhenkel.voicechat.VoicechatClient", false,
                    SimpleVoiceChatDeathScreenUi.class.getClassLoader());
            Field field = clientClass.getField("PLAYER_VOLUME_CONFIG");
            Object config = field.get(null);
            if (config == null) return false;
            Method getVolume = config.getClass().getMethod(
                    "getVolume", Object.class);
            Object value = getVolume.invoke(config, id);
            return value instanceof Number number
                    && number.doubleValue() <= 0.0001D;
        } catch (ReflectiveOperationException | LinkageError
                | RuntimeException exception) {
            logReflectionFailure(exception);
            return false;
        }
    }

    private static void toggleLocalMute() {
        try {
            Object manager = playerStateManager();
            if (manager == null) return;
            Method isMuted = manager.getClass().getMethod("isMuted");
            boolean muted = Boolean.TRUE.equals(isMuted.invoke(manager));
            manager.getClass().getMethod("setMuted", boolean.class)
                    .invoke(manager, !muted);
        } catch (ReflectiveOperationException | LinkageError
                | RuntimeException exception) {
            logReflectionFailure(exception);
        }
    }

    private static Object invokeStatic(String className, String methodName)
            throws ReflectiveOperationException {
        Class<?> type = Class.forName(className, false,
                SimpleVoiceChatDeathScreenUi.class.getClassLoader());
        return type.getMethod(methodName).invoke(null);
    }

    private static void logReflectionFailure(Throwable exception) {
        if (reflectionFailureLogged) return;
        reflectionFailureLogged = true;
        ScpClassifiedDirectiveMod.LOGGER.warn(
                "Simple Voice Chat client presentation API changed; some death voice indicators may be unavailable",
                exception);
    }

    static DetachedLayout detachedLayout(ScpDeathScreen screen) {
        if (screen == null) return null;
        Bounds feed = feed(screen);
        int cardRight = finalCardRight(screen);
        int railWidth = feed.left - cardRight;
        int headSize = railWidth >= HEAD_MIN_SIZE + 16
                ? Mth.clamp(railWidth - 18, HEAD_MIN_SIZE, HEAD_MAX_SIZE)
                : HEAD_MAX_SIZE;

        int feedHeadX = feed.left - headSize - 9;
        if (feedHeadX < cardRight + 7) {
            feedHeadX = cardRight + Math.max(5,
                    (railWidth - headSize) / 2);
        }
        int feedDeadBaseY = feed.bottom - headSize - 7;
        int feedMicX = feed.left + 8;
        int feedMicY = feed.bottom - MIC_BOX_SIZE - 8;

        int cornerHeadX = DETACHED_MARGIN + DETACHED_ICON_RESERVE;
        int cornerMicX = cornerHeadX + (headSize - MIC_BOX_SIZE) / 2;
        int cornerMicY = screen.height - DETACHED_MARGIN - MIC_BOX_SIZE;
        int cornerDeadBaseY = cornerMicY - DETACHED_MIC_GAP - headSize;

        // The death report uses this same 520 ms transition when it returns from
        // the spectate layout to center, so the call rail physically travels with
        // that layout change instead of teleporting to its fallback corner.
        float transition = Mth.clamp(
                1.0F - MineZeroClientState.spectateLayoutProgress(),
                0.0F, 1.0F);
        int headX = Mth.lerpInt(transition, feedHeadX, cornerHeadX);
        int deadBaseY = Mth.lerpInt(transition,
                feedDeadBaseY, cornerDeadBaseY);
        int micX = Mth.lerpInt(transition, feedMicX, cornerMicX);
        int micY = Mth.lerpInt(transition, feedMicY, cornerMicY);
        int top = Mth.lerpInt(transition, feed.top, DETACHED_MARGIN);
        int maxVisible = Math.max(1,
                (Math.max(0, deadBaseY - top) + HEAD_GAP)
                        / (headSize + HEAD_GAP) + 1);
        return new DetachedLayout(headX, deadBaseY, headSize, top,
                micX, micY, maxVisible);
    }

    private static int finalCardRight(ScpDeathScreen screen) {
        int cardWidth = screen.width < 700
                ? Mth.clamp(Math.round(screen.width * 0.52F), 320, 410)
                : Mth.clamp(Math.round(screen.width * 0.42F), 340, 470);
        int left = Math.max(20, Math.round(screen.width * 0.035F));
        return left + cardWidth;
    }

    private static Bounds feed(ScpDeathScreen screen) {
        int left = Math.max(screen.width / 2 + 28,
                Math.round(screen.width * 0.53F));
        int right = Math.max(left + 120, screen.width - 28);
        int top = Math.max(46, Math.round(screen.height * 0.095F));
        int bottom = Math.max(top + 100, screen.height - 64);
        return new Bounds(left, top, right, bottom);
    }

    private static int withAlpha(int color, float alpha) {
        int source = color >>> 24;
        int result = Mth.clamp(Math.round(source
                * Mth.clamp(alpha, 0.0F, 1.0F)), 0, 255);
        return result << 24 | color & 0x00FFFFFF;
    }

    static record DetachedLayout(int headX, int deadBaseY, int headSize,
            int top, int micX, int micY, int maxVisible) {
    }

    private record Bounds(int left, int top, int right, int bottom) {
    }

    private record HeadTexture(ResourceLocation location, int width, int height) {
    }

    private record CachedSkin(long modified, HeadTexture texture) {
    }
}
