import groovy.json.JsonSlurper

class Globals{
    static String mavenInstallation = "Maven 3.2.5"
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
//createBuildJobs(testEditorView, 'test-editor')
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
    String jobName = 'Test-Editor-Target-Platform'
    def buildJob = defaultBuildJob(jobName, repo, 'master', false, { job ->
        job.steps {
            maven {
                mavenInstallation(Globals.mavenInstallation)
                goals('clean install')
            }
        }

        job.publishers {
            archiveArtifacts('org.testeditor.releng.target/p2-local/**')
        }
    })
    addJob2View(view, jobName)
}

/**
 * Creates build jobs for master-, develop- and all feature-branches.
 */
void createBuildJobs(def view, String repo){

    // Create jobs for static branches
    ['develop', 'master'].each { branch ->
        // define jobs
        def jobName = createJobName(repo, branch)
        def buildJob = defaultBuildJob(jobName, repo, branch, true, { job ->
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
        addJob2View(view, jobName)
    }

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
        defaultBuildJob(featureJobName, repo, branch.name, true, { job ->
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
        addJob2View(view, featureJobName)
    }
}

/**
 * Creates a release job for the given fixture.
 */
void createReleaseJobs4Fixtures(def view, String fixtureName, String repo){
        def releaseJobName = "${fixtureName}_fixture_RELEASE"

        defaultBuildJob(releaseJobName, repo, 'master', true, { job ->
            if(!fixtureName.equals('core')){
                job.parameters {
                    stringParam('NEW_FIXTURE_VERSION', '', 'Please enter the fixture version number of the new release.')
                    stringParam('CORE_VERSION', '', 'Please enter the version number for the parent pom, this fixture depends on.')
                }
            }

            if(["swing", "swt"].contains(fixtureName)){
                addXvfbStart(job)
            }

            addPreBuildCleanup(job)

            job.steps {
                shell('git merge origin/develop')

                maven {
                    mavenInstallation(Globals.mavenInstallation)
                    if(fixtureName.equals('core')){
                        goals("build-helper:parse-version")
                        goals("versions:set")
                        property("generateBackupPoms", "false")
                        property("newVersion", "\${parsedVersion.majorVersion}.\${parsedVersion.minorVersion}.\${parsedVersion.incrementalVersion}")
                        rootPOM("${fixtureName}/org.testeditor.fixture.parent/pom.xml")
                    } else{
                        goals('versions:update-parent')
                        property("generateBackupPoms", "false")
                        property("parentVersion", "[\${CORE_VERSION}]")
                        rootPOM("${fixtureName}/pom.xml")
                    }
                }

                if(!fixtureName.equals('core')){
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
                    if(fixtureName == 'core') {
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
def defaultBuildJob(String jobName, String repo, String branch, boolean withQA, Closure closure) {
    def buildJob = job(jobName) {
        description "Performs a build on branch: $branch"
        scm {
            git (
                    "git@github.com:test-editor/${repo}.git",
                    branch,
                    gitConfigure(branch, true)
            )
        }
        triggers {
            // githubPush()
        }
    }

    if(withQA){
        addQAPublishers(buildJob)
    }

    if(closure){
        closure(buildJob)
    }
    return buildJob
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
