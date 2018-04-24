package st.amnesty.fixtures

import ratpack.func.Action
import ratpack.guice.BindingsImposition
import ratpack.impose.ImpositionsSpec
import ratpack.impose.ServerConfigImposition
import ratpack.impose.UserRegistryImposition
import ratpack.registry.Registry
import ratpack.test.MainClassApplicationUnderTest
import ratpack.test.http.TestHttpClient
import st.amnesty.RatpackMain
import st.amnesty.ratelimit.S3ClientProvider
import st.ratpack.auth.TokenValidator

class AmnestyApplicationUnderTest extends MainClassApplicationUnderTest {

	Action<? super ImpositionsSpec> impositions
	Properties serverConfigProperties
	private static
	final Map<String, String> DEFAULTS = ["metrics.jvmMetrics": "false", "metrics.jmx.enabled": "false"].asImmutable()

	AmnestyApplicationUnderTest(
		Action<? super ImpositionsSpec> impositions = Action.noop(),
		Map<String, String> serverConfigProperties = Collections.emptyMap()
	) {
		super(RatpackMain)
		this.impositions = impositions
		this.serverConfigProperties = serverConfigProperties
		DEFAULTS.each(this.serverConfigProperties.&putIfAbsent)
	}

	@Override
	protected void addImpositions(ImpositionsSpec impositionSpec) {
		super.addImpositions(impositionSpec)
		impositions.execute(impositionSpec)
		impositionSpec.add(ServerConfigImposition.of { s -> s.props(serverConfigProperties) })
	}

	TestHttpClient getHttpClient(Map<String, String> headers) {
		return TestHttpClient.testHttpClient(this) { headers.each(it.headers.&add) }
	}

	static class Builder {
		private Properties serverConfigProperties = new Properties()
		private Action<? super ImpositionsSpec> impositions = Action.noop()

		Builder() {}

		Builder s3ClientProvider(S3ClientProvider s3ClientProvider) {
			impositions = impositions.append { ImpositionsSpec spec ->
				spec.add(BindingsImposition.of { bindingsSpec ->
					bindingsSpec.bindInstance(S3ClientProvider.class, s3ClientProvider)
				})
			}
			return this
		}

		Builder tokenValidator(TokenValidator tokenValidator) {
			impositions = impositions.append { ImpositionsSpec spec ->
				spec.add(UserRegistryImposition.of(
					Registry.of { r ->
						r.add(TokenValidator, tokenValidator)
					}
				))
			}
			return this
		}

		AmnestyApplicationUnderTest build() {
			return new AmnestyApplicationUnderTest(impositions, serverConfigProperties)
		}
	}

}
