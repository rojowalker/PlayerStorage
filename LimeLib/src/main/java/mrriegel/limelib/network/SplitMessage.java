package mrriegel.limelib.network;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.lang3.reflect.ConstructorUtils;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.relauncher.Side;

public class SplitMessage extends AbstractMessage {

	private static Multimap<String, NBTTagCompound> map = HashMultimap.create();
	public static final String size = "%size_%", clazz = "%clazz_%", id = "%id_%";

	public SplitMessage() {
	}

	public static Iterable<SplitMessage> messages(Class<? extends AbstractMessage> clazz, Object... paras) {
		List<SplitMessage> lis = new ArrayList<>();
		try {
			AbstractMessage am = ConstructorUtils.invokeConstructor(clazz, paras);
			List<NBTTagCompound> ns = new ArrayList<>();
			//TODO
			for (NBTTagCompound n : ns) {
				SplitMessage m = new SplitMessage();
				m.shallSend = am.shallSend;
				m.nbt = n;
				lis.add(m);
			}
		} catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException e) {
			throw new RuntimeException(e);
		}
		return lis;
	}

	@Override
	public void handleMessage(EntityPlayer player, NBTTagCompound nbt, Side side) {
		String id = nbt.getString(SplitMessage.id);
		int size = nbt.getInteger(SplitMessage.size);
		nbt.removeTag(SplitMessage.id);
		nbt.removeTag(SplitMessage.size);
		map.put(id, nbt);
		Collection<NBTTagCompound> col = map.get(id);
		if (col.size() == size) {
			try {
				Class<? extends AbstractMessage> clazz = (Class<? extends AbstractMessage>) Class.forName(nbt.getString(SplitMessage.clazz));
				nbt.removeTag(SplitMessage.clazz);
				AbstractMessage am = clazz.newInstance();
				map.removeAll(id);
				NBTTagCompound n = new NBTTagCompound();
				for (NBTTagCompound nn : col)
					n.merge(nn);
				am.handleMessage(player, n, side);
			} catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
				throw new RuntimeException(e);
			}
		}
	}

}
