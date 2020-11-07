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
					new FC3ReadRegistersTask(11122, Priority.LOW,	// Long 아닌 Interger 로 처리됨.
						m(Accura2300.ChannelId.ACCURA2300_VOLTAGE, new FloatDoublewordElement(11122))),
					new FC3ReadRegistersTask(11150, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2300_FREQUENCY, new FloatDoublewordElement(11150))),
					new FC3ReadRegistersTask(11206, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_1_CURRENT, new FloatDoublewordElement(11206))),
					new FC3ReadRegistersTask(11266, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_1_ACTIVE_POWER, new FloatDoublewordElement(11266))),
					new FC3ReadRegistersTask(11274, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_1_REACTIVE_POWER, new FloatDoublewordElement(11274))),
					new FC3ReadRegistersTask(11356, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_CURRENT, new FloatDoublewordElement(11356))),
					new FC3ReadRegistersTask(11416, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_ACTIVE_POWER, new FloatDoublewordElement(11416))),
					new FC3ReadRegistersTask(11424, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_REACTIVE_POWER, new FloatDoublewordElement(11424))),
					new FC3ReadRegistersTask(11506, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_3_CURRENT, new FloatDoublewordElement(11506))),
					new FC3ReadRegistersTask(11566, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_3_ACTIVE_POWER, new FloatDoublewordElement(11566))),
					new FC3ReadRegistersTask(11574, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_3_REACTIVE_POWER, new FloatDoublewordElement(11574))),
					new FC3ReadRegistersTask(11657, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_4_CURRENT, new FloatDoublewordElement(11656))),
					new FC3ReadRegistersTask(11716, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_4_ACTIVE_POWER, new FloatDoublewordElement(11716))),
					new FC3ReadRegistersTask(11724, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_4_REACTIVE_POWER, new FloatDoublewordElement(11724))),
					new FC3ReadRegistersTask(11906, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_5_CURRENT, new FloatDoublewordElement(11906))),
					new FC3ReadRegistersTask(11966, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_5_ACTIVE_POWER, new FloatDoublewordElement(11966))),
					new FC3ReadRegistersTask(11974, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_5_REACTIVE_POWER, new FloatDoublewordElement(11974))),
					new FC3ReadRegistersTask(12156, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_6_CURRENT, new FloatDoublewordElement(12156))),
					new FC3ReadRegistersTask(12216, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_6_ACTIVE_POWER, new FloatDoublewordElement(12216))),
					new FC3ReadRegistersTask(12224, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_6_REACTIVE_POWER, new FloatDoublewordElement(12224))),
					new FC3ReadRegistersTask(12306, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_7_CURRENT, new FloatDoublewordElement(12306))),
					new FC3ReadRegistersTask(12366, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_7_ACTIVE_POWER, new FloatDoublewordElement(12366))),
					new FC3ReadRegistersTask(12374, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_7_REACTIVE_POWER, new FloatDoublewordElement(12275))),
					new FC3ReadRegistersTask(12456, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_8_CURRENT, new FloatDoublewordElement(12456))),
					new FC3ReadRegistersTask(12516, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_8_ACTIVE_POWER, new FloatDoublewordElement(12516))),
					new FC3ReadRegistersTask(12524, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_8_REACTIVE_POWER, new FloatDoublewordElement(12524))),
					new FC3ReadRegistersTask(12606, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_9_CURRENT, new FloatDoublewordElement(12606))),
					new FC3ReadRegistersTask(12666, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_9_ACTIVE_POWER, new FloatDoublewordElement(12666))),
					new FC3ReadRegistersTask(12674, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_9_REACTIVE_POWER, new FloatDoublewordElement(12674))),
					new FC3ReadRegistersTask(12756, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_10_CURRENT, new FloatDoublewordElement(12756))),
					new FC3ReadRegistersTask(12816, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_10_ACTIVE_POWER, new FloatDoublewordElement(12816))),
					new FC3ReadRegistersTask(12824, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_10_REACTIVE_POWER, new FloatDoublewordElement(12824))),
					new FC3ReadRegistersTask(12906, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_11_CURRENT, new FloatDoublewordElement(12906))),
					new FC3ReadRegistersTask(12966, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_11_ACTIVE_POWER, new FloatDoublewordElement(12966))),
					new FC3ReadRegistersTask(12974, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_11_REACTIVE_POWER, new FloatDoublewordElement(12974))),
					new FC3ReadRegistersTask(13056, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_12_CURRENT, new FloatDoublewordElement(13056))),
					new FC3ReadRegistersTask(13116, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_12_ACTIVE_POWER, new FloatDoublewordElement(13116))),
					new FC3ReadRegistersTask(13124, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_12_REACTIVE_POWER, new FloatDoublewordElement(13124))),
					new FC3ReadRegistersTask(13206, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_13_CURRENT, new FloatDoublewordElement(13206))),
					new FC3ReadRegistersTask(13266, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_13_ACTIVE_POWER, new FloatDoublewordElement(13266))),
					new FC3ReadRegistersTask(13274, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_13_REACTIVE_POWER, new FloatDoublewordElement(13274))),
					new FC3ReadRegistersTask(13356, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_14_CURRENT, new FloatDoublewordElement(13356))),
					new FC3ReadRegistersTask(13416, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_14_ACTIVE_POWER, new FloatDoublewordElement(13416))),
					new FC3ReadRegistersTask(13424, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_14_REACTIVE_POWER, new FloatDoublewordElement(13424))),
					new FC3ReadRegistersTask(13506, Priority.LOW,	
						m(Accura2300.ChannelId.ACCURA2350_15_CURRENT, new FloatDoublewordElement(13506))),
					new FC3ReadRegistersTask(13566, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_15_ACTIVE_POWER, new FloatDoublewordElement(13566))),
					new FC3ReadRegistersTask(13574, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_15_REACTIVE_POWER, new FloatDoublewordElement(13574))),
					new FC3ReadRegistersTask(13656, Priority.LOW,	
						m(Accura2300.ChannelId.ACCURA2350_16_CURRENT, new FloatDoublewordElement(13656))),
					new FC3ReadRegistersTask(13716, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_16_ACTIVE_POWER, new FloatDoublewordElement(13716))),
					new FC3ReadRegistersTask(13724, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_16_REACTIVE_POWER, new FloatDoublewordElement(13724))),
					new FC3ReadRegistersTask(13806, Priority.LOW,	
						m(Accura2300.ChannelId.ACCURA2350_17_CURRENT, new FloatDoublewordElement(13806))),
					new FC3ReadRegistersTask(13866, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_17_ACTIVE_POWER, new FloatDoublewordElement(13866))),
					new FC3ReadRegistersTask(13874, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_17_REACTIVE_POWER, new FloatDoublewordElement(13874))),
					new FC3ReadRegistersTask(13956, Priority.LOW,	
						m(Accura2300.ChannelId.ACCURA2350_18_CURRENT, new FloatDoublewordElement(13956))),
					new FC3ReadRegistersTask(14016, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_18_ACTIVE_POWER, new FloatDoublewordElement(14016))),
					new FC3ReadRegistersTask(14024, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_18_REACTIVE_POWER, new FloatDoublewordElement(14024)))
				);
		}else if(this.config.sensor_num() == 17) {
			mod = new ModbusProtocol(this,					
					new FC3ReadRegistersTask(11122, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2300_VOLTAGE, new FloatDoublewordElement(11122))),
					new FC3ReadRegistersTask(11150, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2300_FREQUENCY, new FloatDoublewordElement(11150))),
					new FC3ReadRegistersTask(11206, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_1_CURRENT, new FloatDoublewordElement(11206))),
					new FC3ReadRegistersTask(11266, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_1_ACTIVE_POWER, new FloatDoublewordElement(11266))),
					new FC3ReadRegistersTask(11274, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_1_REACTIVE_POWER, new FloatDoublewordElement(11274))),
					new FC3ReadRegistersTask(11356, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_CURRENT, new FloatDoublewordElement(11356))),
					new FC3ReadRegistersTask(11416, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_ACTIVE_POWER, new FloatDoublewordElement(11416))),
					new FC3ReadRegistersTask(11424, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_REACTIVE_POWER, new FloatDoublewordElement(11424))),
					new FC3ReadRegistersTask(11506, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_3_CURRENT, new FloatDoublewordElement(11506))),
					new FC3ReadRegistersTask(11566, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_3_ACTIVE_POWER, new FloatDoublewordElement(11566))),
					new FC3ReadRegistersTask(11574, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_3_REACTIVE_POWER, new FloatDoublewordElement(11574))),
					new FC3ReadRegistersTask(11656, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_4_CURRENT, new FloatDoublewordElement(11656))),
					new FC3ReadRegistersTask(11716, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_4_ACTIVE_POWER, new FloatDoublewordElement(11716))),
					new FC3ReadRegistersTask(11724, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_4_REACTIVE_POWER, new FloatDoublewordElement(11724))),
					new FC3ReadRegistersTask(11906, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_5_CURRENT, new FloatDoublewordElement(11906))),
					new FC3ReadRegistersTask(11966, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_5_ACTIVE_POWER, new FloatDoublewordElement(11966))),
					new FC3ReadRegistersTask(11974, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_5_REACTIVE_POWER, new FloatDoublewordElement(11974))),
					new FC3ReadRegistersTask(12156, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_6_CURRENT, new FloatDoublewordElement(12156))),
					new FC3ReadRegistersTask(12216, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_6_ACTIVE_POWER, new FloatDoublewordElement(12216))),
					new FC3ReadRegistersTask(12224, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_6_REACTIVE_POWER, new FloatDoublewordElement(12224))),
					new FC3ReadRegistersTask(12306, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_7_CURRENT, new FloatDoublewordElement(12306))),
					new FC3ReadRegistersTask(12366, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_7_ACTIVE_POWER, new FloatDoublewordElement(12366))),
					new FC3ReadRegistersTask(12374, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_7_REACTIVE_POWER, new FloatDoublewordElement(12275))),
					new FC3ReadRegistersTask(12456, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_8_CURRENT, new FloatDoublewordElement(12456))),
					new FC3ReadRegistersTask(12516, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_8_ACTIVE_POWER, new FloatDoublewordElement(12516))),
					new FC3ReadRegistersTask(12524, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_8_REACTIVE_POWER, new FloatDoublewordElement(12524))),
					new FC3ReadRegistersTask(12606, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_9_CURRENT, new FloatDoublewordElement(12606))),
					new FC3ReadRegistersTask(12666, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_9_ACTIVE_POWER, new FloatDoublewordElement(12666))),
					new FC3ReadRegistersTask(12674, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_9_REACTIVE_POWER, new FloatDoublewordElement(12674))),
					new FC3ReadRegistersTask(12756, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_10_CURRENT, new FloatDoublewordElement(12756))),
					new FC3ReadRegistersTask(12816, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_10_ACTIVE_POWER, new FloatDoublewordElement(12816))),
					new FC3ReadRegistersTask(12824, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_10_REACTIVE_POWER, new FloatDoublewordElement(12824))),
					new FC3ReadRegistersTask(12906, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_11_CURRENT, new FloatDoublewordElement(12906))),
					new FC3ReadRegistersTask(12966, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_11_ACTIVE_POWER, new FloatDoublewordElement(12966))),
					new FC3ReadRegistersTask(12974, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_11_REACTIVE_POWER, new FloatDoublewordElement(12974))),
					new FC3ReadRegistersTask(13056, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_12_CURRENT, new FloatDoublewordElement(13056))),
					new FC3ReadRegistersTask(13116, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_12_ACTIVE_POWER, new FloatDoublewordElement(13116))),
					new FC3ReadRegistersTask(13124, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_12_REACTIVE_POWER, new FloatDoublewordElement(13124))),
					new FC3ReadRegistersTask(13206, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_13_CURRENT, new FloatDoublewordElement(13206))),
					new FC3ReadRegistersTask(13266, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_13_ACTIVE_POWER, new FloatDoublewordElement(13266))),
					new FC3ReadRegistersTask(13274, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_13_REACTIVE_POWER, new FloatDoublewordElement(13274))),
					new FC3ReadRegistersTask(13356, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_14_CURRENT, new FloatDoublewordElement(13356))),
					new FC3ReadRegistersTask(13416, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_14_ACTIVE_POWER, new FloatDoublewordElement(13416))),
					new FC3ReadRegistersTask(13424, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_14_REACTIVE_POWER, new FloatDoublewordElement(13424))),
					new FC3ReadRegistersTask(13506, Priority.LOW,	
						m(Accura2300.ChannelId.ACCURA2350_15_CURRENT, new FloatDoublewordElement(13506))),
					new FC3ReadRegistersTask(13566, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_15_ACTIVE_POWER, new FloatDoublewordElement(13566))),
					new FC3ReadRegistersTask(13574, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_15_REACTIVE_POWER, new FloatDoublewordElement(13574))),
					new FC3ReadRegistersTask(13656, Priority.LOW,	
						m(Accura2300.ChannelId.ACCURA2350_16_CURRENT, new FloatDoublewordElement(13656))),
					new FC3ReadRegistersTask(13716, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_16_ACTIVE_POWER, new FloatDoublewordElement(13716))),
					new FC3ReadRegistersTask(13724, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_16_REACTIVE_POWER, new FloatDoublewordElement(13724))),
					new FC3ReadRegistersTask(13806, Priority.LOW,	
						m(Accura2300.ChannelId.ACCURA2350_17_CURRENT, new FloatDoublewordElement(13806))),
					new FC3ReadRegistersTask(13866, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_17_ACTIVE_POWER, new FloatDoublewordElement(13866))),
					new FC3ReadRegistersTask(13874, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_17_REACTIVE_POWER, new FloatDoublewordElement(13874)))
				);
		}else if(this.config.sensor_num() == 16) {
			mod = new ModbusProtocol(this,					
					new FC3ReadRegistersTask(11122, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2300_VOLTAGE, new FloatDoublewordElement(11122))),
					new FC3ReadRegistersTask(11150, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2300_FREQUENCY, new FloatDoublewordElement(11150))),
					new FC3ReadRegistersTask(11206, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_1_CURRENT, new FloatDoublewordElement(11206))),
					new FC3ReadRegistersTask(11266, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_1_ACTIVE_POWER, new FloatDoublewordElement(11266))),
					new FC3ReadRegistersTask(11274, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_1_REACTIVE_POWER, new FloatDoublewordElement(11274))),
					new FC3ReadRegistersTask(11356, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_CURRENT, new FloatDoublewordElement(11356))),
					new FC3ReadRegistersTask(11416, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_ACTIVE_POWER, new FloatDoublewordElement(11416))),
					new FC3ReadRegistersTask(11424, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_REACTIVE_POWER, new FloatDoublewordElement(11424))),
					new FC3ReadRegistersTask(11506, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_3_CURRENT, new FloatDoublewordElement(11506))),
					new FC3ReadRegistersTask(11566, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_3_ACTIVE_POWER, new FloatDoublewordElement(11566))),
					new FC3ReadRegistersTask(11574, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_3_REACTIVE_POWER, new FloatDoublewordElement(11574))),
					new FC3ReadRegistersTask(11656, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_4_CURRENT, new FloatDoublewordElement(11656))),
					new FC3ReadRegistersTask(11716, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_4_ACTIVE_POWER, new FloatDoublewordElement(11716))),
					new FC3ReadRegistersTask(11724, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_4_REACTIVE_POWER, new FloatDoublewordElement(11724))),
					new FC3ReadRegistersTask(11906, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_5_CURRENT, new FloatDoublewordElement(11906))),
					new FC3ReadRegistersTask(11966, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_5_ACTIVE_POWER, new FloatDoublewordElement(11966))),
					new FC3ReadRegistersTask(11974, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_5_REACTIVE_POWER, new FloatDoublewordElement(11974))),
					new FC3ReadRegistersTask(12156, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_6_CURRENT, new FloatDoublewordElement(12156))),
					new FC3ReadRegistersTask(12216, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_6_ACTIVE_POWER, new FloatDoublewordElement(12216))),
					new FC3ReadRegistersTask(12224, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_6_REACTIVE_POWER, new FloatDoublewordElement(12224))),
					new FC3ReadRegistersTask(12306, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_7_CURRENT, new FloatDoublewordElement(12306))),
					new FC3ReadRegistersTask(12366, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_7_ACTIVE_POWER, new FloatDoublewordElement(12366))),
					new FC3ReadRegistersTask(12374, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_7_REACTIVE_POWER, new FloatDoublewordElement(12275))),
					new FC3ReadRegistersTask(12456, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_8_CURRENT, new FloatDoublewordElement(12456))),
					new FC3ReadRegistersTask(12516, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_8_ACTIVE_POWER, new FloatDoublewordElement(12516))),
					new FC3ReadRegistersTask(12524, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_8_REACTIVE_POWER, new FloatDoublewordElement(12524))),
					new FC3ReadRegistersTask(12606, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_9_CURRENT, new FloatDoublewordElement(12606))),
					new FC3ReadRegistersTask(12666, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_9_ACTIVE_POWER, new FloatDoublewordElement(12666))),
					new FC3ReadRegistersTask(12674, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_9_REACTIVE_POWER, new FloatDoublewordElement(12674))),
					new FC3ReadRegistersTask(12756, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_10_CURRENT, new FloatDoublewordElement(12756))),
					new FC3ReadRegistersTask(12816, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_10_ACTIVE_POWER, new FloatDoublewordElement(12816))),
					new FC3ReadRegistersTask(12824, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_10_REACTIVE_POWER, new FloatDoublewordElement(12824))),
					new FC3ReadRegistersTask(12906, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_11_CURRENT, new FloatDoublewordElement(12906))),
					new FC3ReadRegistersTask(12966, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_11_ACTIVE_POWER, new FloatDoublewordElement(12966))),
					new FC3ReadRegistersTask(12974, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_11_REACTIVE_POWER, new FloatDoublewordElement(12974))),
					new FC3ReadRegistersTask(13056, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_12_CURRENT, new FloatDoublewordElement(13056))),
					new FC3ReadRegistersTask(13116, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_12_ACTIVE_POWER, new FloatDoublewordElement(13116))),
					new FC3ReadRegistersTask(13124, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_12_REACTIVE_POWER, new FloatDoublewordElement(13124))),
					new FC3ReadRegistersTask(13206, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_13_CURRENT, new FloatDoublewordElement(13206))),
					new FC3ReadRegistersTask(13266, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_13_ACTIVE_POWER, new FloatDoublewordElement(13266))),
					new FC3ReadRegistersTask(13274, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_13_REACTIVE_POWER, new FloatDoublewordElement(13274))),
					new FC3ReadRegistersTask(13356, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_14_CURRENT, new FloatDoublewordElement(13356))),
					new FC3ReadRegistersTask(13416, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_14_ACTIVE_POWER, new FloatDoublewordElement(13416))),
					new FC3ReadRegistersTask(13424, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_14_REACTIVE_POWER, new FloatDoublewordElement(13424))),
					new FC3ReadRegistersTask(13506, Priority.LOW,	
						m(Accura2300.ChannelId.ACCURA2350_15_CURRENT, new FloatDoublewordElement(13506))),
					new FC3ReadRegistersTask(13566, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_15_ACTIVE_POWER, new FloatDoublewordElement(13566))),
					new FC3ReadRegistersTask(13574, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_15_REACTIVE_POWER, new FloatDoublewordElement(13574))),
					new FC3ReadRegistersTask(13656, Priority.LOW,	
						m(Accura2300.ChannelId.ACCURA2350_16_CURRENT, new FloatDoublewordElement(13656))),
					new FC3ReadRegistersTask(13716, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_16_ACTIVE_POWER, new FloatDoublewordElement(13716))),
					new FC3ReadRegistersTask(13724, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_16_REACTIVE_POWER, new FloatDoublewordElement(13724)))
				);
		}else if(this.config.sensor_num() == 15) {
			mod = new ModbusProtocol(this,					
					new FC3ReadRegistersTask(11122, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2300_VOLTAGE, new FloatDoublewordElement(11122))),
					new FC3ReadRegistersTask(11150, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2300_FREQUENCY, new FloatDoublewordElement(11150))),
					new FC3ReadRegistersTask(11206, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_1_CURRENT, new FloatDoublewordElement(11206))),
					new FC3ReadRegistersTask(11266, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_1_ACTIVE_POWER, new FloatDoublewordElement(11266))),
					new FC3ReadRegistersTask(11274, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_1_REACTIVE_POWER, new FloatDoublewordElement(11274))),
					new FC3ReadRegistersTask(11356, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_CURRENT, new FloatDoublewordElement(11356))),
					new FC3ReadRegistersTask(11416, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_ACTIVE_POWER, new FloatDoublewordElement(11416))),
					new FC3ReadRegistersTask(11424, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_REACTIVE_POWER, new FloatDoublewordElement(11424))),
					new FC3ReadRegistersTask(11506, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_3_CURRENT, new FloatDoublewordElement(11506))),
					new FC3ReadRegistersTask(11566, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_3_ACTIVE_POWER, new FloatDoublewordElement(11566))),
					new FC3ReadRegistersTask(11574, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_3_REACTIVE_POWER, new FloatDoublewordElement(11574))),
					new FC3ReadRegistersTask(11656, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_4_CURRENT, new FloatDoublewordElement(11656))),
					new FC3ReadRegistersTask(11716, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_4_ACTIVE_POWER, new FloatDoublewordElement(11716))),
					new FC3ReadRegistersTask(11724, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_4_REACTIVE_POWER, new FloatDoublewordElement(11724))),
					new FC3ReadRegistersTask(11906, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_5_CURRENT, new FloatDoublewordElement(11906))),
					new FC3ReadRegistersTask(11966, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_5_ACTIVE_POWER, new FloatDoublewordElement(11966))),
					new FC3ReadRegistersTask(11974, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_5_REACTIVE_POWER, new FloatDoublewordElement(11974))),
					new FC3ReadRegistersTask(12156, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_6_CURRENT, new FloatDoublewordElement(12156))),
					new FC3ReadRegistersTask(12216, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_6_ACTIVE_POWER, new FloatDoublewordElement(12216))),
					new FC3ReadRegistersTask(12224, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_6_REACTIVE_POWER, new FloatDoublewordElement(12224))),
					new FC3ReadRegistersTask(12306, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_7_CURRENT, new FloatDoublewordElement(12306))),
					new FC3ReadRegistersTask(12366, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_7_ACTIVE_POWER, new FloatDoublewordElement(12366))),
					new FC3ReadRegistersTask(12374, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_7_REACTIVE_POWER, new FloatDoublewordElement(12275))),
					new FC3ReadRegistersTask(12456, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_8_CURRENT, new FloatDoublewordElement(12456))),
					new FC3ReadRegistersTask(12516, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_8_ACTIVE_POWER, new FloatDoublewordElement(12516))),
					new FC3ReadRegistersTask(12524, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_8_REACTIVE_POWER, new FloatDoublewordElement(12524))),
					new FC3ReadRegistersTask(12606, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_9_CURRENT, new FloatDoublewordElement(12606))),
					new FC3ReadRegistersTask(12666, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_9_ACTIVE_POWER, new FloatDoublewordElement(12666))),
					new FC3ReadRegistersTask(12674, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_9_REACTIVE_POWER, new FloatDoublewordElement(12674))),
					new FC3ReadRegistersTask(12756, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_10_CURRENT, new FloatDoublewordElement(12756))),
					new FC3ReadRegistersTask(12816, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_10_ACTIVE_POWER, new FloatDoublewordElement(12816))),
					new FC3ReadRegistersTask(12824, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_10_REACTIVE_POWER, new FloatDoublewordElement(12824))),
					new FC3ReadRegistersTask(12906, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_11_CURRENT, new FloatDoublewordElement(12906))),
					new FC3ReadRegistersTask(12966, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_11_ACTIVE_POWER, new FloatDoublewordElement(12966))),
					new FC3ReadRegistersTask(12974, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_11_REACTIVE_POWER, new FloatDoublewordElement(12974))),
					new FC3ReadRegistersTask(13056, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_12_CURRENT, new FloatDoublewordElement(13056))),
					new FC3ReadRegistersTask(13116, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_12_ACTIVE_POWER, new FloatDoublewordElement(13116))),
					new FC3ReadRegistersTask(13124, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_12_REACTIVE_POWER, new FloatDoublewordElement(13124))),
					new FC3ReadRegistersTask(13206, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_13_CURRENT, new FloatDoublewordElement(13206))),
					new FC3ReadRegistersTask(13266, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_13_ACTIVE_POWER, new FloatDoublewordElement(13266))),
					new FC3ReadRegistersTask(13274, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_13_REACTIVE_POWER, new FloatDoublewordElement(13274))),
					new FC3ReadRegistersTask(13356, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_14_CURRENT, new FloatDoublewordElement(13356))),
					new FC3ReadRegistersTask(13416, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_14_ACTIVE_POWER, new FloatDoublewordElement(13416))),
					new FC3ReadRegistersTask(13424, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_14_REACTIVE_POWER, new FloatDoublewordElement(13424))),
					new FC3ReadRegistersTask(13506, Priority.LOW,	
						m(Accura2300.ChannelId.ACCURA2350_15_CURRENT, new FloatDoublewordElement(13506))),
					new FC3ReadRegistersTask(13566, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_15_ACTIVE_POWER, new FloatDoublewordElement(13566))),
					new FC3ReadRegistersTask(13574, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_15_REACTIVE_POWER, new FloatDoublewordElement(13574)))
				);
		}else if(this.config.sensor_num() == 14) {
			mod = new ModbusProtocol(this,					
					new FC3ReadRegistersTask(11122, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2300_VOLTAGE, new FloatDoublewordElement(11122))),
					new FC3ReadRegistersTask(11150, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2300_FREQUENCY, new FloatDoublewordElement(11150))),
					new FC3ReadRegistersTask(11206, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_1_CURRENT, new FloatDoublewordElement(11206))),
					new FC3ReadRegistersTask(11266, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_1_ACTIVE_POWER, new FloatDoublewordElement(11266))),
					new FC3ReadRegistersTask(11274, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_1_REACTIVE_POWER, new FloatDoublewordElement(11274))),
					new FC3ReadRegistersTask(11356, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_CURRENT, new FloatDoublewordElement(11356))),
					new FC3ReadRegistersTask(11416, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_ACTIVE_POWER, new FloatDoublewordElement(11416))),
					new FC3ReadRegistersTask(11424, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_REACTIVE_POWER, new FloatDoublewordElement(11424))),
					new FC3ReadRegistersTask(11506, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_3_CURRENT, new FloatDoublewordElement(11506))),
					new FC3ReadRegistersTask(11566, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_3_ACTIVE_POWER, new FloatDoublewordElement(11566))),
					new FC3ReadRegistersTask(11574, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_3_REACTIVE_POWER, new FloatDoublewordElement(11574))),
					new FC3ReadRegistersTask(11656, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_4_CURRENT, new FloatDoublewordElement(11656))),
					new FC3ReadRegistersTask(11716, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_4_ACTIVE_POWER, new FloatDoublewordElement(11716))),
					new FC3ReadRegistersTask(11724, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_4_REACTIVE_POWER, new FloatDoublewordElement(11724))),
					new FC3ReadRegistersTask(11906, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_5_CURRENT, new FloatDoublewordElement(11906))),
					new FC3ReadRegistersTask(11966, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_5_ACTIVE_POWER, new FloatDoublewordElement(11966))),
					new FC3ReadRegistersTask(11974, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_5_REACTIVE_POWER, new FloatDoublewordElement(11974))),
					new FC3ReadRegistersTask(12156, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_6_CURRENT, new FloatDoublewordElement(12156))),
					new FC3ReadRegistersTask(12216, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_6_ACTIVE_POWER, new FloatDoublewordElement(12216))),
					new FC3ReadRegistersTask(12224, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_6_REACTIVE_POWER, new FloatDoublewordElement(12224))),
					new FC3ReadRegistersTask(12306, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_7_CURRENT, new FloatDoublewordElement(12306))),
					new FC3ReadRegistersTask(12366, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_7_ACTIVE_POWER, new FloatDoublewordElement(12366))),
					new FC3ReadRegistersTask(12374, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_7_REACTIVE_POWER, new FloatDoublewordElement(12275))),
					new FC3ReadRegistersTask(12456, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_8_CURRENT, new FloatDoublewordElement(12456))),
					new FC3ReadRegistersTask(12516, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_8_ACTIVE_POWER, new FloatDoublewordElement(12516))),
					new FC3ReadRegistersTask(12524, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_8_REACTIVE_POWER, new FloatDoublewordElement(12524))),
					new FC3ReadRegistersTask(12606, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_9_CURRENT, new FloatDoublewordElement(12606))),
					new FC3ReadRegistersTask(12666, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_9_ACTIVE_POWER, new FloatDoublewordElement(12666))),
					new FC3ReadRegistersTask(12674, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_9_REACTIVE_POWER, new FloatDoublewordElement(12674))),
					new FC3ReadRegistersTask(12756, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_10_CURRENT, new FloatDoublewordElement(12756))),
					new FC3ReadRegistersTask(12816, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_10_ACTIVE_POWER, new FloatDoublewordElement(12816))),
					new FC3ReadRegistersTask(12824, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_10_REACTIVE_POWER, new FloatDoublewordElement(12824))),
					new FC3ReadRegistersTask(12906, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_11_CURRENT, new FloatDoublewordElement(12906))),
					new FC3ReadRegistersTask(12966, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_11_ACTIVE_POWER, new FloatDoublewordElement(12966))),
					new FC3ReadRegistersTask(12974, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_11_REACTIVE_POWER, new FloatDoublewordElement(12974))),
					new FC3ReadRegistersTask(13056, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_12_CURRENT, new FloatDoublewordElement(13056))),
					new FC3ReadRegistersTask(13116, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_12_ACTIVE_POWER, new FloatDoublewordElement(13116))),
					new FC3ReadRegistersTask(13124, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_12_REACTIVE_POWER, new FloatDoublewordElement(13124))),
					new FC3ReadRegistersTask(13206, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_13_CURRENT, new FloatDoublewordElement(13206))),
					new FC3ReadRegistersTask(13266, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_13_ACTIVE_POWER, new FloatDoublewordElement(13266))),
					new FC3ReadRegistersTask(13274, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_13_REACTIVE_POWER, new FloatDoublewordElement(13274))),
					new FC3ReadRegistersTask(13356, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_14_CURRENT, new FloatDoublewordElement(13356))),
					new FC3ReadRegistersTask(13416, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_14_ACTIVE_POWER, new FloatDoublewordElement(13416))),
					new FC3ReadRegistersTask(13424, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_14_REACTIVE_POWER, new FloatDoublewordElement(13424)))
				);
		}else if(this.config.sensor_num() == 13) {
			mod = new ModbusProtocol(this,					
					new FC3ReadRegistersTask(11122, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2300_VOLTAGE, new FloatDoublewordElement(11122))),
					new FC3ReadRegistersTask(11150, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2300_FREQUENCY, new FloatDoublewordElement(11150))),
					new FC3ReadRegistersTask(11206, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_1_CURRENT, new FloatDoublewordElement(11206))),
					new FC3ReadRegistersTask(11266, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_1_ACTIVE_POWER, new FloatDoublewordElement(11266))),
					new FC3ReadRegistersTask(11274, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_1_REACTIVE_POWER, new FloatDoublewordElement(11274))),
					new FC3ReadRegistersTask(11356, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_CURRENT, new FloatDoublewordElement(11356))),
					new FC3ReadRegistersTask(11416, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_ACTIVE_POWER, new FloatDoublewordElement(11416))),
					new FC3ReadRegistersTask(11424, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_REACTIVE_POWER, new FloatDoublewordElement(11424))),
					new FC3ReadRegistersTask(11506, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_3_CURRENT, new FloatDoublewordElement(11506))),
					new FC3ReadRegistersTask(11566, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_3_ACTIVE_POWER, new FloatDoublewordElement(11566))),
					new FC3ReadRegistersTask(11574, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_3_REACTIVE_POWER, new FloatDoublewordElement(11574))),
					new FC3ReadRegistersTask(11656, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_4_CURRENT, new FloatDoublewordElement(11656))),
					new FC3ReadRegistersTask(11716, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_4_ACTIVE_POWER, new FloatDoublewordElement(11716))),
					new FC3ReadRegistersTask(11724, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_4_REACTIVE_POWER, new FloatDoublewordElement(11724))),
					new FC3ReadRegistersTask(11906, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_5_CURRENT, new FloatDoublewordElement(11906))),
					new FC3ReadRegistersTask(11966, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_5_ACTIVE_POWER, new FloatDoublewordElement(11966))),
					new FC3ReadRegistersTask(11974, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_5_REACTIVE_POWER, new FloatDoublewordElement(11974))),
					new FC3ReadRegistersTask(12156, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_6_CURRENT, new FloatDoublewordElement(12156))),
					new FC3ReadRegistersTask(12216, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_6_ACTIVE_POWER, new FloatDoublewordElement(12216))),
					new FC3ReadRegistersTask(12224, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_6_REACTIVE_POWER, new FloatDoublewordElement(12224))),
					new FC3ReadRegistersTask(12306, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_7_CURRENT, new FloatDoublewordElement(12306))),
					new FC3ReadRegistersTask(12366, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_7_ACTIVE_POWER, new FloatDoublewordElement(12366))),
					new FC3ReadRegistersTask(12374, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_7_REACTIVE_POWER, new FloatDoublewordElement(12275))),
					new FC3ReadRegistersTask(12456, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_8_CURRENT, new FloatDoublewordElement(12456))),
					new FC3ReadRegistersTask(12516, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_8_ACTIVE_POWER, new FloatDoublewordElement(12516))),
					new FC3ReadRegistersTask(12524, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_8_REACTIVE_POWER, new FloatDoublewordElement(12524))),
					new FC3ReadRegistersTask(12606, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_9_CURRENT, new FloatDoublewordElement(12606))),
					new FC3ReadRegistersTask(12666, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_9_ACTIVE_POWER, new FloatDoublewordElement(12666))),
					new FC3ReadRegistersTask(12674, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_9_REACTIVE_POWER, new FloatDoublewordElement(12674))),
					new FC3ReadRegistersTask(12756, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_10_CURRENT, new FloatDoublewordElement(12756))),
					new FC3ReadRegistersTask(12816, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_10_ACTIVE_POWER, new FloatDoublewordElement(12816))),
					new FC3ReadRegistersTask(12824, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_10_REACTIVE_POWER, new FloatDoublewordElement(12824))),
					new FC3ReadRegistersTask(12906, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_11_CURRENT, new FloatDoublewordElement(12906))),
					new FC3ReadRegistersTask(12966, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_11_ACTIVE_POWER, new FloatDoublewordElement(12966))),
					new FC3ReadRegistersTask(12974, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_11_REACTIVE_POWER, new FloatDoublewordElement(12974))),
					new FC3ReadRegistersTask(13056, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_12_CURRENT, new FloatDoublewordElement(13056))),
					new FC3ReadRegistersTask(13116, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_12_ACTIVE_POWER, new FloatDoublewordElement(13116))),
					new FC3ReadRegistersTask(13124, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_12_REACTIVE_POWER, new FloatDoublewordElement(13124))),
					new FC3ReadRegistersTask(13206, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_13_CURRENT, new FloatDoublewordElement(13206))),
					new FC3ReadRegistersTask(13266, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_13_ACTIVE_POWER, new FloatDoublewordElement(13266))),
					new FC3ReadRegistersTask(13274, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_13_REACTIVE_POWER, new FloatDoublewordElement(13274)))
				);
		}else if(this.config.sensor_num() == 12) {
			mod = new ModbusProtocol(this,					
					new FC3ReadRegistersTask(11122, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2300_VOLTAGE, new FloatDoublewordElement(11122))),
					new FC3ReadRegistersTask(11150, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2300_FREQUENCY, new FloatDoublewordElement(11150))),
					new FC3ReadRegistersTask(11206, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_1_CURRENT, new FloatDoublewordElement(11206))),
					new FC3ReadRegistersTask(11266, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_1_ACTIVE_POWER, new FloatDoublewordElement(11266))),
					new FC3ReadRegistersTask(11274, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_1_REACTIVE_POWER, new FloatDoublewordElement(11274))),
					new FC3ReadRegistersTask(11356, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_CURRENT, new FloatDoublewordElement(11356))),
					new FC3ReadRegistersTask(11416, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_ACTIVE_POWER, new FloatDoublewordElement(11416))),
					new FC3ReadRegistersTask(11424, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_REACTIVE_POWER, new FloatDoublewordElement(11424))),
					new FC3ReadRegistersTask(11506, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_3_CURRENT, new FloatDoublewordElement(11506))),
					new FC3ReadRegistersTask(11566, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_3_ACTIVE_POWER, new FloatDoublewordElement(11566))),
					new FC3ReadRegistersTask(11574, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_3_REACTIVE_POWER, new FloatDoublewordElement(11574))),
					new FC3ReadRegistersTask(11656, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_4_CURRENT, new FloatDoublewordElement(11656))),
					new FC3ReadRegistersTask(11716, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_4_ACTIVE_POWER, new FloatDoublewordElement(11716))),
					new FC3ReadRegistersTask(11724, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_4_REACTIVE_POWER, new FloatDoublewordElement(11724))),
					new FC3ReadRegistersTask(11906, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_5_CURRENT, new FloatDoublewordElement(11906))),
					new FC3ReadRegistersTask(11966, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_5_ACTIVE_POWER, new FloatDoublewordElement(11966))),
					new FC3ReadRegistersTask(11974, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_5_REACTIVE_POWER, new FloatDoublewordElement(11974))),
					new FC3ReadRegistersTask(12156, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_6_CURRENT, new FloatDoublewordElement(12156))),
					new FC3ReadRegistersTask(12216, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_6_ACTIVE_POWER, new FloatDoublewordElement(12216))),
					new FC3ReadRegistersTask(12224, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_6_REACTIVE_POWER, new FloatDoublewordElement(12224))),
					new FC3ReadRegistersTask(12306, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_7_CURRENT, new FloatDoublewordElement(12306))),
					new FC3ReadRegistersTask(12366, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_7_ACTIVE_POWER, new FloatDoublewordElement(12366))),
					new FC3ReadRegistersTask(12374, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_7_REACTIVE_POWER, new FloatDoublewordElement(12275))),
					new FC3ReadRegistersTask(12456, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_8_CURRENT, new FloatDoublewordElement(12456))),
					new FC3ReadRegistersTask(12516, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_8_ACTIVE_POWER, new FloatDoublewordElement(12516))),
					new FC3ReadRegistersTask(12524, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_8_REACTIVE_POWER, new FloatDoublewordElement(12524))),
					new FC3ReadRegistersTask(12606, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_9_CURRENT, new FloatDoublewordElement(12606))),
					new FC3ReadRegistersTask(12666, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_9_ACTIVE_POWER, new FloatDoublewordElement(12666))),
					new FC3ReadRegistersTask(12674, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_9_REACTIVE_POWER, new FloatDoublewordElement(12674))),
					new FC3ReadRegistersTask(12756, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_10_CURRENT, new FloatDoublewordElement(12756))),
					new FC3ReadRegistersTask(12816, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_10_ACTIVE_POWER, new FloatDoublewordElement(12816))),
					new FC3ReadRegistersTask(12824, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_10_REACTIVE_POWER, new FloatDoublewordElement(12824))),
					new FC3ReadRegistersTask(12906, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_11_CURRENT, new FloatDoublewordElement(12906))),
					new FC3ReadRegistersTask(12966, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_11_ACTIVE_POWER, new FloatDoublewordElement(12966))),
					new FC3ReadRegistersTask(12974, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_11_REACTIVE_POWER, new FloatDoublewordElement(12974))),
					new FC3ReadRegistersTask(13056, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_12_CURRENT, new FloatDoublewordElement(13056))),
					new FC3ReadRegistersTask(13116, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_12_ACTIVE_POWER, new FloatDoublewordElement(13116))),
					new FC3ReadRegistersTask(13124, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_12_REACTIVE_POWER, new FloatDoublewordElement(13124)))
				);
		}else if(this.config.sensor_num() == 11) {
			mod = new ModbusProtocol(this,					
					new FC3ReadRegistersTask(11122, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2300_VOLTAGE, new FloatDoublewordElement(11122))),
					new FC3ReadRegistersTask(11150, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2300_FREQUENCY, new FloatDoublewordElement(11150))),
					new FC3ReadRegistersTask(11206, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_1_CURRENT, new FloatDoublewordElement(11206))),
					new FC3ReadRegistersTask(11266, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_1_ACTIVE_POWER, new FloatDoublewordElement(11266))),
					new FC3ReadRegistersTask(11274, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_1_REACTIVE_POWER, new FloatDoublewordElement(11274))),
					new FC3ReadRegistersTask(11356, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_CURRENT, new FloatDoublewordElement(11356))),
					new FC3ReadRegistersTask(11416, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_ACTIVE_POWER, new FloatDoublewordElement(11416))),
					new FC3ReadRegistersTask(11424, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_REACTIVE_POWER, new FloatDoublewordElement(11424))),
					new FC3ReadRegistersTask(11506, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_3_CURRENT, new FloatDoublewordElement(11506))),
					new FC3ReadRegistersTask(11566, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_3_ACTIVE_POWER, new FloatDoublewordElement(11566))),
					new FC3ReadRegistersTask(11574, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_3_REACTIVE_POWER, new FloatDoublewordElement(11574))),
					new FC3ReadRegistersTask(11656, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_4_CURRENT, new FloatDoublewordElement(11656))),
					new FC3ReadRegistersTask(11716, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_4_ACTIVE_POWER, new FloatDoublewordElement(11716))),
					new FC3ReadRegistersTask(11724, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_4_REACTIVE_POWER, new FloatDoublewordElement(11724))),
					new FC3ReadRegistersTask(11906, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_5_CURRENT, new FloatDoublewordElement(11906))),
					new FC3ReadRegistersTask(11966, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_5_ACTIVE_POWER, new FloatDoublewordElement(11966))),
					new FC3ReadRegistersTask(11974, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_5_REACTIVE_POWER, new FloatDoublewordElement(11974))),
					new FC3ReadRegistersTask(12156, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_6_CURRENT, new FloatDoublewordElement(12156))),
					new FC3ReadRegistersTask(12216, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_6_ACTIVE_POWER, new FloatDoublewordElement(12216))),
					new FC3ReadRegistersTask(12224, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_6_REACTIVE_POWER, new FloatDoublewordElement(12224))),
					new FC3ReadRegistersTask(12306, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_7_CURRENT, new FloatDoublewordElement(12306))),
					new FC3ReadRegistersTask(12366, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_7_ACTIVE_POWER, new FloatDoublewordElement(12366))),
					new FC3ReadRegistersTask(12374, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_7_REACTIVE_POWER, new FloatDoublewordElement(12275))),
					new FC3ReadRegistersTask(12456, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_8_CURRENT, new FloatDoublewordElement(12456))),
					new FC3ReadRegistersTask(12516, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_8_ACTIVE_POWER, new FloatDoublewordElement(12516))),
					new FC3ReadRegistersTask(12524, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_8_REACTIVE_POWER, new FloatDoublewordElement(12524))),
					new FC3ReadRegistersTask(12606, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_9_CURRENT, new FloatDoublewordElement(12606))),
					new FC3ReadRegistersTask(12666, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_9_ACTIVE_POWER, new FloatDoublewordElement(12666))),
					new FC3ReadRegistersTask(12674, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_9_REACTIVE_POWER, new FloatDoublewordElement(12674))),
					new FC3ReadRegistersTask(12756, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_10_CURRENT, new FloatDoublewordElement(12756))),
					new FC3ReadRegistersTask(12816, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_10_ACTIVE_POWER, new FloatDoublewordElement(12816))),
					new FC3ReadRegistersTask(12824, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_10_REACTIVE_POWER, new FloatDoublewordElement(12824))),
					new FC3ReadRegistersTask(12906, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_11_CURRENT, new FloatDoublewordElement(12906))),
					new FC3ReadRegistersTask(12966, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_11_ACTIVE_POWER, new FloatDoublewordElement(12966))),
					new FC3ReadRegistersTask(12974, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_11_REACTIVE_POWER, new FloatDoublewordElement(12974)))
				);
		}else if(this.config.sensor_num() == 10) {
			mod = new ModbusProtocol(this,					
					new FC3ReadRegistersTask(11122, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2300_VOLTAGE, new FloatDoublewordElement(11122))),
					new FC3ReadRegistersTask(11150, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2300_FREQUENCY, new FloatDoublewordElement(11150))),
					new FC3ReadRegistersTask(11206, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_1_CURRENT, new FloatDoublewordElement(11206))),
					new FC3ReadRegistersTask(11266, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_1_ACTIVE_POWER, new FloatDoublewordElement(11266))),
					new FC3ReadRegistersTask(11274, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_1_REACTIVE_POWER, new FloatDoublewordElement(11274))),
					new FC3ReadRegistersTask(11356, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_CURRENT, new FloatDoublewordElement(11356))),
					new FC3ReadRegistersTask(11416, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_ACTIVE_POWER, new FloatDoublewordElement(11416))),
					new FC3ReadRegistersTask(11424, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_REACTIVE_POWER, new FloatDoublewordElement(11424))),
					new FC3ReadRegistersTask(11506, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_3_CURRENT, new FloatDoublewordElement(11506))),
					new FC3ReadRegistersTask(11566, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_3_ACTIVE_POWER, new FloatDoublewordElement(11566))),
					new FC3ReadRegistersTask(11574, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_3_REACTIVE_POWER, new FloatDoublewordElement(11574))),
					new FC3ReadRegistersTask(11656, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_4_CURRENT, new FloatDoublewordElement(11656))),
					new FC3ReadRegistersTask(11716, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_4_ACTIVE_POWER, new FloatDoublewordElement(11716))),
					new FC3ReadRegistersTask(11724, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_4_REACTIVE_POWER, new FloatDoublewordElement(11724))),
					new FC3ReadRegistersTask(11906, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_5_CURRENT, new FloatDoublewordElement(11906))),
					new FC3ReadRegistersTask(11966, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_5_ACTIVE_POWER, new FloatDoublewordElement(11966))),
					new FC3ReadRegistersTask(11974, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_5_REACTIVE_POWER, new FloatDoublewordElement(11974))),
					new FC3ReadRegistersTask(12156, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_6_CURRENT, new FloatDoublewordElement(12156))),
					new FC3ReadRegistersTask(12216, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_6_ACTIVE_POWER, new FloatDoublewordElement(12216))),
					new FC3ReadRegistersTask(12224, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_6_REACTIVE_POWER, new FloatDoublewordElement(12224))),
					new FC3ReadRegistersTask(12306, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_7_CURRENT, new FloatDoublewordElement(12306))),
					new FC3ReadRegistersTask(12366, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_7_ACTIVE_POWER, new FloatDoublewordElement(12366))),
					new FC3ReadRegistersTask(12374, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_7_REACTIVE_POWER, new FloatDoublewordElement(12275))),
					new FC3ReadRegistersTask(12456, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_8_CURRENT, new FloatDoublewordElement(12456))),
					new FC3ReadRegistersTask(12516, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_8_ACTIVE_POWER, new FloatDoublewordElement(12516))),
					new FC3ReadRegistersTask(12524, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_8_REACTIVE_POWER, new FloatDoublewordElement(12524))),
					new FC3ReadRegistersTask(12606, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_9_CURRENT, new FloatDoublewordElement(12606))),
					new FC3ReadRegistersTask(12666, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_9_ACTIVE_POWER, new FloatDoublewordElement(12666))),
					new FC3ReadRegistersTask(12674, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_9_REACTIVE_POWER, new FloatDoublewordElement(12674))),
					new FC3ReadRegistersTask(12756, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_10_CURRENT, new FloatDoublewordElement(12756))),
					new FC3ReadRegistersTask(12816, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_10_ACTIVE_POWER, new FloatDoublewordElement(12816))),
					new FC3ReadRegistersTask(12824, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_10_REACTIVE_POWER, new FloatDoublewordElement(12824)))
				);
		}else if(this.config.sensor_num() == 9) {
			mod = new ModbusProtocol(this,					
					new FC3ReadRegistersTask(11122, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2300_VOLTAGE, new FloatDoublewordElement(11122))),
					new FC3ReadRegistersTask(11150, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2300_FREQUENCY, new FloatDoublewordElement(11150))),
					new FC3ReadRegistersTask(11206, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_1_CURRENT, new FloatDoublewordElement(11206))),
					new FC3ReadRegistersTask(11266, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_1_ACTIVE_POWER, new FloatDoublewordElement(11266))),
					new FC3ReadRegistersTask(11274, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_1_REACTIVE_POWER, new FloatDoublewordElement(11274))),
					new FC3ReadRegistersTask(11356, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_CURRENT, new FloatDoublewordElement(11356))),
					new FC3ReadRegistersTask(11416, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_ACTIVE_POWER, new FloatDoublewordElement(11416))),
					new FC3ReadRegistersTask(11424, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_REACTIVE_POWER, new FloatDoublewordElement(11424))),
					new FC3ReadRegistersTask(11506, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_3_CURRENT, new FloatDoublewordElement(11506))),
					new FC3ReadRegistersTask(11566, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_3_ACTIVE_POWER, new FloatDoublewordElement(11566))),
					new FC3ReadRegistersTask(11574, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_3_REACTIVE_POWER, new FloatDoublewordElement(11574))),
					new FC3ReadRegistersTask(11656, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_4_CURRENT, new FloatDoublewordElement(11656))),
					new FC3ReadRegistersTask(11716, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_4_ACTIVE_POWER, new FloatDoublewordElement(11716))),
					new FC3ReadRegistersTask(11724, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_4_REACTIVE_POWER, new FloatDoublewordElement(11724))),
					new FC3ReadRegistersTask(11906, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_5_CURRENT, new FloatDoublewordElement(11906))),
					new FC3ReadRegistersTask(11966, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_5_ACTIVE_POWER, new FloatDoublewordElement(11966))),
					new FC3ReadRegistersTask(11974, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_5_REACTIVE_POWER, new FloatDoublewordElement(11974))),
					new FC3ReadRegistersTask(12156, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_6_CURRENT, new FloatDoublewordElement(12156))),
					new FC3ReadRegistersTask(12216, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_6_ACTIVE_POWER, new FloatDoublewordElement(12216))),
					new FC3ReadRegistersTask(12224, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_6_REACTIVE_POWER, new FloatDoublewordElement(12224))),
					new FC3ReadRegistersTask(12306, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_7_CURRENT, new FloatDoublewordElement(12306))),
					new FC3ReadRegistersTask(12366, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_7_ACTIVE_POWER, new FloatDoublewordElement(12366))),
					new FC3ReadRegistersTask(12374, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_7_REACTIVE_POWER, new FloatDoublewordElement(12275))),
					new FC3ReadRegistersTask(12456, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_8_CURRENT, new FloatDoublewordElement(12456))),
					new FC3ReadRegistersTask(12516, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_8_ACTIVE_POWER, new FloatDoublewordElement(12516))),
					new FC3ReadRegistersTask(12524, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_8_REACTIVE_POWER, new FloatDoublewordElement(12524))),
					new FC3ReadRegistersTask(12606, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_9_CURRENT, new FloatDoublewordElement(12606))),
					new FC3ReadRegistersTask(12666, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_9_ACTIVE_POWER, new FloatDoublewordElement(12666))),
					new FC3ReadRegistersTask(12674, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_9_REACTIVE_POWER, new FloatDoublewordElement(12674)))
				);
		}else if(this.config.sensor_num() == 8) {
			mod = new ModbusProtocol(this,					
					new FC3ReadRegistersTask(11122, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2300_VOLTAGE, new FloatDoublewordElement(11122))),
					new FC3ReadRegistersTask(11150, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2300_FREQUENCY, new FloatDoublewordElement(11150))),
					new FC3ReadRegistersTask(11206, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_1_CURRENT, new FloatDoublewordElement(11206))),
					new FC3ReadRegistersTask(11266, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_1_ACTIVE_POWER, new FloatDoublewordElement(11266))),
					new FC3ReadRegistersTask(11274, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_1_REACTIVE_POWER, new FloatDoublewordElement(11274))),
					new FC3ReadRegistersTask(11356, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_CURRENT, new FloatDoublewordElement(11356))),
					new FC3ReadRegistersTask(11416, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_ACTIVE_POWER, new FloatDoublewordElement(11416))),
					new FC3ReadRegistersTask(11424, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_REACTIVE_POWER, new FloatDoublewordElement(11424))),
					new FC3ReadRegistersTask(11506, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_3_CURRENT, new FloatDoublewordElement(11506))),
					new FC3ReadRegistersTask(11566, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_3_ACTIVE_POWER, new FloatDoublewordElement(11566))),
					new FC3ReadRegistersTask(11574, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_3_REACTIVE_POWER, new FloatDoublewordElement(11574))),
					new FC3ReadRegistersTask(11656, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_4_CURRENT, new FloatDoublewordElement(11656))),
					new FC3ReadRegistersTask(11716, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_4_ACTIVE_POWER, new FloatDoublewordElement(11716))),
					new FC3ReadRegistersTask(11724, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_4_REACTIVE_POWER, new FloatDoublewordElement(11724))),
					new FC3ReadRegistersTask(11906, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_5_CURRENT, new FloatDoublewordElement(11906))),
					new FC3ReadRegistersTask(11966, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_5_ACTIVE_POWER, new FloatDoublewordElement(11966))),
					new FC3ReadRegistersTask(11974, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_5_REACTIVE_POWER, new FloatDoublewordElement(11974))),
					new FC3ReadRegistersTask(12156, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_6_CURRENT, new FloatDoublewordElement(12156))),
					new FC3ReadRegistersTask(12216, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_6_ACTIVE_POWER, new FloatDoublewordElement(12216))),
					new FC3ReadRegistersTask(12224, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_6_REACTIVE_POWER, new FloatDoublewordElement(12224))),
					new FC3ReadRegistersTask(12306, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_7_CURRENT, new FloatDoublewordElement(12306))),
					new FC3ReadRegistersTask(12366, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_7_ACTIVE_POWER, new FloatDoublewordElement(12366))),
					new FC3ReadRegistersTask(12374, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_7_REACTIVE_POWER, new FloatDoublewordElement(12275))),
					new FC3ReadRegistersTask(12456, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_8_CURRENT, new FloatDoublewordElement(12456))),
					new FC3ReadRegistersTask(12516, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_8_ACTIVE_POWER, new FloatDoublewordElement(12516))),
					new FC3ReadRegistersTask(12524, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_8_REACTIVE_POWER, new FloatDoublewordElement(12524)))
				);
		}else if(this.config.sensor_num() == 7) {
			mod = new ModbusProtocol(this,					
					new FC3ReadRegistersTask(11122, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2300_VOLTAGE, new FloatDoublewordElement(11122))),
					new FC3ReadRegistersTask(11150, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2300_FREQUENCY, new FloatDoublewordElement(11150))),
					new FC3ReadRegistersTask(11206, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_1_CURRENT, new FloatDoublewordElement(11206))),
					new FC3ReadRegistersTask(11266, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_1_ACTIVE_POWER, new FloatDoublewordElement(11266))),
					new FC3ReadRegistersTask(11274, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_1_REACTIVE_POWER, new FloatDoublewordElement(11274))),
					new FC3ReadRegistersTask(11356, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_CURRENT, new FloatDoublewordElement(11356))),
					new FC3ReadRegistersTask(11416, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_ACTIVE_POWER, new FloatDoublewordElement(11416))),
					new FC3ReadRegistersTask(11424, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_REACTIVE_POWER, new FloatDoublewordElement(11424))),
					new FC3ReadRegistersTask(11506, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_3_CURRENT, new FloatDoublewordElement(11506))),
					new FC3ReadRegistersTask(11566, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_3_ACTIVE_POWER, new FloatDoublewordElement(11566))),
					new FC3ReadRegistersTask(11574, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_3_REACTIVE_POWER, new FloatDoublewordElement(11574))),
					new FC3ReadRegistersTask(11656, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_4_CURRENT, new FloatDoublewordElement(11656))),
					new FC3ReadRegistersTask(11716, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_4_ACTIVE_POWER, new FloatDoublewordElement(11716))),
					new FC3ReadRegistersTask(11724, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_4_REACTIVE_POWER, new FloatDoublewordElement(11724))),
					new FC3ReadRegistersTask(11906, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_5_CURRENT, new FloatDoublewordElement(11906))),
					new FC3ReadRegistersTask(11966, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_5_ACTIVE_POWER, new FloatDoublewordElement(11966))),
					new FC3ReadRegistersTask(11974, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_5_REACTIVE_POWER, new FloatDoublewordElement(11974))),
					new FC3ReadRegistersTask(12156, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_6_CURRENT, new FloatDoublewordElement(12156))),
					new FC3ReadRegistersTask(12216, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_6_ACTIVE_POWER, new FloatDoublewordElement(12216))),
					new FC3ReadRegistersTask(12224, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_6_REACTIVE_POWER, new FloatDoublewordElement(12224))),
					new FC3ReadRegistersTask(12306, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_7_CURRENT, new FloatDoublewordElement(12306))),
					new FC3ReadRegistersTask(12366, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_7_ACTIVE_POWER, new FloatDoublewordElement(12366))),
					new FC3ReadRegistersTask(12374, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_7_REACTIVE_POWER, new FloatDoublewordElement(12275)))
				);
		}else if(this.config.sensor_num() == 6) {
			mod = new ModbusProtocol(this,					
					new FC3ReadRegistersTask(11122, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2300_VOLTAGE, new FloatDoublewordElement(11122))),
					new FC3ReadRegistersTask(11150, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2300_FREQUENCY, new FloatDoublewordElement(11150))),
					new FC3ReadRegistersTask(11206, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_1_CURRENT, new FloatDoublewordElement(11206))),
					new FC3ReadRegistersTask(11266, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_1_ACTIVE_POWER, new FloatDoublewordElement(11266))),
					new FC3ReadRegistersTask(11274, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_1_REACTIVE_POWER, new FloatDoublewordElement(11274))),
					new FC3ReadRegistersTask(11356, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_CURRENT, new FloatDoublewordElement(11356))),
					new FC3ReadRegistersTask(11416, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_ACTIVE_POWER, new FloatDoublewordElement(11416))),
					new FC3ReadRegistersTask(11424, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_REACTIVE_POWER, new FloatDoublewordElement(11424))),
					new FC3ReadRegistersTask(11506, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_3_CURRENT, new FloatDoublewordElement(11506))),
					new FC3ReadRegistersTask(11566, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_3_ACTIVE_POWER, new FloatDoublewordElement(11566))),
					new FC3ReadRegistersTask(11574, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_3_REACTIVE_POWER, new FloatDoublewordElement(11574))),
					new FC3ReadRegistersTask(11656, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_4_CURRENT, new FloatDoublewordElement(11656))),
					new FC3ReadRegistersTask(11716, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_4_ACTIVE_POWER, new FloatDoublewordElement(11716))),
					new FC3ReadRegistersTask(11724, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_4_REACTIVE_POWER, new FloatDoublewordElement(11724))),
					new FC3ReadRegistersTask(11906, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_5_CURRENT, new FloatDoublewordElement(11906))),
					new FC3ReadRegistersTask(11966, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_5_ACTIVE_POWER, new FloatDoublewordElement(11966))),
					new FC3ReadRegistersTask(11974, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_5_REACTIVE_POWER, new FloatDoublewordElement(11974))),
					new FC3ReadRegistersTask(12156, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_6_CURRENT, new FloatDoublewordElement(12156))),
					new FC3ReadRegistersTask(12216, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_6_ACTIVE_POWER, new FloatDoublewordElement(12216))),
					new FC3ReadRegistersTask(12224, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_6_REACTIVE_POWER, new FloatDoublewordElement(12224)))
				);
		}else if(this.config.sensor_num() == 5) {
			mod = new ModbusProtocol(this,					
					new FC3ReadRegistersTask(11122, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2300_VOLTAGE, new FloatDoublewordElement(11122))),
					new FC3ReadRegistersTask(11150, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2300_FREQUENCY, new FloatDoublewordElement(11150))),
					new FC3ReadRegistersTask(11206, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_1_CURRENT, new FloatDoublewordElement(11206))),
					new FC3ReadRegistersTask(11266, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_1_ACTIVE_POWER, new FloatDoublewordElement(11266))),
					new FC3ReadRegistersTask(11274, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_1_REACTIVE_POWER, new FloatDoublewordElement(11274))),
					new FC3ReadRegistersTask(11356, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_CURRENT, new FloatDoublewordElement(11356))),
					new FC3ReadRegistersTask(11416, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_ACTIVE_POWER, new FloatDoublewordElement(11416))),
					new FC3ReadRegistersTask(11424, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_REACTIVE_POWER, new FloatDoublewordElement(11424))),
					new FC3ReadRegistersTask(11506, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_3_CURRENT, new FloatDoublewordElement(11506))),
					new FC3ReadRegistersTask(11566, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_3_ACTIVE_POWER, new FloatDoublewordElement(11566))),
					new FC3ReadRegistersTask(11574, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_3_REACTIVE_POWER, new FloatDoublewordElement(11574))),
					new FC3ReadRegistersTask(11656, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_4_CURRENT, new FloatDoublewordElement(11656))),
					new FC3ReadRegistersTask(11716, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_4_ACTIVE_POWER, new FloatDoublewordElement(11716))),
					new FC3ReadRegistersTask(11724, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_4_REACTIVE_POWER, new FloatDoublewordElement(11724))),
					new FC3ReadRegistersTask(11906, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_5_CURRENT, new FloatDoublewordElement(11906))),
					new FC3ReadRegistersTask(11966, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_5_ACTIVE_POWER, new FloatDoublewordElement(11966))),
					new FC3ReadRegistersTask(11974, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_5_REACTIVE_POWER, new FloatDoublewordElement(11974)))
				);
		}else if(this.config.sensor_num() == 4) {
			mod = new ModbusProtocol(this,					
					new FC3ReadRegistersTask(11122, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2300_VOLTAGE, new FloatDoublewordElement(11122))),
					new FC3ReadRegistersTask(11150, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2300_FREQUENCY, new FloatDoublewordElement(11150))),
					new FC3ReadRegistersTask(11206, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_1_CURRENT, new FloatDoublewordElement(11206))),
					new FC3ReadRegistersTask(11266, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_1_ACTIVE_POWER, new FloatDoublewordElement(11266))),
					new FC3ReadRegistersTask(11274, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_1_REACTIVE_POWER, new FloatDoublewordElement(11274))),
					new FC3ReadRegistersTask(11356, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_CURRENT, new FloatDoublewordElement(11356))),
					new FC3ReadRegistersTask(11416, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_ACTIVE_POWER, new FloatDoublewordElement(11416))),
					new FC3ReadRegistersTask(11424, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_REACTIVE_POWER, new FloatDoublewordElement(11424))),
					new FC3ReadRegistersTask(11506, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_3_CURRENT, new FloatDoublewordElement(11506))),
					new FC3ReadRegistersTask(11566, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_3_ACTIVE_POWER, new FloatDoublewordElement(11566))),
					new FC3ReadRegistersTask(11574, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_3_REACTIVE_POWER, new FloatDoublewordElement(11574))),
					new FC3ReadRegistersTask(11656, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_4_CURRENT, new FloatDoublewordElement(11656))),
					new FC3ReadRegistersTask(11716, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_4_ACTIVE_POWER, new FloatDoublewordElement(11716))),
					new FC3ReadRegistersTask(11724, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_4_REACTIVE_POWER, new FloatDoublewordElement(11724)))
				);
		}else if(this.config.sensor_num() == 3) {
			mod = new ModbusProtocol(this,					
					new FC3ReadRegistersTask(11122, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2300_VOLTAGE, new FloatDoublewordElement(11122))),
					new FC3ReadRegistersTask(11150, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2300_FREQUENCY, new FloatDoublewordElement(11150))),
					new FC3ReadRegistersTask(11206, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_1_CURRENT, new FloatDoublewordElement(11206))),
					new FC3ReadRegistersTask(11266, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_1_ACTIVE_POWER, new FloatDoublewordElement(11266))),
					new FC3ReadRegistersTask(11274, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_1_REACTIVE_POWER, new FloatDoublewordElement(11274))),
					new FC3ReadRegistersTask(11356, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_CURRENT, new FloatDoublewordElement(11356))),
					new FC3ReadRegistersTask(11416, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_ACTIVE_POWER, new FloatDoublewordElement(11416))),
					new FC3ReadRegistersTask(11424, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_REACTIVE_POWER, new FloatDoublewordElement(11424))),
					new FC3ReadRegistersTask(11506, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_3_CURRENT, new FloatDoublewordElement(11506))),
					new FC3ReadRegistersTask(11566, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_3_ACTIVE_POWER, new FloatDoublewordElement(11566))),
					new FC3ReadRegistersTask(11574, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_3_REACTIVE_POWER, new FloatDoublewordElement(11574)))
				);
		} else if(this.config.sensor_num() == 2) {
			mod = new ModbusProtocol(this,				
					new FC3ReadRegistersTask(11122, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2300_VOLTAGE, new FloatDoublewordElement(11122))),
					new FC3ReadRegistersTask(11150, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2300_FREQUENCY, new FloatDoublewordElement(11150))),
					new FC3ReadRegistersTask(11206, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_1_CURRENT, new FloatDoublewordElement(11206))),
					new FC3ReadRegistersTask(11266, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_1_ACTIVE_POWER, new FloatDoublewordElement(11266))),
					new FC3ReadRegistersTask(11274, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_1_REACTIVE_POWER, new FloatDoublewordElement(11274))),
					new FC3ReadRegistersTask(11356, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_CURRENT, new FloatDoublewordElement(11356))),
					new FC3ReadRegistersTask(11416, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_ACTIVE_POWER, new FloatDoublewordElement(11416))),
					new FC3ReadRegistersTask(11424, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_REACTIVE_POWER, new FloatDoublewordElement(11424)))
				);
		} else {		
		// sensor_num == 1  일때 시
		// 이 방식은 데이터 종류별로 묶어서 컨넥션 횟수를 줄일 수 있
			mod =  new ModbusProtocol(this,
					// Voltage Data of Accura 2300[S]
					new FC3ReadRegistersTask(11122, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2300_VOLTAGE, new FloatDoublewordElement(11122))),
					new FC3ReadRegistersTask(11150, Priority.LOW,
					    // 11150  Frequency  Float32  PR  입력 전압 주파수. 단[Hz]
						m(Accura2300.ChannelId.ACCURA2300_FREQUENCY, new FloatDoublewordElement(11150))),								
						// subunit   모듈 기기(센싱  기기)
						// Module ID 0의 시작 Number는 11201   // Module ID === Sensor Num (Accura2350)
						// 기본값으로 Accura 2350  1개가 설치됐다고 가정 
//						new DummyRegisterElement(11201),
						// 모든상의 전류의 평균 11206
					new FC3ReadRegistersTask(11206, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_1_CURRENT, new FloatDoublewordElement(11206))),								
						// 11266  유효전력[kW]
					new FC3ReadRegistersTask(11266, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_1_ACTIVE_POWER, new FloatDoublewordElement(11266))),								
						// 11274  무효전력[KVAR total]
					new FC3ReadRegistersTask(11274, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_1_REACTIVE_POWER, new FloatDoublewordElement(11274)))								
				);
		}
		
		return mod;
		
//		11266  유효전력[kW]    11274  무효전력[kVAR]     11283 피상전력[kVA]      11285 누설전류[A]
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
//		readRgVoltage = new FC3ReadRegistersTask(11122, Priority.LOW,
//			m(SymmetricMeter.ChannelId.VOLTAGE, new FloatDoublewo11122ment(11122)));
//		    // 11150  Frequency  Float32  PR  입력 전압 주파수. 단[Hz] 		
//		readRgFrequency = new FC3ReadRegistersTask(11150, Priority.LOW,
//			m(SymmetricMeter.ChannelId.FREQUENCY, new FloatDoublewordElement(11150)));
//		
//		for(int i=0; i< num_sensor; i++) {
//			// subunit   모듈 기기(센싱  기기)
//			// Module ID 0의 시작 Number는 11201   // Module ID === Sensor Num (Accura2350)
//			// 기본값으로 Accura 2350  1개가 설치됐다고 가정 
//			// 모든상의 전류의 평균 11206
//			readRg[i][0] = new FC3ReadRegistersTask(11206, Priority.LOW,
//				m(Accura2300.ChannelId.ACCURA2350_1_CURRENT, new FloatDoublewordElement(11206+(150*i))));
//			
//				// 11266  유효전력[kW]
//			readRg[i][1] = new FC3ReadRegistersTask(11266, Priority.LOW,
//				m(Accura2300.ChannelId.ACCURA2350_1_ACTIVE_POWER, new FloatDoublewordElement(11266+(150*i))));
//						
//				// 11274  무효전력[KVAR total]
//			readRg[i][2] = new FC3ReadRegistersTask(11274, Priority.LOW,
//					m(Accura2300.ChannelId.ACCURA2350_1_REACTIVE_POWER, new FloatDoublewordElement(11274+(150*i))));
//
//		}			
//		
//		return new ModbusProtocol(this, readRgVoltage, readRgFrequency, readRg.length);
//	}
	
}
