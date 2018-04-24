package st.amnesty.setup

import okhttp3.logging.HttpLoggingInterceptor
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import retrofit2.Response
import rx.Observable
import rx.schedulers.Schedulers
import smartthings.test.common.credentials.AuthCredentials
import smartthings.testkit.model.auth.Client
import smartthings.testkit.model.auth.OAuthToken
import smartthings.testkit.model.auth.User
import smartthings.testkit.service.auth.OAuthAuthService
import smartthings.testkit.service.auth.OAuthAuthServiceProvider
import smartthings.testkit.util.Auth
import st.amnesty.setup.model.AuthenticatedUser

import java.util.stream.Collectors
import java.util.stream.IntStream

class OAuthSetup {

	private static final Logger log = LoggerFactory.getLogger(OAuthSetup.class)

	private OAuthAuthService oAuthAuthService
	private Client serviceClient
	private Client userClient //mobile
	private Client fineGrainedServiceClient
	private AuthCredentials authCredentials = new AuthCredentials()

	OAuthSetup(String oauthUrl) {
		this.oAuthAuthService = new OAuthAuthServiceProvider(
			oauthUrl,
			HttpLoggingInterceptor.Level.BODY
		).getOAuthAuthService()

		// Create service client
		this.serviceClient = buildServiceClient()
		this.oAuthAuthService.createClient(
			Auth.basic(authCredentials.username, authCredentials.password),
			serviceClient
		).execute()

		// Create user client
		this.userClient = buildUserClient()
		this.oAuthAuthService.createClient(
			Auth.basic(authCredentials.username, authCredentials.password),
			userClient
		).execute()

		// Create installed app client
		this.fineGrainedServiceClient = buildFineGrainedServiceClient()
		oAuthAuthService.createClient(
			Auth.basic(authCredentials.username, authCredentials.password),
			fineGrainedServiceClient
		).execute()
	}

	private static Client buildServiceClient() {
		new Client(
			name: 'amnesty-service-functional-test-service-client',
			clientId: 'amnesty-service-functional-test-service-client',
			clientSecret: 'e7db7d60-8cc1-11e6-ae22-56b6b6499611',
			scope: ['service'],
			authorizedGrantTypes: ['client_credentials'],
			redirectUri: ['www.example.com'],
			accessTokenValiditySeconds: 1576800000
		)
	}

	private static Client buildFineGrainedServiceClient() {
		new Client(
			name: 'amnesty-service-functional-test-service-client-finegrained',
			clientId: 'amnesty-service-functional-test-service-client-finegrained',
			clientSecret: 'e7db7d60-8cc1-11e6-ae22-56b6b6499611',
			scope: ['w:service:rate:overrides:*'],
			authorizedGrantTypes: ['client_credentials'],
			redirectUri: ['www.example.com'],
			accessTokenValiditySeconds: 1576800000
		)
	}

	private static Client buildUserClient() {
		new Client(
			name: 'amnesty-service-functional-test-user-client',
			clientId: 'amnesty-service-functional-test-user-client',
			clientSecret: 'e7db7d60-8cc1-11e6-ae22-56b6b6499611',
			scope: ['mobile'],
			authorizedGrantTypes: ['password'],
			redirectUri: ['www.example.com'],
			accessTokenValiditySeconds: 1576800000
		)
	}

	Observable<AuthenticatedUser> createAuthenticatedUser() {
		return createAuthenticatedUser(userClient)
	}

	Observable<AuthenticatedUser> createAuthenticatedUser(Client client) {
		return users(1)
			.toList()
			.flatMap({ users ->
			User user = users.get(0)
			OAuthToken token = token(client, user.getUsername(), user.getPassword())

			return oAuthAuthService.getMeRx("Bearer " + token.getAccessToken())
				.map({ Response<User> r ->
				return new AuthenticatedUser(r.body(), token)
			})
		})
	}

	String createServiceToken() {
		String serviceToken = oAuthAuthService.createOauthAccessToken(
			Auth.basic(serviceClient.clientId, serviceClient.clientSecret),
			serviceClient.clientId,
			serviceClient.clientSecret,
			serviceClient.clientId,
			serviceClient.clientSecret,
			serviceClient.scope[0],
			serviceClient.authorizedGrantTypes[0],
			serviceClient.redirectUri[0]
		)
			.execute()
			.body().accessToken

		return Auth.bearer(serviceToken)
	}

	String createFineGrainedServiceToken() {
		String serviceToken = oAuthAuthService.createOauthAccessToken(
			Auth.basic(fineGrainedServiceClient.clientId, fineGrainedServiceClient.clientSecret),
			fineGrainedServiceClient.clientId,
			fineGrainedServiceClient.clientSecret,
			fineGrainedServiceClient.clientId,
			fineGrainedServiceClient.clientSecret,
			fineGrainedServiceClient.scope[0],
			fineGrainedServiceClient.authorizedGrantTypes[0],
			fineGrainedServiceClient.redirectUri[0]
		)
			.execute()
			.body().accessToken

		return Auth.bearer(serviceToken)
	}

	private Observable<User> users(int numOfUsers) {
		List<User> users =
			IntStream.range(0, numOfUsers)
				.mapToObj({ int i -> user(i) })
				.collect(Collectors.toList())

		return Observable.from(users)
			.flatMap({ this.user(it) })
	}

	private Observable<User> user(User user) {
		return oAuthAuthService.registerUserRx(Auth.basic(authCredentials.username, authCredentials.password), user)
			.subscribeOn(Schedulers.immediate())
			.filter({ Response response -> response.isSuccessful() })
			.map({ Response response -> user })
	}

	private OAuthToken token(Client client, String username, String password) {
		Response<OAuthToken> response = oAuthAuthService.createOauthAccessToken(
			Auth.basic(client.getClientId(), client.getClientSecret()),
			client.getClientId(),
			client.getClientSecret(),
			username,
			password,
			client.getScope().get(0),
			client.getAuthorizedGrantTypes().get(0),
			client.getRedirectUri().get(0)
		).execute()

		if (!response.isSuccessful()) {
			log.error("Unable to create oauth token [" + response.errorBody().string() + "]")
			throw new IllegalStateException("Unable to create user token")
		}
		return response.body()
	}

	private static User user(int index) {
		String userUuid = UUID.randomUUID().toString()
		User user = new User()
		user.setUsername(userUuid + index + "@example.com")
		user.setEmail(userUuid + "@example.com")
		user.setFullName("Test " + userUuid)
		user.setPassword("Password1")

		return user
	}

}
