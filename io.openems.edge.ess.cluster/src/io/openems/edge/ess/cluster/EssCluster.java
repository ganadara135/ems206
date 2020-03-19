package io.openems.edge.ess.cluster;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.channel.AccessMode;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.calculate.CalculateAverage;
import io.openems.edge.common.channel.calculate.CalculateIntegerSum;
import io.openems.edge.common.channel.calculate.CalculateLongSum;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveNatureTable;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.ess.api.AsymmetricEss;
import io.openems.edge.ess.api.CalculateGridMode;
import io.openems.edge.ess.api.ManagedAsymmetricEss;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.MetaEss;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.ess.power.api.Power;

@Designate(ocd = Config.class, factory = true)
@Component( //
		name = "Ess.Cluster", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = { //
				EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE //
		})
public class EssCluster extends AbstractOpenemsComponent implements ManagedAsymmetricEss, AsymmetricEss,
		ManagedSymmetricEss, SymmetricEss, MetaEss, OpenemsComponent, EventHandler, ModbusSlave {

	private final Logger log = LoggerFactory.getLogger(EssCluster.class);

	@Reference
	private Power power = null;

	@Reference
	protected ComponentManager componentManager;

	private Config config;

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

	public EssCluster() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				SymmetricEss.ChannelId.values(), //
				ManagedSymmetricEss.ChannelId.values(), //
				AsymmetricEss.ChannelId.values(), //
				ManagedAsymmetricEss.ChannelId.values(), //
				ChannelId.values() //
		);
	}

	@Activate
	void activate(ComponentContext context, Config config) throws OpenemsException {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.config = config;
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public void handleEvent(Event event) {
		switch (event.getTopic()) {

		case EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE:
			this.calculateChannelValues();
			break;
		}
	}

	/**
	 * Calculates the sum-value for each Channel.
	 */
	private void calculateChannelValues() {
		final CalculateAverage soc = new CalculateAverage();
		final CalculateIntegerSum capacity = new CalculateIntegerSum();
		final CalculateGridMode gridMode = new CalculateGridMode();
		final CalculateIntegerSum activePower = new CalculateIntegerSum();
		final CalculateIntegerSum reactivePower = new CalculateIntegerSum();
		final CalculateIntegerSum maxApparentPower = new CalculateIntegerSum();
		final CalculateLongSum activeChargeEnergy = new CalculateLongSum();
		final CalculateLongSum activeDischargeEnergy = new CalculateLongSum();

		final CalculateIntegerSum activePowerL1 = new CalculateIntegerSum();
		final CalculateIntegerSum reactivePowerL1 = new CalculateIntegerSum();
		final CalculateIntegerSum activePowerL2 = new CalculateIntegerSum();
		final CalculateIntegerSum reactivePowerL2 = new CalculateIntegerSum();
		final CalculateIntegerSum activePowerL3 = new CalculateIntegerSum();
		final CalculateIntegerSum reactivePowerL3 = new CalculateIntegerSum();

		for (String essId : this.config.ess_ids()) {
			SymmetricEss ess;
			try {
				ess = this.componentManager.getComponent(essId);
			} catch (OpenemsNamedException e) {
				this.logError(this.log, e.getMessage());
				continue;
			}

			soc.addValue(ess.getSoc());
			capacity.addValue(ess.getCapacity());
			gridMode.addValue(ess.getGridMode());
			activePower.addValue(ess.getActivePower());
			reactivePower.addValue(ess.getReactivePower());
			maxApparentPower.addValue(ess.getMaxApparentPower());
			activeChargeEnergy.addValue(ess.getActiveChargeEnergy());
			activeDischargeEnergy.addValue(ess.getActiveDischargeEnergy());

			if (ess instanceof AsymmetricEss) {
				AsymmetricEss e = (AsymmetricEss) ess;
				activePowerL1.addValue(e.getActivePowerL1());
				reactivePowerL1.addValue(e.getReactivePowerL1());
				activePowerL2.addValue(e.getActivePowerL2());
				reactivePowerL2.addValue(e.getReactivePowerL2());
				activePowerL3.addValue(e.getActivePowerL3());
				reactivePowerL3.addValue(e.getReactivePowerL3());
			}
		}

		// Set values
		this.getSoc().setNextValue(soc.calculate());
		this.getCapacity().setNextValue(capacity.calculate());
		this.getGridMode().setNextValue(gridMode.calculate());
		this.getActivePower().setNextValue(activePower.calculate());
		this.getReactivePower().setNextValue(reactivePower.calculate());
		this.getMaxApparentPower().setNextValue(maxApparentPower.calculate());
		this.getActiveChargeEnergy().setNextValue(activeChargeEnergy.calculate());
		this.getActiveDischargeEnergy().setNextValue(activeDischargeEnergy.calculate());

		this.getActivePowerL1().setNextValue(activePowerL1.calculate());
		this.getReactivePowerL1().setNextValue(reactivePowerL1.calculate());
		this.getActivePowerL2().setNextValue(activePowerL2.calculate());
		this.getReactivePowerL2().setNextValue(reactivePowerL2.calculate());
		this.getActivePowerL3().setNextValue(activePowerL3.calculate());
		this.getReactivePowerL3().setNextValue(reactivePowerL3.calculate());
	}

	@Override
	public void applyPower(int activePower, int reactivePower) throws OpenemsException {
		throw new OpenemsException("EssClusterImpl.applyPower() should never be called.");
	}

	@Override
	public void applyPower(int activePowerL1, int reactivePowerL1, int activePowerL2, int reactivePowerL2,
			int activePowerL3, int reactivePowerL3) throws OpenemsException {
		throw new OpenemsException("EssClusterImpl.applyPower() should never be called.");
	}

	@Override
	public int getPowerPrecision() {
		// TODO kleinstes gemeinsames Vielfaches von PowerPrecision
		return 0;
	}

	@Override
	public synchronized String[] getEssIds() {
		return this.config.ess_ids();
	}

	@Override
	public Power getPower() {
		return this.power;
	}

	@Override
	public ModbusSlaveTable getModbusSlaveTable(AccessMode accessMode) {
		return new ModbusSlaveTable( //
				OpenemsComponent.getModbusSlaveNatureTable(accessMode), //
				SymmetricEss.getModbusSlaveNatureTable(accessMode), //
				ManagedSymmetricEss.getModbusSlaveNatureTable(accessMode), //
				AsymmetricEss.getModbusSlaveNatureTable(accessMode), //
				ManagedAsymmetricEss.getModbusSlaveNatureTable(accessMode), //
				ModbusSlaveNatureTable.of(EssCluster.class, accessMode, 300) //
						.build());
	}
}
