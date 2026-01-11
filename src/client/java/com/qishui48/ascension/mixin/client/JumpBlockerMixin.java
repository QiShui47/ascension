package com.qishui48.ascension.mixin.client;

import com.qishui48.ascension.util.IEntityDataSaver;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntity.class)
public class JumpBlockerMixin {

    @Inject(method = "jump", at = @At("HEAD"), cancellable = true)
    private void onJump(CallbackInfo ci) {
        // 1. 我们只关心客户端玩家
        if ((Object) this instanceof ClientPlayerEntity player) {

            // 2. 判定蓄力条件
            // 只要解锁了蓄力跳，且正在潜行，就禁止原版起跳逻辑
            // 这样 MovementSkillMixin 里的蓄力逻辑才能接管控制权
            boolean canCharge = player.isOnGround() && player.isSneaking() && isSkillActive(player, "charged_jump");

            if (canCharge) {
                ci.cancel(); // "取消" 原版跳跃，把舞台留给蓄力逻辑
            }
        }
    }

    // === [修复] 更新为检查 skill_levels ===
    @Unique
    private boolean isSkillActive(PlayerEntity player, String id) {
        if (!(player instanceof IEntityDataSaver)) return false;
        IEntityDataSaver data = (IEntityDataSaver) player;
        NbtCompound nbt = data.getPersistentData();

        // 1. 检查是否解锁 (等级 > 0)
        boolean unlocked = false;
        if (nbt.contains("skill_levels")) {
            unlocked = nbt.getCompound("skill_levels").getInt(id) > 0;
        }

        if (!unlocked) return false;

        // 2. [建议] 既然我们有了禁用功能，这里最好也检查一下是否被禁用
        // 如果玩家中键关掉了技能，那就不应该阻止他正常跳跃
        if (nbt.contains("disabled_skills") && nbt.getCompound("disabled_skills").getBoolean(id)) {
            return false;
        }

        return true;
    }
}