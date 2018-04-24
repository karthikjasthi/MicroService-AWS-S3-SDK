package st.amnesty

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import smartthings.test.common.spec.FunctionalTestSpecification
import spock.lang.Shared

class HealthCheckFunctionalSpec extends FunctionalTestSpecification {

	@Shared
	String amnestyUrl = getSetting('amnestyUrl', getServiceUrl("http://localhost:8233"))

	@Shared
	OkHttpClient client

	void "it should have a healthy health check"() {
		setup:
		client = new OkHttpClient()
		Request request = new Request.Builder().get().url("${amnestyUrl}/health").build()

		when:
		Response response = client.newCall(request).execute()

		then:
		response.code() == 200
	}

	void "it should have reachable build info"() {
		setup:
		client = new OkHttpClient()
		Request request = new Request.Builder().get().url("${amnestyUrl}/buildinfo").build()

		when:
		Response response = client.newCall(request).execute()

		then:
		response.code() == 200
	}
}
