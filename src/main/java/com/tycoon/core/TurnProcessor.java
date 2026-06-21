package com.tycoon.core;
// LOCAL-ONLY VERIFICATION STUB (Task 1 owns this) — never committed by Task 2.
import java.util.List;
public interface TurnProcessor {
    List<Interrupt> processHour(GameState state);
}
