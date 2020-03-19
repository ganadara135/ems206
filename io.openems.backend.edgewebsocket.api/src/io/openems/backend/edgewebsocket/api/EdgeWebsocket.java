package io.openems.backend.edgewebsocket.api;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.osgi.annotation.versioning.ProviderType;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.base.JsonrpcNotification;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;
import io.openems.common.jsonrpc.request.SubscribeSystemLogRequest;
import io.openems.common.session.User;

@ProviderType
public interface EdgeWebsocket {

	/**
	 * Send an authenticated JSON-RPC Request to an Edge via Websocket and expect a
	 * JSON-RPC Response.
	 * 
	 * @param edgeId  the Edge-ID
	 * @param request the JsonrpcRequest
	 * @param user    the authenticated User
	 * @return the JSON-RPC Success Response Future
	 * @throws OpenemsNamedException on error
	 */
	public CompletableFuture<JsonrpcResponseSuccess> send(String edgeId, User user, JsonrpcRequest request)
			throws OpenemsNamedException;

	/**
	 * Send a JSON-RPC Notification to an Edge.
	 * 
	 * @param edgeId       the Edge-ID
	 * @param notification the JsonrpcNotification
	 * @throws OpenemsNamedException on error
	 */
	public void send(String edgeId, JsonrpcNotification notification) throws OpenemsNamedException;

	/**
	 * Handles a {@link SubscribeSystemLogRequest}.
	 * 
	 * @param edgeId  the Edge-ID
	 * @param token   the UI session token
	 * @param request the {@link SubscribeSystemLogRequest}
	 * @return a reply
	 * @throws OpenemsNamedException on error
	 */
	public CompletableFuture<JsonrpcResponseSuccess> handleSubscribeSystemLogRequest(String edgeId, User user,
			UUID token, SubscribeSystemLogRequest request) throws OpenemsNamedException;

}
