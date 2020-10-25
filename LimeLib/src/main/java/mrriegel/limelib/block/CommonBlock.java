package mrriegel.limelib.block;

import mrriegel.limelib.LimeLib;
import mrriegel.limelib.helper.RegistryHelper;
import mrriegel.limelib.item.CommonItemBlock;
import mrriegel.limelib.tile.CommonTile;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.ItemBlock;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class CommonBlock extends Block {

	protected ItemBlock itemblock;
	protected final boolean hasTile;
	protected final Class<? extends CommonTile> tileClass;

	public CommonBlock(Material materialIn, String name) {
		super(materialIn);
		setRegistryName(name);
		setTranslationKey(getRegistryName().toString());
		itemblock = new CommonItemBlock(this);
		hasTile = false;
		tileClass = null;
	}

	public void registerBlock() {
		RegistryHelper.register(this);
		RegistryHelper.register(getItemBlock());
	}

	public void initModel() {
		RegistryHelper.initModel(getItemBlock(), 0, new ModelResourceLocation(getRegistryName(), "inventory"));
	}

	public final ItemBlock getItemBlock() {
		return itemblock;
	}

	public <T extends Comparable<T>, V extends T> void changeProperty(World world, BlockPos pos, IProperty<T> property, V value) {
		if (!getBlockState().getProperties().contains(property))
			LimeLib.log.warn("Property " + property.getName() + " doesn't fit to " + getRegistryName() + ".");
		else {
			IBlockState state = world.getBlockState(pos);
			if (!state.getValue(property).equals(value)) {
				world.setBlockState(pos, state.withProperty(property, value));
				world.markBlockRangeForRenderUpdate(pos, pos);
			}
		}
	}

	@Override
	public TileEntity createTileEntity(World world, IBlockState state) {
		//		if (hasTileEntity(state))
		//			throw new RuntimeException("override it");
		return super.createTileEntity(world, state);
	}

}
