package st.amnesty.endpoints

import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import smartthings.test.common.retry.RetryOnFailure
import smartthings.test.common.spec.FunctionalTestSpecification
import smartthings.testkit.service.RetrofitServiceBuilder
import smartthings.testkit.util.ConverterType
import smartthings.testkit.util.Environment
import smartthings.testkit.util.Urls
import spock.lang.Shared
import st.amnesty.setup.OAuthSetup
import st.amnesty.setup.model.AuthenticatedUser
import v20180126.internal.st.amnesty.AmnestyApi
import v20180126.internal.st.amnesty.OverrideETag
import v20180126.internal.st.amnesty.RateLimitOverride

import static st.amnesty.setup.utils.RetrofitUtils.interceptor

@RetryOnFailure(times = 2)
class RateLimitFunctionalSpec extends FunctionalTestSpecification {

	@Shared
	String amnestyUrl = getServiceUrl("http://localhost:8233")

	@Shared
	String bearerServiceToken

	@Shared
	String fineGrainedBearerServiceToken

	@Shared
	AmnestyApi amnestyApi

	@Shared
	AuthenticatedUser user

	private final static String SERVICENAME = UUID.randomUUID().toString()

	void setupSpec() {
		Urls urls = new Urls(Environment.fromString(getEnvironment()))
		OAuthSetup oAuthSetup = new OAuthSetup(urls.authService)
		user = oAuthSetup.createAuthenticatedUser().toBlocking().first()
		bearerServiceToken = oAuthSetup.createServiceToken()
		fineGrainedBearerServiceToken = oAuthSetup.createFineGrainedServiceToken()

		amnestyApi = new RetrofitServiceBuilder<AmnestyApi>()
			.withServiceClass(AmnestyApi)
			.withConverterType(ConverterType.JSON)
			.withUrl(amnestyUrl)
			.withLoggingLevel(HttpLoggingInterceptor.Level.BODY)
			.withInterceptor(interceptor("application/vnd.smartthings+json;v=1"))
			.build()
	}

	void "post, get, get with eTag, delete overrides on S3"() {
		when: "this posts JSON Object to S3, expected status 204"
		RateLimitOverride rateLimitOverride = new RateLimitOverride(
			principalId: "ours",
			handlerId: "me",
			bucketId: "yours",
			rateLimit: 78
		)
		OverrideETag overrideETag = new OverrideETag().addRateLimitOverrideSetItem(rateLimitOverride)
		Response response = amnestyApi.createRateLimitOverride(fineGrainedBearerServiceToken, SERVICENAME, overrideETag).execute()

		then:
		response.code() == 204

		when: "this should get a JSON Object from S3, expected status 200"
		response = amnestyApi.getRateLimitOverrides(bearerServiceToken, SERVICENAME, null).execute()
		String eTag = response.headers().get("etag")

		then:
		response.code() == 200
		response.body() != null

		when: "this sends eTag and expects status 304"
		response = amnestyApi.getRateLimitOverrides(bearerServiceToken, SERVICENAME, eTag).execute()

		then:
		response.code() == 304

		when: "this should delete a JSON Object on S3, expected status 204"
		response = amnestyApi.deleteOverrides(fineGrainedBearerServiceToken, SERVICENAME).execute()

		then:
		response.code() == 204
	}

	void "post No Bearer Token and expects status 401"() {
		when:
		OverrideETag overrideETag = new OverrideETag()
		Response response = amnestyApi.createRateLimitOverride(null, SERVICENAME, overrideETag).execute()

		then:
		response.code() == 401
	}

	void "Valid Mobile Token expects status 403"() {
		when:
		OverrideETag overrideETag = new OverrideETag()
		bearerServiceToken = "Bearer ${user.token.accessToken}"
		Response response = amnestyApi.createRateLimitOverride(bearerServiceToken, SERVICENAME, overrideETag).execute()

		then:
		response.code() == 403
	}

	void "Invalid Token expects status 401"() {
		when:
		OverrideETag overrideETag = new OverrideETag()
		bearerServiceToken = "Bearer ${UUID.randomUUID()}"
		Response response = amnestyApi.createRateLimitOverride(bearerServiceToken, SERVICENAME, overrideETag).execute()

		then:
		response.code() == 401
	}
}
