package st.amnesty

import java.util.Collections
import java.util.UUID.randomUUID

import io.gatling.core.Predef._
import io.gatling.core.structure.PopulationBuilder
import io.gatling.http.Predef._
import okhttp3.logging.HttpLoggingInterceptor
import smartthings.testkit.model.auth.Client
import smartthings.testkit.service.auth.OAuthAuthServiceProvider
import smartthings.testkit.util.Auth
import scala.collection.mutable.ListBuffer

class AmnestySimulation extends Simulation {

	val config = new AmnestySimulationConfig(AmnestySimulationConfig.loadConfig())
	val initialized = new {
		var serviceToken = ""
	}

	val CreateGetDeleteOverridesFeeder: Feeder[String] = Iterator.continually(
			Map[String, String](
				"auth_token" -> initialized.serviceToken,
				"uuid" -> randomUUID().toString))


	def dataSetup(): Unit = {
		val oAuthAuthService = new OAuthAuthServiceProvider(
			config.oauthServiceUrl,
			HttpLoggingInterceptor.Level.BASIC
		).getOAuthAuthService

		val serviceClient = createServiceClient()
		oAuthAuthService.createClient(
			Auth.basic(config.setup.authUsername, config.setup.authPassword),
			serviceClient
		).execute()

		initialized.serviceToken = 	oAuthAuthService.createOauthAccessToken(Auth.basic(serviceClient.getClientId,
			serviceClient.getClientSecret), serviceClient.getClientId,
			serviceClient.getClientSecret, serviceClient.getClientId,
			serviceClient.getClientSecret, serviceClient.getScope.get(0),
			serviceClient.getAuthorizedGrantTypes.get(0),
			serviceClient.getRedirectUri.get(0)).execute.body.getAccessToken
	}

	def createServiceClient(): Client = {
		val fiftyYearsInSeconds: Integer = 60 * 60 * 24 * 365 * 50

		val client = new Client()
		client.setName("app-service-load-test-service-client")
		client.setClientId("app-service-load-test-service-client")
		client.setClientSecret("password1")
		client.setScope(Collections.singletonList("service"))
		client.setAuthorizedGrantTypes(Collections.singletonList("client_credentials"))
		client.setRedirectUri(Collections.singletonList("www.example.com"))
		client.setAccessTokenValiditySeconds(fiftyYearsInSeconds)
		return client
	}

	def createScenarioList() : List[PopulationBuilder] = {
		val scenarios: ListBuffer[PopulationBuilder] = new ListBuffer()
		if(config.postGetDeleteOverrides.rampUsersTo > 0) {
			scenarios.append(
					AmnestyScenarios.combinedTests(CreateGetDeleteOverridesFeeder).inject(
							rampUsers(config.postGetDeleteOverrides.rampUsersTo) over config.rampUpPeriod,
							constantUsersPerSec(config.postGetDeleteOverrides.rampUsersPerSec) during config.duration
			).protocols(http.baseURL(config.serviceUrl)))
		}
		return scenarios.toList
	}


	setUp {
		dataSetup()
		createScenarioList()
	}.maxDuration(config.duration)
}

