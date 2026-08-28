package com.example.mixin;

import com.example.LegacyPortalDelayFlag;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownSplashPotion;
import net.minecraft.world.phys.Vec3;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class LegacyPotionPortalDelayMixin {

    @Inject(
            method = "handlePortal",
            at = @At("HEAD"),
            cancellable = true
    )
    private void legacyPortalDupe$delaySplashPotionPortal(
            CallbackInfo ci
    ) {
        Entity self =
                (Entity) (Object) this;

        /*
         * 只处理喷溅药水。
         */
        if (!(self instanceof ThrownSplashPotion)) {
            return;
        }

        /*
         * 只在服务端执行。
         */
        if (!(self.level() instanceof ServerLevel)) {
            return;
        }

        /*
         * 当前实体必须已经拥有 PortalProcessor。
         */
        if (self.portalProcess == null) {
            return;
        }

        LegacyPortalDelayFlag flag =
                (LegacyPortalDelayFlag) self;

        /*
         * 一个 portal transition 只延迟第一次。
         *
         * 下一 tick 再进入 handlePortal 时，
         * shouldDelayPortal() 为 false，
         * vanilla portal transfer 可以继续。
         */
        if (!flag.legacyPortalDupe$shouldDelayPortal()) {
            return;
        }

        /*
         * =========================================================
         * 1.21.1 discovery tick movement compensation
         *
         * 1.21.1 中，新进入 portal 后并不会在同一处
         * 立刻结束 projectile 的运动流程。
         *
         * 它仍然会完成这一 tick 后面的 projectile movement。
         *
         * 26.2 的相关顺序改变以后，如果这里只 cancel
         * handlePortal，下一 tick 的 transfer HEAD 会停留在
         * discovery 时的位置。
         *
         * 因此这里补上这一段位置推进。
         * =========================================================
         */

        Vec3 currentPosition =
                self.position();

        Vec3 currentDelta =
                self.getDeltaMovement();

        self.setPos(
                currentPosition.add(currentDelta)
        );

        /*
         * 标记：
         *
         * 这一次 portal 已经延迟过。
         */
        flag.legacyPortalDupe$markPortalDelayed();

        /*
         * 标记：
         *
         * 下一 tick 进入 legacy transfer ordering。
         */
        flag.legacyPortalDupe$markLegacyTransferPending();

        /*
         * 本 tick 不允许 vanilla handlePortal
         * 立即完成跨维度。
         *
         * PortalProcessor 保留下来。
         */
        ci.cancel();
    }
}