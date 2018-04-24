package st.amnesty.ratelimit;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.Protocol;
import com.amazonaws.auth.*;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.google.inject.Inject;
import st.amnesty.modules.AmnestyModule;

import javax.inject.Provider;

/**
 * Provider Class to build S3 Client.
 */
public class S3ClientProvider implements Provider<AmazonS3> {

	private final AmnestyModule.Config config;

	@Inject
	public S3ClientProvider(AmnestyModule.Config config) {
		this.config = config;
	}

	public AmazonS3 get() {
		//Setup client protocol
		ClientConfiguration clientConfig = new ClientConfiguration();
		clientConfig.setProtocol(Protocol.HTTP);

		AWSCredentialsProvider provider;
		String localS3AccessKey = config.getS3().getAccessKey();
		String localS3SecretKey = config.getS3().getSecretKey();

		if (localS3AccessKey == null
			|| localS3SecretKey == null
			|| localS3AccessKey.equals("")
			|| localS3SecretKey.equals("")) {
			//Use InstanceProfileCredentialsProvider in ec2
			provider = new InstanceProfileCredentialsProvider(false);
		} else {
			//Setup AWS credential provider for local docker s3
			provider = new AWSStaticCredentialsProvider(new BasicAWSCredentials(localS3AccessKey, localS3SecretKey));
		}

		//Build amazon s3 client
		return AmazonS3ClientBuilder
			.standard()
			.withCredentials(provider)
			.withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(config.getS3().getServiceEndpoint(),
				config.getS3().getRegion()))
			.withPathStyleAccessEnabled(true)
			.withClientConfiguration(clientConfig)
			.build();
	}
}
