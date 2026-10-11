package com.titanreborn.combat;

import com.titanreborn.entity.TitanZombie;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 泰坦伤害规则（M0.5 验证项）。
 *
 * <p>入口是 {@link LivingIncomingDamageEvent}（在游戏总线上，可取消）。
 * 已核验的可用方法：{@code getAmount} / {@code setAmount} /
 * {@code addReductionModifier} / {@code setInvulnerabilityTicks}。
 *
 * <p>处理顺序（完整版见 docs/03-gameplay-systems.md §1.1）：
 * <pre>
 *   原始伤害
 *     ├─ 目标是泰坦？
 *     │    ├─ 未破防 → 归零（几乎免疫）
 *     │    ├─ 低于最小门槛 → 归零
 *     │    ├─ 超过上限 → 截断
 *     │    └─ 应用伤害冷却
 *     └─ 非泰坦 → 不处理
 * </pre>
 *
 * <p><b>M0 阶段数值为验证用，不是平衡值。</b>
 */
@EventBusSubscriber(modid = com.titanreborn.TitanReborn.MODID)
public final class TitanDamageRules {

    private static final Logger LOGGER = LoggerFactory.getLogger("titanreborn/combat");

    /** 单次伤害上限（设计值，待平衡）。 */
    private static final float MAX_SINGLE_HIT = 1000.0F;
    /** 低于此值的攻击完全无效（泰坦无视小伤害）。 */
    private static final float MIN_EFFECTIVE_HIT = 20.0F;
    /** 伤害冷却：把无敌帧拉长到 1.5 秒（30 tick）。 */
    private static final int DAMAGE_COOLDOWN_TICKS = 30;

    /**
     * 管理员 / 命令类伤害的来源标识 —— 必须**原样放行**，不能套泰坦规则。
     *
     * <p>踩过的坑（2026-10-11 实测）：{@code /kill} 走的是 {@code genericKill} +
     * {@code Float.MAX_VALUE}，被本规则归零后泰坦**杀不掉**；而且命令每次尝试都会
     * 触发一次事件，实测 1 秒刷了 2.5 万行 {@code [M0.5]} 日志，把日志文件冲到 3 万行。
     * 所以这类来源直接返回，既不改伤害也不打日志。
     */
    private static final java.util.Set<String> BYPASS_SOURCES = java.util.Set.of("genericKill", "generic_kill");

    private TitanDamageRules() {}

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof TitanZombie titan)) {
            return;
        }

        String source = event.getSource().getMsgId();
        if (BYPASS_SOURCES.contains(source)) {
            return;   // /kill 等命令伤害原样放行
        }

        float original = event.getAmount();
        float result = original;
        String reason;

        if (!isBroken(titan)) {
            // 未破防：几乎免疫
            result = 0.0F;
            reason = "未破防";
        } else if (original < MIN_EFFECTIVE_HIT) {
            result = 0.0F;
            reason = "低于最小门槛";
        } else if (original > MAX_SINGLE_HIT) {
            result = MAX_SINGLE_HIT;
            reason = "超过伤害上限";
        } else {
            reason = "正常";
        }

        event.setAmount(result);
        event.setInvulnerabilityTicks(DAMAGE_COOLDOWN_TICKS);

        LOGGER.info("[M0.5] 泰坦受击 原始={} 最终={} 判定={} 破防={} 来源={}",
                original, result, reason, isBroken(titan), source);
    }

    /**
     * 破防判定 —— **当前临时停用，恒为 false**。
     *
     * <p>原因：2026-10-12 外观基线简化为「原版僵尸等比放大 16 倍 + 同比例铁剑」，
     * 三片腐骨甲片（{@code bone_plate_0/1/2}）已从模型与部件中移除，
     * 原先「打中 3 片甲片即破防」的临时规则失去依据。
     *
     * <p>新的破防条件属于机制设计，等方案定稿后再实现（候选：血量阈值保底 /
     * 特定武器 / 击晕）。在此之前泰坦保持「未破防完全免伤」的行为。
     */
    public static boolean isBroken(TitanZombie titan) {
        return false;
    }
}
