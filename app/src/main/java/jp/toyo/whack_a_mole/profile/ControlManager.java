package jp.toyo.whack_a_mole.profile;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.UUID;

import jp.toyo.whack_a_mole.profile.callback.WamDataCallback;
import no.nordicsemi.android.ble.BleManager;
import no.nordicsemi.android.ble.data.Data;
import no.nordicsemi.android.log.LogContract;
import no.nordicsemi.android.log.LogSession;
import no.nordicsemi.android.log.Logger;

public class ControlManager extends BleManager<ControlManagerCallbacks> {
	private static final byte STATE_OFF = 0x00;
	private static final byte STATE_ON = 0x01;
	public static Data turnOn() {
		return Data.opCode(STATE_ON);
	}
	public static Data turnOff() {
		return Data.opCode(STATE_OFF);
	}
	public final static UUID LBS_UUID_SERVICE = UUID.fromString("19b10010-e8f2-537e-4f6c-d104768a1214");

	private BluetoothGattCharacteristic mLedCharacteristic, mButtonCharacteristicA, mButtonCharacteristicB, mButtonCharacteristicC;
	private LogSession mLogSession;
	private boolean mSupported;
	private boolean mLedOn;

	public ControlManager(@NonNull final Context context) {
		super(context);
	}

	@NonNull
	@Override
	protected BleManagerGattCallback getGattCallback() {
		return mGattCallback;
	}

	public void setLogger(@Nullable final LogSession session) {
		this.mLogSession = session;
	}

	@Override
	public void log(final int priority, @NonNull final String message) {
		Logger.log(mLogSession, LogContract.Log.Level.fromPriority(priority), message);
	}

	@Override
	protected boolean shouldClearCacheWhenDisconnected() {
		return !mSupported;
	}

	private final WamDataCallback mLedCallback = new WamDataCallback() {
		@Override
		public void onLedStateChanged(@NonNull final BluetoothDevice device,
									  final boolean on) {
			mLedOn = on;
			log(LogContract.Log.Level.APPLICATION, "LED " + (on ? "ON" : "OFF"));
			mCallbacks.onLedStateChanged(device, on);
		}

		@Override
		public void onInvalidDataReceived(@NonNull final BluetoothDevice device,
										  @NonNull final Data data) {
			// Data can only invalid if we read them. We assume the app always sends correct data.
			log(Log.WARN, "Invalid data received: " + data);
		}
	};

	private final BleManagerGattCallback mGattCallback = new BleManagerGattCallback() {
		@Override
		protected void initialize() {
			readCharacteristic(mLedCharacteristic).with(mLedCallback).enqueue();
		}

		@Override
		public boolean isRequiredServiceSupported(@NonNull final BluetoothGatt gatt) {
			final BluetoothGattService service = gatt.getService(LBS_UUID_SERVICE);
			if (service != null) {
				mLedCharacteristic = service.getCharacteristics().get(0);
				mButtonCharacteristicA = service.getCharacteristics().get(1);
				mButtonCharacteristicB = service.getCharacteristics().get(2);
				mButtonCharacteristicC = service.getCharacteristics().get(3);
			}

			boolean writeRequest = false;
			if (mLedCharacteristic != null) {
				final int rxProperties = mLedCharacteristic.getProperties();
				boolean test = (1 & 1) > 0;
				writeRequest = (rxProperties & BluetoothGattCharacteristic.PROPERTY_WRITE) > 0;
				writeRequest = true;
			}

			mSupported = mLedCharacteristic != null && mButtonCharacteristicA != null && mButtonCharacteristicB != null && mButtonCharacteristicC != null && writeRequest;
			return mSupported;
		}

		@Override
		protected void onDeviceDisconnected() {
			mLedCharacteristic = null;
			mButtonCharacteristicA = null;
			mButtonCharacteristicB = null;
			mButtonCharacteristicC = null;
		}
	};

	public void send(final boolean on) {
		// Are we connected?
		if (mLedCharacteristic == null)
			return;

		// No need to change?
		if (mLedOn == on)
			return;

		log(Log.WARN, "Turning LED " + (on ? "ON" : "OFF") + "...");
		byte[] value = mLedCharacteristic.getValue();
		writeCharacteristic(mLedCharacteristic, on ? turnOn() : turnOff()).with(mLedCallback).enqueue();
	}

	public void send(int type, boolean on) {
		switch (type) {
			case 0:
				writeCharacteristic(mButtonCharacteristicA, on ? turnOn() : turnOff()).with(mLedCallback).enqueue();
				break;
			case 1:
				writeCharacteristic(mButtonCharacteristicB, on ? turnOn() : turnOff()).with(mLedCallback).enqueue();
				break;
			case 2:
				writeCharacteristic(mButtonCharacteristicC, on ? turnOn() : turnOff()).with(mLedCallback).enqueue();
				break;
		}
	}
}
