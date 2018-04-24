package st.amnesty

import ratpack.test.http.TestHttpClient
import spock.lang.Specification
import st.amnesty.fixtures.AmnestyApplicationUnderTest

class HealthCheckSpec extends Specification {

	AmnestyApplicationUnderTest aut = new AmnestyApplicationUnderTest.Builder().build()
	TestHttpClient client = aut.httpClient

	def 'health check'() {
		expect:
		client.get('/health').statusCode == 200
	}
}
