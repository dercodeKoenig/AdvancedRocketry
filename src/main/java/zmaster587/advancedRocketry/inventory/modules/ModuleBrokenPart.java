package zmaster587.advancedRocketry.inventory.modules;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;
import zmaster587.libVulpes.LibVulpes;
import zmaster587.libVulpes.gui.CommonResources;
import zmaster587.libVulpes.inventory.modules.ModuleBase;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.List;

public class ModuleBrokenPart extends ModuleBase {

    private final ItemStack part;

    public ModuleBrokenPart(final int offsetX, final int offsetY, @Nonnull ItemStack part) {
        super(offsetX, offsetY);
        this.part = part;
        this.sizeX = 18;
        this.sizeY = 18;
    }

    @SideOnly(Side.CLIENT)
    @Override
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

    private boolean isMouseOver(int mouseX, int mouseY) {
        int relativeX = mouseX - this.offsetX;
        int relativeY = mouseY - this.offsetY;
        return relativeX > 0 && relativeX < this.sizeX && relativeY > 0 && relativeY < this.sizeY;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void renderToolTip(final int guiOffsetX, final int guiOffsetY, final int mouseX, final int mouseY, final float zLevel, final GuiContainer gui, final FontRenderer font) {
        super.renderToolTip(guiOffsetX, guiOffsetY, mouseX, mouseY, zLevel, gui, font);

        if (this.part != null && this.isMouseOver(mouseX, mouseY)) {
            List<String> list = Arrays.asList(
                    LibVulpes.proxy.getLocalizedString(this.part.getUnlocalizedName() + ".name"),
                    LibVulpes.proxy.getLocalizedString("msg.brokenstage.text") + ": " + this.part.getItemDamage() / 3
            );
            this.drawTooltip(gui, list, mouseX, mouseY, zLevel, font);
        }
    }
}
