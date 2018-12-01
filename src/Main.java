import com.github.sarxos.webcam.*;

import javax.imageio.ImageIO;
import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;


//images to gif
//convert -delay 20 22*.jpg movie.gif

public class Main implements WebcamMotionListener {

    private final DateFormat folderFormat = new SimpleDateFormat("dd-MM-yyyy_HH");
    private final DateFormat hourCheckFormat = new SimpleDateFormat("HH");
    private final DateFormat picNameFormat = new SimpleDateFormat("HH:mm:ss.SSS");

    private int savedHour = 25;
    private String currentFolder;
    private Session session;
    private boolean lastHourMotion = false;

    private boolean running = false;

    public Main(){
        running = true;

        //Date date = new Date();
        //savedHour = Integer.parseInt(hourCheckFormat.format(date));
        //prepareDirectory(date);

        Properties properties = System.getProperties();
        properties.put("mail.smtp.auth","true");
        properties.put("mail.smtp.starttls.enable","true");
        properties.put("mail.smtp.host","smtp.gmail.com");
        properties.put("mail.smtp.port","587");

        session = Session.getInstance(properties, new javax.mail.Authenticator(){
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(PrivateCredentials.EMAIL, PrivateCredentials.EMAIL_PASS);
            }
        });

        Dimension[] nonStandardResolutions = new Dimension[]{
                WebcamResolution.PAL.getSize(),
                WebcamResolution.HD.getSize(),
        };
        //176,144 / 320,240 / 640,480

        Webcam webcam = Webcam.getDefault();
        webcam.setCustomViewSizes(nonStandardResolutions);
        webcam.setViewSize(new Dimension(320,240));

        WebcamMotionDetector detector = new WebcamMotionDetector(Webcam.getDefault());
        detector.setInterval(250);
        detector.addMotionListener(this);
        detector.start();



        while(running){
            Date date = new Date();
            int currentHour = Integer.parseInt(hourCheckFormat.format(date));
            if(currentHour!=savedHour) {
                updateFolders(date,currentHour);
            }

            try {
                Thread.sleep(1000*60);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void updateFolders(Date date, int currentHour){

            if (lastHourMotion) {
                lastHourMotion = false;
                sendText("Motion detected within the last hour: " + currentFolder);
            }
            savedHour = currentHour;
            prepareDirectory(date);

    }


    @Override
    public void motionDetected(WebcamMotionEvent webcamMotionEvent) {

        Date date = new Date();
        int currentHour = Integer.parseInt(hourCheckFormat.format(date));
        if(currentHour!=savedHour) {
            updateFolders(date,currentHour);
        }


        String picName = picNameFormat.format(date);
        try {
            ImageIO.write(webcamMotionEvent.getCurrentImage(),"JPG",new File(currentFolder+"/"+picName+".jpg"));
            lastHourMotion = true;
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    private void prepareDirectory(Date date){

        currentFolder = PrivateCredentials.FILE_PATH+folderFormat.format(date);
        boolean result = new File(currentFolder).mkdirs();
        if(!result){
            System.out.println("Failed to create directory or directory already exists: "+currentFolder);
        }
    }

    private  void sendText(String messageText){
        new Thread(() -> {

            MimeMessage message = new MimeMessage(session);
            try {
               message.setFrom(new InternetAddress(PrivateCredentials.EMAIL));
                message.setRecipient(Message.RecipientType.TO,new InternetAddress(PrivateCredentials.PHONE_NUMBER));
                message.setText(messageText);
                Transport.send(message);

            } catch (MessagingException e) {
                System.out.println("Failed to send text");
                e.printStackTrace();
            }
        }).start();

    }



    public static void main(String[] args) throws IOException {

        new Main();
        System.out.println("Setup complete");


    }



}
