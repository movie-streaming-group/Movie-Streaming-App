
package com.streamdemo.ui;

import com.streamdemo.audio.SoundManager;
import com.streamdemo.model.Movie;
import com.streamdemo.player.PlayerServer;

import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.application.HostServices;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.DoubleBinding;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.concurrent.Worker;
import javafx.util.Duration;

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
            new Label("Ⅱ  PAUSE");

    private WebView webView;
    private WebEngine engine;

    private Timeline loadingCheck;

    private Movie currentMovie;
    private boolean playing = true;

    public PlayerView(PlayerServer server, HostServices hostServices) {

        this.server = server;
        this.hostServices = hostServices;

        setStyle("-fx-background-color: #000000;");
        setVisible(false);

        // Top bar
        CustomButton back = new CustomButton(
                "Back",
                Icons.chevronLeft(16, Color.WHITE),
                CustomButton.Kind.SECONDARY,
                this::close
        );

        titleLabel.setStyle(
                "-fx-text-fill: white;" +
                "-fx-font-size: 20px;" +
                "-fx-font-weight: bold;"
        );

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        CustomButton openOnYoutube = new CustomButton(
                "Open in Browser",
                CustomButton.Kind.GHOST,
                this::openInBrowser
        );

        HBox topBar = new HBox(
                18, back, titleLabel, spacer, openOnYoutube
        );

        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(15, 30, 10, 30));

        // Video stage
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

        stage.setStyle(
                "-fx-background-color: black;" +
                "-fx-background-radius: 8;"
        );

        // Loading animation
        VBox loadingContent = new VBox(18);

        logoLabel.setStyle(
                "-fx-text-fill: #ff1428;" +
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

        loadingOverlay.setStyle("-fx-background-color: #000000;");
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

        // Playback controls
        playPauseLabel.setStyle(
                "-fx-text-fill: white;" +
                "-fx-font-size: 16px;" +
                "-fx-font-weight: bold;" +
                "-fx-padding: 12 24;" +
                "-fx-background-color: #e50914;" +
                "-fx-background-radius: 5;" +
                "-fx-cursor: hand;"
        );

        playPauseLabel.setOnMouseClicked(
                event -> togglePlayback()
        );

        HBox controls = new HBox(playPauseLabel);
        controls.setAlignment(Pos.CENTER);
        controls.setPadding(new Insets(15));

        // Main layout
        BorderPane layout = new BorderPane();

        layout.setTop(topBar);
        layout.setCenter(stageHolder);
        layout.setBottom(controls);

        getChildren().add(layout);

        ensureWebView();
    }

    private void ensureWebView() {

        if (webView != null) return;

        webView = new WebView();
        webView.setContextMenuEnabled(false);

        engine = webView.getEngine();

        engine.getLoadWorker().stateProperty().addListener(
                (obs, oldState, newState) -> {

                    if (newState == Worker.State.SUCCEEDED) {
                        startLoadingCheck();
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

                            loadingOverlay.setVisible(false);
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
                        // The page may still be initializing.
                    }
                })
        );

        loadingCheck.setCycleCount(Animation.INDEFINITE);
        loadingCheck.play();
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

        playing = true;
        playPauseLabel.setText("Ⅱ  PAUSE");

        setVisible(true);
        toFront();

        SoundManager.play(SoundManager.Effect.OPEN);

        if (loadingCheck != null) {
            loadingCheck.stop();
        }

        engine.load(server.urlFor(movie.youtubeId()));
    }

    public void togglePlayback() {

        if (engine == null || !isVisible()) return;

        if (playing) {
            sendYoutubeCommand("pauseVideo");
            playPauseLabel.setText("▶  PLAY");
            playing = false;

        } else {
            sendYoutubeCommand("playVideo");
            playPauseLabel.setText("Ⅱ  PAUSE");
            playing = true;
        }
    }

    private void sendYoutubeCommand(String command) {

        if (engine == null) return;

        try {

            String script =
                    "var frame = document.querySelector('iframe');" +
                    "if(frame) {" +
                    "frame.contentWindow.postMessage(" +
                    "JSON.stringify({" +
                    "event:'command'," +
                    "func:'" + command + "'," +
                    "args:[]" +
                    "}), '*');" +
                    "}";

            engine.executeScript(script);

        } catch (Exception ex) {
            System.out.println(
                    "Unable to control YouTube playback: "
                            + ex.getMessage()
            );
        }
    }

    public void close() {

        if (!isVisible()) return;

        SoundManager.play(SoundManager.Effect.CLOSE);

        if (loadingCheck != null) {
            loadingCheck.stop();
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
