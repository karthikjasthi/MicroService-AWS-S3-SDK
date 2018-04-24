package st.amnesty.ratelimit;

import lombok.*;
import st.amnesty.SmartThingsApiModel;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * RateLimitOverride Model.
 */

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RateLimitOverride implements SmartThingsApiModel {
	@NotNull
	@Size(min = 1, max = 50)
	private String principalId;
	@Size(max = 50)
	private String handlerId;
	@Size(max = 50)
	private String bucketId;
	@Min(1)
	private int rateLimit;
}
