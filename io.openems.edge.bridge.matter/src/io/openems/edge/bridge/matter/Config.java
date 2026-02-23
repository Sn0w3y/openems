package io.openems.edge.bridge.matter;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "Bridge Matter", //
		description = "Provides a Matter protocol bridge via a Node.js matter.js subprocess.")
public @interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "matter0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Node.js Path", description = "Path to Node.js binary. Leave empty for auto-detection.")
	String nodejsPath() default "";

	@AttributeDefinition(name = "Storage Path", description = "Path for Matter data storage (fabric, device state)")
	String storagePath() default "data/matter";

	@AttributeDefinition(name = "WebSocket Port", description = "Port for internal WebSocket communication with Node.js process. 0 = random.")
	int websocketPort() default 0;

	@AttributeDefinition(name = "Debug Mode", description = "Enable debug logging for Matter communication")
	boolean debugMode() default false;

	String webconsole_configurationFactory_nameHint() default "Bridge Matter [{id}]";
}
