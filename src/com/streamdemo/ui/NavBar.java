
package com.streamdemo.ui;

import com.streamdemo.audio.SoundManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

public class NavBar extends VBox {

    private final HBox topRow = new HBox(8);
    private final HBox categoryRow = new HBox(12);

    private final ScrollPane categoryScroll = new ScrollPane();

    private final List<Node> navigationButtons = new ArrayList<>();

    private final Region spacer = new Region();

    public NavBar(
            List<String> categoryNames,
            Runnable onHome,
            IntConsumer onCategory,
            Consumer<String> onSearch
    ) {

        setSpacing(0);
        setStyle("-fx-background-color: linear-gradient(to bottom, #000000, #141414);");

        // APP LOGO
        Label logoRed = new Label("STREAM");
        logoRed.setStyle(
                "-fx-text-fill: #e50914;" +
                "-fx-font-size: 30px;" +
                "-fx-font-weight: 900;"
        );

        Label logoWhite = new Label("BOX");
        logoWhite.setStyle(
                "-fx-text-fill: white;" +
                "-fx-font-size: 30px;" +
                "-fx-font-weight: 900;"
        );

        HBox logo = new HBox(2, logoRed, logoWhite);
        logo.setAlignment(Pos.CENTER_LEFT);
        logo.setPadding(new Insets(0, 18, 0, 0));

        // NAVIGATION BUTTONS
        navigationButtons.add(
                new CustomButton("Home", CustomButton.Kind.GHOST, onHome)
        );

        for (int i = 0; i < categoryNames.size(); i++) {
            final int index = i;

            navigationButtons.add(
                    new CustomButton(
                            categoryNames.get(i),
                            CustomButton.Kind.GHOST,
                            () -> onCategory.accept(index)
                    )
            );
        }

        // SEARCH
        TextField searchField = new TextField();
        searchField.setPromptText("Search movies...");
        searchField.setPrefWidth(170);
        searchField.setMinWidth(100);

        searchField.setStyle(
                "-fx-background-color: #333333;" +
                "-fx-text-fill: white;" +
                "-fx-prompt-text-fill: #888888;" +
                "-fx-background-radius: 4;" +
                "-fx-padding: 6 12;" +
                "-fx-font-size: 14px;"
        );

        searchField.textProperty().addListener(
                (obs, oldValue, newValue) ->
                        onSearch.accept(newValue.toLowerCase().trim())
        );

        // SOUND
        CustomButton sound = new CustomButton(
                "Sound: ON",
                CustomButton.Kind.GHOST,
                null
        );

        sound.setOnAction(() -> {
            SoundManager.setMuted(!SoundManager.isMuted());

            sound.update(
                    SoundManager.isMuted() ? "Sound: OFF" : "Sound: ON",
                    null
            );
        });

        // SPACER
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // TOP ROW
        topRow.setAlignment(Pos.CENTER_LEFT);
        topRow.setPadding(new Insets(12, 32, 12, 32));

        // SECOND ROW
        categoryRow.setAlignment(Pos.CENTER_LEFT);
        categoryRow.setPadding(new Insets(4, 24, 10, 24));
        categoryRow.getChildren().addAll(navigationButtons);

        // SCROLLABLE CATEGORY ROW
        categoryScroll.setContent(categoryRow);
        categoryScroll.setFitToHeight(true);
        categoryScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        categoryScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        categoryScroll.setPannable(true);

        categoryScroll.setPrefHeight(44);
        categoryScroll.setMinHeight(44);
        categoryScroll.setMaxHeight(44);

        categoryScroll.setStyle(
                "-fx-background-color: transparent;" +
                "-fx-background: transparent;" +
                "-fx-border-color: transparent;"
        );

        getChildren().addAll(topRow, categoryScroll);

        // RESPOND TO WINDOW RESIZING
        widthProperty().addListener(
                (obs, oldWidth, newWidth) ->
                        updateLayout(newWidth.doubleValue(),
                                logo, searchField, sound)
        );

        updateLayout(1100, logo, searchField, sound);
    }

    private void updateLayout(
            double width,
            Node logo,
            Node search,
            Node sound
    ) {

        if (width > 1000) {

            // LARGE WINDOW: EVERYTHING ON ONE ROW
            categoryScroll.setVisible(false);
            categoryScroll.setManaged(false);

            topRow.getChildren().setAll(
                    logo,
                    navigationButtons.get(0),
                    navigationButtons.get(1),
                    navigationButtons.get(2),
                    navigationButtons.get(3),
                    spacer,
                    search,
                    sound
            );

        } else {

            // SMALL WINDOW: NAVIGATION MOVES TO SECOND ROW
            topRow.getChildren().setAll(
                    logo,
                    spacer,
                    search,
                    sound
            );

            categoryRow.getChildren().setAll(navigationButtons);

            categoryScroll.setVisible(true);
            categoryScroll.setManaged(true);
        }
    }
}
