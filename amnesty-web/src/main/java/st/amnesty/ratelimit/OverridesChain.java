package st.amnesty.ratelimit;

import ratpack.func.Action;
import ratpack.handling.Chain;
import smartthings.auth.core.scope.Scope;
import st.ratpack.auth.handler.TokenScopeFilterHandler;

/**
 * Amnesty endpoint that route overrides endpoint based on method
 * and auth.
 */
public class OverridesChain implements Action<Chain> {

	private static final String SERVICE_NAME = ":serviceName";

	@Override
	public void execute(Chain chain) throws Exception {
		chain
			.when(ctx -> ctx.getRequest().getMethod().isGet(),
				c -> c.all(new TokenScopeFilterHandler(Scope.SERVICE.getTemplate())),
				c -> c.all(new TokenScopeFilterHandler(Scope.RATELIMIT_OVERRIDES_WRITE.getTemplate()))
			).path(SERVICE_NAME, OverridesHandler.class);
	}
}
