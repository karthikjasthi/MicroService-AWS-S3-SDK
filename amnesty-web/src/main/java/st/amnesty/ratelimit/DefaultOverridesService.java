package st.amnesty.ratelimit;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ratpack.exec.Blocking;
import ratpack.exec.Promise;
import smartthings.logging.KVMap;
import smartthings.logging.slf4j.KVLogger;
import st.amnesty.modules.AmnestyModule;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

/**
 * This Implementation class interacts with AWS S3 to perform get and set operations achieving RateLimit.
 */
public class DefaultOverridesService implements OverridesService {

	private static final Logger LOG = LoggerFactory.getLogger(DefaultOverridesService.class);
	private static final String KEY_SUFFIX = ".json";
	private AmazonS3 s3Client;
	private static final TypeReference<Set<RateLimitOverride>> typeReference =
		new TypeReference<Set<RateLimitOverride>>() {
		};
	private final AmnestyModule.S3Config s3Config;
	private final String bucketName;
	private final ObjectMapper objectMapper;

	@Inject
	public DefaultOverridesService(AmazonS3 s3Client, AmnestyModule.Config config, ObjectMapper objectMapper) {
		this.s3Client = s3Client;
		this.s3Config = config.getS3();
		this.bucketName = s3Config.getBucketName();
		this.objectMapper = objectMapper;
	}

	/**
	 * Gets objects from S3.
	 *
	 * @param serviceName - Name of service
	 * @return overrideEtag - set of Overrides and its Hash (ETag)
	 * @throws IOException - S3 connectivity exception
	 */
	@Override
	public Promise<OverrideETag> getOverrides(String serviceName) {
		//Suffix service for fully qualified file name
		String key = serviceName.concat(KEY_SUFFIX);
		//Constructing Object Request with headers
		GetObjectRequest request = new GetObjectRequest(bucketName, key);
		ResponseHeaderOverrides responseHeaders = new ResponseHeaderOverrides();
		responseHeaders.setCacheControl("No-Cache");
		request.setResponseHeaders(responseHeaders);

		return withExceptionLogging(Blocking.get(() -> {
				if (s3Client.doesBucketExistV2(bucketName) && s3Client.doesObjectExist(bucketName, key)) {
					//Retrieve the Data object
					S3Object s3Object = s3Client.getObject(request);
					//Get Object Metadata to access eTag
					ObjectMetadata metadata = s3Object.getObjectMetadata();
					//Read Object content into Input Stream
					try (InputStream is = s3Object.getObjectContent()) {
						//Parse downloaded JSON file
						Set<RateLimitOverride> rateLimitOverrideSet = objectMapper.readValue(is, typeReference);

						OverrideETag overrideETag = new OverrideETag();
						//Parse InputStream to a Set
						overrideETag.setRateLimitOverrideSet(rateLimitOverrideSet);
						//Set eTag retrieved from S3
						overrideETag.setETag(metadata.getETag());
						return overrideETag;
					}
				} else {
					//Return empty Overrides if bucket does not exist
					return new OverrideETag(new HashSet<>(), null);
				}
			}
		));
	}

	/**
	 * Add Json files with Overrides to S3.
	 *
	 * @param serviceName
	 * @param rateLimitOverrides
	 * @throws IOException
	 */
	public Promise<Void> storeOverrides(String serviceName, Set<RateLimitOverride> rateLimitOverrides) {
		return withExceptionLogging(Blocking.get(() -> {
				//Create bucket if it doesn't exist
				if (!s3Client.doesBucketExistV2(bucketName)) {
					s3Client.createBucket(bucketName);
				}

				//Parse Set to InputStream
				ObjectMapper objectMapper = new ObjectMapper();
				byte[] bytes = objectMapper.writeValueAsBytes(rateLimitOverrides);
				InputStream is = new ByteArrayInputStream(bytes);

				//Construct PutObject
				PutObjectRequest request = new PutObjectRequest(bucketName, serviceName.concat(KEY_SUFFIX),
					is, new ObjectMetadata());

				//Store to S3
				s3Client.putObject(request);
				return null;
			}
		));
	}

	@Override
	public Promise<Boolean> deleteOverrides(String serviceName) {
		//Suffix service for fully qualified file name
		String key = serviceName.concat(KEY_SUFFIX);
		//Constructing Object Request with headers
		DeleteObjectRequest request = new DeleteObjectRequest(bucketName, key);
		return withExceptionLogging(Blocking.get(() -> {
				if (s3Client.doesBucketExistV2(bucketName) && s3Client.doesObjectExist(bucketName, key)) {
					//Deleting the Data object
					s3Client.deleteObject(request);
					return true;
				}
				return false;
			}
		));
	}

	private static <T> Promise<T> withExceptionLogging(Promise<T> promise) {
		return promise.onError(AmazonServiceException.class, ase -> KVLogger.error(LOG, "amazon-service-exception",
			KVMap.of(
				"errorMessage", ase.getMessage(),
				"statusCode", String.valueOf(ase.getStatusCode()),
				"awsErrorCode", ase.getErrorCode(),
				"errorType", ase.getErrorType().name(),
				"awsRequestId", ase.getRequestId()
			), ase)
		).onError(AmazonClientException.class, ace -> KVLogger.error(LOG, "amazon-client-exception",
			KVMap.of("errorMessage", ace.getMessage()), ace)
		).onError(IOException.class, ioe -> KVLogger.error(LOG, "io-exception",
			KVMap.of("errorMessage", ioe.getMessage()), ioe)
		);
	}
}
