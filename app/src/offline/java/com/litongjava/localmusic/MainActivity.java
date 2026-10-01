package com.litongjava.localmusic;

import android.app.*;
import android.content.*;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.*;
import android.text.*;
import android.view.*;
import android.widget.*;
import androidx.media3.common.*;
import androidx.media3.session.MediaController;
import androidx.media3.session.SessionToken;
import androidx.recyclerview.widget.*;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.*;
import java.util.concurrent.*;

public final class MainActivity extends Activity {
    private static final int BG = 0xFF101014, SURFACE = 0xFF222228, TEXT = 0xFFF4F3F0,
            MUTED = 0xFFAAAAB3, ACCENT = 0xFFD8E5AC, PICK_DIRECTORY = 40;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ExecutorService scanner = Executors.newSingleThreadExecutor();
    private Future<?> scanTask;
    private int scanVersion;
    private ListenableFuture<MediaController> controllerFuture;
    private MediaController controller;
    private List<Track> tracks = new ArrayList<>();
    private List<String> warnings = new ArrayList<>();
    private String page = "home", query = "", selectedAlbum;
    private boolean scanning, dragging;
    private LinearLayout shell, mini;
    private FrameLayout content;
    private TextView miniTitle, miniPlay, playerTitle, playerAlbum, playerPlay, modeButton, time, status;
    private SeekBar seek;
    private RecordView record;
    private RecyclerView list;
    private final List<Row> rows = new ArrayList<>();
    private LibraryAdapter adapter;
    private PersonalLibrary personal;
    private Map<String,String> tagIndex=new HashMap<>();
    private String selectedPlaylist, mineTab = "music", playerReturn = "home";
    private MusicIcon playIcon, favoriteIcon, modeIcon;
    private TextView endTime;
    private byte[] shownArtwork;
    private final ExecutorService mediaWorker=Executors.newSingleThreadExecutor();
    private Track pendingAudio;
    private boolean audioIntentHandled;
    private int openVersion, lyricsVersion, highlighted=-2;
    private String lyricsTrackId, importingLyricsId;
    private Lyrics lyrics=Lyrics.parse("");
    private LinearLayout lyricsPanel, lyricsLines;
    private ScrollView lyricsScroll;
    private static final int PICK_LYRICS=41;

    private final Player.Listener listener = new Player.Listener() {
        @Override public void onEvents(Player player, Player.Events events) { refreshPlayback(); }
        @Override public void onPlayerError(PlaybackException error) {
            new AlertDialog.Builder(MainActivity.this).setTitle(getString(R.string.ui_unable_to_play))
                    .setMessage(getString(R.string.ui_the_file_may_have_moved_be_damaged_or_its_folder_permission_ma) + error.getErrorCodeName())
                    .setPositiveButton(getString(R.string.ui_ok), null).show();
        }
    };
    private final Runnable tick = new Runnable() {
        @Override public void run() { updatePosition(); handler.postDelayed(this, 500); }
    };

    @Override public void onCreate(Bundle saved) {
        super.onCreate(saved);
        personal = new PersonalLibrary(this);tagIndex=personal.tagIndex();
        if (saved != null) {
            page = saved.getString("page", "home"); query = saved.getString("query", "");
            selectedAlbum = saved.getString("album");
            selectedPlaylist = saved.getString("playlist"); mineTab = saved.getString("mineTab", "music");
            playerReturn = saved.getString("playerReturn", "home"); importingLyricsId=saved.getString("importingLyricsId");
        }
        buildShell(); showPage(); scan();
        audioIntentHandled=saved!=null&&saved.getBoolean("audioIntentHandled");
        if(!audioIntentHandled)handleAudioIntent(getIntent());
        controllerFuture = new MediaController.Builder(this,
                new SessionToken(this, new ComponentName(this, PlaybackService.class))).buildAsync();
        controllerFuture.addListener(() -> {
            if (isDestroyed()) return;
            try { controller = controllerFuture.get(); controller.addListener(listener); refreshPlayback(); playPendingAudio(); }
            catch (Exception e) { toast(getString(R.string.ui_could_not_connect_to_the_player_please_reopen_the_app)); }
        }, command -> handler.post(command));
    }
    @Override protected void onStart() { super.onStart(); handler.post(tick); }
    @Override protected void onStop() { handler.removeCallbacks(tick); super.onStop(); }
    @Override protected void onSaveInstanceState(Bundle out) {
        out.putString("page", page); out.putString("query", query); out.putString("album", selectedAlbum);
        out.putString("playlist", selectedPlaylist); out.putString("mineTab", mineTab); out.putString("playerReturn", playerReturn);
        out.putString("importingLyricsId",importingLyricsId);out.putBoolean("audioIntentHandled",audioIntentHandled);
        super.onSaveInstanceState(out);
    }
    @Override protected void onDestroy() {
        mediaWorker.shutdownNow(); scanner.shutdownNow(); handler.removeCallbacksAndMessages(null);
        if (controller != null) controller.removeListener(listener);
        if (controllerFuture != null) MediaController.releaseFuture(controllerFuture);
        personal.close();
        super.onDestroy();
    }
    private void buildShell() {
        shell = column(); shell.setBackgroundColor(BG);
        shell.setOnApplyWindowInsetsListener((v, insets) -> {
            v.setPadding(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(),
                    insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom());
            return insets.consumeSystemWindowInsets();
        });
        content = new FrameLayout(this); shell.addView(content, new LinearLayout.LayoutParams(-1, 0, 1));
        mini = horizontal(); mini.setPadding(dp(14), dp(6), dp(8), dp(6)); mini.setBackground(round(SURFACE, 30));
        TextView disc = text("◉", 30, ACCENT); mini.addView(disc, new LinearLayout.LayoutParams(dp(44), dp(48)));
        miniTitle = text(getString(R.string.ui_choose_a_song_to_start_listening), 14, TEXT); miniTitle.setMaxLines(2);
        mini.addView(miniTitle, new LinearLayout.LayoutParams(0, -2, 1));
        miniTitle.setOnClickListener(v -> navigate("player")); disc.setOnClickListener(v -> navigate("player"));
        miniPlay = button("▶", getString(R.string.ui_play_or_pause), v -> toggle()); mini.addView(miniPlay);
        mini.addView(button("☷", getString(R.string.ui_play_queue), v -> showQueue()));
        LinearLayout.LayoutParams mp = new LinearLayout.LayoutParams(-1, dp(64)); mp.setMargins(dp(16), dp(4), dp(16), dp(6));
        shell.addView(mini, mp);
        LinearLayout nav = horizontal();
        String[] labels = {getString(R.string.ui_home), getString(R.string.ui_albums), getString(R.string.ui_notes), getString(R.string.ui_my)}; String[] pages = {"home", "albums", "notes", "mine"};
        for (int i = 0; i < pages.length; i++) {
            final String target = pages[i]; TextView tab = button(labels[i], labels[i], v -> {
                if (target.equals("home") || target.equals("albums")) selectedAlbum = null;
                navigate(target);
            });
            nav.addView(tab, new LinearLayout.LayoutParams(0, dp(58), 1));
        }
        shell.addView(nav); setContentView(shell); shell.requestApplyInsets();
    }
    private void navigate(String target) {
        if (target.equals("player") && !page.equals("player")) playerReturn = page;
        page = target; showPage();
    }
    private void showPage() {
        content.removeAllViews(); playerTitle = playerAlbum = playerPlay = modeButton = time = status = null;
        lyricsPanel=null;lyricsLines=null;lyricsScroll=null;lyricsTrackId=null;lyricsVersion++;
        record = null; seek = null; list = null; adapter = null;
        playIcon = favoriteIcon = modeIcon = null; endTime = null; shownArtwork = null;
        mini.setVisibility(page.equals("player") ? View.GONE : View.VISIBLE);
        LinearLayout nav = (LinearLayout) shell.getChildAt(2);
        nav.setVisibility(page.equals("player") ? View.GONE : View.VISIBLE);
        shell.setBackgroundColor(page.equals("player") ? 0xFF202B3B : BG);
        String[] pages = {"home", "albums", "notes", "mine"};
        for (int i = 0; i < pages.length; i++) ((TextView) nav.getChildAt(i)).setTextColor(page.equals(pages[i]) ? ACCENT : MUTED);
        if (page.equals("settings")) showSettings();
        else if (page.equals("player")) showPlayer();
        else if (page.equals("mine")) showMine();
        else if (page.equals("notes")) showNotes();
        else if (page.equals("favorites") || page.equals("playlist")) showPersonalTracks();
        else if (page.equals("favorite-albums")) showFavoriteAlbums();
        else showLibrary();
        refreshPlayback();
    }
    private void showLibrary() {
        LinearLayout body = column(); body.setPadding(dp(18), dp(12), dp(18), 0); content.addView(body);
        LinearLayout top = horizontal();
        TextView brand = text(selectedAlbum == null ? getString(R.string.app_name) : getString(R.string.ui_back_to_albums), 23, TEXT);
        brand.setTypeface(null, Typeface.BOLD); top.addView(brand, new LinearLayout.LayoutParams(0, dp(48), 1));
        brand.setOnClickListener(v -> { selectedAlbum = null; navigate("albums"); });
        if(selectedAlbum==null)top.addView(button("↻", getString(R.string.ui_scan_again), v -> scan()));
        else top.addView(icon("more",getString(R.string.album_options),this::showAlbumMenu),new LinearLayout.LayoutParams(dp(48),dp(48)));
        body.addView(top);
        EditText search = new EditText(this); search.setTextColor(TEXT); search.setHintTextColor(MUTED);
        search.setSingleLine(true); search.setTextSize(15); search.setPadding(dp(18), 0, dp(14), 0);
        search.setBackground(round(SURFACE, 28)); search.setHint(getString(R.string.ui_search_songs_and_albums)); search.setContentDescription(getString(R.string.ui_search_songs_and_albums));
        search.setText(query); body.addView(search, new LinearLayout.LayoutParams(-1, dp(50)));
        body.setFocusableInTouchMode(true); body.requestFocus();
        search.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            public void onTextChanged(CharSequence s, int start, int before, int count) { query = s.toString(); rebuildRows(); }
            public void afterTextChanged(Editable e) { }
        });
        list = new RecyclerView(this); list.setClipToPadding(false); list.setPadding(0, dp(14), 0, dp(8));
        GridLayoutManager grid = new GridLayoutManager(this, 2);
        grid.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            public int getSpanSize(int position) { return position < rows.size() && rows.get(position).kind == 2 ? 1 : 2; }
        });
        list.setLayoutManager(grid); adapter = new LibraryAdapter(); list.setAdapter(adapter);
        body.addView(list, new LinearLayout.LayoutParams(-1, 0, 1)); rebuildRows();
    }
    private void showAlbumMenu(View anchor) {
        List<Track> songs=new ArrayList<>();for(Track track:tracks)if(track.albumId.equals(selectedAlbum))songs.add(track);
        PopupMenu menu=new PopupMenu(this,anchor);
        if(!songs.isEmpty()) {
            Track first=songs.get(0);
            menu.getMenu().add(personal.isAlbumFavorite(first.albumId)?R.string.album_unsave:R.string.album_save).setOnMenuItemClickListener(item->{personal.toggleAlbum(first.albumId,first.album,first.source);return true;});
            menu.getMenu().add(R.string.ui_add_to_playlist_2).setOnMenuItemClickListener(item->{choosePlaylist(songs);return true;});
        }
        menu.getMenu().add(R.string.ui_scan_again).setOnMenuItemClickListener(item->{scan();return true;});menu.show();
    }
    private void rebuildRows() {
        if (adapter == null) return;
        rows.clear();
        LinkedHashMap<String, List<Track>> albums = new LinkedHashMap<>();
        for (Track track : tracks) {
            if (!albums.containsKey(track.albumId)) albums.put(track.albumId, new ArrayList<>());
            albums.get(track.albumId).add(track);
        }
        if (scanning) rows.add(new Row(3, getString(R.string.ui_scanning_local_folders), getString(R.string.ui_you_can_keep_listening_while_we_scan), null));
        if (!warnings.isEmpty()) rows.add(new Row(3, getString(R.string.ui_there_are) + warnings.size() + getString(R.string.ui_folders_that_need_attention), getString(R.string.ui_review_folder_access_in_settings), () -> navigate("settings")));
        if (!query.trim().isEmpty()) {
            rows.add(new Row(0, getString(R.string.ui_search_results), "", null));
            boolean found = false;
            for (Track track : tracks) if (matchesTrack(track)) { rows.add(trackRow(track)); found = true; }
            if (!found) rows.add(new Row(3, getString(R.string.ui_no_matching_music), getString(R.string.ui_try_a_song_or_album_name), null));
        } else if (selectedAlbum != null) {
            List<Track> albumTracks = albums.get(selectedAlbum);
            if (albumTracks != null) {
                rows.add(new Row(5, albumTracks.get(0).album, albumTracks.size() + getString(R.string.ui_tracks_tap_to_play_all), () -> play(albumTracks, 0)));
                for (Track track : albumTracks) rows.add(trackRow(track));
            } else rows.add(new Row(3, getString(R.string.ui_this_album_has_no_available_music), getString(R.string.ui_scan_again_or_check_folder_access), null));
        } else {
            if (page.equals("home")) rows.add(new Row(1, getString(R.string.ui_keep_your_favorites_close), "OFFLINE COLLECTION\n" + albums.size() + getString(R.string.ui_albums_2) + tracks.size() + getString(R.string.ui_tracks_nplay_all), () -> play(tracks, 0)));
            rows.add(new Row(0, page.equals("home") ? getString(R.string.ui_your_music_albums) : getString(R.string.ui_all_albums), "", null));
            int index = 0;
            for (List<Track> group : albums.values()) {
                Track first = group.get(0);
                Row row = new Row(2, first.album, group.size() + getString(R.string.ui_tracks) + first.source, () -> {
                    selectedAlbum = first.albumId; navigate("albums");
                });
                row.tint = new int[]{0xFF587593, 0xFF837891, 0xFF557D76, 0xFFA68B43}[index++ % 4]; rows.add(row);
            }
            if (tracks.isEmpty()) rows.add(new Row(3, getString(R.string.ui_give_your_music_a_home), getString(R.string.ui_add_folders_in_settings_subfolders_become_albums), () -> navigate("settings")));
            if (page.equals("home") && !tracks.isEmpty()) {
                rows.add(new Row(0, getString(R.string.ui_ready_to_play), "", null));
                for (int i = 0; i < Math.min(6, tracks.size()); i++) rows.add(trackRow(tracks.get(i)));
            }
        }
        adapter.notifyDataSetChanged();
    }
    private Row trackRow(Track track) {
        Row row = new Row(4, track.title, track.album+(tagIndex.containsKey(track.id)?" · #"+tagIndex.get(track.id).replace(", "," #"):""), () -> {
            List<Track> queue = new ArrayList<>();
            for (Track t : tracks) if (!query.trim().isEmpty() ? matchesTrack(t) : selectedAlbum == null || t.albumId.equals(selectedAlbum)) queue.add(t);
            int index = queue.indexOf(track); if (index >= 0) { play(queue, index); navigate("player"); }
        });
        row.longAction = () -> trackOptions(track, null);
        return row;
    }
    private void showSettings() {
        LinearLayout body = scrollBody(); backHeader(body, getString(R.string.ui_settings), "mine");
        addNote(body, getString(R.string.ui_music_folders), getString(R.string.ui_add_multiple_folders_each_direct_subfolder_becomes_an_album_in));
        addAction(body, getString(R.string.ui_add_music_folder), v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
            intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
            try { startActivityForResult(intent, PICK_DIRECTORY); }
            catch (ActivityNotFoundException e) { toast(getString(R.string.ui_no_system_file_picker_is_available_on_this_device)); }
        });
        for (Uri uri : new DirectoryStore(this).get()) {
            LinearLayout card = column(); card.setPadding(dp(16), dp(12), dp(16), dp(12)); card.setBackground(round(SURFACE, 16));
            card.addView(text(LibraryRepository.label(this, uri), 16, TEXT));
            TextView remove = button(getString(R.string.ui_remove_folder), getString(R.string.ui_remove_folder_2) + LibraryRepository.label(this, uri), v -> {
                new DirectoryStore(this).remove(uri); scan(); showPage();
            }); card.addView(remove); addSpaced(body, card);
        }
        addNote(body, getString(R.string.ui_built_in_album_fool_s_garden), getString(R.string.ui_lemon_tree_included_with_the_app_available_offline));
        addAction(body, scanning ? getString(R.string.ui_scanning) : getString(R.string.ui_scan_all_folders_again), v -> scan());
        if (warnings.isEmpty()) addNote(body, getString(R.string.ui_scan_status), scanning ? getString(R.string.ui_reading_file_list) : getString(R.string.ui_found) + tracks.size() + getString(R.string.ui_audio_files));
        else for (String warning : warnings) addNote(body, getString(R.string.ui_needs_attention), warning);
        addNote(body, getString(R.string.ui_files_and_privacy), getString(R.string.ui_only_folders_you_authorize_are_accessed_original_files_are_nev));
    }
    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if(request==PICK_LYRICS&&result==RESULT_OK&&data!=null&&data.getData()!=null&&importingLyricsId!=null){
            final String id=importingLyricsId;final Uri uri=data.getData();
            mediaWorker.submit(()->{try{LocalLyrics.importFile(getApplicationContext(),id,uri);handler.post(()->{if(isDestroyed())return;lyricsTrackId=null;loadLyricsIfNeeded();});}
                catch(Exception e){handler.post(()->{if(!isDestroyed())toast(getString(R.string.lyrics_error));});}});
        }
        if (request == PICK_DIRECTORY && result == RESULT_OK && data != null && data.getData() != null) {
            try { new DirectoryStore(this).add(data.getData()); scan(); showPage(); }
            catch (SecurityException e) { toast(getString(R.string.ui_could_not_save_folder_access_please_select_the_folder_again)); }
        }
    }
    private void scan() {
        int version = ++scanVersion; scanning = true;
        if (scanTask != null) scanTask.cancel(true);
        rebuildRows();
        scanTask = scanner.submit(() -> {
            LibraryIndexer.Result result = new LibraryRepository(getApplicationContext()).scan();
            handler.post(() -> {
                if (isDestroyed() || version != scanVersion) return;
                tracks = result.tracks; warnings = result.warnings; scanning = false;
                if (page.equals("settings")) showPage(); else rebuildRows();
            });
        });
    }
    @Override protected void onNewIntent(Intent intent){super.onNewIntent(intent);setIntent(intent);handleAudioIntent(intent);}
    private void handleAudioIntent(Intent intent){
        if(intent==null)return;Uri uri=null;
        if(Intent.ACTION_VIEW.equals(intent.getAction()))uri=intent.getData();
        else if(Intent.ACTION_SEND.equals(intent.getAction()))uri=intent.getParcelableExtra(Intent.EXTRA_STREAM);
        if(uri==null)return;audioIntentHandled=false;final Uri source=uri;final int version=++openVersion;
        toast(getString(R.string.open_audio_wait));
        mediaWorker.submit(()->{try{
            Track imported=ExternalAudio.importFile(getApplicationContext(),source);
            try(PersonalLibrary db=new PersonalLibrary(getApplicationContext())){db.saveImported(imported);}
            handler.post(()->{if(isDestroyed()||version!=openVersion)return;pendingAudio=imported;scan();playPendingAudio();});
        }catch(Exception e){handler.post(()->{if(!isDestroyed()&&version==openVersion)toast(getString(R.string.open_audio_failed));});}});
    }
    private void playPendingAudio(){if(controller!=null&&pendingAudio!=null){Track track=pendingAudio;pendingAudio=null;audioIntentHandled=true;play(Collections.singletonList(track),0);navigate("player");}}
    private void pickLyrics(){
        Track track=currentTrack();if(track==null)return;importingLyricsId=track.id;
        Intent intent=new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("*/*").addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent,PICK_LYRICS);
    }
    private void loadLyricsIfNeeded(){
        if(lyricsLines==null)return;Track track=currentTrack();String id=track==null?"":track.id;
        if(id.equals(lyricsTrackId))return;lyricsTrackId=id;int version=++lyricsVersion;
        lyrics=Lyrics.parse("");highlighted=-2;lyricsLines.removeAllViews();
        TextView loading=text(getString(R.string.lyrics_loading),16,MUTED);loading.setGravity(Gravity.CENTER);lyricsLines.addView(loading);
        mediaWorker.submit(()->{Lyrics loaded=track==null?Lyrics.parse(""):LocalLyrics.load(getApplicationContext(),track);
            handler.post(()->{if(isDestroyed()||version!=lyricsVersion||lyricsLines==null)return;lyrics=loaded;renderLyrics();});});
    }
    private void renderLyrics(){
        lyricsLines.removeAllViews();highlighted=-2;
        if(lyrics.lines.isEmpty()){TextView empty=text(getString(R.string.lyrics_empty),16,0xFFBAC3D0);empty.setGravity(Gravity.CENTER);empty.setLineSpacing(dp(10),1);lyricsLines.addView(empty);return;}
        for(Lyrics.Line line:lyrics.lines){TextView label=text(line.text.isEmpty()?"♪":line.text,18,0xFFBAC3D0);label.setGravity(Gravity.CENTER);label.setPadding(dp(8),dp(14),dp(8),dp(14));lyricsLines.addView(label,new LinearLayout.LayoutParams(-1,-2));if(lyrics.timed)label.setOnClickListener(v->{if(controller!=null&&controller.isCurrentMediaItemSeekable())controller.seekTo(line.time);});}
        updateLyricsPosition();
    }
    private void updateLyricsPosition(){
        if(controller==null||lyricsPanel==null||lyricsPanel.getVisibility()!=View.VISIBLE||!lyrics.timed)return;
        int active=lyrics.active(controller.getCurrentPosition());if(active==highlighted)return;highlighted=active;
        for(int i=0;i<lyricsLines.getChildCount();i++){TextView line=(TextView)lyricsLines.getChildAt(i);boolean current=active>=0&&lyrics.lines.get(i).time==lyrics.lines.get(active).time;line.setTextColor(current?Color.WHITE:0xFF8E9BAC);line.setTypeface(null,current?Typeface.BOLD:Typeface.NORMAL);}
        if(active>=0){View line=lyricsLines.getChildAt(active);lyricsScroll.post(()->{if(lyricsScroll!=null&&line.getParent()==lyricsLines)lyricsScroll.smoothScrollTo(0,Math.max(0,line.getTop()-lyricsScroll.getHeight()/2+line.getHeight()/2));});}
    }
    private void showPlayer() {
        LinearLayout body = column(); body.setPadding(dp(22), dp(6), dp(22), dp(12));
        body.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[]{0xFF37475F, 0xFF19212D}));
        boolean compact = getResources().getConfiguration().screenHeightDp < 580;
        if (compact) { ScrollView scroll = new ScrollView(this); scroll.addView(body); content.addView(scroll); }
        else content.addView(body);
        LinearLayout top = horizontal();
        top.addView(icon("down", getString(R.string.ui_close_player), v -> navigate(playerReturn)), new LinearLayout.LayoutParams(dp(44), dp(44)));
        TextView heading = text(getString(R.string.app_name), 14, 0xFFBAC3D0); heading.setGravity(Gravity.CENTER);
        top.addView(heading, new LinearLayout.LayoutParams(0, dp(44), 1));
        top.addView(icon("more", getString(R.string.ui_song_options), v -> { Track t = currentTrack(); if (t != null) trackOptions(t, null); }), new LinearLayout.LayoutParams(dp(44), dp(44)));
        body.addView(top);
        record = new RecordView(this);
        FrameLayout artArea=new FrameLayout(this);artArea.addView(record,new FrameLayout.LayoutParams(-1,-1));
        lyricsPanel=column();lyricsPanel.setVisibility(View.GONE);artArea.addView(lyricsPanel,new FrameLayout.LayoutParams(-1,-1));
        LinearLayout lyricActions=horizontal();
        lyricActions.addView(button(getString(R.string.lyrics_back),getString(R.string.lyrics_back),v->{lyricsPanel.setVisibility(View.GONE);record.setVisibility(View.VISIBLE);}),new LinearLayout.LayoutParams(0,dp(48),1));
        lyricActions.addView(button(getString(R.string.lyrics_import),getString(R.string.lyrics_import),v->pickLyrics()),new LinearLayout.LayoutParams(0,dp(48),1));lyricsPanel.addView(lyricActions);
        lyricsScroll=new ScrollView(this);lyricsLines=column();lyricsLines.setPadding(0,dp(32),0,dp(50));lyricsScroll.addView(lyricsLines);lyricsPanel.addView(lyricsScroll,new LinearLayout.LayoutParams(-1,0,1));
        record.setOnClickListener(v->{record.setVisibility(View.GONE);lyricsPanel.setVisibility(View.VISIBLE);highlighted=-2;updateLyricsPosition();});
        body.addView(artArea, compact ? new LinearLayout.LayoutParams(-1, dp(280)) : new LinearLayout.LayoutParams(-1, 0, 1));
        LinearLayout song = horizontal(); LinearLayout labels = column();
        playerTitle = text(getString(R.string.ui_choose_a_song), 21, 0xFFF0F2F5); playerTitle.setSingleLine(true); playerTitle.setEllipsize(TextUtils.TruncateAt.MARQUEE); playerTitle.setSelected(true); playerTitle.setMarqueeRepeatLimit(-1); playerTitle.setTypeface(null, Typeface.BOLD);
        labels.addView(playerTitle); playerAlbum = text(getString(R.string.ui_local_music_offline_listening), 14, 0xFFAAB4C2); playerAlbum.setSingleLine(true); playerAlbum.setEllipsize(TextUtils.TruncateAt.END); labels.addView(playerAlbum);
        song.addView(labels, new LinearLayout.LayoutParams(0, -2, 1));
        favoriteIcon = icon("heart", getString(R.string.ui_favorite_song), v -> toggleCurrentFavorite());
        song.addView(favoriteIcon, new LinearLayout.LayoutParams(dp(44), dp(52)));
        song.addView(icon("note", getString(R.string.ui_add_playback_note), v -> addCurrentNote()), new LinearLayout.LayoutParams(dp(44), dp(52)));
        LinearLayout.LayoutParams songParams = new LinearLayout.LayoutParams(-1, -2); songParams.setMargins(0,dp(10),0,dp(12)); body.addView(song,songParams);
        seek = new SeekBar(this); seek.setMax(1000); seek.setPadding(dp(10), 0, dp(10), 0);
        seek.setProgressTintList(ColorStateList.valueOf(0xFFD2D9E3)); seek.setThumbTintList(ColorStateList.valueOf(0xFFF1F4F7));
        body.addView(seek,new LinearLayout.LayoutParams(-1,dp(30)));
        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar s,int progress,boolean user) { }
            public void onStartTrackingTouch(SeekBar s) { dragging=true; }
            public void onStopTrackingTouch(SeekBar s) { if(controller!=null&&controller.isCurrentMediaItemSeekable()&&controller.getDuration()>0)controller.seekTo(controller.getDuration()*s.getProgress()/1000);dragging=false; }
        });
        LinearLayout timestamps = horizontal(); time = text("00:00", 11, 0xFF8E9BAC); endTime = text("00:00",11,0xFF8E9BAC);
        timestamps.addView(time); status=text(getString(R.string.ui_offline_audio),11,0xFF8E9BAC);status.setGravity(Gravity.CENTER);timestamps.addView(status,new LinearLayout.LayoutParams(0,dp(24),1));timestamps.addView(endTime);body.addView(timestamps);
        LinearLayout controls = horizontal(); controls.setPadding(0,dp(12),0,dp(8));
        modeIcon=icon("repeat",getString(R.string.ui_repeat_all_tap_to_change),v->cycleMode());
        controls.addView(modeIcon,new LinearLayout.LayoutParams(0,dp(64),1));
        controls.addView(icon("previous",getString(R.string.ui_previous_track),v->{if(ready())controller.seekToPreviousMediaItem();}),new LinearLayout.LayoutParams(0,dp(64),1));
        playIcon=icon("play",getString(R.string.ui_play),v->toggle());controls.addView(playIcon,new LinearLayout.LayoutParams(0,dp(74),1.35f));
        controls.addView(icon("next",getString(R.string.ui_next_track),v->{if(ready())controller.seekToNextMediaItem();}),new LinearLayout.LayoutParams(0,dp(64),1));
        controls.addView(icon("queue",getString(R.string.ui_play_queue),v->showQueue()),new LinearLayout.LayoutParams(0,dp(64),1));body.addView(controls);
        LinearLayout tools = horizontal();
        playerTool(tools,"plus",getString(R.string.ui_add_to_playlist_2),v->{Track t=currentTrack();if(t!=null)choosePlaylist(Collections.singletonList(t));});
        playerTool(tools,"note",getString(R.string.ui_write_note),v->addCurrentNote());
        playerTool(tools,"timer",getString(R.string.ui_timer),v->showSleepTimer());
        playerTool(tools,"folder",getString(R.string.ui_view_notes),v->{Track t=currentTrack();if(t!=null)showTrackNotes(t);});
        body.addView(tools,new LinearLayout.LayoutParams(-1,dp(58)));
    }
    private MusicIcon icon(String kind,String description,View.OnClickListener listener) { return new MusicIcon(this,kind,description,listener); }
    private void playerTool(LinearLayout parent,String kind,String label,View.OnClickListener listener) {
        LinearLayout box=column();box.setGravity(Gravity.CENTER);MusicIcon i=icon(kind,label,listener);i.setTint(0xFF919FAF);
        box.addView(i,new LinearLayout.LayoutParams(dp(40),dp(35)));TextView t=text(label,10,0xFF919FAF);t.setGravity(Gravity.CENTER);box.addView(t);
        box.setOnClickListener(listener);parent.addView(box,new LinearLayout.LayoutParams(0,-1,1));
    }
    private void showSleepTimer() {
        new AlertDialog.Builder(this).setTitle(getString(R.string.ui_sleep_timer)).setItems(new String[]{getString(R.string.ui_turn_off_timer),getString(R.string.ui_15_minutes),getString(R.string.ui_30_minutes),getString(R.string.ui_60_minutes)},(d,which)->{
            if(!ready())return;int minutes=new int[]{0,15,30,60}[which];
            startService(new Intent(this,PlaybackService.class).setAction(PlaybackService.ACTION_SLEEP).putExtra("minutes",(long)minutes));
            toast(minutes==0?getString(R.string.ui_timer_turned_off):minutes+getString(R.string.ui_minutes_until_playback_stops));
        }).show();
    }
    private boolean ready() { if (controller != null) return true; toast(getString(R.string.ui_connecting_to_the_player)); return false; }
    private void play(List<Track> queue, int index) {
        if (!ready()) return;
        if (queue.isEmpty()) { toast(getString(R.string.ui_no_audio_available_add_a_music_folder_first)); return; }
        List<MediaItem> items = new ArrayList<>();
        for (Track track : queue) items.add(new MediaItem.Builder().setMediaId(track.id).setUri(track.uri)
                .setMediaMetadata(new MediaMetadata.Builder().setTitle(track.title).setAlbumTitle(track.album)
                        .setArtist(track.album).setIsPlayable(true).build()).build());
        controller.setMediaItems(items, index, 0); controller.prepare(); controller.play();
    }
    private void toggle() {
        if (!ready()) return;
        if (controller.getMediaItemCount() == 0) { play(tracks, 0); return; }
        if (controller.getPlayWhenReady()) controller.pause();
        else {
            if (controller.getPlaybackState() == Player.STATE_ENDED) controller.seekTo(0);
            if (controller.getPlaybackState() == Player.STATE_IDLE) controller.prepare();
            controller.play();
        }
    }
    private void cycleMode() {
        if (!ready()) return;
        if (controller.getShuffleModeEnabled()) { controller.setShuffleModeEnabled(false); controller.setRepeatMode(Player.REPEAT_MODE_OFF); }
        else if (controller.getRepeatMode() == Player.REPEAT_MODE_ALL) controller.setRepeatMode(Player.REPEAT_MODE_ONE);
        else if (controller.getRepeatMode() == Player.REPEAT_MODE_ONE) { controller.setRepeatMode(Player.REPEAT_MODE_ALL); controller.setShuffleModeEnabled(true); }
        else controller.setRepeatMode(Player.REPEAT_MODE_ALL);
        refreshPlayback();
    }
    private void showQueue() {
        if (!ready()) return;
        int count = controller.getMediaItemCount();
        if (count == 0) { toast(getString(R.string.ui_the_queue_is_empty_choose_a_song_first)); return; }
        String[] titles = new String[count];
        for (int i = 0; i < count; i++) titles[i] = String.valueOf(controller.getMediaItemAt(i).mediaMetadata.title);
        new AlertDialog.Builder(this).setTitle(getString(R.string.ui_play_queue_2) + count + getString(R.string.ui_tracks_2))
                .setSingleChoiceItems(titles, controller.getCurrentMediaItemIndex(), (dialog, which) -> {
                    controller.seekToDefaultPosition(which); controller.prepare(); controller.play(); dialog.dismiss();
                }).setNegativeButton(getString(R.string.ui_close), null).show();
    }
    private void refreshPlayback() {
        if (controller == null || miniTitle == null) return;
        CharSequence title = controller.getMediaMetadata().title, album = controller.getMediaMetadata().albumTitle;
        boolean hasTrack = controller.getMediaItemCount() > 0;
        miniTitle.setText(hasTrack ? title + "\n" + album : getString(R.string.ui_choose_a_song_to_start_listening));
        String symbol = controller.getPlayWhenReady() ? "Ⅱ" : "▶";
        miniPlay.setText(symbol); miniPlay.setContentDescription(controller.getPlayWhenReady() ? getString(R.string.ui_pause) : getString(R.string.ui_play));
        if (playerTitle != null) {
            playerTitle.setText(hasTrack ? title : getString(R.string.ui_choose_a_song));
            playerAlbum.setText(hasTrack ? album : getString(R.string.ui_local_music_offline_listening));
            playIcon.setKind(controller.getPlayWhenReady() ? "pause" : "play");
            playIcon.setContentDescription(controller.getPlayWhenReady() ? getString(R.string.ui_pause) : getString(R.string.ui_play));
            String mode = controller.getShuffleModeEnabled() ? "shuffle" : controller.getRepeatMode() == Player.REPEAT_MODE_ONE ? "repeat-one" : controller.getRepeatMode() == Player.REPEAT_MODE_ALL ? "repeat" : "sequential";
            modeIcon.setKind(mode);
            modeIcon.setTint(mode.equals("sequential") ? 0xFF778494 : 0xFFD3D9E2);
            modeIcon.setContentDescription(controller.getShuffleModeEnabled() ? getString(R.string.ui_shuffle_tap_to_change) : controller.getRepeatMode() == Player.REPEAT_MODE_ONE ? getString(R.string.ui_repeat_one_tap_to_change) : controller.getRepeatMode() == Player.REPEAT_MODE_ALL ? getString(R.string.ui_repeat_all_tap_to_change) : getString(R.string.ui_play_in_order_tap_to_change));
            updateFavoriteIcon();
            byte[] art = controller.getMediaMetadata().artworkData;
            if (shownArtwork != art) {
                shownArtwork = art;
                android.graphics.Bitmap bitmap = null;
                if (art != null) {
                    android.graphics.BitmapFactory.Options options = new android.graphics.BitmapFactory.Options(); options.inJustDecodeBounds = true;
                    android.graphics.BitmapFactory.decodeByteArray(art, 0, art.length, options);
                    options.inSampleSize = Math.max(1, Math.max(options.outWidth, options.outHeight) / 600); options.inJustDecodeBounds = false;
                    bitmap = android.graphics.BitmapFactory.decodeByteArray(art, 0, art.length, options);
                }
                record.setArtwork(bitmap);
            }
            record.setPlaying(controller.isPlaying());
            loadLyricsIfNeeded();
        }
        updatePosition();
    }
    private void updatePosition() {
        if (controller == null || seek == null) return;
        long duration = Math.max(0, controller.getDuration()), position = Math.max(0, controller.getCurrentPosition());
        seek.setEnabled(duration > 0 && controller.isCurrentMediaItemSeekable());
        if (!dragging) seek.setProgress(duration > 0 ? (int) (position * 1000 / duration) : 0);
        time.setText(formatTime(position));
        updateLyricsPosition();
        if (endTime != null) endTime.setText(formatTime(duration));
        long remaining = PlaybackService.sleepDeadline - SystemClock.elapsedRealtime();
        status.setText(controller.getPlaybackState() == Player.STATE_BUFFERING ? getString(R.string.ui_loading) : remaining > 0 ? getString(R.string.ui_timer_2) + (remaining / 60_000 + 1) + getString(R.string.ui_min) : getString(R.string.ui_offline_audio));
    }
    private static String formatTime(long millis) { return String.format(Locale.ROOT, "%02d:%02d", millis / 60000, (millis / 1000) % 60); }
    @Override public void onBackPressed() {
        if (page.equals("player")) navigate(playerReturn);
        else if (page.equals("settings") || page.equals("favorites") || page.equals("favorite-albums") || page.equals("playlist")) navigate("mine");
        else if (selectedAlbum != null) { selectedAlbum = null; navigate("albums"); }
        else if (!page.equals("home")) navigate("home"); else super.onBackPressed();
    }
    private void backHeader(LinearLayout body, String name, String destination) {
        LinearLayout row=horizontal();row.addView(icon("back",getString(R.string.ui_back),v->navigate(destination)),new LinearLayout.LayoutParams(dp(42),dp(48)));
        TextView h=text(name,22,TEXT);h.setTypeface(null,Typeface.BOLD);row.addView(h,new LinearLayout.LayoutParams(0,dp(52),1));body.addView(row);
    }
    private void showMine() {
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);content.addView(scroll);LinearLayout body=column();scroll.addView(body);
        LinearLayout profile=column();profile.setPadding(dp(18),dp(8),dp(18),dp(24));
        profile.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,new int[]{0xFF372016,0xFF24150F,0xFF171215}));
        LinearLayout toolbar=horizontal();TextView title=text(getString(R.string.ui_my_music),18,TEXT);toolbar.addView(title,new LinearLayout.LayoutParams(0,dp(44),1));
        toolbar.addView(icon("plus",getString(R.string.ui_new_playlist),v->playlistNameDialog(null,null)),new LinearLayout.LayoutParams(dp(42),dp(44)));
        toolbar.addView(icon("search",getString(R.string.ui_search_local_music),v->{selectedAlbum=null;navigate("albums");}),new LinearLayout.LayoutParams(dp(42),dp(44)));
        MusicIcon more=icon("more",getString(R.string.ui_my_music_options),v->{PopupMenu menu=new PopupMenu(this,v);menu.getMenu().add(getString(R.string.ui_settings));menu.setOnMenuItemClickListener(item->{navigate("settings");return true;});menu.show();});
        toolbar.addView(more,new LinearLayout.LayoutParams(dp(42),dp(44)));profile.addView(toolbar);
        TextView avatar=text("♫",36,0xFFF0DACE);avatar.setGravity(Gravity.CENTER);avatar.setBackground(round(0xFF76554B,100));
        LinearLayout.LayoutParams av=new LinearLayout.LayoutParams(dp(72),dp(72));av.gravity=Gravity.CENTER;av.setMargins(0,dp(16),0,dp(14));profile.addView(avatar,av);
        TextView name=text(getString(R.string.ui_my_music_space),23,TEXT);name.setTypeface(null,Typeface.BOLD);name.setGravity(Gravity.CENTER);profile.addView(name);
        TextView caption=text(getString(R.string.ui_a_world_of_sound_just_for_you),12,0xFFB4A29B);caption.setGravity(Gravity.CENTER);addSpaced(profile,caption);
        TextView counts=text(personal.playlists().size()+getString(R.string.ui_playlists)+personal.favorites().size()+getString(R.string.ui_favorites)+personal.notes(null).size()+getString(R.string.ui_notes_2),13,0xFFDBCEC8);counts.setGravity(Gravity.CENTER);addSpaced(profile,counts);
        LinearLayout shortcuts=horizontal();
        for(String label:new String[]{getString(R.string.app_name),getString(R.string.ui_my_favorites),getString(R.string.ui_playback_notes)}){
            TextView b=button(label,label,v->{if(label.equals(getString(R.string.app_name))){selectedAlbum=null;navigate("albums");}else navigate(label.equals(getString(R.string.ui_my_favorites))?"favorites":"notes");});
            b.setTextSize(12);b.setBackground(round(0xFF3D2C27,9));LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(0,dp(42),1);bp.setMargins(dp(4),dp(4),dp(4),0);shortcuts.addView(b,bp);
        }
        profile.addView(shortcuts);body.addView(profile);
        LinearLayout panel=column();panel.setPadding(dp(18),dp(12),dp(18),dp(18));body.addView(panel);
        LinearLayout tabs=horizontal();String[] labels={getString(R.string.ui_music),getString(R.string.ui_favorites_2),getString(R.string.ui_notes)},values={"music","favorites","notes"};
        for(int i=0;i<values.length;i++){final String value=values[i];TextView tab=button(labels[i],labels[i],v->{mineTab=value;showPage();});tab.setTypeface(null,Typeface.BOLD);tab.setTextColor(mineTab.equals(value)?TEXT:MUTED);tabs.addView(tab,new LinearLayout.LayoutParams(0,dp(48),1));}panel.addView(tabs);
        if(mineTab.equals("music")){
            tile(panel,"heart-filled",getString(R.string.ui_liked_songs),personal.favorites().size()+getString(R.string.ui_tracks_saved_locally),()->navigate("favorites"),null);
            tile(panel,"folder",getString(R.string.ui_saved_folder_albums),personal.albums().size()+getString(R.string.ui_albums_3),()->navigate("favorite-albums"),null);
            LinearLayout labelRow=horizontal();labelRow.addView(text(getString(R.string.ui_your_playlists),14,MUTED),new LinearLayout.LayoutParams(0,dp(44),1));labelRow.addView(icon("plus",getString(R.string.ui_new_playlist),v->playlistNameDialog(null,null)),new LinearLayout.LayoutParams(dp(44),dp(44)));panel.addView(labelRow);
            for(PersonalLibrary.Playlist p:personal.playlists())tile(panel,"queue",p.name,p.count+getString(R.string.ui_tracks_2),()->{selectedPlaylist=p.id;navigate("playlist");},()->playlistOptions(p));
            tile(panel,"plus",getString(R.string.ui_new_playlist),getString(R.string.ui_organize_your_own_music),()->playlistNameDialog(null,null),null);
        } else if(mineTab.equals("favorites")){
            tile(panel,"heart-filled",getString(R.string.ui_favorite_songs),personal.favorites().size()+getString(R.string.ui_tracks_2),()->navigate("favorites"),null);
            tile(panel,"folder",getString(R.string.ui_saved_folder_albums),personal.albums().size()+getString(R.string.ui_folders_music_subfolders),()->navigate("favorite-albums"),null);
        } else renderNotes(panel,personal.notes(null));
    }
    private void tile(LinearLayout parent,String kind,String title,String subtitle,Runnable action,Runnable more){
        LinearLayout row=horizontal();row.setPadding(0,dp(10),0,dp(10));
        MusicIcon art=icon(kind,title,v->action.run());art.setBackground(round(0xFF24232A,9));art.setTint(kind.equals("heart-filled")?0xFFE2B6B9:0xFFC3C5D0);row.addView(art,new LinearLayout.LayoutParams(dp(54),dp(54)));
        LinearLayout labels=column();labels.setPadding(dp(14),0,dp(4),0);TextView name=text(title,16,TEXT);name.setMaxLines(2);name.setEllipsize(TextUtils.TruncateAt.END);labels.addView(name);
        TextView detail=text(subtitle,12,MUTED);detail.setMaxLines(2);detail.setEllipsize(TextUtils.TruncateAt.END);labels.addView(detail);row.addView(labels,new LinearLayout.LayoutParams(0,-2,1));
        labels.setOnClickListener(v->action.run());row.setOnClickListener(v->action.run());
        if(more!=null){row.addView(icon("more",getString(R.string.ui_more)+title,v->more.run()),new LinearLayout.LayoutParams(dp(44),dp(48)));row.setOnLongClickListener(v->{more.run();return true;});}
        parent.addView(row,new LinearLayout.LayoutParams(-1,-2));
    }
    private void playlistNameDialog(PersonalLibrary.Playlist playlist,List<Track> initialTracks){
        EditText input=new EditText(this);input.setSingleLine(true);input.setHint(getString(R.string.ui_for_example_evening_walk));input.setTextColor(TEXT);input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(60)});
        if(playlist!=null)input.setText(playlist.name);
        LinearLayout box=column();box.setPadding(dp(24),dp(12),dp(24),0);box.addView(input);
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle(playlist==null?getString(R.string.ui_new_playlist):getString(R.string.ui_rename_playlist)).setView(box).setNegativeButton(getString(R.string.ui_cancel),null).setPositiveButton(getString(R.string.ui_save),null).create();
        dialog.setOnShowListener(v->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(b->{
            try{
                if(playlist==null){String id=personal.createPlaylist(input.getText().toString());if(initialTracks!=null)personal.addToPlaylist(id,initialTracks);}
                else personal.renamePlaylist(playlist.id,input.getText().toString());
                dialog.dismiss();if(!page.equals("player"))showPage();toast(getString(R.string.ui_playlist_saved));
            }catch(android.database.sqlite.SQLiteConstraintException e){input.setError(getString(R.string.ui_a_playlist_with_that_name_already_exists));}
            catch(IllegalArgumentException e){input.setError(e.getMessage());}
        }));dialog.show();
    }
    private void playlistOptions(PersonalLibrary.Playlist playlist){
        new AlertDialog.Builder(this).setTitle(playlist.name).setItems(new String[]{getString(R.string.ui_rename),getString(R.string.ui_delete_playlist)},(d,i)->{
            if(i==0)playlistNameDialog(playlist,null);
            else new AlertDialog.Builder(this).setTitle(getString(R.string.ui_delete_this_playlist)).setMessage(getString(R.string.ui_only_the_playlist_is_deleted_audio_files_favorites_and_notes_a))
                    .setNegativeButton(getString(R.string.ui_cancel),null).setPositiveButton(getString(R.string.ui_delete),(a,b)->{personal.deletePlaylist(playlist.id);navigate("mine");}).show();
        }).show();
    }
    private void choosePlaylist(List<Track> songs){
        if(songs.isEmpty()){toast(getString(R.string.ui_no_songs_to_add));return;}
        List<PersonalLibrary.Playlist> lists=personal.playlists();String[] labels=new String[lists.size()+1];labels[0]=getString(R.string.ui_new_playlist_2);
        for(int i=0;i<lists.size();i++)labels[i+1]=lists.get(i).name;
        new AlertDialog.Builder(this).setTitle(getString(R.string.ui_add_to_playlist_3)+songs.size()+getString(R.string.ui_tracks_2)).setItems(labels,(d,which)->{
            if(which==0)playlistNameDialog(null,songs);
            else{personal.addToPlaylist(lists.get(which-1).id,songs);toast(getString(R.string.ui_added_to)+lists.get(which-1).name+"」");if(!page.equals("player"))showPage();}
        }).show();
    }
    private void showPersonalTracks(){
        boolean favorites=page.equals("favorites");PersonalLibrary.Playlist chosen=null;
        if(!favorites)for(PersonalLibrary.Playlist p:personal.playlists())if(p.id.equals(selectedPlaylist))chosen=p;
        LinearLayout body=scrollBody();backHeader(body,favorites?getString(R.string.ui_liked_songs):chosen==null?getString(R.string.ui_playlist):chosen.name,"mine");
        List<Track> songs=favorites?personal.favorites():personal.playlistTracks(selectedPlaylist);
        TextView info=text(songs.size()+getString(R.string.ui_tracks_stored_locally),13,MUTED);addSpaced(body,info);
        if(!songs.isEmpty())addAction(body,getString(R.string.ui_play_all),v->{play(songs,0);navigate("player");});
        if(!favorites){addAction(body,getString(R.string.ui_add_songs),v->selectTracksForPlaylist());final PersonalLibrary.Playlist p=chosen;if(p!=null)addAction(body,getString(R.string.ui_manage_playlist),v->playlistOptions(p));}
        if(songs.isEmpty())addNote(body,getString(R.string.ui_no_songs_yet),favorites?getString(R.string.ui_tap_the_heart_while_listening_or_favorite_a_song_from_its_opti):getString(R.string.ui_add_songs_here_or_from_the_player));
        for(int i=0;i<songs.size();i++){final int index=i;Track t=songs.get(i);tile(body,"queue",t.title,t.album,()->{play(songs,index);navigate("player");},()->trackOptions(t,favorites?null:selectedPlaylist));}
    }
    private void selectTracksForPlaylist(){
        if(tracks.isEmpty()){toast(getString(R.string.ui_add_a_music_folder_in_settings_first));return;}
        List<Track> available=new ArrayList<>(tracks);Set<String> existing=new HashSet<>();for(Track t:personal.playlistTracks(selectedPlaylist))existing.add(t.id);
        List<Track> candidates=new ArrayList<>();for(Track t:available)if(!existing.contains(t.id))candidates.add(t);
        if(candidates.isEmpty()){toast(getString(R.string.ui_all_songs_are_already_in_this_playlist));return;}
        String[] labels=new String[candidates.size()];boolean[] selected=new boolean[candidates.size()];
        for(int i=0;i<labels.length;i++)labels[i]=candidates.get(i).title+" · "+candidates.get(i).album;
        final String playlist=selectedPlaylist;
        new AlertDialog.Builder(this).setTitle(getString(R.string.ui_choose_songs)).setMultiChoiceItems(labels,selected,(d,which,checked)->selected[which]=checked)
                .setNegativeButton(getString(R.string.ui_cancel),null).setPositiveButton(getString(R.string.ui_add),(d,w)->{List<Track> songs=new ArrayList<>();for(int i=0;i<selected.length;i++)if(selected[i])songs.add(candidates.get(i));personal.addToPlaylist(playlist,songs);showPage();}).show();
    }
    private void showFavoriteAlbums(){
        LinearLayout body=scrollBody();backHeader(body,getString(R.string.ui_saved_folder_albums),"mine");
        List<PersonalLibrary.Album> albums=personal.albums();if(albums.isEmpty())addNote(body,getString(R.string.ui_save_an_album_you_love),getString(R.string.ui_open_a_folder_album_on_home_and_tap_save_this_folder_album));
        for(PersonalLibrary.Album a:albums){int count=0;for(Track t:tracks)if(t.albumId.equals(a.id))count++;final int available=count;
            tile(body,"folder",a.name,count>0?count+getString(R.string.ui_tracks)+a.source:getString(R.string.ui_folder_unavailable_check_access_permissions),()->{selectedAlbum=a.id;query="";navigate("albums");},()->new AlertDialog.Builder(this).setTitle(a.name).setItems(new String[]{getString(R.string.ui_remove_favorite)},(d,i)->{personal.toggleAlbum(a.id,a.name,a.source);showPage();}).show());
        }
    }
    private Track currentTrack(){
        if(controller==null||controller.getCurrentMediaItem()==null){toast(getString(R.string.ui_choose_a_song_first));return null;}
        MediaItem item=controller.getCurrentMediaItem();for(Track t:tracks)if(t.id.equals(item.mediaId))return t;
        String uri=item.localConfiguration==null?"":item.localConfiguration.uri.toString();
        // Controller media items may omit local configuration. Saved snapshots retain the URI.
        Track saved = personal.savedTrack(item.mediaId); if (saved != null) return saved;
        if(uri.isEmpty()){toast(getString(R.string.ui_this_song_is_no_longer_in_the_library_scan_again));return null;}
        return Track.restore(item.mediaId,uri,String.valueOf(controller.getMediaMetadata().title),String.valueOf(controller.getMediaMetadata().albumTitle),String.valueOf(controller.getMediaMetadata().albumTitle),getString(R.string.ui_play_queue));
    }
    private void updateFavoriteIcon(){
        if(favoriteIcon==null||controller==null)return;MediaItem item=controller.getCurrentMediaItem();boolean liked=item!=null&&personal.isFavorite(item.mediaId);
        favoriteIcon.setKind(liked?"heart-filled":"heart");favoriteIcon.setTint(liked?0xFFEBA6AE:0xFFD3D9E2);favoriteIcon.setContentDescription(liked?getString(R.string.ui_unfavorite_song):getString(R.string.ui_favorite_song));
    }
    private void toggleCurrentFavorite(){Track t=currentTrack();if(t!=null){boolean liked=personal.toggleFavorite(t);updateFavoriteIcon();toast(liked?getString(R.string.ui_added_to_liked_songs):getString(R.string.ui_removed_from_favorites));}}
    private void trackOptions(Track track,String playlist){
        List<String> actions=new ArrayList<>(Arrays.asList(getString(R.string.ui_play),getString(R.string.ui_add_to_playlist_2),personal.isFavorite(track.id)?getString(R.string.ui_remove_favorite):getString(R.string.ui_favorite_song),getString(R.string.ui_add_note),getString(R.string.rename_file),getString(R.string.edit_tags)));if(playlist!=null)actions.add(getString(R.string.ui_remove_from_this_playlist));
        new AlertDialog.Builder(this).setTitle(track.title).setItems(actions.toArray(new String[0]),(d,which)->{
            if(which==0){play(Collections.singletonList(track),0);navigate("player");}
            else if(which==1)choosePlaylist(Collections.singletonList(track));
            else if(which==2){personal.toggleFavorite(track);if(page.equals("player"))updateFavoriteIcon();else showPage();}
            else if(which==3){long position=controller!=null&&controller.getCurrentMediaItem()!=null&&controller.getCurrentMediaItem().mediaId.equals(track.id)?controller.getCurrentPosition():0;noteEditor(track,position,null);}
            else if(which==4)renameEditor(track);
            else if(which==5)tagsEditor(track);
            else{personal.removeFromPlaylist(playlist,track.id);showPage();}
        }).show();
    }
    private boolean matchesTrack(Track track){
        String q=query.trim().toLowerCase(Locale.ROOT);if(q.startsWith("#"))q=q.substring(1);
        String tags=tagIndex.get(track.id);return track.matches(query)||(tags!=null&&tags.toLowerCase(Locale.ROOT).contains(q));
    }
    private void tagsEditor(Track track){
        EditText input=new EditText(this);input.setText(tagIndex.containsKey(track.id)?tagIndex.get(track.id):"");input.setHint(getString(R.string.tags_hint));input.setTextColor(TEXT);input.setMaxLines(4);
        LinearLayout body=column();body.setPadding(dp(24),dp(12),dp(24),dp(12));body.addView(input);body.addView(text(getString(R.string.tags_help),13,MUTED));
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle(getString(R.string.edit_tags)).setView(body).setNegativeButton(getString(R.string.ui_cancel),null).setPositiveButton(getString(R.string.ui_save),null).create();
        dialog.setOnShowListener(v->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(b->{try{personal.setTags(track,input.getText().toString());tagIndex=personal.tagIndex();dialog.dismiss();if(!page.equals("player"))showPage();}catch(IllegalArgumentException e){input.setError(getString(R.string.tags_help));}}));dialog.show();
    }
    private void renameEditor(Track track){
        if(track.uri.startsWith("asset:")){toast(getString(R.string.rename_readonly));return;}
        EditText input=new EditText(this);input.setSingleLine();input.setText(track.title);input.setSelectAllOnFocus(true);input.setTextColor(TEXT);
        LinearLayout body=column();body.setPadding(dp(24),dp(12),dp(24),dp(12));body.addView(input);body.addView(text(getString(R.string.rename_help),13,MUTED));
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle(getString(R.string.rename_file)).setView(body).setNegativeButton(getString(R.string.ui_cancel),null).setPositiveButton(getString(R.string.ui_save),null).create();
        dialog.setOnShowListener(v->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(b->{
            String name;try{name=AudioRenamer.validate(input.getText().toString());}catch(IllegalArgumentException e){input.setError(getString(R.string.rename_invalid));return;}
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);scanVersion++;if(scanTask!=null)scanTask.cancel(true);
            mediaWorker.submit(()->{try{
                Lyrics previousLyrics=LocalLyrics.load(getApplicationContext(),track);
                Track updated=AudioRenamer.rename(getApplicationContext(),track,name);
                try(PersonalLibrary db=new PersonalLibrary(getApplicationContext())){db.renamed(track,updated);}
                try{LocalLyrics.preserveAfterRename(getApplicationContext(),updated.id,previousLyrics);}catch(Exception ignored){}
                handler.post(()->{if(isDestroyed())return;tagIndex=personal.tagIndex();
                    if(controller!=null){
                        int current=controller.getCurrentMediaItemIndex();long position=controller.getCurrentPosition();boolean resume=controller.getPlayWhenReady();boolean changedCurrent=false;
                        for(int i=0;i<controller.getMediaItemCount();i++){MediaItem item=controller.getMediaItemAt(i);if(item.mediaId.equals(track.id)){changedCurrent|=i==current;controller.replaceMediaItem(i,item.buildUpon().setMediaId(updated.id).setUri(updated.uri).setMediaMetadata(item.mediaMetadata.buildUpon().setTitle(updated.title).build()).build());}}
                        if(changedCurrent){controller.seekTo(current,position);controller.prepare();controller.setPlayWhenReady(resume);}
                    }
                    dialog.dismiss();lyricsTrackId=null;scan();showPage();toast(getString(R.string.rename_done));});
            }catch(Exception e){handler.post(()->{if(isDestroyed())return;dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);input.setError(getString(R.string.rename_failed));scan();});}});
        }));dialog.show();
    }
    private void addCurrentNote(){Track track=currentTrack();if(track!=null)noteEditor(track,controller.getCurrentPosition(),null);}
    private void noteEditor(Track track,long position,PersonalLibrary.Note existing){
        LinearLayout body=column();body.setPadding(dp(22),dp(12),dp(22),0);body.addView(text(track.title+" · "+formatTime(position),13,MUTED));
        EditText input=new EditText(this);input.setTextColor(TEXT);input.setHint(getString(R.string.ui_write_down_your_thoughts));input.setGravity(Gravity.TOP);input.setMinLines(4);input.setMaxLines(8);input.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE|android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(5000)});if(existing!=null)input.setText(existing.body);body.addView(input);
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle(existing==null?getString(R.string.ui_add_playback_note):getString(R.string.ui_edit_note)).setView(body).setNegativeButton(getString(R.string.ui_cancel),null).setPositiveButton(getString(R.string.ui_save),null).create();
        dialog.setOnShowListener(v->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(b->{
            try{if(existing==null)personal.addNote(track,position,input.getText().toString());else personal.editNote(existing.id,input.getText().toString());dialog.dismiss();toast(getString(R.string.ui_note_saved_on_this_device));if(page.equals("notes")||page.equals("mine"))showPage();}
            catch(IllegalArgumentException e){input.setError(e.getMessage());}
        }));dialog.show();
    }
    private void showNotes(){LinearLayout body=scrollBody();backHeader(body,getString(R.string.ui_playback_notes),"mine");renderNotes(body,personal.notes(null));}
    private void renderNotes(LinearLayout body,List<PersonalLibrary.Note> notes){
        if(notes.isEmpty()){addNote(body,getString(R.string.ui_capture_this_moment),getString(R.string.ui_tap_the_note_icon_while_listening_to_save_a_thought_and_its_ti));return;}
        for(PersonalLibrary.Note n:notes){String summary=n.body.length()>85?n.body.substring(0,85)+"…":n.body;tile(body,"note",summary,n.track.title+" · "+formatTime(n.position),()->noteDetails(n),null);}
    }
    private void showTrackNotes(Track track){
        List<PersonalLibrary.Note> notes=personal.notes(track.id);if(notes.isEmpty()){toast(getString(R.string.ui_no_notes_for_this_song_yet));return;}
        String[] titles=new String[notes.size()];for(int i=0;i<titles.length;i++)titles[i]=formatTime(notes.get(i).position)+"  "+notes.get(i).body;
        new AlertDialog.Builder(this).setTitle(getString(R.string.ui_view_notes)).setItems(titles,(d,i)->noteDetails(notes.get(i))).setNegativeButton(getString(R.string.ui_close),null).show();
    }
    private void noteDetails(PersonalLibrary.Note note){
        new AlertDialog.Builder(this).setTitle(note.track.title+" · "+formatTime(note.position)).setMessage(note.body)
                .setPositiveButton(getString(R.string.ui_play_from_here),(d,w)->{
                    if(!ready())return;MediaItem current=controller.getCurrentMediaItem();
                    if(current==null||!current.mediaId.equals(note.track.id))play(Collections.singletonList(note.track),0);
                    controller.seekTo(note.position);controller.prepare();controller.play();navigate("player");
                }).setNeutralButton(getString(R.string.ui_edit),(d,w)->noteEditor(note.track,note.position,note))
                .setNegativeButton(getString(R.string.ui_delete),(d,w)->new AlertDialog.Builder(this).setTitle(getString(R.string.ui_delete_this_note)).setNegativeButton(getString(R.string.ui_cancel),null).setPositiveButton(getString(R.string.ui_delete),(a,b)->{personal.deleteNote(note.id);if(page.equals("notes")||page.equals("mine"))showPage();}).show()).show();
    }
    private LinearLayout scrollBody() {
        ScrollView scroll = new ScrollView(this); scroll.setFillViewport(true); content.addView(scroll);
        LinearLayout body = column(); body.setPadding(dp(22), dp(12), dp(22), dp(22)); scroll.addView(body); return body;
    }
    private void title(LinearLayout body, String title, String subtitle) {
        TextView h = text(title, 28, TEXT); h.setTypeface(null, Typeface.BOLD); addSpaced(body, h);
        addSpaced(body, text(subtitle, 14, MUTED));
    }
    private void addNote(LinearLayout body, String heading, String note) {
        TextView label = text(heading, 17, TEXT); label.setTypeface(null, Typeface.BOLD); addSpaced(body, label);
        TextView description = text(note, 14, MUTED); description.setLineSpacing(dp(4), 1); addSpaced(body, description);
    }
    private void addAction(LinearLayout body, String label, View.OnClickListener action) {
        TextView b = button(label, label, action); b.setBackground(round(SURFACE, 16));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, dp(54)); p.setMargins(0, dp(12), 0, dp(8)); body.addView(b, p);
    }
    private void addSpaced(LinearLayout body, View view) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2); p.setMargins(0, dp(12), 0, dp(8)); body.addView(view, p);
    }
    private TextView text(String value, int size, int color) {
        TextView view = new TextView(this); view.setText(value); view.setTextSize(size); view.setTextColor(color); view.setGravity(Gravity.CENTER_VERTICAL); return view;
    }
    private TextView button(String value, String description, View.OnClickListener action) {
        TextView b = text(value, 18, TEXT); b.setGravity(Gravity.CENTER); b.setMinWidth(dp(48)); b.setMinHeight(dp(48));
        b.setPadding(dp(6), dp(4), dp(6), dp(4)); b.setContentDescription(description); b.setOnClickListener(action);
        b.setBackground(new android.graphics.drawable.RippleDrawable(ColorStateList.valueOf(0x33666666), null, round(Color.WHITE, 14)));
        return b;
    }
    private LinearLayout column() { LinearLayout layout = new LinearLayout(this); layout.setOrientation(LinearLayout.VERTICAL); return layout; }
    private LinearLayout horizontal() { LinearLayout layout = new LinearLayout(this); layout.setGravity(Gravity.CENTER_VERTICAL); return layout; }
    private GradientDrawable round(int color, int radius) { GradientDrawable d = new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(radius)); return d; }
    private int dp(float value) { return (int) (value * getResources().getDisplayMetrics().density + .5f); }
    private void toast(String message) { Toast.makeText(this, message, Toast.LENGTH_SHORT).show(); }
    private static final class Row {
        final int kind; final String title, subtitle; final Runnable action; int tint; Runnable longAction;
        Row(int kind, String title, String subtitle, Runnable action) { this.kind = kind; this.title = title; this.subtitle = subtitle; this.action = action; }
    }
    private final class LibraryAdapter extends RecyclerView.Adapter<LibraryAdapter.Holder> {
        final class Holder extends RecyclerView.ViewHolder {
            final LinearLayout box; final TextView art, title, subtitle;
            Holder(LinearLayout box, TextView art, TextView title, TextView subtitle) { super(box); this.box = box; this.art = art; this.title = title; this.subtitle = subtitle; }
        }
        @Override public Holder onCreateViewHolder(ViewGroup parent, int type) {
            LinearLayout box = column(); TextView art = text("◉", 34, TEXT), title = text("", 17, TEXT), subtitle = text("", 12, MUTED);
            title.setTypeface(null, Typeface.BOLD); box.addView(art); box.addView(title); box.addView(subtitle);
            return new Holder(box, art, title, subtitle);
        }
        @Override public void onBindViewHolder(Holder h, int position) {
            Row row = rows.get(position); boolean card = row.kind == 2, hero = row.kind == 1 || row.kind == 5, header = row.kind == 0;
            RecyclerView.LayoutParams p = new RecyclerView.LayoutParams(-1, -2); p.setMargins(card ? dp(4) : 0, dp(6), card ? dp(4) : 0, dp(6)); h.box.setLayoutParams(p);
            h.box.setPadding(dp(header ? 0 : 16), dp(header ? 8 : 18), dp(header ? 0 : 16), dp(header ? 8 : 18));
            h.box.setMinimumHeight(dp(card ? 154 : row.kind == 5 ? 116 : hero ? 186 : 0));
            h.box.setBackground(header ? null : round(hero ? 0xFF344A45 : card ? row.tint : SURFACE, 18));
            h.art.setVisibility(card ? View.VISIBLE : View.GONE);
            h.title.setText(row.title); h.title.setTextSize(row.kind == 5 ? 22 : hero ? 25 : header ? 20 : 16);
            h.title.setMaxLines(card || row.kind == 5 ? 2 : 3); h.title.setEllipsize(TextUtils.TruncateAt.END);
            h.subtitle.setText(row.subtitle); h.subtitle.setTextColor(hero ? ACCENT : card ? 0xFFE8E8ED : MUTED);
            h.subtitle.setVisibility(header ? View.GONE : View.VISIBLE); h.subtitle.setPadding(0, dp(8), 0, 0);
            h.subtitle.setMaxLines(hero ? 4 : 2); h.subtitle.setEllipsize(TextUtils.TruncateAt.END);
            h.box.setOnClickListener(row.action == null ? null : v -> row.action.run());
            h.box.setOnLongClickListener(row.longAction == null ? null : v -> { row.longAction.run(); return true; });
            h.box.setContentDescription(row.title + "，" + row.subtitle);
        }
        @Override public int getItemCount() { return rows.size(); }
    }
}


