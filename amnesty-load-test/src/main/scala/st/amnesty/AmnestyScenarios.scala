package st.amnesty

import io.gatling.core.Predef._
import io.gatling.core.structure.ScenarioBuilder

object AmnestyScenarios {

	def createOverrides: ScenarioBuilder = {
		scenario("Create overrides on Amnesty")
			.exec(AmnestyActions.createOverrides)
	}

	def deleteOverrides: ScenarioBuilder = {
		scenario("Delete Overrides from Amnesty")
			.exec(AmnestyActions.deleteOverrides)
	}

	def getOverride: ScenarioBuilder = {
		scenario("Get request without Etag")
			.exec(AmnestyActions.getOverrides)
	}

	def getOverrideWithEtag: ScenarioBuilder = {
		scenario("Get Request with Etag")
			.exec(AmnestyActions.getOverrides304)
	}

	def combinedTests(feeder: Feeder[String]): ScenarioBuilder = {
		scenario("Combined test")
			.feed(feeder)
			.exec(createOverrides)
			.exec(getOverride)
			.exec(getOverrideWithEtag)
			.exec(deleteOverrides)
	}
}
