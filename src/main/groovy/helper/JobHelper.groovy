package helper

import javaposse.jobdsl.dsl.jobs.FreeStyleJob
import javaposse.jobdsl.dsl.views.ListView

class JobHelper {

    /**
     *  Adds JDK, default discription and optional closure to given job.
     */
    static void addDefaultConfiguration(FreeStyleJob job, String branch, Closure closure) {
        job.with {
            jdk(Globals.jdk)
            description "Performs a build on branch: $branch"
        }

        if (closure) {
            closure(job)
        }
    }

    /**
     *  Adds Git repo to given job.
     */
    static void addTEGitRepo(FreeStyleJob job, String repo, String branch) {
        job.with {
            scm {
                git("git@github.com:test-editor/${repo}.git", branch, null)
            }
        }
        addGitConfigureSkipTag(job, true)
        addGitConfigureBranch(job, branch)
    }

    /**
     * Adds Git branch configuration section.
     */
    static void addGitConfigureBranch(FreeStyleJob job, branchName) {
        job.with {
            configure { project ->
                // checkout to local branch
                project / 'scm' / 'extensions' / 'hudson.plugins.git.extensions.impl.LocalBranch' / localBranch(branchName)
            }
        }
    }

    /**
     * Adds Git skipping tag creation configuration section.
     */
    static void addGitConfigureSkipTag(FreeStyleJob job, skippingTag) {
        job.with {
            configure { project ->
                // no default tagging
                project / 'scm' / 'skipTag'(skippingTag)
            }
        }
    }

    /**
     * Adds Git submodules configuration section.
     */
    static void addGitConfigureSubmodules(FreeStyleJob job, boolean disable, boolean recursive, boolean tracking) {
        job.with {
            configure { project ->
                project / 'scm' / 'extensions' / 'hudson.plugins.git.extensions.impl.SubmoduleOption' {
                    'disableSubmodules'(disable)
                    'recursiveSubmodules'(recursive)
                    'trackingSubmodules'(tracking)
                }
            }
        }
    }

    /**
     * FitNesse plugin configuration section.
     */
    static void fitNesseConfigure(FreeStyleJob job, path) {
        job.with {
            configure { project ->
                project / 'publishers' / 'hudson.plugins.fitnesse.FitnesseResultsRecorder' {
                    'fitnessePathToXmlResultsIn'(path)
                }
            }
        }
    }

    /**
     * Adds github push trigger to given job
     */
    static void addGithubPush(FreeStyleJob job) {
        job.with {
            triggers {
                githubPush()
            }
        }
    }

    /**
     * Adds QA publishers to given job
     */
    static void addQAPublishers(FreeStyleJob job) {
        job.with {
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
    static void addExtendedQAPublishers(FreeStyleJob job) {
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
    static void addArchiveArtefacts(FreeStyleJob job, String artefact) {
        job.with {
            publishers {
                archiveArtifacts(artefact)
            }
        }
    }

    /**
     * Creates job name for build jobs.
     */
    static def createJobName(String repo, String branch) {
        return "${repo}_${branch}_CI".replaceAll('/', '_')
    }

    /**
     * Adds job to given view.
     */
    static void addJob2View(ListView view, String jobName) {
        view.with {
            jobs {
                name(jobName)
            }
        }
    }

    /**
     * Adds the xvfb start to build job
     */
    static void addXvfbStart(FreeStyleJob job) {
        job.with {
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
    static void addPreBuildCleanup(FreeStyleJob job) {
        job.with {
            wrappers {
                preBuildCleanup()
            }
        }
    }

    /**
     * Adds post build trigger for project
     */
    static void addTriggerBuildOnProject(FreeStyleJob job, String projectName) {
        job.with {
            publishers {
                downstream(projectName, 'SUCCESS')
            }
        }
    }

    /**
     * Limits build number to archive
     */
    static void limitBuildsTo(FreeStyleJob job, int count) {
        job.with {
            logRotator {
                numToKeep(count)
            }
        }
    }

    /**
     * Vagrant configuration section.
     */
    static void addVagrantConfigure(FreeStyleJob job, String vagrantFilePath, String launchCommand) {
        job.with {
            configure { project ->
                project / builders / 'org.jenkinsci.plugins.vagrant.VagrantDestroyCommand' / 'wrapper' {
                    'vagrantFile'(vagrantFilePath)
                }
                project / builders / 'org.jenkinsci.plugins.vagrant.VagrantUpCommand' / 'wrapper' {
                    'vagrantFile'(vagrantFilePath)
                }
                project / builders / 'org.jenkinsci.plugins.vagrant.VagrantSshCommand' {
                    'command'(launchCommand)
                    'wrapper' {
                        'vagrantFile'(vagrantFilePath)
                    }
                }
                project / builders << 'org.jenkinsci.plugins.vagrant.VagrantDestroyCommand' {
                    'wrapper' {
                        'vagrantFile'(vagrantFilePath)
                    }
                }
            }
        }
    }
}

