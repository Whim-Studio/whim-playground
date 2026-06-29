package com.whim.ruinlander.domain;

/**
 * The six survival stats tracked on the {@link Player}.
 * <ul>
 *   <li>HEALTH: downward = death (0 is dead).</li>
 *   <li>HUNGER / THIRST / FATIGUE: accumulate upward (0 = sated, 100 = critical).</li>
 *   <li>RADIATION: accumulates upward (0 good, high bad).</li>
 *   <li>TEMPERATURE: comfort stat (100 = ideal, low = hypothermia risk).</li>
 * </ul>
 */
public enum StatType {
    HEALTH, HUNGER, THIRST, FATIGUE, RADIATION, TEMPERATURE
}
