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
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
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
		// register 862  Wiring mode  결선모드 UInt16    1p or 3p    // 1p 와 3p 따라서 Modbus Map 다
		// register 1112  CT(Accura2350) 갯수 관리 대상 모듈의 수.  UInt16 
		// register 11045	Validity of Accura 2300[S] Voltage data, UInt16  1: Accura 2300[S] 전압 데이터가 정상적 fetch 됨.
		// register 11046 ~ +39  CT 모듈 데이터유효성체크  UInt16   -1 = 비유효,  0 = 3상, 1 = 1상 
		
		ACCURA2300_VOLTAGE(Doc.of(OpenemsType.FLOAT)  // 삼상전압의기본파성분평균.단위[V]  //  REGISTER  11123
				.unit(Unit.VOLT)),
		ACCURA2300_FREQUENCY(Doc.of(OpenemsType.FLOAT) // 입력 전압 주파수. 단위 [Hz]   //  REGISTER  11151
				.unit(Unit.HERTZ)),
		ACCURA2300_WIRING_MODE(Doc.of(OpenemsType.INTEGER) //  Wiring mode  결선모드 UInt16    1p or 3p 
				.unit(Unit.NONE)),
		ACCURA2300_CT_CONNECTED_NUM(Doc.of(OpenemsType.INTEGER) //  CT(Accura2350) 갯수 관리 대상 모듈의 수.
				.unit(Unit.NONE)),
		ACCURA2300_VALIDITY_VOLTAGE_DATA(Doc.of(OpenemsType.INTEGER) //  Validity of Accura 2300[S] Voltage data
				.unit(Unit.NONE)),
		ACCURA2300_VALIDITY_CT_DATA(Doc.of(OpenemsType.INTEGER) //  CT 모듈 데이터유효성체크
				.unit(Unit.NONE)),
		
		ACCURA2350_1_CURRENT(Doc.of(OpenemsType.FLOAT) // 삼상전류평균.단위[A]  //  REGISTER  +16    간격 150
				.unit(Unit.AMPERE)),
		ACCURA2350_1_ACTIVE_POWER(Doc.of(OpenemsType.FLOAT) // 삼상의 유효전력 총합. 단위 [kW]  //  REGISTER  +66
				.unit(Unit.KILOWATT)),		
		ACCURA2350_1_REACTIVE_POWER(Doc.of(OpenemsType.FLOAT) // 삼상의 무효전력 총합. 단위 [kVAR] //  REGISTER  +74
				.unit(Unit.KILOVOLT_AMPERE_REACTIVE)),
		ACCURA2350_1_APPARENT_POWER(Doc.of(OpenemsType.FLOAT) // 삼상의 피상전력 총합. 단위 [kVA]  //  REGISTER  +82
				.unit(Unit.KILOVOLT_AMPERE)),  
		ACCURA2350_1_NET_KWH(Doc.of(OpenemsType.INTEGER) //  수전 유효전력량과 송전 유효전력량의 REGISTER  +92
				.unit(Unit.KILOWATT_HOURS)),
		ACCURA2350_1_POWER_FACTOR(Doc.of(OpenemsType.FLOAT) //  Total 역률.		//  REGISTER  +130
				.unit(Unit.NONE)),		
		ACCURA2350_2_CURRENT(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.AMPERE)),		
		ACCURA2350_2_ACTIVE_POWER(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.KILOWATT)),			
		ACCURA2350_2_REACTIVE_POWER(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.KILOVOLT_AMPERE_REACTIVE)),
		ACCURA2350_2_APPARENT_POWER(Doc.of(OpenemsType.FLOAT) // 삼상의 피상전력 총합. 단위 [kVA]  //  REGISTER  +82
				.unit(Unit.KILOVOLT_AMPERE)),  
		ACCURA2350_2_NET_KWH(Doc.of(OpenemsType.INTEGER) //  수전 유효전력량과 송전 유효전력량의 REGISTER  +92
				.unit(Unit.KILOWATT_HOURS)),
		ACCURA2350_2_POWER_FACTOR(Doc.of(OpenemsType.FLOAT) //  Total 역률.		//  REGISTER  +130
				.unit(Unit.NONE)),
		ACCURA2350_3_CURRENT(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.AMPERE)),	
		ACCURA2350_3_ACTIVE_POWER(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.KILOWATT)),		
		ACCURA2350_3_REACTIVE_POWER(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.KILOVOLT_AMPERE_REACTIVE)),
		ACCURA2350_3_APPARENT_POWER(Doc.of(OpenemsType.FLOAT) // 삼상의 피상전력 총합. 단위 [kVA]  //  REGISTER  +82
				.unit(Unit.KILOVOLT_AMPERE)),  
		ACCURA2350_3_NET_KWH(Doc.of(OpenemsType.INTEGER) //  수전 유효전력량과 송전 유효전력량의 REGISTER  +92
				.unit(Unit.KILOWATT_HOURS)),
		ACCURA2350_3_POWER_FACTOR(Doc.of(OpenemsType.FLOAT) //  Total 역률.		//  REGISTER  +130
				.unit(Unit.NONE)),
		ACCURA2350_4_CURRENT(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.AMPERE)),
		ACCURA2350_4_ACTIVE_POWER(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.KILOWATT)),		
		ACCURA2350_4_REACTIVE_POWER(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.KILOVOLT_AMPERE_REACTIVE)),
		ACCURA2350_4_APPARENT_POWER(Doc.of(OpenemsType.FLOAT) // 삼상의 피상전력 총합. 단위 [kVA]  //  REGISTER  +82
				.unit(Unit.KILOVOLT_AMPERE)),
		ACCURA2350_4_NET_KWH(Doc.of(OpenemsType.INTEGER) //  수전 유효전력량과 송전 유효전력량의 REGISTER  +92
				.unit(Unit.KILOWATT_HOURS)),
		ACCURA2350_4_POWER_FACTOR(Doc.of(OpenemsType.FLOAT) //  Total 역률.		//  REGISTER  +130
				.unit(Unit.NONE)),
		ACCURA2350_5_CURRENT(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.AMPERE)),	
		ACCURA2350_5_ACTIVE_POWER(Doc.of(OpenemsType.FLOAT) // 해당파트의 오류 잡기 위해서 바꿈 
				.unit(Unit.KILOWATT)),		
		ACCURA2350_5_REACTIVE_POWER(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.KILOVOLT_AMPERE_REACTIVE)),
		ACCURA2350_5_APPARENT_POWER(Doc.of(OpenemsType.FLOAT) // 삼상의 피상전력 총합. 단위 [kVA]  //  REGISTER  +82
				.unit(Unit.KILOVOLT_AMPERE)),  
		ACCURA2350_5_NET_KWH(Doc.of(OpenemsType.INTEGER) //  수전 유효전력량과 송전 유효전력량의 REGISTER  +92
				.unit(Unit.KILOWATT_HOURS)),
		ACCURA2350_5_POWER_FACTOR(Doc.of(OpenemsType.FLOAT) //  Total 역률.		//  REGISTER  +130
				.unit(Unit.NONE)),
		ACCURA2350_6_CURRENT(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.AMPERE)),	
		ACCURA2350_6_ACTIVE_POWER(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.KILOWATT)),
		ACCURA2350_6_REACTIVE_POWER(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.KILOVOLT_AMPERE_REACTIVE)),
		ACCURA2350_6_APPARENT_POWER(Doc.of(OpenemsType.FLOAT) // 삼상의 피상전력 총합. 단위 [kVA]  //  REGISTER  +82
				.unit(Unit.KILOVOLT_AMPERE)),
		ACCURA2350_6_NET_KWH(Doc.of(OpenemsType.INTEGER) //  수전 유효전력량과 송전 유효전력량의 REGISTER  +92
				.unit(Unit.KILOWATT_HOURS)),
		ACCURA2350_6_POWER_FACTOR(Doc.of(OpenemsType.FLOAT) //  Total 역률.		//  REGISTER  +130
				.unit(Unit.NONE)),
		ACCURA2350_7_CURRENT(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.AMPERE)),
		ACCURA2350_7_ACTIVE_POWER(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.KILOWATT)),		
		ACCURA2350_7_REACTIVE_POWER(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.KILOVOLT_AMPERE_REACTIVE)),
		ACCURA2350_7_APPARENT_POWER(Doc.of(OpenemsType.FLOAT) // 삼상의 피상전력 총합. 단위 [kVA]  //  REGISTER  +82
				.unit(Unit.KILOVOLT_AMPERE)),
		ACCURA2350_7_NET_KWH(Doc.of(OpenemsType.INTEGER) //  수전 유효전력량과 송전 유효전력량의 REGISTER  +92
				.unit(Unit.KILOWATT_HOURS)),
		ACCURA2350_7_POWER_FACTOR(Doc.of(OpenemsType.FLOAT) //  Total 역률.		//  REGISTER  +130
				.unit(Unit.NONE)),			
		ACCURA2350_8_CURRENT(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.AMPERE)),		
		ACCURA2350_8_ACTIVE_POWER(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.KILOWATT)),			
		ACCURA2350_8_REACTIVE_POWER(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.KILOVOLT_AMPERE_REACTIVE)),
		ACCURA2350_8_APPARENT_POWER(Doc.of(OpenemsType.FLOAT) // 삼상의 피상전력 총합. 단위 [kVA]  //  REGISTER  +82
				.unit(Unit.KILOVOLT_AMPERE)), 
		ACCURA2350_8_NET_KWH(Doc.of(OpenemsType.INTEGER) //  수전 유효전력량과 송전 유효전력량의 REGISTER  +92
				.unit(Unit.KILOWATT_HOURS)),
		ACCURA2350_8_POWER_FACTOR(Doc.of(OpenemsType.FLOAT) //  Total 역률.		//  REGISTER  +130
				.unit(Unit.NONE)),
		ACCURA2350_9_CURRENT(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.AMPERE)),	
		ACCURA2350_9_ACTIVE_POWER(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.KILOWATT)),		
		ACCURA2350_9_REACTIVE_POWER(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.KILOVOLT_AMPERE_REACTIVE)),
		ACCURA2350_9_APPARENT_POWER(Doc.of(OpenemsType.FLOAT) // 삼상의 피상전력 총합. 단위 [kVA]  //  REGISTER  +82
				.unit(Unit.KILOVOLT_AMPERE)), 
		ACCURA2350_9_NET_KWH(Doc.of(OpenemsType.INTEGER) //  수전 유효전력량과 송전 유효전력량의 REGISTER  +92
				.unit(Unit.KILOWATT_HOURS)),
		ACCURA2350_9_POWER_FACTOR(Doc.of(OpenemsType.FLOAT) //  Total 역률.		//  REGISTER  +130
				.unit(Unit.NONE)),
		ACCURA2350_10_CURRENT(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.AMPERE)),
		ACCURA2350_10_ACTIVE_POWER(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.KILOWATT)),		
		ACCURA2350_10_REACTIVE_POWER(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.KILOVOLT_AMPERE_REACTIVE)),
		ACCURA2350_10_APPARENT_POWER(Doc.of(OpenemsType.FLOAT) // 삼상의 피상전력 총합. 단위 [kVA]  //  REGISTER  +82
				.unit(Unit.KILOVOLT_AMPERE)),
		ACCURA2350_10_NET_KWH(Doc.of(OpenemsType.INTEGER) //  수전 유효전력량과 송전 유효전력량의 REGISTER  +92
				.unit(Unit.KILOWATT_HOURS)),
		ACCURA2350_10_POWER_FACTOR(Doc.of(OpenemsType.FLOAT) //  Total 역률.		//  REGISTER  +130
				.unit(Unit.NONE)),
		ACCURA2350_11_CURRENT(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.AMPERE)),	
		ACCURA2350_11_ACTIVE_POWER(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.KILOWATT)),		
		ACCURA2350_11_REACTIVE_POWER(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.KILOVOLT_AMPERE_REACTIVE)),
		ACCURA2350_11_APPARENT_POWER(Doc.of(OpenemsType.FLOAT) // 삼상의 피상전력 총합. 단위 [kVA]  //  REGISTER  +82
				.unit(Unit.KILOVOLT_AMPERE)), 
		ACCURA2350_11_NET_KWH(Doc.of(OpenemsType.INTEGER) //  수전 유효전력량과 송전 유효전력량의 REGISTER  +92
				.unit(Unit.KILOWATT_HOURS)),
		ACCURA2350_11_POWER_FACTOR(Doc.of(OpenemsType.FLOAT) //  Total 역률.		//  REGISTER  +130
				.unit(Unit.NONE)),
		ACCURA2350_12_CURRENT(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.AMPERE)),	
		ACCURA2350_12_ACTIVE_POWER(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.KILOWATT)),
		ACCURA2350_12_REACTIVE_POWER(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.KILOVOLT_AMPERE_REACTIVE)),
		ACCURA2350_12_APPARENT_POWER(Doc.of(OpenemsType.FLOAT) // 삼상의 피상전력 총합. 단위 [kVA]  //  REGISTER  +82
				.unit(Unit.KILOVOLT_AMPERE)), 
		ACCURA2350_12_NET_KWH(Doc.of(OpenemsType.INTEGER) //  수전 유효전력량과 송전 유효전력량의 REGISTER  +92
				.unit(Unit.KILOWATT_HOURS)),
		ACCURA2350_12_POWER_FACTOR(Doc.of(OpenemsType.FLOAT) //  Total 역률.		//  REGISTER  +130
				.unit(Unit.NONE)),
		ACCURA2350_13_CURRENT(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.AMPERE)),	
		ACCURA2350_13_ACTIVE_POWER(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.KILOWATT)),		
		ACCURA2350_13_REACTIVE_POWER(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.KILOVOLT_AMPERE_REACTIVE)),
		ACCURA2350_13_APPARENT_POWER(Doc.of(OpenemsType.FLOAT) // 삼상의 피상전력 총합. 단위 [kVA]  //  REGISTER  +82
				.unit(Unit.KILOVOLT_AMPERE)),  
		ACCURA2350_13_NET_KWH(Doc.of(OpenemsType.INTEGER) //  수전 유효전력량과 송전 유효전력량의 REGISTER  +92
				.unit(Unit.KILOWATT_HOURS)),
		ACCURA2350_13_POWER_FACTOR(Doc.of(OpenemsType.FLOAT) //  Total 역률.		//  REGISTER  +130
				.unit(Unit.NONE)),
		ACCURA2350_14_CURRENT(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.AMPERE)),	
		ACCURA2350_14_ACTIVE_POWER(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.KILOWATT)),
		ACCURA2350_14_REACTIVE_POWER(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.KILOVOLT_AMPERE_REACTIVE)),
		ACCURA2350_14_APPARENT_POWER(Doc.of(OpenemsType.FLOAT) // 삼상의 피상전력 총합. 단위 [kVA]  //  REGISTER  +82
				.unit(Unit.KILOVOLT_AMPERE)),  
		ACCURA2350_14_NET_KWH(Doc.of(OpenemsType.INTEGER) //  수전 유효전력량과 송전 유효전력량의 REGISTER  +92
				.unit(Unit.KILOWATT_HOURS)),
		ACCURA2350_14_POWER_FACTOR(Doc.of(OpenemsType.FLOAT) //  Total 역률.		//  REGISTER  +130
				.unit(Unit.NONE)),
		ACCURA2350_15_CURRENT(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.AMPERE)),	
		ACCURA2350_15_ACTIVE_POWER(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.KILOWATT)),		
		ACCURA2350_15_REACTIVE_POWER(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.KILOVOLT_AMPERE_REACTIVE)),
		ACCURA2350_15_APPARENT_POWER(Doc.of(OpenemsType.FLOAT) // 삼상의 피상전력 총합. 단위 [kVA]  //  REGISTER  +82
				.unit(Unit.KILOVOLT_AMPERE)),  
		ACCURA2350_15_NET_KWH(Doc.of(OpenemsType.INTEGER) //  수전 유효전력량과 송전 유효전력량의 REGISTER  +92
				.unit(Unit.KILOWATT_HOURS)),
		ACCURA2350_15_POWER_FACTOR(Doc.of(OpenemsType.FLOAT) //  Total 역률.		//  REGISTER  +130
				.unit(Unit.NONE)),
		ACCURA2350_16_CURRENT(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.AMPERE)),	
		ACCURA2350_16_ACTIVE_POWER(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.KILOWATT)),
		ACCURA2350_16_REACTIVE_POWER(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.KILOVOLT_AMPERE_REACTIVE)),
		ACCURA2350_16_APPARENT_POWER(Doc.of(OpenemsType.FLOAT) // 삼상의 피상전력 총합. 단위 [kVA]  //  REGISTER  +82
				.unit(Unit.KILOVOLT_AMPERE)),  
		ACCURA2350_16_NET_KWH(Doc.of(OpenemsType.INTEGER) //  수전 유효전력량과 송전 유효전력량의 REGISTER  +92
				.unit(Unit.KILOWATT_HOURS)),
		ACCURA2350_16_POWER_FACTOR(Doc.of(OpenemsType.FLOAT) //  Total 역률.		//  REGISTER  +130
				.unit(Unit.NONE)),
		ACCURA2350_17_CURRENT(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.AMPERE)),	
		ACCURA2350_17_ACTIVE_POWER(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.KILOWATT)),		
		ACCURA2350_17_REACTIVE_POWER(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.KILOVOLT_AMPERE_REACTIVE)),
		ACCURA2350_17_APPARENT_POWER(Doc.of(OpenemsType.FLOAT) // 삼상의 피상전력 총합. 단위 [kVA]  //  REGISTER  +82
				.unit(Unit.KILOVOLT_AMPERE)), 
		ACCURA2350_17_NET_KWH(Doc.of(OpenemsType.INTEGER) //  수전 유효전력량과 송전 유효전력량의 REGISTER  +92
				.unit(Unit.KILOWATT_HOURS)),
		ACCURA2350_17_POWER_FACTOR(Doc.of(OpenemsType.FLOAT) //  Total 역률.		//  REGISTER  +130
				.unit(Unit.NONE)),
		ACCURA2350_18_CURRENT(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.AMPERE)),	
		ACCURA2350_18_ACTIVE_POWER(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.KILOWATT)),
		ACCURA2350_18_REACTIVE_POWER(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.KILOVOLT_AMPERE_REACTIVE)),
		ACCURA2350_18_APPARENT_POWER(Doc.of(OpenemsType.FLOAT) // 삼상의 피상전력 총합. 단위 [kVA]  //  REGISTER  +82
				.unit(Unit.KILOVOLT_AMPERE)),
		ACCURA2350_18_NET_KWH(Doc.of(OpenemsType.INTEGER) //  수전 유효전력량과 송전 유효전력량의 REGISTER  +92
				.unit(Unit.KILOWATT_HOURS)),
		ACCURA2350_18_POWER_FACTOR(Doc.of(OpenemsType.FLOAT) //  Total 역률.		//  REGISTER  +130
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
				+ this.getAccura2350Apparent01().value().asString() + " / "
				+ this.getAccura2350PowerFactor01().value().asString() + " / "
				
				+ this.getAccura2300Wiring_Mode().value().asString() + " / "
				+ this.getAccura2300_CT_Connected_Num().value().asString() + " / "
				+ this.getAccura2300_Validity_CT_Data().value().asString() + " / "
				+ this.getAccura2300_Validity_Voltage_Data().value().asString() + " / "
				+ this.getAccura2350NetkWh01().value().asString();
	}
	
	public Channel<Float> getAccuraVoltage() {
		return this.channel(ChannelId.ACCURA2300_VOLTAGE);
	}	
	public Channel<Float> getAccuraFrequency() {
		return this.channel(ChannelId.ACCURA2300_FREQUENCY);
	}
	public Channel<Integer> getAccura2300Wiring_Mode() {
		return this.channel(ChannelId.ACCURA2300_WIRING_MODE);
	}
	public Channel<Integer> getAccura2300_CT_Connected_Num() {
		return this.channel(ChannelId.ACCURA2300_CT_CONNECTED_NUM);
	}
	public Channel<Integer> getAccura2300_Validity_Voltage_Data() {
		return this.channel(ChannelId.ACCURA2300_VALIDITY_VOLTAGE_DATA);
	}
	public Channel<Integer> getAccura2300_Validity_CT_Data() {
		return this.channel(ChannelId.ACCURA2300_VALIDITY_CT_DATA);
	}
	public Channel<Integer> getAccura2350NetkWh01() {
		return this.channel(ChannelId.ACCURA2350_1_NET_KWH);
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
	public Channel<Float> getAccura2350Apparent01() {
		return this.channel(ChannelId.ACCURA2350_1_APPARENT_POWER);
	}	
	public Channel<Float> getAccura2350PowerFactor01() {
		return this.channel(ChannelId.ACCURA2350_1_POWER_FACTOR);
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
	public Channel<Float> getAccura2350Apparent02() {
		return this.channel(ChannelId.ACCURA2350_2_APPARENT_POWER);
	}	
	public Channel<Float> getAccura2350PowerFactor02() {
		return this.channel(ChannelId.ACCURA2350_2_POWER_FACTOR);
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
	public Channel<Float> getAccura2350Apparent03() {
		return this.channel(ChannelId.ACCURA2350_3_APPARENT_POWER);
	}	
	public Channel<Float> getAccura2350PowerFactor03() {
		return this.channel(ChannelId.ACCURA2350_3_POWER_FACTOR);
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
	public Channel<Float> getAccura2350Apparent10() {
		return this.channel(ChannelId.ACCURA2350_10_APPARENT_POWER);
	}	
	public Channel<Float> getAccura2350PowerFactor10() {
		return this.channel(ChannelId.ACCURA2350_10_POWER_FACTOR);
	}

	
	@Override
	public MeterType getMeterType() {
		// TODO Auto-generated method stub
		return this.config.type();
	}
	
	@Override
	protected ModbusProtocol defineModbusProtocol() {
		// TODO implement ModbusProtocol
		// register 862  Wiring mode  결선모드 UInt16    1p or 3p    // 1p 와 3p 따라서 Modbus Map 다
		// register 1112  CT(Accura2350) 갯수 관리 대상 모듈의 수.  UInt16 
		// register 11001  Aggregation selection  UInt16    1: (default) Aggregation 1 (1초), Max/Min 포함
		// register 11045	Validity of Accura 2300[S] Voltage data, UInt16  1: Accura 2300[S] 전압 데이터가 정상적 fetch 됨.
		// register 11046 ~ +39  CT 모듈 데이터유효성체크  UInt16   -1 = 비유효,  0 = 3상, 1 = 1상 
		// 본 프로그램 가동전에 해당 CT 들이 정상 작동하는 지 유효성체크 후에 가동 시킨다.
		// 설명 링크 : https://blog.naver.com/kjamjalee/222093298925
//		11266  유효전력[kW]    11274  무효전력[kVAR]     11283 피상전력[kVA]      11331  역률
		
		ModbusProtocol mod = null;
		
		if(this.config.sensor_num() == 18) {
			mod = new ModbusProtocol(this,			
					// Java 는 레지스터 넘버 0 번 부터 시작.
					new FC3ReadRegistersTask(861, Priority.LOW,	
							m(Accura2300.ChannelId.ACCURA2300_WIRING_MODE, new UnsignedWordElement(861))),
					new FC3ReadRegistersTask(1111, Priority.LOW,	
							m(Accura2300.ChannelId.ACCURA2300_CT_CONNECTED_NUM, new UnsignedWordElement(1111))),
					new FC3ReadRegistersTask(11044, Priority.LOW,	
							m(Accura2300.ChannelId.ACCURA2300_VALIDITY_VOLTAGE_DATA, new UnsignedWordElement(11044)),
							m(Accura2300.ChannelId.ACCURA2300_VALIDITY_CT_DATA, new UnsignedWordElement(11045)),
							m(Accura2300.ChannelId.ACCURA2300_VALIDITY_CT_DATA, new UnsignedWordElement(11046)),
							m(Accura2300.ChannelId.ACCURA2300_VALIDITY_CT_DATA, new UnsignedWordElement(11047)),
							m(Accura2300.ChannelId.ACCURA2300_VALIDITY_CT_DATA, new UnsignedWordElement(11048)),
							m(Accura2300.ChannelId.ACCURA2300_VALIDITY_CT_DATA, new UnsignedWordElement(11049)),
							m(Accura2300.ChannelId.ACCURA2300_VALIDITY_CT_DATA, new UnsignedWordElement(11050)),
							m(Accura2300.ChannelId.ACCURA2300_VALIDITY_CT_DATA, new UnsignedWordElement(11051)),
							m(Accura2300.ChannelId.ACCURA2300_VALIDITY_CT_DATA, new UnsignedWordElement(11052)),
							m(Accura2300.ChannelId.ACCURA2300_VALIDITY_CT_DATA, new UnsignedWordElement(11053)),
							m(Accura2300.ChannelId.ACCURA2300_VALIDITY_CT_DATA, new UnsignedWordElement(11054)),
							m(Accura2300.ChannelId.ACCURA2300_VALIDITY_CT_DATA, new UnsignedWordElement(11055)),
							m(Accura2300.ChannelId.ACCURA2300_VALIDITY_CT_DATA, new UnsignedWordElement(11056)),
							m(Accura2300.ChannelId.ACCURA2300_VALIDITY_CT_DATA, new UnsignedWordElement(11057)),
							m(Accura2300.ChannelId.ACCURA2300_VALIDITY_CT_DATA, new UnsignedWordElement(11058)),
							m(Accura2300.ChannelId.ACCURA2300_VALIDITY_CT_DATA, new UnsignedWordElement(11059)),
							m(Accura2300.ChannelId.ACCURA2300_VALIDITY_CT_DATA, new UnsignedWordElement(11060)),
							m(Accura2300.ChannelId.ACCURA2300_VALIDITY_CT_DATA, new UnsignedWordElement(11061)),
							m(Accura2300.ChannelId.ACCURA2300_VALIDITY_CT_DATA, new UnsignedWordElement(11062))),
					
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
					new FC3ReadRegistersTask(11282, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_1_APPARENT_POWER, new FloatDoublewordElement(11282))),
					new FC3ReadRegistersTask(11292, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_1_NET_KWH, new UnsignedDoublewordElement(11292))),
					new FC3ReadRegistersTask(11330, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_1_POWER_FACTOR, new FloatDoublewordElement(11330))),
					
					new FC3ReadRegistersTask(11356, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_CURRENT, new FloatDoublewordElement(11356))),
					new FC3ReadRegistersTask(11416, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_ACTIVE_POWER, new FloatDoublewordElement(11416))),
					new FC3ReadRegistersTask(11424, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_REACTIVE_POWER, new FloatDoublewordElement(11424))),
					new FC3ReadRegistersTask(11432, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_APPARENT_POWER, new FloatDoublewordElement(11432))),
					new FC3ReadRegistersTask(11442, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_NET_KWH, new UnsignedDoublewordElement(11442))),
					new FC3ReadRegistersTask(11480, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_POWER_FACTOR, new FloatDoublewordElement(11480))),
					
					new FC3ReadRegistersTask(11506, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_3_CURRENT, new FloatDoublewordElement(11506))),
					new FC3ReadRegistersTask(11566, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_3_ACTIVE_POWER, new FloatDoublewordElement(11566))),
					new FC3ReadRegistersTask(11574, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_3_REACTIVE_POWER, new FloatDoublewordElement(11574))),
					new FC3ReadRegistersTask(11582, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_3_APPARENT_POWER, new FloatDoublewordElement(11582))),
					new FC3ReadRegistersTask(11592, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_3_NET_KWH, new UnsignedDoublewordElement(11592))),
					new FC3ReadRegistersTask(11630, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_3_POWER_FACTOR, new FloatDoublewordElement(11630))),
					
					new FC3ReadRegistersTask(11657, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_4_CURRENT, new FloatDoublewordElement(11656))),
					new FC3ReadRegistersTask(11716, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_4_ACTIVE_POWER, new FloatDoublewordElement(11716))),
					new FC3ReadRegistersTask(11724, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_4_REACTIVE_POWER, new FloatDoublewordElement(11724))),
					new FC3ReadRegistersTask(11732, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_4_APPARENT_POWER, new FloatDoublewordElement(11732))),
					new FC3ReadRegistersTask(11742, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_4_NET_KWH, new UnsignedDoublewordElement(11742))),
					new FC3ReadRegistersTask(11780, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_4_POWER_FACTOR, new FloatDoublewordElement(11780))),
					
					new FC3ReadRegistersTask(11906, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_5_CURRENT, new FloatDoublewordElement(11906))),
					new FC3ReadRegistersTask(11966, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_5_ACTIVE_POWER, new FloatDoublewordElement(11966))),
					new FC3ReadRegistersTask(11974, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_5_REACTIVE_POWER, new FloatDoublewordElement(11974))),
					new FC3ReadRegistersTask(11982, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_5_APPARENT_POWER, new FloatDoublewordElement(11982))),
					new FC3ReadRegistersTask(11992, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_5_NET_KWH, new UnsignedDoublewordElement(11992))),
					new FC3ReadRegistersTask(12030, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_5_POWER_FACTOR, new FloatDoublewordElement(12030))),
					
					new FC3ReadRegistersTask(12156, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_6_CURRENT, new FloatDoublewordElement(12156))),
					new FC3ReadRegistersTask(12216, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_6_ACTIVE_POWER, new FloatDoublewordElement(12216))),
					new FC3ReadRegistersTask(12224, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_6_REACTIVE_POWER, new FloatDoublewordElement(12224))),
					new FC3ReadRegistersTask(12232, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_6_APPARENT_POWER, new FloatDoublewordElement(12232))),
					new FC3ReadRegistersTask(12242, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_6_NET_KWH, new UnsignedDoublewordElement(12242))),
					new FC3ReadRegistersTask(12280, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_6_POWER_FACTOR, new FloatDoublewordElement(12280))),
					
					new FC3ReadRegistersTask(12306, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_7_CURRENT, new FloatDoublewordElement(12306))),
					new FC3ReadRegistersTask(12366, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_7_ACTIVE_POWER, new FloatDoublewordElement(12366))),
					new FC3ReadRegistersTask(12374, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_7_REACTIVE_POWER, new FloatDoublewordElement(12374))),
					new FC3ReadRegistersTask(12382, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_7_APPARENT_POWER, new FloatDoublewordElement(12382))),
					new FC3ReadRegistersTask(12392, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_7_NET_KWH, new UnsignedDoublewordElement(12392))),
					new FC3ReadRegistersTask(12430, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_7_POWER_FACTOR, new FloatDoublewordElement(12430))),
					
					new FC3ReadRegistersTask(12456, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_8_CURRENT, new FloatDoublewordElement(12456))),
					new FC3ReadRegistersTask(12516, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_8_ACTIVE_POWER, new FloatDoublewordElement(12516))),
					new FC3ReadRegistersTask(12524, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_8_REACTIVE_POWER, new FloatDoublewordElement(12524))),
					new FC3ReadRegistersTask(12532, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_8_APPARENT_POWER, new FloatDoublewordElement(12532))),
					new FC3ReadRegistersTask(12542, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_8_NET_KWH, new UnsignedDoublewordElement(12542))),
					new FC3ReadRegistersTask(12580, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_8_POWER_FACTOR, new FloatDoublewordElement(12580))),
					
					new FC3ReadRegistersTask(12606, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_9_CURRENT, new FloatDoublewordElement(12606))),
					new FC3ReadRegistersTask(12666, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_9_ACTIVE_POWER, new FloatDoublewordElement(12666))),
					new FC3ReadRegistersTask(12674, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_9_REACTIVE_POWER, new FloatDoublewordElement(12674))),
					new FC3ReadRegistersTask(12682, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_9_APPARENT_POWER, new FloatDoublewordElement(12682))),
					new FC3ReadRegistersTask(12692, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_9_NET_KWH, new UnsignedDoublewordElement(12692))),
					new FC3ReadRegistersTask(12730, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_9_POWER_FACTOR, new FloatDoublewordElement(12730))),
					
					new FC3ReadRegistersTask(12756, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_10_CURRENT, new FloatDoublewordElement(12756))),
					new FC3ReadRegistersTask(12816, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_10_ACTIVE_POWER, new FloatDoublewordElement(12816))),
					new FC3ReadRegistersTask(12824, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_10_REACTIVE_POWER, new FloatDoublewordElement(12824))),
					new FC3ReadRegistersTask(12832, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_10_APPARENT_POWER, new FloatDoublewordElement(12832))),
					new FC3ReadRegistersTask(12842, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_10_NET_KWH, new UnsignedDoublewordElement(12842))),
					new FC3ReadRegistersTask(12880, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_10_POWER_FACTOR, new FloatDoublewordElement(12880))),
					
					new FC3ReadRegistersTask(12906, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_11_CURRENT, new FloatDoublewordElement(12906))),
					new FC3ReadRegistersTask(12966, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_11_ACTIVE_POWER, new FloatDoublewordElement(12966))),
					new FC3ReadRegistersTask(12974, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_11_REACTIVE_POWER, new FloatDoublewordElement(12974))),
					new FC3ReadRegistersTask(12982, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_11_APPARENT_POWER, new FloatDoublewordElement(12982))),
					new FC3ReadRegistersTask(12992, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_11_NET_KWH, new UnsignedDoublewordElement(12992))),
					new FC3ReadRegistersTask(13030, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_11_POWER_FACTOR, new FloatDoublewordElement(13030))),
					
					new FC3ReadRegistersTask(13056, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_12_CURRENT, new FloatDoublewordElement(13056))),
					new FC3ReadRegistersTask(13116, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_12_ACTIVE_POWER, new FloatDoublewordElement(13116))),
					new FC3ReadRegistersTask(13124, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_12_REACTIVE_POWER, new FloatDoublewordElement(13124))),
					new FC3ReadRegistersTask(13132, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_12_APPARENT_POWER, new FloatDoublewordElement(13132))),
					new FC3ReadRegistersTask(13142, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_12_NET_KWH, new UnsignedDoublewordElement(13142))),
					new FC3ReadRegistersTask(13180, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_12_POWER_FACTOR, new FloatDoublewordElement(13180))),
					
					new FC3ReadRegistersTask(13206, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_13_CURRENT, new FloatDoublewordElement(13206))),
					new FC3ReadRegistersTask(13266, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_13_ACTIVE_POWER, new FloatDoublewordElement(13266))),
					new FC3ReadRegistersTask(13274, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_13_REACTIVE_POWER, new FloatDoublewordElement(13274))),
					new FC3ReadRegistersTask(13282, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_13_APPARENT_POWER, new FloatDoublewordElement(13282))),
					new FC3ReadRegistersTask(13292, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_13_NET_KWH, new UnsignedDoublewordElement(13292))),
					new FC3ReadRegistersTask(13330, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_13_POWER_FACTOR, new FloatDoublewordElement(13330))),
					
					new FC3ReadRegistersTask(13356, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_14_CURRENT, new FloatDoublewordElement(13356))),
					new FC3ReadRegistersTask(13416, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_14_ACTIVE_POWER, new FloatDoublewordElement(13416))),
					new FC3ReadRegistersTask(13424, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_14_REACTIVE_POWER, new FloatDoublewordElement(13424))),
					new FC3ReadRegistersTask(13432, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_14_APPARENT_POWER, new FloatDoublewordElement(13432))),
					new FC3ReadRegistersTask(13442, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_14_NET_KWH, new UnsignedDoublewordElement(13442))),
					new FC3ReadRegistersTask(13480, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_14_POWER_FACTOR, new FloatDoublewordElement(13480))),
					
					new FC3ReadRegistersTask(13506, Priority.LOW,	
						m(Accura2300.ChannelId.ACCURA2350_15_CURRENT, new FloatDoublewordElement(13506))),
					new FC3ReadRegistersTask(13566, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_15_ACTIVE_POWER, new FloatDoublewordElement(13566))),
					new FC3ReadRegistersTask(13574, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_15_REACTIVE_POWER, new FloatDoublewordElement(13574))),
					new FC3ReadRegistersTask(13582, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_15_APPARENT_POWER, new FloatDoublewordElement(13582))),
					new FC3ReadRegistersTask(13592, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_15_NET_KWH, new UnsignedDoublewordElement(13592))),
					new FC3ReadRegistersTask(13630, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_15_POWER_FACTOR, new FloatDoublewordElement(13630))),
					
					new FC3ReadRegistersTask(13656, Priority.LOW,	
						m(Accura2300.ChannelId.ACCURA2350_16_CURRENT, new FloatDoublewordElement(13656))),
					new FC3ReadRegistersTask(13716, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_16_ACTIVE_POWER, new FloatDoublewordElement(13716))),
					new FC3ReadRegistersTask(13724, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_16_REACTIVE_POWER, new FloatDoublewordElement(13724))),
					new FC3ReadRegistersTask(13732, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_16_APPARENT_POWER, new FloatDoublewordElement(13732))),
					new FC3ReadRegistersTask(13742, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_16_NET_KWH, new UnsignedDoublewordElement(13742))),
					new FC3ReadRegistersTask(13780, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_16_POWER_FACTOR, new FloatDoublewordElement(13780))),
					
					new FC3ReadRegistersTask(13806, Priority.LOW,	
						m(Accura2300.ChannelId.ACCURA2350_17_CURRENT, new FloatDoublewordElement(13806))),
					new FC3ReadRegistersTask(13866, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_17_ACTIVE_POWER, new FloatDoublewordElement(13866))),
					new FC3ReadRegistersTask(13874, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_17_REACTIVE_POWER, new FloatDoublewordElement(13874))),
					new FC3ReadRegistersTask(13882, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_17_APPARENT_POWER, new FloatDoublewordElement(13882))),
					new FC3ReadRegistersTask(13892, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_17_NET_KWH, new UnsignedDoublewordElement(13892))),
					new FC3ReadRegistersTask(13930, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_17_POWER_FACTOR, new FloatDoublewordElement(13930))),
					
					new FC3ReadRegistersTask(13956, Priority.LOW,	
						m(Accura2300.ChannelId.ACCURA2350_18_CURRENT, new FloatDoublewordElement(13956))),
					new FC3ReadRegistersTask(14016, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_18_ACTIVE_POWER, new FloatDoublewordElement(14016))),
					new FC3ReadRegistersTask(14024, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_18_REACTIVE_POWER, new FloatDoublewordElement(14024))),
					new FC3ReadRegistersTask(14032, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_18_APPARENT_POWER, new FloatDoublewordElement(14032))),
					new FC3ReadRegistersTask(14042, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_18_NET_KWH, new UnsignedDoublewordElement(14042))),
					new FC3ReadRegistersTask(14080, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_18_POWER_FACTOR, new FloatDoublewordElement(14080)))
				);
		}else if(this.config.sensor_num() == 17) {
			mod = new ModbusProtocol(this,		
					new FC3ReadRegistersTask(861, Priority.LOW,	
							m(Accura2300.ChannelId.ACCURA2300_WIRING_MODE, new UnsignedWordElement(861))),
					new FC3ReadRegistersTask(1111, Priority.LOW,	
							m(Accura2300.ChannelId.ACCURA2300_CT_CONNECTED_NUM, new UnsignedWordElement(1111))),
					new FC3ReadRegistersTask(11044, Priority.LOW,	
							m(Accura2300.ChannelId.ACCURA2300_VALIDITY_VOLTAGE_DATA, new UnsignedWordElement(11044)),
							m(Accura2300.ChannelId.ACCURA2300_VALIDITY_CT_DATA, new UnsignedWordElement(11045)),
							m(Accura2300.ChannelId.ACCURA2300_VALIDITY_CT_DATA, new UnsignedWordElement(11046)),
							m(Accura2300.ChannelId.ACCURA2300_VALIDITY_CT_DATA, new UnsignedWordElement(11047)),
							m(Accura2300.ChannelId.ACCURA2300_VALIDITY_CT_DATA, new UnsignedWordElement(11048)),
							m(Accura2300.ChannelId.ACCURA2300_VALIDITY_CT_DATA, new UnsignedWordElement(11049)),
							m(Accura2300.ChannelId.ACCURA2300_VALIDITY_CT_DATA, new UnsignedWordElement(11050)),
							m(Accura2300.ChannelId.ACCURA2300_VALIDITY_CT_DATA, new UnsignedWordElement(11051)),
							m(Accura2300.ChannelId.ACCURA2300_VALIDITY_CT_DATA, new UnsignedWordElement(11052)),
							m(Accura2300.ChannelId.ACCURA2300_VALIDITY_CT_DATA, new UnsignedWordElement(11053)),
							m(Accura2300.ChannelId.ACCURA2300_VALIDITY_CT_DATA, new UnsignedWordElement(11054)),
							m(Accura2300.ChannelId.ACCURA2300_VALIDITY_CT_DATA, new UnsignedWordElement(11055)),
							m(Accura2300.ChannelId.ACCURA2300_VALIDITY_CT_DATA, new UnsignedWordElement(11056)),
							m(Accura2300.ChannelId.ACCURA2300_VALIDITY_CT_DATA, new UnsignedWordElement(11057)),
							m(Accura2300.ChannelId.ACCURA2300_VALIDITY_CT_DATA, new UnsignedWordElement(11058)),
							m(Accura2300.ChannelId.ACCURA2300_VALIDITY_CT_DATA, new UnsignedWordElement(11059)),
							m(Accura2300.ChannelId.ACCURA2300_VALIDITY_CT_DATA, new UnsignedWordElement(11060)),
							m(Accura2300.ChannelId.ACCURA2300_VALIDITY_CT_DATA, new UnsignedWordElement(11061))),							
					
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
					new FC3ReadRegistersTask(11282, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_1_APPARENT_POWER, new FloatDoublewordElement(11282))),
					new FC3ReadRegistersTask(11292, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_1_NET_KWH, new UnsignedDoublewordElement(11292))),
					new FC3ReadRegistersTask(11330, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_1_POWER_FACTOR, new FloatDoublewordElement(11330))),
					
					new FC3ReadRegistersTask(11356, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_CURRENT, new FloatDoublewordElement(11356))),
					new FC3ReadRegistersTask(11416, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_ACTIVE_POWER, new FloatDoublewordElement(11416))),
					new FC3ReadRegistersTask(11424, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_REACTIVE_POWER, new FloatDoublewordElement(11424))),
					new FC3ReadRegistersTask(11432, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_APPARENT_POWER, new FloatDoublewordElement(11432))),
					new FC3ReadRegistersTask(11442, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_NET_KWH, new UnsignedDoublewordElement(11442))),
					new FC3ReadRegistersTask(11480, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_POWER_FACTOR, new FloatDoublewordElement(11480))),
					
					new FC3ReadRegistersTask(11506, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_3_CURRENT, new FloatDoublewordElement(11506))),
					new FC3ReadRegistersTask(11566, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_3_ACTIVE_POWER, new FloatDoublewordElement(11566))),
					new FC3ReadRegistersTask(11574, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_3_REACTIVE_POWER, new FloatDoublewordElement(11574))),
					new FC3ReadRegistersTask(11582, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_3_APPARENT_POWER, new FloatDoublewordElement(11582))),
					new FC3ReadRegistersTask(11592, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_3_NET_KWH, new UnsignedDoublewordElement(11592))),
					new FC3ReadRegistersTask(11630, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_3_POWER_FACTOR, new FloatDoublewordElement(11630))),
					
					new FC3ReadRegistersTask(11657, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_4_CURRENT, new FloatDoublewordElement(11656))),
					new FC3ReadRegistersTask(11716, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_4_ACTIVE_POWER, new FloatDoublewordElement(11716))),
					new FC3ReadRegistersTask(11724, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_4_REACTIVE_POWER, new FloatDoublewordElement(11724))),
					new FC3ReadRegistersTask(11732, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_4_APPARENT_POWER, new FloatDoublewordElement(11732))),
					new FC3ReadRegistersTask(11742, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_4_NET_KWH, new UnsignedDoublewordElement(11742))),
					new FC3ReadRegistersTask(11780, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_4_POWER_FACTOR, new FloatDoublewordElement(11780))),
					
					new FC3ReadRegistersTask(11906, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_5_CURRENT, new FloatDoublewordElement(11906))),
					new FC3ReadRegistersTask(11966, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_5_ACTIVE_POWER, new FloatDoublewordElement(11966))),
					new FC3ReadRegistersTask(11974, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_5_REACTIVE_POWER, new FloatDoublewordElement(11974))),
					new FC3ReadRegistersTask(11982, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_5_APPARENT_POWER, new FloatDoublewordElement(11982))),
					new FC3ReadRegistersTask(11992, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_5_NET_KWH, new UnsignedDoublewordElement(11992))),
					new FC3ReadRegistersTask(12030, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_5_POWER_FACTOR, new FloatDoublewordElement(12030))),
					
					new FC3ReadRegistersTask(12156, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_6_CURRENT, new FloatDoublewordElement(12156))),
					new FC3ReadRegistersTask(12216, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_6_ACTIVE_POWER, new FloatDoublewordElement(12216))),
					new FC3ReadRegistersTask(12224, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_6_REACTIVE_POWER, new FloatDoublewordElement(12224))),
					new FC3ReadRegistersTask(12232, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_6_APPARENT_POWER, new FloatDoublewordElement(12232))),
					new FC3ReadRegistersTask(12242, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_6_NET_KWH, new UnsignedDoublewordElement(12242))),
					new FC3ReadRegistersTask(12280, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_6_POWER_FACTOR, new FloatDoublewordElement(12280))),
					
					new FC3ReadRegistersTask(12306, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_7_CURRENT, new FloatDoublewordElement(12306))),
					new FC3ReadRegistersTask(12366, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_7_ACTIVE_POWER, new FloatDoublewordElement(12366))),
					new FC3ReadRegistersTask(12374, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_7_REACTIVE_POWER, new FloatDoublewordElement(12374))),
					new FC3ReadRegistersTask(12382, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_7_APPARENT_POWER, new FloatDoublewordElement(12382))),
					new FC3ReadRegistersTask(12392, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_7_NET_KWH, new UnsignedDoublewordElement(12392))),
					new FC3ReadRegistersTask(12430, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_7_POWER_FACTOR, new FloatDoublewordElement(12430))),
					
					new FC3ReadRegistersTask(12456, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_8_CURRENT, new FloatDoublewordElement(12456))),
					new FC3ReadRegistersTask(12516, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_8_ACTIVE_POWER, new FloatDoublewordElement(12516))),
					new FC3ReadRegistersTask(12524, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_8_REACTIVE_POWER, new FloatDoublewordElement(12524))),
					new FC3ReadRegistersTask(12532, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_8_APPARENT_POWER, new FloatDoublewordElement(12532))),
					new FC3ReadRegistersTask(12542, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_8_NET_KWH, new UnsignedDoublewordElement(12542))),
					new FC3ReadRegistersTask(12580, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_8_POWER_FACTOR, new FloatDoublewordElement(12580))),
					
					new FC3ReadRegistersTask(12606, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_9_CURRENT, new FloatDoublewordElement(12606))),
					new FC3ReadRegistersTask(12666, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_9_ACTIVE_POWER, new FloatDoublewordElement(12666))),
					new FC3ReadRegistersTask(12674, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_9_REACTIVE_POWER, new FloatDoublewordElement(12674))),
					new FC3ReadRegistersTask(12682, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_9_APPARENT_POWER, new FloatDoublewordElement(12682))),
					new FC3ReadRegistersTask(12692, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_9_NET_KWH, new UnsignedDoublewordElement(12692))),
					new FC3ReadRegistersTask(12730, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_9_POWER_FACTOR, new FloatDoublewordElement(12730))),
					
					new FC3ReadRegistersTask(12756, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_10_CURRENT, new FloatDoublewordElement(12756))),
					new FC3ReadRegistersTask(12816, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_10_ACTIVE_POWER, new FloatDoublewordElement(12816))),
					new FC3ReadRegistersTask(12824, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_10_REACTIVE_POWER, new FloatDoublewordElement(12824))),
					new FC3ReadRegistersTask(12832, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_10_APPARENT_POWER, new FloatDoublewordElement(12832))),
					new FC3ReadRegistersTask(12842, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_10_NET_KWH, new UnsignedDoublewordElement(12842))),
					new FC3ReadRegistersTask(12880, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_10_POWER_FACTOR, new FloatDoublewordElement(12880))),
					
					new FC3ReadRegistersTask(12906, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_11_CURRENT, new FloatDoublewordElement(12906))),
					new FC3ReadRegistersTask(12966, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_11_ACTIVE_POWER, new FloatDoublewordElement(12966))),
					new FC3ReadRegistersTask(12974, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_11_REACTIVE_POWER, new FloatDoublewordElement(12974))),
					new FC3ReadRegistersTask(12982, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_11_APPARENT_POWER, new FloatDoublewordElement(12982))),
					new FC3ReadRegistersTask(12992, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_11_NET_KWH, new UnsignedDoublewordElement(12992))),
					new FC3ReadRegistersTask(13030, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_11_POWER_FACTOR, new FloatDoublewordElement(13030))),
					
					new FC3ReadRegistersTask(13056, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_12_CURRENT, new FloatDoublewordElement(13056))),
					new FC3ReadRegistersTask(13116, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_12_ACTIVE_POWER, new FloatDoublewordElement(13116))),
					new FC3ReadRegistersTask(13124, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_12_REACTIVE_POWER, new FloatDoublewordElement(13124))),
					new FC3ReadRegistersTask(13132, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_12_APPARENT_POWER, new FloatDoublewordElement(13132))),
					new FC3ReadRegistersTask(13142, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_12_NET_KWH, new UnsignedDoublewordElement(13142))),
					new FC3ReadRegistersTask(13180, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_12_POWER_FACTOR, new FloatDoublewordElement(13180))),
					
					new FC3ReadRegistersTask(13206, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_13_CURRENT, new FloatDoublewordElement(13206))),
					new FC3ReadRegistersTask(13266, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_13_ACTIVE_POWER, new FloatDoublewordElement(13266))),
					new FC3ReadRegistersTask(13274, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_13_REACTIVE_POWER, new FloatDoublewordElement(13274))),
					new FC3ReadRegistersTask(13282, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_13_APPARENT_POWER, new FloatDoublewordElement(13282))),
					new FC3ReadRegistersTask(13292, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_13_NET_KWH, new UnsignedDoublewordElement(13292))),
					new FC3ReadRegistersTask(13330, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_13_POWER_FACTOR, new FloatDoublewordElement(13330))),
					
					new FC3ReadRegistersTask(13356, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_14_CURRENT, new FloatDoublewordElement(13356))),
					new FC3ReadRegistersTask(13416, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_14_ACTIVE_POWER, new FloatDoublewordElement(13416))),
					new FC3ReadRegistersTask(13424, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_14_REACTIVE_POWER, new FloatDoublewordElement(13424))),
					new FC3ReadRegistersTask(13432, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_14_APPARENT_POWER, new FloatDoublewordElement(13432))),
					new FC3ReadRegistersTask(13442, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_14_NET_KWH, new UnsignedDoublewordElement(13442))),
					new FC3ReadRegistersTask(13480, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_14_POWER_FACTOR, new FloatDoublewordElement(13480))),
					
					new FC3ReadRegistersTask(13506, Priority.LOW,	
						m(Accura2300.ChannelId.ACCURA2350_15_CURRENT, new FloatDoublewordElement(13506))),
					new FC3ReadRegistersTask(13566, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_15_ACTIVE_POWER, new FloatDoublewordElement(13566))),
					new FC3ReadRegistersTask(13574, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_15_REACTIVE_POWER, new FloatDoublewordElement(13574))),
					new FC3ReadRegistersTask(13582, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_15_APPARENT_POWER, new FloatDoublewordElement(13582))),
					new FC3ReadRegistersTask(13592, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_15_NET_KWH, new UnsignedDoublewordElement(13592))),
					new FC3ReadRegistersTask(13630, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_15_POWER_FACTOR, new FloatDoublewordElement(13630))),
					
					new FC3ReadRegistersTask(13656, Priority.LOW,	
						m(Accura2300.ChannelId.ACCURA2350_16_CURRENT, new FloatDoublewordElement(13656))),
					new FC3ReadRegistersTask(13716, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_16_ACTIVE_POWER, new FloatDoublewordElement(13716))),
					new FC3ReadRegistersTask(13724, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_16_REACTIVE_POWER, new FloatDoublewordElement(13724))),
					new FC3ReadRegistersTask(13732, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_16_APPARENT_POWER, new FloatDoublewordElement(13732))),
					new FC3ReadRegistersTask(13742, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_16_NET_KWH, new UnsignedDoublewordElement(13742))),
					new FC3ReadRegistersTask(13780, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_16_POWER_FACTOR, new FloatDoublewordElement(13780))),
					
					new FC3ReadRegistersTask(13806, Priority.LOW,	
						m(Accura2300.ChannelId.ACCURA2350_17_CURRENT, new FloatDoublewordElement(13806))),
					new FC3ReadRegistersTask(13866, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_17_ACTIVE_POWER, new FloatDoublewordElement(13866))),
					new FC3ReadRegistersTask(13874, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_17_REACTIVE_POWER, new FloatDoublewordElement(13874))),
					new FC3ReadRegistersTask(13882, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_17_APPARENT_POWER, new FloatDoublewordElement(13882))),
					new FC3ReadRegistersTask(13892, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_17_NET_KWH, new UnsignedDoublewordElement(13892))),
					new FC3ReadRegistersTask(13930, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_17_POWER_FACTOR, new FloatDoublewordElement(13930)))
				);
		}else if(this.config.sensor_num() == 16) {
			mod = new ModbusProtocol(this,					
					new FC3ReadRegistersTask(861, Priority.LOW,	
							m(Accura2300.ChannelId.ACCURA2300_WIRING_MODE, new UnsignedWordElement(861))),
					new FC3ReadRegistersTask(1111, Priority.LOW,	
							m(Accura2300.ChannelId.ACCURA2300_CT_CONNECTED_NUM, new UnsignedWordElement(1111))),
					new FC3ReadRegistersTask(11044, Priority.LOW,	
							m(Accura2300.ChannelId.ACCURA2300_VALIDITY_VOLTAGE_DATA, new UnsignedWordElement(11044)),
							m(Accura2300.ChannelId.ACCURA2300_VALIDITY_CT_DATA, new UnsignedWordElement(11045)),
							m(Accura2300.ChannelId.ACCURA2300_VALIDITY_CT_DATA, new UnsignedWordElement(11046)),
							m(Accura2300.ChannelId.ACCURA2300_VALIDITY_CT_DATA, new UnsignedWordElement(11047)),
							m(Accura2300.ChannelId.ACCURA2300_VALIDITY_CT_DATA, new UnsignedWordElement(11048)),
							m(Accura2300.ChannelId.ACCURA2300_VALIDITY_CT_DATA, new UnsignedWordElement(11049)),
							m(Accura2300.ChannelId.ACCURA2300_VALIDITY_CT_DATA, new UnsignedWordElement(11050)),
							m(Accura2300.ChannelId.ACCURA2300_VALIDITY_CT_DATA, new UnsignedWordElement(11051)),
							m(Accura2300.ChannelId.ACCURA2300_VALIDITY_CT_DATA, new UnsignedWordElement(11052)),
							m(Accura2300.ChannelId.ACCURA2300_VALIDITY_CT_DATA, new UnsignedWordElement(11053)),
							m(Accura2300.ChannelId.ACCURA2300_VALIDITY_CT_DATA, new UnsignedWordElement(11054)),
							m(Accura2300.ChannelId.ACCURA2300_VALIDITY_CT_DATA, new UnsignedWordElement(11055)),
							m(Accura2300.ChannelId.ACCURA2300_VALIDITY_CT_DATA, new UnsignedWordElement(11056)),
							m(Accura2300.ChannelId.ACCURA2300_VALIDITY_CT_DATA, new UnsignedWordElement(11057)),
							m(Accura2300.ChannelId.ACCURA2300_VALIDITY_CT_DATA, new UnsignedWordElement(11058)),
							m(Accura2300.ChannelId.ACCURA2300_VALIDITY_CT_DATA, new UnsignedWordElement(11059)),
							m(Accura2300.ChannelId.ACCURA2300_VALIDITY_CT_DATA, new UnsignedWordElement(11060))),
					
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
					new FC3ReadRegistersTask(11282, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_1_APPARENT_POWER, new FloatDoublewordElement(11282))),
					new FC3ReadRegistersTask(11292, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_1_NET_KWH, new UnsignedDoublewordElement(11292))),
					new FC3ReadRegistersTask(11330, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_1_POWER_FACTOR, new FloatDoublewordElement(11330))),
					
					new FC3ReadRegistersTask(11356, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_CURRENT, new FloatDoublewordElement(11356))),
					new FC3ReadRegistersTask(11416, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_ACTIVE_POWER, new FloatDoublewordElement(11416))),
					new FC3ReadRegistersTask(11424, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_REACTIVE_POWER, new FloatDoublewordElement(11424))),
					new FC3ReadRegistersTask(11432, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_APPARENT_POWER, new FloatDoublewordElement(11432))),
					new FC3ReadRegistersTask(11442, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_NET_KWH, new UnsignedDoublewordElement(11442))),
					new FC3ReadRegistersTask(11480, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_POWER_FACTOR, new FloatDoublewordElement(11480))),
					
					new FC3ReadRegistersTask(11506, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_3_CURRENT, new FloatDoublewordElement(11506))),
					new FC3ReadRegistersTask(11566, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_3_ACTIVE_POWER, new FloatDoublewordElement(11566))),
					new FC3ReadRegistersTask(11574, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_3_REACTIVE_POWER, new FloatDoublewordElement(11574))),
					new FC3ReadRegistersTask(11582, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_3_APPARENT_POWER, new FloatDoublewordElement(11582))),
					new FC3ReadRegistersTask(11592, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_3_NET_KWH, new UnsignedDoublewordElement(11592))),
					new FC3ReadRegistersTask(11630, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_3_POWER_FACTOR, new FloatDoublewordElement(11630))),
					
					new FC3ReadRegistersTask(11657, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_4_CURRENT, new FloatDoublewordElement(11656))),
					new FC3ReadRegistersTask(11716, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_4_ACTIVE_POWER, new FloatDoublewordElement(11716))),
					new FC3ReadRegistersTask(11724, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_4_REACTIVE_POWER, new FloatDoublewordElement(11724))),
					new FC3ReadRegistersTask(11732, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_4_APPARENT_POWER, new FloatDoublewordElement(11732))),
					new FC3ReadRegistersTask(11742, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_4_NET_KWH, new UnsignedDoublewordElement(11742))),
					new FC3ReadRegistersTask(11780, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_4_POWER_FACTOR, new FloatDoublewordElement(11780))),
					
					new FC3ReadRegistersTask(11906, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_5_CURRENT, new FloatDoublewordElement(11906))),
					new FC3ReadRegistersTask(11966, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_5_ACTIVE_POWER, new FloatDoublewordElement(11966))),
					new FC3ReadRegistersTask(11974, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_5_REACTIVE_POWER, new FloatDoublewordElement(11974))),
					new FC3ReadRegistersTask(11982, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_5_APPARENT_POWER, new FloatDoublewordElement(11982))),
					new FC3ReadRegistersTask(11992, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_5_NET_KWH, new UnsignedDoublewordElement(11992))),
					new FC3ReadRegistersTask(12030, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_5_POWER_FACTOR, new FloatDoublewordElement(12030))),
					
					new FC3ReadRegistersTask(12156, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_6_CURRENT, new FloatDoublewordElement(12156))),
					new FC3ReadRegistersTask(12216, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_6_ACTIVE_POWER, new FloatDoublewordElement(12216))),
					new FC3ReadRegistersTask(12224, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_6_REACTIVE_POWER, new FloatDoublewordElement(12224))),
					new FC3ReadRegistersTask(12232, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_6_APPARENT_POWER, new FloatDoublewordElement(12232))),
					new FC3ReadRegistersTask(12242, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_6_NET_KWH, new UnsignedDoublewordElement(12242))),
					new FC3ReadRegistersTask(12280, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_6_POWER_FACTOR, new FloatDoublewordElement(12280))),
					
					new FC3ReadRegistersTask(12306, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_7_CURRENT, new FloatDoublewordElement(12306))),
					new FC3ReadRegistersTask(12366, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_7_ACTIVE_POWER, new FloatDoublewordElement(12366))),
					new FC3ReadRegistersTask(12374, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_7_REACTIVE_POWER, new FloatDoublewordElement(12374))),
					new FC3ReadRegistersTask(12382, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_7_APPARENT_POWER, new FloatDoublewordElement(12382))),
					new FC3ReadRegistersTask(12392, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_7_NET_KWH, new UnsignedDoublewordElement(12392))),
					new FC3ReadRegistersTask(12430, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_7_POWER_FACTOR, new FloatDoublewordElement(12430))),
					
					new FC3ReadRegistersTask(12456, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_8_CURRENT, new FloatDoublewordElement(12456))),
					new FC3ReadRegistersTask(12516, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_8_ACTIVE_POWER, new FloatDoublewordElement(12516))),
					new FC3ReadRegistersTask(12524, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_8_REACTIVE_POWER, new FloatDoublewordElement(12524))),
					new FC3ReadRegistersTask(12532, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_8_APPARENT_POWER, new FloatDoublewordElement(12532))),
					new FC3ReadRegistersTask(12542, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_8_NET_KWH, new UnsignedDoublewordElement(12542))),
					new FC3ReadRegistersTask(12580, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_8_POWER_FACTOR, new FloatDoublewordElement(12580))),
					
					new FC3ReadRegistersTask(12606, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_9_CURRENT, new FloatDoublewordElement(12606))),
					new FC3ReadRegistersTask(12666, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_9_ACTIVE_POWER, new FloatDoublewordElement(12666))),
					new FC3ReadRegistersTask(12674, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_9_REACTIVE_POWER, new FloatDoublewordElement(12674))),
					new FC3ReadRegistersTask(12682, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_9_APPARENT_POWER, new FloatDoublewordElement(12682))),
					new FC3ReadRegistersTask(12692, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_9_NET_KWH, new UnsignedDoublewordElement(12692))),
					new FC3ReadRegistersTask(12730, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_9_POWER_FACTOR, new FloatDoublewordElement(12730))),
					
					new FC3ReadRegistersTask(12756, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_10_CURRENT, new FloatDoublewordElement(12756))),
					new FC3ReadRegistersTask(12816, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_10_ACTIVE_POWER, new FloatDoublewordElement(12816))),
					new FC3ReadRegistersTask(12824, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_10_REACTIVE_POWER, new FloatDoublewordElement(12824))),
					new FC3ReadRegistersTask(12832, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_10_APPARENT_POWER, new FloatDoublewordElement(12832))),
					new FC3ReadRegistersTask(12842, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_10_NET_KWH, new UnsignedDoublewordElement(12842))),
					new FC3ReadRegistersTask(12880, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_10_POWER_FACTOR, new FloatDoublewordElement(12880))),
					
					new FC3ReadRegistersTask(12906, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_11_CURRENT, new FloatDoublewordElement(12906))),
					new FC3ReadRegistersTask(12966, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_11_ACTIVE_POWER, new FloatDoublewordElement(12966))),
					new FC3ReadRegistersTask(12974, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_11_REACTIVE_POWER, new FloatDoublewordElement(12974))),
					new FC3ReadRegistersTask(12982, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_11_APPARENT_POWER, new FloatDoublewordElement(12982))),
					new FC3ReadRegistersTask(12992, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_11_NET_KWH, new UnsignedDoublewordElement(12992))),
					new FC3ReadRegistersTask(13030, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_11_POWER_FACTOR, new FloatDoublewordElement(13030))),
					
					new FC3ReadRegistersTask(13056, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_12_CURRENT, new FloatDoublewordElement(13056))),
					new FC3ReadRegistersTask(13116, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_12_ACTIVE_POWER, new FloatDoublewordElement(13116))),
					new FC3ReadRegistersTask(13124, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_12_REACTIVE_POWER, new FloatDoublewordElement(13124))),
					new FC3ReadRegistersTask(13132, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_12_APPARENT_POWER, new FloatDoublewordElement(13132))),
					new FC3ReadRegistersTask(13142, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_12_NET_KWH, new UnsignedDoublewordElement(13142))),
					new FC3ReadRegistersTask(13180, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_12_POWER_FACTOR, new FloatDoublewordElement(13180))),
					
					new FC3ReadRegistersTask(13206, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_13_CURRENT, new FloatDoublewordElement(13206))),
					new FC3ReadRegistersTask(13266, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_13_ACTIVE_POWER, new FloatDoublewordElement(13266))),
					new FC3ReadRegistersTask(13274, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_13_REACTIVE_POWER, new FloatDoublewordElement(13274))),
					new FC3ReadRegistersTask(13282, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_13_APPARENT_POWER, new FloatDoublewordElement(13282))),
					new FC3ReadRegistersTask(13292, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_13_NET_KWH, new UnsignedDoublewordElement(13292))),
					new FC3ReadRegistersTask(13330, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_13_POWER_FACTOR, new FloatDoublewordElement(13330))),
					
					new FC3ReadRegistersTask(13356, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_14_CURRENT, new FloatDoublewordElement(13356))),
					new FC3ReadRegistersTask(13416, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_14_ACTIVE_POWER, new FloatDoublewordElement(13416))),
					new FC3ReadRegistersTask(13424, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_14_REACTIVE_POWER, new FloatDoublewordElement(13424))),
					new FC3ReadRegistersTask(13432, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_14_APPARENT_POWER, new FloatDoublewordElement(13432))),
					new FC3ReadRegistersTask(13442, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_14_NET_KWH, new UnsignedDoublewordElement(13442))),
					new FC3ReadRegistersTask(13480, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_14_POWER_FACTOR, new FloatDoublewordElement(13480))),
					
					new FC3ReadRegistersTask(13506, Priority.LOW,	
						m(Accura2300.ChannelId.ACCURA2350_15_CURRENT, new FloatDoublewordElement(13506))),
					new FC3ReadRegistersTask(13566, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_15_ACTIVE_POWER, new FloatDoublewordElement(13566))),
					new FC3ReadRegistersTask(13574, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_15_REACTIVE_POWER, new FloatDoublewordElement(13574))),
					new FC3ReadRegistersTask(13582, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_15_APPARENT_POWER, new FloatDoublewordElement(13582))),
					new FC3ReadRegistersTask(13592, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_15_NET_KWH, new UnsignedDoublewordElement(13592))),
					new FC3ReadRegistersTask(13630, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_15_POWER_FACTOR, new FloatDoublewordElement(13630))),
					
					new FC3ReadRegistersTask(13656, Priority.LOW,	
						m(Accura2300.ChannelId.ACCURA2350_16_CURRENT, new FloatDoublewordElement(13656))),
					new FC3ReadRegistersTask(13716, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_16_ACTIVE_POWER, new FloatDoublewordElement(13716))),
					new FC3ReadRegistersTask(13724, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_16_REACTIVE_POWER, new FloatDoublewordElement(13724))),
					new FC3ReadRegistersTask(13732, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_16_APPARENT_POWER, new FloatDoublewordElement(13732))),
					new FC3ReadRegistersTask(13742, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_16_NET_KWH, new UnsignedDoublewordElement(13742))),
					new FC3ReadRegistersTask(13780, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_16_POWER_FACTOR, new FloatDoublewordElement(13780)))
				);
		}else if(this.config.sensor_num() == 15) {
			mod = new ModbusProtocol(this,		
					new FC3ReadRegistersTask(861, Priority.LOW,	
							m(Accura2300.ChannelId.ACCURA2300_WIRING_MODE, new UnsignedWordElement(861))),
					new FC3ReadRegistersTask(1111, Priority.LOW,	
							m(Accura2300.ChannelId.ACCURA2300_CT_CONNECTED_NUM, new UnsignedWordElement(1111))),
					new FC3ReadRegistersTask(11044, Priority.LOW,	
							m(Accura2300.ChannelId.ACCURA2300_VALIDITY_VOLTAGE_DATA, new UnsignedWordElement(11044)),
							m(Accura2300.ChannelId.ACCURA2300_VALIDITY_CT_DATA, new UnsignedWordElement(11045)),
							m(Accura2300.ChannelId.ACCURA2300_VALIDITY_CT_DATA, new UnsignedWordElement(11046)),
							m(Accura2300.ChannelId.ACCURA2300_VALIDITY_CT_DATA, new UnsignedWordElement(11047)),
							m(Accura2300.ChannelId.ACCURA2300_VALIDITY_CT_DATA, new UnsignedWordElement(11048)),
							m(Accura2300.ChannelId.ACCURA2300_VALIDITY_CT_DATA, new UnsignedWordElement(11049)),
							m(Accura2300.ChannelId.ACCURA2300_VALIDITY_CT_DATA, new UnsignedWordElement(11050)),
							m(Accura2300.ChannelId.ACCURA2300_VALIDITY_CT_DATA, new UnsignedWordElement(11051)),
							m(Accura2300.ChannelId.ACCURA2300_VALIDITY_CT_DATA, new UnsignedWordElement(11052)),
							m(Accura2300.ChannelId.ACCURA2300_VALIDITY_CT_DATA, new UnsignedWordElement(11053)),
							m(Accura2300.ChannelId.ACCURA2300_VALIDITY_CT_DATA, new UnsignedWordElement(11054)),
							m(Accura2300.ChannelId.ACCURA2300_VALIDITY_CT_DATA, new UnsignedWordElement(11055)),
							m(Accura2300.ChannelId.ACCURA2300_VALIDITY_CT_DATA, new UnsignedWordElement(11056)),
							m(Accura2300.ChannelId.ACCURA2300_VALIDITY_CT_DATA, new UnsignedWordElement(11057)),
							m(Accura2300.ChannelId.ACCURA2300_VALIDITY_CT_DATA, new UnsignedWordElement(11058)),
							m(Accura2300.ChannelId.ACCURA2300_VALIDITY_CT_DATA, new UnsignedWordElement(11059))),							
					
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
					new FC3ReadRegistersTask(11282, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_1_APPARENT_POWER, new FloatDoublewordElement(11282))),
					new FC3ReadRegistersTask(11292, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_1_NET_KWH, new UnsignedDoublewordElement(11292))),
					new FC3ReadRegistersTask(11330, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_1_POWER_FACTOR, new FloatDoublewordElement(11330))),
					
					new FC3ReadRegistersTask(11356, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_CURRENT, new FloatDoublewordElement(11356))),
					new FC3ReadRegistersTask(11416, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_ACTIVE_POWER, new FloatDoublewordElement(11416))),
					new FC3ReadRegistersTask(11424, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_REACTIVE_POWER, new FloatDoublewordElement(11424))),
					new FC3ReadRegistersTask(11432, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_APPARENT_POWER, new FloatDoublewordElement(11432))),
					new FC3ReadRegistersTask(11442, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_NET_KWH, new UnsignedDoublewordElement(11442))),
					new FC3ReadRegistersTask(11480, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_POWER_FACTOR, new FloatDoublewordElement(11480))),
					
					new FC3ReadRegistersTask(11506, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_3_CURRENT, new FloatDoublewordElement(11506))),
					new FC3ReadRegistersTask(11566, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_3_ACTIVE_POWER, new FloatDoublewordElement(11566))),
					new FC3ReadRegistersTask(11574, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_3_REACTIVE_POWER, new FloatDoublewordElement(11574))),
					new FC3ReadRegistersTask(11582, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_3_APPARENT_POWER, new FloatDoublewordElement(11582))),
					new FC3ReadRegistersTask(11592, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_3_NET_KWH, new UnsignedDoublewordElement(11592))),
					new FC3ReadRegistersTask(11630, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_3_POWER_FACTOR, new FloatDoublewordElement(11630))),
					
					new FC3ReadRegistersTask(11657, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_4_CURRENT, new FloatDoublewordElement(11656))),
					new FC3ReadRegistersTask(11716, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_4_ACTIVE_POWER, new FloatDoublewordElement(11716))),
					new FC3ReadRegistersTask(11724, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_4_REACTIVE_POWER, new FloatDoublewordElement(11724))),
					new FC3ReadRegistersTask(11732, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_4_APPARENT_POWER, new FloatDoublewordElement(11732))),
					new FC3ReadRegistersTask(11742, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_4_NET_KWH, new UnsignedDoublewordElement(11742))),
					new FC3ReadRegistersTask(11780, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_4_POWER_FACTOR, new FloatDoublewordElement(11780))),
					
					new FC3ReadRegistersTask(11906, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_5_CURRENT, new FloatDoublewordElement(11906))),
					new FC3ReadRegistersTask(11966, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_5_ACTIVE_POWER, new FloatDoublewordElement(11966))),
					new FC3ReadRegistersTask(11974, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_5_REACTIVE_POWER, new FloatDoublewordElement(11974))),
					new FC3ReadRegistersTask(11982, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_5_APPARENT_POWER, new FloatDoublewordElement(11982))),
					new FC3ReadRegistersTask(11992, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_5_NET_KWH, new UnsignedDoublewordElement(11992))),
					new FC3ReadRegistersTask(12030, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_5_POWER_FACTOR, new FloatDoublewordElement(12030))),
					
					new FC3ReadRegistersTask(12156, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_6_CURRENT, new FloatDoublewordElement(12156))),
					new FC3ReadRegistersTask(12216, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_6_ACTIVE_POWER, new FloatDoublewordElement(12216))),
					new FC3ReadRegistersTask(12224, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_6_REACTIVE_POWER, new FloatDoublewordElement(12224))),
					new FC3ReadRegistersTask(12232, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_6_APPARENT_POWER, new FloatDoublewordElement(12232))),
					new FC3ReadRegistersTask(12242, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_6_NET_KWH, new UnsignedDoublewordElement(12242))),
					new FC3ReadRegistersTask(12280, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_6_POWER_FACTOR, new FloatDoublewordElement(12280))),
					
					new FC3ReadRegistersTask(12306, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_7_CURRENT, new FloatDoublewordElement(12306))),
					new FC3ReadRegistersTask(12366, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_7_ACTIVE_POWER, new FloatDoublewordElement(12366))),
					new FC3ReadRegistersTask(12374, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_7_REACTIVE_POWER, new FloatDoublewordElement(12374))),
					new FC3ReadRegistersTask(12382, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_7_APPARENT_POWER, new FloatDoublewordElement(12382))),
					new FC3ReadRegistersTask(12392, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_7_NET_KWH, new UnsignedDoublewordElement(12392))),
					new FC3ReadRegistersTask(12430, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_7_POWER_FACTOR, new FloatDoublewordElement(12430))),
					
					new FC3ReadRegistersTask(12456, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_8_CURRENT, new FloatDoublewordElement(12456))),
					new FC3ReadRegistersTask(12516, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_8_ACTIVE_POWER, new FloatDoublewordElement(12516))),
					new FC3ReadRegistersTask(12524, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_8_REACTIVE_POWER, new FloatDoublewordElement(12524))),
					new FC3ReadRegistersTask(12532, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_8_APPARENT_POWER, new FloatDoublewordElement(12532))),
					new FC3ReadRegistersTask(12542, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_8_NET_KWH, new UnsignedDoublewordElement(12542))),
					new FC3ReadRegistersTask(12580, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_8_POWER_FACTOR, new FloatDoublewordElement(12580))),
					
					new FC3ReadRegistersTask(12606, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_9_CURRENT, new FloatDoublewordElement(12606))),
					new FC3ReadRegistersTask(12666, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_9_ACTIVE_POWER, new FloatDoublewordElement(12666))),
					new FC3ReadRegistersTask(12674, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_9_REACTIVE_POWER, new FloatDoublewordElement(12674))),
					new FC3ReadRegistersTask(12682, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_9_APPARENT_POWER, new FloatDoublewordElement(12682))),
					new FC3ReadRegistersTask(12692, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_9_NET_KWH, new UnsignedDoublewordElement(12692))),
					new FC3ReadRegistersTask(12730, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_9_POWER_FACTOR, new FloatDoublewordElement(12730))),
					
					new FC3ReadRegistersTask(12756, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_10_CURRENT, new FloatDoublewordElement(12756))),
					new FC3ReadRegistersTask(12816, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_10_ACTIVE_POWER, new FloatDoublewordElement(12816))),
					new FC3ReadRegistersTask(12824, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_10_REACTIVE_POWER, new FloatDoublewordElement(12824))),
					new FC3ReadRegistersTask(12832, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_10_APPARENT_POWER, new FloatDoublewordElement(12832))),
					new FC3ReadRegistersTask(12842, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_10_NET_KWH, new UnsignedDoublewordElement(12842))),
					new FC3ReadRegistersTask(12880, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_10_POWER_FACTOR, new FloatDoublewordElement(12880))),
					
					new FC3ReadRegistersTask(12906, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_11_CURRENT, new FloatDoublewordElement(12906))),
					new FC3ReadRegistersTask(12966, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_11_ACTIVE_POWER, new FloatDoublewordElement(12966))),
					new FC3ReadRegistersTask(12974, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_11_REACTIVE_POWER, new FloatDoublewordElement(12974))),
					new FC3ReadRegistersTask(12982, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_11_APPARENT_POWER, new FloatDoublewordElement(12982))),
					new FC3ReadRegistersTask(12992, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_11_NET_KWH, new UnsignedDoublewordElement(12992))),
					new FC3ReadRegistersTask(13030, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_11_POWER_FACTOR, new FloatDoublewordElement(13030))),
					
					new FC3ReadRegistersTask(13056, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_12_CURRENT, new FloatDoublewordElement(13056))),
					new FC3ReadRegistersTask(13116, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_12_ACTIVE_POWER, new FloatDoublewordElement(13116))),
					new FC3ReadRegistersTask(13124, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_12_REACTIVE_POWER, new FloatDoublewordElement(13124))),
					new FC3ReadRegistersTask(13132, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_12_APPARENT_POWER, new FloatDoublewordElement(13132))),
					new FC3ReadRegistersTask(13142, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_12_NET_KWH, new UnsignedDoublewordElement(13132))),
					new FC3ReadRegistersTask(13180, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_12_POWER_FACTOR, new FloatDoublewordElement(13180))),
					
					new FC3ReadRegistersTask(13206, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_13_CURRENT, new FloatDoublewordElement(13206))),
					new FC3ReadRegistersTask(13266, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_13_ACTIVE_POWER, new FloatDoublewordElement(13266))),
					new FC3ReadRegistersTask(13274, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_13_REACTIVE_POWER, new FloatDoublewordElement(13274))),
					new FC3ReadRegistersTask(13282, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_13_APPARENT_POWER, new FloatDoublewordElement(13282))),
					new FC3ReadRegistersTask(13292, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_13_NET_KWH, new UnsignedDoublewordElement(13292))),
					new FC3ReadRegistersTask(13330, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_13_POWER_FACTOR, new FloatDoublewordElement(13330))),
					
					new FC3ReadRegistersTask(13356, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_14_CURRENT, new FloatDoublewordElement(13356))),
					new FC3ReadRegistersTask(13416, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_14_ACTIVE_POWER, new FloatDoublewordElement(13416))),
					new FC3ReadRegistersTask(13424, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_14_REACTIVE_POWER, new FloatDoublewordElement(13424))),
					new FC3ReadRegistersTask(13432, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_14_APPARENT_POWER, new FloatDoublewordElement(13432))),
					new FC3ReadRegistersTask(13442, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_14_NET_KWH, new UnsignedDoublewordElement(13442))),
					new FC3ReadRegistersTask(13480, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_14_POWER_FACTOR, new FloatDoublewordElement(13480))),
					
					new FC3ReadRegistersTask(13506, Priority.LOW,	
						m(Accura2300.ChannelId.ACCURA2350_15_CURRENT, new FloatDoublewordElement(13506))),
					new FC3ReadRegistersTask(13566, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_15_ACTIVE_POWER, new FloatDoublewordElement(13566))),
					new FC3ReadRegistersTask(13574, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_15_REACTIVE_POWER, new FloatDoublewordElement(13574))),
					new FC3ReadRegistersTask(13582, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_15_APPARENT_POWER, new FloatDoublewordElement(13582))),
					new FC3ReadRegistersTask(13592, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_15_NET_KWH, new UnsignedDoublewordElement(13592))),
					new FC3ReadRegistersTask(13630, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_15_POWER_FACTOR, new FloatDoublewordElement(13630)))
				);
		}else if(this.config.sensor_num() == 14) {
			mod = new ModbusProtocol(this,					
					new FC3ReadRegistersTask(861, Priority.LOW,	
							m(Accura2300.ChannelId.ACCURA2300_WIRING_MODE, new UnsignedWordElement(861))),
					new FC3ReadRegistersTask(1111, Priority.LOW,	
							m(Accura2300.ChannelId.ACCURA2300_CT_CONNECTED_NUM, new UnsignedWordElement(1111))),
					new FC3ReadRegistersTask(11044, Priority.LOW,	
							m(Accura2300.ChannelId.ACCURA2300_VALIDITY_VOLTAGE_DATA, new UnsignedWordElement(11044)),
							m(Accura2300.ChannelId.ACCURA2300_VALIDITY_CT_DATA, new UnsignedWordElement(11045)),
							m(Accura2300.ChannelId.ACCURA2300_VALIDITY_CT_DATA, new UnsignedWordElement(11046)),
							m(Accura2300.ChannelId.ACCURA2300_VALIDITY_CT_DATA, new UnsignedWordElement(11047)),
							m(Accura2300.ChannelId.ACCURA2300_VALIDITY_CT_DATA, new UnsignedWordElement(11048)),
							m(Accura2300.ChannelId.ACCURA2300_VALIDITY_CT_DATA, new UnsignedWordElement(11049)),
							m(Accura2300.ChannelId.ACCURA2300_VALIDITY_CT_DATA, new UnsignedWordElement(11050)),
							m(Accura2300.ChannelId.ACCURA2300_VALIDITY_CT_DATA, new UnsignedWordElement(11051)),
							m(Accura2300.ChannelId.ACCURA2300_VALIDITY_CT_DATA, new UnsignedWordElement(11052)),
							m(Accura2300.ChannelId.ACCURA2300_VALIDITY_CT_DATA, new UnsignedWordElement(11053)),
							m(Accura2300.ChannelId.ACCURA2300_VALIDITY_CT_DATA, new UnsignedWordElement(11054)),
							m(Accura2300.ChannelId.ACCURA2300_VALIDITY_CT_DATA, new UnsignedWordElement(11055)),
							m(Accura2300.ChannelId.ACCURA2300_VALIDITY_CT_DATA, new UnsignedWordElement(11056)),
							m(Accura2300.ChannelId.ACCURA2300_VALIDITY_CT_DATA, new UnsignedWordElement(11057)),
							m(Accura2300.ChannelId.ACCURA2300_VALIDITY_CT_DATA, new UnsignedWordElement(11058))),
					
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
					new FC3ReadRegistersTask(11282, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_1_APPARENT_POWER, new FloatDoublewordElement(11282))),
					new FC3ReadRegistersTask(11292, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_1_NET_KWH, new UnsignedDoublewordElement(11292))),
					new FC3ReadRegistersTask(11330, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_1_POWER_FACTOR, new FloatDoublewordElement(11330))),
					
					new FC3ReadRegistersTask(11356, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_CURRENT, new FloatDoublewordElement(11356))),
					new FC3ReadRegistersTask(11416, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_ACTIVE_POWER, new FloatDoublewordElement(11416))),
					new FC3ReadRegistersTask(11424, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_REACTIVE_POWER, new FloatDoublewordElement(11424))),
					new FC3ReadRegistersTask(11432, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_APPARENT_POWER, new FloatDoublewordElement(11432))),
					new FC3ReadRegistersTask(11442, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_NET_KWH, new UnsignedDoublewordElement(11442))),
					new FC3ReadRegistersTask(11480, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_POWER_FACTOR, new FloatDoublewordElement(11480))),
					
					new FC3ReadRegistersTask(11506, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_3_CURRENT, new FloatDoublewordElement(11506))),
					new FC3ReadRegistersTask(11566, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_3_ACTIVE_POWER, new FloatDoublewordElement(11566))),
					new FC3ReadRegistersTask(11574, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_3_REACTIVE_POWER, new FloatDoublewordElement(11574))),
					new FC3ReadRegistersTask(11582, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_3_APPARENT_POWER, new FloatDoublewordElement(11582))),
					new FC3ReadRegistersTask(11592, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_3_NET_KWH, new UnsignedDoublewordElement(11592))),
					new FC3ReadRegistersTask(11630, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_3_POWER_FACTOR, new FloatDoublewordElement(11630))),
					
					new FC3ReadRegistersTask(11657, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_4_CURRENT, new FloatDoublewordElement(11656))),
					new FC3ReadRegistersTask(11716, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_4_ACTIVE_POWER, new FloatDoublewordElement(11716))),
					new FC3ReadRegistersTask(11724, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_4_REACTIVE_POWER, new FloatDoublewordElement(11724))),
					new FC3ReadRegistersTask(11732, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_4_APPARENT_POWER, new FloatDoublewordElement(11732))),
					new FC3ReadRegistersTask(11742, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_4_NET_KWH, new UnsignedDoublewordElement(11742))),
					new FC3ReadRegistersTask(11780, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_4_POWER_FACTOR, new FloatDoublewordElement(11780))),
					
					new FC3ReadRegistersTask(11906, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_5_CURRENT, new FloatDoublewordElement(11906))),
					new FC3ReadRegistersTask(11966, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_5_ACTIVE_POWER, new FloatDoublewordElement(11966))),
					new FC3ReadRegistersTask(11974, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_5_REACTIVE_POWER, new FloatDoublewordElement(11974))),
					new FC3ReadRegistersTask(11982, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_5_APPARENT_POWER, new FloatDoublewordElement(11982))),
					new FC3ReadRegistersTask(11992, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_5_NET_KWH, new UnsignedDoublewordElement(11992))),
					new FC3ReadRegistersTask(12030, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_5_POWER_FACTOR, new FloatDoublewordElement(12030))),
					
					new FC3ReadRegistersTask(12156, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_6_CURRENT, new FloatDoublewordElement(12156))),
					new FC3ReadRegistersTask(12216, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_6_ACTIVE_POWER, new FloatDoublewordElement(12216))),
					new FC3ReadRegistersTask(12224, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_6_REACTIVE_POWER, new FloatDoublewordElement(12224))),
					new FC3ReadRegistersTask(12232, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_6_APPARENT_POWER, new FloatDoublewordElement(12232))),
					new FC3ReadRegistersTask(12242, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_6_NET_KWH, new UnsignedDoublewordElement(12242))),
					new FC3ReadRegistersTask(12280, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_6_POWER_FACTOR, new FloatDoublewordElement(12280))),
					
					new FC3ReadRegistersTask(12306, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_7_CURRENT, new FloatDoublewordElement(12306))),
					new FC3ReadRegistersTask(12366, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_7_ACTIVE_POWER, new FloatDoublewordElement(12366))),
					new FC3ReadRegistersTask(12374, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_7_REACTIVE_POWER, new FloatDoublewordElement(12374))),
					new FC3ReadRegistersTask(12382, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_7_APPARENT_POWER, new FloatDoublewordElement(12382))),
					new FC3ReadRegistersTask(12392, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_7_NET_KWH, new UnsignedDoublewordElement(12392))),
					new FC3ReadRegistersTask(12430, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_7_POWER_FACTOR, new FloatDoublewordElement(12430))),
					
					new FC3ReadRegistersTask(12456, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_8_CURRENT, new FloatDoublewordElement(12456))),
					new FC3ReadRegistersTask(12516, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_8_ACTIVE_POWER, new FloatDoublewordElement(12516))),
					new FC3ReadRegistersTask(12524, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_8_REACTIVE_POWER, new FloatDoublewordElement(12524))),
					new FC3ReadRegistersTask(12532, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_8_APPARENT_POWER, new FloatDoublewordElement(12532))),
					new FC3ReadRegistersTask(12542, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_8_NET_KWH, new UnsignedDoublewordElement(12542))),
					new FC3ReadRegistersTask(12580, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_8_POWER_FACTOR, new FloatDoublewordElement(12580))),
					
					new FC3ReadRegistersTask(12606, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_9_CURRENT, new FloatDoublewordElement(12606))),
					new FC3ReadRegistersTask(12666, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_9_ACTIVE_POWER, new FloatDoublewordElement(12666))),
					new FC3ReadRegistersTask(12674, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_9_REACTIVE_POWER, new FloatDoublewordElement(12674))),
					new FC3ReadRegistersTask(12682, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_9_APPARENT_POWER, new FloatDoublewordElement(12682))),
					new FC3ReadRegistersTask(12692, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_9_NET_KWH, new UnsignedDoublewordElement(12692))),
					new FC3ReadRegistersTask(12730, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_9_POWER_FACTOR, new FloatDoublewordElement(12730))),
					
					new FC3ReadRegistersTask(12756, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_10_CURRENT, new FloatDoublewordElement(12756))),
					new FC3ReadRegistersTask(12816, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_10_ACTIVE_POWER, new FloatDoublewordElement(12816))),
					new FC3ReadRegistersTask(12824, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_10_REACTIVE_POWER, new FloatDoublewordElement(12824))),
					new FC3ReadRegistersTask(12832, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_10_APPARENT_POWER, new FloatDoublewordElement(12832))),
					new FC3ReadRegistersTask(12842, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_10_NET_KWH, new UnsignedDoublewordElement(12842))),
					new FC3ReadRegistersTask(12880, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_10_POWER_FACTOR, new FloatDoublewordElement(12880))),
					
					new FC3ReadRegistersTask(12906, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_11_CURRENT, new FloatDoublewordElement(12906))),
					new FC3ReadRegistersTask(12966, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_11_ACTIVE_POWER, new FloatDoublewordElement(12966))),
					new FC3ReadRegistersTask(12974, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_11_REACTIVE_POWER, new FloatDoublewordElement(12974))),
					new FC3ReadRegistersTask(12982, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_11_APPARENT_POWER, new FloatDoublewordElement(12982))),
					new FC3ReadRegistersTask(12992, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_11_NET_KWH, new UnsignedDoublewordElement(12992))),
					new FC3ReadRegistersTask(13030, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_11_POWER_FACTOR, new FloatDoublewordElement(13030))),
					
					new FC3ReadRegistersTask(13056, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_12_CURRENT, new FloatDoublewordElement(13056))),
					new FC3ReadRegistersTask(13116, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_12_ACTIVE_POWER, new FloatDoublewordElement(13116))),
					new FC3ReadRegistersTask(13124, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_12_REACTIVE_POWER, new FloatDoublewordElement(13124))),
					new FC3ReadRegistersTask(13132, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_12_APPARENT_POWER, new FloatDoublewordElement(13132))),
					new FC3ReadRegistersTask(13142, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_12_NET_KWH, new UnsignedDoublewordElement(13142))),
					new FC3ReadRegistersTask(13180, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_12_POWER_FACTOR, new FloatDoublewordElement(13180))),
					
					new FC3ReadRegistersTask(13206, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_13_CURRENT, new FloatDoublewordElement(13206))),
					new FC3ReadRegistersTask(13266, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_13_ACTIVE_POWER, new FloatDoublewordElement(13266))),
					new FC3ReadRegistersTask(13274, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_13_REACTIVE_POWER, new FloatDoublewordElement(13274))),
					new FC3ReadRegistersTask(13282, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_13_APPARENT_POWER, new FloatDoublewordElement(13282))),
					new FC3ReadRegistersTask(13292, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_13_NET_KWH, new UnsignedDoublewordElement(13292))),
					new FC3ReadRegistersTask(13330, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_13_POWER_FACTOR, new FloatDoublewordElement(13330))),
					
					new FC3ReadRegistersTask(13356, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_14_CURRENT, new FloatDoublewordElement(13356))),
					new FC3ReadRegistersTask(13416, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_14_ACTIVE_POWER, new FloatDoublewordElement(13416))),
					new FC3ReadRegistersTask(13424, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_14_REACTIVE_POWER, new FloatDoublewordElement(13424))),
					new FC3ReadRegistersTask(13432, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_14_APPARENT_POWER, new FloatDoublewordElement(13432))),
					new FC3ReadRegistersTask(13442, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_14_NET_KWH, new UnsignedDoublewordElement(13442))),
					new FC3ReadRegistersTask(13480, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_14_POWER_FACTOR, new FloatDoublewordElement(13480)))
				);
		}else if(this.config.sensor_num() == 13) {
			mod = new ModbusProtocol(this,					
					new FC3ReadRegistersTask(861, Priority.LOW,	
							m(Accura2300.ChannelId.ACCURA2300_WIRING_MODE, new UnsignedWordElement(861))),
					new FC3ReadRegistersTask(1111, Priority.LOW,	
							m(Accura2300.ChannelId.ACCURA2300_CT_CONNECTED_NUM, new UnsignedWordElement(1111))),
					new FC3ReadRegistersTask(11044, Priority.LOW,	
							m(Accura2300.ChannelId.ACCURA2300_VALIDITY_VOLTAGE_DATA, new UnsignedWordElement(11044)),
							m(Accura2300.ChannelId.ACCURA2300_VALIDITY_CT_DATA, new UnsignedWordElement(11045)),
							m(Accura2300.ChannelId.ACCURA2300_VALIDITY_CT_DATA, new UnsignedWordElement(11046)),
							m(Accura2300.ChannelId.ACCURA2300_VALIDITY_CT_DATA, new UnsignedWordElement(11047)),
							m(Accura2300.ChannelId.ACCURA2300_VALIDITY_CT_DATA, new UnsignedWordElement(11048)),
							m(Accura2300.ChannelId.ACCURA2300_VALIDITY_CT_DATA, new UnsignedWordElement(11049)),
							m(Accura2300.ChannelId.ACCURA2300_VALIDITY_CT_DATA, new UnsignedWordElement(11050)),
							m(Accura2300.ChannelId.ACCURA2300_VALIDITY_CT_DATA, new UnsignedWordElement(11051)),
							m(Accura2300.ChannelId.ACCURA2300_VALIDITY_CT_DATA, new UnsignedWordElement(11052)),
							m(Accura2300.ChannelId.ACCURA2300_VALIDITY_CT_DATA, new UnsignedWordElement(11053)),
							m(Accura2300.ChannelId.ACCURA2300_VALIDITY_CT_DATA, new UnsignedWordElement(11054)),
							m(Accura2300.ChannelId.ACCURA2300_VALIDITY_CT_DATA, new UnsignedWordElement(11055)),
							m(Accura2300.ChannelId.ACCURA2300_VALIDITY_CT_DATA, new UnsignedWordElement(11056)),
							m(Accura2300.ChannelId.ACCURA2300_VALIDITY_CT_DATA, new UnsignedWordElement(11057))),
					
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
					new FC3ReadRegistersTask(11282, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_1_APPARENT_POWER, new FloatDoublewordElement(11282))),
					new FC3ReadRegistersTask(11292, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_1_NET_KWH, new UnsignedDoublewordElement(11292))),
					new FC3ReadRegistersTask(11330, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_1_POWER_FACTOR, new FloatDoublewordElement(11330))),
					
					new FC3ReadRegistersTask(11356, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_CURRENT, new FloatDoublewordElement(11356))),
					new FC3ReadRegistersTask(11416, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_ACTIVE_POWER, new FloatDoublewordElement(11416))),
					new FC3ReadRegistersTask(11424, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_REACTIVE_POWER, new FloatDoublewordElement(11424))),
					new FC3ReadRegistersTask(11432, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_APPARENT_POWER, new FloatDoublewordElement(11432))),
					new FC3ReadRegistersTask(11442, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_NET_KWH, new UnsignedDoublewordElement(11442))),
					new FC3ReadRegistersTask(11480, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_POWER_FACTOR, new FloatDoublewordElement(11480))),
					
					new FC3ReadRegistersTask(11506, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_3_CURRENT, new FloatDoublewordElement(11506))),
					new FC3ReadRegistersTask(11566, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_3_ACTIVE_POWER, new FloatDoublewordElement(11566))),
					new FC3ReadRegistersTask(11574, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_3_REACTIVE_POWER, new FloatDoublewordElement(11574))),
					new FC3ReadRegistersTask(11582, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_3_APPARENT_POWER, new FloatDoublewordElement(11582))),
					new FC3ReadRegistersTask(11592, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_3_NET_KWH, new UnsignedDoublewordElement(11592))),
					new FC3ReadRegistersTask(11630, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_3_POWER_FACTOR, new FloatDoublewordElement(11630))),
					
					new FC3ReadRegistersTask(11657, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_4_CURRENT, new FloatDoublewordElement(11656))),
					new FC3ReadRegistersTask(11716, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_4_ACTIVE_POWER, new FloatDoublewordElement(11716))),
					new FC3ReadRegistersTask(11724, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_4_REACTIVE_POWER, new FloatDoublewordElement(11724))),
					new FC3ReadRegistersTask(11732, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_4_APPARENT_POWER, new FloatDoublewordElement(11732))),
					new FC3ReadRegistersTask(11742, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_4_NET_KWH, new UnsignedDoublewordElement(11742))),
					new FC3ReadRegistersTask(11780, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_4_POWER_FACTOR, new FloatDoublewordElement(11780))),
					
					new FC3ReadRegistersTask(11906, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_5_CURRENT, new FloatDoublewordElement(11906))),
					new FC3ReadRegistersTask(11966, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_5_ACTIVE_POWER, new FloatDoublewordElement(11966))),
					new FC3ReadRegistersTask(11974, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_5_REACTIVE_POWER, new FloatDoublewordElement(11974))),
					new FC3ReadRegistersTask(11982, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_5_APPARENT_POWER, new FloatDoublewordElement(11982))),
					new FC3ReadRegistersTask(11992, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_5_NET_KWH, new UnsignedDoublewordElement(11992))),
					new FC3ReadRegistersTask(12030, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_5_POWER_FACTOR, new FloatDoublewordElement(12030))),
					
					new FC3ReadRegistersTask(12156, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_6_CURRENT, new FloatDoublewordElement(12156))),
					new FC3ReadRegistersTask(12216, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_6_ACTIVE_POWER, new FloatDoublewordElement(12216))),
					new FC3ReadRegistersTask(12224, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_6_REACTIVE_POWER, new FloatDoublewordElement(12224))),
					new FC3ReadRegistersTask(12232, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_6_APPARENT_POWER, new FloatDoublewordElement(12232))),
					new FC3ReadRegistersTask(12242, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_6_NET_KWH, new UnsignedDoublewordElement(12242))),
					new FC3ReadRegistersTask(12280, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_6_POWER_FACTOR, new FloatDoublewordElement(12280))),
					
					new FC3ReadRegistersTask(12306, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_7_CURRENT, new FloatDoublewordElement(12306))),
					new FC3ReadRegistersTask(12366, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_7_ACTIVE_POWER, new FloatDoublewordElement(12366))),
					new FC3ReadRegistersTask(12374, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_7_REACTIVE_POWER, new FloatDoublewordElement(12374))),
					new FC3ReadRegistersTask(12382, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_7_APPARENT_POWER, new FloatDoublewordElement(12382))),
					new FC3ReadRegistersTask(12392, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_7_NET_KWH, new UnsignedDoublewordElement(12392))),
					new FC3ReadRegistersTask(12430, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_7_POWER_FACTOR, new FloatDoublewordElement(12430))),
					
					new FC3ReadRegistersTask(12456, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_8_CURRENT, new FloatDoublewordElement(12456))),
					new FC3ReadRegistersTask(12516, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_8_ACTIVE_POWER, new FloatDoublewordElement(12516))),
					new FC3ReadRegistersTask(12524, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_8_REACTIVE_POWER, new FloatDoublewordElement(12524))),
					new FC3ReadRegistersTask(12532, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_8_APPARENT_POWER, new FloatDoublewordElement(12532))),
					new FC3ReadRegistersTask(12542, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_8_NET_KWH, new UnsignedDoublewordElement(12542))),
					new FC3ReadRegistersTask(12580, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_8_POWER_FACTOR, new FloatDoublewordElement(12580))),
					
					new FC3ReadRegistersTask(12606, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_9_CURRENT, new FloatDoublewordElement(12606))),
					new FC3ReadRegistersTask(12666, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_9_ACTIVE_POWER, new FloatDoublewordElement(12666))),
					new FC3ReadRegistersTask(12674, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_9_REACTIVE_POWER, new FloatDoublewordElement(12674))),
					new FC3ReadRegistersTask(12682, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_9_APPARENT_POWER, new FloatDoublewordElement(12682))),
					new FC3ReadRegistersTask(12692, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_9_NET_KWH, new UnsignedDoublewordElement(12692))),
					new FC3ReadRegistersTask(12730, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_9_POWER_FACTOR, new FloatDoublewordElement(12730))),
					
					new FC3ReadRegistersTask(12756, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_10_CURRENT, new FloatDoublewordElement(12756))),
					new FC3ReadRegistersTask(12816, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_10_ACTIVE_POWER, new FloatDoublewordElement(12816))),
					new FC3ReadRegistersTask(12824, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_10_REACTIVE_POWER, new FloatDoublewordElement(12824))),
					new FC3ReadRegistersTask(12832, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_10_APPARENT_POWER, new FloatDoublewordElement(12832))),
					new FC3ReadRegistersTask(12842, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_10_NET_KWH, new UnsignedDoublewordElement(12842))),
					new FC3ReadRegistersTask(12880, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_10_POWER_FACTOR, new FloatDoublewordElement(12880))),
					
					new FC3ReadRegistersTask(12906, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_11_CURRENT, new FloatDoublewordElement(12906))),
					new FC3ReadRegistersTask(12966, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_11_ACTIVE_POWER, new FloatDoublewordElement(12966))),
					new FC3ReadRegistersTask(12974, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_11_REACTIVE_POWER, new FloatDoublewordElement(12974))),
					new FC3ReadRegistersTask(12982, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_11_APPARENT_POWER, new FloatDoublewordElement(12982))),
					new FC3ReadRegistersTask(12992, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_11_NET_KWH, new UnsignedDoublewordElement(12992))),
					new FC3ReadRegistersTask(13030, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_11_POWER_FACTOR, new FloatDoublewordElement(13030))),
					
					new FC3ReadRegistersTask(13056, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_12_CURRENT, new FloatDoublewordElement(13056))),
					new FC3ReadRegistersTask(13116, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_12_ACTIVE_POWER, new FloatDoublewordElement(13116))),
					new FC3ReadRegistersTask(13124, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_12_REACTIVE_POWER, new FloatDoublewordElement(13124))),
					new FC3ReadRegistersTask(13132, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_12_APPARENT_POWER, new FloatDoublewordElement(13132))),
					new FC3ReadRegistersTask(13142, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_12_NET_KWH, new UnsignedDoublewordElement(13142))),
					new FC3ReadRegistersTask(13180, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_12_POWER_FACTOR, new FloatDoublewordElement(13180))),
					
					new FC3ReadRegistersTask(13206, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_13_CURRENT, new FloatDoublewordElement(13206))),
					new FC3ReadRegistersTask(13266, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_13_ACTIVE_POWER, new FloatDoublewordElement(13266))),
					new FC3ReadRegistersTask(13274, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_13_REACTIVE_POWER, new FloatDoublewordElement(13274))),
					new FC3ReadRegistersTask(13282, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_13_APPARENT_POWER, new FloatDoublewordElement(13282))),
					new FC3ReadRegistersTask(13292, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_13_NET_KWH, new UnsignedDoublewordElement(13292))),
					new FC3ReadRegistersTask(13330, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_13_POWER_FACTOR, new FloatDoublewordElement(13330)))
				);
		}else if(this.config.sensor_num() == 12) {
			mod = new ModbusProtocol(this,				
					new FC3ReadRegistersTask(861, Priority.LOW,	
							m(Accura2300.ChannelId.ACCURA2300_WIRING_MODE, new UnsignedWordElement(861))),
					new FC3ReadRegistersTask(1111, Priority.LOW,	
							m(Accura2300.ChannelId.ACCURA2300_CT_CONNECTED_NUM, new UnsignedWordElement(1111))),
					new FC3ReadRegistersTask(11044, Priority.LOW,	
							m(Accura2300.ChannelId.ACCURA2300_VALIDITY_VOLTAGE_DATA, new UnsignedWordElement(11044)),
							m(Accura2300.ChannelId.ACCURA2300_VALIDITY_CT_DATA, new UnsignedWordElement(11045)),
							m(Accura2300.ChannelId.ACCURA2300_VALIDITY_CT_DATA, new UnsignedWordElement(11046)),
							m(Accura2300.ChannelId.ACCURA2300_VALIDITY_CT_DATA, new UnsignedWordElement(11047)),
							m(Accura2300.ChannelId.ACCURA2300_VALIDITY_CT_DATA, new UnsignedWordElement(11048)),
							m(Accura2300.ChannelId.ACCURA2300_VALIDITY_CT_DATA, new UnsignedWordElement(11049)),
							m(Accura2300.ChannelId.ACCURA2300_VALIDITY_CT_DATA, new UnsignedWordElement(11050)),
							m(Accura2300.ChannelId.ACCURA2300_VALIDITY_CT_DATA, new UnsignedWordElement(11051)),
							m(Accura2300.ChannelId.ACCURA2300_VALIDITY_CT_DATA, new UnsignedWordElement(11052)),
							m(Accura2300.ChannelId.ACCURA2300_VALIDITY_CT_DATA, new UnsignedWordElement(11053)),
							m(Accura2300.ChannelId.ACCURA2300_VALIDITY_CT_DATA, new UnsignedWordElement(11054)),
							m(Accura2300.ChannelId.ACCURA2300_VALIDITY_CT_DATA, new UnsignedWordElement(11055)),
							m(Accura2300.ChannelId.ACCURA2300_VALIDITY_CT_DATA, new UnsignedWordElement(11056))),
					
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
					new FC3ReadRegistersTask(11282, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_1_APPARENT_POWER, new FloatDoublewordElement(11282))),
					new FC3ReadRegistersTask(11292, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_1_NET_KWH, new UnsignedDoublewordElement(11292))),
					new FC3ReadRegistersTask(11330, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_1_POWER_FACTOR, new FloatDoublewordElement(11330))),
					
					new FC3ReadRegistersTask(11356, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_CURRENT, new FloatDoublewordElement(11356))),
					new FC3ReadRegistersTask(11416, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_ACTIVE_POWER, new FloatDoublewordElement(11416))),
					new FC3ReadRegistersTask(11424, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_REACTIVE_POWER, new FloatDoublewordElement(11424))),
					new FC3ReadRegistersTask(11432, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_APPARENT_POWER, new FloatDoublewordElement(11432))),
					new FC3ReadRegistersTask(11442, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_NET_KWH, new UnsignedDoublewordElement(11442))),
					new FC3ReadRegistersTask(11480, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_POWER_FACTOR, new FloatDoublewordElement(11480))),
					
					new FC3ReadRegistersTask(11506, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_3_CURRENT, new FloatDoublewordElement(11506))),
					new FC3ReadRegistersTask(11566, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_3_ACTIVE_POWER, new FloatDoublewordElement(11566))),
					new FC3ReadRegistersTask(11574, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_3_REACTIVE_POWER, new FloatDoublewordElement(11574))),
					new FC3ReadRegistersTask(11582, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_3_APPARENT_POWER, new FloatDoublewordElement(11582))),
					new FC3ReadRegistersTask(11592, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_3_NET_KWH, new UnsignedDoublewordElement(11592))),
					new FC3ReadRegistersTask(11630, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_3_POWER_FACTOR, new FloatDoublewordElement(11630))),
					
					new FC3ReadRegistersTask(11657, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_4_CURRENT, new FloatDoublewordElement(11656))),
					new FC3ReadRegistersTask(11716, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_4_ACTIVE_POWER, new FloatDoublewordElement(11716))),
					new FC3ReadRegistersTask(11724, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_4_REACTIVE_POWER, new FloatDoublewordElement(11724))),
					new FC3ReadRegistersTask(11732, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_4_APPARENT_POWER, new FloatDoublewordElement(11732))),
					new FC3ReadRegistersTask(11742, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_4_NET_KWH, new UnsignedDoublewordElement(11742))),
					new FC3ReadRegistersTask(11780, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_4_POWER_FACTOR, new FloatDoublewordElement(11780))),
					
					new FC3ReadRegistersTask(11906, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_5_CURRENT, new FloatDoublewordElement(11906))),
					new FC3ReadRegistersTask(11966, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_5_ACTIVE_POWER, new FloatDoublewordElement(11966))),
					new FC3ReadRegistersTask(11974, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_5_REACTIVE_POWER, new FloatDoublewordElement(11974))),
					new FC3ReadRegistersTask(11982, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_5_APPARENT_POWER, new FloatDoublewordElement(11982))),
					new FC3ReadRegistersTask(11992, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_5_NET_KWH, new UnsignedDoublewordElement(11992))),
					new FC3ReadRegistersTask(12030, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_5_POWER_FACTOR, new FloatDoublewordElement(12030))),
					
					new FC3ReadRegistersTask(12156, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_6_CURRENT, new FloatDoublewordElement(12156))),
					new FC3ReadRegistersTask(12216, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_6_ACTIVE_POWER, new FloatDoublewordElement(12216))),
					new FC3ReadRegistersTask(12224, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_6_REACTIVE_POWER, new FloatDoublewordElement(12224))),
					new FC3ReadRegistersTask(12232, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_6_APPARENT_POWER, new FloatDoublewordElement(12232))),
					new FC3ReadRegistersTask(12242, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_6_NET_KWH, new UnsignedDoublewordElement(12242))),
					new FC3ReadRegistersTask(12280, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_6_POWER_FACTOR, new FloatDoublewordElement(12280))),
					
					new FC3ReadRegistersTask(12306, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_7_CURRENT, new FloatDoublewordElement(12306))),
					new FC3ReadRegistersTask(12366, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_7_ACTIVE_POWER, new FloatDoublewordElement(12366))),
					new FC3ReadRegistersTask(12374, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_7_REACTIVE_POWER, new FloatDoublewordElement(12374))),
					new FC3ReadRegistersTask(12382, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_7_APPARENT_POWER, new FloatDoublewordElement(12382))),
					new FC3ReadRegistersTask(12392, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_7_NET_KWH, new UnsignedDoublewordElement(12392))),
					new FC3ReadRegistersTask(12430, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_7_POWER_FACTOR, new FloatDoublewordElement(12430))),
					
					new FC3ReadRegistersTask(12456, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_8_CURRENT, new FloatDoublewordElement(12456))),
					new FC3ReadRegistersTask(12516, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_8_ACTIVE_POWER, new FloatDoublewordElement(12516))),
					new FC3ReadRegistersTask(12524, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_8_REACTIVE_POWER, new FloatDoublewordElement(12524))),
					new FC3ReadRegistersTask(12532, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_8_APPARENT_POWER, new FloatDoublewordElement(12532))),
					new FC3ReadRegistersTask(12542, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_8_NET_KWH, new UnsignedDoublewordElement(12542))),
					new FC3ReadRegistersTask(12580, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_8_POWER_FACTOR, new FloatDoublewordElement(12580))),
					
					new FC3ReadRegistersTask(12606, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_9_CURRENT, new FloatDoublewordElement(12606))),
					new FC3ReadRegistersTask(12666, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_9_ACTIVE_POWER, new FloatDoublewordElement(12666))),
					new FC3ReadRegistersTask(12674, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_9_REACTIVE_POWER, new FloatDoublewordElement(12674))),
					new FC3ReadRegistersTask(12682, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_9_APPARENT_POWER, new FloatDoublewordElement(12682))),
					new FC3ReadRegistersTask(12692, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_9_NET_KWH, new UnsignedDoublewordElement(12692))),
					new FC3ReadRegistersTask(12730, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_9_POWER_FACTOR, new FloatDoublewordElement(12730))),
					
					new FC3ReadRegistersTask(12756, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_10_CURRENT, new FloatDoublewordElement(12756))),
					new FC3ReadRegistersTask(12816, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_10_ACTIVE_POWER, new FloatDoublewordElement(12816))),
					new FC3ReadRegistersTask(12824, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_10_REACTIVE_POWER, new FloatDoublewordElement(12824))),
					new FC3ReadRegistersTask(12832, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_10_APPARENT_POWER, new FloatDoublewordElement(12832))),
					new FC3ReadRegistersTask(12842, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_10_NET_KWH, new UnsignedDoublewordElement(12842))),
					new FC3ReadRegistersTask(12880, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_10_POWER_FACTOR, new FloatDoublewordElement(12880))),
					
					new FC3ReadRegistersTask(12906, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_11_CURRENT, new FloatDoublewordElement(12906))),
					new FC3ReadRegistersTask(12966, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_11_ACTIVE_POWER, new FloatDoublewordElement(12966))),
					new FC3ReadRegistersTask(12974, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_11_REACTIVE_POWER, new FloatDoublewordElement(12974))),
					new FC3ReadRegistersTask(12982, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_11_APPARENT_POWER, new FloatDoublewordElement(12982))),
					new FC3ReadRegistersTask(12992, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_11_NET_KWH, new UnsignedDoublewordElement(12992))),
					new FC3ReadRegistersTask(13030, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_11_POWER_FACTOR, new FloatDoublewordElement(13030))),
					
					new FC3ReadRegistersTask(13056, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_12_CURRENT, new FloatDoublewordElement(13056))),
					new FC3ReadRegistersTask(13116, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_12_ACTIVE_POWER, new FloatDoublewordElement(13116))),
					new FC3ReadRegistersTask(13124, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_12_REACTIVE_POWER, new FloatDoublewordElement(13124))),
					new FC3ReadRegistersTask(13132, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_12_APPARENT_POWER, new FloatDoublewordElement(13132))),
					new FC3ReadRegistersTask(13142, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_12_NET_KWH, new UnsignedDoublewordElement(13142))),
					new FC3ReadRegistersTask(13180, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_12_POWER_FACTOR, new FloatDoublewordElement(13180)))
				);
		}else if(this.config.sensor_num() == 11) {
			mod = new ModbusProtocol(this,					
					new FC3ReadRegistersTask(861, Priority.LOW,	
							m(Accura2300.ChannelId.ACCURA2300_WIRING_MODE, new UnsignedWordElement(861))),
					new FC3ReadRegistersTask(1111, Priority.LOW,	
							m(Accura2300.ChannelId.ACCURA2300_CT_CONNECTED_NUM, new UnsignedWordElement(1111))),
					new FC3ReadRegistersTask(11044, Priority.LOW,	
							m(Accura2300.ChannelId.ACCURA2300_VALIDITY_VOLTAGE_DATA, new UnsignedWordElement(11044)),
							m(Accura2300.ChannelId.ACCURA2300_VALIDITY_CT_DATA, new UnsignedWordElement(11045)),
							m(Accura2300.ChannelId.ACCURA2300_VALIDITY_CT_DATA, new UnsignedWordElement(11046)),
							m(Accura2300.ChannelId.ACCURA2300_VALIDITY_CT_DATA, new UnsignedWordElement(11047)),
							m(Accura2300.ChannelId.ACCURA2300_VALIDITY_CT_DATA, new UnsignedWordElement(11048)),
							m(Accura2300.ChannelId.ACCURA2300_VALIDITY_CT_DATA, new UnsignedWordElement(11049)),
							m(Accura2300.ChannelId.ACCURA2300_VALIDITY_CT_DATA, new UnsignedWordElement(11050)),
							m(Accura2300.ChannelId.ACCURA2300_VALIDITY_CT_DATA, new UnsignedWordElement(11051)),
							m(Accura2300.ChannelId.ACCURA2300_VALIDITY_CT_DATA, new UnsignedWordElement(11052)),
							m(Accura2300.ChannelId.ACCURA2300_VALIDITY_CT_DATA, new UnsignedWordElement(11053)),
							m(Accura2300.ChannelId.ACCURA2300_VALIDITY_CT_DATA, new UnsignedWordElement(11054)),
							m(Accura2300.ChannelId.ACCURA2300_VALIDITY_CT_DATA, new UnsignedWordElement(11055))),
					
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
					new FC3ReadRegistersTask(11282, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_1_APPARENT_POWER, new FloatDoublewordElement(11282))),
					new FC3ReadRegistersTask(11292, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_1_NET_KWH, new UnsignedDoublewordElement(11292))),
					new FC3ReadRegistersTask(11330, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_1_POWER_FACTOR, new FloatDoublewordElement(11330))),
					
					new FC3ReadRegistersTask(11356, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_CURRENT, new FloatDoublewordElement(11356))),
					new FC3ReadRegistersTask(11416, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_ACTIVE_POWER, new FloatDoublewordElement(11416))),
					new FC3ReadRegistersTask(11424, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_REACTIVE_POWER, new FloatDoublewordElement(11424))),
					new FC3ReadRegistersTask(11432, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_APPARENT_POWER, new FloatDoublewordElement(11432))),
					new FC3ReadRegistersTask(11442, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_NET_KWH, new UnsignedDoublewordElement(11442))),
					new FC3ReadRegistersTask(11480, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_POWER_FACTOR, new FloatDoublewordElement(11480))),
					
					new FC3ReadRegistersTask(11506, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_3_CURRENT, new FloatDoublewordElement(11506))),
					new FC3ReadRegistersTask(11566, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_3_ACTIVE_POWER, new FloatDoublewordElement(11566))),
					new FC3ReadRegistersTask(11574, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_3_REACTIVE_POWER, new FloatDoublewordElement(11574))),
					new FC3ReadRegistersTask(11582, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_3_APPARENT_POWER, new FloatDoublewordElement(11582))),
					new FC3ReadRegistersTask(11592, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_3_NET_KWH, new UnsignedDoublewordElement(11592))),
					new FC3ReadRegistersTask(11630, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_3_POWER_FACTOR, new FloatDoublewordElement(11630))),
					
					new FC3ReadRegistersTask(11657, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_4_CURRENT, new FloatDoublewordElement(11656))),
					new FC3ReadRegistersTask(11716, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_4_ACTIVE_POWER, new FloatDoublewordElement(11716))),
					new FC3ReadRegistersTask(11724, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_4_REACTIVE_POWER, new FloatDoublewordElement(11724))),
					new FC3ReadRegistersTask(11732, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_4_APPARENT_POWER, new FloatDoublewordElement(11732))),
					new FC3ReadRegistersTask(11742, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_4_NET_KWH, new UnsignedDoublewordElement(11742))),
					new FC3ReadRegistersTask(11780, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_4_POWER_FACTOR, new FloatDoublewordElement(11780))),
					
					new FC3ReadRegistersTask(11906, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_5_CURRENT, new FloatDoublewordElement(11906))),
					new FC3ReadRegistersTask(11966, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_5_ACTIVE_POWER, new FloatDoublewordElement(11966))),
					new FC3ReadRegistersTask(11974, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_5_REACTIVE_POWER, new FloatDoublewordElement(11974))),
					new FC3ReadRegistersTask(11982, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_5_APPARENT_POWER, new FloatDoublewordElement(11982))),
					new FC3ReadRegistersTask(11992, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_5_NET_KWH, new UnsignedDoublewordElement(11992))),
					new FC3ReadRegistersTask(12030, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_5_POWER_FACTOR, new FloatDoublewordElement(12030))),
					
					new FC3ReadRegistersTask(12156, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_6_CURRENT, new FloatDoublewordElement(12156))),
					new FC3ReadRegistersTask(12216, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_6_ACTIVE_POWER, new FloatDoublewordElement(12216))),
					new FC3ReadRegistersTask(12224, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_6_REACTIVE_POWER, new FloatDoublewordElement(12224))),
					new FC3ReadRegistersTask(12232, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_6_APPARENT_POWER, new FloatDoublewordElement(12232))),
					new FC3ReadRegistersTask(12242, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_6_NET_KWH, new UnsignedDoublewordElement(12242))),
					new FC3ReadRegistersTask(12280, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_6_POWER_FACTOR, new FloatDoublewordElement(12280))),
					
					new FC3ReadRegistersTask(12306, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_7_CURRENT, new FloatDoublewordElement(12306))),
					new FC3ReadRegistersTask(12366, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_7_ACTIVE_POWER, new FloatDoublewordElement(12366))),
					new FC3ReadRegistersTask(12374, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_7_REACTIVE_POWER, new FloatDoublewordElement(12374))),
					new FC3ReadRegistersTask(12382, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_7_APPARENT_POWER, new FloatDoublewordElement(12382))),
					new FC3ReadRegistersTask(12392, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_7_NET_KWH, new UnsignedDoublewordElement(12392))),
					new FC3ReadRegistersTask(12430, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_7_POWER_FACTOR, new FloatDoublewordElement(12430))),
					
					new FC3ReadRegistersTask(12456, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_8_CURRENT, new FloatDoublewordElement(12456))),
					new FC3ReadRegistersTask(12516, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_8_ACTIVE_POWER, new FloatDoublewordElement(12516))),
					new FC3ReadRegistersTask(12524, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_8_REACTIVE_POWER, new FloatDoublewordElement(12524))),
					new FC3ReadRegistersTask(12532, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_8_APPARENT_POWER, new FloatDoublewordElement(12532))),
					new FC3ReadRegistersTask(12542, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_8_NET_KWH, new UnsignedDoublewordElement(12542))),
					new FC3ReadRegistersTask(12580, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_8_POWER_FACTOR, new FloatDoublewordElement(12580))),
					
					new FC3ReadRegistersTask(12606, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_9_CURRENT, new FloatDoublewordElement(12606))),
					new FC3ReadRegistersTask(12666, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_9_ACTIVE_POWER, new FloatDoublewordElement(12666))),
					new FC3ReadRegistersTask(12674, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_9_REACTIVE_POWER, new FloatDoublewordElement(12674))),
					new FC3ReadRegistersTask(12682, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_9_APPARENT_POWER, new FloatDoublewordElement(12682))),
					new FC3ReadRegistersTask(12692, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_9_NET_KWH, new UnsignedDoublewordElement(12692))),
					new FC3ReadRegistersTask(12740, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_9_POWER_FACTOR, new FloatDoublewordElement(12740))),
					
					new FC3ReadRegistersTask(12756, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_10_CURRENT, new FloatDoublewordElement(12756))),
					new FC3ReadRegistersTask(12816, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_10_ACTIVE_POWER, new FloatDoublewordElement(12816))),
					new FC3ReadRegistersTask(12824, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_10_REACTIVE_POWER, new FloatDoublewordElement(12824))),
					new FC3ReadRegistersTask(12832, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_10_APPARENT_POWER, new FloatDoublewordElement(12832))),
					new FC3ReadRegistersTask(12842, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_10_NET_KWH, new UnsignedDoublewordElement(12842))),
					new FC3ReadRegistersTask(12880, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_10_POWER_FACTOR, new FloatDoublewordElement(12880))),
					
					new FC3ReadRegistersTask(12906, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_11_CURRENT, new FloatDoublewordElement(12906))),
					new FC3ReadRegistersTask(12966, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_11_ACTIVE_POWER, new FloatDoublewordElement(12966))),
					new FC3ReadRegistersTask(12974, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_11_REACTIVE_POWER, new FloatDoublewordElement(12974))),
					new FC3ReadRegistersTask(12982, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_11_APPARENT_POWER, new FloatDoublewordElement(12982))),
					new FC3ReadRegistersTask(12992, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_11_NET_KWH, new UnsignedDoublewordElement(12992))),
					new FC3ReadRegistersTask(13030, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_11_POWER_FACTOR, new FloatDoublewordElement(13030)))
				);
		}else if(this.config.sensor_num() == 10) {
			mod = new ModbusProtocol(this,					
					new FC3ReadRegistersTask(861, Priority.LOW,	
							m(Accura2300.ChannelId.ACCURA2300_WIRING_MODE, new UnsignedWordElement(861))),
					new FC3ReadRegistersTask(1111, Priority.LOW,	
							m(Accura2300.ChannelId.ACCURA2300_CT_CONNECTED_NUM, new UnsignedWordElement(1111))),
					new FC3ReadRegistersTask(11044, Priority.LOW,	
							m(Accura2300.ChannelId.ACCURA2300_VALIDITY_VOLTAGE_DATA, new UnsignedWordElement(11044)),
							m(Accura2300.ChannelId.ACCURA2300_VALIDITY_CT_DATA, new UnsignedWordElement(11045)),
							m(Accura2300.ChannelId.ACCURA2300_VALIDITY_CT_DATA, new UnsignedWordElement(11046)),
							m(Accura2300.ChannelId.ACCURA2300_VALIDITY_CT_DATA, new UnsignedWordElement(11047)),
							m(Accura2300.ChannelId.ACCURA2300_VALIDITY_CT_DATA, new UnsignedWordElement(11048)),
							m(Accura2300.ChannelId.ACCURA2300_VALIDITY_CT_DATA, new UnsignedWordElement(11049)),
							m(Accura2300.ChannelId.ACCURA2300_VALIDITY_CT_DATA, new UnsignedWordElement(11050)),
							m(Accura2300.ChannelId.ACCURA2300_VALIDITY_CT_DATA, new UnsignedWordElement(11051)),
							m(Accura2300.ChannelId.ACCURA2300_VALIDITY_CT_DATA, new UnsignedWordElement(11052)),
							m(Accura2300.ChannelId.ACCURA2300_VALIDITY_CT_DATA, new UnsignedWordElement(11053)),
							m(Accura2300.ChannelId.ACCURA2300_VALIDITY_CT_DATA, new UnsignedWordElement(11054))),
					
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
					new FC3ReadRegistersTask(11282, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_1_APPARENT_POWER, new FloatDoublewordElement(11282))),
					new FC3ReadRegistersTask(11292, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_1_NET_KWH, new UnsignedDoublewordElement(11292))),
					new FC3ReadRegistersTask(11330, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_1_POWER_FACTOR, new FloatDoublewordElement(11330))),
					
					new FC3ReadRegistersTask(11356, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_CURRENT, new FloatDoublewordElement(11356))),
					new FC3ReadRegistersTask(11416, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_ACTIVE_POWER, new FloatDoublewordElement(11416))),
					new FC3ReadRegistersTask(11424, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_REACTIVE_POWER, new FloatDoublewordElement(11424))),
					new FC3ReadRegistersTask(11432, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_APPARENT_POWER, new FloatDoublewordElement(11432))),
					new FC3ReadRegistersTask(11442, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_NET_KWH, new UnsignedDoublewordElement(11442))),
					new FC3ReadRegistersTask(11480, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_POWER_FACTOR, new FloatDoublewordElement(11480))),
					
					new FC3ReadRegistersTask(11506, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_3_CURRENT, new FloatDoublewordElement(11506))),
					new FC3ReadRegistersTask(11566, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_3_ACTIVE_POWER, new FloatDoublewordElement(11566))),
					new FC3ReadRegistersTask(11574, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_3_REACTIVE_POWER, new FloatDoublewordElement(11574))),
					new FC3ReadRegistersTask(11582, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_3_APPARENT_POWER, new FloatDoublewordElement(11582))),
					new FC3ReadRegistersTask(11592, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_3_NET_KWH, new UnsignedDoublewordElement(11592))),
					new FC3ReadRegistersTask(11630, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_3_POWER_FACTOR, new FloatDoublewordElement(11630))),
					
					new FC3ReadRegistersTask(11657, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_4_CURRENT, new FloatDoublewordElement(11656))),
					new FC3ReadRegistersTask(11716, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_4_ACTIVE_POWER, new FloatDoublewordElement(11716))),
					new FC3ReadRegistersTask(11724, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_4_REACTIVE_POWER, new FloatDoublewordElement(11724))),
					new FC3ReadRegistersTask(11732, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_4_APPARENT_POWER, new FloatDoublewordElement(11732))),
					new FC3ReadRegistersTask(11742, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_4_NET_KWH, new UnsignedDoublewordElement(11742))),
					new FC3ReadRegistersTask(11780, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_4_POWER_FACTOR, new FloatDoublewordElement(11780))),
					
					new FC3ReadRegistersTask(11906, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_5_CURRENT, new FloatDoublewordElement(11906))),
					new FC3ReadRegistersTask(11966, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_5_ACTIVE_POWER, new FloatDoublewordElement(11966))),
					new FC3ReadRegistersTask(11974, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_5_REACTIVE_POWER, new FloatDoublewordElement(11974))),
					new FC3ReadRegistersTask(11982, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_5_APPARENT_POWER, new FloatDoublewordElement(11982))),
					new FC3ReadRegistersTask(11992, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_5_NET_KWH, new UnsignedDoublewordElement(11992))),
					new FC3ReadRegistersTask(12030, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_5_POWER_FACTOR, new FloatDoublewordElement(12030))),
					
					new FC3ReadRegistersTask(12156, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_6_CURRENT, new FloatDoublewordElement(12156))),
					new FC3ReadRegistersTask(12216, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_6_ACTIVE_POWER, new FloatDoublewordElement(12216))),
					new FC3ReadRegistersTask(12224, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_6_REACTIVE_POWER, new FloatDoublewordElement(12224))),
					new FC3ReadRegistersTask(12232, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_6_APPARENT_POWER, new FloatDoublewordElement(12232))),
					new FC3ReadRegistersTask(12242, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_6_NET_KWH, new UnsignedDoublewordElement(12242))),
					new FC3ReadRegistersTask(12280, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_6_POWER_FACTOR, new FloatDoublewordElement(12280))),
					
					new FC3ReadRegistersTask(12306, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_7_CURRENT, new FloatDoublewordElement(12306))),
					new FC3ReadRegistersTask(12366, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_7_ACTIVE_POWER, new FloatDoublewordElement(12366))),
					new FC3ReadRegistersTask(12374, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_7_REACTIVE_POWER, new FloatDoublewordElement(12374))),
					new FC3ReadRegistersTask(12382, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_7_APPARENT_POWER, new FloatDoublewordElement(12382))),
					new FC3ReadRegistersTask(12392, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_7_NET_KWH, new UnsignedDoublewordElement(12392))),
					new FC3ReadRegistersTask(12430, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_7_POWER_FACTOR, new FloatDoublewordElement(12430))),
					
					new FC3ReadRegistersTask(12456, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_8_CURRENT, new FloatDoublewordElement(12456))),
					new FC3ReadRegistersTask(12516, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_8_ACTIVE_POWER, new FloatDoublewordElement(12516))),
					new FC3ReadRegistersTask(12524, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_8_REACTIVE_POWER, new FloatDoublewordElement(12524))),
					new FC3ReadRegistersTask(12532, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_8_APPARENT_POWER, new FloatDoublewordElement(12532))),
					new FC3ReadRegistersTask(12542, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_8_NET_KWH, new UnsignedDoublewordElement(12542))),
					new FC3ReadRegistersTask(12580, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_8_POWER_FACTOR, new FloatDoublewordElement(12580))),
					
					new FC3ReadRegistersTask(12606, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_9_CURRENT, new FloatDoublewordElement(12606))),
					new FC3ReadRegistersTask(12666, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_9_ACTIVE_POWER, new FloatDoublewordElement(12666))),
					new FC3ReadRegistersTask(12674, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_9_REACTIVE_POWER, new FloatDoublewordElement(12674))),
					new FC3ReadRegistersTask(12682, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_9_APPARENT_POWER, new FloatDoublewordElement(12682))),
					new FC3ReadRegistersTask(12692, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_9_NET_KWH, new UnsignedDoublewordElement(12692))),
					new FC3ReadRegistersTask(12730, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_9_POWER_FACTOR, new FloatDoublewordElement(12730))),
					
					new FC3ReadRegistersTask(12756, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_10_CURRENT, new FloatDoublewordElement(12756))),
					new FC3ReadRegistersTask(12816, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_10_ACTIVE_POWER, new FloatDoublewordElement(12816))),
					new FC3ReadRegistersTask(12824, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_10_REACTIVE_POWER, new FloatDoublewordElement(12824))),
					new FC3ReadRegistersTask(12832, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_10_APPARENT_POWER, new FloatDoublewordElement(12832))),
					new FC3ReadRegistersTask(12842, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_10_NET_KWH, new UnsignedDoublewordElement(12842))),
					new FC3ReadRegistersTask(12880, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_10_POWER_FACTOR, new FloatDoublewordElement(12880)))
				);
		}else if(this.config.sensor_num() == 9) {
			mod = new ModbusProtocol(this,					
					new FC3ReadRegistersTask(861, Priority.LOW,	
							m(Accura2300.ChannelId.ACCURA2300_WIRING_MODE, new UnsignedWordElement(861))),
					new FC3ReadRegistersTask(1111, Priority.LOW,	
							m(Accura2300.ChannelId.ACCURA2300_CT_CONNECTED_NUM, new UnsignedWordElement(1111))),
					new FC3ReadRegistersTask(11044, Priority.LOW,	
							m(Accura2300.ChannelId.ACCURA2300_VALIDITY_VOLTAGE_DATA, new UnsignedWordElement(11044)),
							m(Accura2300.ChannelId.ACCURA2300_VALIDITY_CT_DATA, new UnsignedWordElement(11045)),
							m(Accura2300.ChannelId.ACCURA2300_VALIDITY_CT_DATA, new UnsignedWordElement(11046)),
							m(Accura2300.ChannelId.ACCURA2300_VALIDITY_CT_DATA, new UnsignedWordElement(11047)),
							m(Accura2300.ChannelId.ACCURA2300_VALIDITY_CT_DATA, new UnsignedWordElement(11048)),
							m(Accura2300.ChannelId.ACCURA2300_VALIDITY_CT_DATA, new UnsignedWordElement(11049)),
							m(Accura2300.ChannelId.ACCURA2300_VALIDITY_CT_DATA, new UnsignedWordElement(11050)),
							m(Accura2300.ChannelId.ACCURA2300_VALIDITY_CT_DATA, new UnsignedWordElement(11051)),
							m(Accura2300.ChannelId.ACCURA2300_VALIDITY_CT_DATA, new UnsignedWordElement(11052)),
							m(Accura2300.ChannelId.ACCURA2300_VALIDITY_CT_DATA, new UnsignedWordElement(11053))),
					
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
					new FC3ReadRegistersTask(11282, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_1_APPARENT_POWER, new FloatDoublewordElement(11282))),
					new FC3ReadRegistersTask(11292, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_1_NET_KWH, new UnsignedDoublewordElement(11292))),
					new FC3ReadRegistersTask(11330, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_1_POWER_FACTOR, new FloatDoublewordElement(11330))),
					
					new FC3ReadRegistersTask(11356, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_CURRENT, new FloatDoublewordElement(11356))),
					new FC3ReadRegistersTask(11416, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_ACTIVE_POWER, new FloatDoublewordElement(11416))),
					new FC3ReadRegistersTask(11424, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_REACTIVE_POWER, new FloatDoublewordElement(11424))),
					new FC3ReadRegistersTask(11432, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_APPARENT_POWER, new FloatDoublewordElement(11432))),
					new FC3ReadRegistersTask(11442, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_NET_KWH, new UnsignedDoublewordElement(11442))),
					new FC3ReadRegistersTask(11480, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_POWER_FACTOR, new FloatDoublewordElement(11480))),
					
					new FC3ReadRegistersTask(11506, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_3_CURRENT, new FloatDoublewordElement(11506))),
					new FC3ReadRegistersTask(11566, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_3_ACTIVE_POWER, new FloatDoublewordElement(11566))),
					new FC3ReadRegistersTask(11574, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_3_REACTIVE_POWER, new FloatDoublewordElement(11574))),
					new FC3ReadRegistersTask(11582, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_3_APPARENT_POWER, new FloatDoublewordElement(11582))),
					new FC3ReadRegistersTask(11592, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_3_NET_KWH, new UnsignedDoublewordElement(11592))),
					new FC3ReadRegistersTask(11630, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_3_POWER_FACTOR, new FloatDoublewordElement(11630))),
					
					new FC3ReadRegistersTask(11657, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_4_CURRENT, new FloatDoublewordElement(11656))),
					new FC3ReadRegistersTask(11716, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_4_ACTIVE_POWER, new FloatDoublewordElement(11716))),
					new FC3ReadRegistersTask(11724, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_4_REACTIVE_POWER, new FloatDoublewordElement(11724))),
					new FC3ReadRegistersTask(11732, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_4_APPARENT_POWER, new FloatDoublewordElement(11732))),
					new FC3ReadRegistersTask(11742, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_4_NET_KWH, new UnsignedDoublewordElement(11742))),
					new FC3ReadRegistersTask(11780, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_4_POWER_FACTOR, new FloatDoublewordElement(11780))),
					
					new FC3ReadRegistersTask(11906, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_5_CURRENT, new FloatDoublewordElement(11906))),
					new FC3ReadRegistersTask(11966, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_5_ACTIVE_POWER, new FloatDoublewordElement(11966))),
					new FC3ReadRegistersTask(11974, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_5_REACTIVE_POWER, new FloatDoublewordElement(11974))),
					new FC3ReadRegistersTask(11982, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_5_APPARENT_POWER, new FloatDoublewordElement(11982))),
					new FC3ReadRegistersTask(11992, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_5_NET_KWH, new UnsignedDoublewordElement(11992))),
					new FC3ReadRegistersTask(12030, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_5_POWER_FACTOR, new FloatDoublewordElement(12030))),
					
					new FC3ReadRegistersTask(12156, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_6_CURRENT, new FloatDoublewordElement(12156))),
					new FC3ReadRegistersTask(12216, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_6_ACTIVE_POWER, new FloatDoublewordElement(12216))),
					new FC3ReadRegistersTask(12224, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_6_REACTIVE_POWER, new FloatDoublewordElement(12224))),
					new FC3ReadRegistersTask(12232, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_6_APPARENT_POWER, new FloatDoublewordElement(12232))),
					new FC3ReadRegistersTask(12242, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_6_NET_KWH, new UnsignedDoublewordElement(12242))),
					new FC3ReadRegistersTask(12280, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_6_POWER_FACTOR, new FloatDoublewordElement(12280))),
					
					new FC3ReadRegistersTask(12306, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_7_CURRENT, new FloatDoublewordElement(12306))),
					new FC3ReadRegistersTask(12366, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_7_ACTIVE_POWER, new FloatDoublewordElement(12366))),
					new FC3ReadRegistersTask(12374, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_7_REACTIVE_POWER, new FloatDoublewordElement(12374))),
					new FC3ReadRegistersTask(12382, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_7_APPARENT_POWER, new FloatDoublewordElement(12382))),
					new FC3ReadRegistersTask(12392, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_7_NET_KWH, new UnsignedDoublewordElement(12392))),
					new FC3ReadRegistersTask(12430, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_7_POWER_FACTOR, new FloatDoublewordElement(12430))),
					
					new FC3ReadRegistersTask(12456, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_8_CURRENT, new FloatDoublewordElement(12456))),
					new FC3ReadRegistersTask(12516, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_8_ACTIVE_POWER, new FloatDoublewordElement(12516))),
					new FC3ReadRegistersTask(12524, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_8_REACTIVE_POWER, new FloatDoublewordElement(12524))),
					new FC3ReadRegistersTask(12532, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_8_APPARENT_POWER, new FloatDoublewordElement(12532))),
					new FC3ReadRegistersTask(12542, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_8_NET_KWH, new UnsignedDoublewordElement(12542))),
					new FC3ReadRegistersTask(12580, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_8_POWER_FACTOR, new FloatDoublewordElement(12580))),
					
					new FC3ReadRegistersTask(12606, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_9_CURRENT, new FloatDoublewordElement(12606))),
					new FC3ReadRegistersTask(12666, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_9_ACTIVE_POWER, new FloatDoublewordElement(12666))),
					new FC3ReadRegistersTask(12674, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_9_REACTIVE_POWER, new FloatDoublewordElement(12674))),
					new FC3ReadRegistersTask(12682, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_9_APPARENT_POWER, new FloatDoublewordElement(12682))),
					new FC3ReadRegistersTask(12692, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_9_NET_KWH, new UnsignedDoublewordElement(12692))),
					new FC3ReadRegistersTask(12730, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_9_POWER_FACTOR, new FloatDoublewordElement(12730)))
				);
		}else if(this.config.sensor_num() == 8) {
			mod = new ModbusProtocol(this,					
					new FC3ReadRegistersTask(861, Priority.LOW,	
							m(Accura2300.ChannelId.ACCURA2300_WIRING_MODE, new UnsignedWordElement(861))),
					new FC3ReadRegistersTask(1111, Priority.LOW,	
							m(Accura2300.ChannelId.ACCURA2300_CT_CONNECTED_NUM, new UnsignedWordElement(1111))),
					new FC3ReadRegistersTask(11044, Priority.LOW,	
							m(Accura2300.ChannelId.ACCURA2300_VALIDITY_VOLTAGE_DATA, new UnsignedWordElement(11044)),
							m(Accura2300.ChannelId.ACCURA2300_VALIDITY_CT_DATA, new UnsignedWordElement(11045)),
							m(Accura2300.ChannelId.ACCURA2300_VALIDITY_CT_DATA, new UnsignedWordElement(11046)),
							m(Accura2300.ChannelId.ACCURA2300_VALIDITY_CT_DATA, new UnsignedWordElement(11047)),
							m(Accura2300.ChannelId.ACCURA2300_VALIDITY_CT_DATA, new UnsignedWordElement(11048)),
							m(Accura2300.ChannelId.ACCURA2300_VALIDITY_CT_DATA, new UnsignedWordElement(11049)),
							m(Accura2300.ChannelId.ACCURA2300_VALIDITY_CT_DATA, new UnsignedWordElement(11050)),
							m(Accura2300.ChannelId.ACCURA2300_VALIDITY_CT_DATA, new UnsignedWordElement(11051)),
							m(Accura2300.ChannelId.ACCURA2300_VALIDITY_CT_DATA, new UnsignedWordElement(11052))),
					
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
					new FC3ReadRegistersTask(11282, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_1_APPARENT_POWER, new FloatDoublewordElement(11282))),
					new FC3ReadRegistersTask(11292, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_1_NET_KWH, new UnsignedDoublewordElement(11292))),
					new FC3ReadRegistersTask(11330, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_1_POWER_FACTOR, new FloatDoublewordElement(11330))),
					
					new FC3ReadRegistersTask(11356, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_CURRENT, new FloatDoublewordElement(11356))),
					new FC3ReadRegistersTask(11416, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_ACTIVE_POWER, new FloatDoublewordElement(11416))),
					new FC3ReadRegistersTask(11424, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_REACTIVE_POWER, new FloatDoublewordElement(11424))),
					new FC3ReadRegistersTask(11432, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_APPARENT_POWER, new FloatDoublewordElement(11432))),
					new FC3ReadRegistersTask(11442, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_NET_KWH, new UnsignedDoublewordElement(11442))),
					new FC3ReadRegistersTask(11480, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_POWER_FACTOR, new FloatDoublewordElement(11480))),
					
					new FC3ReadRegistersTask(11506, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_3_CURRENT, new FloatDoublewordElement(11506))),
					new FC3ReadRegistersTask(11566, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_3_ACTIVE_POWER, new FloatDoublewordElement(11566))),
					new FC3ReadRegistersTask(11574, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_3_REACTIVE_POWER, new FloatDoublewordElement(11574))),
					new FC3ReadRegistersTask(11582, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_3_APPARENT_POWER, new FloatDoublewordElement(11582))),
					new FC3ReadRegistersTask(11592, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_3_NET_KWH, new UnsignedDoublewordElement(11592))),
					new FC3ReadRegistersTask(11630, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_3_POWER_FACTOR, new FloatDoublewordElement(11630))),
					
					new FC3ReadRegistersTask(11657, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_4_CURRENT, new FloatDoublewordElement(11656))),
					new FC3ReadRegistersTask(11716, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_4_ACTIVE_POWER, new FloatDoublewordElement(11716))),
					new FC3ReadRegistersTask(11724, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_4_REACTIVE_POWER, new FloatDoublewordElement(11724))),
					new FC3ReadRegistersTask(11732, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_4_APPARENT_POWER, new FloatDoublewordElement(11732))),
					new FC3ReadRegistersTask(11742, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_4_NET_KWH, new UnsignedDoublewordElement(11742))),
					new FC3ReadRegistersTask(11780, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_4_POWER_FACTOR, new FloatDoublewordElement(11780))),
					
					new FC3ReadRegistersTask(11906, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_5_CURRENT, new FloatDoublewordElement(11906))),
					new FC3ReadRegistersTask(11966, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_5_ACTIVE_POWER, new FloatDoublewordElement(11966))),
					new FC3ReadRegistersTask(11974, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_5_REACTIVE_POWER, new FloatDoublewordElement(11974))),
					new FC3ReadRegistersTask(11982, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_5_APPARENT_POWER, new FloatDoublewordElement(11982))),
					new FC3ReadRegistersTask(11992, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_5_NET_KWH, new UnsignedDoublewordElement(11992))),
					new FC3ReadRegistersTask(12030, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_5_POWER_FACTOR, new FloatDoublewordElement(12030))),
					
					new FC3ReadRegistersTask(12156, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_6_CURRENT, new FloatDoublewordElement(12156))),
					new FC3ReadRegistersTask(12216, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_6_ACTIVE_POWER, new FloatDoublewordElement(12216))),
					new FC3ReadRegistersTask(12224, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_6_REACTIVE_POWER, new FloatDoublewordElement(12224))),
					new FC3ReadRegistersTask(12232, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_6_APPARENT_POWER, new FloatDoublewordElement(12232))),
					new FC3ReadRegistersTask(12242, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_6_NET_KWH, new UnsignedDoublewordElement(12242))),
					new FC3ReadRegistersTask(12280, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_6_POWER_FACTOR, new FloatDoublewordElement(12280))),
					
					new FC3ReadRegistersTask(12306, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_7_CURRENT, new FloatDoublewordElement(12306))),
					new FC3ReadRegistersTask(12366, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_7_ACTIVE_POWER, new FloatDoublewordElement(12366))),
					new FC3ReadRegistersTask(12374, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_7_REACTIVE_POWER, new FloatDoublewordElement(12374))),
					new FC3ReadRegistersTask(12382, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_7_APPARENT_POWER, new FloatDoublewordElement(12382))),
					new FC3ReadRegistersTask(12392, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_7_NET_KWH, new UnsignedDoublewordElement(12392))),
					new FC3ReadRegistersTask(12430, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_7_POWER_FACTOR, new FloatDoublewordElement(12430))),
					
					new FC3ReadRegistersTask(12456, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_8_CURRENT, new FloatDoublewordElement(12456))),
					new FC3ReadRegistersTask(12516, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_8_ACTIVE_POWER, new FloatDoublewordElement(12516))),
					new FC3ReadRegistersTask(12524, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_8_REACTIVE_POWER, new FloatDoublewordElement(12524))),
					new FC3ReadRegistersTask(12532, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_8_APPARENT_POWER, new FloatDoublewordElement(12532))),
					new FC3ReadRegistersTask(12542, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_8_NET_KWH, new UnsignedDoublewordElement(12542))),
					new FC3ReadRegistersTask(12580, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_8_POWER_FACTOR, new FloatDoublewordElement(12580)))
				);
		}else if(this.config.sensor_num() == 7) {
			mod = new ModbusProtocol(this,					
					new FC3ReadRegistersTask(861, Priority.LOW,	
							m(Accura2300.ChannelId.ACCURA2300_WIRING_MODE, new UnsignedWordElement(861))),
					new FC3ReadRegistersTask(1111, Priority.LOW,	
							m(Accura2300.ChannelId.ACCURA2300_CT_CONNECTED_NUM, new UnsignedWordElement(1111))),
					new FC3ReadRegistersTask(11044, Priority.LOW,	
							m(Accura2300.ChannelId.ACCURA2300_VALIDITY_VOLTAGE_DATA, new UnsignedWordElement(11044)),
							m(Accura2300.ChannelId.ACCURA2300_VALIDITY_CT_DATA, new UnsignedWordElement(11045)),
							m(Accura2300.ChannelId.ACCURA2300_VALIDITY_CT_DATA, new UnsignedWordElement(11046)),
							m(Accura2300.ChannelId.ACCURA2300_VALIDITY_CT_DATA, new UnsignedWordElement(11047)),
							m(Accura2300.ChannelId.ACCURA2300_VALIDITY_CT_DATA, new UnsignedWordElement(11048)),
							m(Accura2300.ChannelId.ACCURA2300_VALIDITY_CT_DATA, new UnsignedWordElement(11049)),
							m(Accura2300.ChannelId.ACCURA2300_VALIDITY_CT_DATA, new UnsignedWordElement(11050)),
							m(Accura2300.ChannelId.ACCURA2300_VALIDITY_CT_DATA, new UnsignedWordElement(11051))),
					
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
					new FC3ReadRegistersTask(11282, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_1_APPARENT_POWER, new FloatDoublewordElement(11282))),
					new FC3ReadRegistersTask(11292, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_1_NET_KWH, new UnsignedDoublewordElement(11292))),
					new FC3ReadRegistersTask(11330, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_1_POWER_FACTOR, new FloatDoublewordElement(11330))),
					
					new FC3ReadRegistersTask(11356, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_CURRENT, new FloatDoublewordElement(11356))),
					new FC3ReadRegistersTask(11416, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_ACTIVE_POWER, new FloatDoublewordElement(11416))),
					new FC3ReadRegistersTask(11424, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_REACTIVE_POWER, new FloatDoublewordElement(11424))),
					new FC3ReadRegistersTask(11432, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_APPARENT_POWER, new FloatDoublewordElement(11432))),
					new FC3ReadRegistersTask(11442, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_NET_KWH, new UnsignedDoublewordElement(11442))),
					new FC3ReadRegistersTask(11480, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_POWER_FACTOR, new FloatDoublewordElement(11480))),
					
					new FC3ReadRegistersTask(11506, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_3_CURRENT, new FloatDoublewordElement(11506))),
					new FC3ReadRegistersTask(11566, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_3_ACTIVE_POWER, new FloatDoublewordElement(11566))),
					new FC3ReadRegistersTask(11574, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_3_REACTIVE_POWER, new FloatDoublewordElement(11574))),
					new FC3ReadRegistersTask(11582, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_3_APPARENT_POWER, new FloatDoublewordElement(11582))),
					new FC3ReadRegistersTask(11592, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_3_NET_KWH, new UnsignedDoublewordElement(11592))),
					new FC3ReadRegistersTask(11630, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_3_POWER_FACTOR, new FloatDoublewordElement(11630))),
					
					new FC3ReadRegistersTask(11657, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_4_CURRENT, new FloatDoublewordElement(11656))),
					new FC3ReadRegistersTask(11716, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_4_ACTIVE_POWER, new FloatDoublewordElement(11716))),
					new FC3ReadRegistersTask(11724, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_4_REACTIVE_POWER, new FloatDoublewordElement(11724))),
					new FC3ReadRegistersTask(11732, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_4_APPARENT_POWER, new FloatDoublewordElement(11732))),
					new FC3ReadRegistersTask(11742, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_4_NET_KWH, new UnsignedDoublewordElement(11742))),
					new FC3ReadRegistersTask(11780, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_4_POWER_FACTOR, new FloatDoublewordElement(11780))),
					
					new FC3ReadRegistersTask(11906, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_5_CURRENT, new FloatDoublewordElement(11906))),
					new FC3ReadRegistersTask(11966, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_5_ACTIVE_POWER, new FloatDoublewordElement(11966))),
					new FC3ReadRegistersTask(11974, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_5_REACTIVE_POWER, new FloatDoublewordElement(11974))),
					new FC3ReadRegistersTask(11982, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_5_APPARENT_POWER, new FloatDoublewordElement(11982))),
					new FC3ReadRegistersTask(11992, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_5_NET_KWH, new UnsignedDoublewordElement(11992))),
					new FC3ReadRegistersTask(12030, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_5_POWER_FACTOR, new FloatDoublewordElement(12030))),
					
					new FC3ReadRegistersTask(12156, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_6_CURRENT, new FloatDoublewordElement(12156))),
					new FC3ReadRegistersTask(12216, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_6_ACTIVE_POWER, new FloatDoublewordElement(12216))),
					new FC3ReadRegistersTask(12224, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_6_REACTIVE_POWER, new FloatDoublewordElement(12224))),
					new FC3ReadRegistersTask(12232, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_6_APPARENT_POWER, new FloatDoublewordElement(12232))),
					new FC3ReadRegistersTask(12242, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_6_NET_KWH, new UnsignedDoublewordElement(12242))),
					new FC3ReadRegistersTask(12280, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_6_POWER_FACTOR, new FloatDoublewordElement(12280))),
					
					new FC3ReadRegistersTask(12306, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_7_CURRENT, new FloatDoublewordElement(12306))),
					new FC3ReadRegistersTask(12366, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_7_ACTIVE_POWER, new FloatDoublewordElement(12366))),
					new FC3ReadRegistersTask(12374, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_7_REACTIVE_POWER, new FloatDoublewordElement(12374))),
					new FC3ReadRegistersTask(12382, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_7_APPARENT_POWER, new FloatDoublewordElement(12382))),
					new FC3ReadRegistersTask(12392, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_7_NET_KWH, new UnsignedDoublewordElement(12392))),
					new FC3ReadRegistersTask(12430, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_7_POWER_FACTOR, new FloatDoublewordElement(12430)))
				);
		}else if(this.config.sensor_num() == 6) {
			mod = new ModbusProtocol(this,					
					new FC3ReadRegistersTask(861, Priority.LOW,	
							m(Accura2300.ChannelId.ACCURA2300_WIRING_MODE, new UnsignedWordElement(861))),
					new FC3ReadRegistersTask(1111, Priority.LOW,	
							m(Accura2300.ChannelId.ACCURA2300_CT_CONNECTED_NUM, new UnsignedWordElement(1111))),
					new FC3ReadRegistersTask(11044, Priority.LOW,	
							m(Accura2300.ChannelId.ACCURA2300_VALIDITY_VOLTAGE_DATA, new UnsignedWordElement(11044)),
							m(Accura2300.ChannelId.ACCURA2300_VALIDITY_CT_DATA, new UnsignedWordElement(11045)),
							m(Accura2300.ChannelId.ACCURA2300_VALIDITY_CT_DATA, new UnsignedWordElement(11046)),
							m(Accura2300.ChannelId.ACCURA2300_VALIDITY_CT_DATA, new UnsignedWordElement(11047)),
							m(Accura2300.ChannelId.ACCURA2300_VALIDITY_CT_DATA, new UnsignedWordElement(11048)),
							m(Accura2300.ChannelId.ACCURA2300_VALIDITY_CT_DATA, new UnsignedWordElement(11049)),
							m(Accura2300.ChannelId.ACCURA2300_VALIDITY_CT_DATA, new UnsignedWordElement(11050))),
					
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
					new FC3ReadRegistersTask(11282, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_1_APPARENT_POWER, new FloatDoublewordElement(11282))),
					new FC3ReadRegistersTask(11292, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_1_NET_KWH, new UnsignedDoublewordElement(11292))),
					new FC3ReadRegistersTask(11330, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_1_POWER_FACTOR, new FloatDoublewordElement(11330))),
					
					new FC3ReadRegistersTask(11356, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_CURRENT, new FloatDoublewordElement(11356))),
					new FC3ReadRegistersTask(11416, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_ACTIVE_POWER, new FloatDoublewordElement(11416))),
					new FC3ReadRegistersTask(11424, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_REACTIVE_POWER, new FloatDoublewordElement(11424))),
					new FC3ReadRegistersTask(11432, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_APPARENT_POWER, new FloatDoublewordElement(11432))),
					new FC3ReadRegistersTask(11442, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_NET_KWH, new UnsignedDoublewordElement(11442))),
					new FC3ReadRegistersTask(11480, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_POWER_FACTOR, new FloatDoublewordElement(11480))),
					
					new FC3ReadRegistersTask(11506, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_3_CURRENT, new FloatDoublewordElement(11506))),
					new FC3ReadRegistersTask(11566, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_3_ACTIVE_POWER, new FloatDoublewordElement(11566))),
					new FC3ReadRegistersTask(11574, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_3_REACTIVE_POWER, new FloatDoublewordElement(11574))),
					new FC3ReadRegistersTask(11582, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_3_APPARENT_POWER, new FloatDoublewordElement(11582))),
					new FC3ReadRegistersTask(11592, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_3_NET_KWH, new UnsignedDoublewordElement(11592))),
					new FC3ReadRegistersTask(11630, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_3_POWER_FACTOR, new FloatDoublewordElement(11630))),
					
					new FC3ReadRegistersTask(11657, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_4_CURRENT, new FloatDoublewordElement(11656))),
					new FC3ReadRegistersTask(11716, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_4_ACTIVE_POWER, new FloatDoublewordElement(11716))),
					new FC3ReadRegistersTask(11724, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_4_REACTIVE_POWER, new FloatDoublewordElement(11724))),
					new FC3ReadRegistersTask(11732, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_4_APPARENT_POWER, new FloatDoublewordElement(11732))),
					new FC3ReadRegistersTask(11742, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_4_NET_KWH, new UnsignedDoublewordElement(11742))),
					new FC3ReadRegistersTask(11780, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_4_POWER_FACTOR, new FloatDoublewordElement(11780))),
					
					new FC3ReadRegistersTask(11906, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_5_CURRENT, new FloatDoublewordElement(11906))),
					new FC3ReadRegistersTask(11966, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_5_ACTIVE_POWER, new FloatDoublewordElement(11966))),
					new FC3ReadRegistersTask(11974, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_5_REACTIVE_POWER, new FloatDoublewordElement(11974))),
					new FC3ReadRegistersTask(11982, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_5_APPARENT_POWER, new FloatDoublewordElement(11982))),
					new FC3ReadRegistersTask(11992, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_5_NET_KWH, new UnsignedDoublewordElement(11992))),
					new FC3ReadRegistersTask(12030, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_5_POWER_FACTOR, new FloatDoublewordElement(12030))),
					
					new FC3ReadRegistersTask(12156, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_6_CURRENT, new FloatDoublewordElement(12156))),
					new FC3ReadRegistersTask(12216, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_6_ACTIVE_POWER, new FloatDoublewordElement(12216))),
					new FC3ReadRegistersTask(12224, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_6_REACTIVE_POWER, new FloatDoublewordElement(12224))),
					new FC3ReadRegistersTask(12232, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_6_APPARENT_POWER, new FloatDoublewordElement(12232))),
					new FC3ReadRegistersTask(12242, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_6_NET_KWH, new UnsignedDoublewordElement(12242))),
					new FC3ReadRegistersTask(12280, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_6_POWER_FACTOR, new FloatDoublewordElement(12280)))
				);
		}else if(this.config.sensor_num() == 5) {
			mod = new ModbusProtocol(this,					
					new FC3ReadRegistersTask(861, Priority.LOW,	
							m(Accura2300.ChannelId.ACCURA2300_WIRING_MODE, new UnsignedWordElement(861))),
					new FC3ReadRegistersTask(1111, Priority.LOW,	
							m(Accura2300.ChannelId.ACCURA2300_CT_CONNECTED_NUM, new UnsignedWordElement(1111))),
					new FC3ReadRegistersTask(11044, Priority.LOW,	
							m(Accura2300.ChannelId.ACCURA2300_VALIDITY_VOLTAGE_DATA, new UnsignedWordElement(11044)),
							m(Accura2300.ChannelId.ACCURA2300_VALIDITY_CT_DATA, new UnsignedWordElement(11045)),
							m(Accura2300.ChannelId.ACCURA2300_VALIDITY_CT_DATA, new UnsignedWordElement(11046)),
							m(Accura2300.ChannelId.ACCURA2300_VALIDITY_CT_DATA, new UnsignedWordElement(11047)),
							m(Accura2300.ChannelId.ACCURA2300_VALIDITY_CT_DATA, new UnsignedWordElement(11048)),
							m(Accura2300.ChannelId.ACCURA2300_VALIDITY_CT_DATA, new UnsignedWordElement(11049))),
					
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
					new FC3ReadRegistersTask(11282, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_1_APPARENT_POWER, new FloatDoublewordElement(11282))),
					new FC3ReadRegistersTask(11292, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_1_NET_KWH, new UnsignedDoublewordElement(11292))),
					new FC3ReadRegistersTask(11330, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_1_POWER_FACTOR, new FloatDoublewordElement(11330))),
					
					new FC3ReadRegistersTask(11356, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_CURRENT, new FloatDoublewordElement(11356))),
					new FC3ReadRegistersTask(11416, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_ACTIVE_POWER, new FloatDoublewordElement(11416))),
					new FC3ReadRegistersTask(11424, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_REACTIVE_POWER, new FloatDoublewordElement(11424))),
					new FC3ReadRegistersTask(11432, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_APPARENT_POWER, new FloatDoublewordElement(11432))),
					new FC3ReadRegistersTask(11442, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_NET_KWH, new UnsignedDoublewordElement(11442))),
					new FC3ReadRegistersTask(11480, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_POWER_FACTOR, new FloatDoublewordElement(11480))),
					
					new FC3ReadRegistersTask(11506, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_3_CURRENT, new FloatDoublewordElement(11506))),
					new FC3ReadRegistersTask(11566, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_3_ACTIVE_POWER, new FloatDoublewordElement(11566))),
					new FC3ReadRegistersTask(11574, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_3_REACTIVE_POWER, new FloatDoublewordElement(11574))),
					new FC3ReadRegistersTask(11582, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_3_APPARENT_POWER, new FloatDoublewordElement(11582))),
					new FC3ReadRegistersTask(11592, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_3_NET_KWH, new UnsignedDoublewordElement(11592))),
					new FC3ReadRegistersTask(11630, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_3_POWER_FACTOR, new FloatDoublewordElement(11630))),
					
					new FC3ReadRegistersTask(11657, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_4_CURRENT, new FloatDoublewordElement(11656))),
					new FC3ReadRegistersTask(11716, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_4_ACTIVE_POWER, new FloatDoublewordElement(11716))),
					new FC3ReadRegistersTask(11724, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_4_REACTIVE_POWER, new FloatDoublewordElement(11724))),
					new FC3ReadRegistersTask(11732, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_4_APPARENT_POWER, new FloatDoublewordElement(11732))),
					new FC3ReadRegistersTask(11742, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_4_NET_KWH, new UnsignedDoublewordElement(11742))),
					new FC3ReadRegistersTask(11780, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_4_POWER_FACTOR, new FloatDoublewordElement(11780))),
					
					new FC3ReadRegistersTask(11906, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_5_CURRENT, new FloatDoublewordElement(11906))),
					new FC3ReadRegistersTask(11966, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_5_ACTIVE_POWER, new FloatDoublewordElement(11966))),
					new FC3ReadRegistersTask(11974, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_5_REACTIVE_POWER, new FloatDoublewordElement(11974))),
					new FC3ReadRegistersTask(11982, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_5_APPARENT_POWER, new FloatDoublewordElement(11982))),
					new FC3ReadRegistersTask(11992, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_5_NET_KWH, new UnsignedDoublewordElement(11992))),
					new FC3ReadRegistersTask(12030, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_5_POWER_FACTOR, new FloatDoublewordElement(12030)))
				);
		}else if(this.config.sensor_num() == 4) {
			mod = new ModbusProtocol(this,					
					new FC3ReadRegistersTask(861, Priority.LOW,	
							m(Accura2300.ChannelId.ACCURA2300_WIRING_MODE, new UnsignedWordElement(861))),
					new FC3ReadRegistersTask(1111, Priority.LOW,	
							m(Accura2300.ChannelId.ACCURA2300_CT_CONNECTED_NUM, new UnsignedWordElement(1111))),
					new FC3ReadRegistersTask(11044, Priority.LOW,	
							m(Accura2300.ChannelId.ACCURA2300_VALIDITY_VOLTAGE_DATA, new UnsignedWordElement(11044)),
							m(Accura2300.ChannelId.ACCURA2300_VALIDITY_CT_DATA, new UnsignedWordElement(11045)),
							m(Accura2300.ChannelId.ACCURA2300_VALIDITY_CT_DATA, new UnsignedWordElement(11046)),
							m(Accura2300.ChannelId.ACCURA2300_VALIDITY_CT_DATA, new UnsignedWordElement(11047)),
							m(Accura2300.ChannelId.ACCURA2300_VALIDITY_CT_DATA, new UnsignedWordElement(11048))),
					
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
					new FC3ReadRegistersTask(11282, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_1_APPARENT_POWER, new FloatDoublewordElement(11282))),
					new FC3ReadRegistersTask(11292, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_1_NET_KWH, new UnsignedDoublewordElement(11292))),
					new FC3ReadRegistersTask(11330, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_1_POWER_FACTOR, new FloatDoublewordElement(11330))),
					
					new FC3ReadRegistersTask(11356, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_CURRENT, new FloatDoublewordElement(11356))),
					new FC3ReadRegistersTask(11416, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_ACTIVE_POWER, new FloatDoublewordElement(11416))),
					new FC3ReadRegistersTask(11424, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_REACTIVE_POWER, new FloatDoublewordElement(11424))),
					new FC3ReadRegistersTask(11432, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_APPARENT_POWER, new FloatDoublewordElement(11432))),
					new FC3ReadRegistersTask(11442, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_NET_KWH, new UnsignedDoublewordElement(11442))),
					new FC3ReadRegistersTask(11480, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_POWER_FACTOR, new FloatDoublewordElement(11480))),
					
					new FC3ReadRegistersTask(11506, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_3_CURRENT, new FloatDoublewordElement(11506))),
					new FC3ReadRegistersTask(11566, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_3_ACTIVE_POWER, new FloatDoublewordElement(11566))),
					new FC3ReadRegistersTask(11574, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_3_REACTIVE_POWER, new FloatDoublewordElement(11574))),
					new FC3ReadRegistersTask(11582, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_3_APPARENT_POWER, new FloatDoublewordElement(11582))),
					new FC3ReadRegistersTask(11592, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_3_NET_KWH, new UnsignedDoublewordElement(11592))),
					new FC3ReadRegistersTask(11630, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_3_POWER_FACTOR, new FloatDoublewordElement(11630))),
					
					new FC3ReadRegistersTask(11657, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_4_CURRENT, new FloatDoublewordElement(11656))),
					new FC3ReadRegistersTask(11716, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_4_ACTIVE_POWER, new FloatDoublewordElement(11716))),
					new FC3ReadRegistersTask(11724, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_4_REACTIVE_POWER, new FloatDoublewordElement(11724))),
					new FC3ReadRegistersTask(11732, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_4_APPARENT_POWER, new FloatDoublewordElement(11732))),
					new FC3ReadRegistersTask(11742, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_4_NET_KWH, new UnsignedDoublewordElement(11742))),
					new FC3ReadRegistersTask(11780, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_4_POWER_FACTOR, new FloatDoublewordElement(11780)))
				);
		}else if(this.config.sensor_num() == 3) {
			mod = new ModbusProtocol(this,					
					new FC3ReadRegistersTask(861, Priority.LOW,	
							m(Accura2300.ChannelId.ACCURA2300_WIRING_MODE, new UnsignedWordElement(861))),
					new FC3ReadRegistersTask(1111, Priority.LOW,	
							m(Accura2300.ChannelId.ACCURA2300_CT_CONNECTED_NUM, new UnsignedWordElement(1111))),
					new FC3ReadRegistersTask(11044, Priority.LOW,	
							m(Accura2300.ChannelId.ACCURA2300_VALIDITY_VOLTAGE_DATA, new UnsignedWordElement(11044)),
							m(Accura2300.ChannelId.ACCURA2300_VALIDITY_CT_DATA, new UnsignedWordElement(11045)),
							m(Accura2300.ChannelId.ACCURA2300_VALIDITY_CT_DATA, new UnsignedWordElement(11046)),
							m(Accura2300.ChannelId.ACCURA2300_VALIDITY_CT_DATA, new UnsignedWordElement(11047))),
					
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
					new FC3ReadRegistersTask(11282, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_1_APPARENT_POWER, new FloatDoublewordElement(11282))),
					new FC3ReadRegistersTask(11292, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_1_NET_KWH, new UnsignedDoublewordElement(11292))),
					new FC3ReadRegistersTask(11330, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_1_POWER_FACTOR, new FloatDoublewordElement(11330))),
					
					new FC3ReadRegistersTask(11356, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_CURRENT, new FloatDoublewordElement(11356))),
					new FC3ReadRegistersTask(11416, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_ACTIVE_POWER, new FloatDoublewordElement(11416))),
					new FC3ReadRegistersTask(11424, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_REACTIVE_POWER, new FloatDoublewordElement(11424))),
					new FC3ReadRegistersTask(11432, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_APPARENT_POWER, new FloatDoublewordElement(11432))),
					new FC3ReadRegistersTask(11442, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_NET_KWH, new UnsignedDoublewordElement(11442))),
					new FC3ReadRegistersTask(11480, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_POWER_FACTOR, new FloatDoublewordElement(11480))),
					
					new FC3ReadRegistersTask(11506, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_3_CURRENT, new FloatDoublewordElement(11506))),
					new FC3ReadRegistersTask(11566, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_3_ACTIVE_POWER, new FloatDoublewordElement(11566))),
					new FC3ReadRegistersTask(11574, Priority.LOW, //
						m(Accura2300.ChannelId.ACCURA2350_3_REACTIVE_POWER, new FloatDoublewordElement(11574))),
					new FC3ReadRegistersTask(11582, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_3_APPARENT_POWER, new FloatDoublewordElement(11582))),
					new FC3ReadRegistersTask(11592, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_3_NET_KWH, new UnsignedDoublewordElement(11592))),
					new FC3ReadRegistersTask(11630, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_3_POWER_FACTOR, new FloatDoublewordElement(11630)))
				);
		} else if(this.config.sensor_num() == 2) {
			mod = new ModbusProtocol(this,				
					new FC3ReadRegistersTask(861, Priority.LOW,	
							m(Accura2300.ChannelId.ACCURA2300_WIRING_MODE, new UnsignedWordElement(861))),
					new FC3ReadRegistersTask(1111, Priority.LOW,	
							m(Accura2300.ChannelId.ACCURA2300_CT_CONNECTED_NUM, new UnsignedWordElement(1111))),
					new FC3ReadRegistersTask(11044, Priority.LOW,	
							m(Accura2300.ChannelId.ACCURA2300_VALIDITY_VOLTAGE_DATA, new UnsignedWordElement(11044)),
							m(Accura2300.ChannelId.ACCURA2300_VALIDITY_CT_DATA, new UnsignedWordElement(11045)),
							m(Accura2300.ChannelId.ACCURA2300_VALIDITY_CT_DATA, new UnsignedWordElement(11046))),
					
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
					new FC3ReadRegistersTask(11282, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_1_APPARENT_POWER, new FloatDoublewordElement(11282))),
					new FC3ReadRegistersTask(11292, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_1_NET_KWH, new UnsignedDoublewordElement(11292))),
					new FC3ReadRegistersTask(11330, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_1_POWER_FACTOR, new FloatDoublewordElement(11330))),
					
					new FC3ReadRegistersTask(11356, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_CURRENT, new FloatDoublewordElement(11356))),
					new FC3ReadRegistersTask(11416, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_ACTIVE_POWER, new FloatDoublewordElement(11416))),
					new FC3ReadRegistersTask(11424, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_REACTIVE_POWER, new FloatDoublewordElement(11424))),
					new FC3ReadRegistersTask(11432, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_APPARENT_POWER, new FloatDoublewordElement(11432))),
					new FC3ReadRegistersTask(11442, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_NET_KWH, new UnsignedDoublewordElement(11442))),
					new FC3ReadRegistersTask(11480, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_2_POWER_FACTOR, new FloatDoublewordElement(11480)))
				);
		} else {		
		// sensor_num == 1  일때 시
		// 이 방식은 데이터 종류별로 묶어서 컨넥션 횟수를 줄일 수 있
			mod =  new ModbusProtocol(this,
					new FC3ReadRegistersTask(861, Priority.LOW,	
							m(Accura2300.ChannelId.ACCURA2300_WIRING_MODE, new UnsignedWordElement(861))),
					new FC3ReadRegistersTask(1111, Priority.LOW,	
							m(Accura2300.ChannelId.ACCURA2300_CT_CONNECTED_NUM, new UnsignedWordElement(1111))),
					new FC3ReadRegistersTask(11044, Priority.LOW,	
							m(Accura2300.ChannelId.ACCURA2300_VALIDITY_VOLTAGE_DATA, new UnsignedWordElement(11044)),
							m(Accura2300.ChannelId.ACCURA2300_VALIDITY_CT_DATA, new UnsignedWordElement(11045))),
					
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
						m(Accura2300.ChannelId.ACCURA2350_1_REACTIVE_POWER, new FloatDoublewordElement(11274))),
					new FC3ReadRegistersTask(11282, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_1_APPARENT_POWER, new FloatDoublewordElement(11282))),
					new FC3ReadRegistersTask(11292, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_1_NET_KWH, new UnsignedDoublewordElement(11292))),
					new FC3ReadRegistersTask(11330, Priority.LOW,
						m(Accura2300.ChannelId.ACCURA2350_1_POWER_FACTOR, new FloatDoublewordElement(11330)))
				);
		}
		
		return mod; 
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
