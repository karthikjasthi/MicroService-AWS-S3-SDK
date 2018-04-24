package st.amnesty

import java.io.File

import com.typesafe.config.{Config, ConfigException, ConfigFactory}
import smartthings.testkit.util.{Environment, Urls}

object AmnestySimulationConfig {
	def loadConfig(): Config = {
		val configFilePath = sys.props.get("simulation.config")

		if (configFilePath.isDefined) {
			val file = new File(configFilePath.get)
			ConfigFactory.parseFile(file)
		} else {
			ConfigFactory.parseResources("amnesty-simulation.conf")
		}
	}
}

class AmnestySimulationConfig(config: Config) {
	def urls = new Urls(Environment.fromString(config.getString("service.amnesty.environment")))
	def oauthServiceUrl = optionalGetString(config, "service.amnesty.authServiceUrl").getOrElse(urls.getAuthService)
	def serviceUrl = config.getString("service.amnesty.serviceUrl")
	def rampUpPeriod = config.getInt("service.amnesty.rampUpPeriod")
	def duration = config.getInt("service.amnesty.duration")
	def environment = config.getString("service.amnesty.environment")

	val setup = new {
		def authUsername = config.getString("service.amnesty.setup.authUsername")
		def authPassword = config.getString("service.amnesty.setup.authPassword")
		def threadPoolSize = config.getInt("service.amnesty.setup.threadPoolSize")

	}

	val postGetDeleteOverrides = new {
		def rampUsersPerSec = config.getInt("service.amnesty.createGetDeleteOverrides.rampUsersPerSec")
		def rampUsersTo = config.getInt("service.amnesty.createGetDeleteOverrides.rampUsersTo")
		def users = config.getInt("service.amnesty.createGetDeleteOverrides.users")
	}

	def optionalGetString(config: Config, key: String): Option[String] = {
		try {
			Some(config.getString(key))
		} catch {
			case e: ConfigException.Missing => None
		}
	}

}
