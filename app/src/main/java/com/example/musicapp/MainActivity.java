package com.example.musicapp;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;

public class MainActivity extends Activity {

    private static final int REQUEST_AUDIO = 100;
    private ListView musicList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        musicList = new ListView(this);
        setContentView(musicList);

        if (android.os.Build.VERSION.SDK_INT >= 33) {

            if (checkSelfPermission(Manifest.permission.READ_MEDIA_AUDIO)
                    != PackageManager.PERMISSION_GRANTED) {

                requestPermissions(
                        new String[]{Manifest.permission.READ_MEDIA_AUDIO},
                        REQUEST_AUDIO
                );

            } else {
                loadMusic();
            }

        } else {

            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {

                requestPermissions(
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        REQUEST_AUDIO
                );

            } else {
                loadMusic();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            String[] permissions,
            int[] grantResults) {

        super.onRequestPermissionsResult(
                requestCode,
                permissions,
                grantResults);

        if (requestCode == REQUEST_AUDIO &&
                grantResults.length > 0 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {

            loadMusic();

        } else {

            Toast.makeText(
                    this,
                    "اجازه دسترسی به آهنگ‌ها داده نشد",
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    private void loadMusic() {

        ArrayList<String> songs = new ArrayList<>();

        String[] projection = {
                MediaStore.Audio.Media.TITLE
        };

        Cursor cursor = getContentResolver().query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                MediaStore.Audio.Media.IS_MUSIC + " != 0",
                null,
                MediaStore.Audio.Media.TITLE + " ASC"
        );

        if (cursor != null) {

            int titleColumn =
                    cursor.getColumnIndex(
                            MediaStore.Audio.Media.TITLE);

            while (cursor.moveToNext()) {

                if (titleColumn >= 0) {
                    songs.add(
                            cursor.getString(titleColumn)
                    );
                }
            }

            cursor.close();
        }

        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_list_item_1,
                        songs
                );

        musicList.setAdapter(adapter);

        if (songs.isEmpty()) {

            Toast.makeText(
                    this,
                    "هیچ آهنگی پیدا نشد",
                    Toast.LENGTH_LONG
            ).show();
        }
    }
}

