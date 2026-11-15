
package com.streamdemo.ui;

import com.streamdemo.model.Category;
import com.streamdemo.model.Movie;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class HomeView extends ScrollPane {

    private final VBox content = new VBox();

    private final List<CategoryRow> rows = new ArrayList<>();

    private final List<Category> originalCategories;

    private Movie featured;

    private final Consumer<Movie> onSelect;

    private HeroBanner heroBanner;

    private Timeline scrollAnimation;

    public HomeView(
            List<Category> categories,
            Movie featured,
            Consumer<Movie> onSelect
    ) {
        this.originalCategories = categories;
        this.featured = featured;
        this.onSelect = onSelect;

        setFitToWidth(true);
        setHbarPolicy(ScrollBarPolicy.NEVER);
        setVbarPolicy(ScrollBarPolicy.AS_NEEDED);

        content.setStyle("-fx-background-color: #141414;");

        setContent(content);

        buildHome(false, "");
    }


private void buildHome(boolean isSearching, String query) {

    content.getChildren().clear();
    rows.clear();
    heroBanner = null;

    if (!isSearching) {

        // FEATURED TRAILER
        heroBanner = new HeroBanner(
                featured,
                widthProperty(),
                onSelect
        );

        content.getChildren().add(heroBanner);

        // MOVIE CATEGORIES
        for (Category category : originalCategories) {

            CategoryRow row = new CategoryRow(
                    category,
                    this::selectFeaturedMovie
            );

            rows.add(row);
            content.getChildren().add(row);
        }

    } else {

        // SEARCH RESULTS
        List<Movie> matches = new ArrayList<>();

        for (Category category : originalCategories) {
            for (Movie movie : category.movies()) {

                if (movie.title()
                        .toLowerCase()
                        .contains(query.toLowerCase())) {

                    matches.add(movie);
                }
            }
        }

        if (!matches.isEmpty()) {

            CategoryRow searchRow = new CategoryRow(
                    new Category("Search Results", matches),
                    this::selectFeaturedMovie
            );

            rows.add(searchRow);
            content.getChildren().add(searchRow);

        } else {

            Label noResult = new Label(
                    "No results found for \"" + query + "\""
            );

            noResult.setStyle(
                    "-fx-text-fill: #888888;" +
                    "-fx-font-size: 24px;" +
                    "-fx-font-weight: bold;"
            );

            noResult.setPadding(
                    new Insets(60, 48, 40, 48)
            );

            content.getChildren().add(noResult);
        }
    }

    // FOOTER
    if (!isSearching) {

        Label footer = new Label(
                "StreamBox demo  •  Trailers are streamed from YouTube and are the property of their owners."
        );

        footer.setStyle(
                "-fx-text-fill: #5a5a5a;" +
                "-fx-font-size: 12px;"
        );

        footer.setPadding(
                new Insets(40, 48, 40, 48)
        );

        content.getChildren().add(footer);
    }
}


    // UPDATE THE FEATURED MOVIE WHEN A CARD IS CLICKED
    private void selectFeaturedMovie(Movie movie) {

        featured = movie;

        if (heroBanner != null) {

            heroBanner.setMovie(movie);

        } else {

            // If the user selected a movie from search results,
            // return to the main page and show it in the featured area.
            buildHome(false, "");

            if (heroBanner != null) {
                heroBanner.setMovie(movie);
            }
        }

        // Bring the updated featured trailer into view.
        animateTo(0);
    }

    public void filter(String query) {

        if (query == null || query.trim().isEmpty()) {

            buildHome(false, "");

        } else {

            buildHome(true, query.trim());
        }
    }

    public void scrollToTop() {

        animateTo(0);
    }

    public void scrollToCategory(int index) {

        double scrollable =
                content.getHeight() - getViewportBounds().getHeight();

        if (index < 0 || index >= rows.size() || scrollable <= 0) {
            return;
        }

        animateTo(
                Math.min(
                        1,
                        rows.get(index).getBoundsInParent().getMinY()
                                / scrollable
                )
        );
    }

    private void animateTo(double vvalue) {

        if (scrollAnimation != null) {
            scrollAnimation.stop();
        }

        scrollAnimation = new Timeline(
                new KeyFrame(
                        Duration.millis(500),
                        new KeyValue(
                                vvalueProperty(),
                                vvalue,
                                Interpolator.EASE_BOTH
                        )
                )
        );

        scrollAnimation.play();
    }
}
