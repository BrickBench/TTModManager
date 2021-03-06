package modmanager.ui;

import javafx.application.Platform;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;

import java.io.OutputStream;
import java.io.PrintStream;

public class BottomPane extends VBox {
    private BottomBar bottomBar;
    public ModPane modPane;

    public BottomPane(){
        var textArea = new TextArea();
        textArea.setEditable(false);

        var out = System.out;
        var err= System.err;
        PrintStream printStreamOut = new PrintStream(new ConsoleOutput(textArea, out));
        System.setOut(printStreamOut);

        PrintStream printStreamErr = new PrintStream(new ConsoleOutput(textArea, err));

        System.setErr(printStreamErr);

        textArea.prefWidthProperty().bind(this.widthProperty());
        this.setPrefHeight(150);

        modPane = new ModPane();

        var tabPane = new TabPane();
        tabPane.setStyle("-fx-border-color: gray");
        tabPane.prefWidthProperty().bind(this.widthProperty());
        textArea.prefWidthProperty().bind(tabPane.widthProperty());
        modPane.prefWidthProperty().bind(tabPane.widthProperty());
        modPane.prefHeightProperty().bind(tabPane.heightProperty());


        tabPane.getTabs().add(new Tab("Mod", modPane));
        tabPane.getTabs().add(new Tab("Console", new ScrollPane(textArea)));

        this.getChildren().add(tabPane);
        this.getChildren().add(bottomBar = new BottomBar());
    }

    class ConsoleOutput extends OutputStream {
        private TextArea textArea;
        private PrintStream out;
        private String lastLine;

        public ConsoleOutput(TextArea textArea, PrintStream out) {
            this.textArea = textArea;
            this.out = out;
        }

        @Override
        public void write(int b) {
            lastLine += (char)b;
            out.write(b);

            if(b == '\n'){
                textArea.appendText(lastLine);
                textArea.positionCaret(textArea.getLength()-1);
                lastLine = "";
            }
        }
    }

    public void setProgress(double progress){
        Platform.runLater(() -> this.bottomBar.setProgress(progress));
    }

    public void setProgressString(String progress){
        Platform.runLater(() -> {
            this.bottomBar.setProgressString(progress);
        });
    }

    public static void log(String log){
        if(Platform.isFxApplicationThread()){
            System.out.println(log);
        }else{
            Platform.runLater(() -> System.out.println(log));
        }
    }
}

