package jp.toyo.whack_a_mole.model;

import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.location.LocationManager;
import android.preference.PreferenceManager;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;

import java.util.List;

import jp.toyo.whack_a_mole.util.Utils;
import no.nordicsemi.android.support.v18.scanner.BluetoothLeScannerCompat;
import no.nordicsemi.android.support.v18.scanner.ScanCallback;
import no.nordicsemi.android.support.v18.scanner.ScanResult;
import no.nordicsemi.android.support.v18.scanner.ScanSettings;

public class ScanViewModel extends AndroidViewModel {
	private static final String PREFS_FILTER_UUID_REQUIRED = "filter_uuid";
	private static final String PREFS_FILTER_NEARBY_ONLY = "filter_nearby";
	private final DevicesLiveData mDevicesLiveData;
	private final ScanStateLiveData mScannerStateLiveData;
	private final SharedPreferences mPreferences;

	public DevicesLiveData getDevices() {
		return mDevicesLiveData;
	}

	public ScanStateLiveData getScannerState() {
		return mScannerStateLiveData;
	}

	public ScanViewModel(final Application application) {
		super(application);
		mPreferences = PreferenceManager.getDefaultSharedPreferences(application);

		final boolean filterUuidRequired = isUuidFilterEnabled();
		final boolean filerNearbyOnly = isNearbyFilterEnabled();

		mScannerStateLiveData = new ScanStateLiveData(Utils.isBleEnabled(),
				Utils.isLocationEnabled(application));
		mDevicesLiveData = new DevicesLiveData(filterUuidRequired, filerNearbyOnly);
		registerBroadcastReceivers(application);
	}

	@Override
	protected void onCleared() {
		super.onCleared();
		getApplication().unregisterReceiver(mBluetoothStateBroadcastReceiver);

		if (Utils.isMarshmallowOrAbove()) {
			getApplication().unregisterReceiver(mLocationProviderChangedReceiver);
		}
	}

	public boolean isUuidFilterEnabled() {
		return mPreferences.getBoolean(PREFS_FILTER_UUID_REQUIRED, true);
	}

	public boolean isNearbyFilterEnabled() {
		return mPreferences.getBoolean(PREFS_FILTER_NEARBY_ONLY, false);
	}

	public void refresh() {
		mScannerStateLiveData.refresh();
	}

	public void filterByUuid(final boolean uuidRequired) {
		mPreferences.edit().putBoolean(PREFS_FILTER_UUID_REQUIRED, uuidRequired).apply();
		if (mDevicesLiveData.filterByUuid(uuidRequired))
			mScannerStateLiveData.recordFound();
		else
			mScannerStateLiveData.clearRecords();
	}

	public void filterByDistance(final boolean nearbyOnly) {
		mPreferences.edit().putBoolean(PREFS_FILTER_NEARBY_ONLY, nearbyOnly).apply();
		if (mDevicesLiveData.filterByDistance(nearbyOnly))
			mScannerStateLiveData.recordFound();
		else
			mScannerStateLiveData.clearRecords();
	}

	public void startScan() {
		if (mScannerStateLiveData.isScanning()) {
			return;
		}

		// Scanning settings
		final ScanSettings settings = new ScanSettings.Builder()
				.setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
				.setReportDelay(500)
				.setUseHardwareBatchingIfSupported(false)
				.build();

		final BluetoothLeScannerCompat scanner = BluetoothLeScannerCompat.getScanner();
		scanner.startScan(null, settings, scanCallback);
		mScannerStateLiveData.scanningStarted();
	}

	public void stopScan() {
		if (mScannerStateLiveData.isScanning() && mScannerStateLiveData.isBluetoothEnabled()) {
			final BluetoothLeScannerCompat scanner = BluetoothLeScannerCompat.getScanner();
			scanner.stopScan(scanCallback);
			mScannerStateLiveData.scanningStopped();
		}
	}

	private final ScanCallback scanCallback = new ScanCallback() {
		@Override
		public void onScanResult(final int callbackType, @NonNull final ScanResult result) {
			// This callback will be called only if the scan report delay is not set or is set to 0.

			// If the packet has been obtained while Location was disabled, mark Location as not required
			if (Utils.isLocationRequired(getApplication()) && !Utils.isLocationEnabled(getApplication()))
				Utils.markLocationNotRequired(getApplication());

			if (mDevicesLiveData.deviceDiscovered(result)) {
				mDevicesLiveData.applyFilter();
				mScannerStateLiveData.recordFound();
			}
		}

		@Override
		public void onBatchScanResults(@NonNull final List<ScanResult> results) {
			// This callback will be called only if the report delay set above is greater then 0.

			// If the packet has been obtained while Location was disabled, mark Location as not required
			if (Utils.isLocationRequired(getApplication()) && !Utils.isLocationEnabled(getApplication()))
				Utils.markLocationNotRequired(getApplication());

			boolean atLeastOneMatchedFilter = false;
			for (final ScanResult result : results)
				atLeastOneMatchedFilter = mDevicesLiveData.deviceDiscovered(result) || atLeastOneMatchedFilter;
			if (atLeastOneMatchedFilter) {
				mDevicesLiveData.applyFilter();
				mScannerStateLiveData.recordFound();
			}
		}

		@Override
		public void onScanFailed(final int errorCode) {
			// TODO This should be handled
			mScannerStateLiveData.scanningStopped();
		}
	};

	private void registerBroadcastReceivers(final Application application) {
		application.registerReceiver(mBluetoothStateBroadcastReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
		if (Utils.isMarshmallowOrAbove()) {
			application.registerReceiver(mLocationProviderChangedReceiver, new IntentFilter(LocationManager.MODE_CHANGED_ACTION));
		}
	}

	private final BroadcastReceiver mLocationProviderChangedReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(final Context context, final Intent intent) {
			final boolean enabled = Utils.isLocationEnabled(context);
			mScannerStateLiveData.setLocationEnabled(enabled);
		}
	};

	private final BroadcastReceiver mBluetoothStateBroadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(final Context context, final Intent intent) {
			final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_OFF);
			final int previousState = intent.getIntExtra(BluetoothAdapter.EXTRA_PREVIOUS_STATE, BluetoothAdapter.STATE_OFF);

			switch (state) {
				case BluetoothAdapter.STATE_ON:
					mScannerStateLiveData.bluetoothEnabled();
					break;
				case BluetoothAdapter.STATE_TURNING_OFF:
				case BluetoothAdapter.STATE_OFF:
					if (previousState != BluetoothAdapter.STATE_TURNING_OFF && previousState != BluetoothAdapter.STATE_OFF) {
						stopScan();
						mScannerStateLiveData.bluetoothDisabled();
					}
					break;
			}
		}
	};
}
