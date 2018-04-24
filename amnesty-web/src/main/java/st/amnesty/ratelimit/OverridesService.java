package st.amnesty.ratelimit;

import ratpack.exec.Promise;

import java.util.Set;

/**
 * OverrideService Interface.
 */

public interface OverridesService {

	Promise<OverrideETag> getOverrides(String serviceName);

	Promise<Void> storeOverrides(String serviceName, Set<RateLimitOverride> rateLimitOverrides);

	Promise<Boolean> deleteOverrides(String serviceName);

}
