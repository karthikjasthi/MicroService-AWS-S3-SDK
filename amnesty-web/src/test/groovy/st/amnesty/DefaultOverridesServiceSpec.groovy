package st.amnesty

import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.ObjectMetadata
import com.amazonaws.services.s3.model.S3Object
import com.amazonaws.services.s3.model.S3ObjectInputStream
import com.fasterxml.jackson.databind.ObjectMapper
import ratpack.exec.ExecResult
import ratpack.test.exec.ExecHarness
import spock.lang.AutoCleanup
import spock.lang.Specification
import spock.lang.Subject
import st.amnesty.modules.AmnestyModule
import st.amnesty.ratelimit.DefaultOverridesService
import st.amnesty.ratelimit.OverrideETag
import st.amnesty.ratelimit.RateLimitOverride

/**
 * Unit tests against DefaultOverridesService class.
 */
class DefaultOverridesServiceSpec extends Specification {

	AmazonS3 s3Client = Mock()
	AmnestyModule.Config config = new AmnestyModule.Config(s3: new AmnestyModule.S3Config(bucketName: 'amnesty-ratelimit'))
	ObjectMapper objectMapper = Mock()
	@Subject
	DefaultOverridesService overridesService = new DefaultOverridesService(s3Client, config, objectMapper)

	@AutoCleanup
	ExecHarness execHarness = ExecHarness.harness()

	def setup() {
		0 * _
	}

	def "Read Objects from S3"() {
		given:
		S3ObjectInputStream inputStream = Mock()
		S3Object s3Object = Mock()
		ObjectMetadata objectMetadata = Mock()

		when:
		OverrideETag overrideETag = execHarness.yield {
			overridesService.getOverrides("platform")
		}.valueOrThrow

		then:
		overrideETag.rateLimitOverrideSet.size() == 1
		overrideETag.getETag() == "etag"

		and:
		1 * s3Object.getObjectContent() >> inputStream
		1 * inputStream.close()
		1 * s3Object.getObjectMetadata() >> objectMetadata
		1 * s3Client.doesBucketExistV2('amnesty-ratelimit') >> true
		1 * s3Client.doesObjectExist('amnesty-ratelimit', 'platform.json') >> true
		1 * s3Client.getObject(_) >> s3Object
		1 * objectMetadata.getETag() >> "etag"
		1 * objectMapper.readValue(inputStream, _) >> Collections.singleton(new RateLimitOverride())
	}

	def "Write Objects to S3"() {
		given:
		RateLimitOverride override = new RateLimitOverride()
		override.setBucketId("6a37f9db-d5a1-49a2-a7e3-d9366e44ce10")
		override.setPrincipalId("cd86e06d-5645-4da7-817f-cd7e22b513a0")
		override.setHandlerId("CAPABILITY_GET")
		Set<RateLimitOverride> overrideSet = Collections.singleton(override)

		when:
		ExecResult execResult = execHarness.yield({ overridesService.storeOverrides("platform", overrideSet) })

		then:
		execResult.success

		and:
		1 * s3Client.doesBucketExistV2('amnesty-ratelimit') >> true
		1 * s3Client.putObject(_) >> null
	}

	def "Create Bucket on S3 if it does not already exist"() {
		setup:
		RateLimitOverride override = new RateLimitOverride()
		override.setBucketId("6a37f9db-d5a1-49a2-a7e3-d9366e44ce10")
		override.setPrincipalId("cd86e06d-5645-4da7-817f-cd7e22b513a0")
		override.setHandlerId("CAPABILITY_GET")
		Set<RateLimitOverride> overrideSet = Collections.singleton(override)

		when:
		ExecResult execResult = execHarness.yield({ overridesService.storeOverrides("platform", overrideSet) })

		then:
		execResult.success

		and:
		1 * s3Client.putObject(_)
		1 * s3Client.doesBucketExistV2('amnesty-ratelimit') >> false
		1 * s3Client.createBucket('amnesty-ratelimit')

	}

	def "Return null when S3 bucket does not exist"() {
		when:
		OverrideETag overrideETag = execHarness.yield {
			overridesService.getOverrides("platform")
		}.valueOrThrow

		then:
		overrideETag.ETag == null
		overrideETag.rateLimitOverrideSet.empty

		and:
		s3Client.doesBucketExistV2('amnesty-ratelimit') >> false
	}
}
