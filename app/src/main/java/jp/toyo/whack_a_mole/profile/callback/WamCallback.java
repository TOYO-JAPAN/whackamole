package jp.toyo.whack_a_mole.profile.callback;

import android.bluetooth.BluetoothDevice;

import androidx.annotation.NonNull;

public interface WamCallback {
    void onLedStateChanged(@NonNull final BluetoothDevice device, final boolean on);
}
