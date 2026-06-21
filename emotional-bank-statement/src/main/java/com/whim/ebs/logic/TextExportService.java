package com.whim.ebs.logic;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

import com.whim.ebs.domain.Belief;
import com.whim.ebs.domain.SessionState;
import com.whim.ebs.spi.ExportService;

/**
 * Renders a {@link SessionState} as a plain-text Emotional Bank Statement and
 * writes it to disk as UTF-8.
 */
public class TextExportService implements ExportService {

    private static final Charset UTF_8 = Charset.forName("UTF-8");
    private static final String NL = System.lineSeparator();
    private static final String NONE_SELECTED = "(none selected)";

    @Override
    public String render(SessionState state) {
        StringBuilder sb = new StringBuilder();

        sb.append("=====================================").append(NL);
        sb.append("      THE EMOTIONAL BANK STATEMENT").append(NL);
        sb.append("=====================================").append(NL);
        sb.append(NL);

        sb.append("Core Belief: ").append(beliefName(state)).append(NL);
        sb.append(NL);

        sb.append("Proof I have lived this belief:").append(NL);
        String[] proofs = (state == null) ? null : state.getProofs();
        for (int i = 0; i < 3; i++) {
            String proof = (proofs != null && i < proofs.length) ? proofs[i] : null;
            sb.append("  ").append(i + 1).append(". ").append(orBlank(proof)).append(NL);
        }
        sb.append(NL);

        sb.append("One action I will take today to build further proof:").append(NL);
        String action = (state == null) ? null : state.getDailyAction();
        sb.append("  ").append(orBlank(action)).append(NL);

        return sb.toString();
    }

    @Override
    public void exportToFile(SessionState state, File target) throws IOException {
        if (target == null) {
            throw new IOException("Export target file must not be null.");
        }
        File parent = target.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        byte[] bytes = render(state).getBytes(UTF_8);
        Files.write(target.toPath(), bytes,
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE,
                StandardOpenOption.TRUNCATE_EXISTING);
    }

    private static String beliefName(SessionState state) {
        if (state == null) {
            return NONE_SELECTED;
        }
        Belief belief = state.getSelectedBelief();
        if (belief == null) {
            return NONE_SELECTED;
        }
        String name = belief.getName();
        return (name == null || name.trim().isEmpty()) ? NONE_SELECTED : name;
    }

    private static String orBlank(String value) {
        return (value == null) ? "" : value;
    }
}
