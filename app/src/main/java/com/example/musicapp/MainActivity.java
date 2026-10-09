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
import android.widget.ScrollView;
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
    private boolean english = false;
    private String selectedTheme = "purple";

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
        android.content.SharedPreferences prefs = getPreferences(MODE_PRIVATE);
        darkTheme = prefs.getBoolean("darkTheme", true);
        english = prefs.getBoolean("english", false);
        selectedTheme = prefs.getString("selectedTheme", "purple");
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


    private String tr(String fa, String en) {
        return english ? en : fa;
    }

    private void saveSettings() {
        getPreferences(MODE_PRIVATE).edit()
                .putBoolean("darkTheme", darkTheme)
                .putBoolean("english", english)
                .putString("selectedTheme", selectedTheme)
                .apply();
    }

    private int accentColor() {
        switch (selectedTheme) {
            case "blue": return Color.rgb(55, 145, 255);
            case "green": return Color.rgb(40, 190, 120);
            case "red": return Color.rgb(240, 75, 85);
            case "pink": return Color.rgb(245, 90, 170);
            case "orange": return Color.rgb(255, 145, 45);
            default: return Color.rgb(157, 104, 255);
        }
    }

    private void chooseTheme(String theme) {
        selectedTheme = theme;
        saveSettings();
        showSettings();
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
        b.setBackground(rounded(accentColor(), 24));
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

        root.addView(text(tr("پخش‌کننده موزیک آفلاین", "Offline Music Player"), 13, secondary(), false));

        coverText = text("♫", 76, WHITE, true);
        GradientDrawable coverBg = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{accentColor(), Color.rgb(62, 35, 106), Color.rgb(22, 19, 39)}
        );
        coverBg.setCornerRadius(36);
        coverText.setBackground(coverBg);
        LinearLayout.LayoutParams coverParams =
                new LinearLayout.LayoutParams(-1, 155);
        coverParams.setMargins(0, 12, 0, 12);
        root.addView(coverText, coverParams);

        titleText = text(tr("آهنگی انتخاب نشده", "No song selected"), 19, foreground(), true);
        root.addView(titleText);
        artistText = text(tr("برای شروع، یک آهنگ انتخاب کن", "Choose a song to start"), 13, secondary(), false);
        root.addView(artistText);

        progressBar = new SeekBar(this);
        progressBar.setProgressTintList(
                android.content.res.ColorStateList.valueOf(accentColor()));
        progressBar.setThumbTintList(
                android.content.res.ColorStateList.valueOf(accentColor()));
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
        Button previous = button(tr("⏮ قبلی", "⏮ Previous"));
        playButton = button(tr("▶ پخش", "▶ Play"));
        Button next = button(tr("Next ⏭", "Next ⏭"));

        previous.setOnClickListener(v -> playAdjacent(-1));
        playButton.setOnClickListener(v -> togglePlayback());
        next.setOnClickListener(v -> playAdjacent(1));

        controls.addView(previous, new LinearLayout.LayoutParams(0, 54, 1));
        controls.addView(playButton, new LinearLayout.LayoutParams(0, 54, 1));
        controls.addView(next, new LinearLayout.LayoutParams(0, 54, 1));
        root.addView(controls);

        LinearLayout listHeader = row();
        listHeader.addView(text(tr("🎵 آهنگ‌های من", "🎵 My Music"), 17, foreground(), true),
                new LinearLayout.LayoutParams(0, -2, 1));
        countText = text(tr("0 آهنگ", "0 songs"), 12, secondary(), false);
        listHeader.addView(countText);
        root.addView(listHeader);

        musicList = new ListView(this);
        musicList.setDividerHeight(1);
        musicList.setBackground(rounded(darkTheme ? PANEL : Color.WHITE, 18));
        musicList.setPadding(4, 4, 4, 4);
        musicList.setSelector(android.R.color.transparent);
        LinearLayout.LayoutParams musicListParams =
                new LinearLayout.LayoutParams(-1, 0, 1f);
        musicListParams.setMargins(0, 8, 0, 8);
        root.addView(musicList, musicListParams);
        musicList.setVisibility(View.VISIBLE);

        musicList.setOnItemClickListener((parent, view, position, id) ->
                playSong(position));

        LinearLayout bottom = row();
        Button about = button(tr("ⓘ درباره سازنده", "ⓘ About"));
        Button refresh = button(tr("⟳ تازه‌سازی", "⟳ Refresh"));
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

    private void startBackgroundSong(int position) {
        if (position < 0 || position >= songUris.size()) return;

        Intent intent = new Intent(this, MusicService.class);
        intent.setAction(MusicService.ACTION_PLAY);
        intent.putExtra(MusicService.EXTRA_URI, songUris.get(position).toString());
        intent.putExtra(MusicService.EXTRA_TITLE, songNames.get(position));
        intent.putExtra(MusicService.EXTRA_ARTIST, songArtists.get(position));

        if (Build.VERSION.SDK_INT >= 26) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
    }

    private void playSong(int position) {
        if (position < 0 || position >= songUris.size()) return;

        stopPlayer();
        currentIndex = position;
        startBackgroundSong(position);
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
            player.setOnCompletionListener(mp -> {
                if (songUris.size() > 1) {
                    playAdjacent(1);
                } else {
                    playButton.setText("▶ پخش");
                }
            });
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

        ScrollView scroll = new ScrollView(this);
        LinearLayout content = column();
        content.setPadding(0, 8, 0, 24);
        scroll.addView(content);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

        content.addView(text(tr("⚙ تنظیمات", "⚙ Settings"),
                26, foreground(), true));
        content.addView(text(tr("زبان، رنگ و ظاهر برنامه", "Language, colors and appearance"),
                14, secondary(), false));

        Button language = button(english
                ? "🌐 Switch to Persian / تغییر به فارسی"
                : "🌐 English / تغییر زبان به انگلیسی");
        language.setOnClickListener(v -> {
            english = !english;
            saveSettings();
            showSettings();
        });
        LinearLayout.LayoutParams languageLp = new LinearLayout.LayoutParams(-1, 54);
        languageLp.setMargins(0, 14, 0, 18);
        content.addView(language, languageLp);

        content.addView(text(tr("🎨 انتخاب رنگ برنامه", "🎨 Choose app color"),
                18, foreground(), true));

        String[] namesFa = {"بنفش", "آبی", "سبز", "قرمز", "صورتی", "نارنجی"};
        String[] namesEn = {"Purple", "Blue", "Green", "Red", "Pink", "Orange"};
        String[] keys = {"purple", "blue", "green", "red", "pink", "orange"};
        int[] colors = {
                Color.rgb(157, 104, 255),
                Color.rgb(55, 145, 255),
                Color.rgb(40, 190, 120),
                Color.rgb(240, 75, 85),
                Color.rgb(245, 90, 170),
                Color.rgb(255, 145, 45)
        };

        for (int i = 0; i < keys.length; i++) {
            final String key = keys[i];
            Button colorButton = button(
                    (selectedTheme.equals(key) ? "✓ " : "   ")
                    + (english ? namesEn[i] : namesFa[i]));
            colorButton.setBackground(rounded(colors[i], 24));
            colorButton.setOnClickListener(v -> chooseTheme(key));
            LinearLayout.LayoutParams colorLp = new LinearLayout.LayoutParams(-1, 48);
            colorLp.setMargins(0, 6, 0, 6);
            content.addView(colorButton, colorLp);
        }

        Button theme = button(darkTheme
                ? tr("☀ فعال کردن تم روشن", "☀ Enable light mode")
                : tr("🌙 فعال کردن تم تیره", "🌙 Enable dark mode"));
        theme.setOnClickListener(v -> {
            darkTheme = !darkTheme;
            saveSettings();
            showSettings();
        });
        LinearLayout.LayoutParams themeLp = new LinearLayout.LayoutParams(-1, 54);
        themeLp.setMargins(0, 18, 0, 14);
        content.addView(theme, themeLp);

        Button channel = button(tr("📢 کانال تلگرام ما", "📢 Our Telegram Channel"));
        channel.setOnClickListener(v -> {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://t.me/Scam0093")));
            } catch (Exception e) {
                Toast.makeText(this,
                        tr("باز کردن لینک ممکن نشد", "Could not open the link"),
                        Toast.LENGTH_SHORT).show();
            }
        });
        LinearLayout.LayoutParams channelLp = new LinearLayout.LayoutParams(-1, 54);
        channelLp.setMargins(0, 8, 0, 8);
        content.addView(channel, channelLp);

        Button support = button(tr("🎧 پشتیبانی", "🎧 Support"));
        support.setOnClickListener(v -> {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://t.me/Merc_ifulGod")));
            } catch (Exception e) {
                Toast.makeText(this,
                        tr("باز کردن تلگرام ممکن نشد", "Could not open Telegram"),
                        Toast.LENGTH_SHORT).show();
            }
        });
        LinearLayout.LayoutParams supportLp = new LinearLayout.LayoutParams(-1, 54);
        supportLp.setMargins(0, 8, 0, 8);
        content.addView(support, supportLp);

        Button reload = button(tr("⟳ تازه‌سازی فهرست آهنگ‌ها", "⟳ Refresh song list"));
        reload.setOnClickListener(v -> {
            showHome();
            loadMusic();
        });
        LinearLayout.LayoutParams reloadLp = new LinearLayout.LayoutParams(-1, 54);
        reloadLp.setMargins(0, 8, 0, 8);
        content.addView(reload, reloadLp);

        Button about = button(tr("ⓘ درباره سازنده", "ⓘ About the developer"));
        about.setOnClickListener(v -> showAbout());
        LinearLayout.LayoutParams aboutLp = new LinearLayout.LayoutParams(-1, 54);
        aboutLp.setMargins(0, 8, 0, 8);
        content.addView(about, aboutLp);

        Button back = button(tr("بازگشت به صفحه اصلی", "Back to Home"));
        back.setOnClickListener(v -> showHome());
        LinearLayout.LayoutParams backLp = new LinearLayout.LayoutParams(-1, 54);
        backLp.setMargins(0, 8, 0, 8);
        content.addView(back, backLp);
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
