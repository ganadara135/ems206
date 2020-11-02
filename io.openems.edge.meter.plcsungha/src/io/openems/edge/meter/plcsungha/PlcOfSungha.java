package io.openems.edge.meter.plcsungha;

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
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.task.FC4ReadInputRegistersTask;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.meter.api.MeterType;
import io.openems.edge.meter.api.SymmetricMeter;


@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "meter.PlcOfSungha", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class PlcOfSungha extends AbstractOpenemsModbusComponent 
implements SymmetricMeter, OpenemsComponent {

	private Config config = null;

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		GAS_USAGE_1(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE)), //
		GAS_USAGE_2(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE)),
		GAS_USAGE_3(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE)), //
		GAS_USAGE_4(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE)),
		GAS_USAGE_5(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE)),
		GAS_USAGE_6(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE)),
		GAS_USAGE_7(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE)),
		GAS_USAGE_8(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE)),
		GAS_USAGE_9(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE)),
		GAS_USAGE_10(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE)),
		GAS_USAGE_11(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE)),
		GAS_USAGE_12(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE)),
		GAS_USAGE_13(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE)),
		GAS_USAGE_14(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE)),
		GAS_USAGE_15(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE)),
		GAS_USAGE_16(Doc.of(OpenemsType.INTEGER) //
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

	public PlcOfSungha() {
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
					m(PlcOfSungha.ChannelId.GAS_USAGE_1, new UnsignedDoublewordElement(1))),
			new FC4ReadInputRegistersTask(11, Priority.LOW,
					m(PlcOfSungha.ChannelId.GAS_USAGE_2, new UnsignedDoublewordElement(11))),
			new FC4ReadInputRegistersTask(21, Priority.LOW,	
					m(PlcOfSungha.ChannelId.GAS_USAGE_3, new UnsignedDoublewordElement(21))),
			new FC4ReadInputRegistersTask(31, Priority.LOW,
					m(PlcOfSungha.ChannelId.GAS_USAGE_4, new UnsignedDoublewordElement(31))),
			new FC4ReadInputRegistersTask(41, Priority.LOW,
					m(PlcOfSungha.ChannelId.GAS_USAGE_5, new UnsignedDoublewordElement(41))),
			new FC4ReadInputRegistersTask(51, Priority.LOW,
					m(PlcOfSungha.ChannelId.GAS_USAGE_6, new UnsignedDoublewordElement(51))),
			new FC4ReadInputRegistersTask(61, Priority.LOW,
					m(PlcOfSungha.ChannelId.GAS_USAGE_7, new UnsignedDoublewordElement(61))),
			new FC4ReadInputRegistersTask(71, Priority.LOW,
					m(PlcOfSungha.ChannelId.GAS_USAGE_8, new UnsignedDoublewordElement(71))),
			new FC4ReadInputRegistersTask(81, Priority.LOW,
					m(PlcOfSungha.ChannelId.GAS_USAGE_9, new UnsignedDoublewordElement(81))),
			new FC4ReadInputRegistersTask(91, Priority.LOW,
					m(PlcOfSungha.ChannelId.GAS_USAGE_10, new UnsignedDoublewordElement(91))),
			new FC4ReadInputRegistersTask(101, Priority.LOW,
					m(PlcOfSungha.ChannelId.GAS_USAGE_11, new UnsignedDoublewordElement(101))),
			new FC4ReadInputRegistersTask(111, Priority.LOW,
					m(PlcOfSungha.ChannelId.GAS_USAGE_12, new UnsignedDoublewordElement(111))),
			new FC4ReadInputRegistersTask(121, Priority.LOW,
					m(PlcOfSungha.ChannelId.GAS_USAGE_13, new UnsignedDoublewordElement(121))),
			new FC4ReadInputRegistersTask(131, Priority.LOW,
					m(PlcOfSungha.ChannelId.GAS_USAGE_14, new UnsignedDoublewordElement(131))),
			new FC4ReadInputRegistersTask(141, Priority.LOW,
					m(PlcOfSungha.ChannelId.GAS_USAGE_15, new UnsignedDoublewordElement(141))),
			new FC4ReadInputRegistersTask(151, Priority.LOW,
					m(PlcOfSungha.ChannelId.GAS_USAGE_16, new UnsignedDoublewordElement(151)))
			);
	}
	
	Channel<Integer> getGasUsage1() {
		return this.channel(ChannelId.GAS_USAGE_1);
	}
	Channel<Integer> getGasUsage2() {
		return this.channel(ChannelId.GAS_USAGE_2);
	}
	Channel<Integer> getGasUsage3() {
		return this.channel(ChannelId.GAS_USAGE_3);
	}
	Channel<Integer> getGasUsage4() {
		return this.channel(ChannelId.GAS_USAGE_4);
	}
	Channel<Integer> getGasUsage5() {
		return this.channel(ChannelId.GAS_USAGE_5);
	}
	Channel<Integer> getGasUsage6() {
		return this.channel(ChannelId.GAS_USAGE_6);
	}
	Channel<Integer> getGasUsage7() {
		return this.channel(ChannelId.GAS_USAGE_7);
	}
	Channel<Integer> getGasUsage8() {
		return this.channel(ChannelId.GAS_USAGE_8);
	}
	Channel<Integer> getGasUsage9() {
		return this.channel(ChannelId.GAS_USAGE_9);
	}
	Channel<Integer> getGasUsage10() {
		return this.channel(ChannelId.GAS_USAGE_10);
	}
	Channel<Integer> getGasUsage11() {
		return this.channel(ChannelId.GAS_USAGE_11);
	}
	Channel<Integer> getGasUsage12() {
		return this.channel(ChannelId.GAS_USAGE_12);
	}
	Channel<Integer> getGasUsage13() {
		return this.channel(ChannelId.GAS_USAGE_13);
	}
	Channel<Integer> getGasUsage14() {
		return this.channel(ChannelId.GAS_USAGE_14);
	}
	Channel<Integer> getGasUsage15() {
		return this.channel(ChannelId.GAS_USAGE_15);
	}
	Channel<Integer> getGasUsage16() {
		return this.channel(ChannelId.GAS_USAGE_16);
	}

	

	@Override
	public String debugLog() {
		return "L:" + this.getGasUsage1().value().asString() + " / "
				+ this.getGasUsage2().value().asString() + " / "
				+ this.getGasUsage3().value().asString() + " / "
				+ this.getGasUsage4().value().asString() + " / "
				+ this.getGasUsage5().value().asString() + " / "
				+ this.getGasUsage16().value().asString();
	}
	
	@Override
	public MeterType getMeterType() {
		return this.config.type();
	}

}