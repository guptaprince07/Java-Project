import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads a job description text file.
 *
 * Recognised keys (case-insensitive):
 *   Title / Job Title / Role / Position
 *   Required Skills / Must Have
 *   Preferred Skills / Nice To Have
 *   Minimum Experience / Experience        (e.g. "3 years")
 *   Minimum Education / Education          (e.g. "Bachelor's")
 * Skill lists may be comma separated or written as bullet lines underneath the key.
 * If no "Required Skills" key exists, every known skill in the text is treated as required.
 */
public class JobParser {

    private enum Field { NONE, TITLE, REQ, PREF, EXP, EDU }

    private static final Pattern KEY_VALUE = Pattern.compile("^\\s*([A-Za-z ]{2,30}?)\\s*:\\s*(.*)$");
    private static final Pattern NUMBER = Pattern.compile("(\\d+(?:\\.\\d+)?)");

    private final SkillDatabase db;

    public JobParser(SkillDatabase db) {
        this.db = db;
    }

    public JobDescription parse(Path file) throws IOException {
        String text = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
        JobDescription job = new JobDescription();
        job.fileName = file.getFileName().toString();
        job.rawText = text;

        List<String> reqItems = new ArrayList<>();
        List<String> prefItems = new ArrayList<>();
        Field current = Field.NONE;
        boolean titleSet = false;

        for (String line : text.split("\\r?\\n")) {
            if (line.trim().isEmpty()) { current = Field.NONE; continue; }

            Matcher m = KEY_VALUE.matcher(line);
            if (m.matches() && fieldOf(m.group(1)) != Field.NONE) {
                current = fieldOf(m.group(1));
                String value = m.group(2).trim();
                switch (current) {
                    case TITLE: job.title = value; titleSet = true; break;
                    case REQ: if (!value.isEmpty()) reqItems.add(value); break;
                    case PREF: if (!value.isEmpty()) prefItems.add(value); break;
                    case EXP: job.minExperience = firstNumber(value); break;
                    case EDU: job.minEducationLevel = EducationUtil.detectLevel(value); break;
                    default: break;
                }
            } else if (line.trim().matches("^[-*\\u2022].*")) {
                String item = line.trim().replaceFirst("^[-*\\u2022]\\s*", "");
                if (current == Field.REQ) reqItems.add(item);
                else if (current == Field.PREF) prefItems.add(item);
            }
        }

        if (!titleSet) {
            job.title = job.fileName.replaceFirst("\\.[^.]+$", "").replace('_', ' ');
        }

        job.requiredSkills = toSkills(reqItems);
        job.preferredSkills = toSkills(prefItems);
        if (job.requiredSkills.isEmpty()) job.requiredSkills = db.extract(text);
        job.preferredSkills.removeAll(job.requiredSkills);
        return job;
    }

    private Field fieldOf(String key) {
        String k = key.trim().toLowerCase();
        switch (k) {
            case "title": case "job title": case "role": case "position": return Field.TITLE;
            case "required skills": case "skills required": case "must have": case "required": case "requirements":
                return Field.REQ;
            case "preferred skills": case "nice to have": case "good to have": case "preferred": return Field.PREF;
            case "minimum experience": case "experience": case "experience required": case "min experience":
                return Field.EXP;
            case "minimum education": case "education": case "qualification": case "minimum qualification":
                return Field.EDU;
            default: return Field.NONE;
        }
    }

    private double firstNumber(String s) {
        Matcher m = NUMBER.matcher(s);
        return m.find() ? Double.parseDouble(m.group(1)) : 0;
    }

    /** Turns "Java, SQL; Spring Boot" into canonical skills; unknown tokens are kept as custom skills. */
    private Set<String> toSkills(List<String> items) {
        Set<String> out = new LinkedHashSet<>();
        for (String item : items) {
            for (String token : item.split("[,;|]")) {
                String t = token.trim();
                if (t.isEmpty()) continue;
                Set<String> known = db.extract(t);
                if (!known.isEmpty()) out.addAll(known);
                else if (t.length() <= 40) out.add(t);
            }
        }
        return out;
    }
}
