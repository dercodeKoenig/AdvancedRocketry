package zmaster587.advancedRocketry.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.lwjgl.opengl.GL11;

public final class GraphicsHelper {

    public static void drawCenteredScaledString(String text, int x, int y, float scale, int color) {
        drawCenteredScaledString(Minecraft.getMinecraft().fontRenderer, text, x, y, scale, color);
    }

    public static void drawCenteredScaledString(FontRenderer fontRenderer, String text, int x, int y, float scale, int color) {
        GlStateManager.pushMatrix();
        GlStateManager.scale(scale, scale, 1.0F);
        drawCenteredString(fontRenderer, text, (int) (x / scale), (int) (y / scale), color);
        GlStateManager.popMatrix();
    }

    public static void drawScaledString(String text, int x, int y, float scale, int color) {
        drawScaledString(Minecraft.getMinecraft().fontRenderer, text, x, y, scale, color);
    }

    public static void drawScaledString(FontRenderer fontRenderer, String text, int x, int y, float scale, int color) {
        GlStateManager.pushMatrix();
        GlStateManager.scale(scale, scale, 1.0F);
        drawString(fontRenderer, text, (int) (x / scale), (int) (y / scale), color);
        GlStateManager.popMatrix();
    }

    public static void drawCenteredString(String text, int x, int y, int color) {
        drawCenteredString(Minecraft.getMinecraft().fontRenderer, text, x, y, color);
    }

    public static void drawCenteredString(FontRenderer fontRenderer, String text, int x, int y, int color) {
        drawString(text, x - fontRenderer.getStringWidth(text) / 2, y, color);
    }

    public static void drawString(String text, int x, int y, int color) {
        drawString(Minecraft.getMinecraft().fontRenderer, text, x, y, color);
    }

    public static void drawString(FontRenderer fontRenderer, String text, int x, int y, int color) {
        fontRenderer.drawString(text, x, y, color);
    }

    /**
     * Represents the GUI adaptation of glScissor mechanism
     * Given coordinates are from up-left corner
     *
     * @param x      start X coordinate
     * @param y      start Y coordinate
     * @param width  new window width
     * @param height new window height
     */
    public static void glScissor(int x, int y, int width, int height) {
        GL11.glScissor(x, Minecraft.getMinecraft().displayHeight - (y + height), width, height);
    }

    public static void drawTexturedModalRect(int x, int y, int width, int height, int u, int v, int textureWidth, int textureHeight, int textureSizeX, int textureSizeY, float zLevel) {
        float f = 1.0F / textureSizeX;
        float f1 = 1.0F / textureSizeY;

        GlStateManager.enableTexture2D();

        Tessellator tess = Tessellator.getInstance();
        BufferBuilder bb = tess.getBuffer();
        bb.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);

        bb.pos(x, y + height, zLevel).tex(u * f, (v + textureHeight) * f1).endVertex();
        bb.pos(x + width, y + height, zLevel).tex((u + textureWidth) * f, (v + textureHeight) * f1).endVertex();
        bb.pos(x + width, y, zLevel).tex((u + textureWidth) * f, v * f1).endVertex();
        bb.pos(x, y, zLevel).tex(u * f, v * f1).endVertex();

        tess.draw();
    }

    public static void drawTexturedModalRect(int x, int y, int textureX, int textureY, int width, int height, float zLevel) {
        drawTexturedModalRect(x, y, width, height, textureX, textureY, width, height, 256, 256, zLevel);
    }

    public static void drawColoredModalRect(int x, int y, int width, int height, float r, float g, float b, float a, float zLevel) {
        drawColoredModalRect(x, y, width, height, (int) (r * 255), (int) (g * 255), (int) (b * 255), (int) (a * 255), zLevel);
    }

    public static void drawColoredModalRect(int x, int y, int width, int height, int r, int g, int b, int a, float zLevel) {
        Tessellator tess = Tessellator.getInstance();
        BufferBuilder bb = tess.getBuffer();
        bb.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);

        bb.pos(x, y + height, zLevel).color(r, g, b, a).endVertex();
        bb.pos(x + width, y + height, zLevel).color(r, g, b, a).endVertex();
        bb.pos(x + width, y, zLevel).color(r, g, b, a).endVertex();
        bb.pos(x, y, zLevel).color(r, g, b, a).endVertex();

        tess.draw();
    }
}
