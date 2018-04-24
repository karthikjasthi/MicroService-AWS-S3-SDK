package st.amnesty;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.reflect.TypeToken;
import ratpack.config.ConfigData;
import ratpack.dropwizard.metrics.DropwizardMetricsConfig;
import ratpack.dropwizard.metrics.DropwizardMetricsModule;
import ratpack.guice.Guice;
import ratpack.registry.Registry;
import ratpack.server.RatpackServer;
import ratpack.server.ServerConfig;
import smartthings.common.api.ratpack.ApiModelRenderer;
import smartthings.common.api.ratpack.ApiModule;
import smartthings.common.api.ratpack.JsonParser;
import smartthings.ratpack.auth.JWTSpringSecAuthModule;
import smartthings.ratpack.config.SmartThingsConfig;
import smartthings.ratpack.httpadmin.HttpAdminModule;
import st.amnesty.modules.AmnestyModule;
import st.amnesty.ratelimit.OverridesChain;
import st.ratpack.auth.TokenValidator;
import st.ratpack.auth.handler.BearerTokenAuthHandler;
import st.ratpack.datadog.DatadogReporterModule;


/**
 * Main entry point for a RatPack Application.  Responsible for configuring and launching server.
 */
public class RatpackMain {

	public static void main(String... args) throws Exception {

		RatpackServer.start(server ->
			server
				.serverConfig(config -> {
					SmartThingsConfig.addStandardSources("amnesty-web", config);
					config.port(Integer.parseInt(System.getProperty("ratpack.port", "8233")));
				})
				.registry(Guice.registry(bindings -> {
					ServerConfig configData = bindings.getServerConfig();
					ObjectMapper objectMapper = new ObjectMapper();
					bindings.bindInstance(ConfigData.class, configData);
					bindings.module(HttpAdminModule.class);
					bindings.module(ApiModule.class);
					bindings.bind(ApiModelRenderer.class);

					bindings.moduleConfig(JWTSpringSecAuthModule.class,
						configData.get("/security", JWTSpringSecAuthModule.Config.class));

					bindings.moduleConfig(
						DropwizardMetricsModule.class,
						configData.get("/metrics", DropwizardMetricsConfig.class)
					);

					bindings.moduleConfig(
						DatadogReporterModule.class,
						configData.get("/datadog", DatadogReporterModule.Config.class)
					);

					bindings.moduleConfig(
						AmnestyModule.class,
						configData.get("/amnesty", AmnestyModule.Config.class)
					);
					bindings.bindInstance(new JsonParser(objectMapper) {
						@Override
						protected TypeToken getType() {
							return TypeToken.of(SmartThingsApiModel.class);
						}
					});
				}))
				.handlers(chain -> {
					Registry registry = chain.getRegistry();
					chain
						.all(new BearerTokenAuthHandler(registry.get(TokenValidator.class)))
						.prefix("overrides", OverridesChain.class);
				}));
	}
}
