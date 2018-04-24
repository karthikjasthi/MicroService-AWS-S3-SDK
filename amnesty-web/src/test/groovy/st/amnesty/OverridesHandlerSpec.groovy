package st.amnesty

import com.amazonaws.HttpMethod
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.common.reflect.TypeToken
import io.netty.handler.codec.http.HttpHeaderNames
import ratpack.error.ServerErrorHandler
import ratpack.exec.Promise
import ratpack.http.MediaType
import ratpack.jackson.internal.DefaultJsonRender
import ratpack.registry.RegistrySpec
import ratpack.test.handling.HandlingResult
import ratpack.test.handling.RequestFixture
import smartthings.common.api.ratpack.ApiErrorHandler
import smartthings.common.api.ratpack.JsonParser
import spock.lang.Specification
import spock.lang.Subject
import st.amnesty.ratelimit.DefaultOverridesService
import st.amnesty.ratelimit.OverrideETag
import st.amnesty.ratelimit.OverridesHandler
import st.amnesty.ratelimit.RateLimitOverride

import javax.validation.Validation
import javax.validation.Validator

class OverridesHandlerSpec extends Specification {

	DefaultOverridesService overridesService = Mock()
	Validator validator = Validation.buildDefaultValidatorFactory().getValidator()
	@Subject
	OverridesHandler handler = new OverridesHandler(overridesService, validator)
	ObjectMapper objectMapper = new ObjectMapper()
	String serviceName = 'test'
	String eTag = "eTag"
	Set overrideSet

	def setup() {
		RateLimitOverride test1 = new RateLimitOverride("test1", "test2", "test3", 8)
		RateLimitOverride test2 = new RateLimitOverride("test4", "test5", "test6", 12)
		overrideSet = [test1, test2]

		0 * _
	}

	def "get request with matching eTag returns 304"() {
		given:
		OverrideETag overrideETag = new OverrideETag(overrideSet, eTag)

		when:
		HandlingResult result = RequestFixture.handle(handler, { fixture ->
			fixture.header(HttpHeaderNames.IF_NONE_MATCH, eTag)
			fixture.pathBinding([serviceName: serviceName])
			fixture.method("GET")
		})

		then:
		result.status.code == 304
		result.rendered(DefaultJsonRender.class) == null

		and:
		1 * overridesService.getOverrides(serviceName) >> Promise.value(overrideETag)
	}


	def "get request with no eTag and correct service name returns 200 and overrides json"() {
		given:
		OverrideETag overrideETag = new OverrideETag(overrideSet, eTag)

		when:
		HandlingResult result = RequestFixture.handle(handler, { fixture ->
			fixture.pathBinding([serviceName: serviceName])
			fixture.header(HttpHeaderNames.ACCEPT, MediaType.APPLICATION_JSON)
			fixture.method(HttpMethod.GET.toString())
		})

		then:
		result.status.code == 200
		result.headers.get(HttpHeaderNames.ETAG) == eTag
		Set resultBody = result.rendered(DefaultJsonRender).getObject()
		resultBody[0]["principalId"] == "test1"
		resultBody[1]["handlerId"] == "test5"

		and:
		1 * overridesService.getOverrides(serviceName) >> Promise.value(overrideETag)
	}

	def "get request with un-matched service name returns 200"() {
		when:
		HandlingResult result = RequestFixture.handle(handler, { fixture ->
			fixture.pathBinding([serviceName: "nothing"])
			fixture.method(HttpMethod.GET.toString())
		})

		then:
		result.status.code == 200

		and:
		1 * overridesService.getOverrides("nothing") >> Promise.value(new OverrideETag(new HashSet<RateLimitOverride>(), null))
	}

	def "post request properly formed returns 204"() {
		when:
		RateLimitOverride testOverride = new RateLimitOverride("ghaskj;a67hj", "me", "hit", 7)
		OverrideETag overrideETag = new OverrideETag(new HashSet<RateLimitOverride>(Collections.singletonList(testOverride)), null)
		overrideETag.rateLimitOverrideSet.add(testOverride)
		HandlingResult result = RequestFixture.handle(handler, { fixture ->
			fixture.pathBinding(serviceName: serviceName)
			fixture.header(HttpHeaderNames.ACCEPT, MediaType.APPLICATION_JSON)
			fixture.body(objectMapper.writeValueAsString(overrideETag), MediaType.APPLICATION_JSON)
			fixture.method(HttpMethod.POST.toString())
		})

		then:
		result.status.code == 204

		and:
		1 * overridesService.storeOverrides(serviceName, _) >> Promise.value(null)
	}

	def "post request with malformed body returns 422"() {
		when:
		HandlingResult result = RequestFixture.handle(handler, { fixture ->
			fixture.pathBinding(serviceName: serviceName)
			fixture.header(HttpHeaderNames.ACCEPT, MediaType.APPLICATION_JSON)
			fixture.body("{\"rateLimitOverrideSet\" : [\n" +
				"    {\n" +
				"        \"principalId\": \"ours\",\n" +
				"        \"handlerId\": \"me\",\n" +
				"        \"bucketId\": \"placejdfas\",\n" +
				"        \"rateLiit\": 10\n" +
				"    }\n" +
				"\t]\n" +
				"}", MediaType.APPLICATION_JSON)
			fixture.method(HttpMethod.POST.toString())
			fixture.registry { RegistrySpec spec ->
				spec.add(ServerErrorHandler.class, new ApiErrorHandler())
				spec.add(ObjectMapper, objectMapper)
				spec.add(new JsonParser(objectMapper) {
					@Override
					protected TypeToken getType() {
						return TypeToken.of(SmartThingsApiModel.class)
					}
				})
			}
		})

		then:
		result.status.code == 422
	}

	def "post request with improper type arguments returns 422"() {
		when:
		HandlingResult result = RequestFixture.handle(handler, { fixture ->
			fixture.pathBinding(serviceName: serviceName)
			fixture.header(HttpHeaderNames.ACCEPT, MediaType.APPLICATION_JSON)
			fixture.body("{\"rateLimitOverrideSet\" : [\n" +
				"    {\n" +
				"        \"principalId\": \"ours\",\n" +
				"        \"handlerId\": \"me\",\n" +
				"        \"bucketId\": \"placejdfas\",\n" +
				"        \"rateLiit\": jim\n" +
				"    }\n" +
				"\t]\n" +
				"}", MediaType.APPLICATION_JSON)
			fixture.method(HttpMethod.POST.toString())
			fixture.registry { RegistrySpec spec ->
				spec.add(ServerErrorHandler.class, new ApiErrorHandler())
				spec.add(ObjectMapper, objectMapper)
				spec.add(new JsonParser(objectMapper) {
					@Override
					protected TypeToken getType() {
						return TypeToken.of(SmartThingsApiModel.class)
					}
				})
			}
		})
		then:
		result.status.code == 422
	}

	def "delete request with found service name returns 204"() {
		when:
		HandlingResult result = RequestFixture.handle(handler, { fixture ->
			fixture.pathBinding(serviceName: serviceName)
			fixture.method(HttpMethod.DELETE.toString())
		})
		then:
		result.status.code == 204

		and:
		1 * overridesService.deleteOverrides(serviceName) >> Promise.value(true)
	}

	def "delete request with not found service name returns 404"() {
		when:
		HandlingResult result = RequestFixture.handle(handler, { fixture ->
			fixture.pathBinding(serviceName: serviceName)
			fixture.method(HttpMethod.DELETE.toString())
		})
		then:
		result.status.code == 404

		and:
		1 * overridesService.deleteOverrides(serviceName) >> Promise.value(false)
	}
}
