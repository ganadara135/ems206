package io.openems.edge.meter.accura2300;

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

import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.SignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.meter.api.MeterType;
import io.openems.edge.meter.api.SymmetricMeter;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Meter.Accura2300", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class Accura2300 extends AbstractOpenemsModbusComponent implements SymmetricMeter, OpenemsComponent {

	private Config config = null;

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		;

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}

	public Accura2300() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				SymmetricMeter.ChannelId.values(),
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
		super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm, "Modbus",
				config.modbus_id());
		this.config = config;
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() {
		// TODO implement ModbusProtocol
//		return new ModbusProtocol(this, //
//				new FC3ReadRegistersTask(1, Priority.HIGH,
//						m(SymmetricMeter.ChannelId.ACTIVE_POWER, new SignedWordElement(1))));
		return new ModbusProtocol(this, 
				new FC3ReadRegistersTask(1000, Priority.HIGH, 
						m(SymmetricMeter.ChannelId.ACTIVE_POWER, new SignedWordElement(1000))),
				
				new FC3ReadRegistersTask(1001, Priority.HIGH, 
						m(SymmetricMeter.ChannelId.VOLTAGE, new SignedWordElement(1001))),
		
				new FC3ReadRegistersTask(1001, Priority.HIGH, 
						m(SymmetricMeter.ChannelId.REACTIVE_POWER, new SignedWordElement(1001))),

				new FC3ReadRegistersTask(2000, Priority.HIGH, 
						m(SymmetricMeter.ChannelId.FREQUENCY, new SignedWordElement(2000))));
	}

	@Override
	public String debugLog() {
		return "L:" + this.getActivePower().value().asString() + " / " 
				+ this.getVoltage().value().asString() + " / " 
				+ this.getReactivePower().value().asString() + " / " 
				+ this.getFrequency().value().asString();

	}

	@Override
	public MeterType getMeterType() {
		// TODO Auto-generated method stub
		return this.config.type();
	}
}
