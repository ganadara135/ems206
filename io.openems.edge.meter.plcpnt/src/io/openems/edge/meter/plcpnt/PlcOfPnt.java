package io.openems.edge.meter.plcpnt;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.metatype.annotations.Designate;

import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.SignedWordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedQuadruplewordElement;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC4ReadInputRegistersTask;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.meter.api.MeterType;
import io.openems.edge.meter.api.SymmetricMeter;


@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Meter.PLCofPnt", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class PlcOfPnt extends AbstractOpenemsModbusComponent 
	implements SymmetricMeter, OpenemsComponent {

	private Config config = null;

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		PRESSURE(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.NONE)), //
		MASS_FLOW(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.NONE)),
		VOLUME_FLOW(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.NONE)), //
		ENERGY_FLOW(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.NONE)),
		NORMAL_FLOW(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.NONE));

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}

	public PlcOfPnt() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				SymmetricMeter.ChannelId.values(), //
				ChannelId.values() //
		);
	}

	@Reference
	protected ConfigurationAdmin cm;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	@Activate
	void activate(ComponentContext context, Config config) {
		
		this.config = config;
		super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm, "Modbus",
				config.modbus_id());
		
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() {
		// TODO implement ModbusProtocol
		return new ModbusProtocol(this, //
			new FC4ReadInputRegistersTask(1, Priority.LOW,
					m(PlcOfPnt.ChannelId.PRESSURE, new UnsignedDoublewordElement(1)),
					m(PlcOfPnt.ChannelId.PRESSURE, new UnsignedDoublewordElement(2)),
					m(PlcOfPnt.ChannelId.PRESSURE, new UnsignedDoublewordElement(3)),
					m(PlcOfPnt.ChannelId.PRESSURE, new UnsignedDoublewordElement(4))
					)
			);
	}

	@Override
	public String debugLog() {
		return "L:" + this.getActivePower().value().asString();
	}
	
	@Override
	public MeterType getMeterType() {
		return this.config.type();
	}

}
