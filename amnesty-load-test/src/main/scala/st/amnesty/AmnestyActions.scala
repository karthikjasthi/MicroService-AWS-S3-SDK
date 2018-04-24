package st.amnesty

import io.gatling.core.Predef._
import io.gatling.http.Predef._

import io.gatling.core.action.builder.ActionBuilder
import io.gatling.http.request.builder.HttpRequestBuilder.toActionBuilder


object AmnestyActions {

	var tempString = ""
	def createOverrides: ActionBuilder = {
		http ("Post Overrides to Amnesty")
			.post ("/overrides/${uuid}")
			.header (HttpHeaderNames.Authorization, "Bearer ${auth_token}")
			.header (HttpHeaderNames.ContentType, HttpHeaderValues.ApplicationJson)
			.header (HttpHeaderNames.Accept, HttpHeaderValues.ApplicationJson)
			.body (
			StringBody ("""{"rateLimitOverrideSet" :[{"principalId":"ours","handlerId":"me","bucketId":"placejdfas","rateLimit": 78}]}""")
			).asJSON
			.check (status.is(204))
	}
	def getOverrides: ActionBuilder = {
			 http("Get Overrides from Amnesty")
				.get("/overrides/${uuid}")
				.header(HttpHeaderNames.Authorization, "Bearer ${auth_token}")
				.header(HttpHeaderNames.ContentType, HttpHeaderValues.ApplicationJson)
				.header(HttpHeaderNames.Accept, HttpHeaderValues.ApplicationJson)
				.check(status.is(200))
				.check(header("etag").saveAs(tempString))
	}

	def getOverrides304: ActionBuilder = {
			http("Get 304 response from Amnesty")
				.get("/overrides/${uuid}")
				.header(HttpHeaderNames.Authorization, "Bearer ${auth_token}")
				.header(HttpHeaderNames.ContentType, HttpHeaderValues.ApplicationJson)
				.header(HttpHeaderNames.Accept, HttpHeaderValues.ApplicationJson)
				.header(HttpHeaderNames.IfNoneMatch, tempString)
				.check(status.is(304))
		}


	def deleteOverrides: ActionBuilder =  {
		http("Delete Overrides from Amnesty")
			.delete("/overrides/${uuid}")
			.header(HttpHeaderNames.Authorization, "Bearer ${auth_token}")
			.header(HttpHeaderNames.ContentType,  HttpHeaderValues.ApplicationJson)
			.header(HttpHeaderNames.Accept, HttpHeaderValues.ApplicationJson)
			.check(status.is(204))

	}

}
