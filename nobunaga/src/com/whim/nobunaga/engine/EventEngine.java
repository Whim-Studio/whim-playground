package com.whim.nobunaga.engine;

import com.whim.nobunaga.domain.GameState;
import com.whim.nobunaga.domain.Province;

import java.util.ArrayList;
import java.util.List;

/**
 * Random seasonal disasters and rebellions. Disaster probability and severity
 * are scaled by {@code 1 - floodControl/120} so well-administered provinces
 * suffer less. All randomness draws from {@link GameState#rng}.
 */
public class EventEngine {

    /** Roll disasters + rebellion for every owned province; returns log lines. */
    public List<String> process(GameState s) {
        List<String> log = new ArrayList<String>();
        for (Province p : s.provinces) {
            if (p.isNeutral()) {
                continue;
            }
            double scale = 1.0 - p.getFloodControl() / 120.0; // 0.16 (max flood) .. 1.0
            if (scale < 0) scale = 0;

            // Typhoon: ruins rice + erodes cultivation, far worse when undefended.
            if (s.rng.nextDouble() < 0.09 * scale) {
                int riceLoss = (int) Math.round(p.getRice() * (0.15 + 0.25 * scale));
                int cultLoss = (int) Math.round(2 + 6 * scale);
                p.setRice(Math.max(0, p.getRice() - riceLoss));
                p.setCultivation(Math.max(0, p.getCultivation() - cultLoss));
                log.add(p.getName() + ": TYPHOON! -" + riceLoss + " rice, -" + cultLoss + " cultivation");
            }

            // Plague: kills peasantry loyalty and thins the garrison.
            if (s.rng.nextDouble() < 0.06 * scale) {
                int loyLoss = 5 + s.rng.nextInt(11);
                int sLoss = (int) Math.round(p.getSoldiers() * (0.05 + 0.10 * scale));
                p.setLoyalty(Math.max(0, p.getLoyalty() - loyLoss));
                p.setSoldiers(Math.max(0, p.getSoldiers() - sLoss));
                log.add(p.getName() + ": PLAGUE! -" + loyLoss + " loyalty, -" + sLoss + " soldiers");
            }

            // Ninja: gold theft / sabotage (flood control does little against shinobi,
            // but we still scale lightly so the contract's scaling holds).
            if (s.rng.nextDouble() < 0.05 * (0.5 + 0.5 * scale)) {
                int goldLoss = (int) Math.round(p.getGold() * (0.10 + 0.20 * s.rng.nextDouble()));
                p.setGold(Math.max(0, p.getGold() - goldLoss));
                log.add(p.getName() + ": NINJA raid! -" + goldLoss + " gold stolen");
            }

            // Rebellion: only when loyalty has collapsed.
            if (p.getLoyalty() < 20 && s.rng.nextDouble() < 0.30) {
                int goldLoss = (int) Math.round(p.getGold() * 0.4);
                int riceLoss = (int) Math.round(p.getRice() * 0.4);
                int sLoss = (int) Math.round(p.getSoldiers() * 0.3);
                p.setGold(Math.max(0, p.getGold() - goldLoss));
                p.setRice(Math.max(0, p.getRice() - riceLoss));
                p.setSoldiers(Math.max(0, p.getSoldiers() - sLoss));
                if (p.getLoyalty() < 10 && s.rng.nextDouble() < 0.5) {
                    flipNeutral(s, p);
                    p.setLoyalty(40);
                    log.add(p.getName() + ": REVOLT! province lost to rebels (now neutral)");
                } else {
                    p.setLoyalty(Math.min(100, p.getLoyalty() + 10)); // grievances vented
                    log.add(p.getName() + ": REVOLT! -" + goldLoss + " gold, -" + riceLoss
                            + " rice, -" + sLoss + " soldiers");
                }
            }
        }
        return log;
    }

    /** Detach a province from its daimyo and make it neutral. */
    private void flipNeutral(GameState s, Province p) {
        int owner = p.getOwnerId();
        if (owner >= 0) {
            s.daimyo(owner).getProvinceIds().remove(Integer.valueOf(p.getId()));
        }
        p.setOwnerId(-1);
    }
}
