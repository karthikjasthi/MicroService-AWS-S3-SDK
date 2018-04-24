package st.amnesty.setup.utils

import okhttp3.Interceptor
import okhttp3.Request
import org.slf4j.MDC

import static smartthings.testkit.service.ServiceProvider.*

class RetrofitUtils {
	static Interceptor interceptor(String media) {
		return { chain ->
			Request original = chain.request()
			Request.Builder request = original.newBuilder()
				.header("Content-Type", media)
				.header("Accept", media)
				.method(original.method(), original.body())

			if (MDC.get(LOGGING_ID) != null) {
				request
					.header(CORRELATION_ID_HEADER, MDC.get(LOGGING_ID))
					.header(LOG_LEVEL_HEADER, "DEBUG")
			}

			return chain.proceed(request.build())
		}
	}
}
