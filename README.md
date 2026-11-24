# Smart Resume Analyzer & Job Matcher (Java, console)

Extracts skills, education and experience from plain-text resumes, scores them against a
job description, recommends missing skills, and ranks all candidates for a job.

## Run

Requires JDK 8+ (tested on a recent JDK).

```
cd SmartResume
javac -d out src/*.java
java -cp out Main            # uses ./resumes and ./jobs
java -cp out Main /some/dir  # uses /some/dir/resumes and /some/dir/jobs
```

## Menu

1. Analyze a resume - name, email, phone, skills (grouped by category), education, years of experience
2. Match a resume to a job - overall score + breakdown, matched/missing skills
3. Recommend missing skills - prioritised list with "learn first" prerequisites
4. Rank all candidates for a job - every resume in `resumes/` sorted by score
5. View a parsed job description
6. Change resume / job folders

## Resume format (free text, .txt)

Any layout works. Parsing is helped by section headings such as `SKILLS`, `EXPERIENCE`, `EDUCATION`.
Experience is taken from phrases like "4 years of experience" and from date ranges in the
experience section (`Jun 2021 - Present`, `2018 - 2020`); the larger value is used.

## Job description format (.txt)

```
Title: Java Backend Developer
Required Skills: Java, Spring Boot, REST API, SQL, Git
Preferred Skills: Docker, Microservices, Kubernetes, AWS
Minimum Experience: 3 years
Minimum Education: Bachelor's
```

Skill lists can also be bullet lines under the key. Skills not in the built-in database are
still matched as plain keywords. If there is no `Required Skills` key, every known skill found
in the text is treated as required.

## Scoring

| Component        | Weight | Score                                   |
|------------------|--------|-----------------------------------------|
| Required skills  | 55%    | matched / required                      |
| Preferred skills | 15%    | matched / preferred                     |
| Experience       | 20%    | min(1, candidate years / required years)|
| Education        | 10%    | 1 if level met, else level ratio        |

Components that a job does not specify are dropped and the remaining weights are re-normalised.
Verdicts: >=80 Excellent, >=60 Good, >=40 Moderate, else Weak.

## Files

- `Main.java` - console menu and report printing
- `SkillDatabase.java` - skills, aliases, prerequisites (add your own in the constructor)
- `ResumeParser.java`, `Resume.java` - resume extraction
- `JobParser.java`, `JobDescription.java` - job extraction
- `EducationUtil.java` - education level detection
- `Matcher.java` - scoring, recommendations, ranking
