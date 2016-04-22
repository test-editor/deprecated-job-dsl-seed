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
    def jobName = createJobName(repo, branchName)

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
