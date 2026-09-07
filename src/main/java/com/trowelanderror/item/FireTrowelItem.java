/*
 * Copyright (c) 2026 Trowelanderror
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons allowable, subject to the
 * following conditions:
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 */

package com.trowelanderror.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class FireTrowelItem extends BaseTrowelItem {

    public FireTrowelItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (level.isClientSide()) {
            return InteractionResult.PASS;
        }

        Vec3 eyePos = player.getEyePosition();
        Vec3 lookVec = player.getLookAngle();
        double reach = 128.0D;
        Vec3 reachEnd = eyePos.add(lookVec.scale(reach));

        // Block ray trace
        HitResult blockHit = level.clip(new ClipContext(
                eyePos, reachEnd,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                player
        ));

        // Entity ray trace
        AABB searchBox = player.getBoundingBox()
                .expandTowards(lookVec.scale(reach))
                .inflate(1.0D);

        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(
                level,
                player,
                eyePos,
                reachEnd,
                searchBox,
                e -> !e.isSpectator() && e.isPickable() && e != player,
                0.0F
        );

        // Pick the closer hit
        HitResult finalHit = blockHit;
        if (entityHit != null) {
            double entityDistSq = eyePos.distanceToSqr(entityHit.getLocation());
            double blockDistSq = blockHit.getType() == HitResult.Type.MISS
                    ? Double.MAX_VALUE
                    : eyePos.distanceToSqr(blockHit.getLocation());
            if (entityDistSq < blockDistSq) {
                finalHit = entityHit;
            }
        }

        if (finalHit.getType() != HitResult.Type.MISS) {
            Vec3 hitLoc = finalHit.getLocation();

            // ServerLevel#sendParticles(T, double, double, double, int, double, double, double, double)
            // — confirmed against ForgeJavaDocs-NG ServerLevel javadoc for 1.21.x.
            if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.FLAME,
                        hitLoc.x, hitLoc.y, hitLoc.z, 1, 0.0, 0.1, 0.0, 0.0);
                serverLevel.sendParticles(ParticleTypes.FLAME,
                        hitLoc.x, hitLoc.y, hitLoc.z, 8,
                        0.5, 0.5, 0.5, 0.05);
            }

            if (finalHit instanceof EntityHitResult ehr) {
                Entity target = ehr.getEntity();
                if (target instanceof LivingEntity living) {

                    boolean isUndead = living.getType().is(EntityTypeTags.UNDEAD);
                    boolean isBoss = living instanceof EnderDragon || living instanceof WitherBoss;
                    boolean fireImmune = living.fireImmune();

                    if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                        if (fireImmune || isUndead || isBoss) {
                            // Entity#hurtServer(ServerLevel, DamageSource, float) -> boolean
                            // — standard since 1.20.5; unchanged in 1.21.x.
                            living.hurtServer(serverLevel,
                                    level.damageSources().indirectMagic(player, player), 1000.0F);
                            living.invulnerableTime = 0;
                            living.setDeltaMovement(0, 0.5, 0);
                        } else {
                            living.hurtServer(serverLevel,
                                    level.damageSources().onFire(), 20.0F);
                            // LivingEntity#igniteForSeconds(float) — confirmed in 1.21.x javadoc.
                            living.igniteForSeconds(10.0F);
                            living.setDeltaMovement(lookVec.scale(1.5));
                        }
                    }

                    // Level#playSound(Player, double, double, double, SoundEvent, SoundSource, float, float)
                    // — confirmed: no BlockPos overload exists in 1.21.x.
                    // Pass null as the player so every nearby client hears it.
                    level.playSound(null,
                            player.getX(), player.getY(), player.getZ(),
                            SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.0F, 1.5F);
                }
            } else if (finalHit instanceof BlockHitResult bhr) {
                BlockPos pos = bhr.getBlockPos().relative(bhr.getDirection());
                if (level.getBlockState(pos).isAir()) {
                    level.setBlockAndUpdate(pos, Blocks.FIRE.defaultBlockState());
                }
            }
        }

        return InteractionResult.SUCCESS;
    }
}
