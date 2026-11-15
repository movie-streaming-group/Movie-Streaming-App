
package com.streamdemo.ui;

import com.streamdemo.model.Movie;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.value.ObservableDoubleValue;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import java.util.function.Consumer;

public class HeroBanner extends StackPane {

    private Movie currentMovie;

    private final ImageView picture = new ImageView();

    private final Label title = new Label();

    private final Label blurb = new Label();

    private final Consumer<Movie> onPlay;

    public HeroBanner(
            Movie movie,
            ObservableDoubleValue windowWidth,
            Consumer<Movie> onPlay
    ) {

        this.onPlay = onPlay;

        DoubleBinding height = Bindings.createDoubleBinding(
                () -> Math.max(
                        320,
                        Math.min(520, windowWidth.get() * 0.42)
                ),
                windowWidth
        );

        prefHeightProperty().bind(height);
        minHeightProperty().bind(height);
        maxHeightProperty().bind(height);

        setStyle("-fx-background-color: #000000;");

        Rectangle clip = new Rectangle();

        clip.widthProperty().bind(widthProperty());
        clip.heightProperty().bind(heightProperty());

        setClip(clip);

        // BACKGROUND IMAGE
        picture.setPreserveRatio(true);
        picture.fitWidthProperty().bind(widthProperty());

        // LEFT FADE
        Region fadeLeft = new Region();

        fadeLeft.setStyle(
                "-fx-background-color: linear-gradient(to right, " +
                "rgba(20,20,20,0.95) 0%, " +
                "rgba(20,20,20,0.6) 45%, " +
                "rgba(20,20,20,0) 100%);"
        );

        // BOTTOM FADE
        Region fadeBottom = new Region();

        fadeBottom.setStyle(
                "-fx-background-color: linear-gradient(to bottom, " +
                "rgba(20,20,20,0) 55%, #141414 100%);"
        );

        // FEATURED LABEL
        Label tag = new Label("FEATURED TRAILER");

        tag.setStyle(
                "-fx-text-fill: #e50914;" +
                "-fx-font-size: 14px;" +
                "-fx-font-weight: bold;"
        );

        // TITLE
        title.setWrapText(true);

        title.setStyle(
                "-fx-text-fill: white;" +
                "-fx-font-size: 52px;" +
                "-fx-font-weight: bold;"
        );

        title.setEffect(
                new DropShadow(12, Color.BLACK)
        );

        // DESCRIPTION
        blurb.setWrapText(true);

        blurb.setStyle(
                "-fx-text-fill: #e5e5e5;" +
                "-fx-font-size: 18px;"
        );

        blurb.setEffect(
                new DropShadow(8, Color.BLACK)
        );

        // PLAY VIDEO BUTTON - NO ICON
        CustomButton play = new CustomButton(
                "PLAY VIDEO",
                CustomButton.Kind.PRIMARY,
                () -> this.onPlay.accept(currentMovie)
        );

        HBox buttons = new HBox(12, play);

        buttons.setPadding(
                new Insets(10, 0, 0, 0)
        );

        VBox text = new VBox(
                10,
                tag,
                title,
                blurb,
                buttons
        );

        text.setMaxWidth(560);

        text.setPadding(
                new Insets(0, 0, 0, 48)
        );

        text.setAlignment(Pos.CENTER_LEFT);

        StackPane.setAlignment(
                text,
                Pos.CENTER_LEFT
        );

        getChildren().addAll(
                picture,
                fadeLeft,
                fadeBottom,
                text
        );

        // DISPLAY THE FIRST FEATURED MOVIE
        setMovie(movie);
    }

    // UPDATE FEATURED MOVIE DETAILS
    public void setMovie(Movie movie) {

        if (movie == null) {
            return;
        }

        currentMovie = movie;

        title.setText(movie.title());

        blurb.setText(
                movie.year() + "  •  " + movie.blurb()
        );

        loadBackdrop(movie);
    }

    // LOAD MOVIE BACKGROUND AND FALLBACK IMAGE
    private void loadBackdrop(Movie movie) {

        setStyle("-fx-background-color: #000000;");

        Image backdrop = new Image(
                movie.heroUrl(),
                true
        );

        picture.setImage(backdrop);

        backdrop.errorProperty().addListener(
                (obs, wasError, isError) -> {

                    // Ignore an old image finishing after
                    // the user has selected another movie.
                    if (currentMovie != movie || !isError) {
                        return;
                    }

                    Image fallback = new Image(
                            movie.fallbackHeroUrl(),
                            true
                    );

                    picture.setImage(fallback);

                    fallback.errorProperty().addListener(
                            (fallbackObs, oldValue, failed) -> {

                                if (currentMovie == movie && failed) {
                                    picture.setImage(null);
                                    setStyle(
                                            "-fx-background-color: #1a1a1a;"
                                    );
                                }
                            }
                    );
                }
        );
    }
}
