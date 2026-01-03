package io.github.yappy.annplayer;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentUris;
import android.content.res.Resources;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AppCompatActivity;

import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    // Log
    private static final String TAG = MainActivity.class.getName();
    private final StringBuilder logBuffer = new StringBuilder();

    // State keys
    private static final String STATE_SELECTED_INDEX = "selectedIndex";
    private static final String STATE_PLAYING_INDEX = "playingIndex";
    private static final String STATE_PLAYING_POSITION = "playingPosition";

    private MediaPlayer mediaPlayer = null;
    private final List<MusicElement> musicFileList = new ArrayList<>();
    private final List<Button> buttonList = new ArrayList<>();
    private int selectedIndex = -1;
    private int playingIndex = -1;

    record MusicElement(String name, Uri uri) {
    }

    private void log(String msg) {
        Log.i(TAG, msg);
        logBuffer.append(msg);
        logBuffer.append('\n');
    }

    private void clearLog() {
        logBuffer.setLength(0);
    }

    // 初期化時に一度だけ呼ばれる
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 再生停止ボタンのクリックイベント設定
        ImageButton playButton = findViewById(R.id.buttonPlay);
        playButton.setOnClickListener((view) -> {
            if (selectedIndex >= 0) {
                play(selectedIndex, 0);
            }
        });
        ImageButton stopButton = findViewById(R.id.buttonStop);
        stopButton.setOnClickListener((view) -> {
            stop();
        });

        loadListFromStorage();
    }

    // 破棄されたアクティビティが復帰した場合、onCreate より後
    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        // ファイルリストは読み直しているので可能な場合のみ選択インデックスを復元する
        int tmpSelectedIndex = savedInstanceState.getInt(STATE_SELECTED_INDEX);
        if (tmpSelectedIndex < musicFileList.size()) {
            onSelectList(tmpSelectedIndex);
        }
        // 再生中だった場合も可能な場合のみその場所から再生開始する
        int tmpPlayingIndex = savedInstanceState.getInt(STATE_PLAYING_INDEX);
        if (tmpPlayingIndex >= 0 && tmpPlayingIndex < musicFileList.size()) {
            int msec = savedInstanceState.getInt(STATE_PLAYING_POSITION);
            play(tmpPlayingIndex, msec);
        }
    }

    // 破棄される時
    @Override
    protected void onDestroy() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        super.onDestroy();
    }

    // バックグラウンドへ移動した時
    @Override
    protected void onPause() {
        super.onPause();
        if (mediaPlayer != null) {
            mediaPlayer.pause();
        }
    }

    // フォアグラウンドになった時(起動時も含む)
    @Override
    protected void onResume() {
        super.onResume();
        if (mediaPlayer != null) {
            mediaPlayer.start();
        }
    }

    // アクティビティが破棄される(かもしれない)前
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt(STATE_SELECTED_INDEX, selectedIndex);
        outState.putInt(STATE_PLAYING_INDEX, playingIndex);

        if (mediaPlayer != null) {
            int msec = mediaPlayer.getCurrentPosition();
            outState.putInt(STATE_PLAYING_POSITION, msec);
        } else {
            outState.putInt(STATE_PLAYING_POSITION, 0);
        }

        super.onSaveInstanceState(outState);
    }

    // メニューの生成タイミング
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        return true;
    }

    // メニュー選択イベント
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.menu_refresh) {
            stop();
            loadListFromStorage();
            return true;
        } else if (id == R.id.menu_log) {
            new SimpleDialog(logBuffer.toString()).show(getSupportFragmentManager(), "Log");
            return true;
        } else if (id == R.id.menu_about) {
            new AboutDialog().show(getSupportFragmentManager(), "About");
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    // Toast (short) を表示する
    private void showToast(String msg) {
        Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
    }

    // 再生ボタンクリックイベント
    // 0 <= n < list size
    private void play(int n, int msec) {
        stop();

        mediaPlayer = MediaPlayer.create(this, musicFileList.get(n).uri());
        if (mediaPlayer == null) {
            showToast(getResources().getString(R.string.msg_play_error));
            return;
        }
        // 再生完了イベント
        mediaPlayer.setOnCompletionListener(mp -> {
            selectedIndex = (playingIndex + 1) % musicFileList.size();
            playingIndex = -1;
            updateButtonColors();
            scrollToSelected();
        });
        // エラー終了イベント
        mediaPlayer.setOnErrorListener((mp, what, extra) -> {
            playingIndex = -1;
            updateButtonColors();
            showToast(getResources().getString(R.string.msg_play_error));
            return true;
        });
        mediaPlayer.seekTo(msec);
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

    // 音声ファイル一覧を列挙する
    private void loadListFromStorage() {
        clearLog();
        log("Start scan");

        musicFileList.clear();

        var resolver = getContentResolver();
        String[] projection = new String[]{
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.RELATIVE_PATH,
            MediaStore.Audio.Media.DISPLAY_NAME,
        };

        // This synthetic volume provides a merged view of all media across
        // all currently attached external storage devices.
        Uri contentUri = MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL);
        try (Cursor cursor = resolver.query(contentUri, projection, null, null, null)) {
            if (cursor != null) {
                int colId = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID);
                int colRelativePath = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.RELATIVE_PATH);
                int colDisplayName = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME);
                while (cursor.moveToNext()) {
                    long id = cursor.getLong(colId);
                    String relativePath = cursor.getString(colRelativePath);
                    String displayName = cursor.getString(colDisplayName);

                    Uri uri = ContentUris.withAppendedId(contentUri, id);
                    log("audio: " + displayName);
                    log(relativePath + " - " + uri);
                    musicFileList.add(new MusicElement(displayName, uri));
                }
            }
        }

        // UI に反映
        initializeListArea();
    }

    // リスト UI を更新初期化する
    private void initializeListArea() {
        LinearLayout area = findViewById(R.id.list_area);
        area.removeAllViews();
        buttonList.clear();

        for (int i = 0; i < musicFileList.size(); i++) {
            View inf = getLayoutInflater().inflate(R.layout.list_button, null);
            Button button = inf.findViewById(R.id.list_button);
            button.setText(musicFileList.get(i).name());
            button.setTag(i);
            button.setOnClickListener((view) -> {
                int n = (Integer) view.getTag();
                onSelectList(n);
            });
            area.addView(inf);
            buttonList.add(button);
        }

        // 存在するなら一番上を選択する
        if (!musicFileList.isEmpty()) {
            onSelectList(0);
        } else {
            onSelectList(-1);
        }
    }

    // n 番目のボタンクリックイベント
    // -1 も可
    private void onSelectList(int n) {
        selectedIndex = n;
        updateButtonColors();
        scrollToSelected();
    }

    // selectedIndex と playingIndex をボタンの色に反映する
    private void updateButtonColors() {
        for (Button button : buttonList) {
            button.setBackgroundColor(android.R.drawable.btn_default);
        }
        if (selectedIndex >= 0) {
            buttonList.get(selectedIndex).setBackgroundColor(
                ContextCompat.getColor(this, R.color.colorSelected));
        }
        if (playingIndex >= 0) {
            buttonList.get(playingIndex).setBackgroundColor(
                ContextCompat.getColor(this, R.color.colorPlaying));
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
        int y = content.getHeight() * (selectedIndex + 1) / buttonList.size();
        // そのままだと狙った座標がスクロールビューの一番上に来てしまうので
        // スクロールビューの高さの半分だけ上に戻す
        scrollView.smoothScrollTo(0, y - center);
    }

    public static class SimpleDialog extends DialogFragment {
        private String text;

        public SimpleDialog() {
            this("");
        }

        public SimpleDialog(String text) {
            this.text = text;
        }

        public void setText(String text) {
            this.text = text;
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
            super.onCreateDialog(savedInstanceState);

            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setMessage(text)
                .setPositiveButton("OK", (dialog, id) -> {
                    // OK
                });
            return builder.create();
        }
    }

    // バージョン情報ダイアログ
    public static class AboutDialog extends SimpleDialog {
        @NonNull
        @Override
        public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
            setText(createAboutText(getResources()));
            return super.onCreateDialog(savedInstanceState);
        }

        private static String createAboutText(Resources res) {
            return res.getString(R.string.about,
                res.getString(R.string.app_name),
                res.getString(R.string.copyright),
                BuildConfig.VERSION_NAME,
                BuildConfig.GIT_DATE,
                BuildConfig.GIT_HASH);
        }
    }

}
