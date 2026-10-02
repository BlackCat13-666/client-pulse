package com.example.clientpulse;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import java.util.Locale;

public final class ClientPulseClient implements ClientModInitializer {
    public static final String MOD_ID = "clientpulse";

    private static final Identifier HUD_ID =
            Identifier.fromNamespaceAndPath(MOD_ID, "hud");

    private static final KeyMapping TOGGLE =
            KeyMappingHelper.registerKeyMapping(new KeyMapping(
                    "key.clientpulse.toggle_hud",
                    GLFW.GLFW_KEY_H,
                    KeyMapping.Category.register(
                            Identifier.fromNamespaceAndPath(MOD_ID, "main"))
            ));

    private static final KeyMapping THEME =
            KeyMappingHelper.registerKeyMapping(new KeyMapping(
                    "key.clientpulse.next_theme",
                    GLFW.GLFW_KEY_J,
                    KeyMapping.Category.register(
                            Identifier.fromNamespaceAndPath(MOD_ID, "main"))
            ));

    private static boolean enabled = true;
    private static int theme = 0;
    private static long leftClickStart;
    private static int leftClicks;

    private static final int[][] THEMES = {
            {0xFF55CCFF, 0xFFAA77FF},
            {0xFF55FFAA, 0xFF22BBFF},
            {0xFFFFAA55, 0xFFFF55AA},
            {0xFFFFFF66, 0xFFFF7777},
            {0xFFE0E0E0, 0xFF8888FF}
    };

    @Override
    public void onInitializeClient() {
        HudElementRegistry.attachElementBefore(
                VanillaHudElements.CHAT,
                HUD_ID,
                ClientPulseClient::render
        );
    }

    private static void render(GuiGraphicsExtractor g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();

        while (TOGGLE.consumeClick()) enabled = !enabled;
        while (THEME.consumeClick()) theme = (theme + 1) % THEMES.length;

        if (!enabled || mc.player == null || mc.level == null) return;

        LocalPlayer p = mc.player;
        long now = System.currentTimeMillis();

        if (mc.mouseHandler.isLeftPressed()) {
            if (now - leftClickStart > 90) {
                leftClicks++;
                leftClickStart = now;
            }
        } else if (now - leftClickStart > 1000) {
            leftClicks = 0;
        }

        int primary = animatedColor(THEMES[theme][0], THEMES[theme][1], now);
        int secondary = animatedColor(THEMES[theme][1], THEMES[theme][0], now + 300);

        int x = 7, y = 7;
        int width = 190, height = 128;

        g.fill(x + 2, y + 3, x + width + 2, y + height + 3, 0x30000000);
        g.fill(x, y, x + width, y + height, 0xC00C0D12);
        g.fill(x, y, x + width, y + 2, primary);
        g.fill(x, y + 2, x + 3, y + height, secondary);

        g.drawString(mc.font, "CLIENTPULSE", x + 9, y + 7, 0xFFFFFFFF, true);

        int row = y + 22;
        g.drawString(mc.font, "FPS", x + 9, row, 0xFF9EA5B5, false);
        g.drawString(mc.font, Integer.toString(mc.getFps()), x + 45, row, primary, true);

        g.drawString(mc.font, "CPS", x + 78, row, 0xFF9EA5B5, false);
        g.drawString(mc.font, Integer.toString(leftClicks), x + 111, row, secondary, true);

        int px = p.blockPosition().getX();
        int py = p.blockPosition().getY();
        int pz = p.blockPosition().getZ();
        g.drawString(mc.font, "XYZ", x + 9, row + 14, 0xFF9EA5B5, false);
        g.drawString(mc.font, px + "  " + py + "  " + pz,
                x + 45, row + 14, 0xFFE7E9EF, false);

        String facing = p.getDirection().getName().toUpperCase(Locale.ROOT);
        g.drawString(mc.font, "FACING", x + 9, row + 28, 0xFF9EA5B5, false);
        g.drawString(mc.font, facing, x + 55, row + 28, primary, false);

        g.drawString(mc.font, "SPEED", x + 9, row + 42, 0xFF9EA5B5, false);
        double speed = Math.sqrt(p.getDeltaMovement().x * p.getDeltaMovement().x
                + p.getDeltaMovement().z * p.getDeltaMovement().z) * 20.0;
        g.drawString(mc.font, String.format(Locale.ROOT, "%.2f b/s", speed),
                x + 55, row + 42, 0xFFE7E9EF, false);

        g.drawString(mc.font, "ARMOR", x + 9, row + 58, 0xFF9EA5B5, false);
        for (int i = 0; i < 4; i++) {
            ItemStack stack = p.getInventory().getArmor(i);
            int bx = x + 51 + i * 33;
            int by = row + 58;
            g.fill(bx, by + 1, bx + 27, by + 4, 0xFF24262D);

            if (!stack.isEmpty() && stack.isDamageableItem()) {
                int max = stack.getMaxDamage();
                int remaining = max - stack.getDamageValue();
                int fill = Math.max(1, 27 * remaining / max);
                int c = remaining * 100 / max > 50 ? primary : 0xFFFF5555;
                g.fill(bx, by + 1, bx + fill, by + 4, c);
            }
        }

        int cx = mc.getWindow().getGuiScaledWidth() / 2;
        int cy = mc.getWindow().getGuiScaledHeight() / 2;
        g.fill(cx - 1, cy - 5, cx + 1, cy + 6, primary);
        g.fill(cx - 5, cy - 1, cx + 6, cy + 1, primary);

        g.drawString(mc.font, "THEME " + (theme + 1) + "/5",
                mc.getWindow().getGuiScaledWidth() - 65,
                mc.getWindow().getGuiScaledHeight() - 14,
                secondary, false);
    }

    private static int animatedColor(int a, int b, long time) {
        double phase = (Math.sin((time % 2400L) / 2400.0 * Math.PI * 2.0) + 1.0) * 0.5;
        int ar = (a >> 16) & 255, ag = (a >> 8) & 255, ab = a & 255;
        int br = (b >> 16) & 255, bg = (b >> 8) & 255, bb = b & 255;
        int r = (int) (ar + (br - ar) * phase);
        int g = (int) (ag + (bg - ag) * phase);
        int bl = (int) (ab + (bb - ab) * phase);
        return 0xFF000000 | (r << 16) | (g << 8) | bl;
    }

    private static String formatTicks(int ticks) {
        int seconds = Math.max(0, ticks / 20);
        return String.format(Locale.ROOT, "%d:%02d", seconds / 60, seconds % 60);
    }
}
