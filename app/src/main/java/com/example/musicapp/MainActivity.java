package com.example.musicapp;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;
import android.view.View;

import java.util.ArrayList;

public class MainActivity extends Activity {

    private static final int REQUEST_AUDIO = 100;
    private ListView musicList;
    private final ArrayList<String> songNames = new ArrayList<>();
    private final ArrayList<Uri> songUris = new ArrayList<>();
    private MediaPlayer player;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        musicList = new ListView(this);
        setContentView(musicList);

        musicList.setOnItemClickListener((parent, view, position, id) -> {
            playSong(position);
        });

        String permission;
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            permission = Manifest.permission.READ_MEDIA_AUDIO;
        } else {
            permission = Manifest.permission.READ_EXTERNAL_STORAGE;
        }

        if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{permission}, REQUEST_AUDIO);
        } else {
            loadMusic();
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_AUDIO && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            loadMusic();
        } else {
            Toast.makeText(this, "اجازه دسترسی به آهنگ‌ها داده نشد",
                    Toast.LENGTH_LONG).show();
        }
    }

    private void loadMusic() {
        songNames.clear();
        songUris.clear();

        String[] projection = {
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE
        };

        try (Cursor cursor = getContentResolver().query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                MediaStore.Audio.Media.IS_MUSIC + " != 0",
                null,
                MediaStore.Audio.Media.TITLE + " ASC")) {

            if (cursor != null) {
                int idColumn = cursor.getColumnIndexOrThrow(
                        MediaStore.Audio.Media._ID);
                int titleColumn = cursor.getColumnIndexOrThrow(
                        MediaStore.Audio.Media.TITLE);

                while (cursor.moveToNext()) {
                    long songId = cursor.getLong(idColumn);
                    String title = cursor.getString(titleColumn);

                    Uri songUri = Uri.withAppendedPath(
                            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                            String.valueOf(songId));

                    songNames.add(title);
                    songUris.add(songUri);
                }
            }
        } catch (Exception e) {
            Toast.makeText(this, "خطا در خواندن آهنگ‌ها",
                    Toast.LENGTH_LONG).show();
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                songNames
        );
        musicList.setAdapter(adapter);

        if (songNames.isEmpty()) {
            Toast.makeText(this, "هیچ آهنگی پیدا نشد",
                    Toast.LENGTH_LONG).show();
        }
    }

    private void playSong(int position) {
        stopPlayer();

        try {
            player = new MediaPlayer();
            player.setDataSource(this, songUris.get(position));
            player.setOnPreparedListener(mp -> {
                mp.start();
                Toast.makeText(this, "در حال پخش: " + songNames.get(position),
                        Toast.LENGTH_SHORT).show();
            });
            player.setOnCompletionListener(mp -> stopPlayer());
            player.setOnErrorListener((mp, what, extra) -> {
                stopPlayer();
                Toast.makeText(this, "پخش این آهنگ ممکن نشد",
                        Toast.LENGTH_LONG).show();
                return true;
            });
            player.prepareAsync();
        } catch (Exception e) {
            stopPlayer();
            Toast.makeText(this, "خطا در پخش آهنگ",
                    Toast.LENGTH_LONG).show();
        }
    }

    private void stopPlayer() {
        if (player != null) {
            try {
                if (player.isPlaying()) {
                    player.stop();
                }
            } catch (Exception ignored) {
            }
            player.release();
            player = null;
        }
    }

    @Override
    protected void onDestroy() {
        stopPlayer();
        super.onDestroy();
    }
}
