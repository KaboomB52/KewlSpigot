package net.minecraft.server;

import com.google.common.collect.*;
import java.util.*;

public class ItemPotion extends Item
{
    private Map<Integer, List<MobEffect>> a;
    private static final Map<List<MobEffect>, Integer> b;
    
    public ItemPotion() {
        this.a = Maps.newHashMap();
        this.c(1);
        this.a(true);
        this.setMaxDurability(0);
        this.a(CreativeModeTab.k);
    }
    
    public List<MobEffect> h(final ItemStack itemStack) {
        if (!itemStack.hasTag() || !itemStack.getTag().hasKeyOfType("CustomPotionEffects", 9)) {
            List<MobEffect> effects = this.a.get(itemStack.getData());
            if (effects == null) {
                effects = PotionBrewer.getEffects(itemStack.getData(), false);
                this.a.put(itemStack.getData(), effects);
            }
            return effects;
        }
        final ArrayList<MobEffect> arrayList = Lists.newArrayList();
        final NBTTagList list = itemStack.getTag().getList("CustomPotionEffects", 10);
        for (int i = 0; i < list.size(); ++i) {
            final MobEffect b = MobEffect.b(list.get(i));
            if (b != null) {
                arrayList.add(b);
            }
        }
        return arrayList;
    }
    
    public List<MobEffect> e(final int n) {
        List<MobEffect> effects = this.a.get(n);
        if (effects == null) {
            effects = PotionBrewer.getEffects(n, false);
            this.a.put(n, effects);
        }
        return effects;
    }
    
    @Override
    public ItemStack b(final ItemStack itemStack, final World world, final EntityHuman entityHuman) {
        if (!entityHuman.abilities.canInstantlyBuild) {
            --itemStack.count;
        }
        if (!world.isClientSide) {
            final List<MobEffect> h = this.h(itemStack);
            if (h != null) {
                final Iterator<MobEffect> iterator = h.iterator();
                while (iterator.hasNext()) {
                    entityHuman.addEffect(new MobEffect(iterator.next()));
                }
            }
        }
        entityHuman.b(StatisticList.USE_ITEM_COUNT[Item.getId(this)]);
        if (!entityHuman.abilities.canInstantlyBuild) {
            if (itemStack.count <= 0) {
                return new ItemStack(Items.GLASS_BOTTLE);
            }
            entityHuman.inventory.pickup(new ItemStack(Items.GLASS_BOTTLE));
        }
        return itemStack;
    }
    
    @Override
    public int d(final ItemStack itemStack) {
        return 32;
    }
    
    @Override
    public EnumAnimation e(final ItemStack itemStack) {
        return EnumAnimation.DRINK;
    }
    
    @Override
    public ItemStack a(final ItemStack itemStack, final World world, final EntityHuman entityHuman) {
        if (f(itemStack.getData())) {
            if (!entityHuman.abilities.canInstantlyBuild) {
                --itemStack.count;
            }
            world.makeSound(entityHuman, "random.bow", 0.5f, 0.4f / (ItemPotion.g.nextFloat() * 0.4f + 0.8f));
            if (!world.isClientSide) {
                world.addEntity(new EntityPotion(world, entityHuman, itemStack));
            }
            entityHuman.b(StatisticList.USE_ITEM_COUNT[Item.getId(this)]);
            return itemStack;
        }
        entityHuman.a(itemStack, this.d(itemStack));
        return itemStack;
    }

    public static boolean f(int var0) {
        return (var0 & 16384) != 0;
    }
    
    @Override
    public String a(final ItemStack itemStack) {
        if (itemStack.getData() == 0) {
            return LocaleI18n.get("item.emptyPotion.name").trim();
        }
        String string = "";
        if (f(itemStack.getData())) {
            string = LocaleI18n.get("potion.prefix.grenade").trim() + " ";
        }
        final List<MobEffect> h = Items.POTION.h(itemStack);
        if (h != null && !h.isEmpty()) {
            return string + LocaleI18n.get(h.get(0).g() + ".postfix").trim();
        }
        return LocaleI18n.get(PotionBrewer.c(itemStack.getData())).trim() + " " + super.a(itemStack);
    }
    
    static {
        b = Maps.newLinkedHashMap();
    }
}
