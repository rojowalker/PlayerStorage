package mrriegel.limelib;

import mrriegel.limelib.particle.CommonParticle;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.IThreadListener;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.fml.relauncher.Side;

public class LimeCommonProxy {
	@Deprecated
	public Side getSide() {
		return Side.SERVER;
	}

	//TODO remove
	@Deprecated
	public double getReachDistance(EntityPlayer player) {
		return player instanceof FakePlayer ? 0 : ((EntityPlayerMP) player).interactionManager.getBlockReachDistance();
	}

	@Deprecated
	public World getClientWorld() {
		throw new UnsupportedOperationException();
	}

	@Deprecated
	public EntityPlayer getClientPlayer() {
		throw new UnsupportedOperationException();
	}

	@Deprecated
	public RayTraceResult getClientRayTrace() {
		throw new UnsupportedOperationException();
	}

	@Deprecated
	public IThreadListener getClientListener() {
		throw new UnsupportedOperationException();
	}

	public void renderParticle(CommonParticle par) {
	}

}
