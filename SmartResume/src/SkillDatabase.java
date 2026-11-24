import java.util.*;
import java.util.regex.Pattern;

/**
 * Master list of known skills with aliases, plus helpers to detect them in text.
 * Add new skills in the constructor: add(category, canonicalName, alias1, alias2, ...)
 */
public class SkillDatabase {

    private final Map<String, Pattern> patterns = new LinkedHashMap<>();
    private final Map<String, String> categories = new LinkedHashMap<>();
    private final Map<String, String[]> prerequisites = new HashMap<>();

    public SkillDatabase() {
        // Programming languages
        add("Language", "Java", "core java", "java se", "j2ee", "java 8", "java 11", "java 17");
        add("Language", "Python", "python3", "python 3");
        add("Language", "JavaScript", "js", "ecmascript", "es6");
        add("Language", "TypeScript");
        add("Language", "C++", "cpp");
        add("Language", "C#", "c sharp", "csharp");
        add("Language", "C", "c programming", "c language");
        add("Language", "Go", "golang");
        add("Language", "Kotlin");
        add("Language", "PHP");
        add("Language", "Ruby");
        add("Language", "Swift");
        add("Language", "Scala");
        add("Language", "R Programming", "r language", "r programming");

        // Web / frontend
        add("Frontend", "HTML", "html5");
        add("Frontend", "CSS", "css3");
        add("Frontend", "React", "react.js", "reactjs");
        add("Frontend", "Angular", "angularjs");
        add("Frontend", "Vue.js", "vue", "vuejs");
        add("Frontend", "Bootstrap");
        add("Frontend", "Tailwind CSS", "tailwind");

        // Backend / frameworks
        add("Backend", "Spring", "spring framework", "spring mvc");
        add("Backend", "Spring Boot", "springboot");
        add("Backend", "Hibernate", "jpa");
        add("Backend", "Node.js", "nodejs", "node js");
        add("Backend", "Express.js", "expressjs", "express js");
        add("Backend", "Django");
        add("Backend", "Flask");
        add("Backend", "FastAPI");
        add("Backend", ".NET", "dotnet", "asp.net", ".net core");
        add("Backend", "REST API", "restful", "restful api", "restful apis", "rest apis", "rest services");
        add("Backend", "Microservices", "microservice");
        add("Backend", "GraphQL");

        // Databases
        add("Database", "SQL", "t-sql", "pl/sql");
        add("Database", "MySQL");
        add("Database", "PostgreSQL", "postgres");
        add("Database", "Oracle", "oracle db");
        add("Database", "MongoDB", "mongo");
        add("Database", "Redis");
        add("Database", "SQL Server", "mssql", "ms sql");
        add("Database", "Elasticsearch");

        // DevOps / cloud
        add("DevOps", "Git", "github", "gitlab", "bitbucket");
        add("DevOps", "Docker", "containerization");
        add("DevOps", "Kubernetes", "k8s");
        add("DevOps", "Jenkins");
        add("DevOps", "CI/CD", "cicd", "ci cd", "continuous integration");
        add("DevOps", "Linux", "unix", "shell scripting", "bash");
        add("Cloud", "AWS", "amazon web services", "ec2", "s3");
        add("Cloud", "Azure", "microsoft azure");
        add("Cloud", "GCP", "google cloud");
        add("DevOps", "Terraform");
        add("DevOps", "Maven");
        add("DevOps", "Gradle");

        // Data / ML
        add("Data", "Machine Learning", "ml");
        add("Data", "Deep Learning");
        add("Data", "NLP", "natural language processing");
        add("Data", "Pandas");
        add("Data", "NumPy");
        add("Data", "TensorFlow");
        add("Data", "PyTorch");
        add("Data", "Scikit-learn", "sklearn", "scikit learn");
        add("Data", "Data Analysis", "data analytics");
        add("Data", "Statistics");
        add("Data", "Power BI", "powerbi");
        add("Data", "Tableau");
        add("Data", "MS Excel", "microsoft excel", "advanced excel", "excel spreadsheets");
        add("Data", "Apache Spark", "spark", "pyspark");
        add("Data", "Hadoop");
        add("Data", "Kafka", "apache kafka");

        // Engineering practices
        add("Practice", "Data Structures", "dsa", "data structures and algorithms");
        add("Practice", "Algorithms");
        add("Practice", "OOP", "object oriented programming", "object-oriented programming", "oops");
        add("Practice", "Design Patterns");
        add("Practice", "System Design");
        add("Practice", "Agile", "scrum");
        add("Practice", "JUnit", "unit testing");
        add("Practice", "Selenium");
        add("Practice", "Jira");
        add("Practice", "Multithreading", "concurrency", "multi-threading");

        // Soft skills
        add("Soft", "Communication", "communication skills");
        add("Soft", "Leadership", "team leadership");
        add("Soft", "Problem Solving", "problem-solving");
        add("Soft", "Teamwork", "team player", "collaboration");

        // Learning prerequisites (used for recommendations)
        prereq("Spring Boot", "Java", "Spring");
        prereq("Spring", "Java");
        prereq("Hibernate", "Java", "SQL");
        prereq("React", "JavaScript", "HTML", "CSS");
        prereq("Angular", "TypeScript", "JavaScript");
        prereq("Vue.js", "JavaScript");
        prereq("TypeScript", "JavaScript");
        prereq("Node.js", "JavaScript");
        prereq("Express.js", "Node.js");
        prereq("Django", "Python");
        prereq("Flask", "Python");
        prereq("FastAPI", "Python");
        prereq("Pandas", "Python");
        prereq("NumPy", "Python");
        prereq("Machine Learning", "Python", "Statistics");
        prereq("Deep Learning", "Machine Learning");
        prereq("TensorFlow", "Python", "Machine Learning");
        prereq("PyTorch", "Python", "Machine Learning");
        prereq("Scikit-learn", "Python", "Machine Learning");
        prereq("NLP", "Python", "Machine Learning");
        prereq("Kubernetes", "Docker", "Linux");
        prereq("Docker", "Linux");
        prereq("Jenkins", "Git");
        prereq("CI/CD", "Git");
        prereq("Microservices", "REST API");
        prereq("Apache Spark", "SQL");
        prereq("Terraform", "AWS");
        prereq("Design Patterns", "OOP");
        prereq("System Design", "Data Structures");
    }

    private void add(String category, String canonical, String... aliases) {
        List<String> all = new ArrayList<>();
        all.add(canonical);
        all.addAll(Arrays.asList(aliases));
        patterns.put(canonical, buildPattern(all));
        categories.put(canonical, category);
    }

    private void prereq(String skill, String... needs) {
        prerequisites.put(skill, needs);
    }

    private static Pattern buildPattern(Collection<String> words) {
        StringBuilder sb = new StringBuilder();
        for (String w : words) {
            if (sb.length() > 0) sb.append('|');
            sb.append(Pattern.quote(w));
        }
        // not preceded/followed by letters, digits, '+' or '#'  (so "Java" doesn't match "JavaScript", "C" not in "C++")
        return Pattern.compile("(?<![A-Za-z0-9+#.])(?:" + sb + ")(?![A-Za-z0-9+#])",
                Pattern.CASE_INSENSITIVE);
    }

    /** Finds all known skills in free text. Order = order of the database. */
    public Set<String> extract(String text) {
        Set<String> found = new LinkedHashSet<>();
        if (text == null || text.isEmpty()) return found;

        // Collect every occurrence (skill, start, end)
        List<Object[]> hits = new ArrayList<>();
        for (Map.Entry<String, Pattern> e : patterns.entrySet()) {
            java.util.regex.Matcher m = e.getValue().matcher(text);
            while (m.find()) hits.add(new Object[]{e.getKey(), m.start(), m.end()});
        }
        // Drop an occurrence if it sits inside a longer occurrence of a different skill
        // (e.g. the "Spring" inside "Spring Boot", the "SQL" inside "SQL Server")
        Set<String> kept = new HashSet<>();
        for (Object[] a : hits) {
            boolean swallowed = false;
            for (Object[] b : hits) {
                if (a == b || a[0].equals(b[0])) continue;
                int as = (Integer) a[1], ae = (Integer) a[2], bs = (Integer) b[1], be = (Integer) b[2];
                if (bs <= as && ae <= be && (be - bs) > (ae - as)) { swallowed = true; break; }
            }
            if (!swallowed) kept.add((String) a[0]);
        }
        for (String skill : patterns.keySet()) if (kept.contains(skill)) found.add(skill);
        return found;
    }

    /** True if the skill (known or custom) appears in the text. */
    public boolean containsSkill(String text, String skill) {
        Pattern p = patterns.get(skill);
        if (p == null) p = buildPattern(Collections.singletonList(skill));
        return p.matcher(text).find();
    }

    public boolean isKnown(String skill) {
        return patterns.containsKey(skill);
    }

    public String categoryOf(String skill) {
        return categories.getOrDefault(skill, "Other");
    }

    public String[] prerequisitesOf(String skill) {
        return prerequisites.getOrDefault(skill, new String[0]);
    }

    public int size() {
        return patterns.size();
    }
}
