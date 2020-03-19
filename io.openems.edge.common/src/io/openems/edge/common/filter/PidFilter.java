package io.openems.edge.common.filter;

/**
 * A proportional-integral-derivative controller.
 * 
 * @see https://en.wikipedia.org/wiki/PID_controller
 */
public class PidFilter {

	public static final double DEFAULT_P = 0.3;
	public static final double DEFAULT_I = 0.3;
	public static final double DEFAULT_D = 0.1;

	private final double p;
	private final double i;
	private final double d;

	private boolean firstRun = true;

	private double lastInput = 0;
	private double errorSum = 0;
	private Integer lowLimit = null;
	private Integer highLimit = null;

	/**
	 * Creates a PidFilter.
	 * 
	 * @param p the proportional gain
	 * @param i the integral gain
	 * @param d the derivative gain
	 */
	public PidFilter(double p, double i, double d) {
		this.p = p;
		this.i = i;
		this.d = d;
	}

	/**
	 * Creates a PidFilter using default values.
	 */
	public PidFilter() {
		this(DEFAULT_P, DEFAULT_I, DEFAULT_D);
	}

	/**
	 * Limit the output value.
	 * 
	 * @param lowLimit  lowest allowed output value
	 * @param highLimit highest allowed output value
	 */
	public void setLimits(Integer lowLimit, Integer highLimit) {
		if (lowLimit != null && highLimit != null && lowLimit > highLimit) {
			throw new IllegalArgumentException(
					"Given LowLimit [" + lowLimit + "] is higher than HighLimit [" + highLimit + "]");
		}
		this.lowLimit = lowLimit;
		this.highLimit = highLimit;
	}

	/**
	 * Apply the PID filter using the current Channel value as input and the target
	 * value.
	 * 
	 * @param input  the input value, e.g. the measured Channel value
	 * @param target the target value
	 * @return the filtered set-point value
	 */
	public int applyPidFilter(int input, int target) {
		// Pre-process the target value: apply output value limits
		target = this.applyLowHighLimits(target);

		// Calculate the error
		int error = target - input;

		// We are already there
		if (error == 0) {
			return target;
		}

		// Calculate P
		double outputP = this.p * error;

		// Set last values on first run
		if (this.firstRun) {
			this.lastInput = input;
			this.firstRun = false;
		}

		// Calculate I
		double outputI = this.i * this.errorSum;

		// Calculate D
		double outputD = -this.d * (input - this.lastInput);

		// Store last input value
		this.lastInput = input;

		// Sum outputs
		double output = outputP + outputI + outputD;

		// sum up the error
		this.errorSum += error;

		// Post-process the output value: convert to integer and apply value limits
		return this.applyLowHighLimits(Math.round((float) output));
	}

	/**
	 * Reset the PID filter.
	 * 
	 * <p>
	 * This method should be called when the filter was not used for a while.
	 */
	public void reset() {
		this.errorSum = 0;
		this.firstRun = true;
	}

	/**
	 * Applies the low and high limits to a value.
	 * 
	 * @param value the input value
	 * @return the value within low and high limit
	 */
	private int applyLowHighLimits(int value) {
		if (this.lowLimit != null && value < this.lowLimit) {
			value = this.lowLimit;
		}
		if (this.highLimit != null && value > this.highLimit) {
			value = this.highLimit;
		}
		return value;
	}
}