package com.titanreborn.combat;

import com.titanreborn.entity.TitanZombie;
import com.titanreborn.entity.part.TitanPart;
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
    /** 破防所需的甲片数量（M0 验证用：3 片全打中即破防）。 */
    private static final int PLATES_TO_BREAK = 3;

    private TitanDamageRules() {}

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof TitanZombie titan)) {
            return;
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
                original, result, reason, isBroken(titan), event.getSource().getMsgId());
    }

    /**
     * 破防判定：M0 的临时规则——3 片腐骨甲片都被击中过即破防。
     *
     * <p>M1 会改成完整的破防状态机（含破防窗口计时与保底阶段破防），
     * 见 docs/03-gameplay-systems.md §2。
     */
    public static boolean isBroken(TitanZombie titan) {
        TitanPart[] plates = titan.getBonePlates();
        int hit = 0;
        for (TitanPart plate : plates) {
            if (plate.getHitCount() > 0) {
                hit++;
            }
        }
        return hit >= PLATES_TO_BREAK;
    }
}
