package net.minecraft.server;

import java.util.Iterator;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.bukkit.craftbukkit.util.UnsafeList; // CraftBukkit

public class PathfinderGoalSelector {

    private static final Logger a = LogManager.getLogger();
    public List<PathfinderGoalSelector.PathfinderGoalSelectorItem> b = new UnsafeList<>(); // joeleoli
    public List<PathfinderGoalSelector.PathfinderGoalSelectorItem> c = new UnsafeList<>(); // joeleoli
    private final MethodProfiler d;
    private int e;
    private int f = 3;

    public PathfinderGoalSelector(MethodProfiler methodprofiler) {
        this.d = methodprofiler;
    }

    public void a(int i, PathfinderGoal pathfindergoal) {
        this.b.add(new PathfinderGoalSelector.PathfinderGoalSelectorItem(i, pathfindergoal));
    }

    public void a(PathfinderGoal pathfindergoal) {
        Iterator iterator = this.b.iterator();

        while (iterator.hasNext()) {
            PathfinderGoalSelector.PathfinderGoalSelectorItem selectorItem = (PathfinderGoalSelector.PathfinderGoalSelectorItem) iterator.next();
            PathfinderGoal goal = selectorItem.a;

            if (goal == pathfindergoal) {
                if (this.c.contains(selectorItem)) {
                    goal.d();
                    this.c.remove(selectorItem);
                }

                iterator.remove();
            }
        }

    }

    public void a() {
        this.d.a("goalSetup");
        Iterator iterator;
        PathfinderGoalSelector.PathfinderGoalSelectorItem selectorItem;

        if (this.e++ % this.f == 0) {
            iterator = this.b.iterator();

            while (iterator.hasNext()) {
                selectorItem = (PathfinderGoalSelector.PathfinderGoalSelectorItem) iterator.next();

                boolean flag = this.c.contains(selectorItem);
                if (flag) {
                    if (this.b(selectorItem) && this.a(selectorItem)) {
                        continue;
                    }

                    selectorItem.a.d();
                    this.c.remove(selectorItem);
                }

                if (this.b(selectorItem) && selectorItem.a.a()) {
                    selectorItem.a.c();
                    this.c.add(selectorItem);
                }
            }
        } else {
            iterator = this.c.iterator();

            while (iterator.hasNext()) {
                selectorItem = (PathfinderGoalSelector.PathfinderGoalSelectorItem) iterator.next();
                if (!this.a(selectorItem)) {
                    selectorItem.a.d();
                    iterator.remove();
                }
            }
        }

        this.d.b();
        this.d.a("goalTick");
        iterator = this.c.iterator();

        while (iterator.hasNext()) {
            selectorItem = (PathfinderGoalSelector.PathfinderGoalSelectorItem) iterator.next();
            selectorItem.a.e();
        }

        this.d.b();
    }

    private boolean a(PathfinderGoalSelector.PathfinderGoalSelectorItem selectorItem) {
        boolean flag = selectorItem.a.b();

        return flag;
    }

    // takes priority over other items?
    private boolean b(PathfinderGoalSelector.PathfinderGoalSelectorItem selectorItem) {
        Iterator iterator = this.b.iterator();

        while (iterator.hasNext()) {
            PathfinderGoalSelector.PathfinderGoalSelectorItem nextItem = (PathfinderGoalSelector.PathfinderGoalSelectorItem) iterator.next();

            if (nextItem != selectorItem) {
                if (selectorItem.b >= nextItem.b) {
                    if (!this.a(selectorItem, nextItem) && this.c.contains(nextItem)) {
                        ((UnsafeList.Itr) iterator).valid = false; // CraftBukkit - mark iterator for reuse
                        return false;
                    }
                } else if (!nextItem.a.i() && this.c.contains(nextItem)) {
                    ((UnsafeList.Itr) iterator).valid = false; // CraftBukkit - mark iterator for reuse
                    return false;
                }
            }
        }

        return true;
    }

    private boolean a(PathfinderGoalSelector.PathfinderGoalSelectorItem selectorItem1, PathfinderGoalSelector.PathfinderGoalSelectorItem selectorItem2) {
        return (selectorItem1.a.j() & selectorItem2.a.j()) == 0;
    }

    public class PathfinderGoalSelectorItem {
        public PathfinderGoal a;
        public int b;

        public PathfinderGoalSelectorItem(int i, PathfinderGoal pathfindergoal) {
            this.b = i;
            this.a = pathfindergoal;
        }
    }

}
