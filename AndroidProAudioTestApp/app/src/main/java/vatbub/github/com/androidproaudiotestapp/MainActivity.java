package vatbub.github.com.androidproaudiotestapp;

import android.content.Intent;
import android.net.Uri;
import android.support.v4.view.LayoutInflaterCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import com.mikepenz.iconics.context.IconicsLayoutInflater2;
import com.mikepenz.iconics.view.IconicsButton;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        LayoutInflaterCompat.setFactory2(getLayoutInflater(), new IconicsLayoutInflater2(getDelegate()));
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        IconicsButton githubButton = findViewById(R.id.github_button);
        githubButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String githubURL = "https://github.com/vatbub/HearingAidAndroid";
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(githubURL));
                startActivity(browserIntent);
            }
        });
    }
}
