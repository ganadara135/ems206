package io.openems.edge.meter.realsyscnv;

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
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.FloatDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.SignedWordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedQuadruplewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC4ReadInputRegistersTask;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.meter.api.MeterType;
import io.openems.edge.meter.api.SymmetricMeter;


@Designate(ocd = Config.class, factory = true) 
@Component(
		name = "Meter.RealsysConverter", 
		immediate = true, 
		configurationPolicy = ConfigurationPolicy.REQUIRE 
)
public class RealsysCnv extends AbstractOpenemsModbusComponent 
	implements SymmetricMeter, OpenemsComponent { 

	private Config config = null;

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId { 
		MASS_FLOW_RATE(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.NONE)),
		VOLUME_FLOW_RATE(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.NONE)), //
		ENERGY_FLOW_RATE(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.NONE)),		
		NORMAL_FLOW_RATE(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.NONE)),
		PRESSURE(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.NONE)),
		TOTAL_MASS_FLOW(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.NONE)),
		TOTAL_VOLUME_FLOW(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.NONE)), //
		TOTAL_ENERGY_FLOW(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.NONE)),		
		TOTAL_NORMAL_FLOW(Doc.of(OpenemsType.FLOAT) //
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

	public RealsysCnv() {
		super(
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
//		0x107C PRESSURE kPa 소수2자리 4byte  
//		0x1080 TOTAL MASS FLOW kg
//		0x1084 TOTAL VOLUME FLOW m3
//		0x1088 TOTAL ENERGY FLOW MJ
//		0x108C TOTAL NORMAL FLOW Nm3 
		return new ModbusProtocol(this,
				new FC4ReadInputRegistersTask(0x106C, Priority.LOW,
						m(RealsysCnv.ChannelId.MASS_FLOW_RATE, new FloatDoublewordElement(0x106C))),
				new FC4ReadInputRegistersTask(0x1070, Priority.LOW,
						m(RealsysCnv.ChannelId.VOLUME_FLOW_RATE, new FloatDoublewordElement(0x1070))),
				new FC4ReadInputRegistersTask(0x1074, Priority.LOW,
						m(RealsysCnv.ChannelId.ENERGY_FLOW_RATE, new FloatDoublewordElement(0x1074))),
				new FC4ReadInputRegistersTask(0x1078, Priority.LOW,
						m(RealsysCnv.ChannelId.NORMAL_FLOW_RATE, new FloatDoublewordElement(0x1078))),
				
				new FC4ReadInputRegistersTask(0x107C, Priority.LOW,
						m(RealsysCnv.ChannelId.PRESSURE, new FloatDoublewordElement(0x107C))),
				new FC4ReadInputRegistersTask(0x1080, Priority.LOW,
						m(RealsysCnv.ChannelId.TOTAL_MASS_FLOW, new FloatDoublewordElement(0x1080))),
				new FC4ReadInputRegistersTask(0x1084, Priority.LOW,
						m(RealsysCnv.ChannelId.TOTAL_VOLUME_FLOW, new FloatDoublewordElement(0x1084))),
				new FC4ReadInputRegistersTask(0x1088, Priority.LOW,
						m(RealsysCnv.ChannelId.TOTAL_ENERGY_FLOW, new FloatDoublewordElement(0x1088))),								
				new FC4ReadInputRegistersTask(0x108C, Priority.LOW,
						m(RealsysCnv.ChannelId.TOTAL_NORMAL_FLOW, new FloatDoublewordElement(0x108C)))						
				);
	}
	
	Channel<Float> getMassFlowRate() {
		return this.channel(ChannelId.MASS_FLOW_RATE);
	}	
	Channel<Float> getVolumeFlowRate() {
		return this.channel(ChannelId.VOLUME_FLOW_RATE);
	}	
	Channel<Float> getEnergyFowRate() {
		return this.channel(ChannelId.ENERGY_FLOW_RATE);
	}	
	Channel<Float> getNormalFlowRate() {
		return this.channel(ChannelId.NORMAL_FLOW_RATE);
	}	
	Channel<Float> getPressure() {
		return this.channel(ChannelId.PRESSURE);
	}	
	Channel<Float> getTotalMassFlow() {
		return this.channel(ChannelId.TOTAL_MASS_FLOW);
	}	
	Channel<Float> getTotalVolumeFlow() {
		return this.channel(ChannelId.TOTAL_VOLUME_FLOW);
	}	
	Channel<Float> getTotalEnergyFow() {
		return this.channel(ChannelId.TOTAL_ENERGY_FLOW);
	}	
	Channel<Float> getTotalNormalFlow() {
		return this.channel(ChannelId.TOTAL_NORMAL_FLOW);
	}	

	@Override
	public MeterType getMeterType() { 
		return this.config.type();
	}
	@Override
	public String debugLog() {
		return "L:" + this.getNormalFlowRate().value().asString() + " / "
				+ this.getTotalNormalFlow().value().asString() + " / "
				+ this.getMassFlowRate().value().asString() + " / "
				+ this.getEnergyFowRate().value().asString() + " / "
				+ this.getVolumeFlowRate().value().asString();
	}
}
