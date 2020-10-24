package io.openems.edge.meter.api;

import java.util.function.Consumer;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.modbusslave.ModbusSlaveNatureTable;
import io.openems.edge.common.modbusslave.ModbusType;
import io.openems.edge.common.type.TypeUtils;

/**
 * Represents an Asymmetric Meter.
 * 
 * - Negative ActivePowerL1/L2/L3 and ConsumptionActivePowerL1/L2/L3 represent
 * Consumption, i.e. power that is 'leaving the system', e.g. feed-to-grid
 * 
 * - Positive ActivePowerL1/L2/L3 and ProductionActivePowerL1/L2/L3 represent
 * Production, i.e. power that is 'entering the system', e.g. buy-from-grid
 * 
 */
public interface EDfemsMeter extends SymmetricMeter {

	public final static String POWER_DOC_TEXT = "Accura2350, Gas and Fluid Energy Meter";

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		/**
		 * Active Power L1
		 * 
		 * <ul>
		 * <li>Interface: Meter Asymmetric
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>Range: negative values for Consumption (power that is 'leaving the
		 * system', e.g. feed-to-grid); positive for Production (power that is 'entering
		 * the system')
		 * </ul>
		 */
		ACCURA2350_L1(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.text(POWER_DOC_TEXT)), //
		/**
		 * Active Power L2
		 * 
		 * <ul>
		 * <li>Interface: Meter Asymmetric
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>Range: negative values for Consumption (power that is 'leaving the
		 * system', e.g. feed-to-grid); positive for Production (power that is 'entering
		 * the system')
		 * </ul>
		 */
		ACCURA2350_L2(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.text(POWER_DOC_TEXT)), //
		/**
		 * Active Power L3
		 * 
		 * <ul>
		 * <li>Interface: Meter Asymmetric
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>Range: negative values for Consumption (power that is 'leaving the
		 * system', e.g. feed-to-grid); positive for Production (power that is 'entering
		 * the system')
		 * </ul>
		 */
		ACCURA2350_L3(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.text(POWER_DOC_TEXT)), //
		/**
		 * Reactive Power L1
		 * 
		 * <ul>
		 * <li>Interface: Meter Asymmetric
		 * <li>Type: Integer
		 * <li>Unit: var
		 * <li>Range: negative values for Consumption (power that is 'leaving the
		 * system', e.g. feed-to-grid); positive for Production (power that is 'entering
		 * the system')
		 * </ul>
		 */
		REACTIVE_POWER_L1(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT_AMPERE_REACTIVE) //
				.text(POWER_DOC_TEXT)), //
		/**
		 * Reactive Power L2
		 * 
		 * <ul>
		 * <li>Interface: Meter Asymmetric
		 * <li>Type: Integer
		 * <li>Unit: var
		 * <li>Range: negative values for Consumption (power that is 'leaving the
		 * system', e.g. feed-to-grid); positive for Production (power that is 'entering
		 * the system')
		 * </ul>
		 */
		REACTIVE_POWER_L2(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT_AMPERE_REACTIVE) //
				.text(POWER_DOC_TEXT)), //
		/**
		 * Reactive Power L3
		 * 
		 * <ul>
		 * <li>Interface: Meter Asymmetric
		 * <li>Type: Integer
		 * <li>Unit: var
		 * <li>Range: negative values for Consumption (power that is 'leaving the
		 * system', e.g. feed-to-grid); positive for Production (power that is 'entering
		 * the system')
		 * </ul>
		 */
		REACTIVE_POWER_L3(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT_AMPERE_REACTIVE) //
				.text(POWER_DOC_TEXT)), //
		/**
		 * Voltage L1
		 * 
		 * <ul>
		 * <li>Interface: Meter Asymmetric
		 * <li>Type: Integer
		 * <li>Unit: mV
		 * </ul>
		 */
		VOLTAGE_L1(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		/**
		 * Voltage L2
		 * 
		 * <ul>
		 * <li>Interface: Meter Asymmetric
		 * <li>Type: Integer
		 * <li>Unit: mV
		 * </ul>
		 */
		VOLTAGE_L2(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		/**
		 * Voltage L3
		 * 
		 * <ul>
		 * <li>Interface: Meter Asymmetric
		 * <li>Type: Integer
		 * <li>Unit: mV
		 * </ul>
		 */
		VOLTAGE_L3(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		/**
		 * Current L1
		 * 
		 * <ul>
		 * <li>Interface: Meter Asymmetric
		 * <li>Type: Integer
		 * <li>Unit: mA
		 * </ul>
		 */
		CURRENT_L1(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIAMPERE)), //
		/**
		 * Current L2
		 * 
		 * <ul>
		 * <li>Interface: Meter Asymmetric
		 * <li>Type: Integer
		 * <li>Unit: mA
		 * </ul>
		 */
		CURRENT_L2(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIAMPERE)), //
		/**
		 * Current L3
		 * 
		 * <ul>
		 * <li>Interface: Meter Asymmetric
		 * <li>Type: Integer
		 * <li>Unit: mA
		 * </ul>
		 */
		CURRENT_L3(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIAMPERE)); //

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		public Doc doc() {
			return this.doc;
		}
	}

	public static ModbusSlaveNatureTable getModbusSlaveNatureTable(AccessMode accessMode) {
		return ModbusSlaveNatureTable.of(EDfemsMeter.class, accessMode, 100) //
				.channel(0, ChannelId.ACCURA2350_L1, ModbusType.FLOAT32) //
				.channel(2, ChannelId.ACCURA2350_L2, ModbusType.FLOAT32) //
				.channel(4, ChannelId.ACCURA2350_L3, ModbusType.FLOAT32) //
				.channel(6, ChannelId.REACTIVE_POWER_L1, ModbusType.FLOAT32) //
				.channel(8, ChannelId.REACTIVE_POWER_L2, ModbusType.FLOAT32) //
				.channel(10, ChannelId.REACTIVE_POWER_L3, ModbusType.FLOAT32) //
				.channel(12, ChannelId.VOLTAGE_L1, ModbusType.FLOAT32) //
				.channel(14, ChannelId.VOLTAGE_L2, ModbusType.FLOAT32) //
				.channel(16, ChannelId.VOLTAGE_L3, ModbusType.FLOAT32) //
				.channel(18, ChannelId.CURRENT_L1, ModbusType.FLOAT32) //
				.channel(20, ChannelId.CURRENT_L2, ModbusType.FLOAT32) //
				.channel(22, ChannelId.CURRENT_L3, ModbusType.FLOAT32) //
				.build();
	}

	/**
	 * Gets the Active Power for L1 in [W]. Negative values for Consumption;
	 * positive for Production
	 * 
	 * @return
	 */
	default Channel<Integer> getAccura2350L1() {
		return this.channel(ChannelId.ACCURA2350_L1);
	}

	/**
	 * Gets the Active Power for L2 in [W]. Negative values for Consumption;
	 * positive for Production
	 * 
	 * @return
	 */
	default Channel<Integer> getAccura2350L2() {
		return this.channel(ChannelId.ACCURA2350_L2);
	}

	/**
	 * Gets the Active Power for L3 in [W]. Negative values for Consumption;
	 * positive for Production
	 * 
	 * @return
	 */
	default Channel<Integer> getAccura2350L3() {
		return this.channel(ChannelId.ACCURA2350_L3);
	}

	/**
	 * Gets the Reactive Power for L1 in [var]. Negative values for Consumption;
	 * positive for Production.
	 * 
	 * @return
	 */
	default Channel<Integer> getReactivePowerL1() {
		return this.channel(ChannelId.REACTIVE_POWER_L1);
	}

	/**
	 * Gets the Reactive Power for L2 in [var]. Negative values for Consumption;
	 * positive for Production.
	 * 
	 * @return
	 */
	default Channel<Integer> getReactivePowerL2() {
		return this.channel(ChannelId.REACTIVE_POWER_L2);
	}

	/**
	 * Gets the Reactive Power for L3 in [var]. Negative values for Consumption;
	 * positive for Production.
	 * 
	 * @return
	 */
	default Channel<Integer> getReactivePowerL3() {
		return this.channel(ChannelId.REACTIVE_POWER_L3);
	}

	/**
	 * Gets the Channel for Voltage on L1 in [mV].
	 * 
	 * @return the Channel
	 */
	default Channel<Integer> getVoltageL1() {
		return this.channel(ChannelId.VOLTAGE_L1);
	}

	/**
	 * Gets the Channel for Voltage on L2 in [mV].
	 * 
	 * @return the Channel
	 */
	default Channel<Integer> getVoltageL2() {
		return this.channel(ChannelId.VOLTAGE_L2);
	}

	/**
	 * Gets the Channel for Voltage on L3 in [mV].
	 * 
	 * @return the Channel
	 */
	default Channel<Integer> getVoltageL3() {
		return this.channel(ChannelId.VOLTAGE_L3);
	}

	/**
	 * Initializes Channel listeners to set the Active- and Reactive-Power Channel
	 * value as the sum of L1 + L2 + L3.
	 * 
	 * @param meter
	 */
	public static void initializePowerSumChannels(EDfemsMeter meter) {
		// Active Power
		final Consumer<Value<Integer>> activePowerSum = ignore -> {
			meter.getActivePower().setNextValue(TypeUtils.sum(//
					meter.getAccura2350L1().value().get(), //
					meter.getAccura2350L2().value().get(), //
					meter.getAccura2350L3().value().get()));
		};
		meter.getAccura2350L1().onSetNextValue(activePowerSum);
		meter.getAccura2350L2().onSetNextValue(activePowerSum);
		meter.getAccura2350L3().onSetNextValue(activePowerSum);

		// Reactive Power
		final Consumer<Value<Integer>> reactivePowerSum = ignore -> {
			meter.getReactivePower().setNextValue(TypeUtils.sum(//
					meter.getReactivePowerL1().value().get(), //
					meter.getReactivePowerL2().value().get(), //
					meter.getReactivePowerL3().value().get()));
		};
		meter.getReactivePowerL1().onSetNextValue(reactivePowerSum);
		meter.getReactivePowerL2().onSetNextValue(reactivePowerSum);
		meter.getReactivePowerL3().onSetNextValue(reactivePowerSum);
	}
}
