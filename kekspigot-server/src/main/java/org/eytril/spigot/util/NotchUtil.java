package org.eytril.spigot.util;

import net.minecraft.server.EnumDirection;
import net.minecraft.server.MathHelper;
import net.minecraft.server.Vec3D;
import net.minecraft.server.WorldSettings;

public class NotchUtil {

    // Stitched this together from old source lol
    // NOTE: y is locY + headHeight
    public static EnumDirection getDirection(float pitch, float yaw, double x, double y, double z, WorldSettings.EnumGamemode gamemode) {
        Vec3D firstVector = new Vec3D(x, y, z);

        float f3 = MathHelper.cos(-yaw * 0.017453292F - 3.1415927F);
        float f4 = MathHelper.sin(-yaw * 0.017453292F - 3.1415927F);
        float f5 = -MathHelper.cos(-pitch * 0.017453292F);
        float f6 = MathHelper.sin(-pitch * 0.017453292F);
        float f7 = f4 * f5;
        float f8 = f3 * f5;
        double d3 = gamemode == WorldSettings.EnumGamemode.CREATIVE ? 5.0D : 4.5D;

        Vec3D secondVector = firstVector.add((double) f7 * d3, (double) f6 * d3, (double) f8 * d3);

        int i = MathHelper.floor(secondVector.a);
        int j = MathHelper.floor(secondVector.b);
        int k = MathHelper.floor(secondVector.c);
        int l = MathHelper.floor(firstVector.a);
        int i1 = MathHelper.floor(firstVector.b);
        int j1 = MathHelper.floor(firstVector.c);

        boolean xDivide = true;
        boolean yDivide = true;
        boolean zDivide = true;
        double startX = 999.0D;
        double startY = 999.0D;
        double startZ = 999.0D;
        double xDifference = secondVector.a - firstVector.a;
        double yDifference = secondVector.b - firstVector.b;
        double zDifference = secondVector.c - firstVector.c;

        if (i > l) {
            startX = (double) l + 1.0D;
        } else if (i < l) {
            startX = (double) l + 0.0D;
        } else {
            xDivide = false;
        }

        if (j > i1) {
            startY = (double) i1 + 1.0D;
        } else if (j < i1) {
            startY = (double) i1 + 0.0D;
        } else {
            yDivide = false;
        }

        if (k > j1) {
            startZ = (double) j1 + 1.0D;
        } else if (k < j1) {
            startZ = (double) j1 + 0.0D;
        } else {
            zDivide = false;
        }

        if (xDivide) {
            startX = (startX - firstVector.a) / xDifference;
        }

        if (yDivide) {
            startY = (startY - firstVector.b) / yDifference;
        }

        if (zDivide) {
            startZ = (startZ - firstVector.c) / zDifference;
        }

        if (startX == -0.0D) {
            startX = -1.0E-4D;
        }

        if (startY == -0.0D) {
            startY = -1.0E-4D;
        }

        if (startZ == -0.0D) {
            startZ = -1.0E-4D;
        }

        EnumDirection enumDirection;

        if (startX < startY && startX < startZ) {
            enumDirection = i > l ? EnumDirection.WEST : EnumDirection.EAST;
        } else if (startY < startZ) {
            enumDirection = j > i1 ? EnumDirection.DOWN : EnumDirection.UP;
        } else {
            enumDirection = k > j1 ? EnumDirection.NORTH : EnumDirection.SOUTH;
        }

        return enumDirection;
    }

}
