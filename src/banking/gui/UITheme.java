package banking.gui;

import javax.swing.UIManager;

public class UITheme {

    public static void apply() {
        try {
            UIManager.setLookAndFeel(
                UIManager.getSystemLookAndFeelClassName()
            );
        } catch (Exception e) {
            System.err.println("Could not apply system LookAndFeel");
        }
    }
}
