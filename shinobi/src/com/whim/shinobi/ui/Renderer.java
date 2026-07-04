package com.whim.shinobi.ui;

import com.whim.shinobi.api.Config;
import com.whim.shinobi.api.Enums;
import com.whim.shinobi.api.Views;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.GradientPaint;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.util.List;

/**
 * Draws a {@link Views.GameStateView} into the viewport with Graphics2D. Everything
 * is procedural geometry (rectangles / polygons / simple limbs). UPPER-plane content
 * is drawn desaturated (via {@link Palette#depth}) so the two side-scrolling paths
 * read with depth; LOWER-plane content is drawn in front on top of it.
 *
 * Pure function of the view + camera: no mutation of anything reachable from state.
 */
public final class Renderer {

    private final Stroke thin = new BasicStroke(2f);

    public void render(Graphics2D g, Views.GameStateView state, Camera cam) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        drawSky(g);
        drawParallax(g, cam);

        // UPPER plane (background path) first, then LOWER (foreground) on top.
        drawPlatforms(g, state.platforms(), cam, Enums.Plane.UPPER);
        drawGroundLine(g, cam, Enums.Plane.UPPER);
        drawHostages(g, state.hostages(), cam, Enums.Plane.UPPER);
        drawEnemies(g, state.enemies(), cam, Enums.Plane.UPPER);

        drawPlatforms(g, state.platforms(), cam, Enums.Plane.LOWER);
        drawGroundLine(g, cam, Enums.Plane.LOWER);
        drawHostages(g, state.hostages(), cam, Enums.Plane.LOWER);
        drawEnemies(g, state.enemies(), cam, Enums.Plane.LOWER);

        // Projectiles then player last so Joe reads on top.
        drawProjectiles(g, state.projectiles(), cam);
        if (state.player().alive()) {
            drawPlayer(g, state.player(), cam);
        }

        drawNinjutsuFlash(g, state.ninjutsuFlash());
    }

    // ---------------------------------------------------------------- background

    private void drawSky(Graphics2D g) {
        GradientPaint sky = new GradientPaint(0, 0, Palette.SKY_TOP, 0, Config.VIEW_H, Palette.SKY_BOTTOM);
        g.setPaint(sky);
        g.fillRect(0, 0, Config.VIEW_W, Config.VIEW_H);
    }

    /** A couple of drifting silhouette bands parallaxed against camera X. */
    private void drawParallax(Graphics2D g, Camera cam) {
        double cx = cam.cameraX();
        g.setColor(new Color(30, 22, 48));
        int off = (int) (cx * 0.25);
        for (int i = -1; i < 6; i++) {
            int bx = i * 160 - (off % 160);
            int[] xs = { bx, bx + 60, bx + 120, bx + 160, bx };
            int[] ys = { 240, 150, 210, 240, 240 };
            g.fillPolygon(xs, ys, xs.length);
        }
        g.setColor(new Color(24, 18, 40));
        int off2 = (int) (cx * 0.12);
        for (int i = -1; i < 7; i++) {
            int bx = i * 200 - (off2 % 200);
            g.fillOval(bx, 120, 140, 90);
        }
        // Moon
        g.setColor(new Color(226, 226, 210));
        g.fillOval(Config.VIEW_W - 96, 40, 44, 44);
    }

    // ----------------------------------------------------------------- terrain

    private void drawGroundLine(Graphics2D g, Camera cam, Enums.Plane plane) {
        boolean upper = plane == Enums.Plane.UPPER;
        int feetY = upper ? Config.GROUND_Y_UPPER : Config.GROUND_Y_LOWER;
        Color top = upper ? Palette.GROUND_UPPER_TOP : Palette.GROUND_LOWER_TOP;
        Color body = upper ? Palette.GROUND_UPPER : Palette.GROUND_LOWER;
        int depth = upper ? 26 : Config.VIEW_H - feetY; // upper is a thin ledge band
        g.setColor(body);
        g.fillRect(0, feetY, Config.VIEW_W, depth);
        g.setColor(top);
        g.fillRect(0, feetY, Config.VIEW_W, 4);
        // simple brick hatching scrolled with camera
        g.setColor(new Color(0, 0, 0, upper ? 40 : 60));
        int step = 48;
        int off = (int) (cam.cameraX() % step);
        for (int x = -off; x < Config.VIEW_W; x += step) {
            g.drawLine(x, feetY + 4, x, feetY + depth);
        }
    }

    private void drawPlatforms(Graphics2D g, List<Views.PlatformView> platforms, Camera cam, Enums.Plane plane) {
        boolean upper = plane == Enums.Plane.UPPER;
        Color top = upper ? Palette.GROUND_UPPER_TOP : Palette.GROUND_LOWER_TOP;
        Color body = upper ? Palette.GROUND_UPPER : Palette.GROUND_LOWER;
        for (int i = 0; i < platforms.size(); i++) {
            Views.PlatformView p = platforms.get(i);
            if (p.plane() != plane) continue;
            int x = cam.sx(p.x());
            int y = cam.sy(p.y());
            int w = (int) Math.round(p.w());
            int h = (int) Math.round(p.h());
            if (x + w < 0 || x > Config.VIEW_W) continue;
            g.setColor(body);
            g.fillRect(x, y, w, h);
            g.setColor(top);
            g.fillRect(x, y, w, 4);
            g.setColor(new Color(0, 0, 0, 50));
            g.drawRect(x, y, w - 1, h - 1);
        }
    }

    // ---------------------------------------------------------------- hostages

    private void drawHostages(Graphics2D g, List<Views.HostageView> hostages, Camera cam, Enums.Plane plane) {
        boolean upper = plane == Enums.Plane.UPPER;
        for (int i = 0; i < hostages.size(); i++) {
            Views.HostageView h = hostages.get(i);
            if (h.plane() != plane) continue;
            int x = cam.sx(h.x());
            int y = cam.sy(h.y());
            int w = (int) Math.round(h.w());
            int hh = (int) Math.round(h.h());
            if (x + w < 0 || x > Config.VIEW_W) continue;

            Color rope = upper ? Palette.depth(Palette.HOSTAGE_ROPE) : Palette.HOSTAGE_ROPE;
            Color bodyC = h.rescued()
                    ? (upper ? Palette.depth(Palette.HOSTAGE_FREED) : Palette.HOSTAGE_FREED)
                    : (upper ? Palette.depth(Palette.HOSTAGE_BODY) : Palette.HOSTAGE_BODY);

            // stake / post the hostage is tied to
            g.setColor(rope);
            g.fillRect(x + w / 2 - 2, y - 6, 4, hh + 6);
            // body
            g.setColor(bodyC);
            g.fillRect(x + 4, y + 6, w - 8, hh - 6);
            // head
            g.setColor(upper ? Palette.depth(Palette.JOE_SKIN) : Palette.JOE_SKIN);
            g.fillOval(x + w / 2 - 6, y - 4, 12, 12);
            // ropes across body when not rescued; a little wave when freed
            g.setColor(rope);
            if (!h.rescued()) {
                g.setStroke(thin);
                g.drawLine(x + 2, y + 12, x + w - 2, y + 12);
                g.drawLine(x + 2, y + 22, x + w - 2, y + 22);
            } else {
                g.drawLine(x + w / 2, y - 2, x + w / 2 + 8, y - 10);
            }
        }
    }

    // ----------------------------------------------------------------- enemies

    private void drawEnemies(Graphics2D g, List<Views.EnemyView> enemies, Camera cam, Enums.Plane plane) {
        boolean upper = plane == Enums.Plane.UPPER;
        for (int i = 0; i < enemies.size(); i++) {
            Views.EnemyView e = enemies.get(i);
            if (e.plane() != plane) continue;
            if (!e.alive()) continue;
            int x = cam.sx(e.x());
            int y = cam.sy(e.y());
            int w = (int) Math.round(e.w());
            int h = (int) Math.round(e.h());
            if (x + w < -20 || x > Config.VIEW_W + 20) continue;

            boolean ninja = e.type() == Enums.EnemyType.NINJA;
            Color body = ninja ? Palette.NINJA_BODY : Palette.THUG_BODY;
            Color trim = ninja ? Palette.NINJA_TRIM : Palette.THUG_TRIM;
            if (upper) { body = Palette.depth(body); trim = Palette.depth(trim); }

            boolean facingRight = e.facing() == Enums.Facing.RIGHT;
            if (ninja && e.blocking()) {
                drawNinjaBlock(g, x, y, w, h, body, trim, facingRight, upper);
            } else {
                drawHumanoid(g, x, y, w, h, body, trim,
                        upper ? Palette.depth(Palette.JOE_SKIN) : Palette.JOE_SKIN,
                        facingRight, e.state());
            }
        }
    }

    private void drawNinjaBlock(Graphics2D g, int x, int y, int w, int h,
                                Color body, Color trim, boolean right, boolean upper) {
        // deflect glow
        g.setColor(upper ? Palette.depth(Palette.NINJA_BLOCK) : Palette.NINJA_BLOCK);
        int gx = right ? x + w - 4 : x - 6;
        g.fillRoundRect(gx, y + 6, 10, h - 16, 8, 8);
        // crouched blocking body
        g.setColor(body);
        g.fillRect(x + 4, y + 10, w - 8, h - 10);
        g.setColor(trim);
        g.fillRect(x + 4, y + 10, w - 8, 5);
        // head
        g.setColor(body);
        g.fillRect(x + w / 2 - 5, y + 2, 10, 10);
    }

    // ------------------------------------------------------------------ player

    private void drawPlayer(Graphics2D g, Views.PlayerView p, Camera cam) {
        int x = cam.sx(p.x());
        int y = cam.sy(p.y());
        int w = (int) Math.round(p.w());
        int h = (int) Math.round(p.h());
        boolean right = p.facing() == Enums.Facing.RIGHT;
        boolean crouch = p.state() == Enums.EntityState.IDLE && false; // crouch reflected via box h by engine

        drawHumanoid(g, x, y, w, h, Palette.JOE_SUIT, Palette.JOE_TRIM, Palette.JOE_SKIN, right, p.state());

        // attack pose accent (sword slash / throw arm)
        if (p.state() == Enums.EntityState.ATTACK) {
            g.setColor(Palette.JOE_TRIM);
            g.setStroke(new BasicStroke(3f));
            int ax = right ? x + w : x;
            int ay = y + h / 3;
            int ex = right ? x + w + 20 : x - 20;
            if (p.lastAttack() == Enums.AttackMode.MELEE) {
                // arc slash
                g.setColor(new Color(255, 255, 255, 200));
                g.drawArc(right ? x + w - 10 : x - 30, ay - 14, 40, 40, right ? -50 : 130, 90);
            } else {
                g.drawLine(ax, ay, ex, ay);
            }
        }
    }

    // --------------------------------------------------------------- humanoid

    /** Shared 2-arm/2-leg stick-ninja with a torso block, drawn by pose. */
    private void drawHumanoid(Graphics2D g, int x, int y, int w, int h,
                              Color suit, Color trim, Color skin,
                              boolean right, Enums.EntityState state) {
        int cx = x + w / 2;
        int headR = 10;
        int headY = y + 2;
        int torsoTop = headY + headR + 2;
        int torsoBot = y + h - 14;

        // torso
        g.setColor(suit);
        g.fillRect(x + 5, torsoTop, w - 10, torsoBot - torsoTop);
        // sash / trim
        g.setColor(trim);
        g.fillRect(x + 5, torsoTop + (torsoBot - torsoTop) / 2 - 2, w - 10, 5);

        // head (masked ninja: suit hood + skin eye band)
        g.setColor(suit);
        g.fillOval(cx - headR / 2 - 4, headY, headR + 8, headR + 6);
        g.setColor(skin);
        int eyeY = headY + 4;
        if (right) g.fillRect(cx + 1, eyeY, 6, 4);
        else g.fillRect(cx - 7, eyeY, 6, 4);

        // legs vary by state
        g.setColor(suit);
        int legTop = torsoBot;
        int legBot = y + h;
        Stroke old = g.getStroke();
        g.setStroke(new BasicStroke(5f));
        if (state == Enums.EntityState.JUMP) {
            // tucked
            g.drawLine(cx - 4, legTop, cx - 8, legBot - 6);
            g.drawLine(cx + 4, legTop, cx + 10, legBot - 6);
        } else if (state == Enums.EntityState.WALK) {
            g.drawLine(cx - 4, legTop, x + 2, legBot);
            g.drawLine(cx + 4, legTop, x + w - 2, legBot);
        } else {
            g.drawLine(cx - 4, legTop, cx - 5, legBot);
            g.drawLine(cx + 4, legTop, cx + 5, legBot);
        }

        // arms
        g.setColor(suit);
        int armY = torsoTop + 6;
        if (state == Enums.EntityState.ATTACK) {
            int ex = right ? x + w + 8 : x - 8;
            g.drawLine(cx, armY, ex, armY + 2);
        } else if (state == Enums.EntityState.WALK) {
            g.drawLine(cx, armY, right ? x + w - 2 : x + 2, armY + 8);
        } else {
            g.drawLine(cx, armY, cx + (right ? 6 : -6), armY + 10);
        }
        g.setStroke(old);
    }

    // -------------------------------------------------------------- projectiles

    private void drawProjectiles(Graphics2D g, List<Views.ProjectileView> projectiles, Camera cam) {
        for (int i = 0; i < projectiles.size(); i++) {
            Views.ProjectileView pr = projectiles.get(i);
            if (!pr.alive()) continue;
            int x = cam.sx(pr.x());
            int y = cam.sy(pr.y());
            int w = (int) Math.round(pr.w());
            int h = (int) Math.round(pr.h());
            if (x + w < 0 || x > Config.VIEW_W) continue;
            boolean upper = pr.plane() == Enums.Plane.UPPER;

            Color c;
            if (!pr.fromPlayer()) {
                c = Palette.ENEMY_SHOT;
            } else {
                switch (pr.weapon()) {
                    case KNIFE: c = Palette.KNIFE; break;
                    case GUN:   c = Palette.GUN_SHOT; break;
                    default:    c = Palette.SHURIKEN; break;
                }
            }
            if (upper) c = Palette.depth(c);
            g.setColor(c);

            if (pr.fromPlayer() && pr.weapon() == Enums.Weapon.SHURIKEN) {
                // 4-point star
                int cxp = x + w / 2, cyp = y + h / 2, r = Math.max(4, w / 2 + 2);
                int[] xs = { cxp, cxp + r, cxp, cxp - r };
                int[] ys = { cyp - r, cyp, cyp + r, cyp };
                g.fillPolygon(xs, ys, 4);
                g.drawLine(cxp - r, cyp, cxp + r, cyp);
            } else if (pr.fromPlayer() && pr.weapon() == Enums.Weapon.GUN) {
                g.fillRect(x, y + h / 2 - 1, Math.max(6, w + 4), 3);
            } else {
                g.fillRect(x, y, Math.max(5, w), Math.max(4, h));
            }
        }
    }

    // -------------------------------------------------------------- ninjutsu

    private void drawNinjutsuFlash(Graphics2D g, double flash) {
        if (flash < 0) return;
        int alpha = (int) (Math.min(1.0, flash) * 220);
        g.setColor(new Color(255, 255, 255, alpha));
        g.fillRect(0, 0, Config.VIEW_W, Config.VIEW_H);
        // expanding rings
        g.setColor(new Color(150, 200, 255, (int) (alpha * 0.6)));
        int rings = 3;
        for (int i = 0; i < rings; i++) {
            int r = (int) (flash * (Config.VIEW_W) * (0.4 + i * 0.3));
            g.setStroke(new BasicStroke(4f));
            g.drawOval(Config.VIEW_W / 2 - r / 2, Config.VIEW_H / 2 - r / 2, r, r);
        }
    }
}
