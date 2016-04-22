import spock.lang.Specification
import javaposse.jobdsl.dsl.DslScriptLoader
import javaposse.jobdsl.dsl.JobManagement
import javaposse.jobdsl.dsl.MemoryJobManagement

class JobSpec extends Specification {

    def 'jobExecutionWorks'() {
        given:
        JobManagement jm = new MemoryJobManagement()
        String script = new File('jobs/jobs.groovy').text

        when:
        DslScriptLoader.runDslEngine(script, jm)

        then:
        noExceptionThrown()
    }

}
