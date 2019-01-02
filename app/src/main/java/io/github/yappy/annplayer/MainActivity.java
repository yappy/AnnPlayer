package io.github.yappy.annplayer;

        import android.support.v7.app.AppCompatActivity;
        import android.os.Bundle;
        import android.view.View;
        import android.widget.Button;
        import android.widget.LinearLayout;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeListArea();
    }

    private void initializeListArea() {
        LinearLayout area = findViewById(R.id.list_area);
        area.removeAllViews();
        for (int i = 0; i < 100; i++) {
            View view = getLayoutInflater().inflate(R.layout.list_button, null);
            Button button = view.findViewById(R.id.list_button);
            button.setText("Sound " + i);
            area.addView(view);
        }
    }
}
