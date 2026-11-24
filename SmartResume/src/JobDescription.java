import java.util.*;

/** A job posting: required/preferred skills, minimum experience and education. */
public class JobDescription {
    public String fileName = "";
    public String title = "Untitled Role";
    public Set<String> requiredSkills = new LinkedHashSet<>();
    public Set<String> preferredSkills = new LinkedHashSet<>();
    public double minExperience = 0;
    public int minEducationLevel = EducationUtil.NONE;
    public String rawText = "";
}
