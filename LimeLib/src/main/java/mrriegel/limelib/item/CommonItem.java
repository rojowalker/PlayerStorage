package mrriegel.limelib.item;

import mrriegel.limelib.helper.RegistryHelper;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;

public class CommonItem extends Item {

	public CommonItem(String name) {
		super();
		setRegistryName(name);
		setTranslationKey(getRegistryName().toString());
	}

	public void registerItem() {
		RegistryHelper.register(this);
	}

	public void initModel() {
		RegistryHelper.initModel(this, 0, new ModelResourceLocation(getRegistryName(), "inventory"));
	}

}
