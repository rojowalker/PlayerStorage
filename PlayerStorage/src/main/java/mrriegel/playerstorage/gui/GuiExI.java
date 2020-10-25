package mrriegel.playerstorage.gui;

import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang3.Validate;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import com.google.common.collect.Ordering;
import baubles.client.gui.GuiPlayerExpanded;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceMap;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import mrriegel.limelib.LimeLib;
import mrriegel.limelib.gui.CommonGuiContainer;
import mrriegel.limelib.gui.GuiDrawer;
import mrriegel.limelib.gui.GuiDrawer.Direction;
import mrriegel.limelib.gui.button.CommonGuiButton;
import mrriegel.limelib.gui.button.CommonGuiButton.Design;
import mrriegel.limelib.gui.element.AbstractSlot;
import mrriegel.limelib.gui.element.AbstractSlot.FluidSlot;
import mrriegel.limelib.gui.element.AbstractSlot.ItemSlot;
import mrriegel.limelib.gui.element.ScrollBar;
import mrriegel.limelib.helper.NBTHelper;
import mrriegel.limelib.network.PacketHandler;
import mrriegel.limelib.plugin.JEI;
import mrriegel.limelib.util.StackWrapper;
import mrriegel.limelib.util.Utils;
import mrriegel.playerstorage.ClientProxy;
import mrriegel.playerstorage.ConfigHandler;
import mrriegel.playerstorage.Enums.GuiMode;
import mrriegel.playerstorage.Enums.MessageAction;
import mrriegel.playerstorage.ExInventory;
import mrriegel.playerstorage.Message2Server;
import mrriegel.playerstorage.PlayerStorage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.init.Items;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.EnumFacing.Plane;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.oredict.OreDictionary;

public class GuiExI extends CommonGuiContainer {

	public List<StackWrapper> items = new ArrayList<>();
	protected List<AbstractSlot<?>> slots = new ArrayList<>();
	protected long lastClick;
	protected CommonGuiButton sort, direction, clear, jei, modeButton, inc, dec, defaultt, potion;
	protected GuiTextField searchBar;
	protected int currentPos = 0, maxPos = 0;
	protected AbstractSlot<?> over;
	protected final GuiMode mode;
	protected ScrollBar scrollBar;
	private Reference2ReferenceMap<ItemStack, ItemStack> itemMap = new Reference2ReferenceOpenHashMap<>();
	private boolean fragezeichen;
	private int gridWidth = 12, gridHeight;
	private ContainerExI con;
	ExInventory ei;

	private int hoverCounter;

	private boolean scrollDrag = false;

	protected boolean hasActivePotionEffects;

	public GuiExI(ContainerExI inventorySlotsIn) {
		super(inventorySlotsIn);
		lastClick = System.currentTimeMillis();
		con = inventorySlotsIn;
		mode = Validate.notNull(con.ei.mode);
		gridHeight = con.ei.gridHeight;
		ei = ExInventory.getInventory(Minecraft.getMinecraft().player);
	}

	@Override
	protected void actionPerformed(GuiButton button) throws IOException {
		super.actionPerformed(button);
		if (button.id == 1000) {
			GuiDrawer.openGui(new GuiInfo());
		} else if (button.id < MessageAction.values().length) {
			NBTTagCompound nbt = new NBTTagCompound();
			NBTHelper.set(nbt, "action", MessageAction.values()[button.id]);
			PacketHandler.sendToServer(new Message2Server(nbt));
			new Message2Server().handleMessage(mc.player, nbt, Side.CLIENT);
		}
	}

	public boolean canClick() {
		return System.currentTimeMillis() > lastClick + 150L;
	}

	private void drawActivePotionEffects(boolean left) {
		double z = 605;
		GlStateManager.translate(0, 0, z);
		int i = left ? guiLeft - 124 - 35 : guiLeft;
		int j = guiTop;
		Collection<PotionEffect> collection = mc.player.getActivePotionEffects();

		if (!collection.isEmpty()) {
			GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
			GlStateManager.disableLighting();
			int l = 33;

			if (collection.size() > 5) {
				l = 132 / (collection.size() - 1);
			}

			for (PotionEffect potioneffect : Ordering.natural().sortedCopy(collection)) {
				Potion potion = potioneffect.getPotion();
				if (!potion.shouldRender(potioneffect)) {
					continue;
				}
				GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
				mc.getTextureManager().bindTexture(INVENTORY_BACKGROUND);
				this.drawTexturedModalRect(i, j, 0, 166, 140, 32);

				if (potion.hasStatusIcon()) {
					int i1 = potion.getStatusIconIndex();
					this.drawTexturedModalRect(i + 6, j + 7, 0 + i1 % 8 * 18, 198 + i1 / 8 * 18, 18, 18);
				}

				potion.renderInventoryEffect(i, j, potioneffect, mc);
				if (!potion.shouldRenderInvText(potioneffect)) {
					j += l;
					continue;
				}
				String s1 = I18n.format(potion.getName());

				if (potioneffect.getAmplifier() == 1) {
					s1 = s1 + " " + I18n.format("enchantment.level.2");
				} else if (potioneffect.getAmplifier() == 2) {
					s1 = s1 + " " + I18n.format("enchantment.level.3");
				} else if (potioneffect.getAmplifier() == 3) {
					s1 = s1 + " " + I18n.format("enchantment.level.4");
				}

				fontRenderer.drawStringWithShadow(s1, i + 10 + 18, j + 6, 16777215);
				String s = Potion.getPotionDurationString(potioneffect, 1.0F);
				fontRenderer.drawStringWithShadow(s, i + 10 + 18, j + 6 + 10, 8355711);
				j += l;
			}
		}
		GlStateManager.translate(0, 0, -z);
	}

	@Override
	protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
		drawDefaultBackground();
		//super.drawGuiContainerBackgroundLayer(partialTicks, mouseX, mouseY);
		drawer.drawBackgroundTexture();
		if (PlayerStorage.baubles) {
			mc.getTextureManager().bindTexture(GuiPlayerExpanded.background);
			drawTexturedModalRect(245 + guiLeft, 7 + guiTop, 76, 7, 18, 18 * 4);
			drawTexturedModalRect(245 + guiLeft, 7 + 18 * 4 + guiTop, 76 + 19, 7, 18, 18 * 3);
		}
		drawer.drawPlayerSlots(79, 29 + 18 * gridHeight);//player inventory
		drawer.drawSlots(151, 9 + 18 * gridHeight, 5, 1);//armor slots
		drawer.drawSlot(113, 9 + 18 * gridHeight); //trash icon
		if (inc.isMouseOver() || dec.isMouseOver()) {
			drawer.drawSlots(7, 7, gridWidth, gridHeight);
		} else {
			drawer.drawFramedRectangle(7, 7, gridWidth * 18, gridHeight * 18);
		}
		drawer.drawColoredRectangle(7, 7, gridWidth * 18, gridHeight * 18, mode == GuiMode.ITEM ? 0xAA000000 : 0x2300c5cd);//highlight
		drawer.drawTextfield(searchBar);
		searchBar.drawTextBox();
		drawer.drawSlots(7, 29 + 18 * gridHeight, 3, 3);
		drawer.drawProgressArrow(13, 88 + 18 * gridHeight, 0F, Direction.RIGHT);
		drawer.drawSlot(43, 87 + 18 * gridHeight);
		scrollBar.draw(mouseX, mouseY);
		boolean uni = fontRenderer.getUnicodeFlag();
		fontRenderer.setUnicodeFlag(true);
		for (AbstractSlot<?> slot : slots) {
			slot.draw(mouseX, mouseY);
		}
		fontRenderer.setUnicodeFlag(uni);
		if (!ConfigHandler.infiniteSpace) {
			fontRenderer.drawString("Items: " + ei.itemCount + " / " + ei.itemLimit, guiLeft + 4, guiTop - 8, 0xFFFFFF);
		}

	}

	@Override
	protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
		//fontRenderer.drawString("x", 63, 28 + 18 * gridHeight, 0xE0E0E0);
		fontRenderer.drawString((con.ei.autofocus ? TextFormatting.GREEN : TextFormatting.RED) + "o", 103, 14 + 18 * gridHeight, 0xE0E0E0);
		super.drawGuiContainerForegroundLayer(mouseX, mouseY);
	}

	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		super.drawScreen(mouseX, mouseY, partialTicks);
		for (AbstractSlot<?> slot : slots) {
			if (slot.isMouseOver(mouseX, mouseY)) {
				slot.drawTooltip(mouseX, mouseY);
			}
		}
		if (fragezeichen) {
			List<String> lis = new ArrayList<>();
			lis.add(TextFormatting.AQUA + "- " + TextFormatting.RESET + "Hover over an item in your inventory and press " + ClientProxy.OPENLIMIT.getDisplayName() + " to adjust the limit.");
			lis.add(TextFormatting.AQUA + "- " + TextFormatting.RESET + "Hover over an item in your storage and press " + ClientProxy.HIGHLIGHT.getDisplayName() + " to highlight it.");
			lis.add(TextFormatting.AQUA + "- " + TextFormatting.RESET + "Hover over an item in your storage and press " + ClientProxy.DELETE.getDisplayName() + " to delete it.");
			GuiDrawer.renderToolTip(lis, mouseX, mouseY);
		}
		if (hasActivePotionEffects) {
			if (potion == null) {
				drawActivePotionEffects(true);
			} else if (potion.isMouseOver()) {
				drawActivePotionEffects(false);
			}
		}

	}

	private List<StackWrapper> getFilteredItems() {
		String search = searchBar.getText().toLowerCase().trim();
		List<StackWrapper> tmp = search.isEmpty() ? new ArrayList<>(items) : items.stream().filter(w -> match(w.getStack(), search)).collect(Collectors.toList());
		int mul = !con.ei.topdown ? -1 : 1;
		tmp.sort((StackWrapper o2, StackWrapper o1) -> {
			int t = Boolean.compare(con.ei.highlightItems.contains(o1.getStack()), con.ei.highlightItems.contains(o2.getStack()));
			if (t != 0)
				return t;
			switch (con.ei.sort) {
				case AMOUNT:
					return Integer.compare(o1.getSize(), o2.getSize()) * mul;
				case NAME:
					return TextFormatting.getTextWithoutFormattingCodes(o2.getStack().getDisplayName()).compareToIgnoreCase(TextFormatting.getTextWithoutFormattingCodes(o1.getStack().getDisplayName())) * mul;
				case MOD:
					return Utils.getModName(o2.getStack().getItem()).compareToIgnoreCase(Utils.getModName(o1.getStack().getItem())) * mul;
			}
			return 0;
		});
		return tmp;
	}

	@Override
	public void handleKeyboardInput() throws IOException {
		super.handleKeyboardInput();
		NBTTagCompound nbt = new NBTTagCompound();
		MessageAction.KEYUPDATE.set(nbt);
		NBTHelper.set(nbt, "space", Keyboard.isKeyDown(Keyboard.KEY_SPACE));
		NBTHelper.set(nbt, "shift", isShiftKeyDown());
		NBTHelper.set(nbt, "ctrl", isCtrlKeyDown());
		PacketHandler.sendToServer(new Message2Server(nbt));
	}

	@Override
	public void handleMouseInput() throws IOException {
		super.handleMouseInput();
		if (scrollBar.isMouseOver(GuiDrawer.getMouseX(), GuiDrawer.getMouseY()) || isPointInRegion(7, 7, 18 * gridWidth, 18 * gridHeight, GuiDrawer.getMouseX(), GuiDrawer.getMouseY())) {
			int mouse = Mouse.getEventDWheel();
			if (mouse == 0)
				return;
			if (mouse > 0 && currentPos > 0) {
				currentPos--;
			} else if (mouse < 0 && currentPos < maxPos) {
				currentPos++;
			}
		}
	}

	@Override
	public void initGui() {
		xSize = 248;
		if (PlayerStorage.baubles) {
			xSize += 22;
		}
		ySize = 112 + 18 * gridHeight;
		super.initGui();

		searchBar = new GuiTextField(0, fontRenderer, guiLeft + 9, guiTop + 14 + 18 * gridHeight, 85, fontRenderer.FONT_HEIGHT);
		searchBar.setMaxStringLength(30);
		searchBar.setTextColor(16777215);
		searchBar.setFocused(con.ei.autofocus);
		searchBar.setEnableBackgroundDrawing(false);
		buttonList.add(clear = new CommonGuiButton(MessageAction.CLEAR.ordinal(), guiLeft + 62, guiTop + 29 + 18 * gridHeight, 7, 7, null).setTooltip("Clear grid").setDesign(Design.SIMPLE));
		buttonList.add(new CommonGuiButton(MessageAction.AUTOFOCUS.ordinal(), guiLeft + 102, guiTop + 15 + 18 * gridHeight, 7, 7, null).setTooltip("Toggle auto focus on search bar").setDesign(Design.SIMPLE));
		buttonList.add(new CommonGuiButton(1000, guiLeft - 25, guiTop + ySize - 20, 23, 20, "\u2261").setTooltip("More Options").setDesign(Design.SIMPLE));
		buttonList.add(inc = new CommonGuiButton(MessageAction.INCGRID.ordinal(), guiLeft - 25, guiTop + 1, 23, 10, "+").setTooltip("Increase Grid Height").setDesign(Design.SIMPLE).setButtonColor(Color.GRAY.getRGB()));
		buttonList.add(dec = new CommonGuiButton(MessageAction.DECGRID.ordinal(), guiLeft - 25, guiTop + 14, 23, 10, "-").setTooltip("Decrease Grid Height").setDesign(Design.SIMPLE).setButtonColor(Color.GRAY.getRGB()));
		buttonList.add(sort = new CommonGuiButton(MessageAction.SORT.ordinal(), guiLeft - 25, guiTop + 27, 23, 14, null).setDesign(Design.SIMPLE));
		buttonList.add(direction = new CommonGuiButton(MessageAction.DIRECTION.ordinal(), guiLeft - 25, guiTop + 44, 23, 14, null).setDesign(Design.SIMPLE));
		buttonList.add(defaultt = new CommonGuiButton(MessageAction.DEFAULTGUI.ordinal(), guiLeft - 25, guiTop + 61, 23, 14, null).setTooltip("Set this to default inventory GUI.").setDesign(Design.SIMPLE));
		if (LimeLib.jeiLoaded) {
			buttonList.add(jei = new CommonGuiButton(MessageAction.JEI.ordinal(), guiLeft - 25, guiTop + 78, 23, 14, null).setTooltip("Enable synchronized search with JEI").setDesign(Design.SIMPLE));
		}
		scrollBar = new ScrollBar(0, 227 + guiLeft, 7 + guiTop, 14, 18 * gridHeight, drawer, Plane.VERTICAL);
		slots = new ArrayList<>();
		for (int i = 0; i < gridHeight; i++) {
			for (int j = 0; j < gridWidth; j++) {
				slots.add(new ItemSlot(ItemStack.EMPTY, 0, guiLeft + 8 + j * 18, guiTop + 8 + i * 18, 0, drawer, true, true, false, true) {
					@Override
					public void draw(int mouseX, int mouseY) {
						if (!stack.isEmpty() && con.ei.highlightItems.contains(stack)) {
							int color = 0xFF008BBB;
							GlStateManager.disableLighting();
							drawer.drawFrame(x - 1, y - 1, 17, 17, 1, color);
							GlStateManager.enableLighting();
						}
						super.draw(mouseX, mouseY);
					}
				});
			}
		}
		updateActivePotionEffects();
		potion = null;
		if (hasActivePotionEffects && guiLeft < 160) {
			buttonList.add(potion = new CommonGuiButton(12000, guiLeft + 62, guiTop + 88 + 18 * gridHeight, 16, 16, "") {
				@Override
				public boolean mousePressed(Minecraft mc, int mouseX, int mouseY) {
					return false;
				}
			}.setFrameColor(0xFF444444).setButtonColor(0xFF999999).setDesign(Design.SIMPLE).setStack(new ItemStack(Items.POTIONITEM)));
		}
		updateScreen();
	}

	@Override
	protected void keyTyped(char typedChar, int keyCode) throws IOException {
		Slot slot = null;
		if (LimeLib.jeiLoaded && over != null && over.stack != null && (over instanceof FluidSlot || !((ItemStack) over.stack).isEmpty()) && (keyCode == Keyboard.KEY_R || keyCode == Keyboard.KEY_U) && (!searchBar.isFocused() || searchBar.getText().isEmpty())) {
			if (keyCode == Keyboard.KEY_R) {
				JEI.showRecipes(over.stack);
			} else {
				JEI.showUsage(over.stack);
			}
			return;
		} else if (searchBar.textboxKeyTyped(typedChar, keyCode)) {
			if (con.ei.jeiSearch && LimeLib.jeiLoaded) {
				JEI.setFilterText(searchBar.getText());
			}
			return;
		} else if ((slot = getSlotUnderMouse()) != null && slot.inventory == mc.player.inventory && slot.getHasStack() && keyCode == ClientProxy.OPENLIMIT.getKeyCode()) {
			ItemStack under = slot.getStack();
			FluidStack fs = FluidUtil.getFluidContained(under);
			if (fs != null) {
				GuiDrawer.openGui(new GuiLimit(fs));
			} else {
				GuiDrawer.openGui(new GuiLimit(under));
			}
			return;
		} else if (over != null && over.stack != null && (over instanceof FluidSlot || !((ItemStack) over.stack).isEmpty())) {
			NBTTagCompound nbt = new NBTTagCompound();
			if (ClientProxy.HIGHLIGHT.isActiveAndMatches(keyCode)) {
				MessageAction.HIGHLIGHT.set(nbt);
			} else if (ClientProxy.DELETE.isActiveAndMatches(keyCode)) {
				MessageAction.DELETE.set(nbt);
			} else
				return;
			if (over instanceof ItemSlot) {
				if (!((ItemSlot) over).stack.isEmpty()) {
					NBTHelper.set(nbt, "slot", itemMap.get(((ItemSlot) over).stack).writeToNBT(new NBTTagCompound()));
				}
				NBTHelper.set(nbt, "item", true);
			} else
				throw new RuntimeException();
			PacketHandler.sendToServer(new Message2Server(nbt));
			new Message2Server().handleMessage(mc.player, nbt, Side.CLIENT);
			return;
		}
		super.keyTyped(typedChar, keyCode);
	}

	private boolean match(FluidStack stack, String text) {
		String[] list = text.split("\\|");
		for (String w : list) {
			final String word = w.trim();
			if (word.startsWith("@")) {
				String modID = FluidRegistry.getDefaultFluidName(stack.getFluid()).split(":")[0];
				if (modID.toLowerCase().contains(word.substring(1)) || Loader.instance().getIndexedModList().get(modID).getName().toLowerCase().contains(word.substring(1)))
					return true;
			} else if (TextFormatting.getTextWithoutFormattingCodes(stack.getLocalizedName()).toLowerCase().contains(word) || //
					stack.getUnlocalizedName().toLowerCase().contains(word))
				return true;
		}
		return false;
	}

	private boolean match(ItemStack stack, String text) {
		String[] list = text.split("\\|");
		for (String w : list) {
			final String word = w.trim();
			if (word.startsWith("@")) {
				String modID = Utils.getModID(stack.getItem());
				if (modID.toLowerCase().contains(word.substring(1)) || Loader.instance().getIndexedModList().get(modID).getName().toLowerCase().contains(word.substring(1)))
					return true;
			} else if (word.startsWith("#")) {
				List<String> tooltip = stack.getTooltip(mc.player, mc.gameSettings.advancedItemTooltips ? ITooltipFlag.TooltipFlags.ADVANCED : ITooltipFlag.TooltipFlags.NORMAL);
				if (!tooltip.isEmpty() && tooltip.get(0).contains(stack.getDisplayName())) {
					tooltip.remove(0);
				}
				for (String s : tooltip) {
					if (TextFormatting.getTextWithoutFormattingCodes(s).toLowerCase().contains(word.substring(1)))
						return true;
				}
			} else if (word.startsWith("$"))
				return Arrays.stream(OreDictionary.getOreIDs(stack)).mapToObj(OreDictionary::getOreName).anyMatch(s -> s.toLowerCase().contains(word.substring(1)));
			else if (word.startsWith("%"))
				return Arrays.stream(stack.getItem().getCreativeTabs()).filter(c -> c != null).map(CreativeTabs::getTranslationKey).anyMatch(s -> s.toLowerCase().contains(word.substring(1)));
			else if (TextFormatting.getTextWithoutFormattingCodes(stack.getDisplayName()).toLowerCase().contains(word))
				return true;
		}
		return false;
	}

	@Override
	protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
		super.mouseClicked(mouseX, mouseY, mouseButton);
		searchBar.mouseClicked(mouseX, mouseY, mouseButton);
		if (searchBar.isFocused() && mouseButton == 1) {
			searchBar.setText("");
			if (LimeLib.jeiLoaded && con.ei.jeiSearch) {
				JEI.setFilterText("");
			}
		}
		if (canClick()) {
			if (over != null) {
				if (mouseButton == 0 || mouseButton == 1) {
					sendSlot(over, mouseButton);
				} else if (mouseButton == 2) {
					GuiDrawer.openGui(new GuiLimit(over.stack));
				}
			}
			lastClick = System.currentTimeMillis();
		}
		if (scrollBar.isMouseOver(mouseX, mouseY)) {
			scrollBar.status = (mouseY - scrollBar.y) / (double) scrollBar.height;
			currentPos = MathHelper.clamp((int) Math.round(maxPos * scrollBar.status), 0, maxPos);
			scrollDrag = true;
		}
		if (hoverCounter > 1) {
			PacketHandler.sendToServer(new Message2Server(MessageAction.INVENTORY.set(new NBTTagCompound())));
			mc.displayGuiScreen(new GuiInventory(mc.player));
		}
	}

	@Override
	protected void mouseReleased(int mouseX, int mouseY, int state) {
		if (scrollDrag) {
			scrollDrag = false;
		}
		super.mouseReleased(mouseX, mouseY, state);
	}

	public void openGui() {
		GuiDrawer.openGui(new GuiInfo());
	}

	protected void sendSlot(AbstractSlot<?> slot, int mouseButton) {
		NBTTagCompound nbt = new NBTTagCompound();
		MessageAction.SLOT.set(nbt);
		if (slot instanceof ItemSlot) {
			if (!((ItemSlot) slot).stack.isEmpty()) {
				NBTHelper.set(nbt, "slot", itemMap.get(((ItemSlot) slot).stack).writeToNBT(new NBTTagCompound()));
			}
		} else
			throw new RuntimeException();
		NBTHelper.set(nbt, "mouse", mouseButton);
		NBTHelper.set(nbt, "shift", isShiftKeyDown());
		NBTHelper.set(nbt, "ctrl", isCtrlKeyDown());
		PacketHandler.sendToServer(new Message2Server(nbt));
	}

	protected void updateActivePotionEffects() {
		boolean hasVisibleEffect = false;
		for (PotionEffect potioneffect : mc.player.getActivePotionEffects()) {
			Potion potion = potioneffect.getPotion();
			if (potion.shouldRender(potioneffect)) {
				hasVisibleEffect = true;
				break;
			}
		}
		if (mc.player.getActivePotionEffects().isEmpty() || !hasVisibleEffect) {
			guiLeft = (width - xSize) / 2;
			hasActivePotionEffects = false;
		} else {
			if (net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new net.minecraftforge.client.event.GuiScreenEvent.PotionShiftEvent(this))) {
				guiLeft = (width - xSize) / 2;
			} else {
				guiLeft = 160 + (width - xSize - 200) / 2;
			}
			hasActivePotionEffects = true;
		}
	}

	@Override
	public void updateScreen() {
		super.updateScreen();
		if (searchBar.isFocused() && LimeLib.jeiLoaded && JEI.hasKeyboardFocus()) {
			searchBar.setFocused(false);
		}
		if (mode == GuiMode.ITEM) {
			if (con.ei.needSync) {
				con.ei.needSync = false;
				items = con.ei.getItems();
				itemMap.clear();
				for (StackWrapper sw : items) {
					itemMap.put(sw.getStack(), sw.getStack().copy());
				}
			}
			List<StackWrapper> tmp = getFilteredItems();
			int invisible = tmp.size() - gridWidth * gridHeight;
			if (invisible <= 0) {
				maxPos = 0;
			} else {
				maxPos = invisible / gridWidth;
				if (invisible % gridWidth != 0) {
					maxPos++;
				}
			}
			if (currentPos > maxPos) {
				currentPos = maxPos;
			}
			int index = currentPos * gridWidth;
			int s = 0;
			for (int i = 0; i < gridHeight; i++) {
				for (int j = 0; j < gridWidth; j++) {
					ItemSlot slot = (ItemSlot) slots.get(s);
					if (index >= tmp.size()) {
						slot.stack = ItemStack.EMPTY;
					} else {
						StackWrapper w = tmp.get(index);
						slot.stack = w.getStack();
						slot.amount = w.getSize();
						index++;
					}
					slot.square = !slot.stack.isEmpty();
					s++;
				}
			}
		}

		over = null;
		for (AbstractSlot<?> slot : slots) {
			if (slot.isMouseOver(GuiDrawer.getMouseX(), GuiDrawer.getMouseY())) {
				over = slot;
				break;
			}
		}

		scrollBar.status = currentPos / (double) maxPos;
		if (scrollDrag) {
			scrollBar.status = (GuiDrawer.getMouseY() - scrollBar.y) / (double) scrollBar.height;
			scrollBar.status = MathHelper.clamp(scrollBar.status, 0, 1);
			currentPos = MathHelper.clamp((int) Math.round(maxPos * scrollBar.status), 0, maxPos);
		}

		dec.visible = gridHeight > 1;
		inc.visible = guiTop > 5;
		if (guiTop < 0) {
			try {
				actionPerformed(dec);
			} catch (IOException e) {
			}
		}

		sort.setTooltip("Sort by " + con.ei.sort.name().toLowerCase());
		sort.displayString = TextFormatting.GRAY + con.ei.sort.shortt;
		direction.setTooltip("Sort direction: " + (con.ei.topdown ? "top-down" : "bottom-up"));
		direction.displayString = TextFormatting.GRAY + (con.ei.topdown ? "\u2B07" : "\u2B06");
		defaultt.displayString = (con.ei.defaultGUI ? TextFormatting.GREEN : TextFormatting.RED) + "GUI";
		//modeButton.setStack(new ItemStack(mode != GuiMode.ITEM ? Items.WATER_BUCKET : /*Items.APPLE*/Item.getItemFromBlock(Blocks.GRASS)));
		if (jei != null) {
			jei.displayString = (con.ei.jeiSearch ? TextFormatting.GREEN : TextFormatting.RED) + "JEI";
		}
	}
}
