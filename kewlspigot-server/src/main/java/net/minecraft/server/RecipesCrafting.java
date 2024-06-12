package net.minecraft.server;

public class RecipesCrafting {

	public void a(CraftingManager craftingManager) {
		craftingManager.registerShapedRecipe(new ItemStack(Blocks.CHEST), "###", "# #", "###", '#', Blocks.PLANKS);
		craftingManager.registerShapedRecipe(new ItemStack(Blocks.TRAPPED_CHEST), "#-", '#', Blocks.CHEST, '-', Blocks.TRIPWIRE_HOOK);
		craftingManager.registerShapedRecipe(new ItemStack(Blocks.ENDER_CHEST), "###", "#E#", "###", '#', Blocks.OBSIDIAN, 'E', Items.ENDER_EYE);
		craftingManager.registerShapedRecipe(new ItemStack(Blocks.FURNACE), "###", "# #", "###", '#', Blocks.COBBLESTONE);
		craftingManager.registerShapedRecipe(new ItemStack(Blocks.CRAFTING_TABLE), "##", "##", '#', Blocks.PLANKS);
		craftingManager.registerShapedRecipe(new ItemStack(Blocks.SANDSTONE), "##", "##", '#', new ItemStack(Blocks.SAND, 1, BlockSand.EnumSandVariant.SAND.a()));
		craftingManager.registerShapedRecipe(new ItemStack(Blocks.RED_SANDSTONE), "##", "##", '#', new ItemStack(Blocks.SAND, 1, BlockSand.EnumSandVariant.RED_SAND.a()));
		craftingManager.registerShapedRecipe(new ItemStack(Blocks.SANDSTONE, 4, BlockSandStone.EnumSandstoneVariant.SMOOTH.a()), "##", "##", '#', new ItemStack(Blocks.SANDSTONE, 1, BlockSandStone.EnumSandstoneVariant.DEFAULT.a()));
		craftingManager.registerShapedRecipe(new ItemStack(Blocks.RED_SANDSTONE, 4, BlockRedSandstone.EnumRedSandstoneVariant.SMOOTH.a()), "##", "##", '#', new ItemStack(Blocks.RED_SANDSTONE, 1, BlockRedSandstone.EnumRedSandstoneVariant.DEFAULT.a()));
		craftingManager.registerShapedRecipe(new ItemStack(Blocks.SANDSTONE, 1, BlockSandStone.EnumSandstoneVariant.CHISELED.a()), "#", "#", '#', new ItemStack(Blocks.STONE_SLAB, 1, BlockDoubleStepAbstract.EnumStoneSlabVariant.SAND.a()));
		craftingManager.registerShapedRecipe(new ItemStack(Blocks.RED_SANDSTONE, 1, BlockRedSandstone.EnumRedSandstoneVariant.CHISELED.a()), "#", "#", '#', new ItemStack(Blocks.STONE_SLAB2, 1, BlockDoubleStoneStepAbstract.EnumStoneSlab2Variant.RED_SANDSTONE.a()));
		craftingManager.registerShapedRecipe(new ItemStack(Blocks.QUARTZ_BLOCK, 1, BlockQuartz.EnumQuartzVariant.CHISELED.a()), "#", "#", '#', new ItemStack(Blocks.STONE_SLAB, 1, BlockDoubleStepAbstract.EnumStoneSlabVariant.QUARTZ.a()));
		craftingManager.registerShapedRecipe(new ItemStack(Blocks.QUARTZ_BLOCK, 2, BlockQuartz.EnumQuartzVariant.LINES_Y.a()), "#", "#", '#', new ItemStack(Blocks.QUARTZ_BLOCK, 1, BlockQuartz.EnumQuartzVariant.DEFAULT.a()));
		craftingManager.registerShapedRecipe(new ItemStack(Blocks.STONEBRICK, 4), "##", "##", '#', new ItemStack(Blocks.STONE, 1, BlockStone.EnumStoneVariant.STONE.a()));
		craftingManager.registerShapedRecipe(new ItemStack(Blocks.STONEBRICK, 1, BlockSmoothBrick.P), "#", "#", '#', new ItemStack(Blocks.STONE_SLAB, 1, BlockDoubleStepAbstract.EnumStoneSlabVariant.SMOOTHBRICK.a()));
		craftingManager.registerShapelessRecipe(new ItemStack(Blocks.STONEBRICK, 1, BlockSmoothBrick.N), Blocks.STONEBRICK, Blocks.VINE);
		craftingManager.registerShapelessRecipe(new ItemStack(Blocks.MOSSY_COBBLESTONE, 1), Blocks.COBBLESTONE, Blocks.VINE);
		craftingManager.registerShapedRecipe(new ItemStack(Blocks.IRON_BARS, 16), "###", "###", '#', Items.IRON_INGOT);
		craftingManager.registerShapedRecipe(new ItemStack(Blocks.GLASS_PANE, 16), "###", "###", '#', Blocks.GLASS);
		craftingManager.registerShapedRecipe(new ItemStack(Blocks.REDSTONE_LAMP, 1), " R ", "RGR", " R ", 'R', Items.REDSTONE, 'G', Blocks.GLOWSTONE);
		craftingManager.registerShapedRecipe(new ItemStack(Blocks.BEACON, 1), "GGG", "GSG", "OOO", 'G', Blocks.GLASS, 'S', Items.NETHER_STAR, 'O', Blocks.OBSIDIAN);
		craftingManager.registerShapedRecipe(new ItemStack(Blocks.NETHER_BRICK, 1), "NN", "NN", 'N', Items.NETHERBRICK);
		craftingManager.registerShapedRecipe(new ItemStack(Blocks.DIRT, 4, BlockDirt.EnumDirtVariant.COARSE_DIRT.a()), "DG", "GD", 'D', new ItemStack(Blocks.DIRT, 1, BlockDirt.EnumDirtVariant.DIRT.a()), 'G', Blocks.GRAVEL);
		craftingManager.registerShapedRecipe(new ItemStack(Blocks.PRISMARINE, 1, BlockPrismarine.b), "SS", "SS", 'S', Items.PRISMARINE_SHARD);
		craftingManager.registerShapedRecipe(new ItemStack(Blocks.PRISMARINE, 1, BlockPrismarine.N), "SSS", "SSS", "SSS", 'S', Items.PRISMARINE_SHARD);
		craftingManager.registerShapedRecipe(new ItemStack(Blocks.PRISMARINE, 1, BlockPrismarine.O), "SSS", "SIS", "SSS", 'S', Items.PRISMARINE_SHARD, 'I', new ItemStack(Items.DYE, 1, EnumColor.BLACK.getInvColorIndex()));
		craftingManager.registerShapedRecipe(new ItemStack(Blocks.SEA_LANTERN, 1, 0), "SCS", "CCC", "SCS", 'S', Items.PRISMARINE_SHARD, 'C', Items.PRISMARINE_CRYSTALS);
	}
}

