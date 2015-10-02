/**
 * Creates all Test-Editor related build jobs
 */
//createFeatureBranches('test-editor-xtext')
createBuildJobs('fixtures')
createFeatureBranches('test-editor')

def createJobName(String repo, String branch){
    return "${repo}_${branch}_build".replaceAll('/', '_')
}

def createBuildJobs(String repo){

    // Create jobs for static branches
    ['develop', 'master'].each { branch ->
        // define jobs
        def jobName = createJobName(repo, branch)
        defaultBuildJob(jobName, repo, branch, { job ->
            job.steps {
                maven {
                    mavenInstallation('Maven 3.2.5')
                    goals('clean package checkstyle:checkstyle -DskipTests=true -Dmaven.javadoc.skip=true -B -V')
                }
                maven {
                    mavenInstallation('Maven 3.2.5')
                    goals('test -B')
                }
                if(branch == 'develop'){
                    maven {
                        mavenInstallation('Maven 3.2.5')
                        goals('deploy')
                    }
                }
            }
        })
    }

    createFeatureBranches(repo)
}

def createFeatureBranches(String repo) {
    def branchApi = new URL("https://api.github.com/repos/test-editor/$repo/branches")
    def branches = new groovy.json.JsonSlurper().parse(branchApi.newReader())

    branches.findAll { it.name.startsWith('feature/') }.each { branch ->
        def featureJobName = createJobName(repo, branch.name)
        defaultBuildJob(featureJobName, repo, branch.name, { job ->
            job.steps {
                if (repo == 'test-editor') { // TODO
                    copyArtifacts('Test-Editor-Target-Platform') {
                        buildSelector {
                            latestSuccessful(true)
                        }
                    }
                }
                maven {
                    mavenInstallation('Maven 3.2.5')
                    goals('clean package checkstyle:checkstyle -DskipTests=true -Dmaven.javadoc.skip=true -B -V')
                }
                maven {
                    mavenInstallation('Maven 3.2.5')
                    goals('test -B')
                }
            }
        })
    }
}

/**
 * Defines how a default build job should look like.
 */
def defaultBuildJob(String jobName, String repo, String branch, Closure closure) {
    def buildJob = job(jobName) {
        description "Performs a build on branch: $branch"
        scm {
            git (
                    "https://github.com/test-editor/${repo}.git",
                    branch,
                    gitConfigure(branch, true)
            )
        }
        triggers {
            // scm 'H/3 * * * *'
        }
        publishers {
            checkstyle('**/target/checkstyle-result.xml')
            findbugs('**/target/findbugsXml.xml')
            jacocoCodeCoverage {
                execPattern '**/**.exec'
            }
            archiveJunit '**/target/surefire-reports/*.xml'
        }
    }
    if(closure){
        closure(buildJob)
    }
    return buildJob
}

/**
 * Git configuration section.
 */
def gitConfigure(branchName, skippingTag) {
    { node ->
        // checkout to local branch
        node / 'extensions' / 'hudson.plugins.git.extensions.impl.LocalBranch' / localBranch(branchName)

        // checkout to local branch
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