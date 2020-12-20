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
import io.openems.edge.bridge.modbus.api.element.DummyRegisterElement;
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
@Component(//
		name = "Meter.PLCofPnt", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class PlcOfPnt extends AbstractOpenemsModbusComponent 
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
		
		TEMP_CNT_PV_1(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE)),
		TEMP_CNT_SV_1(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE)),
		TEMP_CNT_PV_2(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE)),
		TEMP_CNT_SV_2(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE)),
		
		DRY_AP1_1(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.NONE)),
		DRY_RTD_TEMP_1(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.NONE)),
		DRY_NM_FLOW_1(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.NONE)),
		DRY_TOTAL_NM_FLOW_1(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.NONE)),
		DRY_AP1_2(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.NONE)),
		DRY_RTD_TEMP_2(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.NONE)),
		DRY_NM_FLOW_2(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.NONE)),
		DRY_TOTAL_NM_FLOW_2(Doc.of(OpenemsType.FLOAT) //
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
			new FC4ReadInputRegistersTask(0, Priority.LOW,
					m(PlcOfPnt.ChannelId.GAS_USAGE_1, new UnsignedWordElement(0))),
			new FC4ReadInputRegistersTask(10, Priority.LOW,
					m(PlcOfPnt.ChannelId.GAS_USAGE_2, new UnsignedWordElement(10))),
			new FC4ReadInputRegistersTask(20, Priority.LOW,	
					m(PlcOfPnt.ChannelId.GAS_USAGE_3, new UnsignedWordElement(20))),
			new FC4ReadInputRegistersTask(30, Priority.LOW,
					m(PlcOfPnt.ChannelId.GAS_USAGE_4, new UnsignedWordElement(30))),
			new FC4ReadInputRegistersTask(40, Priority.LOW,
					m(PlcOfPnt.ChannelId.GAS_USAGE_5, new UnsignedWordElement(40))),
			new FC4ReadInputRegistersTask(50, Priority.LOW,
					m(PlcOfPnt.ChannelId.GAS_USAGE_6, new UnsignedWordElement(50))),
			new FC4ReadInputRegistersTask(60, Priority.LOW,
					m(PlcOfPnt.ChannelId.GAS_USAGE_7, new UnsignedWordElement(60))),
			
			new FC4ReadInputRegistersTask(71, Priority.LOW,
					m(PlcOfPnt.ChannelId.DRY_AP1_1, new FloatDoublewordElement(71)),
					m(PlcOfPnt.ChannelId.DRY_RTD_TEMP_1, new FloatDoublewordElement(73)),
					m(PlcOfPnt.ChannelId.DRY_NM_FLOW_1, new FloatDoublewordElement(75)),
					m(PlcOfPnt.ChannelId.DRY_TOTAL_NM_FLOW_1, new FloatDoublewordElement(77))),
			
			new FC4ReadInputRegistersTask(81, Priority.LOW,
					m(PlcOfPnt.ChannelId.DRY_AP1_2, new FloatDoublewordElement(81)),
					m(PlcOfPnt.ChannelId.DRY_RTD_TEMP_2, new FloatDoublewordElement(83)),
					m(PlcOfPnt.ChannelId.DRY_NM_FLOW_2, new FloatDoublewordElement(85)),
					m(PlcOfPnt.ChannelId.DRY_TOTAL_NM_FLOW_2, new FloatDoublewordElement(87))),

//			new FC4ReadInputRegistersTask(70, Priority.LOW,
//					m(PlcOfPnt.ChannelId.DRY_AP1_1, new FloatDoublewordElement(70)),
//					m(PlcOfPnt.ChannelId.DRY_RTD_TEMP_1, new FloatDoublewordElement(72)),
//					m(PlcOfPnt.ChannelId.DRY_NM_FLOW_1, new FloatDoublewordElement(74))),
////					new DummyRegisterElement(77, 78), // 매개변수는 범위
//			new FC4ReadInputRegistersTask(78, Priority.LOW,
//					m(PlcOfPnt.ChannelId.DRY_TOTAL_NM_FLOW_1, new FloatDoublewordElement(78)),
////			new FC4ReadInputRegistersTask(81, Priority.LOW,
//					m(PlcOfPnt.ChannelId.DRY_AP1_2, new FloatDoublewordElement(80)),
//					m(PlcOfPnt.ChannelId.DRY_RTD_TEMP_2, new FloatDoublewordElement(82)),
//					m(PlcOfPnt.ChannelId.DRY_NM_FLOW_2, new FloatDoublewordElement(84)),
//					m(PlcOfPnt.ChannelId.DRY_TOTAL_NM_FLOW_2, new FloatDoublewordElement(86))),

			new FC4ReadInputRegistersTask(90, Priority.LOW,
					m(PlcOfPnt.ChannelId.TEMP_CNT_PV_1, new SignedWordElement(90)),
					m(PlcOfPnt.ChannelId.TEMP_CNT_SV_1, new SignedWordElement(91)),
					m(PlcOfPnt.ChannelId.TEMP_CNT_PV_2, new SignedWordElement(92)),
					m(PlcOfPnt.ChannelId.TEMP_CNT_SV_2, new SignedWordElement(93)))
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
	Channel<Integer> getTempCntPv1() {
		return this.channel(ChannelId.TEMP_CNT_PV_1);
	}
	Channel<Integer> getTempCntSv1() {
		return this.channel(ChannelId.TEMP_CNT_SV_2);
	}
	Channel<Integer> getTempCntPv2() {
		return this.channel(ChannelId.TEMP_CNT_PV_2);
	}
	Channel<Integer> getTempCntSv2() {
		return this.channel(ChannelId.TEMP_CNT_SV_2);
	}
	Channel<Float> getDryAp1() {
		return this.channel(ChannelId.DRY_AP1_1);
	}
	Channel<Float> getDryRtdTemp1() {
		return this.channel(ChannelId.DRY_RTD_TEMP_1);
	}
	Channel<Float> getDryNmFlow1() {
		return this.channel(ChannelId.DRY_NM_FLOW_1);
	}
	Channel<Float> getDryTotalNmFlow1() {
		return this.channel(ChannelId.DRY_TOTAL_NM_FLOW_1);
	}
	Channel<Float> getDryAp2() {
		return this.channel(ChannelId.DRY_AP1_2);
	}
	Channel<Float> getDryRtdTemp2() {
		return this.channel(ChannelId.DRY_RTD_TEMP_2);
	}
	Channel<Float> getDryNmFlow2() {
		return this.channel(ChannelId.DRY_NM_FLOW_2);
	}
	Channel<Float> getDryTotalNmFlow2() {
		return this.channel(ChannelId.DRY_TOTAL_NM_FLOW_2);
	}

	@Override
	public String debugLog() {
		return "L:" + this.getDryAp1().value().asString() + " / "
				+ this.getDryRtdTemp1().value().asString() + " / "
				+ this.getDryNmFlow1().value().asString() + " / "
				+ this.getDryTotalNmFlow1().value().asString() + " / "
				
				+ this.getDryAp2().value().asString() + " / "
				+ this.getDryRtdTemp2().value().asString() + " / "
				+ this.getDryNmFlow2().value().asString() + " / "
				+ this.getDryTotalNmFlow2().value().asString();
	}
	
	@Override
	public MeterType getMeterType() {
		return this.config.type();
	}

}
