
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

    private final Label tag = new Label("FEATURED TRAILER");

    private final Consumer<Movie> onPlay;

    public HeroBanner(
            Movie movie,
            ObservableDoubleValue windowWidth,
            Consumer<Movie> onPlay
    ) {

        this.onPlay = onPlay;

        // RESPONSIVE BANNER HEIGHT
        DoubleBinding height = Bindings.createDoubleBinding(
                () -> {
                    double width = windowWidth.get();

                    if (width < 600) {
                        return Math.max(240, Math.min(360, width * 0.85));
                    }

                    return Math.max(320, Math.min(520, width * 0.42));
                },
                windowWidth
        );

        prefHeightProperty().bind(height);
        minHeightProperty().bind(height);
        maxHeightProperty().bind(height);

        setStyle("-fx-background-color: #000000;");

        // CLIP IMAGE TO BANNER
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
        tag.setStyle(
                "-fx-text-fill: #e50914;" +
                "-fx-font-size: 14px;" +
                "-fx-font-weight: bold;"
        );

        // MOVIE TITLE
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

        // PLAY VIDEO BUTTON
        CustomButton play = new CustomButton(
                "PLAY VIDEO",
                CustomButton.Kind.PRIMARY,
                () -> this.onPlay.accept(currentMovie)
        );

        HBox buttons = new HBox(12, play);

        buttons.setPadding(
                new Insets(8, 0, 0, 0)
        );

        // TEXT CONTAINER
        VBox text = new VBox(
                10,
                tag,
                title,
                blurb,
                buttons
        );

        text.setAlignment(Pos.CENTER_LEFT);

        text.maxWidthProperty().bind(
                widthProperty().multiply(0.90)
        );

        // RESPONSIVE PADDING
        text.paddingProperty().bind(
                Bindings.createObjectBinding(
                        () -> {
                            double width = windowWidth.get();

                            double horizontalPadding =
                                    width < 600
                                            ? 16
                                            : Math.min(48, width * 0.045);

                            return new Insets(
                                    0,
                                    horizontalPadding,
                                    0,
                                    horizontalPadding
                            );
                        },
                        windowWidth
                )
        );

        // RESPONSIVE FONT SIZES AND SPACING
        Runnable updateTextSize = () -> {

            double width = windowWidth.get();

            double titleSize;
            double descriptionSize;
            double tagSize;
            double spacing;

            if (width < 450) {

                titleSize = 27;
                descriptionSize = 12;
                tagSize = 10;
                spacing = 6;

            } else if (width < 600) {

                titleSize = 30;
                descriptionSize = 13;
                tagSize = 11;
                spacing = 7;

            } else if (width < 900) {

                titleSize = 38;
                descriptionSize = 15;
                tagSize = 12;
                spacing = 8;

            } else {

                titleSize = Math.min(52, width * 0.055);
                descriptionSize = 18;
                tagSize = 14;
                spacing = 10;
            }

            title.setStyle(
                    "-fx-text-fill: white;" +
                    "-fx-font-size: " + titleSize + "px;" +
                    "-fx-font-weight: bold;"
            );

            blurb.setStyle(
                    "-fx-text-fill: #e5e5e5;" +
                    "-fx-font-size: " + descriptionSize + "px;"
            );

            tag.setStyle(
                    "-fx-text-fill: #e50914;" +
                    "-fx-font-size: " + tagSize + "px;" +
                    "-fx-font-weight: bold;"
            );

            text.setSpacing(spacing);
        };

        // APPLY SIZES WHEN THE BANNER FIRST OPENS
        updateTextSize.run();

        // UPDATE WHEN THE WINDOW IS RESIZED
        windowWidth.addListener(
                (obs, oldValue, newValue) ->
                        updateTextSize.run()
        );

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

        // DISPLAY SELECTED MOVIE
        setMovie(movie);
    }

    // UPDATE FEATURED MOVIE
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

    // LOAD BACKGROUND IMAGE
    private void loadBackdrop(Movie movie) {

        setStyle("-fx-background-color: #000000;");

        Image backdrop = new Image(
                movie.heroUrl(),
                true
        );

        picture.setImage(backdrop);

        backdrop.errorProperty().addListener(
                (obs, wasError, isError) -> {

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
