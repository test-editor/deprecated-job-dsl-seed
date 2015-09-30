githuburl = 'https://github.com/test-editor/core-fixture.git'

// Create jobs for static branches
['feature/te-195'].each { branch ->

    // define jobs
    def buildJob = defaultBuildJob(branch, true)
}

/**
 * Defines how a default build job should look like.
 */
def defaultBuildJob(String branch, boolean clean) {
    def jobName = "${branch}_build".replaceAll('/', '_')
    def buildJob = job(jobName) {
        description "Performs a build on branch: $branch"
        scm {
              git (
                  githuburl,
                  branch,
                  gitConfigure(branch, true)
              )
        }
        triggers {
            scm 'H/3 * * * *'
        }
        wrappers {
          release {
    				preBuildSteps {
    					maven {
    						mavenInstallation("Maven 3.2.2")
    						rootPOM("pom.xml")
    						goals("build-helper:parse-version")
    						goals("versions:set")
    						property("newVersion", "\${parsedVersion.majorVersion}.\${parsedVersion.minorVersion}.\${parsedVersion.incrementalVersion}-\${BUILD_NUMBER}")
    					}
    				}
    				postSuccessfulBuildSteps {
    					maven {
    						rootPOM("pom.xml")
    						goals("deploy")
    					}
    					maven {
    						goals("scm:tag")
    					}
    					downstreamParameterized {
    						trigger("deploy-application") {
                  parameters {
                    predefinedProp("STAGE", "development")
                  }
    						}
    					}
    				}
    			}
        }
        steps {
            maven {
                goals('clean')
                goals('package')
                goals('checkstyle:checkstyle')
                mavenInstallation('Maven 3.2.2')
            }
        }
        publishers {
            groovyPostBuild("manager.addShortText(manager.build.getEnvironment(manager.listener)[\'POM_VERSION\'])")
            jacocoCodeCoverage {
                execPattern '**/**.exec'
            }
            archiveJunit '**/app/**/build/test-results/*.xml'
        }
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
