package io.github.yappy.annplayer;

import android.Manifest;
import android.content.ContentUris;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.core.content.ContextCompat;
import androidx.core.content.PermissionChecker;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {
    // Log
    private static final String TAG = MainActivity.class.getName();
    private final StringBuilder logBuffer = new StringBuilder();

    // State keys
    private static final String STATE_SELECTED_INDEX = "selectedIndex";
    private static final String STATE_PLAYING_INDEX = "playingIndex";
    private static final String STATE_PLAYING_POSITION = "playingPosition";

    private static final String PREF_KEY_FILTER = "filter";
    private static final String FILTER_DEFAULT = ".wav";

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
            reload();
            return true;
        } else if (id == R.id.menu_filter) {
            Resources res = getResources();
            SharedPreferences pref = getPreferences(Context.MODE_PRIVATE);
            EditText edit = new AppCompatEditText(this);
            edit.setText(pref.getString(PREF_KEY_FILTER, FILTER_DEFAULT));
            new AlertDialog.Builder(this)
                .setTitle("Search Filter")
                .setMessage(res.getString(R.string.msg_filter_input))
                .setView(edit)
                .setPositiveButton("OK", (dialog, which) -> {
                    String text = Objects.requireNonNull(edit.getText()).toString();
                    pref.edit().putString(PREF_KEY_FILTER, text).apply();
                    reload();
                })
                .setNegativeButton("Cancel", null)
                .setNeutralButton("Set Default", (dialog, which) -> {
                    pref.edit().putString(PREF_KEY_FILTER, FILTER_DEFAULT).apply();
                    reload();
                })
                .show();
            return true;
        } else if (id == R.id.menu_log) {
            new AlertDialog.Builder(this)
                .setTitle("Log")
                .setMessage(logBuffer.toString())
                .setPositiveButton("OK", null)
                .show();
            return true;
        } else if (id == R.id.menu_about) {
            new AlertDialog.Builder(this)
                .setTitle("About this application")
                .setMessage(createAboutText())
                .setPositiveButton("OK", null)
                .show();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    // Toast (short) を表示する
    private void showToast(String msg) {
        Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
    }

    // 再生を停止してリストを更新する
    private void reload() {
        stop();
        loadListFromStorage();
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

    // パーミッションを確認し、満たされているなら音声リストを更新する
    // 満たされていないなら適切な UI を呼ぶ
    private void loadListFromStorage() {
        clearLog();

        boolean already_granted;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // >= 33 (= Android 13 Tiramisu)
            already_granted = checkPermissions(Manifest.permission.READ_MEDIA_AUDIO);
        } else {
            // < 33
            already_granted = checkPermissions(Manifest.permission.READ_EXTERNAL_STORAGE);
        }
        if (already_granted) {
            loadListFromStorageBody();
        }
    }

    // パーミッションリストをチェックし、足りないものがあれば要求する
    // すべて満たされているならば true を返す
    // 満たされていないものがあれば要求画面を表示し、false を返す
    // (onRequestPermissionsResult ハンドラで続きは処理する、requestCode はそこに渡される)
    private boolean checkPermissions(String... perms) {
        List<String> required = new ArrayList<>();
        boolean shouldShowUI = false;
        for (String perm : perms) {
            int result = PermissionChecker.checkSelfPermission(this, perm);
            if (result != PermissionChecker.PERMISSION_GRANTED) {
                shouldShowUI = shouldShowUI || shouldShowRequestPermissionRationale(perm);
                required.add(perm);
            }
        }

        if (required.isEmpty()) {
            log("permission: already granted");
            return true;
        } else if (shouldShowUI) {
            log("permission: not granted, show UI");
            new AlertDialog.Builder(MainActivity.this)
                .setTitle("Permission Required")
                .setMessage("Permit to list sound files")
                .setPositiveButton("OK", (dialog, which) -> {
                    log("OK button on UI");
                    requestPermissions(required.toArray(String[]::new), 0);
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    log("cancel button on UI");
                })
                .show();
            return false;
        } else {
            log("permission: not granted, do request");
            requestPermissions(required.toArray(String[]::new), 0);
            return false;
        }
    }

    // requestPermissions の結果
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        for (int result : grantResults) {
            if (result != PermissionChecker.PERMISSION_GRANTED) {
                return;
            }
        }
        // リクエストがすべて許可された場合
        loadListFromStorageBody();
    }

    // 音声ファイル一覧を列挙する
    // (パーミッションが必要、無いと成功はするが結果が大幅に減る)
    private void loadListFromStorageBody() {
        log("Start scan");

        // SharedPreferences からフィルタを読み出して空白文字で split
        SharedPreferences pref = getPreferences(Context.MODE_PRIVATE);
        String filter_all = pref.getString(PREF_KEY_FILTER, FILTER_DEFAULT);
        String[] filters = filter_all.split("\\s+");
        for (String f : filters) {
            log("filter: " + f);
        }

        musicFileList.clear();

        var resolver = getContentResolver();
        String[] projection = new String[]{
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.RELATIVE_PATH,
            MediaStore.Audio.Media.DISPLAY_NAME,
        };
        String order = MediaStore.Audio.Media.DISPLAY_NAME;


        // This synthetic volume provides a merged view of all media across
        // all currently attached external storage devices.
        Uri contentUri = MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL);
        try (Cursor cursor = resolver.query(contentUri, projection, null, null, order)) {
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
                    boolean hit = false;
                    for (String f : filters) {
                        if (displayName.contains(f)) {
                            hit = true;
                            break;
                        }
                    }
                    if (hit) {
                        musicFileList.add(new MusicElement(displayName, uri));
                        log("HIT");
                    } else {
                        log("NOT HIT");
                    }
                }
            }
        }

        // UI に反映
        updateListArea();
    }

    // musicFileList で リスト UI を更新初期化する
    private void updateListArea() {
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
        scrollView.smoothScrollTo(0, Math.max(y - center, 0));
    }

    private String createAboutText() {
        Resources res = getResources();
        return res.getString(R.string.about,
            res.getString(R.string.app_name),
            res.getString(R.string.copyright),
            BuildConfig.VERSION_NAME,
            BuildConfig.GIT_DATE,
            BuildConfig.GIT_HASH);
    }

}
