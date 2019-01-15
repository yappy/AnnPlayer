package io.github.yappy.annplayer;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
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
    private int playingIndex = -1;

    // 初期化時に一度だけ呼ばれる
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 再生停止ボタンのクリックイベント設定
        FloatingActionButton playButton = findViewById(R.id.fab1);
        playButton.setOnClickListener((view) -> {
            if (selectedIndex >= 0) {
                play(selectedIndex);
            }
        });
        FloatingActionButton stopButton = findViewById(R.id.fab2);
        stopButton.setOnClickListener((view) -> {
            stop();
        });

        loadListFromSdCard();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_refresh:
                stop();
                loadListFromSdCard();
                return true;
            case R.id.menu_about:
                new AboutDialog().show(getSupportFragmentManager(), "About");
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
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

    // 再生ボタンクリックイベント
    // 0 <= n < list size
    private void play(int n) {
        stop();

        mediaPlayer = MediaPlayer.create(this, Uri.fromFile(musicFiles[n]));
        if (mediaPlayer == null) {
            showToast("An error occurred");
            return;
        }
        // 再生完了イベント
        mediaPlayer.setOnCompletionListener(mp -> {
            selectedIndex = (playingIndex + 1) % musicFiles.length;
            playingIndex = -1;
            updateButtonColors();
            scrollToSelected();
        });
        // エラー終了イベント
        mediaPlayer.setOnErrorListener((mp, what, extra) -> {
            playingIndex = -1;
            updateButtonColors();
            showToast("An error occurred");
            return true;
        });
        mediaPlayer.start();

        playingIndex = n;
        updateButtonColors();
    }

    // 停止ボタンクリックイベント
    private void stop() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        playingIndex = -1;
        updateButtonColors();
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

    // n 番目のボタンクリックイベント
    private void onSelectList(int n) {
        selectedIndex = n;
        updateButtonColors();
        scrollToSelected();
    }

    // selectedIndex と playingIndex をボタンの色に反映する
    private void updateButtonColors() {
        for (Button button : playListButtons) {
            button.setBackgroundColor(android.R.drawable.btn_default);
        }
        if (selectedIndex >= 0) {
            playListButtons[selectedIndex].setBackgroundColor(Color.rgb(255, 128, 128));
        }
        if (playingIndex >= 0) {
            playListButtons[playingIndex].setBackgroundColor(Color.rgb(255, 0, 0));
        }
    }

    // n番目のボタンがスクロールビューの中央付近に来るようスクロールする
    private void scrollToSelected() {
        if (selectedIndex < 0) {
            return;
        }
        ScrollView scrollView = findViewById(R.id.scrollView);
        View content = scrollView.getChildAt(0);
        // 中心 y 座標はスクロールビューの高さの半分
        int center = scrollView.getHeight() / 2;
        // 中身の高さの (index+1 / 全ボタン数) の座標を狙う
        int y = content.getHeight() * (selectedIndex + 1) / playListButtons.length;
        // そのままだと狙った座標がスクロールビューの一番上に来てしまうので
        // スクロールビューの高さの半分だけ上に戻す
        scrollView.smoothScrollTo(0, y - center);
    }

    // バージョン情報ダイアログ
    public static class AboutDialog extends DialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            Resources res = getResources();
            String text = res.getString(R.string.about,
                    res.getString(R.string.app_name),
                    res.getString(R.string.copyright),
                    BuildConfig.VERSION_NAME,
                    BuildConfig.GIT_DATE,
                    BuildConfig.GIT_HASH);

            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setMessage(text)
                    .setPositiveButton("OK", (dialog, id) -> {
                        // OK
                    });
            return builder.create();
        }
    }

}
