# Submission checklist

Student: **Moldir Amanbek, SE-2521**.

The assignment deadline is **23 October 2026, 23:59**, as stated in the supplied PDF. Submit the ZIP to Moodle and include a real GitHub repository link. The oral defense is during Week 9 practice classes.

## Completed in this package

- [x] Maven source project with one graph class and all required algorithms.
- [x] Both bonus algorithms and their checks.
- [x] Four small graph files, including disconnected and cyclic examples, used by tests.
- [x] 21 passing JUnit tests; reference, brute-force, Bellman-Ford, and stack-safety checks.
- [x] Full 50-row CSV and 10-row bonus CSV from actual executions.
- [x] Three PNG plots and a five-page PDF report, with editable Markdown.
- [x] README, AI-use acknowledgement, reproducibility script, defense guide.
- [x] Local Git history with all five feature branches, main, and v1.0, preserved in a Git bundle.

## Account actions that still require completion

- [ ] Publish the repository to the student's GitHub account and record its actual URL.
- [ ] Confirm `main`, all five feature branches, and tag `v1.0` are visible on GitHub.
- [ ] Give the instructor access if the repository is private.
- [ ] Upload `DAA_Assignment3_Moldir_Amanbek_SE-2521.zip` and the GitHub link to Moodle.
- [ ] Rehearse the build, benchmark, and explanations in `DEFENSE_RU.md`.

No GitHub publication or Moodle submission is claimed by the local archive itself.

## Restore the complete Git history

From the extracted project directory:

```bash
git clone repository.bundle ../DAA_Assignment3_Git
cd ../DAA_Assignment3_Git
git switch main
git branch feature/dsu origin/feature/dsu
git branch feature/mst origin/feature/mst
git branch feature/scc origin/feature/scc
git branch feature/paths origin/feature/paths
git branch feature/metrics origin/feature/metrics
git remote remove origin
git branch --list
git tag --list
```

The bundle contains project files as well as history. The extracted archive also contains the bundle and a prebuilt JAR for convenience; the bundle itself is intentionally not committed into its own history.

## Publish with GitHub CLI

If GitHub CLI (`gh`) is already installed, authenticate to the correct account, then run from the restored repository:

```bash
gh auth login
gh repo create DAA_Assignment3_Graphs --private --source=. --remote=origin --push
git push origin --all
git push origin --tags
gh repo view --json url --jq .url
```

If that repository name already exists, choose a new name or deliberately use the intended existing repository; do not overwrite unrelated work. Copy the actual URL printed by the last command into Moodle alongside the ZIP. Private repository access must be granted to the instructor through GitHub; making the source public is optional and requires the student's choice.

Alternatively, create an empty repository on GitHub's website and copy its provided remote URL. Add that URL with `git remote add origin URL`, then push with the two `git push` commands above. A GitHub browser file upload alone does not preserve the required feature-branch history.

## Before the defense

Run `mvn clean verify`, then `java -jar target/city-network-1.0.jar demo`, and finally the full benchmark command from README. Know why the two MSTs agree, why negative edges are safe in a DAG, how SCC finish order works, and why an operation counter is stronger evidence than one timing measurement. A prepared project cannot guarantee an oral-defense grade.
