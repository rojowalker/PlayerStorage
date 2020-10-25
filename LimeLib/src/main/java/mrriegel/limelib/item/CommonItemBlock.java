package mrriegel.limelib.item;

import net.minecraft.block.Block;
import net.minecraft.item.ItemBlock;

public class CommonItemBlock extends ItemBlock {

	public CommonItemBlock(Block block) {
		super(block);
		setRegistryName(block.getRegistryName());
	}

}
