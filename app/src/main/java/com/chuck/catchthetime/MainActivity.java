package com.chuck.catchthetime;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

public class MainActivity extends Activity {
    private static final int NOTIFICATION_PERMISSION_REQUEST = 1001;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable minuteTick = new Runnable() {
        @Override public void run() {
            if ("home".equals(page) || "times".equals(page)) rebuildUi();
            long now = System.currentTimeMillis();
            handler.postDelayed(this, 60_000L - (now % 60_000L) + 120L);
        }
    };

    private ThemeConfig theme;
    private FontProfile fonts;
    private String page = "home";

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        NotificationHelper.ensureAllChannels(this);
        rebuildUi();
    }

    @Override protected void onResume() {
        super.onResume();
        if (AlarmScheduler.isEnabled(this) && AlarmScheduler.canScheduleExact(this)) {
            AlarmScheduler.scheduleNext(this);
        }
        CatchWidgetUpdater.updateAll(this);
        handler.removeCallbacks(minuteTick);
        handler.post(minuteTick);
    }

    @Override protected void onPause() {
        handler.removeCallbacks(minuteTick);
        super.onPause();
    }

    private void rebuildUi() {
        theme = ThemeConfig.load(this);
        fonts = FontProfile.load(this);
        applySystemBars();
        setContentView(buildScreen());
    }

    private void applySystemBars() {
        getWindow().setStatusBarColor(theme.background);
        getWindow().setNavigationBarColor(theme.background);
        int flags = 0;
        if (!theme.isDark()) {
            flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            flags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
        }
        getWindow().getDecorView().setSystemUiVisibility(flags);
    }

    private View buildScreen() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        GradientDrawable backdrop = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{blend(theme.background, theme.accent, 0.035f), theme.background}
        );
        scroll.setBackground(backdrop);

        LinearLayout root = column();
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(dp(18), dp(12), dp(18), dp(32));
        scroll.addView(root, matchWrap());

        root.addView(header(), matchWrap());
        LinearLayout.LayoutParams np = matchWrap();
        np.setMargins(0, dp(12), 0, 0);
        root.addView(navBar(), np);

        View content;
        switch (page) {
            case "times": content = buildTimesPage(); break;
            case "style": content = buildStylePage(); break;
            case "settings": content = buildSettingsPage(); break;
            default: content = buildHomePage();
        }
        LinearLayout.LayoutParams cp = matchWrap();
        cp.setMargins(0, dp(14), 0, 0);
        root.addView(content, cp);
        return scroll;
    }

    private View header() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(2), dp(2), dp(2), dp(2));

        HandCatchView mark = new HandCatchView(this);
        row.addView(mark, new LinearLayout.LayoutParams(dp(54), dp(54)));

        LinearLayout words = column();
        LinearLayout.LayoutParams wp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        wp.setMargins(dp(10), 0, 0, 0);
        TextView title = text("CATCH THE TIME", 22, theme.text, fonts.display);
        title.setLetterSpacing(fonts.titleSpacing);
        words.addView(title, matchWrap());
        TextView tiny = text("v3.0", 11, theme.muted, fonts.body);
        tiny.setLetterSpacing(0.08f);
        words.addView(tiny, matchWrap());
        row.addView(words, wp);
        return row;
    }

    private View navBar() {
        LinearLayout shell = new LinearLayout(this);
        shell.setOrientation(LinearLayout.HORIZONTAL);
        shell.setGravity(Gravity.CENTER);
        shell.setPadding(dp(4), dp(4), dp(4), dp(4));
        shell.setBackground(roundRect(theme.surface, dp(17), blend(theme.surface, theme.text, 0.08f), 1));
        shell.setElevation(dp(1));

        String[][] tabs = {{"home","Now"},{"times","Times"},{"style","Style"},{"settings","Setup"}};
        for (String[] tab : tabs) {
            boolean selected = page.equals(tab[0]);
            Button b = pill(tab[1], selected ? theme.accent : Color.TRANSPARENT,
                    selected ? contrast(theme.accent) : theme.muted, 13, 13);
            b.setTypeface(fonts.medium);
            b.setOnClickListener(v -> {
                page = tab[0];
                v.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
                rebuildUi();
            });
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, dp(42), 1f);
            if (shell.getChildCount() > 0) p.setMargins(dp(3), 0, 0, 0);
            shell.addView(b, p);
        }
        return shell;
    }

    private View buildHomePage() {
        LinearLayout root = column();
        LocalDateTime now = LocalDateTime.now();
        List<LocalTime> times = CatchCatalog.activeTimes(this);
        int current = CatchCatalog.currentIndex(this, now);
        LocalDateTime next = CatchCatalog.nextAfter(this, now);

        LinearLayout hero = heroCard();
        hero.setGravity(Gravity.CENTER_HORIZONTAL);
        hero.setPadding(dp(20), dp(24), dp(20), dp(22));

        String topLabel;
        int heroColor;
        String shownTime;
        if (current >= 0) {
            topLabel = "CATCH IT";
            heroColor = theme.current;
            shownTime = CatchCatalog.HHMM.format(times.get(current));
        } else if (next != null) {
            topLabel = next.toLocalDate().equals(now.toLocalDate()) ? "NEXT CATCH" : "NEXT · TOMORROW";
            heroColor = theme.next;
            shownTime = CatchCatalog.HHMM.format(next);
        } else {
            topLabel = "NO ACTIVE TIMES";
            heroColor = theme.muted;
            shownTime = "--:--";
        }

        TextView eyebrow = eyebrow(topLabel, heroColor);
        eyebrow.setGravity(Gravity.CENTER);
        hero.addView(eyebrow, matchWrap());

        TextView time = text(shownTime, 64, heroColor, fonts.time);
        time.setGravity(Gravity.CENTER);
        time.setLetterSpacing(fonts.timeSpacing);
        LinearLayout.LayoutParams timeP = matchWrap();
        timeP.setMargins(0, dp(2), 0, dp(2));
        hero.addView(time, timeP);

        int remaining = CatchCatalog.remainingToday(this, now);
        String remainingLabel = remaining == 1 ? "1 moment remaining today" : remaining + " moments remaining today";
        TextView rem = text(remainingLabel, 13, theme.muted, fonts.body);
        rem.setGravity(Gravity.CENTER);
        hero.addView(rem, matchWrap());

        if (current >= 0) {
            LinearLayout actions = new LinearLayout(this);
            actions.setOrientation(LinearLayout.HORIZONTAL);
            actions.setGravity(Gravity.CENTER);
            Button copy = pill("Copy", theme.surface, theme.text, 13, 14);
            Button share = pill("Share", theme.accent, contrast(theme.accent), 13, 14);
            copy.setOnClickListener(v -> copyCatch(times.get(current), v));
            share.setOnClickListener(v -> shareCatch(times.get(current), v));
            actions.addView(copy, new LinearLayout.LayoutParams(0, dp(46), 1f));
            LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(0, dp(46), 1f);
            bp.setMargins(dp(8), 0, 0, 0);
            actions.addView(share, bp);
            LinearLayout.LayoutParams ap = matchWrap();
            ap.setMargins(0, dp(16), 0, 0);
            hero.addView(actions, ap);
        }
        root.addView(hero, fullCardParams());

        if (!SettingsStore.compactHome(this)) {
            root.addView(buildUpcomingCard(now, times, next), fullCardParams());
        }
        root.addView(buildStatsCard(), fullCardParams());

        // Deliberately last. This is setup/status information, not the point of the Now screen.
        root.addView(buildArmedCard(), fullCardParams());
        return root;
    }

    private View buildUpcomingCard(LocalDateTime now, List<LocalTime> times, LocalDateTime next) {
        LinearLayout c = card();
        c.addView(sectionTitle("UPCOMING"), matchWrap());
        if (times.isEmpty()) {
            c.addView(body("No active catch times. Add one in Times."), matchWrap());
            return c;
        }

        LocalTime minute = now.toLocalTime().withSecond(0).withNano(0);
        int shown = 0;
        for (LocalTime t : times) {
            if (!t.isBefore(minute) && shown < 5) {
                boolean isNow = t.equals(minute);
                c.addView(timeRow(t, isNow ? "NOW" : "TODAY", isNow ? theme.current : theme.time), matchWrap());
                shown++;
            }
        }
        if (shown == 0 && next != null) {
            c.addView(timeRow(next.toLocalTime(), "TOMORROW", theme.next), matchWrap());
        }
        return c;
    }

    private View buildArmedCard() {
        LinearLayout c = quietCard();
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout labels = column();
        TextView title = text("Catch notifications", 15, theme.text, fonts.medium);
        TextView sub = text(statusLine(), 11, statusColor(), fonts.body);
        labels.addView(title, matchWrap());
        labels.addView(sub, matchWrap());
        row.addView(labels, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        Switch sw = switchView(AlarmScheduler.isEnabled(this));
        sw.setOnCheckedChangeListener((buttonView, isChecked) -> {
            AlarmScheduler.setEnabled(this, isChecked);
            if (isChecked) ensurePermissions();
            buttonView.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
            rebuildUi();
        });
        row.addView(sw, new LinearLayout.LayoutParams(dp(60), dp(46)));
        c.addView(row, matchWrap());
        return c;
    }

    private View buildStatsCard() {
        LinearLayout c = card();
        c.addView(sectionTitle("ACTIVITY"), matchWrap());
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER);
        row.addView(stat("TODAY", String.valueOf(StatsStore.today(this))), new LinearLayout.LayoutParams(0, dp(72), 1f));
        row.addView(stat("TOTAL", String.valueOf(StatsStore.total(this))), new LinearLayout.LayoutParams(0, dp(72), 1f));
        row.addView(stat("LAST", StatsStore.last(this)), new LinearLayout.LayoutParams(0, dp(72), 1f));
        c.addView(row, matchWrap());
        return c;
    }

    private View stat(String label, String value) {
        LinearLayout box = column();
        box.setGravity(Gravity.CENTER);
        TextView v = text(value, 21, theme.time, fonts.time);
        v.setGravity(Gravity.CENTER);
        v.setLetterSpacing(fonts.timeSpacing);
        TextView l = eyebrow(label, theme.muted);
        l.setGravity(Gravity.CENTER);
        box.addView(v, matchWrap());
        box.addView(l, matchWrap());
        return box;
    }

    private View buildTimesPage() {
        LinearLayout root = column();

        LinearLayout modeCard = card();
        modeCard.addView(sectionTitle("TIME SOURCE"), matchWrap());
        TextView intro = body("Use the built-in patterns, your own times, or both.");
        LinearLayout.LayoutParams introP = matchWrap();
        introP.setMargins(0, 0, 0, dp(12));
        modeCard.addView(intro, introP);
        modeCard.addView(modeSelector(), matchWrap());
        root.addView(modeCard, fullCardParams());

        LinearLayout packs = card();
        packs.addView(sectionTitle("DEFAULT PATTERNS"), matchWrap());
        packs.addView(packSwitch("Repeats", "repeats", "11:11 · 22:22 · matching digits"), matchWrap());
        packs.addView(packSwitch("Runs up", "ascending", "01:23 · 12:34 · 23:45"), matchWrap());
        packs.addView(packSwitch("Runs down", "descending", "03:21 · 04:32 · 06:54"), matchWrap());
        packs.addView(packSwitch("Mirrors", "mirrors", "10:01 · 12:21 · 23:32"), matchWrap());
        root.addView(packs, fullCardParams());

        LinearLayout custom = card();
        LinearLayout customHeader = new LinearLayout(this);
        customHeader.setOrientation(LinearLayout.HORIZONTAL);
        customHeader.setGravity(Gravity.CENTER_VERTICAL);
        customHeader.addView(sectionTitle("CUSTOM TIMES"), new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        Button add = pill("+ Add", theme.accent, contrast(theme.accent), 13, 14);
        add.setOnClickListener(v -> addCustomTime());
        customHeader.addView(add, new LinearLayout.LayoutParams(dp(90), dp(42)));
        custom.addView(customHeader, matchWrap());

        List<String> customTimes = SettingsStore.customTimesSorted(this);
        if (customTimes.isEmpty()) {
            TextView empty = body("No custom times yet. Add any minute you fancy.");
            LinearLayout.LayoutParams ep = matchWrap();
            ep.setMargins(0, dp(8), 0, 0);
            custom.addView(empty, ep);
        } else {
            for (String s : customTimes) custom.addView(customTimeRow(s), matchWrap());
        }
        root.addView(custom, fullCardParams());

        LinearLayout timeline = card();
        timeline.addView(sectionTitle("TODAY'S TIMELINE"), matchWrap());
        List<LocalTime> active = CatchCatalog.activeTimes(this);
        LocalDateTime now = LocalDateTime.now();
        LocalTime minute = now.toLocalTime().withSecond(0).withNano(0);
        LocalDateTime next = CatchCatalog.nextAfter(this, now);
        Set<String> customs = SettingsStore.customTimes(this);
        if (active.isEmpty()) timeline.addView(body("Nothing is active right now."), matchWrap());
        for (LocalTime t : active) {
            String badge;
            int color;
            if (t.equals(minute)) {
                badge = "NOW";
                color = theme.current;
            } else if (next != null && t.equals(next.toLocalTime()) && next.toLocalDate().equals(now.toLocalDate())) {
                badge = "NEXT";
                color = theme.next;
            } else if (t.isBefore(minute)) {
                badge = "PAST";
                color = theme.muted;
            } else {
                badge = customs.contains(CatchCatalog.HHMM.format(t)) ? "CUSTOM" : "TODAY";
                color = theme.time;
            }
            timeline.addView(timeRow(t, badge, color), matchWrap());
        }
        root.addView(timeline, fullCardParams());
        return root;
    }

    private View modeSelector() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        String mode = SettingsStore.mode(this);
        String[][] modes = {{"defaults","Defaults"},{"mixed","Mixed"},{"custom","Custom"}};
        for (String[] m : modes) {
            boolean selected = mode.equals(m[0]);
            Button b = pill(m[1], selected ? theme.accent : theme.background,
                    selected ? contrast(theme.accent) : theme.text, 12, 13);
            b.setOnClickListener(v -> {
                SettingsStore.prefs(this).edit().putString(SettingsStore.KEY_MODE, m[0]).apply();
                AlarmScheduler.scheduleNext(this);
                CatchWidgetUpdater.updateAll(this);
                rebuildUi();
            });
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, dp(44), 1f);
            if (row.getChildCount() > 0) p.setMargins(dp(6), 0, 0, 0);
            row.addView(b, p);
        }
        return row;
    }

    private View packSwitch(String title, String id, String summary) {
        LinearLayout row = settingRowBase();
        LinearLayout labels = column();
        labels.addView(text(title, 15, theme.text, fonts.medium), matchWrap());
        labels.addView(text(summary, 12, theme.muted, fonts.body), matchWrap());
        row.addView(labels, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        Switch sw = switchView(SettingsStore.packEnabled(this, id));
        sw.setOnCheckedChangeListener((button, checked) -> {
            String key = "repeats".equals(id) ? SettingsStore.KEY_PACK_REPEATS
                    : "ascending".equals(id) ? SettingsStore.KEY_PACK_ASC
                    : "descending".equals(id) ? SettingsStore.KEY_PACK_DESC
                    : SettingsStore.KEY_PACK_MIRROR;
            SettingsStore.prefs(this).edit().putBoolean(key, checked).apply();
            AlarmScheduler.scheduleNext(this);
            CatchWidgetUpdater.updateAll(this);
            rebuildUi();
        });
        row.addView(sw, new LinearLayout.LayoutParams(dp(60), dp(46)));
        return row;
    }

    private View customTimeRow(String value) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(2), dp(5), dp(2), dp(5));

        TextView time = text(value, 20, theme.time, fonts.time);
        time.setLetterSpacing(fonts.timeSpacing);
        row.addView(time, new LinearLayout.LayoutParams(0, dp(42), 1f));
        Button remove = pill("Remove", theme.background, theme.muted, 12, 12);
        remove.setOnClickListener(v -> {
            Set<String> set = SettingsStore.customTimes(this);
            set.remove(value);
            SettingsStore.setCustomTimes(this, set);
            AlarmScheduler.scheduleNext(this);
            CatchWidgetUpdater.updateAll(this);
            rebuildUi();
        });
        row.addView(remove, new LinearLayout.LayoutParams(dp(96), dp(40)));
        return row;
    }

    private void addCustomTime() {
        LocalTime now = LocalTime.now();
        new TimePickerDialog(this, (view, hour, minute) -> {
            String value = String.format("%02d:%02d", hour, minute);
            Set<String> set = SettingsStore.customTimes(this);
            set.add(value);
            SettingsStore.setCustomTimes(this, set);
            AlarmScheduler.scheduleNext(this);
            CatchWidgetUpdater.updateAll(this);
            rebuildUi();
        }, now.getHour(), now.getMinute(), true).show();
    }

    private View buildStylePage() {
        LinearLayout root = column();

        LinearLayout fontCard = card();
        fontCard.addView(sectionTitle("TYPE"), matchWrap());
        TextView fdesc = body("Font choices are stored per device, so the Pocket can go retro while the KPF goes full future-console.");
        LinearLayout.LayoutParams fdp = matchWrap();
        fdp.setMargins(0, 0, 0, dp(12));
        fontCard.addView(fdesc, fdp);

        String[][] fontIds = {{"clean","Clean"},{"neo","Neo"},{"retro","Retro"},{"classic","Classic"}};
        for (int r = 0; r < 2; r++) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            for (int i = r * 2; i < r * 2 + 2; i++) {
                String id = fontIds[i][0];
                FontProfile preview = FontProfile.forId(id);
                boolean selected = SettingsStore.fontId(this).equals(id);
                Button b = pill((selected ? "✓ " : "") + fontIds[i][1] + "  12:34",
                        selected ? blend(theme.surface, theme.accent, 0.16f) : theme.background,
                        selected ? theme.accent : theme.text, 13, 14);
                b.setTypeface(preview.time);
                b.setLetterSpacing(preview.timeSpacing);
                b.setOnClickListener(v -> {
                    SettingsStore.prefs(this).edit().putString(SettingsStore.KEY_FONT, id).apply();
                    rebuildUi();
                });
                LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, dp(50), 1f);
                if (row.getChildCount() > 0) p.setMargins(dp(8), 0, 0, 0);
                row.addView(b, p);
            }
            LinearLayout.LayoutParams rp = matchWrap();
            if (r > 0) rp.setMargins(0, dp(8), 0, 0);
            fontCard.addView(row, rp);
        }
        root.addView(fontCard, fullCardParams());

        LinearLayout presets = card();
        presets.addView(sectionTitle("PALETTE"), matchWrap());
        TextView pdesc = body("Colour stays local to each device unless you deliberately export the setup.");
        LinearLayout.LayoutParams pdp = matchWrap();
        pdp.setMargins(0, 0, 0, dp(12));
        presets.addView(pdesc, pdp);

        String[][] paletteIds = {
                {"midnight","Obsidian"},{"pocket","Pocket"},{"neon","Neon"},
                {"amber","Amber"},{"paper","Paper"},{"vapor","Vapor"}
        };
        String selectedPreset = SettingsStore.prefs(this).getString(ThemeConfig.KEY_PRESET, "midnight");
        for (int r = 0; r < 2; r++) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            for (int i = r * 3; i < r * 3 + 3; i++) {
                String id = paletteIds[i][0];
                String label = paletteIds[i][1];
                ThemeConfig preview = ThemeConfig.preset(id);
                boolean selected = id.equals(selectedPreset);
                Button b = pill((selected ? "✓ " : "") + label, preview.surface,
                        selected ? preview.accent : preview.text, 11, 14);
                b.setOnClickListener(v -> {
                    SettingsStore.prefs(this).edit().putString(ThemeConfig.KEY_PRESET, id).apply();
                    CatchWidgetUpdater.updateAll(this);
                    rebuildUi();
                });
                LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, dp(48), 1f);
                if (row.getChildCount() > 0) p.setMargins(dp(7), 0, 0, 0);
                row.addView(b, p);
            }
            LinearLayout.LayoutParams rp = matchWrap();
            if (r > 0) rp.setMargins(0, dp(8), 0, 0);
            presets.addView(row, rp);
        }

        Button custom = pill("Edit custom palette", theme.background, theme.text, 13, 14);
        custom.setOnClickListener(v -> showPaletteEditor());
        LinearLayout.LayoutParams customP = matchWrap();
        customP.setMargins(0, dp(12), 0, 0);
        presets.addView(custom, customP);
        root.addView(presets, fullCardParams());

        LinearLayout preview = heroCard();
        preview.setGravity(Gravity.CENTER_HORIZONTAL);
        preview.addView(eyebrow("PREVIEW", theme.accent), matchWrap());
        TextView demo = text("12:34", 52, theme.time, fonts.time);
        demo.setLetterSpacing(fonts.timeSpacing);
        demo.setGravity(Gravity.CENTER);
        preview.addView(demo, matchWrap());
        TextView phrase = text(SettingsStore.phrase(this), 15, theme.text, fonts.body);
        phrase.setGravity(Gravity.CENTER);
        preview.addView(phrase, matchWrap());
        root.addView(preview, fullCardParams());
        return root;
    }

    private void showPaletteEditor() {
        ThemeConfig base = theme;
        LinearLayout fields = column();
        fields.setPadding(dp(18), dp(4), dp(18), dp(4));
        String[] labels = {"Background", "Cards", "Primary text", "Time", "Muted text", "Current", "Next", "Accent"};
        String[] keys = {ThemeConfig.KEY_BG, ThemeConfig.KEY_SURFACE, ThemeConfig.KEY_TEXT, ThemeConfig.KEY_TIME,
                ThemeConfig.KEY_MUTED, ThemeConfig.KEY_CURRENT, ThemeConfig.KEY_NEXT, ThemeConfig.KEY_ACCENT};
        int[] values = {base.background, base.surface, base.text, base.time, base.muted, base.current, base.next, base.accent};
        EditText[] edits = new EditText[keys.length];

        for (int i = 0; i < labels.length; i++) {
            TextView label = new TextView(this);
            label.setText(labels[i]);
            label.setTextSize(12);
            label.setTextColor(Color.DKGRAY);
            fields.addView(label, matchWrap());
            EditText e = new EditText(this);
            e.setSingleLine(true);
            e.setText(ThemeConfig.hex(values[i]));
            e.setTextSize(16);
            e.setTypeface(Typeface.MONOSPACE);
            fields.addView(e, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(50)));
            edits[i] = e;
        }

        ScrollView sc = new ScrollView(this);
        sc.addView(fields);
        new AlertDialog.Builder(this)
                .setTitle("Custom palette")
                .setView(sc)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Save", (dialog, which) -> {
                    SharedPreferences.Editor editor = SettingsStore.prefs(this).edit().putString(ThemeConfig.KEY_PRESET, "custom");
                    for (int i = 0; i < keys.length; i++) {
                        String value = edits[i].getText().toString().trim();
                        try {
                            Color.parseColor(value);
                            editor.putString(keys[i], value);
                        } catch (Exception ex) {
                            Toast.makeText(this, "Skipped invalid colour: " + labels[i], Toast.LENGTH_SHORT).show();
                        }
                    }
                    editor.apply();
                    CatchWidgetUpdater.updateAll(this);
                    rebuildUi();
                })
                .show();
    }

    private View buildSettingsPage() {
        LinearLayout root = column();

        LinearLayout system = card();
        system.addView(sectionTitle("NOTIFICATIONS"), matchWrap());
        system.addView(body(statusLine()), matchWrap());
        Button fix = pill("Check permissions", theme.background, theme.text, 13, 14);
        fix.setOnClickListener(v -> ensurePermissions());
        LinearLayout.LayoutParams fp = matchWrap();
        fp.setMargins(0, dp(10), 0, 0);
        system.addView(fix, fp);
        root.addView(system, fullCardParams());

        LinearLayout wording = card();
        wording.addView(sectionTitle("MESSAGE"), matchWrap());
        TextView phrase = text(SettingsStore.phrase(this), 19, theme.text, fonts.display);
        phrase.setGravity(Gravity.CENTER);
        phrase.setLetterSpacing(fonts.titleSpacing * 0.55f);
        LinearLayout.LayoutParams pp = matchWrap();
        pp.setMargins(0, dp(5), 0, dp(10));
        wording.addView(phrase, pp);
        Button edit = pill("Edit phrase", theme.background, theme.text, 13, 14);
        edit.setOnClickListener(v -> editPhrase());
        wording.addView(edit, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(44)));
        root.addView(wording, fullCardParams());

        LinearLayout behavior = card();
        behavior.addView(sectionTitle("BEHAVIOUR"), matchWrap());
        behavior.addView(settingSwitch("Emoji in copied/shared text", "Time-of-day aware and lightly varied.",
                SettingsStore.emojis(this), checked -> SettingsStore.prefs(this).edit().putBoolean(SettingsStore.KEY_EMOJI, checked).apply()), matchWrap());
        behavior.addView(settingSwitch("Compact Now", "Hide the Upcoming list on the Now page.",
                SettingsStore.compactHome(this), checked -> SettingsStore.prefs(this).edit().putBoolean(SettingsStore.KEY_HOME_COMPACT, checked).apply()), matchWrap());
        root.addView(behavior, fullCardParams());

        LinearLayout notif = card();
        notif.addView(sectionTitle("ALERT STYLE"), matchWrap());
        LinearLayout nrow = new LinearLayout(this);
        nrow.setOrientation(LinearLayout.HORIZONTAL);
        String style = SettingsStore.notificationStyle(this);
        String[][] styles = {{"quiet","Quiet"},{"normal","Normal"},{"celebrate","Pop"}};
        for (String[] s : styles) {
            boolean selected = style.equals(s[0]);
            Button b = pill(s[1], selected ? theme.accent : theme.background,
                    selected ? contrast(theme.accent) : theme.text, 12, 13);
            b.setOnClickListener(v -> {
                SettingsStore.prefs(this).edit().putString(SettingsStore.KEY_NOTIFICATION_STYLE, s[0]).apply();
                NotificationHelper.ensureAllChannels(this);
                rebuildUi();
            });
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, dp(44), 1f);
            if (nrow.getChildCount() > 0) p.setMargins(dp(6), 0, 0, 0);
            nrow.addView(b, p);
        }
        notif.addView(nrow, matchWrap());
        Button test = pill("Send test notification", theme.surface, theme.text, 13, 14);
        test.setOnClickListener(v -> {
            ensurePermissions();
            NotificationHelper.show(this, LocalTime.now().withSecond(0).withNano(0), true);
        });
        LinearLayout.LayoutParams tp = matchWrap();
        tp.setMargins(0, dp(10), 0, 0);
        notif.addView(test, tp);
        root.addView(notif, fullCardParams());

        LinearLayout transfer = card();
        transfer.addView(sectionTitle("MOVE BETWEEN DEVICES"), matchWrap());
        transfer.addView(body("Setup codes carry your colours, type style, phrase, time patterns and custom catches."), matchWrap());
        LinearLayout tr = new LinearLayout(this);
        tr.setOrientation(LinearLayout.HORIZONTAL);
        Button export = pill("Copy setup", theme.accent, contrast(theme.accent), 13, 14);
        Button imp = pill("Import setup", theme.background, theme.text, 13, 14);
        export.setOnClickListener(v -> exportSetup(v));
        imp.setOnClickListener(v -> importSetup());
        tr.addView(export, new LinearLayout.LayoutParams(0, dp(46), 1f));
        LinearLayout.LayoutParams ip = new LinearLayout.LayoutParams(0, dp(46), 1f);
        ip.setMargins(dp(8), 0, 0, 0);
        tr.addView(imp, ip);
        LinearLayout.LayoutParams trp = matchWrap();
        trp.setMargins(0, dp(12), 0, 0);
        transfer.addView(tr, trp);
        root.addView(transfer, fullCardParams());

        TextView version = text("Catch the Time · v3.0.0", 11, theme.muted, fonts.body);
        version.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams vp = matchWrap();
        vp.setMargins(0, dp(10), 0, dp(4));
        root.addView(version, vp);
        return root;
    }

    private interface BoolSetter { void set(boolean value); }

    private View settingSwitch(String title, String summary, boolean checked, BoolSetter setter) {
        LinearLayout row = settingRowBase();
        LinearLayout labels = column();
        labels.addView(text(title, 15, theme.text, fonts.medium), matchWrap());
        labels.addView(text(summary, 12, theme.muted, fonts.body), matchWrap());
        row.addView(labels, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        Switch sw = switchView(checked);
        sw.setOnCheckedChangeListener((button, value) -> {
            setter.set(value);
            button.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
            rebuildUi();
        });
        row.addView(sw, new LinearLayout.LayoutParams(dp(60), dp(46)));
        return row;
    }

    private LinearLayout settingRowBase() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(7), 0, dp(7));
        return row;
    }

    private void editPhrase() {
        final EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setText(SettingsStore.phrase(this));
        input.setSelectAllOnFocus(true);
        LinearLayout holder = column();
        holder.setPadding(dp(18), 0, dp(18), 0);
        holder.addView(input, matchWrap());
        new AlertDialog.Builder(this)
                .setTitle("Notification/share phrase")
                .setView(holder)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Save", (dialog, which) -> {
                    String value = input.getText().toString().trim();
                    if (value.isEmpty()) value = "Catch the time!";
                    SettingsStore.prefs(this).edit().putString(SettingsStore.KEY_PHRASE, value).apply();
                    CatchWidgetUpdater.updateAll(this);
                    rebuildUi();
                })
                .show();
    }

    private void exportSetup(View v) {
        try {
            String code = SettingsStore.exportCode(this);
            ClipboardManager cb = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            cb.setPrimaryClip(ClipData.newPlainText("Catch the Time setup", code));
            v.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
            Toast.makeText(this, "Setup code copied ✓", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Could not export setup", Toast.LENGTH_SHORT).show();
        }
    }

    private void importSetup() {
        final EditText input = new EditText(this);
        input.setMinLines(4);
        input.setGravity(Gravity.TOP);
        input.setHint("Paste CTT3:… setup code");
        LinearLayout holder = column();
        holder.setPadding(dp(18), 0, dp(18), 0);
        holder.addView(input, matchWrap());
        new AlertDialog.Builder(this)
                .setTitle("Import setup")
                .setView(holder)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Import", (dialog, which) -> {
                    try {
                        SettingsStore.importCode(this, input.getText().toString());
                        AlarmScheduler.scheduleNext(this);
                        CatchWidgetUpdater.updateAll(this);
                        Toast.makeText(this, "Setup imported ✓", Toast.LENGTH_SHORT).show();
                        rebuildUi();
                    } catch (Exception e) {
                        Toast.makeText(this, "That setup code is not valid", Toast.LENGTH_LONG).show();
                    }
                })
                .show();
    }

    private void copyCatch(LocalTime time, View v) {
        ClipboardManager cb = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        cb.setPrimaryClip(ClipData.newPlainText("Catch the time", ShareText.messageFor(this, time)));
        v.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
        Toast.makeText(this, "Catch copied ✓", Toast.LENGTH_SHORT).show();
    }

    private void shareCatch(LocalTime time, View v) {
        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType("text/plain");
        send.putExtra(Intent.EXTRA_TEXT, ShareText.messageFor(this, time));
        v.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
        startActivity(Intent.createChooser(send, "Share catch time"));
    }

    private String statusLine() {
        boolean enabled = AlarmScheduler.isEnabled(this);
        boolean exact = AlarmScheduler.canScheduleExact(this);
        boolean notif = Build.VERSION.SDK_INT < 33 || checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
        if (!enabled) return "Off · notifications are not armed";
        if (exact && notif) return "Armed ✓ · exact timing ready";
        if (!exact && !notif) return "Needs notification + exact alarm permission";
        if (!exact) return "Needs exact alarm permission";
        return "Needs notification permission";
    }

    private int statusColor() {
        String s = statusLine();
        return s.startsWith("Armed") ? theme.current : s.startsWith("Off") ? theme.muted : theme.next;
    }

    private void ensurePermissions() {
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATION_PERMISSION_REQUEST);
            return;
        }
        requestExactAlarmPermissionIfNeeded();
    }

    private void requestExactAlarmPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
            if (!am.canScheduleExactAlarms()) {
                startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:" + getPackageName())));
            }
        }
    }

    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == NOTIFICATION_PERMISSION_REQUEST) {
            requestExactAlarmPermissionIfNeeded();
            rebuildUi();
        }
    }

    private LinearLayout heroCard() {
        LinearLayout c = card();
        c.setBackground(roundRect(theme.surface, dp(22), blend(theme.surface, theme.accent, 0.32f), 1));
        c.setElevation(dp(3));
        return c;
    }

    private LinearLayout card() {
        LinearLayout c = column();
        c.setPadding(dp(16), dp(15), dp(16), dp(15));
        c.setBackground(roundRect(theme.surface, dp(20), blend(theme.surface, theme.text, 0.085f), 1));
        c.setElevation(dp(1.5f));
        return c;
    }

    private LinearLayout quietCard() {
        LinearLayout c = column();
        c.setPadding(dp(14), dp(12), dp(14), dp(12));
        int bg = blend(theme.background, theme.surface, 0.54f);
        c.setBackground(roundRect(bg, dp(18), blend(bg, theme.text, 0.055f), 1));
        return c;
    }

    private GradientDrawable roundRect(int fill, float radius, int stroke, int strokeWidthDp) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(fill);
        g.setCornerRadius(radius);
        if (strokeWidthDp > 0) g.setStroke(dp(strokeWidthDp), stroke);
        return g;
    }

    private LinearLayout column() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }

    private TextView sectionTitle(String s) {
        TextView t = text(s, 13, theme.text, fonts.display);
        t.setLetterSpacing(fonts.eyebrowSpacing);
        t.setPadding(0, 0, 0, dp(9));
        return t;
    }

    private TextView eyebrow(String s, int color) {
        TextView t = text(s, 10.5f, color, fonts.medium);
        t.setLetterSpacing(fonts.eyebrowSpacing);
        return t;
    }

    private TextView body(String s) {
        TextView t = text(s, 13, theme.muted, fonts.body);
        t.setLineSpacing(0, 1.10f);
        return t;
    }

    private TextView text(String s, float sp, int color, Typeface face) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        t.setTypeface(face);
        return t;
    }

    private Button pill(String label, int bg, int fg, float textSp, float radiusDp) {
        Button b = new Button(this);
        b.setText(label);
        b.setAllCaps(false);
        b.setTextSize(textSp);
        b.setTypeface(fonts == null ? Typeface.DEFAULT_BOLD : fonts.medium);
        b.setTextColor(fg);
        b.setPadding(dp(10), 0, dp(10), 0);
        b.setMinHeight(0);
        b.setMinWidth(0);
        b.setBackground(roundRect(bg, dp(radiusDp), blend(bg, fg, 0.10f), 1));
        return b;
    }

    private Switch switchView(boolean checked) {
        Switch s = new Switch(this);
        s.setChecked(checked);
        int[][] states = {new int[]{android.R.attr.state_checked}, new int[]{-android.R.attr.state_checked}};
        s.setThumbTintList(new ColorStateList(states, new int[]{theme.accent, theme.muted}));
        s.setTrackTintList(new ColorStateList(states, new int[]{withAlpha(theme.accent, 110), withAlpha(theme.muted, 60)}));
        return s;
    }

    private View timeRow(LocalTime time, String badge, int color) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(3), dp(5), dp(3), dp(5));
        TextView tt = text(CatchCatalog.HHMM.format(time), 19, color, fonts.time);
        tt.setLetterSpacing(fonts.timeSpacing);
        row.addView(tt, new LinearLayout.LayoutParams(0, dp(34), 1f));
        TextView bt = eyebrow(badge, color);
        bt.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        row.addView(bt, new LinearLayout.LayoutParams(dp(118), dp(34)));
        return row;
    }

    private LinearLayout.LayoutParams fullCardParams() {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        p.setMargins(0, 0, 0, dp(10));
        return p;
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    private int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private int withAlpha(int color, int alpha) {
        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color));
    }

    private int blend(int a, int b, float ratio) {
        float r = Math.max(0, Math.min(1, ratio));
        return Color.rgb(
                (int) (Color.red(a) * (1 - r) + Color.red(b) * r),
                (int) (Color.green(a) * (1 - r) + Color.green(b) * r),
                (int) (Color.blue(a) * (1 - r) + Color.blue(b) * r)
        );
    }

    private int contrast(int color) {
        double l = 0.2126 * Color.red(color) + 0.7152 * Color.green(color) + 0.0722 * Color.blue(color);
        return l > 150 ? Color.parseColor("#101318") : Color.WHITE;
    }
}
