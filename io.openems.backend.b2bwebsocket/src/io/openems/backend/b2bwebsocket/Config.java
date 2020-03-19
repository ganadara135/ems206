package io.openems.backend.b2bwebsocket;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "Backend2Backend.Websocket", //
		description = "Provides a websocket server for backend-to-backend communication.")
@interface Config {

	@AttributeDefinition(name = "Port", description = "The port of the websocket server.")
	int port() default B2bWebsocket.DEFAULT_PORT;

	String webconsole_configurationFactory_nameHint() default "Backend2Backend Websocket";

}