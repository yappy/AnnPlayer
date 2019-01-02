package io.github.yappy.annplayer;

        import android.support.design.widget.FloatingActionButton;
        import android.support.v7.app.AppCompatActivity;
        import android.os.Bundle;
        import android.view.View;
        import android.widget.Button;
        import android.widget.LinearLayout;
        import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeListArea();

        FloatingActionButton playButton = findViewById(R.id.fab1);
        playButton.setOnClickListener((view) -> {
            Toast.makeText(MainActivity.this, "play", Toast.LENGTH_SHORT).show();
        });
        FloatingActionButton stopButton = findViewById(R.id.fab2);
        stopButton.setOnClickListener((view) -> {
            Toast.makeText(MainActivity.this, "stop", Toast.LENGTH_SHORT).show();
        });
    }

    private void initializeListArea() {
        LinearLayout area = findViewById(R.id.list_area);
        area.removeAllViews();
        for (int i = 0; i < 100; i++) {
            View inf = getLayoutInflater().inflate(R.layout.list_button, null);
            Button button = inf.findViewById(R.id.list_button);
            button.setText("Sound " + i);
            button.setTag(i);
            button.setOnClickListener((view) -> {
                int n = (Integer)view.getTag();
                Toast.makeText(MainActivity.this, "press " + n, Toast.LENGTH_SHORT).show();
            });
            area.addView(inf);
        }
    }
}
