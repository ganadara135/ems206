package io.openems.backend.uiwebsocket.impl;

import java.util.Optional;
import java.util.UUID;

import io.openems.backend.metadata.api.Metadata;
import io.openems.backend.metadata.api.BackendUser;
import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;

public class WsData extends io.openems.common.websocket.WsData {

	private final SubscribedChannelsWorker subscribedChannelsWorker;
	private Optional<String> userId = Optional.empty();
	private Optional<UUID> token = Optional.empty();

	public WsData(UiWebsocketImpl parent) {
		this.subscribedChannelsWorker = new SubscribedChannelsWorker(parent, this);
	}

	@Override
	public void dispose() {
		this.subscribedChannelsWorker.dispose();
	}

	public synchronized void setUserId(String userId) {
		this.userId = Optional.ofNullable(userId);
	}

	/**
	 * Gets the authenticated User-ID.
	 * 
	 * @return the User-ID or Optional.Empty if the User was not authenticated.
	 */
	public synchronized Optional<String> getUserId() {
		return userId;
	}

	/**
	 * Gets the authenticated User.
	 * 
	 * @param metadata the Metadata service
	 * @return the User or Optional.Empty if the User was not authenticated or it is
	 *         not available from Metadata service
	 */
	public synchronized Optional<BackendUser> getUser(Metadata metadata) {
		Optional<String> userId = this.getUserId();
		if (userId.isPresent()) {
			Optional<BackendUser> user = metadata.getUser(userId.get());
			return user;
		}
		return Optional.empty();
	}

	public void setToken(UUID token) {
		this.token = Optional.ofNullable(token);
	}

	public Optional<UUID> getToken() {
		return token;
	}

	/**
	 * Gets the token or throws an error if no token was set.
	 * 
	 * @return the token
	 * @throws OpenemsNamedException if no token has been set
	 */
	public UUID assertToken() throws OpenemsNamedException {
		Optional<UUID> token = this.token;
		if (token.isPresent()) {
			return token.get();
		}
		throw OpenemsError.BACKEND_UI_TOKEN_MISSING.exception();
	}

	/**
	 * Gets the SubscribedChannelsWorker to take care of subscribe to CurrentData.
	 * 
	 * @return the SubscribedChannelsWorker
	 */
	public SubscribedChannelsWorker getSubscribedChannelsWorker() {
		return subscribedChannelsWorker;
	}

	@Override
	public String toString() {
		String tokenString;
		if (this.token.isPresent()) {
			tokenString = this.token.get().toString();
		} else {
			tokenString = "UNKNOWN";
		}
		return "UiWebsocket.WsData [userId=" + userId.orElse("UNKNOWN") + ", token=" + tokenString + "]";
	}
}
