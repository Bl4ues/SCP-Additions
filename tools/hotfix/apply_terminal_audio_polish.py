from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
SCREEN = ROOT / "src/main/java/net/mcreator/scpadditions/client/FacilityDiagnosticsScreen.java"
AUX_BLOCK = ROOT / "src/main/java/net/mcreator/scpadditions/block/Scp079AuxiliaryPowerBlock.java"
FONT_JSON = ROOT / "src/main/resources/assets/scp_additions/font/scipnet_terminal.json"
LOOP_SOUND = ROOT / "src/main/java/net/mcreator/scpadditions/client/AuxiliaryGeneratorLoopSound.java"
AUDIO_CLIENT = ROOT / "src/main/java/net/mcreator/scpadditions/client/AuxiliaryGeneratorAudioClient.java"


def replace_once(text: str, old: str, new: str, label: str) -> str:
    count = text.count(old)
    if count != 1:
        raise RuntimeError(f"{label}: expected exactly one match, found {count}")
    return text.replace(old, new, 1)


screen = SCREEN.read_text(encoding="utf-8")
screen = replace_once(
    screen,
    '    private static final ResourceLocation TERMINAL_LOGO = new ResourceLocation(\n'
    '            "scp_additions", "textures/screens/terminallogo.png");\n',
    '    private static final ResourceLocation TERMINAL_LOGO = new ResourceLocation(\n'
    '            "scp_additions", "textures/screens/terminallogo.png");\n'
    '    private static final ResourceLocation TERMINAL_TEXT = new ResourceLocation(\n'
    '            "scp_additions", "scipnet_terminal");\n',
    "terminal font resource",
)
screen = replace_once(screen,
    '        int leftWidth = Math.max(168, width * 36 / 100);\n',
    '        int leftWidth = Math.max(190, width * 40 / 100);\n',
    "dashboard column balance")
screen = replace_once(screen,
    '        int logY = controlY + 66;\n',
    '        int logY = controlY + 102;\n',
    "system log position")

operations_start = screen.index('    private void renderOperationsPanel(')
operations_end = screen.index('    private void renderSystemLog(', operations_start)
new_operations = '''    private void renderOperationsPanel(GuiGraphics graphics, int mouseX,
            int mouseY, int x, int y, int width, boolean powered) {
        int height = 92;
        graphics.fill(x, y, x + width, y + height, PANEL);
        border(graphics, x, y, width, height, DIM_BLUE);
        graphics.fill(x, y, x + 4, y + height, FOUNDATION_RED);

        drawHeading(graphics, "SCiPNET OPERATIONS", x + 10, y + 7,
                OFF_WHITE);

        int cooldown = cooldownRemainingTicks();
        String powerState = powered ? "ONLINE" : "OFFLINE";
        int powerColor = powered ? SIGNAL_GOLD : FOUNDATION_RED;
        String cacheState = !powered ? "UNAVAILABLE"
                : cooldown > 0 ? "LOCKOUT " + formatCooldown(cooldown)
                : resetRequested ? "PURGE REQUESTED" : "READY";
        int cacheColor = !powered ? METAL_GRAY
                : cooldown > 0 ? SIGNAL_GOLD : OFF_WHITE;

        int midpoint = x + width / 2;
        drawBody(graphics, "AUXILIARY BUS", x + 10, y + 24, METAL_GRAY);
        drawBody(graphics, powerState, x + 92, y + 24, powerColor);
        drawBody(graphics, "SESSION CACHE", midpoint + 8, y + 24,
                METAL_GRAY);
        rightAligned(graphics, body(cacheState), x + width - 10, y + 24,
                cacheColor);

        int buttonWidth = Math.max(190, Math.min(232, width * 46 / 100));
        resetX = x + (width - buttonWidth) / 2;
        resetY = y + 38;
        resetWidth = buttonWidth;
        boolean enabled = powered && cooldown <= 0 && !resetRequested;
        boolean hovered = enabled && inside(mouseX, mouseY, resetX, resetY,
                resetWidth, RESET_HEIGHT);
        graphics.fill(resetX, resetY, resetX + resetWidth,
                resetY + RESET_HEIGHT, hovered ? BUTTON_HOVER : BUTTON);
        border(graphics, resetX, resetY, resetWidth, RESET_HEIGHT,
                hovered ? FOUNDATION_RED : DIM_BLUE);

        String label = !powered ? "PURGE UNAVAILABLE"
                : cooldown > 0 ? "CACHE LOCKOUT ACTIVE"
                : resetRequested ? "PURGE REQUESTED"
                : "PURGE SESSION CACHE";
        centeredBody(graphics, label, resetX, resetY, resetWidth,
                RESET_HEIGHT, enabled ? OFF_WHITE : METAL_GRAY);

        centeredBody(graphics,
                "WARNING // PURGE TERMINATES ACTIVE REMOTE MAINTENANCE SESSIONS",
                x + 8, y + 66, width - 16, 10, FOUNDATION_RED);
        centeredBody(graphics,
                "CURRENT ACCESS TOKENS WILL BE INVALIDATED.",
                x + 8, y + 78, width - 16, 10, MUTED_BLUE);
    }

'''
screen = screen[:operations_start] + new_operations + screen[operations_end:]

screen = replace_once(screen,
    '        drawBody(graphics, "BUFFER 03", x + width - 58, y + 7, MUTED_BLUE);\n',
    '        rightAligned(graphics, body("BUFFER 03"), x + width - 9,\n'
    '                y + 7, MUTED_BLUE);\n',
    "system log buffer alignment")
screen = replace_once(screen,
    '        drawBody(graphics, "ARC48:SCIPNET>", x + 9, y + 74, MUTED_BLUE);\n'
    '        if ((Util.getMillis() / 500L) % 2L == 0L) {\n'
    '            graphics.fill(x + 88, y + 75, x + 93, y + 83, METAL_GRAY);\n'
    '        }\n',
    '        Component prompt = body("ARC48:SCIPNET>");\n'
    '        graphics.drawString(font, prompt, x + 9, y + 74, MUTED_BLUE, false);\n'
    '        if ((Util.getMillis() / 500L) % 2L == 0L) {\n'
    '            int cursorX = x + 13 + font.width(prompt);\n'
    '            graphics.fill(cursorX, y + 75, cursorX + 5, y + 83,\n'
    '                    METAL_GRAY);\n'
    '        }\n',
    "system prompt cursor alignment")
screen = replace_once(screen,
    '    private static Component body(String text) {\n'
    '        return ScpFonts.titillium(text);\n'
    '    }\n',
    '    private static Component body(String text) {\n'
    '        return Component.literal(text == null ? "" : text)\n'
    '                .withStyle(style -> style.withFont(TERMINAL_TEXT));\n'
    '    }\n',
    "terminal body font")
screen = replace_once(screen,
    '    private void centered(GuiGraphics graphics, Component text, int x, int y,\n'
    '            int width, int height, int color) {\n'
    '        graphics.drawString(font, text,\n'
    '                x + Math.max(0, (width - font.width(text)) / 2),\n'
    '                y + Math.max(0, (height - font.lineHeight) / 2),\n'
    '                color, false);\n'
    '    }\n',
    '    private void centered(GuiGraphics graphics, Component text, int x, int y,\n'
    '            int width, int height, int color) {\n'
    '        graphics.drawString(font, text,\n'
    '                x + Math.max(0, (width - font.width(text)) / 2),\n'
    '                y + Math.max(0, (height - font.lineHeight) / 2),\n'
    '                color, false);\n'
    '    }\n\n'
    '    private void centeredBody(GuiGraphics graphics, String text, int x,\n'
    '            int y, int width, int height, int color) {\n'
    '        Component component = body(text);\n'
    '        graphics.drawString(font, component,\n'
    '                x + Math.max(0, (width - font.width(component)) / 2),\n'
    '                y + Math.max(0, (height - font.lineHeight) / 2) - 1,\n'
    '                color, false);\n'
    '    }\n',
    "terminal text centering")
SCREEN.write_text(screen, encoding="utf-8")

FONT_JSON.parent.mkdir(parents=True, exist_ok=True)
FONT_JSON.write_text('''{
  "providers": [
    {
      "type": "ttf",
      "file": "scp_additions:titillium_web_regular.ttf",
      "shift": [0, 0],
      "size": 12.0,
      "oversample": 8.0
    },
    {
      "type": "reference",
      "id": "minecraft:default"
    }
  ]
}
''', encoding="utf-8")

block = AUX_BLOCK.read_text(encoding="utf-8")
for unwanted in [
    'import net.minecraftforge.registries.ForgeRegistries;\n\n',
    'import net.minecraft.resources.ResourceLocation;\n',
    'import net.minecraft.sounds.SoundEvent;\n',
    'import net.minecraft.sounds.SoundSource;\n',
    'import net.minecraft.util.RandomSource;\n',
    'import java.util.HashMap;\n',
    'import java.util.Map;\n',
]:
    block = block.replace(unwanted, '')
block = replace_once(block,
    '    private static final Map<Long, Long> AUXILIARY_LOOP_NEXT_TICK =\n'
    '            new HashMap<>();\n\n', '', "legacy auxiliary loop state")
animate_start = block.index('    @Override\n    public void animateTick(')
animate_end = block.index('    @Override\n    public InteractionResult use(', animate_start)
block = block[:animate_start] + block[animate_end:]
AUX_BLOCK.write_text(block, encoding="utf-8")

LOOP_SOUND.write_text('''package net.mcreator.scpadditions.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.mcreator.scpadditions.block.Scp079AuxiliaryPowerBlock;
import net.mcreator.scpadditions.init.ScpAdditionsModSounds;

/** Seamless positional generator loop with quick startup and shutdown fades. */
public final class AuxiliaryGeneratorLoopSound
        extends AbstractTickableSoundInstance {
    private static final float MINIMUM_AUDIBLE_VOLUME = 0.015F;
    private static final float TARGET_VOLUME = 0.22F;
    private static final int FADE_IN_TICKS = 12;
    private static final int FADE_OUT_TICKS = 18;
    private static final double POSITION_LERP = 0.22D;

    private final ClientLevel level;
    private BlockPos target;
    private double targetX;
    private double targetY;
    private double targetZ;
    private boolean stopRequested;
    private boolean finished;

    public AuxiliaryGeneratorLoopSound(ClientLevel level, BlockPos pos) {
        super(ScpAdditionsModSounds.AUXGEN.get(), SoundSource.BLOCKS,
                RandomSource.create());
        this.level = level;
        this.looping = true;
        this.delay = 0;
        this.volume = MINIMUM_AUDIBLE_VOLUME;
        this.pitch = 1.0F;
        this.relative = false;
        this.attenuation = SoundInstance.Attenuation.LINEAR;
        retarget(pos);
        this.x = targetX;
        this.y = targetY;
        this.z = targetZ;
    }

    ClientLevel level() {
        return level;
    }

    void retarget(BlockPos newTarget) {
        this.target = newTarget.immutable();
        this.targetX = target.getX() + 0.5D;
        this.targetY = target.getY() + 0.5D;
        this.targetZ = target.getZ() + 0.5D;
        this.stopRequested = false;
    }

    void requestStop() {
        stopRequested = true;
    }

    @Override
    public void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != level || minecraft.player == null) {
            finish();
            return;
        }

        this.x += (targetX - this.x) * POSITION_LERP;
        this.y += (targetY - this.y) * POSITION_LERP;
        this.z += (targetZ - this.z) * POSITION_LERP;

        boolean powered = !stopRequested && level.hasChunkAt(target)
                && shouldPlayFor(level.getBlockState(target));
        if (powered) {
            float step = (TARGET_VOLUME - MINIMUM_AUDIBLE_VOLUME)
                    / FADE_IN_TICKS;
            this.volume = Mth.approach(this.volume, TARGET_VOLUME, step);
            return;
        }

        float step = TARGET_VOLUME / FADE_OUT_TICKS;
        this.volume = Mth.approach(this.volume, MINIMUM_AUDIBLE_VOLUME, step);
        if (this.volume <= MINIMUM_AUDIBLE_VOLUME + 0.0005F) finish();
    }

    static boolean shouldPlayFor(BlockState state) {
        return state.getBlock() instanceof Scp079AuxiliaryPowerBlock
                && state.hasProperty(Scp079AuxiliaryPowerBlock.POWERED)
                && state.getValue(Scp079AuxiliaryPowerBlock.POWERED);
    }

    public boolean isFinished() {
        return finished;
    }

    public void finish() {
        finished = true;
        stop();
    }
}
''', encoding="utf-8")

AUDIO_CLIENT.write_text('''package net.mcreator.scpadditions.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.mcreator.scpadditions.ScpAdditionsMod;

/** Maintains one seamless positional loop for the nearest powered auxiliary unit. */
@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID, value = Dist.CLIENT)
public final class AuxiliaryGeneratorAudioClient {
    private static final int DISCOVERY_INTERVAL_TICKS = 10;
    private static final int HORIZONTAL_DISCOVERY_RADIUS = 16;
    private static final int VERTICAL_DISCOVERY_RADIUS = 8;

    private static AuxiliaryGeneratorLoopSound activeLoop;
    private static int discoveryTicks;

    private AuxiliaryGeneratorAudioClient() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            stopImmediately();
            discoveryTicks = 0;
            return;
        }

        if (activeLoop != null && activeLoop.isFinished()) activeLoop = null;
        discoveryTicks++;
        if (discoveryTicks < DISCOVERY_INTERVAL_TICKS) return;
        discoveryTicks = 0;

        BlockPos nearest = findNearestPoweredUnit(minecraft.level,
                minecraft.player.getX(), minecraft.player.getY(),
                minecraft.player.getZ(), minecraft.player.blockPosition());
        if (nearest == null) {
            if (activeLoop != null) activeLoop.requestStop();
            return;
        }

        if (activeLoop == null || activeLoop.isFinished()
                || activeLoop.level() != minecraft.level) {
            startLoop(minecraft.level, nearest);
        } else {
            activeLoop.retarget(nearest);
        }
    }

    private static void startLoop(ClientLevel level, BlockPos pos) {
        stopImmediately();
        activeLoop = new AuxiliaryGeneratorLoopSound(level, pos);
        Minecraft.getInstance().getSoundManager().play(activeLoop);
    }

    private static void stopImmediately() {
        if (activeLoop != null) {
            activeLoop.finish();
            activeLoop = null;
        }
    }

    private static BlockPos findNearestPoweredUnit(ClientLevel level,
            double listenerX, double listenerY, double listenerZ,
            BlockPos center) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        BlockPos nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        for (int y = -VERTICAL_DISCOVERY_RADIUS;
                y <= VERTICAL_DISCOVERY_RADIUS; y++) {
            for (int x = -HORIZONTAL_DISCOVERY_RADIUS;
                    x <= HORIZONTAL_DISCOVERY_RADIUS; x++) {
                for (int z = -HORIZONTAL_DISCOVERY_RADIUS;
                        z <= HORIZONTAL_DISCOVERY_RADIUS; z++) {
                    cursor.set(center.getX() + x, center.getY() + y,
                            center.getZ() + z);
                    if (!level.hasChunkAt(cursor)
                            || !AuxiliaryGeneratorLoopSound.shouldPlayFor(
                            level.getBlockState(cursor))) continue;
                    double distance = distanceToCenterSqr(cursor, listenerX,
                            listenerY, listenerZ);
                    if (distance < nearestDistance) {
                        nearestDistance = distance;
                        nearest = cursor.immutable();
                    }
                }
            }
        }
        return nearest;
    }

    private static double distanceToCenterSqr(BlockPos pos, double x,
            double y, double z) {
        double dx = pos.getX() + 0.5D - x;
        double dy = pos.getY() + 0.5D - y;
        double dz = pos.getZ() + 0.5D - z;
        return dx * dx + dy * dy + dz * dz;
    }
}
''', encoding="utf-8")

for relative in [
    ".github/workflows/apply-terminal-audio-polish.yml",
    "tools/hotfix/apply_terminal_audio_polish.py",
    "tools/hotfix/APPLY_TERMINAL_AUDIO_POLISH",
]:
    path = ROOT / relative
    if path.exists():
        path.unlink()
