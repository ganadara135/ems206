package io.openems.backend.metadata.dummy;

import io.openems.backend.metadata.api.Edge;
import io.openems.common.types.EdgeConfig;

public class MyEdge extends Edge {

	private final String apikey;

	public MyEdge(String id, String apikey, String comment, State state, String version, String producttype,
			EdgeConfig config) {
		super(id, comment, state, version, producttype, config);
		this.apikey = apikey;
	}

	public String getApikey() {
		return apikey;
	}

}
