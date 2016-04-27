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
    buildJob.with {
        publishers {
            jacocoCodeCoverage {
                execPattern '**/target/**.exec'
                exclusionPattern '**/*Test.class, **/*IT.class, **/antlr/**'
            }
            archiveJunit '**/target/surefire-reports/*.xml'
            githubCommitNotifier()
        }
    }
    if (branchName == 'develop') {
        addArchiveArtefacts(buildJob, 'rcp/org.testeditor.rcp4.product/target/products/*.zip')
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
            jacoco()
        }
    }
}

/**
 * Defines how a default build job should look like.
 */
FreeStyleJob defaultBuildJob(String jobName, String repo, String branchName, Closure closure) {
    FreeStyleJob buildJob = job(jobName)
    buildJob.with {
        jdk(Globals.jdk8)
        description """Performs a build on branch: <a href="https://github.com/test-editor/$repo/tree/$branchName">$branchName</a>."""
        scm {
            git {
                remote {
                    url("git@github.com:test-editor/${repo}.git")
                    branch("*/$branchName")
                }
                createTag(false)
                localBranch(branchName)
            }
        }
    }
    if (closure) {
        closure(buildJob)
    }
    return buildJob
}