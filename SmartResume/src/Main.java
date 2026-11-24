import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/** Console front-end for the Smart Resume Analyzer & Job Matcher. */
public class Main {

    private static final String LINE = "=".repeat(70);
    private static final String THIN = "-".repeat(70);

    private final SkillDatabase db = new SkillDatabase();
    private final ResumeParser resumeParser = new ResumeParser(db);
    private final JobParser jobParser = new JobParser(db);
    private final Matcher matcher = new Matcher(db);
    private final Scanner in = new Scanner(System.in);

    private Path resumeDir;
    private Path jobDir;

    public static void main(String[] args) {
        Path base = Paths.get(args.length > 0 ? args[0] : ".");
        new Main(base.resolve("resumes"), base.resolve("jobs")).run();
    }

    private Main(Path resumeDir, Path jobDir) {
        this.resumeDir = resumeDir;
        this.jobDir = jobDir;
    }

    private void run() {
        System.out.println(LINE);
        System.out.println("   SMART RESUME ANALYZER & JOB MATCHER");
        System.out.println("   Skill database: " + db.size() + " skills");
        System.out.println(LINE);

        while (true) {
            System.out.println();
            System.out.println("  1. Analyze a resume (skills, education, experience)");
            System.out.println("  2. Match a resume to a job (score)");
            System.out.println("  3. Recommend missing skills for a resume + job");
            System.out.println("  4. Rank all candidates for a job");
            System.out.println("  5. View a job description");
            System.out.println("  6. Change resume / job folders");
            System.out.println("  0. Exit");
            System.out.print("Choose an option: ");

            String choice = readLine();
            if (choice == null) break;
            try {
                switch (choice.trim()) {
                    case "1": analyzeResume(); break;
                    case "2": matchOne(false); break;
                    case "3": matchOne(true); break;
                    case "4": rankCandidates(); break;
                    case "5": viewJob(); break;
                    case "6": changeFolders(); break;
                    case "0": System.out.println("Goodbye!"); return;
                    default: System.out.println("Invalid option, try again.");
                }
            } catch (IOException e) {
                System.out.println("Error reading file: " + e.getMessage());
            }
        }
    }

    // ---------------------------------------------------------------- actions

    private void analyzeResume() throws IOException {
        Path p = pickFile(resumeDir, "resume");
        if (p == null) return;
        printResume(resumeParser.parse(p));
    }

    private void matchOne(boolean recommend) throws IOException {
        Path rp = pickFile(resumeDir, "resume");
        if (rp == null) return;
        Path jp = pickFile(jobDir, "job description");
        if (jp == null) return;

        Resume resume = resumeParser.parse(rp);
        JobDescription job = jobParser.parse(jp);
        Matcher.Result res = matcher.match(resume, job);
        printMatch(res);
        if (recommend) printRecommendations(res);
        else if (!res.requiredMissing.isEmpty() || !res.preferredMissing.isEmpty()) {
            System.out.println("(Choose option 3 for learning recommendations.)");
        }
    }

    private void rankCandidates() throws IOException {
        Path jp = pickFile(jobDir, "job description");
        if (jp == null) return;
        List<Path> files = listTxt(resumeDir);
        if (files.isEmpty()) {
            System.out.println("No resumes found in " + resumeDir.toAbsolutePath());
            return;
        }
        JobDescription job = jobParser.parse(jp);
        List<Resume> resumes = new ArrayList<>();
        for (Path f : files) resumes.add(resumeParser.parse(f));
        List<Matcher.Result> ranked = matcher.rank(resumes, job);

        System.out.println();
        System.out.println(LINE);
        System.out.println(" CANDIDATE RANKING  -  " + job.title);
        System.out.println(LINE);
        System.out.printf(" %-4s %-22s %-7s %-9s %-7s %s%n", "Rank", "Candidate", "Score", "Req.Skill", "Exp(y)", "Verdict");
        System.out.println(THIN);
        int rank = 1;
        for (Matcher.Result r : ranked) {
            System.out.printf(" %-4d %-22s %-7s %-9s %-7.1f %s%n",
                    rank++, truncate(r.resume.name, 22), String.format("%.1f%%", r.total),
                    r.requiredMatched.size() + "/" + job.requiredSkills.size(),
                    r.resume.experienceYears, r.verdict);
        }
        System.out.println(THIN);
        Matcher.Result top = ranked.get(0);
        System.out.println(" Top candidate: " + top.resume.name + " (" + String.format("%.1f%%", top.total) + ")");
    }

    private void viewJob() throws IOException {
        Path jp = pickFile(jobDir, "job description");
        if (jp == null) return;
        JobDescription job = jobParser.parse(jp);
        System.out.println();
        System.out.println(LINE);
        System.out.println(" JOB: " + job.title + "   [" + job.fileName + "]");
        System.out.println(LINE);
        System.out.println(" Required skills  : " + joinOrDash(job.requiredSkills));
        System.out.println(" Preferred skills : " + joinOrDash(job.preferredSkills));
        System.out.println(" Min. experience  : " + (job.minExperience > 0 ? job.minExperience + " year(s)" : "not specified"));
        System.out.println(" Min. education   : " + EducationUtil.levelName(job.minEducationLevel));
    }

    private void changeFolders() {
        System.out.println("Current resume folder: " + resumeDir.toAbsolutePath());
        System.out.print("New resume folder (blank = keep): ");
        String r = readLine();
        if (r != null && !r.trim().isEmpty()) resumeDir = Paths.get(r.trim());
        System.out.println("Current job folder: " + jobDir.toAbsolutePath());
        System.out.print("New job folder (blank = keep): ");
        String j = readLine();
        if (j != null && !j.trim().isEmpty()) jobDir = Paths.get(j.trim());
        System.out.println("Folders updated.");
    }

    // ---------------------------------------------------------------- printing

    private void printResume(Resume r) {
        System.out.println();
        System.out.println(LINE);
        System.out.println(" RESUME ANALYSIS  [" + r.fileName + "]");
        System.out.println(LINE);
        System.out.println(" Name       : " + r.name);
        System.out.println(" Email      : " + r.email);
        System.out.println(" Phone      : " + r.phone);
        System.out.println(" Experience : " + (r.experienceYears > 0 ? r.experienceYears + " year(s)" : "not detected"));
        System.out.println(" Education  : " + EducationUtil.levelName(r.educationLevel));
        for (String e : r.education) System.out.println("              - " + e);
        System.out.println(" Skills (" + r.skills.size() + "):");

        Map<String, List<String>> byCategory = new TreeMap<>();
        for (String s : r.skills) byCategory.computeIfAbsent(db.categoryOf(s), k -> new ArrayList<>()).add(s);
        for (Map.Entry<String, List<String>> e : byCategory.entrySet()) {
            System.out.printf("   %-10s: %s%n", e.getKey(), String.join(", ", e.getValue()));
        }
    }

    private void printMatch(Matcher.Result res) {
        System.out.println();
        System.out.println(LINE);
        System.out.println(" MATCH REPORT");
        System.out.println(" Candidate : " + res.resume.name + "  [" + res.resume.fileName + "]");
        System.out.println(" Job       : " + res.job.title + "  [" + res.job.fileName + "]");
        System.out.println(LINE);
        System.out.printf(" OVERALL SCORE: %.1f%%  %s  -> %s%n", res.total, bar(res.total), res.verdict);
        System.out.println(THIN);
        if (!res.job.requiredSkills.isEmpty())
            System.out.printf(" Required skills  : %5.1f%%  (%d of %d)%n", res.requiredScore * 100,
                    res.requiredMatched.size(), res.job.requiredSkills.size());
        if (!res.job.preferredSkills.isEmpty())
            System.out.printf(" Preferred skills : %5.1f%%  (%d of %d)%n", res.preferredScore * 100,
                    res.preferredMatched.size(), res.job.preferredSkills.size());
        if (res.job.minExperience > 0)
            System.out.printf(" Experience       : %5.1f%%  (has %.1f, needs %.1f yrs)%n", res.experienceScore * 100,
                    res.resume.experienceYears, res.job.minExperience);
        if (res.job.minEducationLevel > EducationUtil.NONE)
            System.out.printf(" Education        : %5.1f%%  (has %s, needs %s)%n", res.educationScore * 100,
                    EducationUtil.levelName(res.resume.educationLevel),
                    EducationUtil.levelName(res.job.minEducationLevel));
        System.out.println(THIN);
        System.out.println(" Matched required : " + listOrDash(res.requiredMatched));
        System.out.println(" Missing required : " + listOrDash(res.requiredMissing));
        System.out.println(" Matched preferred: " + listOrDash(res.preferredMatched));
        System.out.println(" Missing preferred: " + listOrDash(res.preferredMissing));
    }

    private void printRecommendations(Matcher.Result res) {
        System.out.println(THIN);
        System.out.println(" RECOMMENDATIONS");
        List<String> recs = matcher.recommendations(res);
        if (recs.isEmpty()) System.out.println(" None - the candidate already meets every requirement!");
        for (String s : recs) System.out.println("  * " + s);
    }

    // ---------------------------------------------------------------- helpers

    private Path pickFile(Path dir, String label) {
        List<Path> files = listTxt(dir);
        System.out.println();
        System.out.println("Available " + label + " files in " + dir.toAbsolutePath().normalize() + ":");
        if (files.isEmpty()) System.out.println("  (none found)");
        for (int i = 0; i < files.size(); i++) {
            System.out.printf("  %2d. %s%n", i + 1, files.get(i).getFileName());
        }
        System.out.print("Enter number or full path to a .txt file (blank to cancel): ");
        String s = readLine();
        if (s == null || s.trim().isEmpty()) return null;
        s = s.trim();
        try {
            int n = Integer.parseInt(s);
            if (n >= 1 && n <= files.size()) return files.get(n - 1);
            System.out.println("Number out of range.");
            return null;
        } catch (NumberFormatException ignored) { /* treat as path */ }
        Path p = Paths.get(s);
        if (!Files.isRegularFile(p)) {
            System.out.println("File not found: " + p.toAbsolutePath());
            return null;
        }
        return p;
    }

    private List<Path> listTxt(Path dir) {
        List<Path> out = new ArrayList<>();
        if (!Files.isDirectory(dir)) return out;
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(dir, "*.txt")) {
            for (Path p : ds) out.add(p);
        } catch (IOException e) {
            System.out.println("Cannot read folder: " + e.getMessage());
        }
        Collections.sort(out);
        return out;
    }

    private String readLine() {
        return in.hasNextLine() ? in.nextLine() : null;
    }

    private static String bar(double pct) {
        int filled = (int) Math.round(pct / 5.0);
        return "[" + "#".repeat(filled) + ".".repeat(20 - filled) + "]";
    }

    private static String truncate(String s, int n) {
        return s.length() <= n ? s : s.substring(0, n - 1) + "~";
    }

    private static String listOrDash(Collection<String> c) {
        return c.isEmpty() ? "-" : String.join(", ", c);
    }

    private static String joinOrDash(Collection<String> c) {
        return listOrDash(c);
    }
}
