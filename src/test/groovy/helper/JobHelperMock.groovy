package helper

import groovy.json.JsonSlurper

class JobHelperMock {

    static Object getMockedBranches(String repo) {
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

}
