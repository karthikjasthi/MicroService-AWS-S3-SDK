package st.amnesty.ratelimit;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import st.amnesty.SmartThingsApiModel;

import javax.validation.Valid;
import java.util.Set;

/**
 * Wrapper class for OverrideEtag.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OverrideETag implements SmartThingsApiModel {
	@Valid
	private Set<RateLimitOverride> rateLimitOverrideSet;
	@JsonIgnore
	private String eTag;
}
