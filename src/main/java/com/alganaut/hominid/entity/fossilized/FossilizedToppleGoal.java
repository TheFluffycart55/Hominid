package com.alganaut.hominid.entity.fossilized;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

public class FossilizedToppleGoal extends Goal {
    public final Fossilized fossilized;
    public LivingEntity target;

    public FossilizedToppleGoal(Fossilized fossilized) {
        this.fossilized = fossilized;
    }

    @Override
    public boolean canUse() {
        LivingEntity livingentity = this.fossilized.getTarget();
        return livingentity != null && !fossilized.fossilizedFallen() && fossilized.fallTimer >= fossilized.fallCooldown;
    }

    public boolean requiresUpdateEveryTick() {
        return true;
    }

    public void stop() {
        this.target = null;
        this.fossilized.fossilizedFallen();
    }

}
