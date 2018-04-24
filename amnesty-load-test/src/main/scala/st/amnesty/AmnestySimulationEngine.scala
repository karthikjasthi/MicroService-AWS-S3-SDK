package st.amnesty

import io.gatling.app.Gatling
import io.gatling.core.config.GatlingPropertiesBuilder

object AmnestySimulationEngine extends App {
	val props = new GatlingPropertiesBuilder
	props.simulationClass(classOf[AmnestySimulation].getName)
	props.resultsDirectory("build/reports/gatling")
	props.binariesDirectory("build/classes/main")

	Gatling.fromMap(props.build)
	sys.exit()
}
