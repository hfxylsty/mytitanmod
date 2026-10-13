package com.titanreborn.entity;

/**
 * 僵尸泰坦的动作状态。
 *
 * <p><b>为什么用枚举而不是原模组那套"整数计时器"</b>：
 * 原模组用 {@code attackTimer > 0} 之类的标记驱动动画，没有状态之间的显式转移，
 * 因此做不出"蓄力→挥砍→收势"，也无法表达"被打断"（见 docs/33-titan-zombie-actions.md）。
 * 我们从一开始就用显式状态 + 明确的转移条件，后续加动作只需往枚举里加项。
 *
 * <p><b>与动画名的对应</b>：{@link #animationName()} 必须与
 * {@code animations/entity/titan_zombie.animation.json} 里的键逐字一致。
 *
 * <p><b>转移规则</b>见 {@link TitanZombie#registerControllers}：
 * <pre>
 *   NORMAL（自动）  ──移动?──▶ WALK
 *                   ──静止──▶ IDLE
 *   手动锁定（调试）──▶ 保持在该动作，直到释放
 * </pre>
 * 也就是说 {@link #NORMAL} 表示"交回自动逻辑"，不是一个独立的动画。
 */
public enum TitanAction {

    /** 交回自动逻辑：移动则 walk、静止则 idle。不是一个动画，是"无锁定"标记。 */
    NORMAL(null),

    /** 待机（循环）。 */
    IDLE("animation.titan_zombie.idle"),

    /** 行走（循环）。 */
    WALK("animation.titan_zombie.walk"),

    /**
     * 强制向前走（调试用）。
     *
     * <p>与 {@link #WALK} 的区别：{@link #WALK} **只播走路的动画**，实体坐标不变，
     * 看起来就是"原地迈腿"；本状态**同时驱动实体真的向前移动**，
     * 用来验证"实体移动 + 走路动画"这条完整链路。
     *
     * <p>移动由 {@link TitanZombie#tick()} 直接施加位移实现，**不走原版寻路**——
     * 巨型实体寻路容易失败，调试时不该被地形挡住。
     */
    TITAN_WALK("animation.titan_zombie.walk"),

    /** 攻击（播放一次）。 */
    ATTACK("animation.titan_zombie.attack");

    private final String animationName;

    TitanAction(String animationName) {
        this.animationName = animationName;
    }

    /** 对应的 GeckoLib 动画名；{@link #NORMAL} 返回 null。 */
    public String animationName() {
        return animationName;
    }

    /** 是否是"手动锁定的具体动作"（即需要一个确定的动画）。 */
    public boolean isLockedAction() {
        return animationName != null;
    }

    /** 供聊天/日志显示的中文名。 */
    public String displayName() {
        return switch (this) {
            case NORMAL -> "正常（自动）";
            case IDLE -> "待机 idle";
            case WALK -> "行走 walk（原地）";
            case TITAN_WALK -> "向前走 walk（真移动）";
            case ATTACK -> "攻击 attack";
        };
    }

    /** 调试用的循环顺序：正常 → 待机 → 行走 → 向前走 → 攻击 → 正常。 */
    public TitanAction next() {
        return switch (this) {
            case NORMAL -> IDLE;
            case IDLE -> WALK;
            case WALK -> TITAN_WALK;
            case TITAN_WALK -> ATTACK;
            case ATTACK -> NORMAL;
        };
    }

    /** 是否驱动实体真正移动（目前只有 {@link #TITAN_WALK}）。 */
    public boolean drivesMovement() {
        return this == TITAN_WALK;
    }
}
