package st.amnesty

import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.ObjectMetadata
import com.amazonaws.services.s3.model.S3Object
import com.amazonaws.services.s3.model.S3ObjectInputStream
import com.fasterxml.jackson.databind.ObjectMapper
import io.netty.handler.codec.http.HttpHeaderNames
import io.netty.handler.codec.http.HttpHeaderValues
import org.apache.http.client.methods.HttpRequestBase
import ratpack.http.HttpMethod
import ratpack.http.client.ReceivedResponse
import ratpack.test.http.TestHttpClient
import spock.lang.Specification
import st.amnesty.fixtures.AmnestyApplicationUnderTest
import st.amnesty.ratelimit.RateLimitOverride
import st.amnesty.ratelimit.S3ClientProvider
import st.fixture.NoOpTokenValidator
import st.ratpack.auth.ValidateTokenResult
import st.ratpack.auth.internal.DefaultOAuthToken

class RateLimitIntegrationSpec extends Specification {

	S3ClientProvider mockS3ClientProvider = Mock()
	AmazonS3 s3Client = Mock()
	S3Object s3Object = Mock()
	S3ObjectInputStream s3ObjectInputStream
	ObjectMetadata metadata = Mock()

	private aut = new AmnestyApplicationUnderTest.Builder()
		.s3ClientProvider(mockS3ClientProvider)
		.tokenValidator(new NoOpTokenValidator({ token ->
		String scope = token.contains('service') ? 'service' : 'w:service:rate:overrides:*'
		return ValidateTokenResult.valid(new DefaultOAuthToken('faketoken', 'fake client', [scope] as Set<String>, [:]))
	}))
		.build()

	private TestHttpClient httpClient = aut.getHttpClient((HttpHeaderNames.AUTHORIZATION): "Bearer service")
	private TestHttpClient writeOverridesScopeClient = aut.getHttpClient((HttpHeaderNames.AUTHORIZATION): "Bearer fineGrained")

	def setup() {
		RateLimitOverride rateLimitOverride = new RateLimitOverride()
		rateLimitOverride.setHandlerId("ours")
		rateLimitOverride.setPrincipalId("yours")
		rateLimitOverride.setHandlerId("mine")
		Set<RateLimitOverride> rateLimitOverrideSet = new HashSet<>(Arrays.asList(rateLimitOverride))
		ObjectMapper objectMapper = new ObjectMapper()
		byte[] bytes = objectMapper.writeValueAsBytes(rateLimitOverrideSet)
		InputStream inputStream = new ByteArrayInputStream(bytes)
		s3ObjectInputStream = new S3ObjectInputStream(inputStream, new HttpRequestBase() {
			@Override
			String getMethod() {
				return null
			}
		})
		0 * _
	}

	void 'post returns status 204 with S3 mock'() {
		when:
		final response = writeOverridesScopeClient.request("/overrides/elder", { spec ->
			spec.method(HttpMethod.POST)
			spec.headers.set(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON)
			spec.body.text("{\"rateLimitOverrideSet\" :[{\"principalId\":\"ours\",\"handlerId\":\"me\",\"bucketId\":\"placejdfas\",\"rateLimit\": 78}]}")
		})

		then:
		response.statusCode == 204

		and:
		1 * mockS3ClientProvider.get() >> s3Client
		1 * s3Client.putObject(_) >> null
		1 * s3Client.doesBucketExistV2('amnesty-ratelimit') >> true
	}

	void 'get returns status 200 with mocked S3'() {
		when:
		ReceivedResponse response = httpClient.get("/overrides/elder")

		then:
		response.statusCode == 200
		response.headers.get(HttpHeaderNames.ETAG) == "e5gd8bxnbzu78ns899sns7"

		and:
		1 * mockS3ClientProvider.get() >> s3Client
		1 * s3Client.doesBucketExistV2('amnesty-ratelimit') >> true
		1 * s3Client.doesObjectExist('amnesty-ratelimit', 'elder.json') >> true
		1 * s3Client.getObject(_) >> s3Object
		1 * s3Object.getObjectContent() >> s3ObjectInputStream
		1 * s3Object.getObjectMetadata() >> metadata
		1 * metadata.ETag >> "e5gd8bxnbzu78ns899sns7"
	}

	void 'get with matching eTag returns status 304 with mocked S3'() {
		when:
		ReceivedResponse response = httpClient.request("/overrides/elder", { requestSpec ->
			requestSpec.method("GET")
			requestSpec.headers.set(HttpHeaderNames.IF_NONE_MATCH, "e5gd8bxnbzu78ns899sns7")
		})

		then:
		response.statusCode == 304

		and:
		1 * mockS3ClientProvider.get() >> s3Client
		1 * s3Client.doesBucketExistV2('amnesty-ratelimit') >> true
		1 * s3Client.doesObjectExist('amnesty-ratelimit', 'elder.json') >> true
		1 * s3Client.getObject(_) >> s3Object
		1 * s3Object.getObjectContent() >> s3ObjectInputStream
		1 * s3Object.getObjectMetadata() >> metadata
		1 * metadata.ETag >> "e5gd8bxnbzu78ns899sns7"
	}

	void 'delete returns status 204 with mocked S3'() {
		when:
		ReceivedResponse response = writeOverridesScopeClient.delete("/overrides/elder")

		then:
		response.statusCode == 204

		and:
		1 * mockS3ClientProvider.get() >> s3Client
		1 * s3Client.deleteObject(_) >> null
		1 * s3Client.doesBucketExistV2('amnesty-ratelimit') >> true
		1 * s3Client.doesObjectExist('amnesty-ratelimit', 'elder.json') >> true
	}

	void 'post returns status 422 with S3 mock'() {
		when:
		ReceivedResponse response = writeOverridesScopeClient.request("/overrides/elder", { requestSpec ->
			requestSpec.method("POST")
			requestSpec.body.type("application/json")
			requestSpec.body.text("{\"rateLimitOverriet\" :[{\"principalId\":\"ours\",\"handlerId\":\"me\",\"bucketId\":\"placejdfas\",\"rateLimit\": 78}]}")
		})

		then:
		response.statusCode == 422

		and:
		1 * mockS3ClientProvider.get() >> s3Client
	}
}
