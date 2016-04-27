package helper

import groovy.json.JsonSlurper

class JobHelperMock extends JobHelper {

    static Object getBranches(String repo) {
        def result = """
            [
              {
                "name": "develop",
                "commit": {
                  "sha": "xxx",
                  "url": "https://api.github.com/repos/test-editor/test-editor-xtext/commits/xxx"
                }
              },
              {
                "name": "feature/myBranch",
                "commit": {
                  "sha": "yyy",
                  "url": "https://api.github.com/repos/test-editor/test-editor-xtext/commits/yyy"
                }
              },
              {
                "name": "master",
                "commit": {
                  "sha": "zzz",
                  "url": "https://api.github.com/repos/test-editor/test-editor-xtext/commits/zzz"
                }
              }
            ]
        """
        return new JsonSlurper().parseText(result)
    }

    /**
     * Mocks the jacoco() call on a list view.
     */
    static void jacoco() {
    }

}
