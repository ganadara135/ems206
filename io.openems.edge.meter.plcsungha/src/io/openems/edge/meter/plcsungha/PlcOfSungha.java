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
import io.openems.edge.bridge.modbus.api.element.SignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.SignedWordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
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
				.unit(Unit.NONE)),
		TEMP_BURNER_1(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEGREE_CELSIUS)),
		SET_TEMP_BURNER_1(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEGREE_CELSIUS)),
		TEMP_BURNER_ON_OFF_1(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE)),
		TEMP_BURNER_9(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEGREE_CELSIUS)),
		SET_TEMP_BURNER_9(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEGREE_CELSIUS)),
		TEMP_BURNER_ON_OFF_9(Doc.of(OpenemsType.INTEGER) //
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
			new FC4ReadInputRegistersTask(0, Priority.LOW,
					m(PlcOfSungha.ChannelId.GAS_USAGE_1, new UnsignedWordElement(0))),
			new FC4ReadInputRegistersTask(10, Priority.LOW,
					m(PlcOfSungha.ChannelId.GAS_USAGE_2, new UnsignedWordElement(10))),
			new FC4ReadInputRegistersTask(20, Priority.LOW,	
					m(PlcOfSungha.ChannelId.GAS_USAGE_3, new UnsignedWordElement(20))),
			new FC4ReadInputRegistersTask(30, Priority.LOW,
					m(PlcOfSungha.ChannelId.GAS_USAGE_4, new UnsignedWordElement(30))),
			new FC4ReadInputRegistersTask(40, Priority.LOW,
					m(PlcOfSungha.ChannelId.GAS_USAGE_5, new UnsignedWordElement(40))),
			new FC4ReadInputRegistersTask(50, Priority.LOW,
					m(PlcOfSungha.ChannelId.GAS_USAGE_6, new UnsignedWordElement(50))),
			new FC4ReadInputRegistersTask(60, Priority.LOW,
					m(PlcOfSungha.ChannelId.GAS_USAGE_7, new UnsignedWordElement(60))),
			new FC4ReadInputRegistersTask(70, Priority.LOW,
					m(PlcOfSungha.ChannelId.GAS_USAGE_8, new UnsignedWordElement(70))),
			new FC4ReadInputRegistersTask(80, Priority.LOW,
					m(PlcOfSungha.ChannelId.GAS_USAGE_9, new UnsignedWordElement(80))),
			new FC4ReadInputRegistersTask(90, Priority.LOW,
					m(PlcOfSungha.ChannelId.GAS_USAGE_10, new UnsignedWordElement(90))),
			new FC4ReadInputRegistersTask(100, Priority.LOW,
					m(PlcOfSungha.ChannelId.GAS_USAGE_11, new UnsignedWordElement(100))),
			new FC4ReadInputRegistersTask(110, Priority.LOW,
					m(PlcOfSungha.ChannelId.GAS_USAGE_12, new UnsignedWordElement(110))),
			new FC4ReadInputRegistersTask(120, Priority.LOW,
					m(PlcOfSungha.ChannelId.GAS_USAGE_13, new UnsignedWordElement(120))),
			new FC4ReadInputRegistersTask(130, Priority.LOW,
					m(PlcOfSungha.ChannelId.GAS_USAGE_14, new UnsignedWordElement(130))),
			new FC4ReadInputRegistersTask(140, Priority.LOW,
					m(PlcOfSungha.ChannelId.GAS_USAGE_15, new UnsignedWordElement(140))),
			new FC4ReadInputRegistersTask(150, Priority.LOW,
					m(PlcOfSungha.ChannelId.GAS_USAGE_16, new UnsignedWordElement(150))),
			new FC4ReadInputRegistersTask(160, Priority.LOW,
					m(PlcOfSungha.ChannelId.TEMP_BURNER_1, new SignedWordElement(160)),
					m(PlcOfSungha.ChannelId.SET_TEMP_BURNER_1, new SignedWordElement(161)),
					m(PlcOfSungha.ChannelId.TEMP_BURNER_ON_OFF_1, new UnsignedWordElement(162))),
			
			new FC4ReadInputRegistersTask(170, Priority.LOW,
					m(PlcOfSungha.ChannelId.TEMP_BURNER_9, new SignedWordElement(170)),
					m(PlcOfSungha.ChannelId.SET_TEMP_BURNER_9, new SignedWordElement(171)),
					m(PlcOfSungha.ChannelId.TEMP_BURNER_ON_OFF_9, new UnsignedWordElement(172)))
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
	Channel<Integer> getTempBurner1() {
		return this.channel(ChannelId.TEMP_BURNER_1);
	}
	Channel<Short> getTempBurnerOnOff_1() {
		return this.channel(ChannelId.TEMP_BURNER_ON_OFF_1);
	}
	Channel<Integer> getTempBurner9() {
		return this.channel(ChannelId.TEMP_BURNER_9);
	}
	Channel<Short> getTempBurnerOnOff_9() {
		return this.channel(ChannelId.TEMP_BURNER_ON_OFF_9);
	}

	

	@Override
	public String debugLog() {
		return "L:" + this.getGasUsage1().value().asString() + " / "
				+ this.getGasUsage2().value().asString() + " / "
				+ this.getGasUsage3().value().asString() + " / "
				+ this.getGasUsage4().value().asString() + " / "
				+ this.getTempBurner9().value().asString() + " / "
				+ this.getTempBurnerOnOff_9().value().asString();
	}
	
	@Override
	public MeterType getMeterType() {
		return this.config.type();
	}

}