package com.example.musicapp;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends Activity {

    private static final int REQUEST_AUDIO = 100;
    private static final int BG = Color.rgb(17, 15, 28);
    private static final int PANEL = Color.rgb(35, 30, 51);
    private static final int PURPLE = Color.rgb(157, 104, 255);
    private static final int WHITE = Color.WHITE;
    private static final int MUTED = Color.rgb(185, 178, 204);

    private final ArrayList<String> songNames = new ArrayList<>();
    private final ArrayList<String> songArtists = new ArrayList<>();
    private final ArrayList<Uri> songUris = new ArrayList<>();
    private final Handler handler = new Handler();

    private MediaPlayer player;
    private int currentIndex = -1;
    private boolean prepared = false;
    private boolean seeking = false;
    private boolean darkTheme = true;

    private LinearLayout root;
    private TextView titleText;
    private TextView artistText;
    private TextView timeText;
    private TextView coverText;
    private TextView countText;
    private SeekBar progressBar;
    private Button playButton;
    private Button themeButton;
    private ListView musicList;

    private final Runnable progressUpdater = new Runnable() {
        @Override
        public void run() {
            if (player != null && prepared) {
                try {
                    if (!seeking) {
                        progressBar.setMax(Math.max(1, player.getDuration()));
                        progressBar.setProgress(player.getCurrentPosition());
                    }
                    timeText.setText(formatTime(player.getCurrentPosition())
                            + " / " + formatTime(player.getDuration()));
                } catch (Exception ignored) {
                }
            }
            handler.postDelayed(this, 500);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        showHome();

        String permission = Build.VERSION.SDK_INT >= 33
                ? Manifest.permission.READ_MEDIA_AUDIO
                : Manifest.permission.READ_EXTERNAL_STORAGE;

        if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{permission}, REQUEST_AUDIO);
        } else {
            loadMusic();
        }

        handler.post(progressUpdater);
    }

    private GradientDrawable rounded(int color, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(radius);
        return d;
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        t.setGravity(Gravity.CENTER);
        if (bold) t.setTypeface(null, Typeface.BOLD);
        t.setPadding(8, 8, 8, 8);
        return t;
    }

    private Button button(String label) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextColor(WHITE);
        b.setTextSize(14);
        b.setAllCaps(false);
        b.setBackground(rounded(PANEL, 24));
        return b;
    }

    private LinearLayout column() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }

    private LinearLayout row() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setGravity(Gravity.CENTER);
        return l;
    }

    private void setupRoot() {
        root = column();
        root.setPadding(16, 12, 16, 12);
        root.setBackgroundColor(darkTheme ? BG : Color.rgb(246, 243, 252));
        setContentView(root);
    }

    private int foreground() {
        return darkTheme ? WHITE : Color.rgb(35, 28, 48);
    }

    private int secondary() {
        return darkTheme ? MUTED : Color.rgb(100, 90, 115);
    }

    private void showHome() {
        setupRoot();

        LinearLayout top = row();
        TextView brand = text("♫  موزیک من", 23, foreground(), true);
        top.addView(brand, new LinearLayout.LayoutParams(0, -2, 1));
        Button settings = button("⚙ تنظیمات");
        settings.setOnClickListener(v -> showSettings());
        top.addView(settings);
        root.addView(top);

        root.addView(text("پخش‌کننده موزیک آفلاین", 13, secondary(), false));

        coverText = text("♫", 76, WHITE, true);
        GradientDrawable coverBg = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{PURPLE, Color.rgb(62, 35, 106), Color.rgb(22, 19, 39)}
        );
        coverBg.setCornerRadius(36);
        coverText.setBackground(coverBg);
        LinearLayout.LayoutParams coverParams =
                new LinearLayout.LayoutParams(-1, 155);
        coverParams.setMargins(0, 12, 0, 12);
        root.addView(coverText, coverParams);

        titleText = text("آهنگی انتخاب نشده", 19, foreground(), true);
        root.addView(titleText);
        artistText = text("برای شروع، یک آهنگ انتخاب کن", 13, secondary(), false);
        root.addView(artistText);

        progressBar = new SeekBar(this);
        progressBar.setProgressTintList(
                android.content.res.ColorStateList.valueOf(PURPLE));
        progressBar.setThumbTintList(
                android.content.res.ColorStateList.valueOf(PURPLE));
        root.addView(progressBar);

        timeText = text("00:00 / 00:00", 12, secondary(), false);
        root.addView(timeText);

        progressBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar bar, int value, boolean fromUser) {
                if (fromUser && player != null && prepared) {
                    timeText.setText(formatTime(value) + " / "
                            + formatTime(player.getDuration()));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar bar) {
                seeking = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar bar) {
                if (player != null && prepared) {
                    try {
                        player.seekTo(bar.getProgress());
                    } catch (Exception ignored) {
                    }
                }
                seeking = false;
            }
        });

        LinearLayout controls = row();
        Button previous = button("⏮ قبلی");
        playButton = button("▶ پخش");
        Button next = button("بعدی ⏭");

        previous.setOnClickListener(v -> playAdjacent(-1));
        playButton.setOnClickListener(v -> togglePlayback());
        next.setOnClickListener(v -> playAdjacent(1));

        controls.addView(previous, new LinearLayout.LayoutParams(0, 54, 1));
        controls.addView(playButton, new LinearLayout.LayoutParams(0, 54, 1));
        controls.addView(next, new LinearLayout.LayoutParams(0, 54, 1));
        root.addView(controls);

        LinearLayout listHeader = row();
        listHeader.addView(text("🎵 آهنگ‌های من", 17, foreground(), true),
                new LinearLayout.LayoutParams(0, -2, 1));
        countText = text("0 آهنگ", 12, secondary(), false);
        listHeader.addView(countText);
        root.addView(listHeader);

        musicList = new ListView(this);
        musicList.setDividerHeight(1);
        musicList.setBackground(rounded(PANEL, 18));
        musicList.setPadding(4, 4, 4, 4);
        musicList.setSelector(android.R.color.transparent);
        root.addView(musicList, new LinearLayout.LayoutParams(
                -1, 0, 1));

        musicList.setOnItemClickListener((parent, view, position, id) ->
                playSong(position));

        LinearLayout bottom = row();
        Button about = button("ⓘ درباره سازنده");
        Button refresh = button("⟳ تازه‌سازی");
        about.setOnClickListener(v -> showAbout());
        refresh.setOnClickListener(v -> loadMusic());
        bottom.addView(about, new LinearLayout.LayoutParams(0, 48, 1));
        bottom.addView(refresh, new LinearLayout.LayoutParams(0, 48, 1));
        root.addView(bottom);
    }

    private void loadMusic() {
        songNames.clear();
        songArtists.clear();
        songUris.clear();

        String[] projection = {
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST
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
                int artistColumn = cursor.getColumnIndex(
                        MediaStore.Audio.Media.ARTIST);

                while (cursor.moveToNext()) {
                    long id = cursor.getLong(idColumn);
                    String title = cursor.getString(titleColumn);
                    String artist = artistColumn >= 0
                            ? cursor.getString(artistColumn) : "";

                    if (title == null || title.trim().isEmpty()) {
                        title = "آهنگ بدون نام";
                    }
                    if (artist == null || artist.equals("<unknown>")) {
                        artist = "هنرمند ناشناس";
                    }

                    songNames.add(title);
                    songArtists.add(artist);
                    songUris.add(Uri.withAppendedPath(
                            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                            String.valueOf(id)));
                }
            }
        } catch (Exception e) {
            Toast.makeText(this, "خواندن آهنگ‌ها با مشکل روبه‌رو شد",
                    Toast.LENGTH_LONG).show();
        }

        if (musicList != null) {
            ArrayList<String> labels = new ArrayList<>();
            for (int i = 0; i < songNames.size(); i++) {
                labels.add("♫  " + songNames.get(i) + "\n     "
                        + songArtists.get(i));
            }

            ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                    this, android.R.layout.simple_list_item_1, labels) {
                @Override
                public View getView(int position, View convertView,
                                    android.view.ViewGroup parent) {
                    TextView t = (TextView) super.getView(
                            position, convertView, parent);
                    t.setTextColor(foreground());
                    t.setTextSize(14);
                    t.setPadding(14, 12, 14, 12);
                    t.setBackgroundColor(Color.TRANSPARENT);
                    return t;
                }
            };
            musicList.setAdapter(adapter);
            countText.setText(songNames.size() + " آهنگ");
        }

        if (songNames.isEmpty()) {
            Toast.makeText(this,
                    "آهنگی پیدا نشد؛ فایل‌های موزیک را در گوشی بررسی کن",
                    Toast.LENGTH_LONG).show();
        }
    }

    private void playSong(int position) {
        if (position < 0 || position >= songUris.size()) return;

        stopPlayer();
        currentIndex = position;
        prepared = false;

        titleText.setText(songNames.get(position));
        artistText.setText(songArtists.get(position));
        playButton.setText("…");
        progressBar.setProgress(0);
        timeText.setText("00:00 / 00:00");

        try {
            player = new MediaPlayer();
            player.setDataSource(this, songUris.get(position));
            player.setOnPreparedListener(mp -> {
                prepared = true;
                mp.start();
                playButton.setText("⏸ توقف");
                progressBar.setMax(Math.max(1, mp.getDuration()));
                timeText.setText("00:00 / " + formatTime(mp.getDuration()));
            });
            player.setOnCompletionListener(mp -> playAdjacent(1));
            player.setOnErrorListener((mp, what, extra) -> {
                stopPlayer();
                playButton.setText("▶ پخش");
                Toast.makeText(this, "پخش این آهنگ ممکن نشد",
                        Toast.LENGTH_LONG).show();
                return true;
            });
            player.prepareAsync();
        } catch (Exception e) {
            stopPlayer();
            playButton.setText("▶ پخش");
            Toast.makeText(this, "خطا در پخش آهنگ",
                    Toast.LENGTH_LONG).show();
        }
    }

    private void togglePlayback() {
        if (player != null && prepared) {
            try {
                if (player.isPlaying()) {
                    player.pause();
                    playButton.setText("▶ پخش");
                } else {
                    player.start();
                    playButton.setText("⏸ توقف");
                }
            } catch (Exception ignored) {
            }
        } else if (!songUris.isEmpty()) {
            playSong(currentIndex >= 0 ? currentIndex : 0);
        } else {
            Toast.makeText(this, "ابتدا آهنگ‌ها را تازه‌سازی کن",
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void playAdjacent(int step) {
        if (songUris.isEmpty()) return;
        int nextIndex = currentIndex < 0 ? 0
                : (currentIndex + step + songUris.size()) % songUris.size();
        playSong(nextIndex);
    }

    private void stopPlayer() {
        prepared = false;
        if (player != null) {
            try {
                player.reset();
                player.release();
            } catch (Exception ignored) {
            }
            player = null;
        }
    }

    private String formatTime(int millis) {
        int seconds = Math.max(0, millis / 1000);
        return String.format(Locale.getDefault(), "%02d:%02d",
                seconds / 60, seconds % 60);
    }

    private void showSettings() {
        setupRoot();
        root.addView(text("⚙ تنظیمات", 26, foreground(), true));
        root.addView(text("ظاهر برنامه و گزینه‌های ساده", 14, secondary(), false));

        themeButton = button(darkTheme
                ? "☀ فعال کردن تم روشن"
                : "🌙 فعال کردن تم تیره");
        themeButton.setOnClickListener(v -> {
            darkTheme = !darkTheme;
            showSettings();
        });
        root.addView(themeButton, new LinearLayout.LayoutParams(-1, 54));

        Button reload = button("⟳ تازه‌سازی فهرست آهنگ‌ها");
        reload.setOnClickListener(v -> {
            showHome();
            loadMusic();
        });
        root.addView(reload, new LinearLayout.LayoutParams(-1, 54));

        Button about = button("ⓘ درباره سازنده");
        about.setOnClickListener(v -> showAbout());
        root.addView(about, new LinearLayout.LayoutParams(-1, 54));

        Button back = button("بازگشت به صفحه اصلی");
        back.setOnClickListener(v -> showHome());
        root.addView(back, new LinearLayout.LayoutParams(-1, 54));
    }

    private void showAbout() {
        setupRoot();
        root.setGravity(Gravity.CENTER_HORIZONTAL);

        TextView logo = text("♫", 62, WHITE, true);
        GradientDrawable gradient = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{PURPLE, Color.rgb(52, 34, 91)}
        );
        gradient.setCornerRadius(32);
        logo.setBackground(gradient);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(150, 150);
        lp.setMargins(0, 30, 0, 18);
        root.addView(logo, lp);

        root.addView(text("موزیک من", 27, foreground(), true));
        root.addView(text("پخش‌کننده موزیک آفلاین", 15, secondary(), false));

        TextView developer = text("سازنده: مرتضی قادری", 20,
                foreground(), true);
        developer.setPadding(8, 24, 8, 12);
        root.addView(developer);

        TextView services = text(
                "هر نوع برنامه‌ای یا رباتی نیاز داشته باشید،\nدر خدمت شما هستم.",
                16, secondary(), false);
        root.addView(services);

        TextView telegram = text("@Merc_ifulGod", 19,
                Color.rgb(90, 180, 255), true);
        root.addView(telegram);

        Button contact = button("✈ ارتباط با مرتضی قادری در تلگرام");
        contact.setBackground(rounded(PURPLE, 26));
        contact.setOnClickListener(v -> {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://t.me/Merc_ifulGod")));
            } catch (Exception e) {
                Toast.makeText(this, "باز کردن تلگرام ممکن نشد",
                        Toast.LENGTH_SHORT).show();
            }
        });
        root.addView(contact, new LinearLayout.LayoutParams(-1, 56));

        root.addView(text("نسخه 1.0", 13, secondary(), false));

        Button back = button("بازگشت به موزیک");
        back.setOnClickListener(v -> showHome());
        LinearLayout.LayoutParams backLp =
                new LinearLayout.LayoutParams(-1, 54);
        backLp.setMargins(0, 22, 0, 0);
        root.addView(back, backLp);
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_AUDIO && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            loadMusic();
        } else if (requestCode == REQUEST_AUDIO) {
            Toast.makeText(this, "برای نمایش آهنگ‌ها اجازه دسترسی لازم است",
                    Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onBackPressed() {
        showHome();
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacks(progressUpdater);
        stopPlayer();
        super.onDestroy();
    }
}
