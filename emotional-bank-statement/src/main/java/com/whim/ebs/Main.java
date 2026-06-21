package com.whim.ebs;

import com.whim.ebs.domain.SessionState;
import com.whim.ebs.logic.DefaultValidationService;
import com.whim.ebs.logic.TextExportService;
import com.whim.ebs.spi.ExportService;
import com.whim.ebs.spi.ValidationService;
import com.whim.ebs.ui.MainFrame;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Application entry point for "The Emotional Bank Statement".
 *
 * <p>Wires the concrete logic implementations into the Swing UI and starts the
 * app on the Event Dispatch Thread. This is the only place where the UI is
 * coupled to concrete {@link ValidationService}/{@link ExportService} impls;
 * everything else depends on the interfaces.
 */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                } catch (Exception ignored) {
                    // Fall back to the default look and feel.
                }

                SessionState state = new SessionState();
                ValidationService validator = new DefaultValidationService();
                ExportService exporter = new TextExportService();

                MainFrame frame = new MainFrame(state, validator, exporter);
                frame.setLocationRelativeTo(null);
                frame.setVisible(true);
            }
        });
    }
}
