package io.openems.backend.edgewebsocket.impl;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "Edge.Websocket", //
		description = "Configures the websocket server for OpenEMS Edge")
@interface Config {

	@AttributeDefinition(name = "Port", description = "The port of the websocket server.")
	int port() default 8081;

	String webconsole_configurationFactory_nameHint() default "Edge Websocket";

}
