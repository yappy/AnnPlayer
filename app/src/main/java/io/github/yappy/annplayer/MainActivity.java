package io.github.yappy.annplayer;

import android.graphics.Color;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    private int selectedIndex = -1;
    private Button[] playListButtons;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeListArea();

        FloatingActionButton playButton = findViewById(R.id.fab1);
        playButton.setOnClickListener((view) -> {
            Toast.makeText(MainActivity.this, "play " + selectedIndex, Toast.LENGTH_SHORT).show();
        });
        FloatingActionButton stopButton = findViewById(R.id.fab2);
        stopButton.setOnClickListener((view) -> {
            Toast.makeText(MainActivity.this, "stop", Toast.LENGTH_SHORT).show();
        });
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

}
