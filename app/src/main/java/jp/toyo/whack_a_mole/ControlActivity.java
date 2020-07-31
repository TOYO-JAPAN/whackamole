package jp.toyo.whack_a_mole;

import androidx.lifecycle.ViewModelProviders;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Button;
import android.widget.Toast;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import jp.toyo.whack_a_mole.adapter.DiscoveredBluetoothDevice;
import jp.toyo.whack_a_mole.model.ControlViewModel;

public class ControlActivity extends AppCompatActivity {
    public static final String EXTRA_DEVICE = "jp.toyo.whack_a_mole.EXTRA_DEVICE";

    private ControlViewModel mViewModel;

    @BindView(R.id.led_switch) Switch mLed;
    @BindView(R.id.button_a) Button mButtonA;
    @BindView(R.id.button_b) Button mButtonB;
    @BindView(R.id.button_c) Button mButtonC;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_control);
        ButterKnife.bind(this);

        final Intent intent = getIntent();
        final DiscoveredBluetoothDevice device = intent.getParcelableExtra(EXTRA_DEVICE);
        final String deviceName = device.getName();
        final String deviceAddress = device.getAddress();

        final Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(deviceName);
        getSupportActionBar().setSubtitle(deviceAddress);
        getSupportActionBar().setDisplayHomeAsUpEnabled(false);

        // Configure the view model
        mViewModel = ViewModelProviders.of(this).get(ControlViewModel.class);
        mViewModel.connect(device);

        // Set up views
        final TextView ledState = findViewById(R.id.led_state);
        final LinearLayout progressContainer = findViewById(R.id.progress_container);
        final TextView connectionState = findViewById(R.id.connection_state);
        final View content = findViewById(R.id.device_container);
        final View notSupported = findViewById(R.id.not_supported);

        mLed.setOnCheckedChangeListener((buttonView, isChecked) -> mViewModel.toggleLED(isChecked));
        mButtonA.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        mViewModel.toggleButton(0, true);
                        break;
                    case MotionEvent.ACTION_UP:
                        mViewModel.toggleButton(0, false);
                        break;
                }
                return false;
            }
        });
        mButtonB.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        mViewModel.toggleButton(1, true);
                        break;
                    case MotionEvent.ACTION_UP:
                        mViewModel.toggleButton(1, false);
                        break;
                }
                return false;
            }
        });
        mButtonC.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        mViewModel.toggleButton(2, true);
                        break;
                    case MotionEvent.ACTION_UP:
                        mViewModel.toggleButton(2, false);
                        break;
                }
                return false;
            }
        });

        mViewModel.isDeviceReady().observe(this, deviceReady -> {
            progressContainer.setVisibility(View.GONE);
            content.setVisibility(View.VISIBLE);
        });
        mViewModel.getConnectionState().observe(this, text -> {
            if (text != null) {
                progressContainer.setVisibility(View.VISIBLE);
                notSupported.setVisibility(View.GONE);
                connectionState.setText(text);
            }
        });
        mViewModel.isConnected().observe(this, this::onConnectionStateChanged);
        mViewModel.isSupported().observe(this, supported -> {
            if (!supported) {
                progressContainer.setVisibility(View.GONE);
                notSupported.setVisibility(View.VISIBLE);
            }
        });
        mViewModel.getLEDState().observe(this, isOn -> {
            ledState.setText(isOn ? R.string.turn_on : R.string.turn_off);
            mLed.setChecked(isOn);
        });
    }

    @OnClick(R.id.action_clear_cache)
    public void onTryAgainClicked() {
        mViewModel.reconnect();
    }

    private void onConnectionStateChanged(final boolean connected) {
        mLed.setEnabled(connected);
        if (!connected) {
            mLed.setChecked(false);
        }
    }
}