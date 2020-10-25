package mrriegel.limelib.item;

import mrriegel.limelib.helper.RegistryHelper;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;

public abstract class CommonSubtypeItem extends CommonItem {

	protected final int num;

	public CommonSubtypeItem(String name, int num) {
		super(name);
		this.num = num;
		setHasSubtypes(true);
	}

	@Override
	public void initModel() {
		for (int i = 0; i < num; i++)
			RegistryHelper.initModel(this, i, new ModelResourceLocation(getRegistryName() + "_" + i, "inventory"));
	}

	@Override
	public void getSubItems(CreativeTabs tab, NonNullList<ItemStack> subItems) {
		if (isInCreativeTab(tab))
			for (int i = 0; i < num; i++)
				subItems.add(new ItemStack(this, 1, i));
	}

	@Override
	public String getTranslationKey(ItemStack stack) {
		return super.getTranslationKey() + "_" + stack.getItemDamage();
	}

}
