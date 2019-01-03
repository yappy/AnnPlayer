package io.github.yappy.annplayer;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.io.File;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity {

    // Music dir から音声リストを読み出す
    private static final int PERM_REQ_READ_MUSIC_LIST = 1;

    private MediaPlayer mediaPlayer = null;
    private File[] musicFiles;
    private Button[] playListButtons;
    private int selectedIndex = -1;
    private int playedIndex = -1;

    // 初期化時に一度だけ呼ばれる
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 再生停止ボタンのクリックイベント設定
        FloatingActionButton playButton = findViewById(R.id.fab1);
        playButton.setOnClickListener((view) -> {
            play(selectedIndex);
            showToast("play " + selectedIndex);
        });
        FloatingActionButton stopButton = findViewById(R.id.fab2);
        stopButton.setOnClickListener((view) -> {
            stop();
            showToast("stop");
        });

        loadListFromSdCard();
    }

    // パーミッション要求の結果
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERM_REQ_READ_MUSIC_LIST: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    loadListFromSdCardBh();
                }
                break;
            }
        }
    }

    // Toast (short) を表示する
    private void showToast(String msg) {
        Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
    }

    private void play(int n) {
        stop();
        mediaPlayer = MediaPlayer.create(this, Uri.fromFile(musicFiles[n]));
        playedIndex = n;
        mediaPlayer.setOnCompletionListener(mp -> {
            selectedIndex = (playedIndex + 1) % musicFiles.length;
            playedIndex = -1;
            updateButtonColors();
        });
        mediaPlayer.setOnErrorListener((mp, what, extra) -> {
            playedIndex = -1;
            updateButtonColors();
            showToast("An error occurred");
            return true;
        });
        mediaPlayer.start();
        updateButtonColors();
    }

    private void stop() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        playedIndex = -1;
    }

    // SD カードの内容を確認して UI に反映する
    private void loadListFromSdCard() {
        // マウント状態確認
        String state = Environment.getExternalStorageState();
        if (!Environment.MEDIA_MOUNTED.equals(state) && !Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            showToast("SD card is not found");
            return;
        }

        // 外部ストレージの read permission を許可されてから後半処理する
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) !=
                PackageManager.PERMISSION_GRANTED) {
            // ダイアログを出して許可を得る (出ない場合もある)
            // 許可されたら onRequestPermissionsResult コールバックから loadListFromSdCardBh() を呼ぶ
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERM_REQ_READ_MUSIC_LIST);
            return;
        }
        else {
            // 許可されているので普通に後半を呼ぶ
            loadListFromSdCardBh();
        }
    }

    // 後半 (bottom half)
    private void loadListFromSdCardBh() {
        // 共有 Music ディレクトリ
        File musicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC);
        File[] files = musicDir.listFiles((f) -> f.isFile() && f.getName().endsWith(".mp3"));
        // エラーの場合 null (おそらくパーミッションエラー)
        if (files == null) {
            showToast("Read music dir error");
            return;
        }
        Arrays.sort(files);
        musicFiles = files;

        // UI に反映
        initializeListArea();
    }

    // リスト UI を更新初期化する
    private void initializeListArea() {
        LinearLayout area = findViewById(R.id.list_area);
        area.removeAllViews();
        playListButtons = new Button[musicFiles.length];

        for (int i = 0; i < playListButtons.length; i++) {
            View inf = getLayoutInflater().inflate(R.layout.list_button, null);
            Button button = inf.findViewById(R.id.list_button);
            button.setText(musicFiles[i].getName());
            button.setTag(i);
            button.setOnClickListener((view) -> {
                int n = (Integer) view.getTag();
                onSelectList(n);
            });
            area.addView(inf);
            playListButtons[i] = button;
        }

        if (musicFiles.length > 0) {
            onSelectList(0);
        }
        else {
            onSelectList(-1);
        }
    }

    private void onSelectList(int n) {
        selectedIndex = n;
        updateButtonColors();
    }

    private void updateButtonColors() {
        for (Button button : playListButtons) {
            button.setBackgroundColor(android.R.drawable.btn_default);
        }
        if (selectedIndex >= 0) {
            playListButtons[selectedIndex].setBackgroundColor(Color.rgb(255, 128, 128));
        }
        if (playedIndex >= 0) {
            playListButtons[playedIndex].setBackgroundColor(Color.rgb(255, 0, 0));
        }
    }

}
