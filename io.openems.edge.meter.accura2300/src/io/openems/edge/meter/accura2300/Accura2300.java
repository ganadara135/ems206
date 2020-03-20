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
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;	 // uint16
import io.openems.edge.bridge.modbus.api.element.DummyRegisterElement;
import io.openems.edge.bridge.modbus.api.element.SignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;  // uint32
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.meter.api.AsymmetricMeter;
import io.openems.edge.meter.api.MeterType;
import io.openems.edge.meter.api.SymmetricMeter;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Meter.Accura2300", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class Accura2300 extends AbstractOpenemsModbusComponent implements SymmetricMeter, AsymmetricMeter, OpenemsComponent {

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
				AsymmetricMeter.ChannelId.values(), //
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
//				new FC3ReadRegistersTask(0, Priority.HIGH,
//						m(SymmetricMeter.ChannelId.ACTIVE_POWER, new UnsignedWordElement(0))),
//				new FC3ReadRegistersTask(1, Priority.HIGH, 
//						m(SymmetricMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY, new UnsignedDoublewordElement(2)) ));
		// 이 방식은 데이터 종류별로 묶어서 컨넥션 횟수를 줄일 수 있
		return new ModbusProtocol(this, //
				// Voltage Data of Accura 2300[S]
				new FC3ReadRegistersTask(0x11123, Priority.HIGH,
						// 11123  Voltage Vavg1  Float32  PR   삼상 전압의 기본파 성 평균 단위[V]
						m(new UnsignedDoublewordElement(0x11123))
							.m(SymmetricMeter.ChannelId.VOLTAGE, ElementToChannelConverter.SCALE_FACTOR_MINUS_3 )
							.build(),
					    // 11151  Frequency  Float32  PR  입력 전압 주파수. 단[Hz] 		
						m(SymmetricMeter.ChannelId.FREQUENCY, new UnsignedDoublewordElement(0x11151),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_3),
						// subunit   모듈 기기(센싱  기기)
						// Module ID 0의 시작 Number는 11201
						// 11207  Current  Float32  PR   Current Average (CT Measured Data). 단위 [A] 		
						m(SymmetricMeter.ChannelId.CURRENT, new UnsignedDoublewordElement(0x11207),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_3)));
	

		// 초기 설정 방식 : 단점 - new() 할 때마다 송신 발생, 즉, 아래 방식은 4번의 컨넥션 발생,  
//		return new ModbusProtocol(this, 
//				new FC3ReadRegistersTask(1, Priority.HIGH, 
//						m(SymmetricMeter.ChannelId.ACTIVE_POWER, new SignedWordElement(1))),
//				new FC3ReadRegistersTask(1001, Priority.HIGH, 
//						m(SymmetricMeter.ChannelId.VOLTAGE, new SignedWordElement(1001))),
//				new FC3ReadRegistersTask(1001, Priority.HIGH, 
//						m(SymmetricMeter.ChannelId.REACTIVE_POWER, new SignedWordElement(1001))),
//				new FC3ReadRegistersTask(2000, Priority.HIGH, 
//						m(SymmetricMeter.ChannelId.FREQUENCY, new SignedWordElement(2000))));
	}

	@Override
	public String debugLog() {
		System.out.println("aaaaaa  aaaaaaaa   aaaaaaaaaaa");
		
		return "L:" + this.getVoltage().value().asString() + " / "
				+ this.getFrequency().value().asString() + " / "
				+ this.getCurrent().value().asString() + " / ";
		
//		return "L:" + this.getActivePower().value().asString() + " / " 
//				+ this.getActiveConsumptionEnergy().value().asString() + " / " ;
//				+ this.getReactivePower().value().asString() + " / " 
//				+ this.getFrequency().value().asString();

	}

	@Override
	public MeterType getMeterType() {
		// TODO Auto-generated method stub
		return this.config.type();
	}
}
