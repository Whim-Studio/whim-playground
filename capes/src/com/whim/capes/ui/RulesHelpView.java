package com.whim.capes.ui;

import java.awt.BorderLayout;

import javax.swing.BorderFactory;
import javax.swing.JEditorPane;
import javax.swing.JScrollPane;
import javax.swing.JPanel;

/**
 * In-app rules reference (Phase 5): a scrollable summary of the core mechanics,
 * keeping every term faithful to the 2005 Muse of Fire text so the app reads as
 * a true digitization. Rendered as lightweight HTML in a read-only editor pane.
 */
public final class RulesHelpView extends JPanel {
    public RulesHelpView() {
        setLayout(new BorderLayout());
        setBackground(Palette.PAPER);

        JEditorPane pane = new JEditorPane("text/html", HTML);
        pane.setEditable(false);
        pane.setBackground(Palette.PAPER);
        pane.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));
        pane.setCaretPosition(0);

        JScrollPane sp = new JScrollPane(pane);
        sp.getVerticalScrollBar().setUnitIncrement(16);
        add(sp, BorderLayout.CENTER);
    }

    private static final String HTML =
        "<html><body style='font-family:sans-serif; font-size:11px; color:#1A1A22;'>"
      + "<h1 style='color:#2C6BB3;'>Capes — Rules Reference</h1>"
      + "<p><i>GM-less super-hero roleplaying. Play clockwise; when it's your turn you speak.</i></p>"

      + "<h2>Scenes &amp; Pages</h2>"
      + "<ul>"
      + "<li><b>Scene</b> — declared by the player to the left of the last declarer. Each player picks one "
      + "character free; extra characters cost a <b>Story Token</b>.</li>"
      + "<li><b>Page</b> — a unit of narration. The <b>Starter</b> advances one seat clockwise each Page. "
      + "Order: Overdrawn checks &rarr; Claim/add Conflicts &rarr; free narration &rarr; Actions &rarr; Resolve.</li>"
      + "<li><b>Action</b> — one per character free; extras cost a Story Token. On an Action you either "
      + "<b>use an Ability</b> or <b>create a Conflict</b> (one, not both).</li>"
      + "</ul>"

      + "<h2>Conflicts</h2>"
      + "<ul>"
      + "<li>An index card with two sides, each starting with one d6 at value <b>1</b>. The higher <b>total</b> "
      + "<b>Controls</b> and may narrate the outcome.</li>"
      + "<li><b>Event</b>: says what <i>will</i> happen; the Resolver narrates <i>how</i>. Any player may veto "
      + "before it is established.</li>"
      + "<li><b>Goal</b>: says someone is <i>trying</i>; the Resolver narrates <i>whether</i> they succeed.</li>"
      + "<li><b>Not Yet</b>: don't narrate an outcome until you Resolve. <b>And Then</b>: hand the finish to a "
      + "Controlling-side player if you aren't on it.</li>"
      + "</ul>"

      + "<h2>Abilities &amp; Reactions</h2>"
      + "<ul>"
      + "<li>Using an Ability rolls one die on a Conflict, or raises an Inspiration by one. The Ability's "
      + "<b>score must be &ge;</b> the die/Inspiration value.</li>"
      + "<li><b>Super</b> Abilities (Powers) earn a <b>Debt Token</b> each use, once per Page. <b>Mundane</b> "
      + "Abilities (Skills/Attitudes) <b>Block</b> — usable once per Scene, no cost.</li>"
      + "<li>After rolling a Conflict die you choose to keep it or <b>turn it back</b>. Any accepted roll opens "
      + "a <b>Reaction</b> window: any player may re-roll that die with an Ability &ge; its value, once per Action.</li>"
      + "</ul>"

      + "<h2>Debt, Drives, Staking, Splitting</h2>"
      + "<ul>"
      + "<li><b>Drives</b> (five, Strengths totaling 9) hold Debt. A Drive is <b>Overdrawn</b> when Debt &gt; "
      + "Strength; at each Page start you roll your highest die and accept only a lower result.</li>"
      + "<li><b>Stake</b>: move Debt from one Drive onto a Claimed side — one Drive per Conflict, never more than "
      + "the Drive's Strength.</li>"
      + "<li><b>Split</b>: divide a die into pieces summing to the same value, as evenly as possible; a side may "
      + "hold at most as many dice as Staked Debt points.</li>"
      + "</ul>"

      + "<h2>Resolving</h2>"
      + "<ul>"
      + "<li>The Claimant whose side Controls Resolves: <b>losers</b> take back <b>double</b> their Staked Debt; "
      + "<b>winners</b> give their Staked Debt away as Story Tokens to losing characters.</li>"
      + "<li><b>Inspirations</b>: pair winning vs losing dice highest-to-highest; each pair yields the "
      + "difference, each excess winning die its full value. Spend one to raise a die to its value.</li>"
      + "<li><b>Deadlock</b>: an unbreakable tie — all Staked Debt is lost and all dice become Inspirations.</li>"
      + "</ul>"

      + "<h2>Gloating &amp; the Comics Code</h2>"
      + "<p>If Resolving would violate the group's absolute <b>Comics Code</b>, the Claimant <b>Gloats</b> instead: "
      + "starting from the highest die, turn dice down to 1; the Resolver gains a Story Token per die turned. The "
      + "Conflict stays on the table.</p>"

      + "<h2>Resources at a glance</h2>"
      + "<ul>"
      + "<li><b>Debt</b> — gained by using Powers or losing Stakes; spent to Stake.</li>"
      + "<li><b>Story Tokens</b> — gained by losing Conflicts; spent for extra roles, Claims, Conflicts, Actions.</li>"
      + "<li><b>Inspirations</b> — gained by winning Conflicts; spent to raise a die.</li>"
      + "</ul>"
      + "</body></html>";
}
