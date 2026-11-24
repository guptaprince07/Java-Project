import java.util.regex.Pattern;

/** Detects the education level mentioned in a line of text. */
public class EducationUtil {

    public static final int NONE = 0, DIPLOMA = 1, BACHELOR = 2, MASTER = 3, PHD = 4;

    private static final Pattern PHD_P = Pattern.compile(
            "\\b(ph\\.?\\s?d|doctorate|doctor of philosophy)\\b", Pattern.CASE_INSENSITIVE);

    private static final Pattern MASTER_CI = Pattern.compile(
            "\\b(master'?s?|post[- ]?graduate|m\\.?tech|m\\.?sc|mca|mba|m\\.?com)\\b|\\bm\\.tech\\b|\\bm\\.sc\\b",
            Pattern.CASE_INSENSITIVE);
    // ambiguous short forms: uppercase only
    private static final Pattern MASTER_CS = Pattern.compile("\\b(M\\.?E|M\\.?S|M\\.?A)\\b\\.?");

    private static final Pattern BACHELOR_CI = Pattern.compile(
            "\\b(bachelor'?s?|under[- ]?graduate|b\\.?tech|b\\.?sc|bca|bba|b\\.?com)\\b|\\bb\\.tech\\b|\\bb\\.sc\\b",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern BACHELOR_CS = Pattern.compile("\\b(B\\.?E|B\\.?S|B\\.?A)\\b\\.?");

    private static final Pattern DIPLOMA_P = Pattern.compile("\\bdiploma\\b", Pattern.CASE_INSENSITIVE);

    public static int detectLevel(String text) {
        if (text == null) return NONE;
        if (PHD_P.matcher(text).find()) return PHD;
        if (MASTER_CI.matcher(text).find() || MASTER_CS.matcher(text).find()) return MASTER;
        if (BACHELOR_CI.matcher(text).find() || BACHELOR_CS.matcher(text).find()) return BACHELOR;
        if (DIPLOMA_P.matcher(text).find()) return DIPLOMA;
        return NONE;
    }

    public static String levelName(int level) {
        switch (level) {
            case PHD: return "PhD / Doctorate";
            case MASTER: return "Master's";
            case BACHELOR: return "Bachelor's";
            case DIPLOMA: return "Diploma";
            default: return "Not specified";
        }
    }
}
