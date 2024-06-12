package net.minecraft.server;

import org.eytril.spigot.event.inventory.PrepareAnvilRepairEvent;

import java.util.Iterator;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.craftbukkit.inventory.CraftInventoryView;
import org.bukkit.craftbukkit.inventory.CraftItemStack;

public class ContainerAnvil extends Container {

	private final EntityHuman k;
	public int a;
	private IInventory g = new InventoryCraftResult();
	private World i;
	private BlockPosition j;
	private int m;
	private String l;
	private CraftInventoryView bukkitEntity = null;
	private PlayerInventory player;
	private IInventory h = new InventorySubcontainer("Repair", true, 2) {
		public void update() {
			super.update();
			ContainerAnvil.this.a(this);
		}
	};

	public ContainerAnvil(PlayerInventory playerinventory, final World world, final BlockPosition blockposition, EntityHuman entityhuman) {
		this.player = playerinventory;
		this.j = blockposition;
		this.i = world;
		this.k = entityhuman;
		this.a(new Slot(this.h, 0, 27, 47));
		this.a(new Slot(this.h, 1, 76, 47));
		this.a(new Slot(this.g, 2, 134, 47) {
			public boolean isAllowed(ItemStack itemstack) {
				return false;
			}

			public boolean isAllowed(EntityHuman entityhuman) {
				return (entityhuman.abilities.canInstantlyBuild || entityhuman.expLevel >= ContainerAnvil.this.a) &&
				       ContainerAnvil.this.a > 0 && this.hasItem();
			}

			public void a(EntityHuman entityhuman, ItemStack itemstack) {
				if (!entityhuman.abilities.canInstantlyBuild) {
					entityhuman.levelDown(-ContainerAnvil.this.a);
				}

				ContainerAnvil.this.h.setItem(0, null);
				if (ContainerAnvil.this.m > 0) {
					ItemStack itemstack1 = ContainerAnvil.this.h.getItem(1);

					if (itemstack1 != null && itemstack1.count > ContainerAnvil.this.m) {
						itemstack1.count -= ContainerAnvil.this.m;
						ContainerAnvil.this.h.setItem(1, itemstack1);
					} else {
						ContainerAnvil.this.h.setItem(1, null);
					}
				} else {
					ContainerAnvil.this.h.setItem(1, null);
				}

				ContainerAnvil.this.a = 0;
				IBlockData iblockdata = world.getType(blockposition);

				if (!entityhuman.abilities.canInstantlyBuild && !world.isClientSide &&
				    iblockdata.getBlock() == Blocks.ANVIL && entityhuman.bc().nextFloat() < 0.12F) {
					int i = iblockdata.get(BlockAnvil.DAMAGE);

					++i;

					if (i > 2) {
						world.setAir(blockposition);
						world.triggerEffect(1020, blockposition, 0);
					} else {
						world.setTypeAndData(blockposition, iblockdata.set(BlockAnvil.DAMAGE, i), 2);
						world.triggerEffect(1021, blockposition, 0);
					}
				} else if (!world.isClientSide) {
					world.triggerEffect(1021, blockposition, 0);
				}
			}
		});

		int i;

		for (i = 0; i < 3; ++i) {
			for (int j = 0; j < 9; ++j) {
				this.a(new Slot(playerinventory, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
			}
		}

		for (i = 0; i < 9; ++i) {
			this.a(new Slot(playerinventory, i, 8 + i * 18, 142));
		}

	}

	public void a(IInventory iinventory) {
		super.a(iinventory);
		if (iinventory == this.h) {
			this.e();
		}

	}

	public void e() {
		ItemStack itemstack = this.h.getItem(0);

		this.a = 0;
		int i = 0;
		byte b0 = 0;
		int j = 0;

		if (itemstack == null) {
			this.g.setItem(0, null);
			this.a = 0;
		} else {
			ItemStack clonedItemStack = itemstack.cloneItemStack();
			ItemStack nextItemStack = this.h.getItem(1);
			Map map = EnchantmentManager.a(clonedItemStack);
			boolean flag = false;
			int k = b0 + itemstack.getRepairCost() + (nextItemStack == null ? 0 : nextItemStack.getRepairCost());

			this.m = 0;
			int l;
			int i1;
			int j1;
			int k1;
			int l1;
			Iterator iterator;
			Enchantment enchantment;

			if (nextItemStack != null) {
				flag = nextItemStack.getItem() == Items.ENCHANTED_BOOK && Items.ENCHANTED_BOOK.h(nextItemStack).size() > 0;

				if (clonedItemStack.e() && clonedItemStack.getItem().a(itemstack, nextItemStack)) {
					l = Math.min(clonedItemStack.h(), clonedItemStack.j() / 4);

					if (l <= 0) {
						this.g.setItem(0, null);
						this.a = 0;
						return;
					}

					for (i1 = 0; l > 0 && i1 < nextItemStack.count; ++i1) {
						j1 = clonedItemStack.h() - l;
						clonedItemStack.setData(j1);
						i += Math.max(1, l / 100) + map.size();
						l = Math.min(clonedItemStack.h(), clonedItemStack.j() / 4);
					}

					this.m = i1;
				} else {
					if (!flag && (clonedItemStack.getItem() != nextItemStack.getItem() || !clonedItemStack.e())) {
						this.g.setItem(0, null);
						this.a = 0;
						return;
					}

					if (clonedItemStack.e() && !flag) {
						l = itemstack.j() - itemstack.h();
						i1 = nextItemStack.j() - nextItemStack.h();
						j1 = i1 + clonedItemStack.j() * 12 / 100;
						int i2 = l + j1;

						k1 = clonedItemStack.j() - i2;

						if (k1 < 0) {
							k1 = 0;
						}

						if (k1 < clonedItemStack.getData()) {
							clonedItemStack.setData(k1);
							i += Math.max(1, j1 / 100);
						}
					}

					Map map1 = EnchantmentManager.a(nextItemStack);

					iterator = map1.keySet().iterator();

					while (iterator.hasNext()) {
						j1 = (Integer) iterator.next();
						enchantment = Enchantment.getById(j1);
						k1 = map.containsKey(j1) ? (Integer) map.get(j1) : 0;
						l1 = (Integer) map1.get(j1);

						int j2;

						if (k1 == l1) {
							++l1;
							j2 = l1;
						} else {
							j2 = Math.max(l1, k1);
						}

						l1 = j2;
						int k2 = l1 - k1;
						boolean flag1 = enchantment.canEnchant(itemstack);

						if (this.k.abilities.canInstantlyBuild || itemstack.getItem() == Items.ENCHANTED_BOOK) {
							flag1 = true;
						}

						Iterator iterator1 = map.keySet().iterator();

						while (iterator1.hasNext()) {
							int l2 = ((Integer) iterator1.next()).intValue();

							if (l2 != j1 && !enchantment.a(Enchantment.getById(l2))) {
								flag1 = false;
								i += k2;
							}
						}

						if (flag1) {
							if (l1 > enchantment.getMaxLevel()) {
								l1 = enchantment.getMaxLevel();
							}

							map.put(Integer.valueOf(j1), Integer.valueOf(l1));
							int i3 = 0;

							switch (enchantment.getRandomWeight()) {
								case 1:
									i3 = 8;
									break;

								case 2:
									i3 = 4;

								case 3:
								case 4:
								case 6:
								case 7:
								case 8:
								case 9:
								default:
									break;

								case 5:
									i3 = 2;
									break;

								case 10:
									i3 = 1;
							}

							if (flag) {
								i3 = Math.max(1, i3 / 2);
							}

							i += i3 * k2;
						}
					}
				}
			}

			if (StringUtils.isBlank(this.l)) {
				if (itemstack.hasName()) {
					j = itemstack.g() ? 7 : itemstack.count * 5;
					i += j;
					clonedItemStack.r();
				}
			} else if (!this.l.equals(itemstack.getName())) {
				j = itemstack.g() ? 7 : itemstack.count * 5;
				i += j;
				if (itemstack.hasName()) {
					k += j / 2;
				}

				clonedItemStack.c(this.l);
			}

			l = 0;

			for (iterator = map.keySet().iterator(); iterator.hasNext(); k += l + k1 * l1) {
				j1 = (Integer) iterator.next();
				enchantment = Enchantment.getById(j1);
				k1 = (Integer) map.get(j1);
				l1 = 0;

				++l;

				switch (enchantment.getRandomWeight()) {
					case 1:
						l1 = 8;
						break;

					case 2:
						l1 = 4;

					case 3:
					case 4:
					case 6:
					case 7:
					case 8:
					case 9:
					default:
						break;

					case 5:
						l1 = 2;
						break;

					case 10:
						l1 = 1;
				}

				if (flag) {
					l1 = Math.max(1, l1 / 2);
				}
			}

			if (flag) {
				k = Math.max(1, k / 2);
			}

			this.a = k + i;
			if (i <= 0) {
				clonedItemStack = null;
			}

			if (j == i && j > 0 && this.a >= 40) {
				this.a = 39;
			}

			if (this.a >= 40 && !this.k.abilities.canInstantlyBuild) {
				clonedItemStack = null;
			}

			if (clonedItemStack != null) {
				i1 = clonedItemStack.getRepairCost();
				if (nextItemStack != null && i1 < nextItemStack.getRepairCost()) {
					i1 = nextItemStack.getRepairCost();
				}

				if (clonedItemStack.hasName()) {
					i1 -= 9;
				}

				if (i1 < 0) {
					i1 = 0;
				}

				i1 += 2;
				clonedItemStack.setRepairCost(i1);
				EnchantmentManager.a(map, clonedItemStack);

				if (clonedItemStack != null) {
					PrepareAnvilRepairEvent prepareAnvilRepairEvent = new PrepareAnvilRepairEvent(this.k
							.getBukkitEntity(), this.getBukkitView(), this.k.world.getWorld().getBlockAt(this.j.getX
							(), this.j.getY(), this.j.getZ()), CraftItemStack.asBukkitCopy(itemstack), CraftItemStack
							.asBukkitCopy(nextItemStack), CraftItemStack.asBukkitCopy(clonedItemStack));
					Bukkit.getPluginManager().callEvent(prepareAnvilRepairEvent);

					if (prepareAnvilRepairEvent.isCancelled() ||
					    prepareAnvilRepairEvent.getResult().getType() == Material.AIR) {
						return;
					}

					clonedItemStack = CraftItemStack.asNMSCopy(prepareAnvilRepairEvent.getResult());

				}
			}

			this.g.setItem(0, clonedItemStack);
			this.b();
		}
	}

	public void addSlotListener(ICrafting icrafting) {
		super.addSlotListener(icrafting);
		icrafting.setContainerData(this, 0, this.a);
	}

	public void b(EntityHuman entityhuman) {
		super.b(entityhuman);

		if (!this.i.isClientSide) {
			for (int i = 0; i < this.h.getSize(); ++i) {
				ItemStack itemstack = this.h.splitWithoutUpdate(i);

				if (itemstack != null) {
					entityhuman.drop(itemstack, false);
				}
			}
		}
	}

	public boolean a(EntityHuman entityhuman) {
		if (!this.checkReachable) {
			return true;
		}

		return this.i.getType(this.j).getBlock() != Blocks.ANVIL ? false : entityhuman.e((double) this.j.getX() + 0.5D,
				(double) this.j.getY() + 0.5D, (double) this.j.getZ() + 0.5D) <= 64.0D;
	}

	public ItemStack b(EntityHuman entityhuman, int i) {
		ItemStack itemstack = null;
		Slot slot = this.c.get(i);

		if (slot != null && slot.hasItem()) {
			ItemStack itemstack1 = slot.getItem();
			itemstack = itemstack1.cloneItemStack();

			if (i == 2) {
				if (!this.a(itemstack1, 3, 39, true)) {
					return null;
				}

				slot.a(itemstack1, itemstack);
			} else if (i != 0 && i != 1) {
				if (i >= 3 && i < 39 && !this.a(itemstack1, 0, 2, false)) {
					return null;
				}
			} else if (!this.a(itemstack1, 3, 39, false)) {
				return null;
			}

			if (itemstack1.count == 0) {
				slot.set(null);
			} else {
				slot.f();
			}

			if (itemstack1.count == itemstack.count) {
				return null;
			}

			slot.a(entityhuman, itemstack1);
		}

		return itemstack;
	}

	public void a(String s) {
		this.l = s;
		if (this.getSlot(2).hasItem()) {
			ItemStack itemstack = this.getSlot(2).getItem();

			if (StringUtils.isBlank(s)) {
				itemstack.r();
			} else {
				itemstack.c(this.l);
			}
		}

		this.e();
	}

	@Override
	public CraftInventoryView getBukkitView() {
		if (bukkitEntity != null) {
			return bukkitEntity;
		}

		org.bukkit.craftbukkit.inventory.CraftInventory inventory = new org.bukkit.craftbukkit.inventory.CraftInventoryAnvil(this.h, this.g);

		bukkitEntity = new CraftInventoryView(this.player.player.getBukkitEntity(), inventory, this);

		return bukkitEntity;
	}

}
