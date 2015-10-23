import groovy.json.JsonSlurper

class Globals {
    static String mavenInstallation = "Maven 3.2.5"
    static String targetPlattformJobName = "Test-Editor-Target-Platform"
    static String teIntegrationTestJobName = "Test-Editor-Integration-Test"
}

String[] fixtures = ["core", "web", "rest", "soap", "swing", "swt"]

def releaseView = createView("Release", "<h3>Release build jobs for the Test-Editor artefacts.</h3>\n" +
        "(see <a href=\"https://github.com/test-editor\">test-editor @ GitHub</a>)")
def fixtureView = createView("Fixtures", "<h3>Build jobs for the fixtures of the Test-Editor artefacts.</h3>\n" +
        "(see <a href=\"https://github.com/test-editor/fixtures\">fixtures @ GitHub</a>)")
def testEditorView = createView("Test-Editor", "<h3>Build jobs for the Test-Editor artefacts.</h3>\n" +
        "(see <a href=\"https://github.com/test-editor/test-editor\">test-editor @ GitHub</a>)")
//def amlView = createView("AML", "<h3>Build jobs for the Application (Under Test) Mapping Language.</h3>\n" +
//        "(see <a href=\"https://github.com/test-editor/test-editor-xtext\">AML @ GitHub</a>)")


/**
 * Creates all Test-Editor related build jobs
 */
createTargetPlattformBuildJob(testEditorView, 'te.target')
//createBuildJobs(amlView, 'test-editor-xtext')
createTEBuildJobs(testEditorView, 'test-editor')
createBuildJobs(fixtureView, 'fixtures')

/**
 * Creates single release jobs for each fixture.
 */
fixtures.each { fixtureName ->
    createReleaseJobs4Fixtures(releaseView, fixtureName, 'fixtures')
}

/**
 * Creates build job for target plattform
 */
void createTargetPlattformBuildJob(def view, String repo){
    String jobName = Globals.targetPlattformJobName
    def buildJob = defaultBuildJob(jobName, repo, 'master', { job ->
        job.steps {
            maven {
                mavenInstallation(Globals.mavenInstallation)
                goals('clean install')
            }
        }
    })
    addArchiveArtefacts(buildJob, 'org.testeditor.releng.target/p2-local/**')
    addJob2View(view, jobName)
}

/**
 * Creates TE build jobs for develop- and all feature-branches.
 */
void createTEBuildJobs(def view, String repo) {

    // Create job for develop branch
    def jobName = createJobName(repo, 'develop')
    def buildJob = defaultBuildJob(jobName, repo, 'develop', { job ->
        job.steps {
            copyArtifacts(Globals.targetPlattformJobName) {
                buildSelector {
                    latestSuccessful(true)
                }
            }

            maven {
                mavenInstallation(Globals.mavenInstallation)
                goals('clean package checkstyle:checkstyle pmd:pmd pmd:cpd -P product')
                property("te-target", "file://\${WORKSPACE}/org.testeditor.releng.target/p2-local")
            }
        }
    })
    addGithubPush(buildJob)
    addXvfbStart(buildJob)
    addPreBuildCleanup(buildJob)
    addExtendedQAPublishers(buildJob)
    addArchiveArtefacts(buildJob, 'product/org.testeditor.product/target/products/TestEditor*.zip')
    addTriggerBuildOnProject(buildJob, Globals.teIntegrationTestJobName)
    addJob2View(view, jobName)

    // Create feature branches
    //createFeatureBranches(view, repo)
}

/**
 * Creates build jobs for develop- and all feature-branches.
 */
void createBuildJobs(def view, String repo){

    // Create job for develop branch
    def jobName = createJobName(repo, 'develop')
    def buildJob = defaultBuildJob(jobName, repo, 'develop', { job ->
        job.steps {
            maven {
                mavenInstallation(Globals.mavenInstallation)
                goals('clean package -DskipTests=true -Dmaven.javadoc.skip=true -B -V')
            }
            maven {
                mavenInstallation(Globals.mavenInstallation)
                goals('test -B')
            }
        }
    })
    addXvfbStart(buildJob)
    addPreBuildCleanup(buildJob)
    addQAPublishers(buildJob)
    addJob2View(view, jobName)

    // Create feature branches
    createFeatureBranches(view, repo)
}

/**
 * Creates build jobs for feature-branches.
 */
void createFeatureBranches(def view, String repo) {
    def branchApi = new URL("https://api.github.com/repos/test-editor/$repo/branches")
    def branches = new JsonSlurper().parse(branchApi.newReader())

    branches.findAll { it.name.startsWith('feature/') }.each { branch ->
        def featureJobName = createJobName(repo, branch.name)
        def buildJob = defaultBuildJob(featureJobName, repo, branch.name, { job ->
            job.steps {
                if (repo == 'test-editor') { // TODO
                    copyArtifacts('Test-Editor-Target-Platform') {
                        buildSelector {
                            latestSuccessful(true)
                        }
                    }
                }
                maven {
                    mavenInstallation(Globals.mavenInstallation)
                    goals('clean package -DskipTests=true -Dmaven.javadoc.skip=true -B -V')
                }
                maven {
                    mavenInstallation(Globals.mavenInstallation)
                    goals('test -B')
                }
            }
        })
        addQAPublishers(buildJob)
        addJob2View(view, featureJobName)
    }
}

/**
 * Creates a release job for the given fixture.
 */
void createReleaseJobs4Fixtures(def view, String fixtureName, String repo){
        def releaseJobName = "${fixtureName}_fixture_RELEASE"

    defaultBuildJob(releaseJobName, repo, 'master', { job ->
        if (!fixtureName.equals('core')) {
            job.parameters {
                stringParam('NEW_FIXTURE_VERSION', '', 'Please enter the fixture version number of the new release.')
                stringParam('CORE_VERSION', '', 'Please enter the version number for the parent pom, this fixture depends on.')
            }
        }

        if (["swing", "swt"].contains(fixtureName)) {
            addXvfbStart(job)
        }

        addPreBuildCleanup(job)
        addQAPublishers(job)

        job.steps {
            shell('git merge origin/develop')

            maven {
                mavenInstallation(Globals.mavenInstallation)
                if (fixtureName.equals('core')) {
                    goals("build-helper:parse-version")
                    goals("versions:set")
                    property("generateBackupPoms", "false")
                    property("newVersion", "\${parsedVersion.majorVersion}.\${parsedVersion.minorVersion}.\${parsedVersion.incrementalVersion}")
                    rootPOM("${fixtureName}/org.testeditor.fixture.parent/pom.xml")
                } else {
                    goals('versions:update-parent')
                    property("generateBackupPoms", "false")
                    property("parentVersion", "[\${CORE_VERSION}]")
                    rootPOM("${fixtureName}/pom.xml")
                }
            }

            if (!fixtureName.equals('core')) {
                maven {
                    mavenInstallation(Globals.mavenInstallation)
                    goals("versions:set")
                    property("generateBackupPoms", "false")
                    property("newVersion", "\${NEW_FIXTURE_VERSION}")
                    property("artifactId", "${fixtureName}-fixture")
                    property("updateMatchingVersions", "false")
                    rootPOM("${fixtureName}/pom.xml")
                }
            }

            shell('git add *')
            shell('git commit -m "develop branch merged and release version set."')

            maven {
                mavenInstallation(Globals.mavenInstallation)
                goals('clean package -DskipTests=true -B -V')
                rootPOM("${fixtureName}/pom.xml")
            }

            maven {
                mavenInstallation(Globals.mavenInstallation)
                goals('test -B')
                rootPOM("${fixtureName}/pom.xml")
            }

            maven {
                mavenInstallation(Globals.mavenInstallation)
                goals('deploy')
                rootPOM("${fixtureName}/pom.xml")
            }

            maven {
                mavenInstallation(Globals.mavenInstallation)
                goals('scm:tag')
                rootPOM("${fixtureName}/pom.xml")
                property("connectionUrl", "scm:git:ssh://git@github.com/test-editor/${repo}")
                property("developerConnectionUrl", "scm:git:ssh://git@github.com/test-editor/${repo}")
            }

            maven {
                mavenInstallation(Globals.mavenInstallation)
                goals("build-helper:parse-version")
                goals("versions:set")
                property("generateBackupPoms", "false")
                property("newVersion", "\${parsedVersion.majorVersion}.\${parsedVersion.minorVersion}.\${parsedVersion.nextIncrementalVersion}-SNAPSHOT")
                if (fixtureName == 'core') {
                    rootPOM("${fixtureName}/org.testeditor.fixture.parent/pom.xml")
                } else {
                    property("artifactId", "${fixtureName}-fixture")
                    property("updateMatchingVersions", "false")
                    rootPOM("${fixtureName}/pom.xml")
                }
            }

            shell('git add *')
            shell('git commit -m "next snapshot version set."')
            shell('git push origin master')
            shell('git checkout -b develop --track origin/develop')
            shell('git merge origin/master')
            shell('git push origin develop')
        }
    })
    addJob2View(view, releaseJobName)
}

/**
 * Defines how a default build job should look like.
 */
def defaultBuildJob(String jobName, String repo, String branch, Closure closure) {
    def buildJob = job(jobName) {
        description "Performs a build on branch: $branch"
        scm {
            git (
                    "git@github.com:test-editor/${repo}.git",
                    branch,
                    gitConfigure(branch, true)
            )
        }
    }

    if(closure){
        closure(buildJob)
    }
    return buildJob
}

/**
 * Adds github push trigger to given job
 */
void addGithubPush(def job) {
    job.with {
        triggers {
            githubPush()
        }
    }
}

/**
 * Adds QA publishers to given job
 */
void addQAPublishers(def job){
    job.with{
        publishers {
            checkstyle('**/target/checkstyle-result.xml')
            findbugs('**/target/findbugsXml.xml')
            jacocoCodeCoverage {
                execPattern '**/**.exec'
            }
            archiveJunit '**/target/surefire-reports/*.xml'
        }
    }
}

/**
 * Adds QA publishers to given job
 */
void addExtendedQAPublishers(def job) {
    job.with {
        publishers {
            checkstyle('**/target/checkstyle-result.xml')
            findbugs('**/target/findbugsXml.xml')
            pmd('**/target/pmd.xml')
            jacocoCodeCoverage {
                execPattern '**/**.exec'
            }
            archiveJunit '**/target/surefire-reports/*.xml'
        }
    }
}

/**
 * Adds artefact archiving
 */
void addArchiveArtefacts(def job, String artefact) {
    job.with {
        publishers {
            archiveArtifacts(artefact)
        }
    }
}

/**
 * Git configuration section.
 */
def gitConfigure(branchName, skippingTag) {
    { node ->
        // checkout to local branch
        node / 'extensions' / 'hudson.plugins.git.extensions.impl.LocalBranch' / localBranch(branchName)

        // no default tagging
        node / 'skipTag'(skippingTag)
    }
}

/**
 * FitNesse plugin configuration section.
 */
def fitNesseConfigure(path) {
    { project ->
        project / 'publishers' / 'hudson.plugins.fitnesse.FitnesseResultsRecorder' {
            'fitnessePathToXmlResultsIn'(path)
        }
    }
}

/**
 * Creates list view with default columns.
 */
def createView(String viewName, String text){
    return listView(viewName){
        description("${text}")
        columns {
            status()
            weather()
            name()
            lastSuccess()
            lastFailure()
            lastDuration()
            buildButton()
        }
    }
}

/**
 * Creates job name for build jobs.
 */
def createJobName(String repo, String branch){
    return "${repo}_${branch}_CI".replaceAll('/', '_')
}

/**
 * Adds job to given view.
 */
void addJob2View(def view, String jobName){
    view.with {
        jobs {
            name(jobName)
        }
    }
}

/**
 * Adds the xvfb start to build job
 */
void addXvfbStart(def job){
    job.with{
        wrappers {
            xvfb('System') {
                timeout(0)
                screen('1024x768x24')
                displayNameOffset(1)
            }
        }
    }
}

/**
 * Adds pre build clean-up of workspace
 */
void addPreBuildCleanup(def job){
    job.with{
        wrappers {
            preBuildCleanup()
        }
    }
}

void addTriggerBuildOnProject(def job, String projectName) {
    job.with {
        publishers {
            downstream(projectName, 'SUCCESS')
        }
    }
}
