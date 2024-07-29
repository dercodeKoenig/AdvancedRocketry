package zmaster587.advancedRocketry.inventory.modules;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;
import zmaster587.libVulpes.gui.CommonResources;
import zmaster587.libVulpes.inventory.modules.ModuleBase;

import javax.annotation.Nonnull;

public class ModuleBrokenPart extends ModuleBase {

    private final ItemStack part;
    private final World world;

    public ModuleBrokenPart(final int offsetX, final int offsetY, @Nonnull ItemStack part, World world) {
        super(offsetX, offsetY);
        this.part = part;
        this.world = world;
    }

    @SideOnly(Side.CLIENT)
    public void renderBackground(GuiContainer gui, int x, int y, int mouseX, int mouseY, FontRenderer font) {
        // render stack

        TextureManager textureManager = Minecraft.getMinecraft().getTextureManager();
        textureManager.bindTexture(CommonResources.genericBackground);
        gui.drawTexturedModalRect(x + this.offsetX - 1, y + this.offsetY - 1, 176, 0, 18, 18);
        int relativeX = x + this.offsetX;
        int relativeY = y + this.offsetY;
        int zLevel = 500;

        GL11.glPushMatrix();
        RenderHelper.disableStandardItemLighting();
        RenderHelper.enableGUIStandardItemLighting();

        GL11.glTranslatef(relativeX, relativeY, zLevel);
        Minecraft.getMinecraft().getRenderItem().renderItemIntoGUI(part, 0, 0);
        Minecraft.getMinecraft().getRenderItem().renderItemOverlayIntoGUI(font, part, 0, 0, "");

        RenderHelper.disableStandardItemLighting();
        GL11.glPopMatrix();
    }
}
