import java.util.*;

/** Structured data extracted from one resume file. */
public class Resume {
    public String fileName = "";
    public String name = "Unknown";
    public String email = "-";
    public String phone = "-";
    public Set<String> skills = new LinkedHashSet<>();
    public List<String> education = new ArrayList<>();
    public int educationLevel = EducationUtil.NONE;
    public double experienceYears = 0;
    public String rawText = "";
}
