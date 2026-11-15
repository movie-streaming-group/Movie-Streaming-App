
package com.streamdemo.ui;

import com.streamdemo.audio.SoundManager;
import com.streamdemo.model.Movie;
import com.streamdemo.player.PlayerServer;

import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.HostServices;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.DoubleBinding;
import javafx.concurrent.Worker;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.util.Duration;

import java.util.Locale;
import java.util.prefs.Preferences;

public class PlayerView extends StackPane {

    private final PlayerServer server;
    private final HostServices hostServices;

    private final Label titleLabel = new Label();
    private final StackPane stage = new StackPane();
    private final StackPane loadingOverlay = new StackPane();

    private final Label statusLabel =
            new Label("LOADING YOUR TRAILER...");

    private final Label logoLabel =
            new Label("STREAMBOX");

    private final Label playPauseLabel =
            new Label("PAUSE");

    private final Label muteLabel =
            new Label("MUTE");

    private final Label currentTimeLabel =
            new Label("00:00");

    private final Label durationLabel =
            new Label("00:00");

    private final Slider progressSlider = new Slider(0, 100, 0);
    private final Slider volumeSlider = new Slider(0, 100, 70);

    private WebView webView;
    private WebEngine engine;

    private Timeline loadingCheck;
    private Timeline progressUpdater;

    private Movie currentMovie;

    private boolean playing = true;
    private boolean muted = false;
    private boolean playerReady = false;
    private boolean updatingProgress = false;

    public PlayerView(PlayerServer server, HostServices hostServices) {

        this.server = server;
        this.hostServices = hostServices;

        setStyle("-fx-background-color: #000000;");
        setVisible(false);

        // TOP BAR
        Label back = textControl("BACK", false);
        back.setOnMouseClicked(e -> close());

        titleLabel.setStyle(
                "-fx-text-fill: white;" +
                "-fx-font-size: 20px;" +
                "-fx-font-weight: bold;"
        );

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label browser = textControl("OPEN IN BROWSER", false);
        browser.setOnMouseClicked(e -> openInBrowser());

        HBox topBar = new HBox(
                20, back, titleLabel, spacer, browser
        );

        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(18, 25, 15, 25));

        // VIDEO STAGE
        StackPane stageHolder = new StackPane(stage);

        DoubleBinding stageWidth =
                Bindings.createDoubleBinding(
                        () -> Math.max(
                                320,
                                Math.min(
                                        stageHolder.getWidth() - 60,
                                        (stageHolder.getHeight() - 40)
                                                * 16.0 / 9.0
                                )
                        ),
                        stageHolder.widthProperty(),
                        stageHolder.heightProperty()
                );

        stage.prefWidthProperty().bind(stageWidth);
        stage.maxWidthProperty().bind(stageWidth);

        stage.prefHeightProperty().bind(
                stageWidth.multiply(9.0 / 16.0)
        );

        stage.maxHeightProperty().bind(
                stageWidth.multiply(9.0 / 16.0)
        );

        stage.setStyle("-fx-background-color: black;");

        // LOADING SCREEN
        VBox loadingContent = new VBox(18);

        logoLabel.setStyle(
                "-fx-text-fill: #E50914;" +
                "-fx-font-size: 32px;" +
                "-fx-font-weight: bold;"
        );

        statusLabel.setStyle(
                "-fx-text-fill: white;" +
                "-fx-font-size: 15px;" +
                "-fx-font-weight: bold;"
        );

        loadingContent.setAlignment(Pos.CENTER);
        loadingContent.getChildren().addAll(
                logoLabel, statusLabel
        );

        loadingOverlay.setStyle("-fx-background-color: black;");
        loadingOverlay.getChildren().add(loadingContent);

        stage.getChildren().add(loadingOverlay);

        FadeTransition fade = new FadeTransition(
                Duration.seconds(1.2), logoLabel
        );

        fade.setFromValue(0.25);
        fade.setToValue(1.0);
        fade.setAutoReverse(true);
        fade.setCycleCount(Animation.INDEFINITE);
        fade.play();

        // PLAYBACK CONTROLS
        Label rewind = textControl("BACK 10s", false);
        Label forward = textControl("FORWARD 10s", false);

        playPauseLabel.setStyle(
                "-fx-text-fill: white;" +
                "-fx-font-size: 14px;" +
                "-fx-font-weight: bold;" +
                "-fx-padding: 12 25;" +
                "-fx-background-color: #E50914;" +
                "-fx-background-radius: 5;" +
                "-fx-cursor: hand;"
        );

        rewind.setOnMouseClicked(e -> seekBy(-10));
        forward.setOnMouseClicked(e -> seekBy(10));

        playPauseLabel.setOnMouseClicked(
                e -> togglePlayback()
        );

        HBox playbackControls = new HBox(
                18, rewind, playPauseLabel, forward
        );

        playbackControls.setAlignment(Pos.CENTER);

        // PROGRESS BAR
        progressSlider.setMin(0);
        progressSlider.setMax(100);
        progressSlider.setValue(0);
        progressSlider.setStyle(
                "-fx-control-inner-background: #444444;"
        );

        HBox.setHgrow(progressSlider, Priority.ALWAYS);

        progressSlider.valueChangingProperty().addListener(
                (obs, oldValue, changing) -> {
                    if (!changing && playerReady) {
                        seekTo(progressSlider.getValue());
                    }
                }
        );

        progressSlider.setOnMouseReleased(e -> {
            if (playerReady) {
                seekTo(progressSlider.getValue());
            }
        });

        HBox timeRow = new HBox(
                10,
                currentTimeLabel,
                progressSlider,
                durationLabel
        );

        timeRow.setAlignment(Pos.CENTER);
        timeRow.setPadding(new Insets(0, 5, 0, 5));

        currentTimeLabel.setStyle(
                "-fx-text-fill: white; -fx-font-size: 12px;"
        );

        durationLabel.setStyle(
                "-fx-text-fill: white; -fx-font-size: 12px;"
        );

        // VOLUME CONTROLS
        Label volumeTitle = new Label("VOLUME");

        volumeTitle.setStyle(
                "-fx-text-fill: white;" +
                "-fx-font-size: 12px;" +
                "-fx-font-weight: bold;"
        );

        volumeSlider.setPrefWidth(130);

        volumeSlider.valueProperty().addListener(
                (obs, oldValue, newValue) -> {
                    if (playerReady && !muted) {
                        setPlayerVolume(newValue.intValue());
                    }
                }
        );

        muteLabel.setOnMouseClicked(e -> toggleMute());

        HBox volumeControls = new HBox(
                12, volumeTitle, volumeSlider, muteLabel
        );

        volumeControls.setAlignment(Pos.CENTER);
        volumeControls.setPadding(new Insets(10));

        // BOTTOM LAYOUT
        VBox controls = new VBox(
                15,
                timeRow,
                playbackControls,
                volumeControls
        );

        controls.setAlignment(Pos.CENTER);
        controls.setPadding(new Insets(12, 25, 20, 25));

        BorderPane layout = new BorderPane();
        layout.setTop(topBar);
        layout.setCenter(stageHolder);
        layout.setBottom(controls);

        getChildren().add(layout);

        ensureWebView();
    }

    private Label textControl(String text, boolean primary) {

        Label label = new Label(text);

        label.setStyle(
                "-fx-text-fill: white;" +
                "-fx-font-size: 13px;" +
                "-fx-font-weight: bold;" +
                "-fx-padding: 10 15;" +
                "-fx-background-color: " +
                (primary ? "#E50914" : "#292929") +
                ";" +
                "-fx-background-radius: 5;" +
                "-fx-cursor: hand;"
        );

        return label;
    }

    private void ensureWebView() {

        if (webView != null) return;

        webView = new WebView();
        webView.setContextMenuEnabled(false);

        webView.setMaxSize(
                Double.MAX_VALUE,
                Double.MAX_VALUE
        );

        engine = webView.getEngine();

        engine.getLoadWorker().stateProperty().addListener(
                (obs, oldState, newState) -> {

                    if (newState == Worker.State.SUCCEEDED) {
                        startLoadingCheck();
                    }

                    if (newState == Worker.State.FAILED) {
                        statusLabel.setText(
                                "UNABLE TO LOAD TRAILER."
                        );
                    }
                }
        );

        stage.getChildren().add(0, webView);
    }

    private void startLoadingCheck() {

        if (loadingCheck != null) {
            loadingCheck.stop();
        }

        final int[] attempts = {0};

        loadingCheck = new Timeline(
                new KeyFrame(Duration.millis(500), event -> {

                    attempts[0]++;

                    try {
                        Object ready = engine.executeScript(
                                "window.streamBoxReady === true"
                        );

                        Object error = engine.executeScript(
                                "window.streamBoxError === true"
                        );

                        if (Boolean.TRUE.equals(ready)) {

                            playerReady = true;
                            loadingOverlay.setVisible(false);

                            setPlayerVolume(
                                    (int) volumeSlider.getValue()
                            );

                            startProgressUpdater();
                            loadingCheck.stop();

                        } else if (Boolean.TRUE.equals(error)) {

                            statusLabel.setText(
                                    "THIS TRAILER IS UNAVAILABLE."
                            );

                            loadingCheck.stop();

                        } else if (attempts[0] >= 40) {

                            statusLabel.setText(
                                    "TRAILER IS TAKING TOO LONG TO LOAD."
                            );

                            loadingCheck.stop();
                        }

                    } catch (Exception ex) {
                        // YouTube is still initializing.
                    }
                })
        );

        loadingCheck.setCycleCount(
                Animation.INDEFINITE
        );

        loadingCheck.play();
    }

    private void startProgressUpdater() {

        if (progressUpdater != null) {
            progressUpdater.stop();
        }

        progressUpdater = new Timeline(
                new KeyFrame(Duration.seconds(0.5), event -> {

                    if (!playerReady || progressSlider.isValueChanging()) {
                        return;
                    }

                    try {
                        double current = getPlayerNumber(
                                "streamBoxGetCurrentTime()"
                        );

                        double total = getPlayerNumber(
                                "streamBoxGetDuration()"
                        );

                        if (total > 0) {

                            updatingProgress = true;

                            progressSlider.setMax(total);
                            progressSlider.setValue(current);

                            updatingProgress = false;

                            currentTimeLabel.setText(
                                    formatTime(current)
                            );

                            durationLabel.setText(
                                    formatTime(total)
                            );
                        }

                    } catch (Exception ex) {
                        updatingProgress = false;
                    }
                })
        );

        progressUpdater.setCycleCount(
                Animation.INDEFINITE
        );

        progressUpdater.play();
    }

    private double getPlayerNumber(String javascript) {

        Object result = engine.executeScript(
                javascript
        );

        return result instanceof Number
                ? ((Number) result).doubleValue()
                : 0;
    }

    private String formatTime(double seconds) {

        int totalSeconds = (int) Math.max(0, seconds);

        int minutes = totalSeconds / 60;
        int remainingSeconds = totalSeconds % 60;

        return String.format(
                Locale.ROOT,
                "%02d:%02d",
                minutes,
                remainingSeconds
        );
    }

    private void seekBy(int seconds) {

        if (!playerReady) return;

        engine.executeScript(
                "streamBoxSeekBy(" + seconds + ")"
        );
    }

    private void seekTo(double seconds) {

        if (!playerReady || updatingProgress) return;

        engine.executeScript(
                "streamBoxSeekTo(" + seconds + ")"
        );
    }

    private void setPlayerVolume(int volume) {

        if (!playerReady) return;

        engine.executeScript(
                "streamBoxSetVolume(" + volume + ")"
        );
    }

    public void togglePlayback() {

        if (!playerReady) return;

        if (playing) {

            engine.executeScript("streamBoxPause()");

            playPauseLabel.setText("PLAY");
            playing = false;

        } else {

            engine.executeScript("streamBoxPlay()");

            playPauseLabel.setText("PAUSE");
            playing = true;
        }
    }

    private void toggleMute() {

        if (!playerReady) return;

        muted = !muted;

        if (muted) {
            engine.executeScript(
                    "if(player) player.mute()"
            );

            muteLabel.setText("UNMUTE");

        } else {
            engine.executeScript(
                    "if(player) player.unMute()"
            );

            muteLabel.setText("MUTE");

            setPlayerVolume(
                    (int) volumeSlider.getValue()
            );
        }
    }

    public boolean isOpen() {
        return isVisible();
    }

    public void open(Movie movie) {

        currentMovie = movie;

        Preferences.userNodeForPackage(PlayerView.class)
                .put("lastWatched", movie.title());

        titleLabel.setText(movie.title());

        loadingOverlay.setVisible(true);
        statusLabel.setText("LOADING YOUR TRAILER...");

        playerReady = false;
        playing = true;
        muted = false;

        playPauseLabel.setText("PAUSE");
        muteLabel.setText("MUTE");

        progressSlider.setValue(0);
        currentTimeLabel.setText("00:00");
        durationLabel.setText("00:00");

        if (loadingCheck != null) {
            loadingCheck.stop();
        }

        if (progressUpdater != null) {
            progressUpdater.stop();
        }

        setVisible(true);
        toFront();

        SoundManager.play(SoundManager.Effect.OPEN);

        engine.load(server.urlFor(movie.youtubeId()));
    }

    public void close() {

        if (!isVisible()) return;

        SoundManager.play(SoundManager.Effect.CLOSE);

        playerReady = false;

        if (loadingCheck != null) {
            loadingCheck.stop();
        }

        if (progressUpdater != null) {
            progressUpdater.stop();
        }

        if (engine != null) {
            engine.load("about:blank");
        }

        setVisible(false);
    }

    public void dispose() {
        close();
    }

    private void openInBrowser() {

        if (currentMovie != null) {
            hostServices.showDocument(
                    currentMovie.watchUrl()
            );
        }
    }
}
