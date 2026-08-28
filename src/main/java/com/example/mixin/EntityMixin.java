package com.example.mixin;

import com.example.LegacyDimensionChangeFlag;
import com.example.LegacyPortalDelayFlag;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownSplashPotion;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityMixin
        implements LegacyDimensionChangeFlag,
        LegacyPortalDelayFlag {

    /*
     * =========================================================
     * Dimension change state
     * =========================================================
     */

    @Unique
    private boolean legacyPortalDupe$dimensionChanged = false;

    /*
     * =========================================================
     * Portal delay state
     * =========================================================
     */

    @Unique
    private boolean legacyPortalDupe$portalDelayed = false;

    /*
     * 这个标记只属于旧世界中的 source entity A。
     *
     * 它表示：
     *
     * “下一 tick 应当按照 1.21.1 的顺序，
     *  先进行 portal transfer，
     *  再执行 residual collision。”
     *
     * 不能复制给目标世界的 B。
     */
    @Unique
    private boolean legacyPortalDupe$legacyTransferPending = false;

    /*
     * =========================================================
     * LegacyDimensionChangeFlag
     * =========================================================
     */

    @Override
    public void legacyPortalDupe$markDimensionChange() {
        this.legacyPortalDupe$dimensionChanged = true;
    }

    @Override
    public boolean legacyPortalDupe$wasDimensionChanged() {
        return this.legacyPortalDupe$dimensionChanged;
    }

    /*
     * =========================================================
     * LegacyPortalDelayFlag
     * =========================================================
     */

    @Override
    public boolean legacyPortalDupe$shouldDelayPortal() {
        return !this.legacyPortalDupe$portalDelayed;
    }

    @Override
    public void legacyPortalDupe$markPortalDelayed() {
        this.legacyPortalDupe$portalDelayed = true;
    }

    @Override
    public void legacyPortalDupe$resetPortalDelay() {
        this.legacyPortalDupe$portalDelayed = false;
    }

    @Override
    public boolean legacyPortalDupe$wasPortalDelayed() {
        return this.legacyPortalDupe$portalDelayed;
    }

    @Override
    public boolean legacyPortalDupe$isLegacyTransferPending() {
        return this.legacyPortalDupe$legacyTransferPending;
    }

    @Override
    public void legacyPortalDupe$markLegacyTransferPending() {
        this.legacyPortalDupe$legacyTransferPending = true;
    }

    @Override
    public void legacyPortalDupe$clearLegacyTransferPending() {
        this.legacyPortalDupe$legacyTransferPending = false;
    }

    /*
     * =========================================================
     * 旧实体 A 即将因为跨维度而被移除。
     *
     * 这个方法本身就是跨维度删除路径，
     * 所以在 HEAD 标记 dimensionChanged。
     * =========================================================
     */

    @Inject(
            method = "removeAfterChangingDimensions",
            at = @At("HEAD")
    )
    private void legacyPortalDupe$beforeDimensionRemoval(
            CallbackInfo ci
    ) {
        Entity self =
                (Entity) (Object) this;

        /*
         * 我们目前只复现喷溅药水的旧行为。
         */
        if (!(self instanceof ThrownSplashPotion)) {
            return;
        }

        this.legacyPortalDupe$dimensionChanged = true;
    }

    /*
     * =========================================================
     * teleportCrossDimension 返回后：
     *
     * old A:
     *
     *     portalDelayed = true
     *     transferPending = true
     *     removed = true
     *
     * new B:
     *
     *     portalDelayed = true
     *     transferPending = false
     *     removed = false
     *
     * 这里只复制 portalDelayed。
     * =========================================================
     */

    @Inject(
            method = "teleportCrossDimension",
            at = @At("RETURN")
    )
    private void legacyPortalDupe$afterCrossDimensionTeleport(
            CallbackInfoReturnable<Entity> cir
    ) {
        Entity self =
                (Entity) (Object) this;

        if (!(self instanceof ThrownSplashPotion)) {
            return;
        }

        Entity newEntity = cir.getReturnValue();

        if (newEntity == null) {
            return;
        }

        if (newEntity instanceof ThrownSplashPotion
                && newEntity instanceof LegacyPortalDelayFlag newFlag) {

            /*
             * 防止目标世界 B 又把同一次 portal
             * 当作一次新的 legacy delay。
             */
            if (this.legacyPortalDupe$portalDelayed) {
                newFlag.legacyPortalDupe$markPortalDelayed();
            }

            /*
             * 非常重要：
             *
             * 不要：
             *
             * newFlag.legacyPortalDupe$markLegacyTransferPending();
             *
             * transferPending 只能属于 source A。
             */
        }
    }
}