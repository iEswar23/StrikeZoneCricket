package io.github.ieswar23.strikezone.ui.game;

import io.github.ieswar23.strikezone.engine.Delivery;
import io.github.ieswar23.strikezone.engine.FieldSetting;

/** Instruction for {@link PitchView} to animate the next ball. */
public final class DeliveryCommand {
    public final Delivery delivery;
    public final FieldSetting field;
    public final String bowlerName;
    public final long startDelayMs;
    public final long perfectWindowMs;
    public final long goodWindowMs;

    public DeliveryCommand(Delivery delivery, FieldSetting field, String bowlerName, long startDelayMs,
                           long perfectWindowMs, long goodWindowMs) {
        this.delivery = delivery;
        this.field = field;
        this.bowlerName = bowlerName;
        this.startDelayMs = startDelayMs;
        this.perfectWindowMs = perfectWindowMs;
        this.goodWindowMs = goodWindowMs;
    }
}
