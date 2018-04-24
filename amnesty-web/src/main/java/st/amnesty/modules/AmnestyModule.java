package st.amnesty.modules;

import com.amazonaws.services.s3.AmazonS3;
import com.google.inject.Scopes;
import lombok.Getter;
import lombok.Setter;
import st.amnesty.ratelimit.*;
import ratpack.guice.ConfigurableModule;
import st.amnesty.ratelimit.S3ClientProvider;

import javax.validation.Validation;
import javax.validation.Validator;

/**
 * Guice bindings for the App API.
 */
public class AmnestyModule extends ConfigurableModule<AmnestyModule.Config> {

	@Override
	protected void configure() {
		bind(OverridesHandler.class).in(Scopes.SINGLETON);
		bind(OverridesService.class).to(DefaultOverridesService.class);
		bind(Validator.class).toInstance(Validation.buildDefaultValidatorFactory().getValidator());
		bind(OverridesService.class).to(DefaultOverridesService.class);
		bind(AmazonS3.class).toProvider(S3ClientProvider.class).in(Scopes.SINGLETON);
		bind(OverridesChain.class).in(Scopes.SINGLETON);
	}

	/**
	 * Configuration POJO for Executable module.
	 */

	@Getter
	@Setter
	public static class Config {
		private int httpClientConnectionPoolSize = 10;
		private int httpClientReadTimeoutInSeconds = 5;
		private S3Config s3;
	}


	/**
	 * Config class to build S3.
	 */

	@Getter
	@Setter
	public static class S3Config {

		private String accessKey;
		private String secretKey;
		private String serviceEndpoint;
		private String region;
		private String bucketName;

	}

}
