package mrriegel.limelib.network;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.commons.lang3.Validate;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;

import mrriegel.limelib.helper.NBTHelper;
import mrriegel.limelib.tile.IHUDProvider;
import mrriegel.limelib.util.ClientEventHandler;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.relauncher.Side;

public class HUDProviderMessage extends AbstractMessage {

	private static final String SPLIT = "~#~²", SPLIT2 = ";§€";

	public HUDProviderMessage() {
	}

	public HUDProviderMessage(EntityPlayerMP player) {
		Map<BlockPos, Map<EnumFacing, String>> map = new HashMap<>();
		BlockPos playerPos = new BlockPos(player);
		for (BlockPos p : BlockPos.getAllInBox(playerPos.add(-6, -6, -6), playerPos.add(6, 6, 6))) {
			TileEntity t = player.world.getTileEntity(p);
			if (IHUDProvider.isHUDProvider(t)) {
				IHUDProvider ds = IHUDProvider.getHUDProvider(t);
				if (ds.readingSide().isServer()) {
					Map<EnumFacing, String> facemap = new EnumMap<>(EnumFacing.class);
					for (EnumFacing f : EnumFacing.HORIZONTALS) {
						facemap.put(f, Joiner.on(SPLIT).join(ds.getData(player.isSneaking(), f)));
					}
					map.put(p, facemap);
				}
			}
		}
		if (map.isEmpty())
			shallSend = false;
		else {
			List<BlockPos> l1 = new ArrayList<>();
			List<Map<EnumFacing, String>> l2 = new ArrayList<>();
			for (Entry<BlockPos, Map<EnumFacing, String>> e : map.entrySet()) {
				l1.add(e.getKey());
				l2.add(e.getValue());
			}
			NBTHelper.setList(nbt, "lis1", l1);
			List<String> ss = new ArrayList<>();
			for (Map<EnumFacing, String> m : l2) {
				ss.add(Joiner.on(SPLIT2).join(Arrays.stream(EnumFacing.HORIZONTALS).map(f -> m.get(f)).collect(Collectors.toList())));
			}
			NBTHelper.setList(nbt, "lis2", ss);
		}
	}

	@Override
	public void handleMessage(EntityPlayer player, NBTTagCompound nbt, Side side) {
		List<BlockPos> lis1 = NBTHelper.getList(nbt, "lis1", BlockPos.class);
		List<String> lis2 = NBTHelper.getList(nbt, "lis2", String.class);
		List<Map<EnumFacing, List<String>>> l2 = new ArrayList<>();
		for (String s : lis2) {
			String[] l = s.split(SPLIT2);
			Validate.isTrue(l.length == 4);
			Map<EnumFacing, List<String>> m = new EnumMap<>(EnumFacing.class);
			for (int i = 0; i < 4; i++)
				m.put(EnumFacing.HORIZONTALS[i], Lists.newArrayList(l[i].split(SPLIT)));
			l2.add(m);
		}
		Validate.isTrue(l2.size() == lis1.size());
		//		ClientEventHandler.supplierTexts.clear();
		for (int i = 0; i < lis1.size(); i++) {
			BlockPos p = lis1.get(i);
			TileEntity t = player.world.getTileEntity(p);
			if (t != null)
				ClientEventHandler.supplierTexts.put(p, l2.get(i));
			else
				ClientEventHandler.supplierTexts.remove(p);
		}
	}

}
