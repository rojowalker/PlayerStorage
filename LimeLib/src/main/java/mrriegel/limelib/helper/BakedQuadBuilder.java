package mrriegel.limelib.helper;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

import org.apache.commons.lang3.Validate;
import org.lwjgl.util.vector.Vector3f;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.client.renderer.vertex.VertexFormatElement;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.Vec3i;
import net.minecraftforge.client.model.pipeline.UnpackedBakedQuad;

public class BakedQuadBuilder {

	private static Map<Vector3f, EnumFacing> dirMap //= new Object2ObjectArrayMap<>(6);
			= new TreeMap<>((v1, v2) -> Integer.compare((v1.x + "" + v1.y + "" + v1.z).hashCode(), (v2.x + "" + v2.y + "" + v2.z).hashCode()));
	static {
		for (EnumFacing f : EnumFacing.VALUES) {
			Vec3i v = f.getDirectionVec();
			dirMap.put(new Vector3f(v.getX(), v.getY(), v.getZ()), f);
		}
	}

	private final VertexFormat format;
	private final TextureAtlasSprite sprite;
	private int vertCount = 0;
	private Vertex[] vertices = new Vertex[4];
	private int tint = -1;
	private EnumFacing face;
	private boolean diffuse = true;
	private Rotation textureRotation = Rotation.D_0;

	public BakedQuadBuilder(VertexFormat format, TextureAtlasSprite sprite) {
		this.format = Objects.requireNonNull(format);
		this.sprite = Objects.requireNonNull(sprite);
		//https://github.com/joansmith/BuildCraft/blob/7d8dd254f020d62148d22a7a4f2d4145efbbe392/common/buildcraft/core/lib/render/MutableQuad.java
	}

	public BakedQuadBuilder(TextureAtlasSprite sprite) {
		this(DefaultVertexFormats.ITEM, sprite);
	}

	public BakedQuad build(boolean unpacked) {
		Validate.isTrue(vertCount == 4, "too few vertices");
		Vector3f normal = Vector3f.cross(Vector3f.sub(vertices[2].pos, vertices[1].pos, null), Vector3f.sub(vertices[0].pos, vertices[1].pos, null), null).normalise(null);
		double degree = 1000;
		Vector3f tmp = null;
		for (Vector3f f : dirMap.keySet()) {
			double d = Math.acos(Vector3f.dot(normal, f));
			if (d < degree) {
				degree = d;
				tmp = f;
			}
		}
		face = dirMap.get(tmp);
		Validate.isTrue(face != null, "no face set");
		ByteBuffer bb = null;
		UnpackedBakedQuad.Builder builder = null;
		if (unpacked) {
			builder = new UnpackedBakedQuad.Builder(format);
			builder.setQuadTint(tint);
			builder.setQuadOrientation(face);
			builder.setTexture(sprite);
			builder.setApplyDiffuseLighting(diffuse);
		} else
			bb = ByteBuffer.allocate(format.getSize() * 4);
		Vertex temp;
		for (int i = 0; i < textureRotation.t; i++) {
			temp = vertices[0];
			for (int j = 0; j < vertices.length - 1; j++) {
				vertices[j] = vertices[j + 1];
			}
			vertices[vertices.length - 1] = temp;
		}
		for (int i = 0; i < vertices.length; i++) {
			Vertex v = vertices[i];
			float[] colors = { ColorHelper.getRed(v.color) / 255f, ColorHelper.getGreen(v.color) / 255f, ColorHelper.getBlue(v.color) / 255f, ColorHelper.getAlpha(v.color) / 255f };
			for (int e = 0; e < format.getElementCount(); e++) {
				VertexFormatElement element = format.getElement(e);
				switch (element.getUsage()) {
				case COLOR:
					if (element.getSize() == 4)
						if (unpacked)
							builder.put(e, colors);
						else
							bb.putInt(v.color);
					else
						nope();
					break;
				case NORMAL:
					if (element.getSize() == 3) {
						if (unpacked)
							builder.put(e, normal.x, normal.y, normal.z);
						else {
							if (false) {
								bb.putShort((short) ((int) (normal.x * 127) & 255));
								bb.putShort((short) ((int) (normal.y * 127) & 255));
								bb.putShort((short) ((int) (normal.z * 127) & 255));
							} else {
								int x = ((byte) Math.round(normal.x * 127)) & 0xFF;
								int y = ((byte) Math.round(normal.y * 127)) & 0xFF;
								int z = ((byte) Math.round(normal.z * 127)) & 0xFF;
								int normalI = x | (y << 0x08) | (z << 0x10);
								ByteBuffer bb2 = ByteBuffer.allocate(4);
								bb2.putInt(normalI);
								byte[] bs = Arrays.copyOfRange(bb2.array(), 1, 4);
								for (int j = 0; j < bs.length; j++)
									bb.put(bs[j]);
							}
						}
					} else
						nope();
					break;
				case PADDING:
					if (unpacked)
						builder.put(e, v.padding);
					else
						bb.put(v.padding);
					break;
				case POSITION:
					if (unpacked)
						builder.put(e, v.pos.x, v.pos.y, v.pos.z);
					else {
						bb.putFloat(v.pos.x);
						bb.putFloat(v.pos.y);
						bb.putFloat(v.pos.z);
					}
					break;
				case UV:
					if (unpacked) {
						if (element.getIndex() == 0) {
							builder.put(e, sprite.getInterpolatedU(v.uf), sprite.getInterpolatedV(v.vf));
						} else {
							builder.put(e, sprite.getInterpolatedU(v.us), sprite.getInterpolatedV(v.vs));
						}
					} else {
						if (element.getIndex() == 0) {
							bb.putFloat(sprite.getInterpolatedU(v.uf));
							bb.putFloat(sprite.getInterpolatedV(v.vf));
						} else {
							bb.putShort((short) sprite.getInterpolatedU(v.us));
							bb.putShort((short) sprite.getInterpolatedU(v.vs));
						}
					}
					break;
				default:
					break;
				}
			}
		}
		if (unpacked)
			return builder.build();
		bb.clear();
		IntBuffer ib = bb.asIntBuffer();
		int[] ints = new int[ib.remaining()];
		ib.get(ints);
		BakedQuad quad = new BakedQuad(ints, tint, face, sprite, diffuse, format);
		if (unpacked) {
			builder = new UnpackedBakedQuad.Builder(format);
			quad.pipe(builder);
			return builder.build();
		}
		return quad;
	}

	private static void nope() {
		throw new RuntimeException("nope");
	}

	public BakedQuadBuilder addVertex(Vertex vertex) {
		Validate.isTrue(vertCount < 4, "too many vertices");
		vertices[vertCount++] = vertex;
		return this;
	}

	public BakedQuadBuilder setQuadTint(int tint) {
		this.tint = tint;
		return this;
	}

	public BakedQuadBuilder setQuadOrientation(EnumFacing orientation) {
		if (true)
			throw new UnsupportedOperationException();
		this.face = Objects.requireNonNull(orientation);
		return this;
	}

	public BakedQuadBuilder setApplyDiffuseLighting(boolean diffuse) {
		this.diffuse = diffuse;
		return this;
	}

	public BakedQuadBuilder setTextureRotation(Rotation rotation) {
		this.textureRotation = Objects.requireNonNull(rotation);
		return this;
	}

	public enum Rotation {
		D_0(0), D_90(1), D_180(2), D_270(3);

		int t;

		Rotation(int t) {
			this.t = t;
		}
	}

	public static class Vertex {
		int color = -1;
		Vector3f pos;
		short us, vs;
		float uf, vf;
		byte padding;

		public static class Builder {
			private Vertex vertex = new Vertex();

			public Builder setColor(int color) {
				vertex.color = color;
				return this;
			}

			public Builder setPos(Vector3f pos) {
				vertex.pos = pos;
				return this;
			}

			public Builder setPos(float x, float y, float z) {
				vertex.pos = new Vector3f(x, y, z);
				return this;
			}

			public Builder setUVshort(short u, short v) {
				vertex.us = u;
				vertex.vs = v;
				return this;
			}

			public Builder setUVfloat(float u, float v) {
				vertex.uf = u;
				vertex.vf = v;
				return this;
			}

			public Builder setPadding(byte padding) {
				vertex.padding = padding;
				return this;
			}

			public Vertex build() {
				Vertex v = new Vertex();
				v.color = vertex.color;
				v.pos = Objects.requireNonNull(vertex.pos, "pos required");
				v.us = vertex.us;
				v.vs = vertex.vs;
				v.uf = vertex.uf;
				v.vf = vertex.vf;
				v.padding = vertex.padding;
				return v;
			}
		}

	}

}
