//This class includes a main method to demonstrate its use
package etomica.graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;

import etomica.Action;
import etomica.Controller;
import etomica.EtomicaElement;
import etomica.EtomicaInfo;

/**
 * Button that causes an action to be performed.
 * 
 * @author David Kofke
 */
 
public class DeviceButton extends Device implements EtomicaElement {
    
    /**
     * Constructs button that is not connected to any action.  Subsequent
     * call to setAction is needed to make the button do something.
     * @param controller
     */
    public DeviceButton(Controller controller) {
        super(controller);
        button = new JButton();
    }
    
    /**
     * Constructs a button connected to the given action.
     */
    public DeviceButton(Controller controller, Action action) {
        this(controller);
        setAction(action);
        setLabel(action.getLabel());
    }
    
    public static EtomicaInfo getEtomicaInfo() {
        EtomicaInfo info = new EtomicaInfo();
        info.setDescription("Button that causes an elementary action to be performed");
        return info;
    }
    
    /**
     * Returns the currently defined action associated with the button.
     */
    public etomica.Action getAction() {return targetAction;}

    /**
     * Defines the action to be performed when the button is pressed.
     */
    public void setAction(Action newAction) {
        if(buttonAction != null) button.removeActionListener(buttonAction);
        if(newAction == null) return;
        targetAction = newAction;
        buttonAction = new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                doAction();
            }
        };
        button.addActionListener(buttonAction);
    }
    
    /**
     * Returns the GUI element for display in the simulation.
     */
    public java.awt.Component graphic(Object obj) {
        return button;
    }
    
    /**
     * Sets the value of a descriptive label using the given string.
     */
    public void setLabel(String text) {button.setText(text);}
    /**
     * @return the current instance of the descriptive label.
     */
    public String getLabel() {return button.getText();}
    
    /**
     * @return a handle to the JButton instance used by this device
     */
    public JButton getButton() {return button;}
    
    private ActionListener buttonAction;
    private JButton button;
    
    /**
     * Method to demonstrate and test the use of this class.  
     * Slider is used to control the temperature of a hard-sphere MD simulation
     */
    public static void main(String[] args) {
        
        etomica.simulations.HSMD2D sim = new etomica.simulations.HSMD2D();
        final SimulationGraphic graphic = new SimulationGraphic(sim);
        
        //here's the part unique to this class
        etomica.action.SimulationRestart action = new etomica.action.SimulationRestart(sim);
        DeviceButton button = new DeviceButton(sim.getController(),action);
        //end of unique part
        graphic.add(button);
        graphic.makeAndDisplayFrame();
    }
    
}