package zmaster587.advancedRocketry.tile.multiblock.machine;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.SoundEvent;
import zmaster587.advancedRocketry.api.AdvancedRocketryBlocks;
import zmaster587.advancedRocketry.inventory.TextureResources;
import zmaster587.advancedRocketry.util.AudioRegistry;
import zmaster587.libVulpes.block.BlockMeta;
import zmaster587.libVulpes.inventory.modules.IModularInventory;
import zmaster587.libVulpes.inventory.modules.ModuleBase;
import zmaster587.libVulpes.inventory.modules.ModuleProgress;
import zmaster587.libVulpes.tile.multiblock.TileMultiBlock;
import zmaster587.libVulpes.tile.multiblock.TileMultiblockMachine;

import java.util.List;

public class TileElectricArcFurnace extends TileMultiblockMachine implements IModularInventory {


    public static final Object[][][] structure = {
            {{null, null, null, null, null},
                    {null, 'P', AdvancedRocketryBlocks.blockBlastBrick, 'P', null},
                    {null, AdvancedRocketryBlocks.blockBlastBrick, AdvancedRocketryBlocks.blockBlastBrick, AdvancedRocketryBlocks.blockBlastBrick, null},
                    {null, AdvancedRocketryBlocks.blockBlastBrick, 'P', AdvancedRocketryBlocks.blockBlastBrick, null},
                    {null, null, null, null, null},
            },

            {{null, AdvancedRocketryBlocks.blockBlastBrick, AdvancedRocketryBlocks.blockBlastBrick, AdvancedRocketryBlocks.blockBlastBrick, null},
                    {AdvancedRocketryBlocks.blockBlastBrick, "blockCoil", Blocks.AIR, "blockCoil", AdvancedRocketryBlocks.blockBlastBrick},
                    {AdvancedRocketryBlocks.blockBlastBrick, Blocks.AIR, Blocks.AIR, Blocks.AIR, AdvancedRocketryBlocks.blockBlastBrick},
                    {AdvancedRocketryBlocks.blockBlastBrick, Blocks.AIR, "blockCoil", Blocks.AIR, AdvancedRocketryBlocks.blockBlastBrick},
                    {null, AdvancedRocketryBlocks.blockBlastBrick, AdvancedRocketryBlocks.blockBlastBrick, AdvancedRocketryBlocks.blockBlastBrick, null},
            },

            {{AdvancedRocketryBlocks.blockBlastBrick, AdvancedRocketryBlocks.blockBlastBrick, AdvancedRocketryBlocks.blockBlastBrick, AdvancedRocketryBlocks.blockBlastBrick, AdvancedRocketryBlocks.blockBlastBrick},
                    {AdvancedRocketryBlocks.blockBlastBrick, Blocks.AIR, Blocks.AIR, Blocks.AIR, AdvancedRocketryBlocks.blockBlastBrick},
                    {AdvancedRocketryBlocks.blockBlastBrick, Blocks.AIR, Blocks.AIR, Blocks.AIR, AdvancedRocketryBlocks.blockBlastBrick},
                    {AdvancedRocketryBlocks.blockBlastBrick, Blocks.AIR, Blocks.AIR, Blocks.AIR, AdvancedRocketryBlocks.blockBlastBrick},
                    {AdvancedRocketryBlocks.blockBlastBrick, AdvancedRocketryBlocks.blockBlastBrick, AdvancedRocketryBlocks.blockBlastBrick, AdvancedRocketryBlocks.blockBlastBrick, AdvancedRocketryBlocks.blockBlastBrick},
            },

            {{AdvancedRocketryBlocks.blockBlastBrick, '*', 'c', '*', AdvancedRocketryBlocks.blockBlastBrick},
                    {'*', AdvancedRocketryBlocks.blockBlastBrick, AdvancedRocketryBlocks.blockBlastBrick, AdvancedRocketryBlocks.blockBlastBrick, '*'},
                    {'*', AdvancedRocketryBlocks.blockBlastBrick, AdvancedRocketryBlocks.blockBlastBrick, AdvancedRocketryBlocks.blockBlastBrick, '*'},
                    {'*', AdvancedRocketryBlocks.blockBlastBrick, AdvancedRocketryBlocks.blockBlastBrick, AdvancedRocketryBlocks.blockBlastBrick, '*'},
                    {AdvancedRocketryBlocks.blockBlastBrick, '*', '*', '*', AdvancedRocketryBlocks.blockBlastBrick},
            },

            {{AdvancedRocketryBlocks.blockBlastBrick, AdvancedRocketryBlocks.blockBlastBrick, AdvancedRocketryBlocks.blockBlastBrick, AdvancedRocketryBlocks.blockBlastBrick, AdvancedRocketryBlocks.blockBlastBrick},
                    {AdvancedRocketryBlocks.blockBlastBrick, AdvancedRocketryBlocks.blockBlastBrick, AdvancedRocketryBlocks.blockBlastBrick, AdvancedRocketryBlocks.blockBlastBrick, AdvancedRocketryBlocks.blockBlastBrick},
                    {AdvancedRocketryBlocks.blockBlastBrick, AdvancedRocketryBlocks.blockBlastBrick, AdvancedRocketryBlocks.blockBlastBrick, AdvancedRocketryBlocks.blockBlastBrick, AdvancedRocketryBlocks.blockBlastBrick},
                    {AdvancedRocketryBlocks.blockBlastBrick, AdvancedRocketryBlocks.blockBlastBrick, AdvancedRocketryBlocks.blockBlastBrick, AdvancedRocketryBlocks.blockBlastBrick, AdvancedRocketryBlocks.blockBlastBrick},
                    {AdvancedRocketryBlocks.blockBlastBrick, AdvancedRocketryBlocks.blockBlastBrick, AdvancedRocketryBlocks.blockBlastBrick, AdvancedRocketryBlocks.blockBlastBrick, AdvancedRocketryBlocks.blockBlastBrick},
            }

    };

    @Override
    public List<BlockMeta> getAllowableWildCardBlocks() {
        List<BlockMeta> list = super.getAllowableWildCardBlocks();
        // Null-guard: TileMultiBlock.getMapping(char) returns null for characters
        // that haven't been registered yet. Without this guard the dedicated
        // server crashes during AR postInit (the client path happens to register
        // these wildcards in time, masking the bug). Surfaced by the headless
        // scenario suite.
        addMappingIfPresent(list, 'O');
        addMappingIfPresent(list, 'I');
        addMappingIfPresent(list, 'l');
        addMappingIfPresent(list, 'L');
        list.add(new BlockMeta(AdvancedRocketryBlocks.blockBlastBrick, -1));
        return list;
    }

    private static void addMappingIfPresent(List<BlockMeta> dest, char wildcard) {
        List<BlockMeta> mapping = TileMultiBlock.getMapping(wildcard);
        if (mapping != null) {
            dest.addAll(mapping);
        }
    }

    @Override
    protected void integrateTile(TileEntity tile) {
        super.integrateTile(tile);
    }

    @Override
    public Object[][][] getStructure() {
        return structure;
    }

    @Override
    public SoundEvent getSound() {
        return AudioRegistry.electricArcFurnace;
    }

    @Override
    public String getMachineName() {
        return AdvancedRocketryBlocks.blockArcFurnace.getLocalizedName();
    }

    @Override
    public List<ModuleBase> getModules(int ID, EntityPlayer player) {
        List<ModuleBase> modules = super.getModules(ID, player);

        modules.add(new ModuleProgress(80, 20, 0, TextureResources.arcFurnaceProgressBar, this));
        return modules;
    }
}
