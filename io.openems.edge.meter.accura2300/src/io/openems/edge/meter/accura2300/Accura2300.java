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
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;  // uint32
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.meter.api.AsymmetricMeter;
import io.openems.edge.meter.api.EDfemsMeter;
import io.openems.edge.meter.api.MeterType;
import io.openems.edge.meter.api.SymmetricMeter;
import io.openems.edge.meter.api.EDfemsMeter.ChannelId;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Meter.Accura2300", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class Accura2300 extends AbstractOpenemsModbusComponent 
	implements SymmetricMeter, AsymmetricMeter, OpenemsComponent {

	private Config config = null;

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {		
		
		ACCURA2350_1_CURRENT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIAMPERE) //
				.text(POWER_DOC_TEXT)),		
		ACCURA2350_1_ACTIVE_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.text(POWER_DOC_TEXT)),				
		ACCURA2350_1_REACTIVE_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT_AMPERE_REACTIVE) //
				.text(POWER_DOC_TEXT)),
		ACCURA2350_2_CURRENT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIAMPERE) //
				.text(POWER_DOC_TEXT)),		
		ACCURA2350_2_ACTIVE_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.text(POWER_DOC_TEXT)),				
		ACCURA2350_2_REACTIVE_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT_AMPERE_REACTIVE) //
				.text(POWER_DOC_TEXT)),
		ACCURA2350_3_CURRENT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIAMPERE) //
				.text(POWER_DOC_TEXT)),		
		ACCURA2350_3_ACTIVE_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.text(POWER_DOC_TEXT)),				
		ACCURA2350_3_REACTIVE_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT_AMPERE_REACTIVE) //
				.text(POWER_DOC_TEXT));

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
	protected ModbusProtocol defineModbusProtocol() {
		// TODO implement ModbusProtocol
			
		ModbusProtocol mod = null;
		
//		System.out.println( 
		
//	    this.config.sensor_num();
//		this.getSensorNum();
		if(this.config.sensor_num() == 3) {
			mod = new ModbusProtocol(this,					
					new FC3ReadRegistersTask(11123, Priority.LOW,
							m(new UnsignedDoublewordElement(11123))
//								.m(ChannelId.ACCURA2350_1_CURRENT, ElementToChannelConverter.SCALE_FACTOR_MINUS_3 )
								.m(AsymmetricMeter.ChannelId.VOLTAGE_L1, ElementToChannelConverter.SCALE_FACTOR_MINUS_3) //
								.m(SymmetricMeter.ChannelId.VOLTAGE, ElementToChannelConverter.SCALE_FACTOR_MINUS_3 )
								.build(),
							m(SymmetricMeter.ChannelId.FREQUENCY, new UnsignedDoublewordElement(11151),      // 0x11125),
									ElementToChannelConverter.SCALE_FACTOR_MINUS_3),
							// 주소 11201 Unit ID 0 시작 주소
							// 모든상의 전류의 평균 11207
							m(Accura2300.ChannelId.ACCURA2350_1_CURRENT, new UnsignedDoublewordElement(11207),
									ElementToChannelConverter.SCALE_FACTOR_MINUS_3),
							// 11267  유효전력[kW]
							m(Accura2300.ChannelId.ACCURA2350_1_ACTIVE_POWER, new UnsignedDoublewordElement(11267),
									ElementToChannelConverter.SCALE_FACTOR_MINUS_3),
							// 11275  무효전력[KVAR total]
							m(Accura2300.ChannelId.ACCURA2350_1_REACTIVE_POWER, new UnsignedDoublewordElement(11275),
									ElementToChannelConverter.SCALE_FACTOR_MINUS_3)
							),
					// 주소 11351 Unit ID 1 시작 주소 
					new FC3ReadRegistersTask(11357, Priority.LOW, //
							// 모든상의 전류의 평균 11357
							m(Accura2300.ChannelId.ACCURA2350_2_CURRENT, new UnsignedDoublewordElement(11357),
									ElementToChannelConverter.SCALE_FACTOR_MINUS_3),
							// 11417  유효전력[KW total]
							m(Accura2300.ChannelId.ACCURA2350_2_ACTIVE_POWER, new UnsignedDoublewordElement(11417),
									ElementToChannelConverter.SCALE_FACTOR_MINUS_3),
							// 11425  무효전력[KVAR total]
							m(Accura2300.ChannelId.ACCURA2350_2_REACTIVE_POWER, new UnsignedDoublewordElement(11425),
									ElementToChannelConverter.SCALE_FACTOR_MINUS_3)
					),
					// 주소 11501 Unit ID 2 시작 주소 
					new FC3ReadRegistersTask(11507, Priority.LOW, //
							// 모든상의 전류의 평균 11507
							m(Accura2300.ChannelId.ACCURA2350_3_CURRENT, new UnsignedDoublewordElement(11507),
									ElementToChannelConverter.SCALE_FACTOR_MINUS_3),
							// 11567  유효전력[KW total]
							m(Accura2300.ChannelId.ACCURA2350_3_ACTIVE_POWER, new UnsignedDoublewordElement(11567),
									ElementToChannelConverter.SCALE_FACTOR_MINUS_3),
							// 11575  무효전력[KVAR total]
							m(Accura2300.ChannelId.ACCURA2350_3_REACTIVE_POWER, new UnsignedDoublewordElement(11575),
									ElementToChannelConverter.SCALE_FACTOR_MINUS_3)
					)
				);
		} else if(this.config.sensor_num() == 2) {
			mod = new ModbusProtocol(this,					
					new FC3ReadRegistersTask(11123, Priority.LOW,
							m(new UnsignedDoublewordElement(11123))
//								.m(ChannelId.ACCURA2350_1_CURRENT, ElementToChannelConverter.SCALE_FACTOR_MINUS_3 )
								.m(AsymmetricMeter.ChannelId.VOLTAGE_L1, ElementToChannelConverter.SCALE_FACTOR_MINUS_3) //
								.m(SymmetricMeter.ChannelId.VOLTAGE, ElementToChannelConverter.SCALE_FACTOR_MINUS_3 )
								.build(),
							m(SymmetricMeter.ChannelId.FREQUENCY, new UnsignedDoublewordElement(11151),      // 0x11125),
									ElementToChannelConverter.SCALE_FACTOR_MINUS_3),
							// 주소 11201 Unit ID 0 시작 주소
							// 모든상의 전류의 평균 11207
							m(Accura2300.ChannelId.ACCURA2350_1_CURRENT, new UnsignedDoublewordElement(11207),
									ElementToChannelConverter.SCALE_FACTOR_MINUS_3),
							// 11267  유효전력[kW]
							m(Accura2300.ChannelId.ACCURA2350_1_ACTIVE_POWER, new UnsignedDoublewordElement(11267),
									ElementToChannelConverter.SCALE_FACTOR_MINUS_3),
							// 11275  무효전력[KVAR total]
							m(Accura2300.ChannelId.ACCURA2350_1_REACTIVE_POWER, new UnsignedDoublewordElement(11275),
									ElementToChannelConverter.SCALE_FACTOR_MINUS_3)
							),
					// 주소 11351 Unit ID 1 시작 주소 
					new FC3ReadRegistersTask(11357, Priority.LOW, //
							// 모든상의 전류의 평균 11357
							m(Accura2300.ChannelId.ACCURA2350_2_CURRENT, new UnsignedDoublewordElement(11357),
									ElementToChannelConverter.SCALE_FACTOR_MINUS_3),
							// 11417  유효전력[KW total]
							m(Accura2300.ChannelId.ACCURA2350_2_ACTIVE_POWER, new UnsignedDoublewordElement(11417),
									ElementToChannelConverter.SCALE_FACTOR_MINUS_3),
							// 11425  무효전력[KVAR total]
							m(Accura2300.ChannelId.ACCURA2350_2_REACTIVE_POWER, new UnsignedDoublewordElement(11425),
									ElementToChannelConverter.SCALE_FACTOR_MINUS_3)
					)
				);
		} else {		
		// sensor_num == 1  일때 시
		// 이 방식은 데이터 종류별로 묶어서 컨넥션 횟수를 줄일 수 있
			mod =  new ModbusProtocol(this,
					// Voltage Data of Accura 2300[S]
					new FC3ReadRegistersTask(11123, Priority.LOW,
						// 11123  Voltage Vavg1  Float32  PR   삼상 전압의 기본파 성 평균 단위[V]
						m(new SignedDoublewordElement(11123))
						    .m(AsymmetricMeter.ChannelId.VOLTAGE_L1, ElementToChannelConverter.SCALE_FACTOR_MINUS_3)
							.m(SymmetricMeter.ChannelId.VOLTAGE, ElementToChannelConverter.SCALE_FACTOR_MINUS_3 )
							.build(),
					    // 11151  Frequency  Float32  PR  입력 전압 주파수. 단[Hz] 		
						m(SymmetricMeter.ChannelId.FREQUENCY, new SignedDoublewordElement(11151),      // 0x11125),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_3),
						// subunit   모듈 기기(센싱  기기)
						// Module ID 0의 시작 Number는 11201   // Module ID === Sensor Num (Accura2350)
						// 기본값으로 Accura 2350  1개가 설치됐다고 가정 
						// 모든상의 전류의 평균 11207
						m(Accura2300.ChannelId.ACCURA2350_1_CURRENT, new UnsignedDoublewordElement(11207),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_3),
						// 11267  유효전력[kW]
						m(Accura2300.ChannelId.ACCURA2350_1_ACTIVE_POWER, new UnsignedDoublewordElement(11267),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_3),
						// 11275  무효전력[KVAR total]
						m(Accura2300.ChannelId.ACCURA2350_1_REACTIVE_POWER, new UnsignedDoublewordElement(11275),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_3)
						)
				);
		}
		
		return mod;
		
//		11267  유효전력[kW]    11275  무효전력[kVAR]     11283 피상전력[kVA]      11285 누설전류[A]
//		11313	삼상의 유효전력 예측[kW]  11323 삼상의 전류 예측[A]		11331  역률 
	}

	@Override
	public String debugLog() {
		System.out.println("센서 갯 : " + this.config.sensor_num());
		
		return "L:" + this.getVoltage().value().asString() + " / "
				+ this.getFrequency().value().asString() + " / "
				+ this.getCurrent().value().asString() + " / "
				+ this.getActivePower().value().asString() + " / "
				+ this.getReactivePower().value().asString() + " / "
				+ this.getAccura2350Current01().value().asString() + " / "
				+ this.getAccura2350Active01().value().asString() + " / "
				+ this.getAccura2350Reactive01().value().asString() + " / "
				+ this.getAccura2350Current02().value().asString() + " / "
				+ this.getAccura2350Active02().value().asString() + " / "
				+ this.getAccura2350Reactive02().value().asString() + " / "
				+ this.getAccura2350Current03().value().asString() + " / "
				+ this.getAccura2350Active03().value().asString() + " / "
				+ this.getAccura2350Reactive03().value().asString() + " / ";
	}
	
	public Channel<Integer> getAccura2350Current01() {
		return this.channel(ChannelId.ACCURA2350_1_CURRENT);
	}
	
	public Channel<Integer> getAccura2350Active01() {
		return this.channel(ChannelId.ACCURA2350_1_ACTIVE_POWER);
	}
	
	public Channel<Integer> getAccura2350Reactive01() {
		return this.channel(ChannelId.ACCURA2350_1_REACTIVE_POWER);
	}
	
	public Channel<Integer> getAccura2350Current02() {
		return this.channel(ChannelId.ACCURA2350_2_CURRENT);
	}
	
	public Channel<Integer> getAccura2350Active02() {
		return this.channel(ChannelId.ACCURA2350_2_ACTIVE_POWER);
	}
	
	public Channel<Integer> getAccura2350Reactive02() {
		return this.channel(ChannelId.ACCURA2350_2_REACTIVE_POWER);
	}
	
	public Channel<Integer> getAccura2350Current03() {
		return this.channel(ChannelId.ACCURA2350_3_CURRENT);
	}
	
	public Channel<Integer> getAccura2350Active03() {
		return this.channel(ChannelId.ACCURA2350_3_ACTIVE_POWER);
	}
	
	public Channel<Integer> getAccura2350Reactive03() {
		return this.channel(ChannelId.ACCURA2350_3_REACTIVE_POWER);
	}

	@Override
	public MeterType getMeterType() {
		// TODO Auto-generated method stub
		return this.config.type();
	}
}
