import java.util.*;

/** Scores resumes against a job, finds missing skills, and ranks candidates. */
public class Matcher {

    // Weights of each component (re-normalised when a component does not apply to the job)
    private static final double W_REQUIRED = 0.55;
    private static final double W_PREFERRED = 0.15;
    private static final double W_EXPERIENCE = 0.20;
    private static final double W_EDUCATION = 0.10;

    public static class Result {
        public Resume resume;
        public JobDescription job;
        public List<String> requiredMatched = new ArrayList<>();
        public List<String> requiredMissing = new ArrayList<>();
        public List<String> preferredMatched = new ArrayList<>();
        public List<String> preferredMissing = new ArrayList<>();
        public double requiredScore, preferredScore, experienceScore, educationScore; // 0..1
        public double total;                                                           // 0..100
        public String verdict;
    }

    private final SkillDatabase db;

    public Matcher(SkillDatabase db) {
        this.db = db;
    }

    public Result match(Resume r, JobDescription job) {
        Result res = new Result();
        res.resume = r;
        res.job = job;

        for (String s : job.requiredSkills) {
            if (has(r, s)) res.requiredMatched.add(s); else res.requiredMissing.add(s);
        }
        for (String s : job.preferredSkills) {
            if (has(r, s)) res.preferredMatched.add(s); else res.preferredMissing.add(s);
        }

        double weightSum = 0, weighted = 0;

        if (!job.requiredSkills.isEmpty()) {
            res.requiredScore = (double) res.requiredMatched.size() / job.requiredSkills.size();
            weighted += W_REQUIRED * res.requiredScore;
            weightSum += W_REQUIRED;
        }
        if (!job.preferredSkills.isEmpty()) {
            res.preferredScore = (double) res.preferredMatched.size() / job.preferredSkills.size();
            weighted += W_PREFERRED * res.preferredScore;
            weightSum += W_PREFERRED;
        }
        if (job.minExperience > 0) {
            res.experienceScore = Math.min(1.0, r.experienceYears / job.minExperience);
            weighted += W_EXPERIENCE * res.experienceScore;
            weightSum += W_EXPERIENCE;
        }
        if (job.minEducationLevel > EducationUtil.NONE) {
            res.educationScore = r.educationLevel >= job.minEducationLevel
                    ? 1.0 : (double) r.educationLevel / job.minEducationLevel;
            weighted += W_EDUCATION * res.educationScore;
            weightSum += W_EDUCATION;
        }

        res.total = weightSum == 0 ? 0 : Math.round(weighted / weightSum * 1000) / 10.0;
        res.verdict = verdict(res.total);
        return res;
    }

    /** Ranks all resumes for one job: highest score first, ties broken by required skills then experience. */
    public List<Result> rank(List<Resume> resumes, JobDescription job) {
        List<Result> results = new ArrayList<>();
        for (Resume r : resumes) results.add(match(r, job));
        results.sort((a, b) -> {
            int c = Double.compare(b.total, a.total);
            if (c != 0) return c;
            c = Integer.compare(b.requiredMatched.size(), a.requiredMatched.size());
            if (c != 0) return c;
            return Double.compare(b.resume.experienceYears, a.resume.experienceYears);
        });
        return results;
    }

    /** Human-readable improvement suggestions for a candidate. */
    public List<String> recommendations(Result res) {
        List<String> out = new ArrayList<>();
        Resume r = res.resume;
        JobDescription job = res.job;

        for (String s : res.requiredMissing) {
            out.add("[HIGH]   Learn " + s + " (required)" + learnFirst(r, s));
        }
        for (String s : res.preferredMissing) {
            out.add("[MEDIUM] Learn " + s + " (preferred)" + learnFirst(r, s));
        }
        if (job.minExperience > 0 && r.experienceYears < job.minExperience) {
            out.add(String.format("[MEDIUM] Gain %.1f more year(s) of experience (has %.1f, needs %.1f) - "
                    + "projects, internships or open-source work can help close the gap",
                    job.minExperience - r.experienceYears, r.experienceYears, job.minExperience));
        }
        if (job.minEducationLevel > EducationUtil.NONE && r.educationLevel < job.minEducationLevel) {
            out.add("[LOW]    Education: job asks for " + EducationUtil.levelName(job.minEducationLevel)
                    + " but resume shows " + EducationUtil.levelName(r.educationLevel)
                    + " - certifications or a higher degree can offset this");
        }
        return out;
    }

    private String learnFirst(Resume r, String skill) {
        List<String> need = new ArrayList<>();
        for (String p : db.prerequisitesOf(skill)) {
            if (!has(r, p)) need.add(p);
        }
        return need.isEmpty() ? "" : "  -> learn first: " + String.join(", ", need);
    }

    private boolean has(Resume r, String skill) {
        return r.skills.contains(skill) || db.containsSkill(r.rawText, skill);
    }

    public static String verdict(double score) {
        if (score >= 80) return "Excellent match";
        if (score >= 60) return "Good match";
        if (score >= 40) return "Moderate match";
        return "Weak match";
    }
}
