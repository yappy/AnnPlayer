package io.github.yappy.annplayer;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.io.File;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity {

    private File[] musicFiles;
    private Button[] playListButtons;
    private int selectedIndex = -1;

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
        // マウント状態確認
        String state = Environment.getExternalStorageState();
        if (!Environment.MEDIA_MOUNTED.equals(state) && !Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            showToast("SD card is not found");
            return;
        }

        // 共有 Music ディレクトリ
        File musicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC);
        File[] files = musicDir.listFiles(/*(f) -> f.isFile() && f.getName().endsWith(".mp3")*/);
        // エラーの場合 null (おそらくパーミッションエラー)
        if (files == null) {
            showToast("Read music dir error");
            return;
        }
        Arrays.sort(files);
        musicFiles = files;

        initializeListArea();
    }

    private void initializeListArea() {
        LinearLayout area = findViewById(R.id.list_area);
        area.removeAllViews();
        playListButtons = new Button[musicFiles.length];

        for (int i = 0; i < playListButtons.length; i++) {
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

}
