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

import io.openems.common.OpenemsConstants;
import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;	 // uint16
import io.openems.edge.bridge.modbus.api.element.DummyRegisterElement;
import io.openems.edge.bridge.modbus.api.element.FloatDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.SignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.FloatDoublewordElement;  // uint32
import io.openems.edge.bridge.modbus.api.element.UnsignedQuadruplewordElement;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.bridge.modbus.api.task.Task;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.common.taskmanager.TasksManager;
import io.openems.edge.meter.api.AsymmetricMeter;
import io.openems.edge.meter.api.EDfemsMeter;
import io.openems.edge.meter.api.MeterType;
import io.openems.edge.meter.api.SymmetricMeter;
import io.openems.edge.meter.api.EDfemsMeter.ChannelId;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Meter.Accura2300 CT 3P", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class Accura2300 extends AbstractOpenemsModbusComponent 
	implements SymmetricMeter, AsymmetricMeter, OpenemsComponent {

	private Config config = null;

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {		
		ACCURA2300_VOLTAGE(Doc.of(OpenemsType.FLOAT)
				.unit(Unit.VOLT)),
		ACCURA2300_FREQUENCY(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.HERTZ)),
		ACCURA2350_1_CURRENT(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.AMPERE)),
//				.text(POWER_DOC_TEXT)),		
		ACCURA2350_1_ACTIVE_POWER(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.KILOWATT)),		
		ACCURA2350_1_REACTIVE_POWER(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.KILOVOLT_AMPERE_REACTIVE)),				
		ACCURA2350_2_CURRENT(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.AMPERE)),		
		ACCURA2350_2_ACTIVE_POWER(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.KILOWATT)),			
		ACCURA2350_2_REACTIVE_POWER(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.KILOVOLT_AMPERE_REACTIVE)),
		ACCURA2350_3_CURRENT(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.AMPERE)),	
		ACCURA2350_3_ACTIVE_POWER(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.KILOWATT)),		
		ACCURA2350_3_REACTIVE_POWER(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.KILOVOLT_AMPERE_REACTIVE)),
		ACCURA2350_4_CURRENT(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.AMPERE)),
		ACCURA2350_4_ACTIVE_POWER(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.KILOWATT)),		
		ACCURA2350_4_REACTIVE_POWER(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.KILOVOLT_AMPERE_REACTIVE)),
		ACCURA2350_5_CURRENT(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.AMPERE)),	
		ACCURA2350_5_ACTIVE_POWER(Doc.of(OpenemsType.FLOAT) // 해당파트의 오류 잡기 위해서 바꿈 
				.unit(Unit.KILOWATT)),		
		ACCURA2350_5_REACTIVE_POWER(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.KILOVOLT_AMPERE_REACTIVE)),
		ACCURA2350_6_CURRENT(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.AMPERE)),	
		ACCURA2350_6_ACTIVE_POWER(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.KILOWATT)),
		ACCURA2350_6_REACTIVE_POWER(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.KILOVOLT_AMPERE_REACTIVE)),
		ACCURA2350_7_CURRENT(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.AMPERE)),
		ACCURA2350_7_ACTIVE_POWER(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.KILOWATT)),		
		ACCURA2350_7_REACTIVE_POWER(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.KILOVOLT_AMPERE_REACTIVE)),				
		ACCURA2350_8_CURRENT(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.AMPERE)),		
		ACCURA2350_8_ACTIVE_POWER(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.KILOWATT)),			
		ACCURA2350_8_REACTIVE_POWER(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.KILOVOLT_AMPERE_REACTIVE)),
		ACCURA2350_9_CURRENT(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.AMPERE)),	
		ACCURA2350_9_ACTIVE_POWER(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.KILOWATT)),		
		ACCURA2350_9_REACTIVE_POWER(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.KILOVOLT_AMPERE_REACTIVE)),
		ACCURA2350_10_CURRENT(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.AMPERE)),
		ACCURA2350_10_ACTIVE_POWER(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.KILOWATT)),		
		ACCURA2350_10_REACTIVE_POWER(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.KILOVOLT_AMPERE_REACTIVE)),
		ACCURA2350_11_CURRENT(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.AMPERE)),	
		ACCURA2350_11_ACTIVE_POWER(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.KILOWATT)),		
		ACCURA2350_11_REACTIVE_POWER(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.KILOVOLT_AMPERE_REACTIVE)),
		ACCURA2350_12_CURRENT(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.AMPERE)),	
		ACCURA2350_12_ACTIVE_POWER(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.KILOWATT)),
		ACCURA2350_12_REACTIVE_POWER(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.KILOVOLT_AMPERE_REACTIVE)),
		ACCURA2350_13_CURRENT(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.AMPERE)),	
		ACCURA2350_13_ACTIVE_POWER(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.KILOWATT)),		
		ACCURA2350_13_REACTIVE_POWER(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.KILOVOLT_AMPERE_REACTIVE)),
		ACCURA2350_14_CURRENT(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.AMPERE)),	
		ACCURA2350_14_ACTIVE_POWER(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.KILOWATT)),
		ACCURA2350_14_REACTIVE_POWER(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.KILOVOLT_AMPERE_REACTIVE)),
		ACCURA2350_15_CURRENT(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.AMPERE)),	
		ACCURA2350_15_ACTIVE_POWER(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.KILOWATT)),		
		ACCURA2350_15_REACTIVE_POWER(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.KILOVOLT_AMPERE_REACTIVE)),
		ACCURA2350_16_CURRENT(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.AMPERE)),	
		ACCURA2350_16_ACTIVE_POWER(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.KILOWATT)),
		ACCURA2350_16_REACTIVE_POWER(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.KILOVOLT_AMPERE_REACTIVE)),
		ACCURA2350_17_CURRENT(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.AMPERE)),	
		ACCURA2350_17_ACTIVE_POWER(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.KILOWATT)),		
		ACCURA2350_17_REACTIVE_POWER(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.KILOVOLT_AMPERE_REACTIVE)),
		ACCURA2350_18_CURRENT(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.AMPERE)),	
		ACCURA2350_18_ACTIVE_POWER(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.KILOWATT)),
		ACCURA2350_18_REACTIVE_POWER(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.KILOVOLT_AMPERE_REACTIVE));

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
		super(
				OpenemsComponent.ChannelId.values(), //
				SymmetricMeter.ChannelId.values(),
				AsymmetricMeter.ChannelId.values(), //
//				EDfemsMeter.ChannelId.values(),
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
		this.config = config;		//  이 위치 중
		// config.sensor_num()  이 설정값은  super 에게 넘기지마라
		super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm, "Modbus",
				config.modbus_id());
		
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	

	@Override
	public String debugLog() {
		System.out.println("센서 갯 : " + this.config.sensor_num());
		
		return "L:" + this.getAccuraVoltage().value().asString() + " / "
				+ this.getAccuraFrequency().value().asString() + " / "
				+ this.getAccura2350Current01().value().asString() + " / "
				+ this.getAccura2350Active01().value().asString() + " / "
				+ this.getAccura2350Reactive01().value().asString() + " / "
				+ this.getAccura2350Current02().value().asString() + " / "
				+ this.getAccura2350Active02().value().asString() + " / "
				+ this.getAccura2350Reactive02().value().asString() + " / "
				+ this.getAccura2350Current03().value().asString() + " / "
				+ this.getAccura2350Active03().value().asString() + " / "
				+ this.getAccura2350Reactive03().value().asString() + " / "
				+ this.getAccura2350Current10().value().asString() + " / "
				+ this.getAccura2350Active10().value().asString() + " / "
				+ this.getAccura2350Reactive10().value().asString() + " / ";
	}
	
	public Channel<Float> getAccuraVoltage() {
		return this.channel(ChannelId.ACCURA2300_VOLTAGE);
	}	
	public Channel<Float> getAccuraFrequency() {
		return this.channel(ChannelId.ACCURA2300_FREQUENCY);
	}	
	public Channel<Float> getAccura2350Current01() {
		return this.channel(ChannelId.ACCURA2350_1_CURRENT);
	}	
	public Channel<Float> getAccura2350Active01() {
		return this.channel(ChannelId.ACCURA2350_1_ACTIVE_POWER);
	}	
	public Channel<Float> getAccura2350Reactive01() {
		return this.channel(ChannelId.ACCURA2350_1_REACTIVE_POWER);
	}	
	public Channel<Float> getAccura2350Current02() {
		return this.channel(ChannelId.ACCURA2350_2_CURRENT);
	}	
	public Channel<Float> getAccura2350Active02() {
		return this.channel(ChannelId.ACCURA2350_2_ACTIVE_POWER);
	}	
	public Channel<Float> getAccura2350Reactive02() {
		return this.channel(ChannelId.ACCURA2350_2_REACTIVE_POWER);
	}	
	public Channel<Float> getAccura2350Current03() {
		return this.channel(ChannelId.ACCURA2350_3_CURRENT);
	}	
	public Channel<Float> getAccura2350Active03() {
		return this.channel(ChannelId.ACCURA2350_3_ACTIVE_POWER);
	}	
	public Channel<Float> getAccura2350Reactive03() {
		return this.channel(ChannelId.ACCURA2350_3_REACTIVE_POWER);
	}
	public Channel<Float> getAccura2350Current10() {
		return this.channel(ChannelId.ACCURA2350_10_CURRENT);
	}	
	public Channel<Float> getAccura2350Active10() {
		return this.channel(ChannelId.ACCURA2350_10_ACTIVE_POWER);
	}	
	public Channel<Float> getAccura2350Reactive10() {
		return this.channel(ChannelId.ACCURA2350_10_REACTIVE_POWER);
	}
	
	
	
	@Override
	public MeterType getMeterType() {
		// TODO Auto-generated method stub
		return this.config.type();
	}
	
	@Override
	protected ModbusProtocol defineModbusProtocol() {
		// TODO implement ModbusProtocol
		// register 11045	Validity of Accura 2300[S] Voltage data, 1: Accura 2300[S] 전압 데이터가 정상적 fetch 됨.
		// register 11046 ~ +39  CT 모듈 데이터유효성체크 -1 = 비유효,  0 = 3상, 1 = 1상 
		// register 862   결선모드  1p or 3p  // 1p 와 3p 따라서 Modbus Map 다
		// register 1112   CT(Accura2350) 갯수 
		// 본 프로그램 가동전에 해당 CT 들이 정상 작동하는 지 유효성체크 후에 가동 시킨다.
		// 설명 링크 : https://blog.naver.com/kjamjalee/222093298925
		
		
		ModbusProtocol mod = null;
		
		if(this.config.sensor_num() == 18) {
			mod = new ModbusProtocol(this,					
					new FC3ReadRegistersTask(11123, Priority.LOW,	// Long 아닌 Interger 로 처리됨.
						m(Accura2300.ChannelId.ACCURA2300_VOLTAGE, new FloatDoublewordElement(11123))),
					new FC3ReadRegistersTask(11151, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2300_FREQUENCY, new FloatDoublewordElement(11151))),
					new FC3ReadRegistersTask(11207, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_1_CURRENT, new FloatDoublewordElement(11207))),
					new FC3ReadRegistersTask(11267, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_1_ACTIVE_POWER, new FloatDoublewordElement(11267))),
					new FC3ReadRegistersTask(11275, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_1_REACTIVE_POWER, new FloatDoublewordElement(11275))),
					new FC3ReadRegistersTask(11357, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_CURRENT, new FloatDoublewordElement(11357))),
					new FC3ReadRegistersTask(11417, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_ACTIVE_POWER, new FloatDoublewordElement(11417))),
					new FC3ReadRegistersTask(11425, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_REACTIVE_POWER, new FloatDoublewordElement(11425))),
					new FC3ReadRegistersTask(11507, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_3_CURRENT, new FloatDoublewordElement(11507))),
					new FC3ReadRegistersTask(11567, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_3_ACTIVE_POWER, new FloatDoublewordElement(11567))),
					new FC3ReadRegistersTask(11575, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_3_REACTIVE_POWER, new FloatDoublewordElement(11575))),
					new FC3ReadRegistersTask(11757, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_4_CURRENT, new FloatDoublewordElement(11757))),
					new FC3ReadRegistersTask(11717, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_4_ACTIVE_POWER, new FloatDoublewordElement(11717))),
					new FC3ReadRegistersTask(11725, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_4_REACTIVE_POWER, new FloatDoublewordElement(11725))),
					new FC3ReadRegistersTask(11907, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_5_CURRENT, new FloatDoublewordElement(11907))),
					new FC3ReadRegistersTask(11967, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_5_ACTIVE_POWER, new FloatDoublewordElement(11967))),
					new FC3ReadRegistersTask(11975, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_5_REACTIVE_POWER, new FloatDoublewordElement(11975))),
					new FC3ReadRegistersTask(12157, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_6_CURRENT, new FloatDoublewordElement(12157))),
					new FC3ReadRegistersTask(12117, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_6_ACTIVE_POWER, new FloatDoublewordElement(12117))),
					new FC3ReadRegistersTask(12125, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_6_REACTIVE_POWER, new FloatDoublewordElement(12125))),
					new FC3ReadRegistersTask(12307, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_7_CURRENT, new FloatDoublewordElement(12307))),
					new FC3ReadRegistersTask(12367, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_7_ACTIVE_POWER, new FloatDoublewordElement(12367))),
					new FC3ReadRegistersTask(12375, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_7_REACTIVE_POWER, new FloatDoublewordElement(12275))),
					new FC3ReadRegistersTask(12457, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_8_CURRENT, new FloatDoublewordElement(12457))),
					new FC3ReadRegistersTask(12517, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_8_ACTIVE_POWER, new FloatDoublewordElement(12517))),
					new FC3ReadRegistersTask(12525, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_8_REACTIVE_POWER, new FloatDoublewordElement(12525))),
					new FC3ReadRegistersTask(12607, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_9_CURRENT, new FloatDoublewordElement(12607))),
					new FC3ReadRegistersTask(12667, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_9_ACTIVE_POWER, new FloatDoublewordElement(12667))),
					new FC3ReadRegistersTask(12675, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_9_REACTIVE_POWER, new FloatDoublewordElement(12675))),
					new FC3ReadRegistersTask(12757, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_10_CURRENT, new FloatDoublewordElement(12757))),
					new FC3ReadRegistersTask(12817, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_10_ACTIVE_POWER, new FloatDoublewordElement(12817))),
					new FC3ReadRegistersTask(12825, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_10_REACTIVE_POWER, new FloatDoublewordElement(12825))),
					new FC3ReadRegistersTask(12907, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_11_CURRENT, new FloatDoublewordElement(12907))),
					new FC3ReadRegistersTask(12967, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_11_ACTIVE_POWER, new FloatDoublewordElement(12967))),
					new FC3ReadRegistersTask(12975, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_11_REACTIVE_POWER, new FloatDoublewordElement(12975))),
					new FC3ReadRegistersTask(13057, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_12_CURRENT, new FloatDoublewordElement(13057))),
					new FC3ReadRegistersTask(13117, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_12_ACTIVE_POWER, new FloatDoublewordElement(13117))),
					new FC3ReadRegistersTask(13125, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_12_REACTIVE_POWER, new FloatDoublewordElement(13125))),
					new FC3ReadRegistersTask(13207, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_13_CURRENT, new FloatDoublewordElement(13207))),
					new FC3ReadRegistersTask(13267, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_13_ACTIVE_POWER, new FloatDoublewordElement(13267))),
					new FC3ReadRegistersTask(13275, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_13_REACTIVE_POWER, new FloatDoublewordElement(13275))),
					new FC3ReadRegistersTask(13357, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_14_CURRENT, new FloatDoublewordElement(13357))),
					new FC3ReadRegistersTask(13417, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_14_ACTIVE_POWER, new FloatDoublewordElement(13417))),
					new FC3ReadRegistersTask(13425, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_14_REACTIVE_POWER, new FloatDoublewordElement(13425))),
					new FC3ReadRegistersTask(13507, Priority.LOW,	
						m(Accura2300.ChannelId.ACCURA2350_15_CURRENT, new FloatDoublewordElement(13507))),
					new FC3ReadRegistersTask(13567, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_15_ACTIVE_POWER, new FloatDoublewordElement(13567))),
					new FC3ReadRegistersTask(13575, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_15_REACTIVE_POWER, new FloatDoublewordElement(13575))),
					new FC3ReadRegistersTask(13657, Priority.LOW,	
						m(Accura2300.ChannelId.ACCURA2350_16_CURRENT, new FloatDoublewordElement(13657))),
					new FC3ReadRegistersTask(13717, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_16_ACTIVE_POWER, new FloatDoublewordElement(13717))),
					new FC3ReadRegistersTask(13725, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_16_REACTIVE_POWER, new FloatDoublewordElement(13725))),
					new FC3ReadRegistersTask(13807, Priority.LOW,	
						m(Accura2300.ChannelId.ACCURA2350_17_CURRENT, new FloatDoublewordElement(13807))),
					new FC3ReadRegistersTask(13867, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_17_ACTIVE_POWER, new FloatDoublewordElement(13867))),
					new FC3ReadRegistersTask(13875, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_17_REACTIVE_POWER, new FloatDoublewordElement(13875))),
					new FC3ReadRegistersTask(13957, Priority.LOW,	
						m(Accura2300.ChannelId.ACCURA2350_18_CURRENT, new FloatDoublewordElement(13957))),
					new FC3ReadRegistersTask(14017, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_18_ACTIVE_POWER, new FloatDoublewordElement(14017))),
					new FC3ReadRegistersTask(14025, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_18_REACTIVE_POWER, new FloatDoublewordElement(14025)))
				);
		}else if(this.config.sensor_num() == 17) {
			mod = new ModbusProtocol(this,					
					new FC3ReadRegistersTask(11123, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2300_VOLTAGE, new FloatDoublewordElement(11123))),
					new FC3ReadRegistersTask(11151, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2300_FREQUENCY, new FloatDoublewordElement(11151))),
					new FC3ReadRegistersTask(11207, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_1_CURRENT, new FloatDoublewordElement(11207))),
					new FC3ReadRegistersTask(11267, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_1_ACTIVE_POWER, new FloatDoublewordElement(11267))),
					new FC3ReadRegistersTask(11275, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_1_REACTIVE_POWER, new FloatDoublewordElement(11275))),
					new FC3ReadRegistersTask(11357, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_CURRENT, new FloatDoublewordElement(11357))),
					new FC3ReadRegistersTask(11417, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_ACTIVE_POWER, new FloatDoublewordElement(11417))),
					new FC3ReadRegistersTask(11425, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_REACTIVE_POWER, new FloatDoublewordElement(11425))),
					new FC3ReadRegistersTask(11507, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_3_CURRENT, new FloatDoublewordElement(11507))),
					new FC3ReadRegistersTask(11567, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_3_ACTIVE_POWER, new FloatDoublewordElement(11567))),
					new FC3ReadRegistersTask(11575, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_3_REACTIVE_POWER, new FloatDoublewordElement(11575))),
					new FC3ReadRegistersTask(11757, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_4_CURRENT, new FloatDoublewordElement(11757))),
					new FC3ReadRegistersTask(11717, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_4_ACTIVE_POWER, new FloatDoublewordElement(11717))),
					new FC3ReadRegistersTask(11725, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_4_REACTIVE_POWER, new FloatDoublewordElement(11725))),
					new FC3ReadRegistersTask(11907, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_5_CURRENT, new FloatDoublewordElement(11907))),
					new FC3ReadRegistersTask(11967, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_5_ACTIVE_POWER, new FloatDoublewordElement(11967))),
					new FC3ReadRegistersTask(11975, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_5_REACTIVE_POWER, new FloatDoublewordElement(11975))),
					new FC3ReadRegistersTask(12157, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_6_CURRENT, new FloatDoublewordElement(12157))),
					new FC3ReadRegistersTask(12117, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_6_ACTIVE_POWER, new FloatDoublewordElement(12117))),
					new FC3ReadRegistersTask(12125, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_6_REACTIVE_POWER, new FloatDoublewordElement(12125))),
					new FC3ReadRegistersTask(12307, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_7_CURRENT, new FloatDoublewordElement(12307))),
					new FC3ReadRegistersTask(12367, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_7_ACTIVE_POWER, new FloatDoublewordElement(12367))),
					new FC3ReadRegistersTask(12375, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_7_REACTIVE_POWER, new FloatDoublewordElement(12275))),
					new FC3ReadRegistersTask(12457, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_8_CURRENT, new FloatDoublewordElement(12457))),
					new FC3ReadRegistersTask(12517, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_8_ACTIVE_POWER, new FloatDoublewordElement(12517))),
					new FC3ReadRegistersTask(12525, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_8_REACTIVE_POWER, new FloatDoublewordElement(12525))),
					new FC3ReadRegistersTask(12607, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_9_CURRENT, new FloatDoublewordElement(12607))),
					new FC3ReadRegistersTask(12667, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_9_ACTIVE_POWER, new FloatDoublewordElement(12667))),
					new FC3ReadRegistersTask(12675, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_9_REACTIVE_POWER, new FloatDoublewordElement(12675))),
					new FC3ReadRegistersTask(12757, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_10_CURRENT, new FloatDoublewordElement(12757))),
					new FC3ReadRegistersTask(12817, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_10_ACTIVE_POWER, new FloatDoublewordElement(12817))),
					new FC3ReadRegistersTask(12825, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_10_REACTIVE_POWER, new FloatDoublewordElement(12825))),
					new FC3ReadRegistersTask(12907, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_11_CURRENT, new FloatDoublewordElement(12907))),
					new FC3ReadRegistersTask(12967, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_11_ACTIVE_POWER, new FloatDoublewordElement(12967))),
					new FC3ReadRegistersTask(12975, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_11_REACTIVE_POWER, new FloatDoublewordElement(12975))),
					new FC3ReadRegistersTask(13057, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_12_CURRENT, new FloatDoublewordElement(13057))),
					new FC3ReadRegistersTask(13117, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_12_ACTIVE_POWER, new FloatDoublewordElement(13117))),
					new FC3ReadRegistersTask(13125, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_12_REACTIVE_POWER, new FloatDoublewordElement(13125))),
					new FC3ReadRegistersTask(13207, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_13_CURRENT, new FloatDoublewordElement(13207))),
					new FC3ReadRegistersTask(13267, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_13_ACTIVE_POWER, new FloatDoublewordElement(13267))),
					new FC3ReadRegistersTask(13275, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_13_REACTIVE_POWER, new FloatDoublewordElement(13275))),
					new FC3ReadRegistersTask(13357, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_14_CURRENT, new FloatDoublewordElement(13357))),
					new FC3ReadRegistersTask(13417, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_14_ACTIVE_POWER, new FloatDoublewordElement(13417))),
					new FC3ReadRegistersTask(13425, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_14_REACTIVE_POWER, new FloatDoublewordElement(13425))),
					new FC3ReadRegistersTask(13507, Priority.LOW,	
						m(Accura2300.ChannelId.ACCURA2350_15_CURRENT, new FloatDoublewordElement(13507))),
					new FC3ReadRegistersTask(13567, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_15_ACTIVE_POWER, new FloatDoublewordElement(13567))),
					new FC3ReadRegistersTask(13575, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_15_REACTIVE_POWER, new FloatDoublewordElement(13575))),
					new FC3ReadRegistersTask(13657, Priority.LOW,	
						m(Accura2300.ChannelId.ACCURA2350_16_CURRENT, new FloatDoublewordElement(13657))),
					new FC3ReadRegistersTask(13717, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_16_ACTIVE_POWER, new FloatDoublewordElement(13717))),
					new FC3ReadRegistersTask(13725, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_16_REACTIVE_POWER, new FloatDoublewordElement(13725))),
					new FC3ReadRegistersTask(13807, Priority.LOW,	
						m(Accura2300.ChannelId.ACCURA2350_17_CURRENT, new FloatDoublewordElement(13807))),
					new FC3ReadRegistersTask(13867, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_17_ACTIVE_POWER, new FloatDoublewordElement(13867))),
					new FC3ReadRegistersTask(13875, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_17_REACTIVE_POWER, new FloatDoublewordElement(13875)))
				);
		}else if(this.config.sensor_num() == 16) {
			mod = new ModbusProtocol(this,					
					new FC3ReadRegistersTask(11123, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2300_VOLTAGE, new FloatDoublewordElement(11123))),
					new FC3ReadRegistersTask(11151, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2300_FREQUENCY, new FloatDoublewordElement(11151))),
					new FC3ReadRegistersTask(11207, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_1_CURRENT, new FloatDoublewordElement(11207))),
					new FC3ReadRegistersTask(11267, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_1_ACTIVE_POWER, new FloatDoublewordElement(11267))),
					new FC3ReadRegistersTask(11275, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_1_REACTIVE_POWER, new FloatDoublewordElement(11275))),
					new FC3ReadRegistersTask(11357, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_CURRENT, new FloatDoublewordElement(11357))),
					new FC3ReadRegistersTask(11417, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_ACTIVE_POWER, new FloatDoublewordElement(11417))),
					new FC3ReadRegistersTask(11425, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_REACTIVE_POWER, new FloatDoublewordElement(11425))),
					new FC3ReadRegistersTask(11507, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_3_CURRENT, new FloatDoublewordElement(11507))),
					new FC3ReadRegistersTask(11567, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_3_ACTIVE_POWER, new FloatDoublewordElement(11567))),
					new FC3ReadRegistersTask(11575, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_3_REACTIVE_POWER, new FloatDoublewordElement(11575))),
					new FC3ReadRegistersTask(11757, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_4_CURRENT, new FloatDoublewordElement(11757))),
					new FC3ReadRegistersTask(11717, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_4_ACTIVE_POWER, new FloatDoublewordElement(11717))),
					new FC3ReadRegistersTask(11725, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_4_REACTIVE_POWER, new FloatDoublewordElement(11725))),
					new FC3ReadRegistersTask(11907, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_5_CURRENT, new FloatDoublewordElement(11907))),
					new FC3ReadRegistersTask(11967, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_5_ACTIVE_POWER, new FloatDoublewordElement(11967))),
					new FC3ReadRegistersTask(11975, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_5_REACTIVE_POWER, new FloatDoublewordElement(11975))),
					new FC3ReadRegistersTask(12157, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_6_CURRENT, new FloatDoublewordElement(12157))),
					new FC3ReadRegistersTask(12117, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_6_ACTIVE_POWER, new FloatDoublewordElement(12117))),
					new FC3ReadRegistersTask(12125, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_6_REACTIVE_POWER, new FloatDoublewordElement(12125))),
					new FC3ReadRegistersTask(12307, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_7_CURRENT, new FloatDoublewordElement(12307))),
					new FC3ReadRegistersTask(12367, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_7_ACTIVE_POWER, new FloatDoublewordElement(12367))),
					new FC3ReadRegistersTask(12375, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_7_REACTIVE_POWER, new FloatDoublewordElement(12275))),
					new FC3ReadRegistersTask(12457, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_8_CURRENT, new FloatDoublewordElement(12457))),
					new FC3ReadRegistersTask(12517, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_8_ACTIVE_POWER, new FloatDoublewordElement(12517))),
					new FC3ReadRegistersTask(12525, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_8_REACTIVE_POWER, new FloatDoublewordElement(12525))),
					new FC3ReadRegistersTask(12607, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_9_CURRENT, new FloatDoublewordElement(12607))),
					new FC3ReadRegistersTask(12667, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_9_ACTIVE_POWER, new FloatDoublewordElement(12667))),
					new FC3ReadRegistersTask(12675, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_9_REACTIVE_POWER, new FloatDoublewordElement(12675))),
					new FC3ReadRegistersTask(12757, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_10_CURRENT, new FloatDoublewordElement(12757))),
					new FC3ReadRegistersTask(12817, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_10_ACTIVE_POWER, new FloatDoublewordElement(12817))),
					new FC3ReadRegistersTask(12825, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_10_REACTIVE_POWER, new FloatDoublewordElement(12825))),
					new FC3ReadRegistersTask(12907, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_11_CURRENT, new FloatDoublewordElement(12907))),
					new FC3ReadRegistersTask(12967, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_11_ACTIVE_POWER, new FloatDoublewordElement(12967))),
					new FC3ReadRegistersTask(12975, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_11_REACTIVE_POWER, new FloatDoublewordElement(12975))),
					new FC3ReadRegistersTask(13057, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_12_CURRENT, new FloatDoublewordElement(13057))),
					new FC3ReadRegistersTask(13117, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_12_ACTIVE_POWER, new FloatDoublewordElement(13117))),
					new FC3ReadRegistersTask(13125, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_12_REACTIVE_POWER, new FloatDoublewordElement(13125))),
					new FC3ReadRegistersTask(13207, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_13_CURRENT, new FloatDoublewordElement(13207))),
					new FC3ReadRegistersTask(13267, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_13_ACTIVE_POWER, new FloatDoublewordElement(13267))),
					new FC3ReadRegistersTask(13275, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_13_REACTIVE_POWER, new FloatDoublewordElement(13275))),
					new FC3ReadRegistersTask(13357, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_14_CURRENT, new FloatDoublewordElement(13357))),
					new FC3ReadRegistersTask(13417, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_14_ACTIVE_POWER, new FloatDoublewordElement(13417))),
					new FC3ReadRegistersTask(13425, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_14_REACTIVE_POWER, new FloatDoublewordElement(13425))),
					new FC3ReadRegistersTask(13507, Priority.LOW,	
						m(Accura2300.ChannelId.ACCURA2350_15_CURRENT, new FloatDoublewordElement(13507))),
					new FC3ReadRegistersTask(13567, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_15_ACTIVE_POWER, new FloatDoublewordElement(13567))),
					new FC3ReadRegistersTask(13575, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_15_REACTIVE_POWER, new FloatDoublewordElement(13575))),
					new FC3ReadRegistersTask(13657, Priority.LOW,	
						m(Accura2300.ChannelId.ACCURA2350_16_CURRENT, new FloatDoublewordElement(13657))),
					new FC3ReadRegistersTask(13717, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_16_ACTIVE_POWER, new FloatDoublewordElement(13717))),
					new FC3ReadRegistersTask(13725, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_16_REACTIVE_POWER, new FloatDoublewordElement(13725)))
				);
		}else if(this.config.sensor_num() == 15) {
			mod = new ModbusProtocol(this,					
					new FC3ReadRegistersTask(11123, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2300_VOLTAGE, new FloatDoublewordElement(11123))),
					new FC3ReadRegistersTask(11151, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2300_FREQUENCY, new FloatDoublewordElement(11151))),
					new FC3ReadRegistersTask(11207, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_1_CURRENT, new FloatDoublewordElement(11207))),
					new FC3ReadRegistersTask(11267, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_1_ACTIVE_POWER, new FloatDoublewordElement(11267))),
					new FC3ReadRegistersTask(11275, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_1_REACTIVE_POWER, new FloatDoublewordElement(11275))),
					new FC3ReadRegistersTask(11357, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_CURRENT, new FloatDoublewordElement(11357))),
					new FC3ReadRegistersTask(11417, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_ACTIVE_POWER, new FloatDoublewordElement(11417))),
					new FC3ReadRegistersTask(11425, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_REACTIVE_POWER, new FloatDoublewordElement(11425))),
					new FC3ReadRegistersTask(11507, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_3_CURRENT, new FloatDoublewordElement(11507))),
					new FC3ReadRegistersTask(11567, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_3_ACTIVE_POWER, new FloatDoublewordElement(11567))),
					new FC3ReadRegistersTask(11575, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_3_REACTIVE_POWER, new FloatDoublewordElement(11575))),
					new FC3ReadRegistersTask(11757, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_4_CURRENT, new FloatDoublewordElement(11757))),
					new FC3ReadRegistersTask(11717, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_4_ACTIVE_POWER, new FloatDoublewordElement(11717))),
					new FC3ReadRegistersTask(11725, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_4_REACTIVE_POWER, new FloatDoublewordElement(11725))),
					new FC3ReadRegistersTask(11907, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_5_CURRENT, new FloatDoublewordElement(11907))),
					new FC3ReadRegistersTask(11967, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_5_ACTIVE_POWER, new FloatDoublewordElement(11967))),
					new FC3ReadRegistersTask(11975, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_5_REACTIVE_POWER, new FloatDoublewordElement(11975))),
					new FC3ReadRegistersTask(12157, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_6_CURRENT, new FloatDoublewordElement(12157))),
					new FC3ReadRegistersTask(12117, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_6_ACTIVE_POWER, new FloatDoublewordElement(12117))),
					new FC3ReadRegistersTask(12125, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_6_REACTIVE_POWER, new FloatDoublewordElement(12125))),
					new FC3ReadRegistersTask(12307, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_7_CURRENT, new FloatDoublewordElement(12307))),
					new FC3ReadRegistersTask(12367, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_7_ACTIVE_POWER, new FloatDoublewordElement(12367))),
					new FC3ReadRegistersTask(12375, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_7_REACTIVE_POWER, new FloatDoublewordElement(12275))),
					new FC3ReadRegistersTask(12457, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_8_CURRENT, new FloatDoublewordElement(12457))),
					new FC3ReadRegistersTask(12517, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_8_ACTIVE_POWER, new FloatDoublewordElement(12517))),
					new FC3ReadRegistersTask(12525, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_8_REACTIVE_POWER, new FloatDoublewordElement(12525))),
					new FC3ReadRegistersTask(12607, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_9_CURRENT, new FloatDoublewordElement(12607))),
					new FC3ReadRegistersTask(12667, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_9_ACTIVE_POWER, new FloatDoublewordElement(12667))),
					new FC3ReadRegistersTask(12675, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_9_REACTIVE_POWER, new FloatDoublewordElement(12675))),
					new FC3ReadRegistersTask(12757, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_10_CURRENT, new FloatDoublewordElement(12757))),
					new FC3ReadRegistersTask(12817, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_10_ACTIVE_POWER, new FloatDoublewordElement(12817))),
					new FC3ReadRegistersTask(12825, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_10_REACTIVE_POWER, new FloatDoublewordElement(12825))),
					new FC3ReadRegistersTask(12907, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_11_CURRENT, new FloatDoublewordElement(12907))),
					new FC3ReadRegistersTask(12967, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_11_ACTIVE_POWER, new FloatDoublewordElement(12967))),
					new FC3ReadRegistersTask(12975, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_11_REACTIVE_POWER, new FloatDoublewordElement(12975))),
					new FC3ReadRegistersTask(13057, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_12_CURRENT, new FloatDoublewordElement(13057))),
					new FC3ReadRegistersTask(13117, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_12_ACTIVE_POWER, new FloatDoublewordElement(13117))),
					new FC3ReadRegistersTask(13125, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_12_REACTIVE_POWER, new FloatDoublewordElement(13125))),
					new FC3ReadRegistersTask(13207, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_13_CURRENT, new FloatDoublewordElement(13207))),
					new FC3ReadRegistersTask(13267, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_13_ACTIVE_POWER, new FloatDoublewordElement(13267))),
					new FC3ReadRegistersTask(13275, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_13_REACTIVE_POWER, new FloatDoublewordElement(13275))),
					new FC3ReadRegistersTask(13357, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_14_CURRENT, new FloatDoublewordElement(13357))),
					new FC3ReadRegistersTask(13417, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_14_ACTIVE_POWER, new FloatDoublewordElement(13417))),
					new FC3ReadRegistersTask(13425, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_14_REACTIVE_POWER, new FloatDoublewordElement(13425))),
					new FC3ReadRegistersTask(13507, Priority.LOW,	
						m(Accura2300.ChannelId.ACCURA2350_15_CURRENT, new FloatDoublewordElement(13507))),
					new FC3ReadRegistersTask(13567, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_15_ACTIVE_POWER, new FloatDoublewordElement(13567))),
					new FC3ReadRegistersTask(13575, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_15_REACTIVE_POWER, new FloatDoublewordElement(13575)))
				);
		}else if(this.config.sensor_num() == 14) {
			mod = new ModbusProtocol(this,					
					new FC3ReadRegistersTask(11123, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2300_VOLTAGE, new FloatDoublewordElement(11123))),
					new FC3ReadRegistersTask(11151, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2300_FREQUENCY, new FloatDoublewordElement(11151))),
					new FC3ReadRegistersTask(11207, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_1_CURRENT, new FloatDoublewordElement(11207))),
					new FC3ReadRegistersTask(11267, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_1_ACTIVE_POWER, new FloatDoublewordElement(11267))),
					new FC3ReadRegistersTask(11275, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_1_REACTIVE_POWER, new FloatDoublewordElement(11275))),
					new FC3ReadRegistersTask(11357, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_CURRENT, new FloatDoublewordElement(11357))),
					new FC3ReadRegistersTask(11417, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_ACTIVE_POWER, new FloatDoublewordElement(11417))),
					new FC3ReadRegistersTask(11425, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_REACTIVE_POWER, new FloatDoublewordElement(11425))),
					new FC3ReadRegistersTask(11507, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_3_CURRENT, new FloatDoublewordElement(11507))),
					new FC3ReadRegistersTask(11567, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_3_ACTIVE_POWER, new FloatDoublewordElement(11567))),
					new FC3ReadRegistersTask(11575, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_3_REACTIVE_POWER, new FloatDoublewordElement(11575))),
					new FC3ReadRegistersTask(11757, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_4_CURRENT, new FloatDoublewordElement(11757))),
					new FC3ReadRegistersTask(11717, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_4_ACTIVE_POWER, new FloatDoublewordElement(11717))),
					new FC3ReadRegistersTask(11725, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_4_REACTIVE_POWER, new FloatDoublewordElement(11725))),
					new FC3ReadRegistersTask(11907, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_5_CURRENT, new FloatDoublewordElement(11907))),
					new FC3ReadRegistersTask(11967, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_5_ACTIVE_POWER, new FloatDoublewordElement(11967))),
					new FC3ReadRegistersTask(11975, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_5_REACTIVE_POWER, new FloatDoublewordElement(11975))),
					new FC3ReadRegistersTask(12157, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_6_CURRENT, new FloatDoublewordElement(12157))),
					new FC3ReadRegistersTask(12117, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_6_ACTIVE_POWER, new FloatDoublewordElement(12117))),
					new FC3ReadRegistersTask(12125, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_6_REACTIVE_POWER, new FloatDoublewordElement(12125))),
					new FC3ReadRegistersTask(12307, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_7_CURRENT, new FloatDoublewordElement(12307))),
					new FC3ReadRegistersTask(12367, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_7_ACTIVE_POWER, new FloatDoublewordElement(12367))),
					new FC3ReadRegistersTask(12375, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_7_REACTIVE_POWER, new FloatDoublewordElement(12275))),
					new FC3ReadRegistersTask(12457, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_8_CURRENT, new FloatDoublewordElement(12457))),
					new FC3ReadRegistersTask(12517, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_8_ACTIVE_POWER, new FloatDoublewordElement(12517))),
					new FC3ReadRegistersTask(12525, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_8_REACTIVE_POWER, new FloatDoublewordElement(12525))),
					new FC3ReadRegistersTask(12607, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_9_CURRENT, new FloatDoublewordElement(12607))),
					new FC3ReadRegistersTask(12667, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_9_ACTIVE_POWER, new FloatDoublewordElement(12667))),
					new FC3ReadRegistersTask(12675, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_9_REACTIVE_POWER, new FloatDoublewordElement(12675))),
					new FC3ReadRegistersTask(12757, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_10_CURRENT, new FloatDoublewordElement(12757))),
					new FC3ReadRegistersTask(12817, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_10_ACTIVE_POWER, new FloatDoublewordElement(12817))),
					new FC3ReadRegistersTask(12825, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_10_REACTIVE_POWER, new FloatDoublewordElement(12825))),
					new FC3ReadRegistersTask(12907, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_11_CURRENT, new FloatDoublewordElement(12907))),
					new FC3ReadRegistersTask(12967, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_11_ACTIVE_POWER, new FloatDoublewordElement(12967))),
					new FC3ReadRegistersTask(12975, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_11_REACTIVE_POWER, new FloatDoublewordElement(12975))),
					new FC3ReadRegistersTask(13057, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_12_CURRENT, new FloatDoublewordElement(13057))),
					new FC3ReadRegistersTask(13117, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_12_ACTIVE_POWER, new FloatDoublewordElement(13117))),
					new FC3ReadRegistersTask(13125, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_12_REACTIVE_POWER, new FloatDoublewordElement(13125))),
					new FC3ReadRegistersTask(13207, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_13_CURRENT, new FloatDoublewordElement(13207))),
					new FC3ReadRegistersTask(13267, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_13_ACTIVE_POWER, new FloatDoublewordElement(13267))),
					new FC3ReadRegistersTask(13275, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_13_REACTIVE_POWER, new FloatDoublewordElement(13275))),
					new FC3ReadRegistersTask(13357, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_14_CURRENT, new FloatDoublewordElement(13357))),
					new FC3ReadRegistersTask(13417, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_14_ACTIVE_POWER, new FloatDoublewordElement(13417))),
					new FC3ReadRegistersTask(13425, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_14_REACTIVE_POWER, new FloatDoublewordElement(13425)))
				);
		}else if(this.config.sensor_num() == 13) {
			mod = new ModbusProtocol(this,					
					new FC3ReadRegistersTask(11123, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2300_VOLTAGE, new FloatDoublewordElement(11123))),
					new FC3ReadRegistersTask(11151, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2300_FREQUENCY, new FloatDoublewordElement(11151))),
					new FC3ReadRegistersTask(11207, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_1_CURRENT, new FloatDoublewordElement(11207))),
					new FC3ReadRegistersTask(11267, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_1_ACTIVE_POWER, new FloatDoublewordElement(11267))),
					new FC3ReadRegistersTask(11275, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_1_REACTIVE_POWER, new FloatDoublewordElement(11275))),
					new FC3ReadRegistersTask(11357, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_CURRENT, new FloatDoublewordElement(11357))),
					new FC3ReadRegistersTask(11417, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_ACTIVE_POWER, new FloatDoublewordElement(11417))),
					new FC3ReadRegistersTask(11425, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_REACTIVE_POWER, new FloatDoublewordElement(11425))),
					new FC3ReadRegistersTask(11507, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_3_CURRENT, new FloatDoublewordElement(11507))),
					new FC3ReadRegistersTask(11567, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_3_ACTIVE_POWER, new FloatDoublewordElement(11567))),
					new FC3ReadRegistersTask(11575, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_3_REACTIVE_POWER, new FloatDoublewordElement(11575))),
					new FC3ReadRegistersTask(11757, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_4_CURRENT, new FloatDoublewordElement(11757))),
					new FC3ReadRegistersTask(11717, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_4_ACTIVE_POWER, new FloatDoublewordElement(11717))),
					new FC3ReadRegistersTask(11725, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_4_REACTIVE_POWER, new FloatDoublewordElement(11725))),
					new FC3ReadRegistersTask(11907, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_5_CURRENT, new FloatDoublewordElement(11907))),
					new FC3ReadRegistersTask(11967, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_5_ACTIVE_POWER, new FloatDoublewordElement(11967))),
					new FC3ReadRegistersTask(11975, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_5_REACTIVE_POWER, new FloatDoublewordElement(11975))),
					new FC3ReadRegistersTask(12157, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_6_CURRENT, new FloatDoublewordElement(12157))),
					new FC3ReadRegistersTask(12117, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_6_ACTIVE_POWER, new FloatDoublewordElement(12117))),
					new FC3ReadRegistersTask(12125, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_6_REACTIVE_POWER, new FloatDoublewordElement(12125))),
					new FC3ReadRegistersTask(12307, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_7_CURRENT, new FloatDoublewordElement(12307))),
					new FC3ReadRegistersTask(12367, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_7_ACTIVE_POWER, new FloatDoublewordElement(12367))),
					new FC3ReadRegistersTask(12375, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_7_REACTIVE_POWER, new FloatDoublewordElement(12275))),
					new FC3ReadRegistersTask(12457, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_8_CURRENT, new FloatDoublewordElement(12457))),
					new FC3ReadRegistersTask(12517, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_8_ACTIVE_POWER, new FloatDoublewordElement(12517))),
					new FC3ReadRegistersTask(12525, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_8_REACTIVE_POWER, new FloatDoublewordElement(12525))),
					new FC3ReadRegistersTask(12607, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_9_CURRENT, new FloatDoublewordElement(12607))),
					new FC3ReadRegistersTask(12667, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_9_ACTIVE_POWER, new FloatDoublewordElement(12667))),
					new FC3ReadRegistersTask(12675, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_9_REACTIVE_POWER, new FloatDoublewordElement(12675))),
					new FC3ReadRegistersTask(12757, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_10_CURRENT, new FloatDoublewordElement(12757))),
					new FC3ReadRegistersTask(12817, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_10_ACTIVE_POWER, new FloatDoublewordElement(12817))),
					new FC3ReadRegistersTask(12825, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_10_REACTIVE_POWER, new FloatDoublewordElement(12825))),
					new FC3ReadRegistersTask(12907, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_11_CURRENT, new FloatDoublewordElement(12907))),
					new FC3ReadRegistersTask(12967, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_11_ACTIVE_POWER, new FloatDoublewordElement(12967))),
					new FC3ReadRegistersTask(12975, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_11_REACTIVE_POWER, new FloatDoublewordElement(12975))),
					new FC3ReadRegistersTask(13057, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_12_CURRENT, new FloatDoublewordElement(13057))),
					new FC3ReadRegistersTask(13117, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_12_ACTIVE_POWER, new FloatDoublewordElement(13117))),
					new FC3ReadRegistersTask(13125, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_12_REACTIVE_POWER, new FloatDoublewordElement(13125))),
					new FC3ReadRegistersTask(13207, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_13_CURRENT, new FloatDoublewordElement(13207))),
					new FC3ReadRegistersTask(13267, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_13_ACTIVE_POWER, new FloatDoublewordElement(13267))),
					new FC3ReadRegistersTask(13275, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_13_REACTIVE_POWER, new FloatDoublewordElement(13275)))
				);
		}else if(this.config.sensor_num() == 12) {
			mod = new ModbusProtocol(this,					
					new FC3ReadRegistersTask(11123, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2300_VOLTAGE, new FloatDoublewordElement(11123))),
					new FC3ReadRegistersTask(11151, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2300_FREQUENCY, new FloatDoublewordElement(11151))),
					new FC3ReadRegistersTask(11207, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_1_CURRENT, new FloatDoublewordElement(11207))),
					new FC3ReadRegistersTask(11267, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_1_ACTIVE_POWER, new FloatDoublewordElement(11267))),
					new FC3ReadRegistersTask(11275, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_1_REACTIVE_POWER, new FloatDoublewordElement(11275))),
					new FC3ReadRegistersTask(11357, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_CURRENT, new FloatDoublewordElement(11357))),
					new FC3ReadRegistersTask(11417, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_ACTIVE_POWER, new FloatDoublewordElement(11417))),
					new FC3ReadRegistersTask(11425, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_REACTIVE_POWER, new FloatDoublewordElement(11425))),
					new FC3ReadRegistersTask(11507, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_3_CURRENT, new FloatDoublewordElement(11507))),
					new FC3ReadRegistersTask(11567, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_3_ACTIVE_POWER, new FloatDoublewordElement(11567))),
					new FC3ReadRegistersTask(11575, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_3_REACTIVE_POWER, new FloatDoublewordElement(11575))),
					new FC3ReadRegistersTask(11757, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_4_CURRENT, new FloatDoublewordElement(11757))),
					new FC3ReadRegistersTask(11717, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_4_ACTIVE_POWER, new FloatDoublewordElement(11717))),
					new FC3ReadRegistersTask(11725, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_4_REACTIVE_POWER, new FloatDoublewordElement(11725))),
					new FC3ReadRegistersTask(11907, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_5_CURRENT, new FloatDoublewordElement(11907))),
					new FC3ReadRegistersTask(11967, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_5_ACTIVE_POWER, new FloatDoublewordElement(11967))),
					new FC3ReadRegistersTask(11975, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_5_REACTIVE_POWER, new FloatDoublewordElement(11975))),
					new FC3ReadRegistersTask(12157, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_6_CURRENT, new FloatDoublewordElement(12157))),
					new FC3ReadRegistersTask(12117, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_6_ACTIVE_POWER, new FloatDoublewordElement(12117))),
					new FC3ReadRegistersTask(12125, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_6_REACTIVE_POWER, new FloatDoublewordElement(12125))),
					new FC3ReadRegistersTask(12307, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_7_CURRENT, new FloatDoublewordElement(12307))),
					new FC3ReadRegistersTask(12367, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_7_ACTIVE_POWER, new FloatDoublewordElement(12367))),
					new FC3ReadRegistersTask(12375, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_7_REACTIVE_POWER, new FloatDoublewordElement(12275))),
					new FC3ReadRegistersTask(12457, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_8_CURRENT, new FloatDoublewordElement(12457))),
					new FC3ReadRegistersTask(12517, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_8_ACTIVE_POWER, new FloatDoublewordElement(12517))),
					new FC3ReadRegistersTask(12525, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_8_REACTIVE_POWER, new FloatDoublewordElement(12525))),
					new FC3ReadRegistersTask(12607, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_9_CURRENT, new FloatDoublewordElement(12607))),
					new FC3ReadRegistersTask(12667, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_9_ACTIVE_POWER, new FloatDoublewordElement(12667))),
					new FC3ReadRegistersTask(12675, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_9_REACTIVE_POWER, new FloatDoublewordElement(12675))),
					new FC3ReadRegistersTask(12757, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_10_CURRENT, new FloatDoublewordElement(12757))),
					new FC3ReadRegistersTask(12817, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_10_ACTIVE_POWER, new FloatDoublewordElement(12817))),
					new FC3ReadRegistersTask(12825, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_10_REACTIVE_POWER, new FloatDoublewordElement(12825))),
					new FC3ReadRegistersTask(12907, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_11_CURRENT, new FloatDoublewordElement(12907))),
					new FC3ReadRegistersTask(12967, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_11_ACTIVE_POWER, new FloatDoublewordElement(12967))),
					new FC3ReadRegistersTask(12975, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_11_REACTIVE_POWER, new FloatDoublewordElement(12975))),
					new FC3ReadRegistersTask(13057, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_12_CURRENT, new FloatDoublewordElement(13057))),
					new FC3ReadRegistersTask(13117, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_12_ACTIVE_POWER, new FloatDoublewordElement(13117))),
					new FC3ReadRegistersTask(13125, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_12_REACTIVE_POWER, new FloatDoublewordElement(13125)))
				);
		}else if(this.config.sensor_num() == 11) {
			mod = new ModbusProtocol(this,					
					new FC3ReadRegistersTask(11123, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2300_VOLTAGE, new FloatDoublewordElement(11123))),
					new FC3ReadRegistersTask(11151, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2300_FREQUENCY, new FloatDoublewordElement(11151))),
					new FC3ReadRegistersTask(11207, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_1_CURRENT, new FloatDoublewordElement(11207))),
					new FC3ReadRegistersTask(11267, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_1_ACTIVE_POWER, new FloatDoublewordElement(11267))),
					new FC3ReadRegistersTask(11275, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_1_REACTIVE_POWER, new FloatDoublewordElement(11275))),
					new FC3ReadRegistersTask(11357, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_CURRENT, new FloatDoublewordElement(11357))),
					new FC3ReadRegistersTask(11417, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_ACTIVE_POWER, new FloatDoublewordElement(11417))),
					new FC3ReadRegistersTask(11425, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_REACTIVE_POWER, new FloatDoublewordElement(11425))),
					new FC3ReadRegistersTask(11507, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_3_CURRENT, new FloatDoublewordElement(11507))),
					new FC3ReadRegistersTask(11567, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_3_ACTIVE_POWER, new FloatDoublewordElement(11567))),
					new FC3ReadRegistersTask(11575, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_3_REACTIVE_POWER, new FloatDoublewordElement(11575))),
					new FC3ReadRegistersTask(11757, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_4_CURRENT, new FloatDoublewordElement(11757))),
					new FC3ReadRegistersTask(11717, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_4_ACTIVE_POWER, new FloatDoublewordElement(11717))),
					new FC3ReadRegistersTask(11725, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_4_REACTIVE_POWER, new FloatDoublewordElement(11725))),
					new FC3ReadRegistersTask(11907, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_5_CURRENT, new FloatDoublewordElement(11907))),
					new FC3ReadRegistersTask(11967, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_5_ACTIVE_POWER, new FloatDoublewordElement(11967))),
					new FC3ReadRegistersTask(11975, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_5_REACTIVE_POWER, new FloatDoublewordElement(11975))),
					new FC3ReadRegistersTask(12157, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_6_CURRENT, new FloatDoublewordElement(12157))),
					new FC3ReadRegistersTask(12117, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_6_ACTIVE_POWER, new FloatDoublewordElement(12117))),
					new FC3ReadRegistersTask(12125, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_6_REACTIVE_POWER, new FloatDoublewordElement(12125))),
					new FC3ReadRegistersTask(12307, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_7_CURRENT, new FloatDoublewordElement(12307))),
					new FC3ReadRegistersTask(12367, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_7_ACTIVE_POWER, new FloatDoublewordElement(12367))),
					new FC3ReadRegistersTask(12375, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_7_REACTIVE_POWER, new FloatDoublewordElement(12275))),
					new FC3ReadRegistersTask(12457, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_8_CURRENT, new FloatDoublewordElement(12457))),
					new FC3ReadRegistersTask(12517, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_8_ACTIVE_POWER, new FloatDoublewordElement(12517))),
					new FC3ReadRegistersTask(12525, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_8_REACTIVE_POWER, new FloatDoublewordElement(12525))),
					new FC3ReadRegistersTask(12607, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_9_CURRENT, new FloatDoublewordElement(12607))),
					new FC3ReadRegistersTask(12667, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_9_ACTIVE_POWER, new FloatDoublewordElement(12667))),
					new FC3ReadRegistersTask(12675, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_9_REACTIVE_POWER, new FloatDoublewordElement(12675))),
					new FC3ReadRegistersTask(12757, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_10_CURRENT, new FloatDoublewordElement(12757))),
					new FC3ReadRegistersTask(12817, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_10_ACTIVE_POWER, new FloatDoublewordElement(12817))),
					new FC3ReadRegistersTask(12825, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_10_REACTIVE_POWER, new FloatDoublewordElement(12825))),
					new FC3ReadRegistersTask(12907, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_11_CURRENT, new FloatDoublewordElement(12907))),
					new FC3ReadRegistersTask(12967, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_11_ACTIVE_POWER, new FloatDoublewordElement(12967))),
					new FC3ReadRegistersTask(12975, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_11_REACTIVE_POWER, new FloatDoublewordElement(12975)))
				);
		}else if(this.config.sensor_num() == 10) {
			mod = new ModbusProtocol(this,					
					new FC3ReadRegistersTask(11123, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2300_VOLTAGE, new FloatDoublewordElement(11123))),
					new FC3ReadRegistersTask(11151, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2300_FREQUENCY, new FloatDoublewordElement(11151))),
					new FC3ReadRegistersTask(11207, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_1_CURRENT, new FloatDoublewordElement(11207))),
					new FC3ReadRegistersTask(11267, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_1_ACTIVE_POWER, new FloatDoublewordElement(11267))),
					new FC3ReadRegistersTask(11275, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_1_REACTIVE_POWER, new FloatDoublewordElement(11275))),
					new FC3ReadRegistersTask(11357, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_CURRENT, new FloatDoublewordElement(11357))),
					new FC3ReadRegistersTask(11417, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_ACTIVE_POWER, new FloatDoublewordElement(11417))),
					new FC3ReadRegistersTask(11425, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_REACTIVE_POWER, new FloatDoublewordElement(11425))),
					new FC3ReadRegistersTask(11507, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_3_CURRENT, new FloatDoublewordElement(11507))),
					new FC3ReadRegistersTask(11567, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_3_ACTIVE_POWER, new FloatDoublewordElement(11567))),
					new FC3ReadRegistersTask(11575, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_3_REACTIVE_POWER, new FloatDoublewordElement(11575))),
					new FC3ReadRegistersTask(11757, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_4_CURRENT, new FloatDoublewordElement(11757))),
					new FC3ReadRegistersTask(11717, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_4_ACTIVE_POWER, new FloatDoublewordElement(11717))),
					new FC3ReadRegistersTask(11725, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_4_REACTIVE_POWER, new FloatDoublewordElement(11725))),
					new FC3ReadRegistersTask(11907, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_5_CURRENT, new FloatDoublewordElement(11907))),
					new FC3ReadRegistersTask(11967, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_5_ACTIVE_POWER, new FloatDoublewordElement(11967))),
					new FC3ReadRegistersTask(11975, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_5_REACTIVE_POWER, new FloatDoublewordElement(11975))),
					new FC3ReadRegistersTask(12157, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_6_CURRENT, new FloatDoublewordElement(12157))),
					new FC3ReadRegistersTask(12117, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_6_ACTIVE_POWER, new FloatDoublewordElement(12117))),
					new FC3ReadRegistersTask(12125, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_6_REACTIVE_POWER, new FloatDoublewordElement(12125))),
					new FC3ReadRegistersTask(12307, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_7_CURRENT, new FloatDoublewordElement(12307))),
					new FC3ReadRegistersTask(12367, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_7_ACTIVE_POWER, new FloatDoublewordElement(12367))),
					new FC3ReadRegistersTask(12375, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_7_REACTIVE_POWER, new FloatDoublewordElement(12275))),
					new FC3ReadRegistersTask(12457, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_8_CURRENT, new FloatDoublewordElement(12457))),
					new FC3ReadRegistersTask(12517, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_8_ACTIVE_POWER, new FloatDoublewordElement(12517))),
					new FC3ReadRegistersTask(12525, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_8_REACTIVE_POWER, new FloatDoublewordElement(12525))),
					new FC3ReadRegistersTask(12607, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_9_CURRENT, new FloatDoublewordElement(12607))),
					new FC3ReadRegistersTask(12667, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_9_ACTIVE_POWER, new FloatDoublewordElement(12667))),
					new FC3ReadRegistersTask(12675, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_9_REACTIVE_POWER, new FloatDoublewordElement(12675))),
					new FC3ReadRegistersTask(12757, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_10_CURRENT, new FloatDoublewordElement(12757))),
					new FC3ReadRegistersTask(12817, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_10_ACTIVE_POWER, new FloatDoublewordElement(12817))),
					new FC3ReadRegistersTask(12825, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_10_REACTIVE_POWER, new FloatDoublewordElement(12825)))
				);
		}else if(this.config.sensor_num() == 9) {
			mod = new ModbusProtocol(this,					
					new FC3ReadRegistersTask(11123, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2300_VOLTAGE, new FloatDoublewordElement(11123))),
					new FC3ReadRegistersTask(11151, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2300_FREQUENCY, new FloatDoublewordElement(11151))),
					new FC3ReadRegistersTask(11207, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_1_CURRENT, new FloatDoublewordElement(11207))),
					new FC3ReadRegistersTask(11267, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_1_ACTIVE_POWER, new FloatDoublewordElement(11267))),
					new FC3ReadRegistersTask(11275, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_1_REACTIVE_POWER, new FloatDoublewordElement(11275))),
					new FC3ReadRegistersTask(11357, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_CURRENT, new FloatDoublewordElement(11357))),
					new FC3ReadRegistersTask(11417, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_ACTIVE_POWER, new FloatDoublewordElement(11417))),
					new FC3ReadRegistersTask(11425, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_REACTIVE_POWER, new FloatDoublewordElement(11425))),
					new FC3ReadRegistersTask(11507, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_3_CURRENT, new FloatDoublewordElement(11507))),
					new FC3ReadRegistersTask(11567, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_3_ACTIVE_POWER, new FloatDoublewordElement(11567))),
					new FC3ReadRegistersTask(11575, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_3_REACTIVE_POWER, new FloatDoublewordElement(11575))),
					new FC3ReadRegistersTask(11757, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_4_CURRENT, new FloatDoublewordElement(11757))),
					new FC3ReadRegistersTask(11717, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_4_ACTIVE_POWER, new FloatDoublewordElement(11717))),
					new FC3ReadRegistersTask(11725, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_4_REACTIVE_POWER, new FloatDoublewordElement(11725))),
					new FC3ReadRegistersTask(11907, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_5_CURRENT, new FloatDoublewordElement(11907))),
					new FC3ReadRegistersTask(11967, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_5_ACTIVE_POWER, new FloatDoublewordElement(11967))),
					new FC3ReadRegistersTask(11975, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_5_REACTIVE_POWER, new FloatDoublewordElement(11975))),
					new FC3ReadRegistersTask(12157, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_6_CURRENT, new FloatDoublewordElement(12157))),
					new FC3ReadRegistersTask(12117, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_6_ACTIVE_POWER, new FloatDoublewordElement(12117))),
					new FC3ReadRegistersTask(12125, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_6_REACTIVE_POWER, new FloatDoublewordElement(12125))),
					new FC3ReadRegistersTask(12307, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_7_CURRENT, new FloatDoublewordElement(12307))),
					new FC3ReadRegistersTask(12367, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_7_ACTIVE_POWER, new FloatDoublewordElement(12367))),
					new FC3ReadRegistersTask(12375, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_7_REACTIVE_POWER, new FloatDoublewordElement(12275))),
					new FC3ReadRegistersTask(12457, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_8_CURRENT, new FloatDoublewordElement(12457))),
					new FC3ReadRegistersTask(12517, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_8_ACTIVE_POWER, new FloatDoublewordElement(12517))),
					new FC3ReadRegistersTask(12525, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_8_REACTIVE_POWER, new FloatDoublewordElement(12525))),
					new FC3ReadRegistersTask(12607, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_9_CURRENT, new FloatDoublewordElement(12607))),
					new FC3ReadRegistersTask(12667, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_9_ACTIVE_POWER, new FloatDoublewordElement(12667))),
					new FC3ReadRegistersTask(12675, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_9_REACTIVE_POWER, new FloatDoublewordElement(12675)))
				);
		}else if(this.config.sensor_num() == 8) {
			mod = new ModbusProtocol(this,					
					new FC3ReadRegistersTask(11123, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2300_VOLTAGE, new FloatDoublewordElement(11123))),
					new FC3ReadRegistersTask(11151, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2300_FREQUENCY, new FloatDoublewordElement(11151))),
					new FC3ReadRegistersTask(11207, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_1_CURRENT, new FloatDoublewordElement(11207))),
					new FC3ReadRegistersTask(11267, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_1_ACTIVE_POWER, new FloatDoublewordElement(11267))),
					new FC3ReadRegistersTask(11275, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_1_REACTIVE_POWER, new FloatDoublewordElement(11275))),
					new FC3ReadRegistersTask(11357, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_CURRENT, new FloatDoublewordElement(11357))),
					new FC3ReadRegistersTask(11417, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_ACTIVE_POWER, new FloatDoublewordElement(11417))),
					new FC3ReadRegistersTask(11425, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_REACTIVE_POWER, new FloatDoublewordElement(11425))),
					new FC3ReadRegistersTask(11507, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_3_CURRENT, new FloatDoublewordElement(11507))),
					new FC3ReadRegistersTask(11567, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_3_ACTIVE_POWER, new FloatDoublewordElement(11567))),
					new FC3ReadRegistersTask(11575, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_3_REACTIVE_POWER, new FloatDoublewordElement(11575))),
					new FC3ReadRegistersTask(11757, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_4_CURRENT, new FloatDoublewordElement(11757))),
					new FC3ReadRegistersTask(11717, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_4_ACTIVE_POWER, new FloatDoublewordElement(11717))),
					new FC3ReadRegistersTask(11725, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_4_REACTIVE_POWER, new FloatDoublewordElement(11725))),
					new FC3ReadRegistersTask(11907, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_5_CURRENT, new FloatDoublewordElement(11907))),
					new FC3ReadRegistersTask(11967, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_5_ACTIVE_POWER, new FloatDoublewordElement(11967))),
					new FC3ReadRegistersTask(11975, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_5_REACTIVE_POWER, new FloatDoublewordElement(11975))),
					new FC3ReadRegistersTask(12157, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_6_CURRENT, new FloatDoublewordElement(12157))),
					new FC3ReadRegistersTask(12117, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_6_ACTIVE_POWER, new FloatDoublewordElement(12117))),
					new FC3ReadRegistersTask(12125, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_6_REACTIVE_POWER, new FloatDoublewordElement(12125))),
					new FC3ReadRegistersTask(12307, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_7_CURRENT, new FloatDoublewordElement(12307))),
					new FC3ReadRegistersTask(12367, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_7_ACTIVE_POWER, new FloatDoublewordElement(12367))),
					new FC3ReadRegistersTask(12375, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_7_REACTIVE_POWER, new FloatDoublewordElement(12275))),
					new FC3ReadRegistersTask(12457, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_8_CURRENT, new FloatDoublewordElement(12457))),
					new FC3ReadRegistersTask(12517, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_8_ACTIVE_POWER, new FloatDoublewordElement(12517))),
					new FC3ReadRegistersTask(12525, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_8_REACTIVE_POWER, new FloatDoublewordElement(12525)))
				);
		}else if(this.config.sensor_num() == 7) {
			mod = new ModbusProtocol(this,					
					new FC3ReadRegistersTask(11123, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2300_VOLTAGE, new FloatDoublewordElement(11123))),
					new FC3ReadRegistersTask(11151, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2300_FREQUENCY, new FloatDoublewordElement(11151))),
					new FC3ReadRegistersTask(11207, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_1_CURRENT, new FloatDoublewordElement(11207))),
					new FC3ReadRegistersTask(11267, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_1_ACTIVE_POWER, new FloatDoublewordElement(11267))),
					new FC3ReadRegistersTask(11275, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_1_REACTIVE_POWER, new FloatDoublewordElement(11275))),
					new FC3ReadRegistersTask(11357, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_CURRENT, new FloatDoublewordElement(11357))),
					new FC3ReadRegistersTask(11417, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_ACTIVE_POWER, new FloatDoublewordElement(11417))),
					new FC3ReadRegistersTask(11425, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_REACTIVE_POWER, new FloatDoublewordElement(11425))),
					new FC3ReadRegistersTask(11507, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_3_CURRENT, new FloatDoublewordElement(11507))),
					new FC3ReadRegistersTask(11567, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_3_ACTIVE_POWER, new FloatDoublewordElement(11567))),
					new FC3ReadRegistersTask(11575, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_3_REACTIVE_POWER, new FloatDoublewordElement(11575))),
					new FC3ReadRegistersTask(11757, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_4_CURRENT, new FloatDoublewordElement(11757))),
					new FC3ReadRegistersTask(11717, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_4_ACTIVE_POWER, new FloatDoublewordElement(11717))),
					new FC3ReadRegistersTask(11725, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_4_REACTIVE_POWER, new FloatDoublewordElement(11725))),
					new FC3ReadRegistersTask(11907, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_5_CURRENT, new FloatDoublewordElement(11907))),
					new FC3ReadRegistersTask(11967, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_5_ACTIVE_POWER, new FloatDoublewordElement(11967))),
					new FC3ReadRegistersTask(11975, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_5_REACTIVE_POWER, new FloatDoublewordElement(11975))),
					new FC3ReadRegistersTask(12157, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_6_CURRENT, new FloatDoublewordElement(12157))),
					new FC3ReadRegistersTask(12117, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_6_ACTIVE_POWER, new FloatDoublewordElement(12117))),
					new FC3ReadRegistersTask(12125, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_6_REACTIVE_POWER, new FloatDoublewordElement(12125))),
					new FC3ReadRegistersTask(12307, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_7_CURRENT, new FloatDoublewordElement(12307))),
					new FC3ReadRegistersTask(12367, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_7_ACTIVE_POWER, new FloatDoublewordElement(12367))),
					new FC3ReadRegistersTask(12375, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_7_REACTIVE_POWER, new FloatDoublewordElement(12275)))
				);
		}else if(this.config.sensor_num() == 6) {
			mod = new ModbusProtocol(this,					
					new FC3ReadRegistersTask(11123, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2300_VOLTAGE, new FloatDoublewordElement(11123))),
					new FC3ReadRegistersTask(11151, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2300_FREQUENCY, new FloatDoublewordElement(11151))),
					new FC3ReadRegistersTask(11207, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_1_CURRENT, new FloatDoublewordElement(11207))),
					new FC3ReadRegistersTask(11267, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_1_ACTIVE_POWER, new FloatDoublewordElement(11267))),
					new FC3ReadRegistersTask(11275, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_1_REACTIVE_POWER, new FloatDoublewordElement(11275))),
					new FC3ReadRegistersTask(11357, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_CURRENT, new FloatDoublewordElement(11357))),
					new FC3ReadRegistersTask(11417, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_ACTIVE_POWER, new FloatDoublewordElement(11417))),
					new FC3ReadRegistersTask(11425, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_REACTIVE_POWER, new FloatDoublewordElement(11425))),
					new FC3ReadRegistersTask(11507, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_3_CURRENT, new FloatDoublewordElement(11507))),
					new FC3ReadRegistersTask(11567, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_3_ACTIVE_POWER, new FloatDoublewordElement(11567))),
					new FC3ReadRegistersTask(11575, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_3_REACTIVE_POWER, new FloatDoublewordElement(11575))),
					new FC3ReadRegistersTask(11757, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_4_CURRENT, new FloatDoublewordElement(11757))),
					new FC3ReadRegistersTask(11717, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_4_ACTIVE_POWER, new FloatDoublewordElement(11717))),
					new FC3ReadRegistersTask(11725, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_4_REACTIVE_POWER, new FloatDoublewordElement(11725))),
					new FC3ReadRegistersTask(11907, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_5_CURRENT, new FloatDoublewordElement(11907))),
					new FC3ReadRegistersTask(11967, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_5_ACTIVE_POWER, new FloatDoublewordElement(11967))),
					new FC3ReadRegistersTask(11975, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_5_REACTIVE_POWER, new FloatDoublewordElement(11975))),
					new FC3ReadRegistersTask(12157, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_6_CURRENT, new FloatDoublewordElement(12157))),
					new FC3ReadRegistersTask(12117, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_6_ACTIVE_POWER, new FloatDoublewordElement(12117))),
					new FC3ReadRegistersTask(12125, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_6_REACTIVE_POWER, new FloatDoublewordElement(12125)))
				);
		}else if(this.config.sensor_num() == 5) {
			mod = new ModbusProtocol(this,					
					new FC3ReadRegistersTask(11123, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2300_VOLTAGE, new FloatDoublewordElement(11123))),
					new FC3ReadRegistersTask(11151, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2300_FREQUENCY, new FloatDoublewordElement(11151))),
					new FC3ReadRegistersTask(11207, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_1_CURRENT, new FloatDoublewordElement(11207))),
					new FC3ReadRegistersTask(11267, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_1_ACTIVE_POWER, new FloatDoublewordElement(11267))),
					new FC3ReadRegistersTask(11275, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_1_REACTIVE_POWER, new FloatDoublewordElement(11275))),
					new FC3ReadRegistersTask(11357, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_CURRENT, new FloatDoublewordElement(11357))),
					new FC3ReadRegistersTask(11417, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_ACTIVE_POWER, new FloatDoublewordElement(11417))),
					new FC3ReadRegistersTask(11425, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_REACTIVE_POWER, new FloatDoublewordElement(11425))),
					new FC3ReadRegistersTask(11507, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_3_CURRENT, new FloatDoublewordElement(11507))),
					new FC3ReadRegistersTask(11567, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_3_ACTIVE_POWER, new FloatDoublewordElement(11567))),
					new FC3ReadRegistersTask(11575, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_3_REACTIVE_POWER, new FloatDoublewordElement(11575))),
					new FC3ReadRegistersTask(11757, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_4_CURRENT, new FloatDoublewordElement(11757))),
					new FC3ReadRegistersTask(11717, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_4_ACTIVE_POWER, new FloatDoublewordElement(11717))),
					new FC3ReadRegistersTask(11725, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_4_REACTIVE_POWER, new FloatDoublewordElement(11725))),
					new FC3ReadRegistersTask(11907, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_5_CURRENT, new FloatDoublewordElement(11907))),
					new FC3ReadRegistersTask(11967, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_5_ACTIVE_POWER, new FloatDoublewordElement(11967))),
					new FC3ReadRegistersTask(11975, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_5_REACTIVE_POWER, new FloatDoublewordElement(11975)))
				);
		}else if(this.config.sensor_num() == 4) {
			mod = new ModbusProtocol(this,					
					new FC3ReadRegistersTask(11123, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2300_VOLTAGE, new FloatDoublewordElement(11123))),
					new FC3ReadRegistersTask(11151, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2300_FREQUENCY, new FloatDoublewordElement(11151))),
					new FC3ReadRegistersTask(11207, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_1_CURRENT, new FloatDoublewordElement(11207))),
					new FC3ReadRegistersTask(11267, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_1_ACTIVE_POWER, new FloatDoublewordElement(11267))),
					new FC3ReadRegistersTask(11275, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_1_REACTIVE_POWER, new FloatDoublewordElement(11275))),
					new FC3ReadRegistersTask(11357, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_CURRENT, new FloatDoublewordElement(11357))),
					new FC3ReadRegistersTask(11417, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_ACTIVE_POWER, new FloatDoublewordElement(11417))),
					new FC3ReadRegistersTask(11425, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_REACTIVE_POWER, new FloatDoublewordElement(11425))),
					new FC3ReadRegistersTask(11507, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_3_CURRENT, new FloatDoublewordElement(11507))),
					new FC3ReadRegistersTask(11567, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_3_ACTIVE_POWER, new FloatDoublewordElement(11567))),
					new FC3ReadRegistersTask(11575, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_3_REACTIVE_POWER, new FloatDoublewordElement(11575))),
					new FC3ReadRegistersTask(11757, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_4_CURRENT, new FloatDoublewordElement(11757))),
					new FC3ReadRegistersTask(11717, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_4_ACTIVE_POWER, new FloatDoublewordElement(11717))),
					new FC3ReadRegistersTask(11725, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_4_REACTIVE_POWER, new FloatDoublewordElement(11725)))
				);
		}else if(this.config.sensor_num() == 3) {
			mod = new ModbusProtocol(this,					
					new FC3ReadRegistersTask(11123, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2300_VOLTAGE, new FloatDoublewordElement(11123))),
					new FC3ReadRegistersTask(11151, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2300_FREQUENCY, new FloatDoublewordElement(11151))),
					new FC3ReadRegistersTask(11207, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_1_CURRENT, new FloatDoublewordElement(11207))),
					new FC3ReadRegistersTask(11267, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_1_ACTIVE_POWER, new FloatDoublewordElement(11267))),
					new FC3ReadRegistersTask(11275, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_1_REACTIVE_POWER, new FloatDoublewordElement(11275))),
					new FC3ReadRegistersTask(11357, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_CURRENT, new FloatDoublewordElement(11357))),
					new FC3ReadRegistersTask(11417, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_ACTIVE_POWER, new FloatDoublewordElement(11417))),
					new FC3ReadRegistersTask(11425, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_REACTIVE_POWER, new FloatDoublewordElement(11425))),
					new FC3ReadRegistersTask(11507, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_3_CURRENT, new FloatDoublewordElement(11507))),
					new FC3ReadRegistersTask(11567, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_3_ACTIVE_POWER, new FloatDoublewordElement(11567))),
					new FC3ReadRegistersTask(11575, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_3_REACTIVE_POWER, new FloatDoublewordElement(11575)))
				);
		} else if(this.config.sensor_num() == 2) {
			mod = new ModbusProtocol(this,				
					new FC3ReadRegistersTask(11123, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2300_VOLTAGE, new FloatDoublewordElement(11123))),
					new FC3ReadRegistersTask(11151, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2300_FREQUENCY, new FloatDoublewordElement(11151))),
					new FC3ReadRegistersTask(11207, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_1_CURRENT, new FloatDoublewordElement(11207))),
					new FC3ReadRegistersTask(11267, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_1_ACTIVE_POWER, new FloatDoublewordElement(11267))),
					new FC3ReadRegistersTask(11275, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_1_REACTIVE_POWER, new FloatDoublewordElement(11275))),
					new FC3ReadRegistersTask(11357, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_CURRENT, new FloatDoublewordElement(11357))),
					new FC3ReadRegistersTask(11417, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_ACTIVE_POWER, new FloatDoublewordElement(11417))),
					new FC3ReadRegistersTask(11425, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_REACTIVE_POWER, new FloatDoublewordElement(11425)))
				);
		} else {		
		// sensor_num == 1  일때 시
		// 이 방식은 데이터 종류별로 묶어서 컨넥션 횟수를 줄일 수 있
			mod =  new ModbusProtocol(this,
					// Voltage Data of Accura 2300[S]
					new FC3ReadRegistersTask(11123, Priority.LOW,
						m(SymmetricMeter.ChannelId.VOLTAGE, new FloatDoublewordElement(11123))),
					new FC3ReadRegistersTask(11151, Priority.LOW,
					    // 11151  Frequency  Float32  PR  입력 전압 주파수. 단[Hz]
						m(SymmetricMeter.ChannelId.FREQUENCY, new FloatDoublewordElement(11151))),								
						// subunit   모듈 기기(센싱  기기)
						// Module ID 0의 시작 Number는 11201   // Module ID === Sensor Num (Accura2350)
						// 기본값으로 Accura 2350  1개가 설치됐다고 가정 
//						new DummyRegisterElement(11201),
						// 모든상의 전류의 평균 11207
					new FC3ReadRegistersTask(11207, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_1_CURRENT, new FloatDoublewordElement(11207))),								
						// 11267  유효전력[kW]
					new FC3ReadRegistersTask(11267, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_1_ACTIVE_POWER, new FloatDoublewordElement(11267))),								
						// 11275  무효전력[KVAR total]
					new FC3ReadRegistersTask(11275, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_1_REACTIVE_POWER, new FloatDoublewordElement(11275)))								
				);
		}
		
		return mod;
		
//		11267  유효전력[kW]    11275  무효전력[kVAR]     11283 피상전력[kVA]      11285 누설전류[A]
//		11313	삼상의 유효전력 예측[kW]  11323 삼상의 전류 예측[A]		11331  역률 
	}

	
//	@Override
//	protected ModbusProtocol defineModbusProtocol() {
//		// TODO implement ModbusProtocol
//		// register 11045	Validity of Accura 2300[S] Voltage data, 1: Accura 2300[S] 전압 데이터가 정상적 fetch 됨.
//		// register 11046 ~ +39  CT 모듈 데이터유효성체크 -1 = 비유효,  0 = 3상, 1 = 1상 
//		// register 862   결선모드  1p or 3p  // 1p 와 3p 따라서 Modbus Map 다
//		// register 1112   CT(Accura2350) 갯수 
//		// 본 프로그램 가동전에 해당 CT 들이 정상 작동하는 지 유효성체크 후에 가동 시킨다.
//		// 설명 링크 : https://blog.naver.com/kjamjalee/222093298925
//		
//		ModbusProtocol mod = null;
//		int num_sensor = this.config.sensor_num();
//		FC3ReadRegistersTask readRgVoltage = null;
//		FC3ReadRegistersTask readRgFrequency = null;
//		Array readRg = 
////		Task task = new Task();
//		
//		// Voltage Data of Accura 2300[S]
//		readRgVoltage = new FC3ReadRegistersTask(11123, Priority.LOW,
//			m(SymmetricMeter.ChannelId.VOLTAGE, new FloatDoublewordElement(11123)));
//		    // 11151  Frequency  Float32  PR  입력 전압 주파수. 단[Hz] 		
//		readRgFrequency = new FC3ReadRegistersTask(11151, Priority.LOW,
//			m(SymmetricMeter.ChannelId.FREQUENCY, new FloatDoublewordElement(11151)));
//		
//		for(int i=0; i< num_sensor; i++) {
//			// subunit   모듈 기기(센싱  기기)
//			// Module ID 0의 시작 Number는 11201   // Module ID === Sensor Num (Accura2350)
//			// 기본값으로 Accura 2350  1개가 설치됐다고 가정 
//			// 모든상의 전류의 평균 11207
//			readRg[i][0] = new FC3ReadRegistersTask(11207, Priority.LOW,
//				m(Accura2300.ChannelId.ACCURA2350_1_CURRENT, new FloatDoublewordElement(11207+(150*i))));
//			
//				// 11267  유효전력[kW]
//			readRg[i][1] = new FC3ReadRegistersTask(11267, Priority.LOW,
//				m(Accura2300.ChannelId.ACCURA2350_1_ACTIVE_POWER, new FloatDoublewordElement(11267+(150*i))));
//						
//				// 11275  무효전력[KVAR total]
//			readRg[i][2] = new FC3ReadRegistersTask(11275, Priority.LOW,
//					m(Accura2300.ChannelId.ACCURA2350_1_REACTIVE_POWER, new FloatDoublewordElement(11275+(150*i))));
//
//		}			
//		
//		return new ModbusProtocol(this, readRgVoltage, readRgFrequency, readRg.length);
//	}
	
}
