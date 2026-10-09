package com.example.musicapp;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;

import androidx.core.app.NotificationCompat;
import androidx.media.app.NotificationCompat.MediaStyle;

public class MusicService extends Service {
    public static final String ACTION_PLAY = "com.example.musicapp.PLAY";
    public static final String ACTION_TOGGLE = "com.example.musicapp.TOGGLE";
    public static final String ACTION_NEXT = "com.example.musicapp.NEXT";
    public static final String ACTION_PREVIOUS = "com.example.musicapp.PREVIOUS";
    public static final String EXTRA_URI = "song_uri";
    public static final String EXTRA_TITLE = "song_title";
    public static final String EXTRA_ARTIST = "song_artist";

    private static final String CHANNEL_ID = "music_playback";
    private static final int NOTIFICATION_ID = 1001;

    private MediaPlayer player;
    private MediaSessionCompat mediaSession;
    private String title = "موزیک من";
    private String artist = "";
    private Uri currentUri;

    @Override
    public void onCreate() {
        super.onCreate();
        createChannel();
        mediaSession = new MediaSessionCompat(this, "MusicAppSession");
        mediaSession.setCallback(new MediaSessionCompat.Callback() {
            @Override
            public void onPlay() {
                resumeMusic();
            }

            @Override
            public void onPause() {
                pauseMusic();
            }
        });
        mediaSession.setActive(true);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            if (ACTION_TOGGLE.equals(action)) {
                toggleMusic();
            } else if (ACTION_NEXT.equals(action) ||
                    ACTION_PREVIOUS.equals(action)) {
                // انتخاب آهنگ بعدی/قبلی از فهرست در مرحله اتصال صفحه اصلی انجام می‌شود.
                updateNotification();
            } else {
                String uri = intent.getStringExtra(EXTRA_URI);
                if (uri != null) {
                    title = intent.getStringExtra(EXTRA_TITLE);
                    artist = intent.getStringExtra(EXTRA_ARTIST);
                    if (title == null) title = "موزیک من";
                    if (artist == null) artist = "";
                    playMusic(Uri.parse(uri));
                }
            }
        }
        startForeground(NOTIFICATION_ID, buildNotification());
        return START_NOT_STICKY;
    }

    private void playMusic(Uri uri) {
        stopPlayer();
        currentUri = uri;
        try {
            player = new MediaPlayer();
            player.setDataSource(this, uri);
            player.setOnPreparedListener(mp -> {
                mp.start();
                updateState();
                updateNotification();
            });
            player.setOnCompletionListener(mp -> {
                updateState();
                updateNotification();
            });
            player.setOnErrorListener((mp, what, extra) -> {
                stopPlayer();
                updateNotification();
                return true;
            });
            updateNotification();
            player.prepareAsync();
        } catch (Exception e) {
            stopPlayer();
            updateNotification();
        }
    }

    private void toggleMusic() {
        if (player == null) return;
        try {
            if (player.isPlaying()) pauseMusic();
            else resumeMusic();
        } catch (Exception ignored) {
        }
    }

    private void pauseMusic() {
        try {
            if (player != null && player.isPlaying()) player.pause();
        } catch (Exception ignored) {
        }
        updateState();
        updateNotification();
    }

    private void resumeMusic() {
        try {
            if (player != null && !player.isPlaying()) player.start();
        } catch (Exception ignored) {
        }
        updateState();
        updateNotification();
    }

    private void updateState() {
        boolean playing = false;
        try {
            playing = player != null && player.isPlaying();
        } catch (Exception ignored) {
        }
        long state = playing ? PlaybackStateCompat.STATE_PLAYING
                : PlaybackStateCompat.STATE_PAUSED;
        mediaSession.setPlaybackState(new PlaybackStateCompat.Builder()
                .setActions(PlaybackStateCompat.ACTION_PLAY
                        | PlaybackStateCompat.ACTION_PAUSE
                        | PlaybackStateCompat.ACTION_PLAY_PAUSE)
                .setState((int) state, 0, 1f)
                .build());
    }

    private PendingIntent actionIntent(String action, int requestCode) {
        Intent intent = new Intent(this, MusicService.class);
        intent.setAction(action);
        return PendingIntent.getService(this, requestCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private Notification buildNotification() {
        boolean playing = false;
        try {
            playing = player != null && player.isPlaying();
        } catch (Exception ignored) {
        }

        Intent openIntent = new Intent(this, MainActivity.class);
        PendingIntent openPending = PendingIntent.getActivity(this, 0, openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_media_play)
                .setContentTitle(title)
                .setContentText(artist)
                .setContentIntent(openPending)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setOngoing(playing)
                .addAction(android.R.drawable.ic_media_previous, "قبلی",
                        actionIntent(ACTION_PREVIOUS, 1))
                .addAction(playing ? android.R.drawable.ic_media_pause
                                : android.R.drawable.ic_media_play,
                        playing ? "توقف" : "پخش",
                        actionIntent(ACTION_TOGGLE, 2))
                .addAction(android.R.drawable.ic_media_next, "بعدی",
                        actionIntent(ACTION_NEXT, 3))
                .setStyle(new MediaStyle()
                        .setMediaSession(mediaSession.getSessionToken())
                        .setShowActionsInCompactView(0, 1, 2))
                .build();
    }

    private void updateNotification() {
        if (Build.VERSION.SDK_INT >= 26) createChannel();
        NotificationManager manager =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (manager != null) manager.notify(NOTIFICATION_ID, buildNotification());
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "پخش موسیقی", NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("کنترل پخش موسیقی در پس‌زمینه");
            NotificationManager manager =
                    (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }

    private void stopPlayer() {
        if (player != null) {
            try {
                player.release();
            } catch (Exception ignored) {
            }
            player = null;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        stopPlayer();
        if (mediaSession != null) {
            mediaSession.setActive(false);
            mediaSession.release();
        }
        super.onDestroy();
    }
}
