package com.qishui48.ascension.event;

import com.qishui48.ascension.util.IEntityDataSaver;
import com.qishui48.ascension.util.PacketUtils;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

public class ModEvents {
    public static void register() {
        // 维度首次踏足奖励
        ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register((player, origin, destination) -> {
            String dimId = destination.getRegistryKey().getValue().toString();

            IEntityDataSaver dataSaver = (IEntityDataSaver) player;
            NbtCompound nbt = dataSaver.getPersistentData();

            NbtList visitedDims;
            if (nbt.contains("visited_dimensions", NbtElement.LIST_TYPE)) {
                visitedDims = nbt.getList("visited_dimensions", NbtElement.STRING_TYPE);
            } else {
                visitedDims = new NbtList();
            }

            for (NbtElement element : visitedDims) {
                if (element.asString().equals(dimId)) return;
            }

            visitedDims.add(NbtString.of(dimId));
            nbt.put("visited_dimensions", visitedDims);

            int points = 20;
            if (dimId.equals("minecraft:the_end")) points = 50;

            int currentPoints = nbt.getInt("skill_points");
            nbt.putInt("skill_points", currentPoints + points);
            PacketUtils.syncSkillData(player);

            // [核心修改] 构建维度的翻译键
            // destination.getRegistryKey().getValue() 返回如 "minecraft:the_nether"
            // 转换为 -> "dimension.minecraft.the_nether"
            Identifier dimIdentifier = destination.getRegistryKey().getValue();
            String dimTranslationKey = "dimension." + dimIdentifier.getNamespace() + "." + dimIdentifier.getPath();

            // 发送通知
            Text msg = Text.translatable("notification.ascension.header.dimension").formatted(Formatting.LIGHT_PURPLE)
                    .append(" ")
                    .append(Text.translatable("notification.ascension.verb.arrive").formatted(Formatting.WHITE))
                    .append(" ")
                    // 自动翻译维度名称
                    .append(Text.translatable(dimTranslationKey).formatted(Formatting.LIGHT_PURPLE))
                    .append(" ")
                    .append(Text.translatable("notification.ascension.suffix.points", points).formatted(Formatting.BOLD, Formatting.GREEN));

            PacketUtils.sendNotification(player, msg);
        });
    }
}