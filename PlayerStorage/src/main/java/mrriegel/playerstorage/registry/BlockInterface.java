package mrriegel.playerstorage.registry;

import java.util.List;
import mrriegel.limelib.block.CommonBlockContainer;
import mrriegel.limelib.helper.RegistryHelper;
import mrriegel.limelib.util.GlobalBlockPos;
import mrriegel.playerstorage.ExInventory;
import mrriegel.playerstorage.PlayerStorage;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import net.minecraftforge.registries.IForgeRegistryEntry;

public class BlockInterface extends CommonBlockContainer<TileInterface> {

	public BlockInterface() {
		super(Material.IRON, "interface");
		setHardness(2.8f);
		setCreativeTab(CreativeTabs.TRANSPORTATION);
	}

	@Override
	protected Class<? extends TileInterface> getTile() {
		return TileInterface.class;
	}
	@Override
	public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
		TileEntity te = world.getTileEntity(pos);
		if (player == ((TileInterface) te).getPlayer()) {
			player.openGui(PlayerStorage.instance, 0, player.world, 0, 0, 0);
			return true;
		}
		return false;
	}

	@Override
	public void onBlockPlacedBy(World worldIn, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack) {
		super.onBlockPlacedBy(worldIn, pos, state, placer, stack);
		TileEntity t;
		if ((t = worldIn.getTileEntity(pos)) instanceof TileInterface) {
			((TileInterface) t).setPlayer((EntityPlayer) placer);
			((TileInterface) t).setOn(true);
			ExInventory.getInventory((EntityPlayer) placer).tiles.add(GlobalBlockPos.fromTile(t));
		}
	}
}
