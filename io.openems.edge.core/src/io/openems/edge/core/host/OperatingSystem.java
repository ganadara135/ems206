package io.openems.edge.core.host;

import java.util.concurrent.CompletableFuture;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;

public interface OperatingSystem {

	/**
	 * Gets the network configuration.
	 * 
	 * @return the network configuration object
	 * @throws OpenemsNamedException on error
	 */
	public NetworkConfiguration getNetworkConfiguration() throws OpenemsNamedException;

	/**
	 * Handles a SetNetworkConfigRequest.
	 * 
	 * @param oldNetworkConfiguration the current/old network configuration
	 * @param request                 the SetNetworkConfigRequest
	 * @throws OpenemsNamedException on error
	 */
	public void handleSetNetworkConfigRequest(NetworkConfiguration oldNetworkConfiguration,
			SetNetworkConfigRequest request) throws OpenemsNamedException;

	/**
	 * Executes a command.
	 * 
	 * @param request the ExecuteCommandRequest
	 * @return a ExecuteCommandResponse
	 */
	public CompletableFuture<ExecuteSystemCommandResponse> handleExecuteCommandRequest(ExecuteSystemCommandRequest request);

}
