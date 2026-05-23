package zmaster587.advancedRocketry.test.server;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * TASK-11 — shared command-invocation + result-readback helpers for the
 * {@code /ar} (WorldCommand) test suites. Keeps each test class small
 * by absorbing the duplicated <em>"run a command then read state back"</em>
 * boilerplate.
 *
 * <p>Result-readback strategy: prefer {@code /artest planet info <dim>}
 * (independent reader, JSON output) over re-reading via {@code /ar planet get}
 * (shares its codepath with {@code /ar planet set} — same impl reading
 * the same field, so they'd agree-but-be-wrong on a shared bug).
 * {@code /ar planet get} is checked for its own contract once, then we
 * trust the independent JSON readback everywhere else.</p>
 *
 * <p>Package-private — only the {@code /ar} test classes need it.</p>
 */
final class WorldCommandFixtures {

    private static final Pattern INT_FIELD =
            Pattern.compile("\"%s\":(-?\\d+)");
    private static final Pattern FLOAT_FIELD =
            Pattern.compile("\"%s\":(-?\\d+(?:\\.\\d+)?(?:[eE][+-]?\\d+)?)");

    private WorldCommandFixtures() {}

    /** Send a command via the shared {@link AbstractSharedServerTest}
     *  harness and return the concatenated console response. */
    static String exec(String cmd) throws Exception {
        return String.join("\n", AbstractSharedServerTest.client().execute(cmd));
    }

    /** Read an integer field out of {@code /artest planet info <dim>}
     *  JSON. Asserts the field is present (matcher must find). */
    static int planetIntField(int dim, String field) throws Exception {
        return Integer.parseInt(matchOrThrow(planetInfo(dim), field, INT_FIELD));
    }

    /** Read a float/double field out of {@code /artest planet info <dim>}. */
    static double planetFloatField(int dim, String field) throws Exception {
        return Double.parseDouble(matchOrThrow(planetInfo(dim), field, FLOAT_FIELD));
    }

    /** True iff AR's planet registry knows the given dim, observed via
     *  {@code /ar planet list} (which iterates {@code getRegisteredDimensions()}
     *  → the underlying {@code dimensionList} keyset). Cannot use
     *  {@code /artest planet info} here because
     *  {@code DimensionManager.getDimensionProperties} falls back to
     *  {@code overworldProperties} for unknown dims (line 539), so the
     *  info probe is incapable of distinguishing "registered" from
     *  "absent" by itself. */
    static boolean planetExists(int dim) throws Exception {
        String list = exec("ar planet list");
        return list.contains("DIM" + dim + ":");
    }

    private static String planetInfo(int dim) throws Exception {
        return exec("artest planet info " + dim);
    }

    private static String matchOrThrow(String src, String field, Pattern template) {
        Pattern p = Pattern.compile(String.format(template.pattern(),
                Pattern.quote(field)));
        Matcher m = p.matcher(src);
        if (!m.find()) {
            throw new AssertionError("field \"" + field + "\" not found in: " + src);
        }
        return m.group(1);
    }

    /** First line that contains the substring, or {@code null}. Useful
     *  for chat-output assertions that don't pin exact wording. */
    static String firstLineContaining(List<String> lines, String needle) {
        for (String l : lines) {
            if (l.contains(needle)) return l;
        }
        return null;
    }
}
