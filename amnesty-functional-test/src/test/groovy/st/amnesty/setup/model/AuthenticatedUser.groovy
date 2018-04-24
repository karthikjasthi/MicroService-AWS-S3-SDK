package st.amnesty.setup.model

import smartthings.testkit.model.auth.OAuthToken
import smartthings.testkit.model.auth.User

class AuthenticatedUser {
	final User user
	final OAuthToken token

	AuthenticatedUser(User user, OAuthToken token) {
		this.user = user
		this.token = token
	}
}
