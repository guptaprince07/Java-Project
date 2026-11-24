import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Reads a plain-text resume and extracts skills, education and experience. */
public class ResumeParser {

    private enum Section { HEADER, SKILLS, EDUCATION, EXPERIENCE, OTHER }

    private static final Pattern HEADING = Pattern.compile(
            "^\\s*(technical skills|key skills|skills|education|academic background|academics|qualifications?|"
                    + "work experience|professional experience|experience|employment history|"
                    + "projects?|summary|objective|profile|certifications?|achievements)\\s*(?::\\s*(.*))?$",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern EMAIL = Pattern.compile("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}");
    private static final Pattern PHONE = Pattern.compile("(\\+?\\d[\\d\\s\\-()]{8,14}\\d)");
    private static final Pattern YEARS = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*\\+?\\s*(?:years?|yrs?)", Pattern.CASE_INSENSITIVE);
    private static final String MONTHS = "jan|feb|mar|apr|may|jun|jul|aug|sep|sept|oct|nov|dec";
    private static final Pattern RANGE = Pattern.compile(
            "(?:(" + MONTHS + ")[a-z]*\\.?\\s+)?((?:19|20)\\d{2})\\s*(?:-|\\u2013|\\u2014|to)\\s*"
                    + "(?:(?:(" + MONTHS + ")[a-z]*\\.?\\s+)?((?:19|20)\\d{2})|(present|current|now|till date))",
            Pattern.CASE_INSENSITIVE);

    private final SkillDatabase db;

    public ResumeParser(SkillDatabase db) {
        this.db = db;
    }

    public Resume parse(Path file) throws IOException {
        String text = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
        Resume r = new Resume();
        r.fileName = file.getFileName().toString();
        r.rawText = text;

        List<String> lines = Arrays.asList(text.split("\\r?\\n"));
        List<String> educationLines = new ArrayList<>();
        List<String> experienceLines = new ArrayList<>();

        Section current = Section.HEADER;
        for (String line : lines) {
            Matcher h = HEADING.matcher(line.trim());
            String content = line;
            if (h.matches()) {
                current = sectionOf(h.group(1));
                content = h.group(2) == null ? "" : h.group(2);
            }
            if (current == Section.EDUCATION) educationLines.add(content);
            if (current == Section.EXPERIENCE) experienceLines.add(content);
        }

        r.name = findName(lines, r.fileName);
        Matcher em = EMAIL.matcher(text);
        if (em.find()) r.email = em.group();
        Matcher pm = PHONE.matcher(text);
        if (pm.find()) r.phone = pm.group().trim();

        r.skills = db.extract(text);

        // Education: only lines in the education section, or (if none) lines naming an institution
        List<String> eduSource = educationLines;
        if (eduSource.stream().allMatch(s -> s.trim().isEmpty())) {
            eduSource = new ArrayList<>();
            for (String l : lines) {
                if (l.toLowerCase().matches(".*\\b(university|college|institute)\\b.*")) eduSource.add(l);
            }
        }
        for (String l : eduSource) {
            int level = EducationUtil.detectLevel(l);
            if (level > EducationUtil.NONE) {
                r.education.add(l.trim().replaceAll("^[-*\\u2022]\\s*", ""));
                r.educationLevel = Math.max(r.educationLevel, level);
            }
        }

        r.experienceYears = Math.max(explicitYears(lines), yearsFromRanges(experienceLines));
        return r;
    }

    private Section sectionOf(String heading) {
        String h = heading.toLowerCase();
        if (h.contains("skill")) return Section.SKILLS;
        if (h.contains("educat") || h.contains("academ") || h.contains("qualif")) return Section.EDUCATION;
        if (h.contains("experience") || h.contains("employment")) return Section.EXPERIENCE;
        return Section.OTHER;
    }

    private String findName(List<String> lines, String fileName) {
        for (String l : lines) {
            Matcher m = Pattern.compile("^\\s*name\\s*:\\s*(.+)$", Pattern.CASE_INSENSITIVE).matcher(l);
            if (m.matches()) return m.group(1).trim();
        }
        for (String l : lines) {
            String t = l.trim();
            if (t.isEmpty()) continue;
            if (t.length() <= 40 && !t.contains("@") && !t.matches(".*\\d.*") && !HEADING.matcher(t).matches()) return t;
            break;
        }
        return fileName.replaceFirst("\\.[^.]+$", "").replace('_', ' ');
    }

    /** "3 years of experience" style statements: takes the largest on lines mentioning experience. */
    private double explicitYears(List<String> lines) {
        double best = 0;
        for (String l : lines) {
            if (!l.toLowerCase().contains("experience")) continue;
            Matcher m = YEARS.matcher(l);
            while (m.find()) best = Math.max(best, Double.parseDouble(m.group(1)));
        }
        return best;
    }

    /** Sums date ranges like "Jun 2019 - Mar 2022" or "2020 - Present" in the experience section. */
    private double yearsFromRanges(List<String> experienceLines) {
        double months = 0;
        LocalDate now = LocalDate.now();
        for (String l : experienceLines) {
            Matcher m = RANGE.matcher(l);
            while (m.find()) {
                int sy = Integer.parseInt(m.group(2));
                int sm = m.group(1) == null ? 1 : monthIndex(m.group(1));
                int ey, em;
                if (m.group(5) != null) { ey = now.getYear(); em = now.getMonthValue(); }
                else { ey = Integer.parseInt(m.group(4)); em = m.group(3) == null ? 1 : monthIndex(m.group(3)); }
                int diff = (ey - sy) * 12 + (em - sm);
                if (diff > 0) months += diff;
            }
        }
        return Math.round(months / 12.0 * 10) / 10.0;
    }

    private int monthIndex(String m) {
        String s = m.toLowerCase().substring(0, 3);
        return Arrays.asList("jan", "feb", "mar", "apr", "may", "jun", "jul", "aug", "sep", "oct", "nov", "dec")
                .indexOf(s) + 1;
    }
}
