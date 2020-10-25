package mrriegel.playerstorage;

import mrriegel.limelib.helper.NBTHelper;
import net.minecraft.nbt.NBTTagCompound;

public class Enums {
	public enum GuiMode {
		ITEM, FLUID;
	}

	public enum MessageAction {
		SORT, //
		DIRECTION, //
		CLEAR, //
		JEI, //
		GUIMODE, //
		SLOT, //
		INCGRID, //
		DECGRID, //
		KEYUPDATE, //
		TEAMINVITE, //
		TEAMACCEPT, //
		TEAMUNINVITE, //
		JEITRANSFER, //
		DEFAULTGUI, //
		SETLIMIT, //
		PICKUP, //
		AUTOFOCUS, //
		INVENTORY, //
		INVERTPICKUP, //
		WATER, //
		NOSHIFT, //
		HIGHLIGHT, //
		REFILL, //
		CRAFT, //
		DELETE;

		public NBTTagCompound set(NBTTagCompound nbt) {
			NBTHelper.set(nbt, "action", this);
			return nbt;
		}

	}

	public enum Sort {
		AMOUNT("\u03A3"), NAME("AZ"), MOD("M");

		private static Sort[] vals = values();
		public String shortt;

		private Sort(String shortt) {
			this.shortt = shortt;
		}

		public Sort next() {
			return vals[(ordinal() + 1) % vals.length];
		}
	}
}
