
package org.dsa.iot.template;

import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.dsa.iot.dslink.DSLink;
import org.dsa.iot.dslink.DSLinkFactory;
import org.dsa.iot.dslink.DSLinkHandler;
import org.dsa.iot.dslink.node.Node;
import org.dsa.iot.dslink.node.NodeBuilder;
import org.dsa.iot.dslink.node.Permission;
import org.dsa.iot.dslink.node.actions.Action;
import org.dsa.iot.dslink.node.actions.ActionResult;
import org.dsa.iot.dslink.node.actions.Parameter;
import org.dsa.iot.dslink.node.value.Value;
import org.dsa.iot.dslink.node.value.ValueType;
import org.dsa.iot.dslink.util.Objects;
import org.dsa.iot.dslink.util.handler.Handler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.PinState;
import com.pi4j.io.gpio.RaspiPin;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.io.IOException;
import java.util.Properties;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
/**
 * The main class that starts the DSLink. Typically it extends
 * {@link DSLinkHandler} and the main method extends into it.
 */
public class Main extends DSLinkHandler {
    final String username = "sender@email.com"; 	
    final String password = "******";
    Calendar cal = Calendar.getInstance();
    SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");

    public void sendMail() {
    	 Properties props = new Properties();
         props.put("mail.smtp.auth", "true");
         props.put("mail.smtp.starttls.enable", "true");
         props.put("mail.smtp.host", "outlook.office365.com");
         props.put("mail.smtp.port", "587");

         Session session = Session.getInstance(props,
           new javax.mail.Authenticator() {
             protected PasswordAuthentication getPasswordAuthentication() {
                 return new PasswordAuthentication(username, password);
             }
           });

         try {

             Message message = new MimeMessage(session);
             message.setFrom(new InternetAddress("sender@email.com"));
             message.setRecipients(Message.RecipientType.TO,
                 InternetAddress.parse("receiver@email.com"));
             message.setSubject("LED is turned on @ "+ sdf.format(cal.getTime()));
             message.setText("sent from Raspberry Pi");

             Transport.send(message);

             System.out.println("Email sent");

         } catch (MessagingException e) {
             throw new RuntimeException(e);
         }
     }
    
    public int A=0;
	final GpioController gpio = GpioFactory.getInstance();
    
    // creating the pin with parameter PinState.HIGH
    // will instantly power up the pin
    final GpioPinDigitalOutput pin = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_07, "PinLED", PinState.LOW);
    
    Node node = null; 

    
    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

    @Override
    public boolean isResponder() {
        return true;
    }

    @Override
    public void onResponderInitialized(DSLink link) {
        Node superRoot = link.getNodeManager().getSuperRoot();
        superRoot.clearChildren();
        NodeBuilder builder = superRoot.createChild("example");
        builder.setSerializable(false);
        builder.setDisplayName("Status");
        builder.setValueType(ValueType.STRING);
           
        node = builder.build();
            
        Action act = setPathAction();
        superRoot.createChild("Set Values").setAction(act).setSerializable(false)
            .build();
    }
    
    private Action setPathAction() {
    	Action act = new Action(Permission.READ, new CreateConnHandler());
        act.addParameter(new Parameter("Switch", ValueType.NUMBER));
        return act;
    }
    
    private class CreateConnHandler implements Handler<ActionResult> {
        @Override
        public void handle(ActionResult event) {
        	System.out.println("operating the LED");
            A = (int) event.getParameter("Switch", ValueType.NUMBER).getNumber();

            if(A == 1) {
            	pin.high();
            	sendMail();
            }
            else {
            	pin.low();
            }
            String status = (A == 1) ? "On" : "Off";
            node.setValue(new Value(status));
        }
    }
    @Override
    public void onResponderConnected(DSLink link) {
        LOGGER.info("Connected");
    }

    public static void main(String[] args) {
        DSLinkFactory.start(args, new Main());
    }
    
}
