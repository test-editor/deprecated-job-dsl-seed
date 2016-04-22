import helper.Globals
import javaposse.jobdsl.dsl.jobs.FreeStyleJob
import javaposse.jobdsl.dsl.views.ListView

import static helper.JobHelper.*

String repo = "test-editor-xtext"

ListView testEditorXtextView = createView('Test-Editor 2', """
    <h3>Build jobs for the Test-Editor 2 artifacts.</h3>
    (see <a href="https://github.com/test-editor/test-editor-xtext">test-editor-xtext @ GitHub</a>)
""".stripIndent())

// Job for each branch except for master
def branches = getBranches(repo)
branches.findAll { it.name != 'master' }.each { branch ->
    
    String branchName = branch.name
    def jobName = "${repo}_${branchName}".replaceAll('/', '_')

    FreeStyleJob buildJob = defaultBuildJob(jobName, repo, branchName, { FreeStyleJob job ->
        job.steps {
            // Build the target platform
            maven {
                mavenInstallation(Globals.mavenInstallation)
                goals('clean install')
                rootPOM('releng/org.testeditor.releng.target/pom.xml')
            }
            // Build the Test-Editor
            maven {
                mavenInstallation(Globals.mavenInstallation)
                goals('clean install')
                if (branchName == 'develop') {
                    // Build product only on develop to speed-up feature branch builds
                    goals('-Pproduct')
                }
                mavenOpts('-Xms512m -Xmx2g')
                property('tycho.localArtifacts', 'ignore')
            }
        }
    })
    limitBuildsTo(buildJob, 10)
    addGithubPush(buildJob)
    addXvfbStart(buildJob)
    addPreBuildCleanup(buildJob)
    addExtendedQAPublishers(buildJob)
    if (branchName == 'develop') {
        addArchiveArtefacts(buildJob, 'product/org.testeditor.product/target/products/TestEditor*.zip')
    }
    addJob2View(testEditorXtextView, jobName)
}

/**
 * Creates list view with default columns.
 */
ListView createView(String viewName, String text) {
    return listView(viewName) {
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
 * Defines how a default build job should look like.
 */
FreeStyleJob defaultBuildJob(String jobName, String repo, String branch, Closure closure) {
    FreeStyleJob buildJob = job(jobName)
    addDefaultConfiguration(buildJob, branch, closure)
    addTEGitRepo(buildJob, repo, branch)
    return buildJob
}