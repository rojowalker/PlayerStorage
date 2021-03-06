package mrriegel.playerstorage.registry;

import java.util.List;
import mrriegel.limelib.LimeLib;
import mrriegel.limelib.helper.RegistryHelper;
import mrriegel.limelib.particle.CommonParticle;
import mrriegel.playerstorage.ClientProxy;
import mrriegel.playerstorage.ConfigHandler;
import mrriegel.playerstorage.ConfigHandler.Unit2;
import mrriegel.playerstorage.ExInventory;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.NonNullList;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.oredict.OreDictionary;

public class ItemApple extends ItemFood {

	protected final int num;

	public ItemApple() {
		super(0, 0, false);
		num = ConfigHandler.apples.size();
		setRegistryName("apple");
		setTranslationKey(getRegistryName().toString());
		setHasSubtypes(true);
		setCreativeTab(CreativeTabs.MISC);
		setAlwaysEdible();
	}

	@Override
	public void addInformation(ItemStack stack, World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
		tooltip.add("Increase your max storage with apples.");
		Unit2 u = ConfigHandler.apples.get(ConfigHandler.appleList.get(stack.getItemDamage()));
		if (u != null) {
			tooltip.add("Increases Max Item limit on your player storage by: " + u.itemLimit + ".");
		}
	}

	@Override
	public String getItemStackDisplayName(ItemStack stack) {
		String ore = ConfigHandler.appleList.get(stack.getItemDamage());
		List<ItemStack> ores = OreDictionary.getOres(ore);
		if (!ores.isEmpty()) {
			ore = ores.get(0).getDisplayName();
		}
		return I18n.translateToLocal("item.playerstorage:apple.name") + " (" + ore + ")";
	}

	@Override
	public void getSubItems(CreativeTabs tab, NonNullList<ItemStack> subItems) {
		if (isInCreativeTab(tab) && !ConfigHandler.infiniteSpace) {
			for (int i = 0; i < num; i++) {
				subItems.add(new ItemStack(this, 1, i));
			}
		}
	}

	@Override
	public String getTranslationKey(ItemStack stack) {
		return super.getTranslationKey() + "_" + stack.getItemDamage();
	}

	@Override
	public boolean hasEffect(ItemStack stack) {
		List<ItemStack> ores = OreDictionary.getOres(ConfigHandler.appleList.get(stack.getItemDamage()));
		if (!ores.isEmpty())
			return ores.get(0).getItem().hasEffect(ores.get(0));
		return super.hasEffect(stack);
	}

	@Override
	public ICapabilityProvider initCapabilities(ItemStack stack, NBTTagCompound nbt) {
		if (ConfigHandler.appleList.get(stack.getItemDamage()).isEmpty()) {
			stack.setCount(0);
		}
		return super.initCapabilities(stack, nbt);
	}

	public void initModel() {
		for (int i = 0; i < num; i++) {
			RegistryHelper.initModel(this, i, new ModelResourceLocation(getRegistryName(), "inventory"));
		}
	}

	@Override
	protected void onFoodEaten(ItemStack stack, World worldIn, EntityPlayer player) {
		super.onFoodEaten(stack, worldIn, player);
		if (!ConfigHandler.infiniteSpace) {
			ExInventory ei = ExInventory.getInventory(player);
			Unit2 u = ConfigHandler.apples.get(ConfigHandler.appleList.get(stack.getItemDamage()));
			if (u == null)
				return;
			ei.itemLimit += Math.abs(u.itemLimit);
			if (ei.itemLimit < 0) {
				ei.itemLimit = Integer.MAX_VALUE;
			}
			ei.markForSync();
			if (worldIn.isRemote) {
				player.sendStatusMessage(new TextComponentString("Player's Max Storage Limit increased by " + u.itemLimit + " items.").setStyle(new Style().setUnderlined(true)), true);
				for (int i = 0; i < 70; i++) {
					LimeLib.proxy.renderParticle(new CommonParticle(player.posX, player.posY + .3, player.posZ).//
							setFlouncing(.015).setScale(1f).setGravity(-.02f).setMaxAge2(100).setColor(ClientProxy.colorMap.get(stack.getItemDamage()), 255, 10));
				}
			}
		}
	}

	public void registerItem() {
		RegistryHelper.register(this);
	}

}
