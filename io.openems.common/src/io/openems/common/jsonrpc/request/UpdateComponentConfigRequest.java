package io.openems.common.jsonrpc.request;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.utils.JsonUtils;

/**
 * Represents a JSON-RPC Request for 'updateComponentConfig'.
 * 
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "method": "updateComponentConfig",
 *   "params": {
 *     "componentId": string,
 *     "properties": [{
 *       "name": string,
 *       "value": any 
 *     }]
 *   }
 * }
 * </pre>
 */
public class UpdateComponentConfigRequest extends JsonrpcRequest {

	public static UpdateComponentConfigRequest from(JsonrpcRequest r) throws OpenemsNamedException {
		JsonObject p = r.getParams();
		String componentId = JsonUtils.getAsString(p, "componentId");
		List<Property> properties = Property.from(JsonUtils.getAsJsonArray(p, "properties"));
		return new UpdateComponentConfigRequest(r.getId(), componentId, properties);
	}

	public final static String METHOD = "updateComponentConfig";

	private final String componentId;
	private final List<Property> properties;

	public UpdateComponentConfigRequest(String componentId, List<Property> properties) {
		this(UUID.randomUUID(), componentId, properties);
	}

	public UpdateComponentConfigRequest(UUID id, String componentId, List<Property> properties) {
		super(id, METHOD);
		this.componentId = componentId;
		this.properties = properties;
	}

	@Override
	public JsonObject getParams() {
		JsonArray properties = new JsonArray();
		for (Property property : this.properties) {
			properties.add(property.toJson());
		}
		return JsonUtils.buildJsonObject() //
				.addProperty("componentId", this.componentId) //
				.add("properties", properties) //
				.build();
	}

	public String getComponentId() {
		return componentId;
	}

	public List<Property> getProperties() {
		return this.properties;
	}

	public static class Property {

		public static List<Property> from(JsonArray j) throws OpenemsNamedException {
			List<Property> properties = new ArrayList<>();
			for (JsonElement property : j) {
				String name = JsonUtils.getAsString(property, "name");
				JsonElement value = JsonUtils.getSubElement(property, "value");
				properties.add(new Property(name, value));
			}
			return properties;
		}

		private final String name;
		private final JsonElement value;

		/**
		 * @param name  the Property name
		 * @param value the new value
		 */
		public Property(String name, JsonElement value) {
			this.name = name;
			this.value = value;
		}

		public Property(String name, String value) {
			this(name, new JsonPrimitive(value));
		}

		public Property(String name, boolean value) {
			this(name, new JsonPrimitive(value));
		}

		public Property(String name, Number value) {
			this(name, new JsonPrimitive(value));
		}

		public String getName() {
			return this.name;
		}

		public JsonElement getValue() {
			return this.value;
		}

		public JsonObject toJson() {
			return JsonUtils.buildJsonObject() //
					.addProperty("name", this.getName()) //
					.add("value", this.getValue()) //
					.build();
		}
	}
}
