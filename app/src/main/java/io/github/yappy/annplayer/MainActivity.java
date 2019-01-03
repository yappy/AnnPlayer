package io.github.yappy.annplayer;

import android.app.Dialog;
import android.graphics.Color;
import android.os.Environment;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    private int selectedIndex = -1;
    private Button[] playListButtons;

    // 初期化時に一度だけ呼ばれる
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 再生停止ボタンのクリックイベント設定
        FloatingActionButton playButton = findViewById(R.id.fab1);
        playButton.setOnClickListener((view) -> {
            showToast("play " + selectedIndex);
        });
        FloatingActionButton stopButton = findViewById(R.id.fab2);
        stopButton.setOnClickListener((view) -> {
            showToast("stop");
        });

        loadSdCard();
    }

    // Toast (short) を表示する
    private void showToast(String msg) {
        Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
    }

    // SD カードの内容を確認して UI に反映する
    private void loadSdCard() {
        String state = Environment.getExternalStorageState();
        if (!Environment.MEDIA_MOUNTED.equals(state) && !Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            showToast("SD card is not found");
            return;
        }
        new SdCardInitDialog().show(getSupportFragmentManager(), "SdInit");
        initializeListArea();
    }

    private void initializeListArea() {
        final int PLAY_LIST_NUM = 100;

        LinearLayout area = findViewById(R.id.list_area);
        area.removeAllViews();
        playListButtons = new Button[PLAY_LIST_NUM];

        for (int i = 0; i < PLAY_LIST_NUM; i++) {
            View inf = getLayoutInflater().inflate(R.layout.list_button, null);
            Button button = inf.findViewById(R.id.list_button);
            button.setText("Sound " + i);
            button.setTag(i);
            button.setOnClickListener((view) -> {
                int n = (Integer) view.getTag();
                onSelectList(n);
            });
            area.addView(inf);
            playListButtons[i] = button;
        }

        onSelectList(-1);
    }

    private void onSelectList(int n) {
        selectedIndex = n;

        for (Button button : playListButtons) {
            button.setBackgroundColor(android.R.drawable.btn_default);
        }
        if (n >= 0) {
            playListButtons[n].setBackgroundColor(Color.RED);
        }
    }

    // SD カードにディレクトリを作る確認ダイアログ
    public static class SdCardInitDialog extends DialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // Use the Builder class for convenient dialog construction
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setMessage("Create a directory and files for this app?")
                    .setPositiveButton("OK", (dialog, id) -> {
                        // OK
                    })
                    .setNegativeButton("Cancel", (dialog, id) -> {
                        // Cancel
                    });
            // Create the AlertDialog object and return it
            return builder.create();
        }
    }

}
