package st.amnesty.ratelimit;

import com.google.inject.Inject;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.apache.http.HttpStatus;
import ratpack.handling.Context;
import ratpack.handling.Handler;

import javax.validation.Validator;

import static ratpack.jackson.Jackson.json;

/**
 * Handler for /overrides/:serviceName.
 */
public class OverridesHandler implements Handler {


	private final OverridesService overridesService;
	private final Validator validator;
	private static final String SERVICE_NAME = "serviceName";

	@Inject
	public OverridesHandler(OverridesService overridesService, Validator validator) {
		this.overridesService = overridesService;
		this.validator = validator;
	}

	@Override
	public void handle(Context ctx) throws Exception {
		ctx.byMethod(byMethodSpec -> byMethodSpec
			.get(() -> {
				String incomingTag = ctx.getRequest().getHeaders().get(HttpHeaderNames.IF_NONE_MATCH);
				overridesService.getOverrides(ctx.getPathTokens().get(SERVICE_NAME))
					.onError(ctx::error)
					.then(requestOverride -> {
						if (checkTagsNotNullAndEqual(incomingTag, requestOverride.getETag())) {
							ctx.getResponse().status(HttpStatus.SC_NOT_MODIFIED).send();
						} else {
							addNewTag(ctx, requestOverride)
								.render(json(requestOverride.getRateLimitOverrideSet()));
						}
					});
			})
			.post(() ->
				ctx
					.parse(OverrideETag.class)
					.route(override -> !validator.validate(override).isEmpty(),
						override -> ctx.clientError(HttpResponseStatus.UNPROCESSABLE_ENTITY.code()))
					.flatMap(override ->
						overridesService.storeOverrides(ctx.getPathTokens().get(SERVICE_NAME), override.getRateLimitOverrideSet()))
					.onError(ctx::error)
					.then(saved -> ctx.getResponse().status(HttpStatus.SC_NO_CONTENT).send())
			)
			.delete(() ->
				overridesService.deleteOverrides(ctx.getPathTokens().get(SERVICE_NAME))
					.onError(ctx::error)
					.then(deleted -> {
						if (deleted) {
							ctx.getResponse().status(HttpStatus.SC_NO_CONTENT).send();
						} else {
							ctx.getResponse().status(HttpStatus.SC_NOT_FOUND).send();
						}

					})
			)
		);
	}

	private Context addNewTag(Context ctx, OverrideETag overrideEtag) {
		if (overrideEtag.getETag() != null) {
			ctx.getResponse()
				.getHeaders()
				.set(HttpHeaderNames.ETAG, overrideEtag.getETag());
		}
		return ctx;
	}

	private boolean checkTagsNotNullAndEqual(String incomingTag, String savedEtag) {
		return incomingTag != null && incomingTag.equals(savedEtag);
	}

}
