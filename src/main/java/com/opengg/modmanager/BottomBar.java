package com.opengg.modmanager;

import javax.swing.*;
import java.awt.*;

public class BottomBar extends JPanel {
    public JProgressBar progressBar;
    public JLabel progressLabel;

    public BottomBar(){
        this.setBorder(BorderFactory.createEtchedBorder());
        this.setLayout(new BorderLayout());

        var progressPanel = new JPanel();
        progressPanel.add(progressLabel = new JLabel());
        progressPanel.add(progressBar = new JProgressBar());

        this.add(progressPanel, BorderLayout.EAST);
    }

    public void setProgressString(String progressString){
        progressLabel.setText(progressString);
    }

    public void setProgressMax(int max){
        progressBar.setMaximum(max);
    }

    public void setProgress(int val){
        progressBar.setValue(val);
    }
}
