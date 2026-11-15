
package com.streamdemo.ui;

import com.streamdemo.audio.SoundManager;
import com.streamdemo.model.Movie;

import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.beans.binding.DoubleBinding;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import java.util.function.Consumer;

public class MovieCard extends VBox {

    public MovieCard(
            Movie movie,
            DoubleBinding width,
            Consumer<Movie> onSelect
    ) {

        setSpacing(8);

        prefWidthProperty().bind(width);
        minWidthProperty().bind(width);
        maxWidthProperty().bind(width);

        setCursor(Cursor.HAND);

        DoubleBinding height = width.multiply(9.0 / 16.0);

        // PLACEHOLDER
        Region placeholder = new Region();

        placeholder.setStyle(
                "-fx-background-color: #262626;"
        );

        // THUMBNAIL
        ImageView image = new ImageView(
                new Image(movie.thumbnailUrl(), true)
        );

        image.setPreserveRatio(false);

        image.fitWidthProperty().bind(width);
        image.fitHeightProperty().bind(height);

        // PLAY OVERLAY
        Circle badge = new Circle(
                24,
                Color.rgb(0, 0, 0, 0.55)
        );

        badge.setStroke(Color.WHITE);
        badge.setStrokeWidth(2);

        Polygon triangle = Icons.play(18, Color.WHITE);
        triangle.setTranslateX(2);

        StackPane overlay = new StackPane(
                badge,
                triangle
        );

        overlay.setStyle(
                "-fx-background-color: linear-gradient(to bottom, " +
                "rgba(0,0,0,0.15), rgba(0,0,0,0.65));"
        );

        overlay.setOpacity(0);
        overlay.setMouseTransparent(true);

        // FRAME
        StackPane frame = new StackPane(
                placeholder,
                image,
                overlay
        );

        frame.prefWidthProperty().bind(width);
        frame.maxWidthProperty().bind(width);

        frame.prefHeightProperty().bind(height);
        frame.maxHeightProperty().bind(height);

        Rectangle clip = new Rectangle();

        clip.setArcWidth(14);
        clip.setArcHeight(14);

        clip.widthProperty().bind(width);
        clip.heightProperty().bind(height);

        frame.setClip(clip);

        StackPane thumb = new StackPane(frame);

        // TITLE
        Label title = new Label(movie.title());

        title.setWrapText(true);
        title.setMaxWidth(Double.MAX_VALUE);

        title.setStyle(
                "-fx-text-fill: #b3b3b3;" +
                "-fx-font-size: 15px;" +
                "-fx-font-weight: bold;"
        );

        width.addListener((obs, oldValue, newValue) -> {

            double fontSize = Math.max(
                    11,
                    Math.min(15, newValue.doubleValue() * 0.085)
            );

            title.setStyle(
                    "-fx-text-fill: #b3b3b3;" +
                    "-fx-font-size: " + fontSize + "px;" +
                    "-fx-font-weight: bold;"
            );
        });

        Label year = new Label(
                String.valueOf(movie.year())
        );

        year.setStyle(
                "-fx-text-fill: #777777;" +
                "-fx-font-size: 12px;"
        );

        getChildren().addAll(
                thumb,
                title,
                year
        );

        // HOVER ANIMATIONS
        ScaleTransition grow = new ScaleTransition(
                Duration.millis(180),
                this
        );

        ScaleTransition zoom = new ScaleTransition(
                Duration.millis(600),
                image
        );

        FadeTransition fade = new FadeTransition(
                Duration.millis(180),
                overlay
        );

        DropShadow glow = new DropShadow(
                30,
                Color.web("#e50914", 0.8)
        );

        glow.setSpread(0.12);

        setOnMouseEntered(e -> {

            setViewOrder(-1);

            grow.stop();
            grow.setToX(1.1);
            grow.setToY(1.1);
            grow.play();

            zoom.stop();
            zoom.setToX(1.12);
            zoom.setToY(1.12);
            zoom.play();

            fade.stop();
            fade.setToValue(1);
            fade.play();

            thumb.setEffect(glow);

            title.setStyle(
                    "-fx-text-fill: white;" +
                    "-fx-font-size: 15px;" +
                    "-fx-font-weight: bold;"
            );

            SoundManager.play(SoundManager.Effect.HOVER);

            ScaleTransition badgePulse =
                    new ScaleTransition(
                            Duration.millis(400),
                            badge
                    );

            badgePulse.setFromX(1.0);
            badgePulse.setFromY(1.0);
            badgePulse.setToX(1.15);
            badgePulse.setToY(1.15);
            badgePulse.setAutoReverse(true);
            badgePulse.setCycleCount(2);
            badgePulse.play();
        });

        setOnMouseExited(e -> {

            setViewOrder(0);

            grow.stop();
            grow.setToX(1.0);
            grow.setToY(1.0);
            grow.play();

            zoom.stop();
            zoom.setToX(1.0);
            zoom.setToY(1.0);
            zoom.play();

            fade.stop();
            fade.setToValue(0);
            fade.play();

            thumb.setEffect(null);

            title.setStyle(
                    "-fx-text-fill: #b3b3b3;" +
                    "-fx-font-size: 15px;" +
                    "-fx-font-weight: bold;"
            );
        });

        setOnMouseClicked(e -> {

            SoundManager.play(SoundManager.Effect.CLICK);

            onSelect.accept(movie);
        });
    }
}
