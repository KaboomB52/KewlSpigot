package com.minexd.spigot.knockback;

public interface KnockbackProfile {

    String getName();

    String[] getValues();

    double getFriction();

    void setFriction(double friction);

    double getHorizontal();

    void setHorizontal(double horizontal);

    double getVertical();

    void setVertical(double vertical);

    double getVerticalLimit();

    void setVerticalLimit(double verticalLimit);

    double getExtraHorizontal();

    void setExtraHorizontal(double extraHorizontal);

    double getExtraVertical();

    void setExtraVertical(double extraVertical);

    void save();

}
