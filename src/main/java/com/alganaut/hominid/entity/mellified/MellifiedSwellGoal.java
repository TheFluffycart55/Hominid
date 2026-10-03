package com.alganaut.hominid.entity.mellified;

import com.alganaut.hominid.registry.sound.HominidSounds;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

public class MellifiedSwellGoal extends Goal {
    public final Mellified mellified;
    public LivingEntity target;

    public MellifiedSwellGoal(Mellified mellified) {
        this.mellified = mellified;
    }

    public boolean canUse() {
        LivingEntity livingentity = this.mellified.getTarget();
        return this.mellified.getSwellDir() > 0 || livingentity != null && this.mellified.distanceToSqr(livingentity) < (double)9.0F && !mellified.hasMellifiedExploded();
    }

    public void start() {
        this.mellified.playSound(HominidSounds.MELLIFIED_BURST.get(), 2.0F, 1F);
        this.target = this.mellified.getTarget();
    }

    public void stop() {
        this.target = null;
        this.mellified.hasMellifiedExploded();
    }

    public boolean requiresUpdateEveryTick() {
        return true;
    }

    public void tick() {
        if (this.target == null) {
            this.mellified.setSwellDir(-1);
        } else if (this.mellified.distanceToSqr(this.target) > (double)49.0F) {
            this.mellified.setSwellDir(-1);
        } else if (!this.mellified.getSensing().hasLineOfSight(this.target)) {
            this.mellified.setSwellDir(-1);
        } else {
            this.mellified.setSwellDir(1);
        }

    }
}
