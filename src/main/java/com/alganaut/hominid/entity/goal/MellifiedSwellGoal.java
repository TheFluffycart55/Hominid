package com.alganaut.hominid.entity.goal;

import com.alganaut.hominid.entity.mellified.Mellified;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

public class MellifiedSwellGoal extends Goal {
    public final Mellified mellified;
    public LivingEntity target;

    //I'm sure this could just be done in the Mellified Class/Through a Mixin into SwellGoal, but for now this works
    public MellifiedSwellGoal(Mellified mellified) {
        this.mellified = mellified;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    public boolean canUse() {
        LivingEntity livingentity = this.mellified.getTarget();
        return this.mellified.getSwellDir() > 0 || livingentity != null && this.mellified.distanceToSqr(livingentity) < (double)9.0F;
    }

    public void start() {
        this.mellified.getNavigation().stop();
        this.target = this.mellified.getTarget();
    }

    public void stop() {
        this.target = null;
    }

    public boolean requiresUpdateEveryTick() {
        return true;
    }

    public void tick() {
        if (this.target == null) {
            stop();
        } else if (this.mellified.distanceToSqr(this.target) > (double)49.0F) {
            this.mellified.setSwellDir(-1);
        } else if (!this.mellified.getSensing().hasLineOfSight(this.target)) {
            this.mellified.setSwellDir(-1);
        } else {
            this.mellified.setSwellDir(1);
        }

    }
}
