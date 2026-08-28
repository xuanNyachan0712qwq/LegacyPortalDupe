package com.example.mixin;

import com.example.LegacyDimensionChangeFlag;
import com.example.LegacyPortalDelayFlag;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownSplashPotion;
import net.minecraft.world.phys.HitResult;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ThrowableProjectile.class)
public abstract class LegacyThrowableProjectileMixin {

    @Inject(
            method = "tick",
            at = @At("HEAD"),
            cancellable = true
    )
    private void legacyPortalDupe$legacyTransferTick(
            CallbackInfo ci
    ) {
        ThrowableProjectile self =
                (ThrowableProjectile) (Object) this;

        /*
         * 目前只恢复喷溅药水行为。
         */
        if (!(self instanceof ThrownSplashPotion)) {
            return;
        }

        /*
         * 客户端不参与这套逻辑。
         */
        if (!(self.level() instanceof ServerLevel)) {
            return;
        }

        LegacyPortalDelayFlag portalFlag =
                (LegacyPortalDelayFlag) self;

        /*
         * 普通 tick：
         *
         * 完全交回 Minecraft 26.2。
         */
        if (!portalFlag
                .legacyPortalDupe$isLegacyTransferPending()) {

            return;
        }

        /*
         * PortalProcessor 已经消失，
         * 说明这次待处理状态失效。
         */
        if (self.portalProcess == null) {

            portalFlag
                    .legacyPortalDupe$clearLegacyTransferPending();

            return;
        }

        /*
         * =========================================================
         * 关键：
         *
         * 这一 tick 不允许执行现代
         * ThrowableProjectile.tick()。
         *
         * 26.2 原版顺序大致是：
         *
         * gravity
         * → inertia
         * → collision
         * → movement
         * → block effects
         * → super.tick / portal
         *
         * 但 1.21.1 的关键行为要求：
         *
         * Entity/base tick
         * → portal transfer
         * → old entity removed
         * → Java 方法继续
         * → projectile collision
         *
         * 因此取消现代 ThrowableProjectile.tick()，
         * 自己执行 legacy transfer ordering。
         * =========================================================
         */

        ci.cancel();

        /*
         * ---------------------------------------------------------
         * 只执行 Entity 的 base tick。
         *
         * 这会进入：
         *
         * baseTick
         * → handlePortal
         * → teleportCrossDimension
         *
         * 但不会先执行 26.2 projectile gravity /
         * inertia / collision / movement。
         *
         * 不要调用 self.tick()。
         *
         * 也不要重新引入之前那个 EntityTickInvoker，
         * 否则虚方法分派会重新回到
         * ThrowableProjectile.tick()，造成递归。
         * ---------------------------------------------------------
         */
        self.baseTick();

        /*
         * 这一 pending 已经消费。
         */
        portalFlag
                .legacyPortalDupe$clearLegacyTransferPending();

        LegacyDimensionChangeFlag dimensionFlag =
                (LegacyDimensionChangeFlag) self;

        /*
         * 必须同时满足：
         *
         * 1. 确实走过跨维度路径
         * 2. 当前 old A 已被 removed
         *
         * 才允许执行 residual。
         */
        if (!dimensionFlag
                .legacyPortalDupe$wasDimensionChanged()) {

            return;
        }

        if (!self.isRemoved()) {
            return;
        }

        /*
         * =========================================================
         * 1.21.1 residual collision
         *
         * 此时 self 仍然是：
         *
         * old Java object A
         *
         * level = source world
         * removed = true
         *
         * position = transfer tick position
         * delta = transfer tick velocity
         *
         * 但旧版 Java tick 仍会继续往下执行。
         * =========================================================
         */

        ProjectileInvoker projectileInvoker =
                (ProjectileInvoker) self;

        HitResult hitResult =
                ProjectileUtil.getHitResultOnMoveVector(
                        self,
                        projectileInvoker::
                                legacyPortalDupe$canHitEntity
                );

        /*
         * MISS：
         *
         * 没撞到任何东西，结束即可。
         */
        if (hitResult.getType()
                == HitResult.Type.MISS) {

            return;
        }

        /*
         * =========================================================
         * 最关键的一点：
         *
         * 这里故意不检查：
         *
         *     self.isAlive()
         *
         * 因为 old A 此时必然已经：
         *
         *     removed = true
         *
         * 1.21.1 的行为恰恰来自于：
         *
         * “实体虽然跨维度删除了，
         *  但当前 Java tick 没有立刻 return。”
         *
         * 所以仍然进入原版：
         *
         * hitTargetOrDeflectSelf
         * → onHit
         * → splash potion effect
         * =========================================================
         */

        projectileInvoker
                .legacyPortalDupe$hitTargetOrDeflectSelf(
                        hitResult
                );
    }
}