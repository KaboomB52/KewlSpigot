package net.minecraft.server;

/**
 * @since 11/12/2017
 */
import java.util.Random;

public class BlockStone extends Block {
	public static final BlockStateEnum<BlockStone.EnumStoneVariant> VARIANT = BlockStateEnum.of("variant", BlockStone.EnumStoneVariant.class);

	public BlockStone() {
		super(Material.STONE);
		this.j(this.blockStateList.getBlockData().set(VARIANT, BlockStone.EnumStoneVariant.STONE));
		this.a(CreativeModeTab.b);
	}

	public String getName() {
		return LocaleI18n.get(this.a() + "." + BlockStone.EnumStoneVariant.STONE.d() + ".name");
	}

	public MaterialMapColor g(IBlockData var1) {
		return ((BlockStone.EnumStoneVariant)var1.get(VARIANT)).c();
	}

	public Item getDropType(IBlockData var1, Random var2, int var3) {
		return var1.get(VARIANT) == BlockStone.EnumStoneVariant.STONE ? Item.getItemOf(Blocks.COBBLESTONE) : Item.getItemOf(Blocks.STONE);
	}

	public int getDropData(IBlockData var1) {
		return ((BlockStone.EnumStoneVariant)var1.get(VARIANT)).a();
	}

	public IBlockData fromLegacyData(int var1) {
		return this.getBlockData().set(VARIANT, BlockStone.EnumStoneVariant.a(var1));
	}

	public int toLegacyData(IBlockData var1) {
		return ((BlockStone.EnumStoneVariant)var1.get(VARIANT)).a();
	}

	public BlockStateList getStateList() {
		return new BlockStateList(this, new IBlockState[]{VARIANT});
	}

	public static enum EnumStoneVariant implements INamable {
		STONE(0, MaterialMapColor.m, "stone");
		/*GRANITE(1, MaterialMapColor.l, "granite"),
		GRANITE_SMOOTH(2, MaterialMapColor.l, "smooth_granite", "graniteSmooth"),
		DIORITE(3, MaterialMapColor.p, "diorite"),
		DIORITE_SMOOTH(4, MaterialMapColor.p, "smooth_diorite", "dioriteSmooth"),
		ANDESITE(5, MaterialMapColor.m, "andesite"),
		ANDESITE_SMOOTH(6, MaterialMapColor.m, "smooth_andesite", "andesiteSmooth");*/

		private static final BlockStone.EnumStoneVariant[] h = new BlockStone.EnumStoneVariant[values().length];
		private final int i;
		private final String j;
		private final String k;
		private final MaterialMapColor l;

		private EnumStoneVariant(int var3, MaterialMapColor var4, String var5) {
			this(var3, var4, var5, var5);
		}

		private EnumStoneVariant(int var3, MaterialMapColor var4, String var5, String var6) {
			this.i = var3;
			this.j = var5;
			this.k = var6;
			this.l = var4;
		}

		public int a() {
			return this.i;
		}

		public MaterialMapColor c() {
			return this.l;
		}

		public String toString() {
			return this.j;
		}

		public static BlockStone.EnumStoneVariant a(int var0) {
			if (var0 < 0 || var0 >= h.length) {
				var0 = 0;
			}

			return h[var0];
		}

		public String getName() {
			return this.j;
		}

		public String d() {
			return this.k;
		}

		static {
			BlockStone.EnumStoneVariant[] var0 = values();
			int var1 = var0.length;

			for(int var2 = 0; var2 < var1; ++var2) {
				BlockStone.EnumStoneVariant var3 = var0[var2];
				h[var3.a()] = var3;
			}

		}
	}
}

