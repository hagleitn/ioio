package ioio.tests.torture;

import java.util.concurrent.TimeoutException;

import ioio.lib.api.DigitalInput;
import ioio.lib.api.DigitalOutput;
import ioio.lib.api.IOIO;
import ioio.lib.api.PulseInput;
import ioio.lib.api.PulseInput.ClockRate;
import ioio.lib.api.PulseInput.PulseMode;
import ioio.lib.api.PwmOutput;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.tests.torture.ResourceAllocator.PeripheralType;
import android.util.Log;

public class PwmIncapTest implements Test<Boolean> {
	private final IOIO ioio_;
	private final ResourceAllocator alloc_;
	private final int pin1_;
	private final int pin2_;

	public PwmIncapTest(IOIO ioio, ResourceAllocator alloc)
			throws InterruptedException {
		ioio_ = ioio;
		alloc_ = alloc;
		pin1_ = alloc.allocatePinPair(ResourceAllocator.PIN_PAIR_PERIPHERAL);
		alloc.allocPeripheral(PeripheralType.PWM);
		alloc.allocPeripheral(PeripheralType.INCAP_SINGLE);
		alloc.allocPeripheral(PeripheralType.INCAP_DOUBLE);
		pin2_ = pin1_ + 1;
	}

	@Override
	public Boolean run() throws ConnectionLostException, InterruptedException, TimeoutException {
		Log.i("IOIOTortureTest", "Starting PwmIncapTest on pins: " + pin1_
				+ ", " + pin2_);
		try {
			if (!runTest(pin1_, pin2_)) {
				return false;
			}
			if (!runTest(pin2_, pin1_)) {
				return false;
			}
		} finally {
			alloc_.freePinPair(pin1_);
			alloc_.freePeripheral(PeripheralType.PWM);
			alloc_.freePeripheral(PeripheralType.INCAP_SINGLE);
			alloc_.freePeripheral(PeripheralType.INCAP_DOUBLE);
		}
		Log.i("IOIOTortureTest", "Passed PwmIncapTest on pins: " + pin1_ + ", "
				+ pin2_);
		return true;
	}

	private boolean runTest(int inPin, int outPin)
			throws ConnectionLostException, InterruptedException, TimeoutException {
		if (outPin == 9) {
			// pin 9 doesn't support peripheral output
			return true;
		}
		if (!runTest(inPin, outPin, 2000, 20, ClockRate.RATE_16MHz,
				PulseMode.FREQ_SCALE_4, false, 0f))
			return false;
		if (!runTest(inPin, outPin, 2000, 50, ClockRate.RATE_16MHz,
				PulseMode.FREQ, false, 0f))
			return false;
		if (!runTest(inPin, outPin, 2000, 100, ClockRate.RATE_2MHz,
				PulseMode.FREQ_SCALE_16, false, 0f))
			return false;
		if (!runTest(inPin, outPin, 2000, 100, ClockRate.RATE_16MHz,
				PulseMode.FREQ_SCALE_16, true, 0f))
			return false;
		if (!runTest(inPin, outPin, 2000, 100, ClockRate.RATE_16MHz,
				PulseMode.FREQ_SCALE_16, true, 0.1f))
			return false;
		if (!runTimeoutTest(inPin, outPin, ClockRate.RATE_16MHz, 0.5f))
			return false;
		return true;
	}
	
	private boolean runTimeoutTest(int inPin, int outPin, ClockRate rate, 
			float timeout) throws ConnectionLostException, InterruptedException {
		DigitalOutput out = ioio_.openDigitalOutput(outPin);
		out.write(false);
		PulseInput pulseDurIn = ioio_.openPulseInput(new DigitalInput.Spec(inPin),
				rate, PulseMode.POSITIVE, true);
		boolean exceptionCaught = false;
		try {
			pulseDurIn.getDuration(timeout);
		} catch (TimeoutException e) {
			exceptionCaught = true;
		} finally {
			pulseDurIn.close();
			out.close();
		}
		return exceptionCaught;
	}

	private boolean runTest(int inPin, int outPin, int freq,
			int pulseWidthUsec, ClockRate rate, PulseMode freqScaling,
			boolean doublePrecision, float timeout) throws ConnectionLostException,
			InterruptedException, TimeoutException {
		PulseInput pulseDurIn = null;
		PulseInput pulseFreqIn = null;
		PwmOutput out = null;
		try {
			out = ioio_.openPwmOutput(outPin, freq);
			out.setPulseWidth(pulseWidthUsec);
			// measure positive pulse
			pulseDurIn = ioio_.openPulseInput(new DigitalInput.Spec(inPin),
					rate, PulseMode.POSITIVE, doublePrecision);
			float duration = (timeout == 0f) ? pulseDurIn.getDuration() : 
				pulseDurIn.getDuration(timeout);
			float expectedDuration = pulseWidthUsec / 1000000.f;
			if (Math.abs((duration - expectedDuration) / duration) > 0.02) {
				Log.w("IOIOTortureTest", "Positive pulse duration is: "
						+ duration + "[s] while expected " + expectedDuration
						+ "[s]");
				return false;
			}
			pulseDurIn.close();
			// measure negative pulse
			pulseDurIn = ioio_.openPulseInput(new DigitalInput.Spec(inPin),
					rate, PulseMode.NEGATIVE, doublePrecision);
			duration = (timeout == 0f) ? pulseDurIn.getDuration() : 
				pulseDurIn.getDuration(timeout);
			expectedDuration = (1.f / freq) - (pulseWidthUsec / 1000000.f);
			if (Math.abs((duration - expectedDuration) / duration) > 0.02) {
				Log.w("IOIOTortureTest", "Negative pulse duration is: "
						+ duration + "[s] while expected " + expectedDuration
						+ "[s]");
				return false;
			}
			pulseDurIn.close();
			pulseDurIn = null;
			// measure frequency
			pulseFreqIn = ioio_.openPulseInput(new DigitalInput.Spec(inPin),
					rate, freqScaling, doublePrecision);
			float actualFreq = (timeout == 0f) ? pulseFreqIn.getFrequency() : 
				pulseFreqIn.getFrequency(timeout);
			if (Math.abs((actualFreq - freq) / freq) > 0.02) {
				Log.w("IOIOTortureTest", "Frequency is: " + actualFreq
						+ " while expected " + freq);
				return false;
			}
		} finally {
			if (pulseDurIn != null) {
				pulseDurIn.close();
			}
			if (pulseFreqIn != null) {
				pulseFreqIn.close();
			}
			if (out != null) {
				out.close();
			}
		}
		return true;
	}
}
