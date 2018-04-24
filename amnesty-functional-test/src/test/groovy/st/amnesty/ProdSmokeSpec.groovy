package st.amnesty

import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import smartthings.test.common.mdc.SpecMDC
import smartthings.test.common.retry.RetryOnFailure
import smartthings.test.common.spec.FunctionalTestSpecification
import smartthings.test.common.tags.ProdSmoke
import smartthings.testkit.service.RetrofitServiceBuilder
import smartthings.testkit.util.ConverterType
import spock.lang.Shared
import v20180126.internal.st.amnesty.AmnestyApi
import v20180126.internal.st.amnesty.OverrideETag
import v20180126.internal.st.amnesty.RateLimitOverride

import static st.amnesty.setup.utils.RetrofitUtils.interceptor

@ProdSmoke
@SpecMDC
@RetryOnFailure(times = 2)
class ProdSmokeSpec extends FunctionalTestSpecification {
	@Shared
	String amnestyUrl = getServiceUrl("http://localhost:8233/")

	@Shared
	String authorizationHeader

	@Shared
		serviceName = UUID.randomUUID().toString()

	@Shared
	AmnestyApi amnestyApi

	void setupSpec() {
		authorizationHeader = "Bearer ${getSetting("serviceToken", "NoTokenProvided")}"

		amnestyApi = new RetrofitServiceBuilder<AmnestyApi>()
			.withServiceClass(AmnestyApi)
			.withConverterType(ConverterType.JSON)
			.withUrl(amnestyUrl)
			.withLoggingLevel(HttpLoggingInterceptor.Level.BODY)
			.withInterceptor(interceptor("application/vnd.smartthings+json;v=1"))
			.build()
	}

	def 'Should only run this spec when tag.prodsmoke is set to true'() {
		expect:
		Boolean.getBoolean('tag.prodsmoke')
	}

	void 'POSTing a new override returns a 204 status code'() {
		given:
		RateLimitOverride rateLimitOverride = new RateLimitOverride(
			principalId: "ours",
			handlerId: "me",
			bucketId: "yours",
			rateLimit: 78
		)
		OverrideETag overrideETag = new OverrideETag().addRateLimitOverrideSetItem(rateLimitOverride)

		when:
		Response response = amnestyApi.createRateLimitOverride(authorizationHeader, serviceName, overrideETag).execute()

		then:
		response.code() == 204

		cleanup:
		amnestyApi.deleteOverrides(authorizationHeader, serviceName).execute()
	}

	void 'GETting a service returns an empty list'() {
		when:
		Response response = amnestyApi.getRateLimitOverrides(authorizationHeader, serviceName, "").execute()

		then:
		response.code() == 200
		response.body().toString() == "[]"

	}

	void 'DELETing an override return a 204 status code'() {
		given:
		RateLimitOverride rateLimitOverride = new RateLimitOverride(
			principalId: "ours",
			handlerId: "me",
			bucketId: "yours",
			rateLimit: 78
		)
		OverrideETag overrideETag = new OverrideETag().addRateLimitOverrideSetItem(rateLimitOverride)
		amnestyApi.createRateLimitOverride(authorizationHeader, serviceName, overrideETag).execute()

		when:
		Response response = amnestyApi.deleteOverrides(authorizationHeader, serviceName).execute()

		then:
		response.code() == 204

	}

}
