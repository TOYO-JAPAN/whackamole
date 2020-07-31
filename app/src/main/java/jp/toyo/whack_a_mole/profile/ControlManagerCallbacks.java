package jp.toyo.whack_a_mole.profile;

import no.nordicsemi.android.ble.BleManagerCallbacks;
import jp.toyo.whack_a_mole.profile.callback.WamCallback;

public interface ControlManagerCallbacks extends BleManagerCallbacks, WamCallback {}
