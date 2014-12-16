package be.uantwerpen.ds.system_y.test;

import javax.swing.SwingUtilities;
import be.uantwerpen.ds.system_y.gui.*;

public class GuiTest
{
    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                View view = new View();
                Model model = new Model();
                Controller controller = new Controller(model, view);
                controller.control();
            }
        });
    }
}