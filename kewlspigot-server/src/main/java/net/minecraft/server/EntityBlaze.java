package net.minecraft.server;

public class EntityBlaze extends EntityMonster {
    private float a = 0.5F;
    private int b;

    public EntityBlaze(World var1) {
        super(var1);
        this.fireProof = true;
        this.b_ = 10;
        this.goalSelector.a(4, new EntityBlaze.PathfinderGoalBlazeFireball(this));
        this.goalSelector.a(5, new PathfinderGoalMoveTowardsRestriction(this, 1.0D));
        this.goalSelector.a(7, new PathfinderGoalRandomStroll(this, 1.0D));
        this.goalSelector.a(8, new PathfinderGoalLookAtPlayer(this, EntityHuman.class, 8.0F));
        this.goalSelector.a(8, new PathfinderGoalRandomLookaround(this));
        this.targetSelector.a(1, new PathfinderGoalHurtByTarget(this, true, new Class[0]));
        this.targetSelector.a(2, new PathfinderGoalNearestAttackableTarget(this, EntityHuman.class, true));
    }

    protected void initAttributes() {
        super.initAttributes();
        this.getAttributeInstance(GenericAttributes.ATTACK_DAMAGE).setValue(6.0D);
        this.getAttributeInstance(GenericAttributes.MOVEMENT_SPEED).setValue(0.23000000417232513D);
        this.getAttributeInstance(GenericAttributes.FOLLOW_RANGE).setValue(48.0D);
    }

    protected void h() {
        super.h();
        this.datawatcher.a(16, new Byte((byte)0));
    }

    protected String z() {
        return "mob.blaze.breathe";
    }

    protected String bo() {
        return "mob.blaze.hit";
    }

    protected String bp() {
        return "mob.blaze.death";
    }

    public float c(float var1) {
        return 1.0F;
    }

    public void m() {
        if (!this.onGround && this.motY < 0.0D) {
            this.motY *= 0.6D;
        }

        if (this.world.isClientSide) {
            if (this.random.nextInt(24) == 0 && !this.R()) {
                this.world.a(this.locX + 0.5D, this.locY + 0.5D, this.locZ + 0.5D, "fire.fire", 1.0F + this.random.nextFloat(), this.random.nextFloat() * 0.7F + 0.3F, false);
            }

            for(int var1 = 0; var1 < 2; ++var1) {
                this.world.addParticle(EnumParticle.SMOKE_LARGE, this.locX + (this.random.nextDouble() - 0.5D) * (double)this.width, this.locY + this.random.nextDouble() * (double)this.length, this.locZ + (this.random.nextDouble() - 0.5D) * (double)this.width, 0.0D, 0.0D, 0.0D, new int[0]);
            }
        }

        super.m();
    }

    protected void E() {
        if (this.U()) {
            this.damageEntity(DamageSource.DROWN, 1.0F);
        }

        --this.b;
        if (this.b <= 0) {
            this.b = 100;
            this.a = 0.5F + (float)this.random.nextGaussian() * 3.0F;
        }

        EntityLiving var1 = this.getGoalTarget();
        if (var1 != null && var1.locY + (double)var1.getHeadHeight() > this.locY + (double)this.getHeadHeight() + (double)this.a) {
            this.motY += (0.30000001192092896D - this.motY) * 0.30000001192092896D;
            this.ai = true;
        }

        super.E();
    }

    public void e(float var1, float var2) {
    }

    protected Item getLoot() {
        return Items.BLAZE_ROD;
    }

    public boolean isBurning() {
        return this.n();
    }

    public void dropDeathLoot(boolean var1, int var2) {
        if (var1) {
            int var3 = this.random.nextInt(2 + var2);

            for(int var4 = 0; var4 < var3; ++var4) {
                this.a(Items.BLAZE_ROD, 1);
            }
        }

    }

    public boolean n() {
        return (this.datawatcher.getByte(16) & 1) != 0;
    }

    public void a(boolean var1) {
        byte var2 = this.datawatcher.getByte(16);
        if (var1) {
            var2 = (byte)(var2 | 1);
        } else {
            var2 &= -2;
        }

        this.datawatcher.watch(16, var2);
    }

    protected boolean n_() {
        return true;
    }

    public static class PathfinderGoalBlazeFireball extends PathfinderGoal {
        private EntityBlaze a;
        private int b;
        private int c;

        public PathfinderGoalBlazeFireball(EntityBlaze var1) {
            this.a = var1;
            this.a(3);
        }

        public boolean a() {
            EntityLiving var1 = this.a.getGoalTarget();
            return var1 != null && var1.isAlive();
        }

        public void c() {
            this.b = 0;
        }

        public void d() {
            this.a.a(false);
        }

        public void e() {
            --this.c;
            EntityLiving var1 = this.a.getGoalTarget();
            double var2 = this.a.h(var1);
            if (var2 < 4.0D) {
                if (this.c <= 0) {
                    this.c = 20;
                    this.a.r(var1);
                }

                this.a.getControllerMove().a(var1.locX, var1.locY, var1.locZ, 1.0D);
            } else if (var2 < 256.0D) {
                double var4 = var1.locX - this.a.locX;
                double var6 = var1.getBoundingBox().b + (double)(var1.length / 2.0F) - (this.a.locY + (double)(this.a.length / 2.0F));
                double var8 = var1.locZ - this.a.locZ;
                if (this.c <= 0) {
                    ++this.b;
                    if (this.b == 1) {
                        this.c = 60;
                        this.a.a(true);
                    } else if (this.b <= 4) {
                        this.c = 6;
                    } else {
                        this.c = 100;
                        this.b = 0;
                        this.a.a(false);
                    }

                    if (this.b > 1) {
                        float var10 = MathHelper.c(MathHelper.sqrt(var2)) * 0.5F;
                        this.a.world.a((EntityHuman)null, 1009, new BlockPosition((int)this.a.locX, (int)this.a.locY, (int)this.a.locZ), 0);

                        for(int var11 = 0; var11 < 1; ++var11) {
                            EntitySmallFireball var12 = new EntitySmallFireball(this.a.world, this.a, var4 + this.a.bc().nextGaussian() * (double)var10, var6, var8 + this.a.bc().nextGaussian() * (double)var10);
                            var12.locY = this.a.locY + (double)(this.a.length / 2.0F) + 0.5D;
                            this.a.world.addEntity(var12);
                        }
                    }
                }

                this.a.getControllerLook().a(var1, 10.0F, 10.0F);
            } else {
                this.a.getNavigation().n();
                this.a.getControllerMove().a(var1.locX, var1.locY, var1.locZ, 1.0D);
            }

            super.e();
        }
    }
}
